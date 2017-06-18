package ch.unibe.scg.methodnullabilityplugin.database;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import com.google.common.hash.Hashing;

import ch.unibe.scg.methodnullabilityplugin.Console;

/**
 * Facilitates access to sqlite database.
 */
public class Database {

	{
		// load SQLite JDBC driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException exception) {
			throw new RuntimeException(exception);
		}
	}

	static final Artifact ARTIFACT_JRE = new Artifact("Oracle Corporation", "jre", "1.8.0_111");
	static final Artifact ARTIFACT_NO_ARTIFACT_FOUND = new Artifact("no-artifact-found", "no-artifact-found",
			"no-artifact-found");
	static final String INDEX_EXACT = "exact";
	static final String INDEX_ANY_VERSION = "any_version";
	static final String INDEX_ANY_ARTIFACT = "any_artifact";

	static String hash(String... parts) {
		return Hashing.sha256().hashString(String.join("|", parts), StandardCharsets.UTF_8).toString();
	}

	private Connection connection;

	public Database(URL databaseUrl) throws SQLException, IOException {
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseUrl.getFile());
	}

	/**
	 * Searches the nullability database for data matching the specified method.
	 * 
	 * @param method
	 *            The method to search nullbility data for
	 * @return The resulting matches, might contain empty matches, if no entries
	 *         are found
	 * @throws JavaModelException
	 *             If the access to method model failed
	 */
	public Result search(IMethod method) throws JavaModelException {
		IType declaringType = method.getDeclaringType();
		String returnType = Signature.toString(method.getReturnType());
		String name = method.getElementName();
		List<String> parameterTypes = Stream.of(method.getParameterTypes()).map(Signature::toString)
				.collect(Collectors.toList());
		String methodFull = String.format("%s %s(%s)", returnType, name, String.join(",", parameterTypes));
		return this.search(this.toArtifact(declaringType), declaringType.getFullyQualifiedName(), methodFull);
	}

	private Result search(Artifact artifact, String clazz, String method) {
		Map<String, String> hashes = new HashMap<>();
		// String hashExact = hash(artifact.groupId, artifact.artifactId,
		// /*artifact.version,*/ clazz, method);
		String hashAnyVersion = hash(artifact.groupId, artifact.artifactId, clazz, method);
		String hashAnyArtifact = hash(clazz, method);
		// hashes.get(INDEX_EXACT).add(hashExact);
		hashes.put(INDEX_ANY_VERSION, hashAnyVersion);
		hashes.put(INDEX_ANY_ARTIFACT, hashAnyArtifact);
		Optional<Match> matchAnyArtifact = this.match(INDEX_ANY_ARTIFACT, hashes.get(INDEX_ANY_ARTIFACT));
		Optional<Match> matchAnyVersion = this.match(INDEX_ANY_VERSION, hashes.get(INDEX_ANY_VERSION));
		Optional<Match> matchExact = Optional.empty(); // this.match(INDEX_EXACT,
														// hashes.get(INDEX_EXACT));
		return new Result(matchExact.orElse(new Match(0, 0)), matchAnyVersion.orElse(new Match(0, 0)),
				matchAnyArtifact.orElse(new Match(0, 0)));
	}

	private Optional<Match> match(String index, String hash) {
		try {
			try (Statement statement = this.connection.createStatement()) {
				ResultSet existsDB = statement.executeQuery(
						"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='" + INDEX_ANY_ARTIFACT + "';");
				if (existsDB.getInt(1) == 0) {
					return Optional.empty();
				}
				ResultSet resultSet = statement.executeQuery("select sum(nonNulls), sum(dereferences) from " + index
						+ " where hash  = " + String.format("'%s'", hash));
				if (resultSet.next()) {
					return Optional.of(new Match(resultSet.getInt(1), resultSet.getInt(2)));
				}
				return Optional.empty();
			}
		} catch (SQLException exception) {
			throw new RuntimeException(exception);
		}
	}

	private Artifact toArtifact(IType type) {
		String path = type.getPackageFragment().getPath().toString();
		if (path.endsWith("/rt.jar")) {
			return ARTIFACT_JRE;
		}
		if (path.contains("/.m2/repository/")) {
			String[] parts = path.split("/\\.m2/repository/")[1].split("/");
			String groupId = String.join(".", Arrays.copyOfRange(parts, 0, parts.length - 3));
			String artifactId = parts[parts.length - 3];
			String version = parts[parts.length - 2];
			return new Artifact(groupId, artifactId, version);
		}
		return ARTIFACT_NO_ARTIFACT_FOUND;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			connection.close();
		} catch (RuntimeException re) {
			Console.msg(re);
		}
	}

}
