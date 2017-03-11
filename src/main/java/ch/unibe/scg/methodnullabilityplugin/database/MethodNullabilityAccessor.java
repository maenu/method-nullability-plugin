package ch.unibe.scg.methodnullabilityplugin.database;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import ch.unibe.scg.methodnullabilityplugin.Console;

public class MethodNullabilityAccessor {

	/**
	 * Contains the method nullability data.
	 */
	private Database database;
	
	public MethodNullabilityAccessor() {
		try {
			this.database = new Database();
		} catch (SQLException | IOException e) {
			Console.err(e);
			throw new RuntimeException(e);
		}
	}
	
	public MethodNullabilityInfo retrieve(IMethod method) {
		try {
			Result result = this.database.search(method);
			return new MethodNullabilityInfo(extractBestMatch(result));
		} catch (JavaModelException exception) {
			// bubble exception if search fails
			throw new RuntimeException(exception);
		}	
	}
	
	/**
	 * Extract the best match from the specified result: extact &gt; anyVersion
	 * &gt; anyArtifact.
	 * 
	 * @param result
	 *            The result to extract the best match from.
	 * @return The best match.
	 */
	private Match extractBestMatch(Result result) {
		if (result.exact.invocations > 0) {
			return result.exact;
		}
		if (result.anyVersion.invocations > 0) {
			return result.anyVersion;
		}
		return result.anyArtifact;
	}
	
}
