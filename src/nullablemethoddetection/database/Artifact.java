package nullablemethoddetection.database;

public class Artifact {

	public final String groupId;
	public final String artifactId;
	public final String version;

	public Artifact(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

}
