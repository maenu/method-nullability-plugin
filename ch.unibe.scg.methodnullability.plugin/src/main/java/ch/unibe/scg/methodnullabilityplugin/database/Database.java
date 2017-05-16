package ch.unibe.scg.methodnullabilityplugin.database;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import com.google.common.hash.Hashing;

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

	private DeclaringRootTypesFinder declaringRootClassFinder;
	private Connection connection;

	public Database() throws SQLException, IOException {
		this.declaringRootClassFinder = new DeclaringRootTypesFinder();
		URL url = FileLocator.toFileURL(
				Platform.getBundle("ch.unibe.scg.methodnullability.plugin").getEntry("src/main/resources/method-nullability.db"));
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + url.getFile());
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
		Map<String, Artifact> declaringRootTypes = this.declaringRootClassFinder.findDeclaringRootTypes(method).stream()
				.collect(Collectors.toMap(IType::getFullyQualifiedName, this::toArtifact));
		String returnType = Signature.toString(method.getReturnType());
		String name = method.getElementName();
		List<String> parameterTypes = Stream.of(method.getParameterTypes()).map(Signature::toString)
				.collect(Collectors.toList());
		String methodFull = String.format("%s %s(%s)", returnType, name, String.join(",", parameterTypes));
		return this.search(declaringRootTypes, methodFull);
	}

	private Result search(Map<String, Artifact> classes, String method) {
		Map<String, List<String>> hashes = new HashMap<>();
		hashes.put(INDEX_EXACT, new ArrayList<>());
		hashes.put(INDEX_ANY_VERSION, new ArrayList<>());
		hashes.put(INDEX_ANY_ARTIFACT, new ArrayList<>());
		classes.entrySet().stream().forEach(entry -> {
			String clazz = entry.getKey();
			Artifact artifact = entry.getValue();
			String hashExact = hash(artifact.groupId, artifact.artifactId, artifact.version, clazz, method);
			String hashAnyVersion = hash(artifact.groupId, artifact.artifactId, clazz, method);
			String hashAnyArtifact = hash(clazz, method);
			hashes.get(INDEX_EXACT).add(hashExact);
			hashes.get(INDEX_ANY_VERSION).add(hashAnyVersion);
			hashes.get(INDEX_ANY_ARTIFACT).add(hashAnyArtifact);
		});
		Optional<Match> matchAnyArtifact = this.match(INDEX_ANY_ARTIFACT, hashes.get(INDEX_ANY_ARTIFACT));
		Optional<Match> matchAnyVersion = this.match(INDEX_ANY_VERSION, hashes.get(INDEX_ANY_VERSION));
		Optional<Match> matchExact = this.match(INDEX_EXACT, hashes.get(INDEX_EXACT));
		return new Result(matchExact.orElse(new Match(0, 0)), matchAnyVersion.orElse(new Match(0, 0)),
				matchAnyArtifact.orElse(new Match(0, 0)));
	}

	private Optional<Match> match(String index, List<String> hashes) {
		String hashStrings = hashes.stream().map(hash -> String.format("'%s'", hash)).collect(Collectors.joining(", "));
		try {
			Statement statement = this.connection.createStatement();
			ResultSet resultSet = statement.executeQuery(
					"select sum(checks), sum(invocations) from " + index + " where hash in (" + hashStrings + ")");
			if (resultSet.next()) {
				return Optional.of(new Match(resultSet.getInt(1), resultSet.getInt(2)));
			}
			return Optional.empty();
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

}
