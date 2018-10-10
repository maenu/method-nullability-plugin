package ch.unibe.scg.methodnullabilityplugin.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.eclipse.jdt.core.util.ExternalAnnotationUtil;
import org.eclipse.jdt.core.util.ExternalAnnotationUtil.MergeStrategy;
import org.eclipse.jdt.internal.compiler.lookup.SignatureWrapper;

import ch.unibe.scg.methodnullabilityplugin.database.Database;

public class DatabaseImporter implements AutoCloseable {

	/**
	 * @see ExternalAnnotationUtil#insertParameterAnnotation(String, int, char,
	 *      MergeStrategy)
	 * @see ExternalAnnotationUtil#insertReturnAnnotation(String, char,
	 *      MergeStrategy)
	 */
	public static boolean isPrimitive(String methodSignature, int index) {
		assert index >= -1;
		methodSignature = methodSignature.substring(methodSignature.indexOf('('));
		int start = 0;
		if (index == -1) {
			start = methodSignature.indexOf(')') + 1;
		} else {
			SignatureWrapper wrapper = new SignatureWrapper(methodSignature.toCharArray());
			wrapper.start = 1;
			for (int i = 0; i < index; i++) {
				wrapper.start = wrapper.computeEnd() + 1;
			}
			start = wrapper.start;
		}
		switch (methodSignature.charAt(start)) {
		case 'L':
		case 'T':
		case '[':
			return false;
		}
		return true;
	}

	private Connection connection;
	private int inserted;
	private int skipped;

	public DatabaseImporter(String url) throws SQLException, IOException {
		this.connection = DriverManager.getConnection(url);
	}

	public void execute(File csv) throws SQLException, FileNotFoundException, IOException {
		this.inserted = 0;
		this.skipped = 0;
		AtomicInteger inserted = new AtomicInteger(0);
		AtomicInteger skipped = new AtomicInteger(0);
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("drop table if exists " + Database.TABLE);
			statement.executeUpdate("create table " + Database.TABLE
					+ " (class string, method string, index_ integer, support integer, evidence integer, primary key (class, method, index_))");
			statement.executeUpdate(
					"create index " + Database.TABLE + "_class_method_index on " + Database.TABLE + " (class, method)");
			try (CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(csv))) {
				parser.forEach(r -> {
					if (DatabaseImporter.isPrimitive(r.get("method"), Integer.valueOf(r.get("index")))) {
						skipped.incrementAndGet();
						return;
					}
					try {
						statement.executeUpdate("insert into " + Database.TABLE + " values('" + r.get("class") + "', '"
								+ r.get("method") + "', " + r.get("index") + ", " + r.get("support") + ", "
								+ r.get("evidence") + ")");
						inserted.incrementAndGet();
					} catch (SQLException exception) {
						throw new RuntimeException(exception);
					}
				});
			}
		}
		this.inserted = inserted.get();
		this.skipped = skipped.get();
	}

	public int getInserted() {
		return inserted;
	}

	public int getSkipped() {
		return skipped;
	}

	@Override
	public void close() throws Exception {
		this.connection.close();
	}

}
