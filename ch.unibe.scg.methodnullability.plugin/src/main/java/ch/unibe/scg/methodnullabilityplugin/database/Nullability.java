package ch.unibe.scg.methodnullabilityplugin.database;

public class Nullability {

	public final int support;
	public final int evidence;
	public final double nullability;

	public Nullability(int support, int evidence) {
		this.support = support;
		this.evidence = evidence;
		this.nullability = (double) evidence / support;
	}

}
