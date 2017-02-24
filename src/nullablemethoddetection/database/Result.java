package nullablemethoddetection.database;

public class Result {

	public final Match exact;
	public final Match anyVersion;
	public final Match anyArtifact;

	public Result(Match exact, Match anyVersion, Match anyArtifact) {
		this.exact = exact;
		this.anyVersion = anyVersion;
		this.anyArtifact = anyArtifact;
	}

}
