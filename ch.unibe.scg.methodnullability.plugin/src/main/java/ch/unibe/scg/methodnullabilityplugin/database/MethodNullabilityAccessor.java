package ch.unibe.scg.methodnullabilityplugin.database;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import ch.unibe.scg.methodnullabilityplugin.Activator;
import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.util.Util;

/**
 * Global accessor class that facilitates access to {@link MethodNullabilityInfo} and the database location.
 * 
 * @see Database
 * @see MethodNullabilityInfo
 */
public class MethodNullabilityAccessor {

	/**
	 * Contains the method nullability data.
	 */
	private Database database;
	
	private static final MethodNullabilityInfo NA = new MethodNullabilityInfo();
	
	public MethodNullabilityAccessor() {
		try {
			this.database = new Database(getDatabaseUrl());
		} catch (SQLException | IOException e) {
			Console.err(e);
			throw new RuntimeException(e);
		}
	}
	
	public URL getDatabaseUrl() {
		IPath stateLocation = Platform.getStateLocation(Activator.getContext().getBundle());
		IPath fileLocation = stateLocation.append("method-nullability").addFileExtension("db");
		URL databaseUrl = null;
		try {
			databaseUrl = new URL("file://" + fileLocation.toOSString());
			URL url = FileLocator.toFileURL(databaseUrl);
			return url;
		} catch (IOException e) {
			Console.err(e);
			throw new IllegalArgumentException(e);
		}
	}
	
	public MethodNullabilityInfo retrieve(IJavaElement javaElement) {
		if (isMethodWithReferenceTypeReturnValue(javaElement)) {
			Console.trace("method " + javaElement.getElementName() + " is nullability-checkable!");
			return retrieve((IMethod) javaElement);
		}
		return NA;
	}
	
	public MethodNullabilityInfo retrieve(IMethod method) {
		try {
			Result result = this.database.search(method);
			return new MethodNullabilityInfo(extractBestMatch(result));
		} catch (JavaModelException exception) {
			Console.err("retrieve for method '" + method.getElementName() + "' failed:", exception);
			return null;
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
		if (result.exact.dereferences > 0) {
			return result.exact;
		}
		if (result.anyVersion.dereferences > 0) {
			return result.anyVersion;
		}
		return result.anyArtifact;
	}
	
	private boolean isMethodWithReferenceTypeReturnValue(IJavaElement javaElement) {
		try {
			return Util.isMethodWithReferenceTypeReturnValue(javaElement);
		} catch (JavaModelException e) {
			e.printStackTrace();
			return false;
		}
	}
	
}
