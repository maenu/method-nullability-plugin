package ch.unibe.scg.methodnullabilityplugin.database;

import java.util.Objects;

/**
 * Simple POJO.
 */
public class MethodNullabilityInfo {

	private final Match match;
	private final boolean exists;
	
	public MethodNullabilityInfo(Match match) {
		this.match = Objects.requireNonNull(match);
		this.exists = true;
	}
	
	public MethodNullabilityInfo() {
		this.match = null;
		this.exists = false;
	}
	
	public boolean exists() {
		return exists;
	}
	
	public int getChecks() {
		return match.checks;
	}
	
	public int getInvocations() {
		return match.invocations;
	}
	
	public boolean hasInvocations() {
		return match.invocations > 0;
	}
	
	public double nullability() {
		return (double) match.checks / match.invocations;
	}
	
	public static void main(String[] args) {
		MethodNullabilityInfo i = new MethodNullabilityInfo(new Match(20, 100));
		System.out.println(i.nullability());
	}
}
