package ch.unibe.scg.methodnullabilityplugin.database;

/**
 * POJO for nullability match.
 */
public class Match {

	/**
	 * How many invocations are checked for null.
	 */
	public final int checks;
	/**
	 * How often the method is invoked.
	 */
	public final int invocations;

	public Match(int checks, int invocations) {
		this.checks = checks;
		this.invocations = invocations;
	}

}
