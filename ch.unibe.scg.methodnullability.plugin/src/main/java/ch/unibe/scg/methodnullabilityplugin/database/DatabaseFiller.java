package ch.unibe.scg.methodnullabilityplugin.database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

public class DatabaseFiller {

	/**
	 * Fills the method nullability database from raw data.
	 * 
	 * @param args
	 *            Paths to CSV and path to resulting database.
	 * @throws SQLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws SQLException, FileNotFoundException, IOException {
		Connection connection = DriverManager.getConnection("jdbc:sqlite:" + args[1]);
		Statement statement = connection.createStatement();
		Arrays.asList(Database.INDEX_EXACT, Database.INDEX_ANY_VERSION, Database.INDEX_ANY_ARTIFACT).stream()
				.forEach(index -> {
					try {
						statement.executeUpdate("drop table if exists " + index);
						statement.executeUpdate(
								"create table " + index + " (hash string, checks integer, invocations integer)");
						statement.executeUpdate("create index " + index + "_hash on exact (hash)");
					} catch (SQLException exception) {
						throw new RuntimeException(exception);
					}
				});
		CSVParser parser = CSVFormat.DEFAULT.withQuote('"').withFirstRecordAsHeader()
				.parse(new InputStreamReader(new FileInputStream(Paths.get(args[0]).toFile())));
		StreamSupport.stream(parser.spliterator(), false).forEach(record -> {
			int checks = Integer.parseInt(record.get("checks"));
			int invocations = Integer.parseInt(record.get("invocations"));
			if (invocations < 100) {
				return;
			}
			String hashExact = Database.hash(record.get("groupId"), record.get("artifactId"), record.get("version"),
					record.get("class"), record.get("method"));
			String hashAnyVersion = Database.hash(record.get("groupId"), record.get("artifactId"), record.get("class"),
					record.get("method"));
			String hashAnyArtifact = Database.hash(record.get("class"), record.get("method"));
			try {
				statement.executeUpdate("insert into " + Database.INDEX_EXACT + " values('" + hashExact + "', " + checks
						+ ", " + invocations + ")");
				statement.executeUpdate("insert into " + Database.INDEX_ANY_VERSION + " values('" + hashAnyVersion
						+ "', " + checks + ", " + invocations + ")");
				statement.executeUpdate("insert into " + Database.INDEX_ANY_ARTIFACT + " values('" + hashAnyArtifact
						+ "', " + checks + ", " + invocations + ")");
			} catch (SQLException exception) {
				throw new RuntimeException(exception);
			}
		});
	}

}
