package nullablemethoddetection.hovers;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import nullablemethoddetection.Database;
import nullablemethoddetection.Result;

public class NullableMethodDetectionHover extends AbstractJavaEditorTextHover {

	private Database database;

	public NullableMethodDetectionHover() {
		super();
		this.database = new Database();
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IJavaElement[] javaElements = this.getJavaElementsAt(textViewer, hoverRegion);
		if (javaElements.length != 1) {
			return null;
		}
		IJavaElement javaElement = javaElements[0];
		if (!(javaElement instanceof IMethod)) {
			return null;
		}
		IMethod method = (IMethod) javaElement;
		try {
			List<Result> results = this.database.query(method);
			return "";
		} catch (JavaModelException exception) {
			throw new RuntimeException(exception);
		}
	}

}