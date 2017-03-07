package ch.unibe.scg.methodnullabilityplugin.hovers;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import ch.unibe.scg.methodnullabilityplugin.database.Database;
import ch.unibe.scg.methodnullabilityplugin.database.Match;
import ch.unibe.scg.methodnullabilityplugin.database.Result;

/**
 * Extension of {@link JavadocHover}, adding a line of nullability information.
 */
public class MethodNullabilityJavadocHover extends JavadocHover {

	private static final String NULLABILITY_NOT_AVAILABLE = "<dl><dt>Nullability:</dt><dd>  not available</dd></dl>";
	/**
	 * Contains the method nullability data.
	 */
	private Database database;

	public MethodNullabilityJavadocHover() throws SQLException, IOException {
		super();
		this.database = new Database();
	}


	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		Object obj = super.getHoverInfo2(textViewer, hoverRegion);
		if (obj instanceof JavadocBrowserInformationControlInput) {
			String nullabilityInfo = extractNullabilityInfo(textViewer, hoverRegion);
			if (nullabilityInfo != null && !nullabilityInfo.isEmpty()) {
				JavadocBrowserInformationControlInput input = (JavadocBrowserInformationControlInput) obj;
				String htmlWithoutEpilog = input.getHtml().replace("</body></html>", ""); //$NON-NLS-1$ //$NON-NLS-2$
				StringBuffer bufHtmlWithoutEpilog = new StringBuffer(htmlWithoutEpilog);
				HTMLPrinter.addParagraph(bufHtmlWithoutEpilog, nullabilityInfo);
				HTMLPrinter.addPageEpilog(bufHtmlWithoutEpilog);
				JavadocBrowserInformationControlInput input2 = 
					new JavadocBrowserInformationControlInput(
							(JavadocBrowserInformationControlInput) input.getPrevious(), 
							input.getElement(), 
							bufHtmlWithoutEpilog.toString(), 
							input.getLeadingImageWidth());
				
				return input2;
			} else {
				return obj;
			}
		} else {
			System.out.println("unexpected hover info type, cannot add nullability info... [hoverInfo: " + obj + "]");
			return obj;
		}
	}
	
	private String extractNullabilityInfo(ITextViewer textViewer, IRegion hoverRegion) {
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
		//System.out.println(javaElement.getClass());
		try {
			if (!(javaElement instanceof IMethod) || ((IMethod) javaElement).isConstructor()) {
				// not a method, hence ignore
				return null;
			}
		} catch (JavaModelException jme) {
			return NULLABILITY_NOT_AVAILABLE;
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
			return String.format("<dl><dt>Nullability:</dt><dd>  %.0f%% check the returned value (%d out of %d invocations)</dd></dl>",
					(float) 100 * match.checks / match.invocations, match.checks, match.invocations);
		}
		return NULLABILITY_NOT_AVAILABLE;
	}

}