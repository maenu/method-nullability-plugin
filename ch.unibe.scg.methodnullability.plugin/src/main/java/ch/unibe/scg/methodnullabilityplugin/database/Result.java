package ch.unibe.scg.methodnullabilityplugin.database;

/**
 * POJO for nullability result.
 */
public class Result {

	/**
	 * Matches exactly the same version.
	 */
	public final Match exact;
	/**
	 * Matches any version of the same artifact.
	 */
	public final Match anyVersion;
	/**
	 * Matches any method with the same signature.
	 */
	public final Match anyArtifact;

	public Result(Match exact, Match anyVersion, Match anyArtifact) {
		this.exact = exact;
		this.anyVersion = anyVersion;
		this.anyArtifact = anyArtifact;
	}

}
