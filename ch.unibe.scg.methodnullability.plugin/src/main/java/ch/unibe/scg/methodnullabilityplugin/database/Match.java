package ch.unibe.scg.methodnullabilityplugin.database;

/**
 * POJO for nullability match.
 */
public class Match {

	/**
	 * How many dereferences are checked for null.
	 */
	public final int checks;
	/**
	 * How often the method is invoked.
	 */
	public final int dereferences;

	public Match(int checks, int dereferences) {
		this.checks = checks;
		this.dereferences = dereferences;
	}

}
