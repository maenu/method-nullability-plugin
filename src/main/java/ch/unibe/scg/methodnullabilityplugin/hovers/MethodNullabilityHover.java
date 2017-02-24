package ch.unibe.scg.methodnullabilityplugin.hovers;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import ch.unibe.scg.methodnullabilityplugin.database.Database;
import ch.unibe.scg.methodnullabilityplugin.database.Match;
import ch.unibe.scg.methodnullabilityplugin.database.Result;

/**
 * The actual plugin, showing a popup with nullability information on hovering a
 * method.
 */
// TODO extending an internal class seems to be discouraged, do we just ignore
// these warnings? can we use a JDT extension point instead?
public class MethodNullabilityHover extends AbstractJavaEditorTextHover {

	/**
	 * Contains the method nullability data.
	 */
	private Database database;

	public MethodNullabilityHover() throws SQLException, IOException {
		super();
		this.database = new Database();
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		// get on what was hovered
		IJavaElement[] javaElements = this.getJavaElementsAt(textViewer, hoverRegion);
		if (javaElements == null) {
			// no elements hovered, return null, which means do not show a popup
			return null;
		}
		if (javaElements.length != 1) {
			// multiple elements hovered, don't know what to do, don't do
			// anything
			return null;
		}
		IJavaElement javaElement = javaElements[0];
		if (!(javaElement instanceof IMethod)) {
			// not a method, hence ignore
			return null;
		}
		IMethod method = (IMethod) javaElement;
		try {
			Result result = this.database.search(method);
			Match match = this.extractBestMatch(result);
			return this.format(match);
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

	/**
	 * 
	 * @param match
	 *            The match to format.
	 * @return A HTML string with the ratio and explicit checks and invocations.
	 */
	private String format(Match match) {
		if (match.invocations > 0) {
			return String.format("<b>%.0f%% check the returned value (%d out of %d invocations)   </b>",
					(float) 100 * match.checks / match.invocations, match.checks, match.invocations);
		}
		return "no data";
	}

}