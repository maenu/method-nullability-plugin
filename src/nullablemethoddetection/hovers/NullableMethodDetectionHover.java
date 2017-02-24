package nullablemethoddetection.hovers;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import nullablemethoddetection.database.Database;
import nullablemethoddetection.database.Match;
import nullablemethoddetection.database.Result;

public class NullableMethodDetectionHover extends AbstractJavaEditorTextHover {

	private Database database;

	public NullableMethodDetectionHover() throws SQLException, IOException {
		super();
		this.database = new Database();
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IJavaElement[] javaElements = this.getJavaElementsAt(textViewer, hoverRegion);
		if (javaElements == null) {
			return null;
		}
		if (javaElements.length != 1) {
			return null;
		}
		IJavaElement javaElement = javaElements[0];
		if (!(javaElement instanceof IMethod)) {
			return null;
		}
		IMethod method = (IMethod) javaElement;
		try {
			Result result = this.database.search(method);
			Match match = this.extractBestMatch(result);
			return this.format(match);
		} catch (JavaModelException exception) {
			throw new RuntimeException(exception);
		}
	}

	private Match extractBestMatch(Result result) {
		if (result.exact.invocations > 0) {
			return result.exact;
		}
		if (result.anyVersion.invocations > 0) {
			return result.anyVersion;
		}
		return result.anyArtifact;
	}

	private String format(Match match) {
		if (match.invocations > 0) {
			return String.format("<b>%.0f%% check the returned value (%d out of %d invocations)   </b>",
					(float) 100 * match.checks / match.invocations, match.checks, match.invocations);
		}
		return "no data";
	}

}