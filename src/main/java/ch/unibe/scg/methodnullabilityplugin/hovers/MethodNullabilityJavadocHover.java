package ch.unibe.scg.methodnullabilityplugin.hovers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import ch.unibe.scg.methodnullabilityplugin.database.Database;
import ch.unibe.scg.methodnullabilityplugin.database.Match;
import ch.unibe.scg.methodnullabilityplugin.database.Result;

/**
 * Extension of {@link JavadocHover}, adding a line of nullability information.
 */
public class MethodNullabilityJavadocHover extends JavadocHover {

	private static final String BACKGROUND_COLOR = "<span style=\"background-color: rgb(217,226,243)\">";
	private static final String NULLABILITY_INFO = BACKGROUND_COLOR + "<b>%.0f%%</b> check the returned value (<b>%d</b> out of <b>%d</b> invocations)</span>";
	private static final String NULLABILITY_NOT_AVAILABLE = BACKGROUND_COLOR + "nullability not available</span>";
	
	private static final String RETURNS_REGEX = "(<dt>Returns:</dt><dd>)(.*?)(</dd>)"; // append to javadoc 'Returns' doc
	private static final Pattern PATTERN = Pattern.compile(RETURNS_REGEX, Pattern.DOTALL);

	private static final String FALLBACK_REGEX = "(.*)(</body></html>)"; // insert at end of document a dummy 'Returns' doc
	private static final Pattern PATTERN_FALLBACK = Pattern.compile(FALLBACK_REGEX, Pattern.DOTALL);
	
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
				Matcher matcherReturn = PATTERN.matcher(input.getHtml());
				String htmlWithNullabilityInfo;
				if (matcherReturn.find()) {
					String replacementReturn = "$1$2<br>" + nullabilityInfo + "$3";
					htmlWithNullabilityInfo = matcherReturn.replaceAll(replacementReturn);
				} else {
					Matcher matcherFallback = PATTERN_FALLBACK.matcher(input.getHtml());
					String replacementFallback = "$1<dt>Returns:</dt><dd>not specified<br>" + nullabilityInfo + "</dd>$2";
					htmlWithNullabilityInfo = matcherFallback.replaceAll(replacementFallback);
				}
				
//				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//				System.out.println(htmlWithNullabilityInfo);
//				System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
				
				JavadocBrowserInformationControlInput input2 = 
					new JavadocBrowserInformationControlInput(
							(JavadocBrowserInformationControlInput) input.getPrevious(), 
							input.getElement(), 
							htmlWithNullabilityInfo,
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
		try {
			if (!isMethodWithReferenceTypeReturnValue(javaElement)) {
				// not a method (or void/primitive return type), hence ignore
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

	private boolean isMethodWithReferenceTypeReturnValue(IJavaElement javaElement) throws JavaModelException {
		if (javaElement instanceof IMethod) {
			IMethod m = (IMethod) javaElement;
			return !m.isConstructor() 
					&& !isPrimitive(m.getReturnType()) 
						&& !m.getReturnType().equals(Signature.SIG_VOID);
		}
		return false;
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
			return String.format(NULLABILITY_INFO, (float) 100 * match.checks / match.invocations, match.checks, match.invocations);
		}
		return NULLABILITY_NOT_AVAILABLE;
	}
	
	private boolean isPrimitive(String type) {
		switch (type) {
		case Signature.SIG_BOOLEAN:
		case Signature.SIG_BYTE:
		case Signature.SIG_CHAR:
		case Signature.SIG_DOUBLE:
		case Signature.SIG_FLOAT:
		case Signature.SIG_INT:
		case Signature.SIG_LONG:
		case Signature.SIG_SHORT:
			return true;

		default:
			return false;
		}
	}
}