package ch.unibe.scg.methodnullabilityplugin.database;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ch.unibe.scg.methodnullabilityplugin.eea.CsvAccessor.NullabilityRecord;

/**
 * Based on a given list of {@link NullabilityRecord}, {@link JavadocDatabaseCreator} creates a sqlite database 
 * in the given <code>databaseUrl</code>. If the database already exists, it is deleted first. If it does not exist,
 * it gets created from scratch.
 * 
 */
public class JavadocDatabaseCreator {

	public int execute(URL databaseUrl, List<NullabilityRecord> csvEntries, String artifactIds, double maxNullabilityNonNull, double minNullabilityNullable) throws Exception {
		if (csvEntries == null || csvEntries.isEmpty()) {
			return 0;
		}
		
		AtomicInteger numRecords = new AtomicInteger(0);
		try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseUrl.getFile())) {
			try (Statement statement = connection.createStatement()) {
				Arrays.asList(Database.INDEX_ANY_VERSION, Database.INDEX_ANY_ARTIFACT).stream()
						.forEach(index -> {
							try {
								statement.executeUpdate("drop table if exists " + index);
								statement.executeUpdate(
										"create table " + index + " (hash string, nonNulls integer, dereferences integer)");
								statement.executeUpdate("create index " + index + "_hash on " + index + " (hash)");
							} catch (SQLException exception) {
								throw new RuntimeException(exception);
							}
						});

				csvEntries.forEach(record -> {
					if (artifactIds == null || artifactIds.trim().isEmpty() || artifactIds.contains(record.getArtifactId())) {
						double nullability = record.nullability();
						boolean skip;
						if (nullability >= minNullabilityNullable) {
							skip = false;
						} else if (nullability < maxNullabilityNonNull || maxNullabilityNonNull == 0 && nullability == maxNullabilityNonNull) {
							skip = false;
						} else {
							skip = true;
						}
						
						if (!skip) {
							int checks = record.getChecks();
							int dereferences = record.getDereferences();
							String hashAnyVersion = Database.hash(record.getGroupId(), record.getArtifactId(), record.getClazz(), record.getMethod());
							String hashAnyArtifact = Database.hash(record.getClazz(), record.getMethod());
							try {
								statement.executeUpdate("insert into " + Database.INDEX_ANY_VERSION + " values('" + hashAnyVersion
										+ "', " + checks + ", " + dereferences + ")");
								statement.executeUpdate("insert into " + Database.INDEX_ANY_ARTIFACT + " values('" + hashAnyArtifact
										+ "', " + checks + ", " + dereferences + ")");
								numRecords.incrementAndGet();
							} catch (SQLException exception) {
								throw new RuntimeException(exception);
							}
						}
					}
					
				});
			}
		}
		return numRecords.get();
	}
	
}
