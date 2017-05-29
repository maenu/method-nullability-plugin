package ch.unibe.scg.methodnullabilityplugin.hovers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.PlatformUI;

import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityAccessor;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityInfo;
import ch.unibe.scg.methodnullabilityplugin.util.Util;

/**
 * Extension of {@link JavadocHover}, adding a line of nullability information to the javadoc of a method in the
 * 'returns' paragraph.
 * 
 * @see JavadocHover
 * @see MethodNullabilityAccessor
 */
public class MethodNullabilityJavadocHover extends JavadocHover {

	private static final String BACKGROUND_COLOR = "<span style=\"background-color: rgb(217,226,243)\">";
	private static final String NULLABILITY_INFO = BACKGROUND_COLOR + "<b>%.0f%%</b> check the returned value (<b>%d</b> out of <b>%d</b> invocations)</span>";
	private static final String NULLABILITY_NOT_AVAILABLE = BACKGROUND_COLOR + "nullability not available</span>";
	
	private static final String RETURNS_REGEX = "(<dt>Returns:</dt><dd>)(.*?)(</dd>)"; // append to javadoc 'Returns' doc
	private static final Pattern PATTERN = Pattern.compile(RETURNS_REGEX, Pattern.DOTALL);

	private static final String FALLBACK_REGEX = "(.*)(</body></html>)"; // insert at end of document a dummy 'Returns' doc
	private static final Pattern PATTERN_FALLBACK = Pattern.compile(FALLBACK_REGEX, Pattern.DOTALL);
	
	private MethodNullabilityAccessor methodNullabilityAccessor;
	
	@SuppressWarnings("null")
	public MethodNullabilityJavadocHover() {
		super();
		this.methodNullabilityAccessor = PlatformUI.getWorkbench().getService(
				MethodNullabilityAccessor.class);
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
			//Console.msg("unexpected hover info type, cannot add nullability info... [hoverInfo: " + obj + "]");
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
		
		return retrieveNullabilityInfo(javaElements[0]);
	}

	private String retrieveNullabilityInfo(IJavaElement javaElement) {
		try {
			if (!Util.isMethodWithReferenceTypeReturnValue(javaElement)) {
				// not a method (or void/primitive return type), hence ignore
				return null;
			}
		} catch (JavaModelException jme) {
			return NULLABILITY_NOT_AVAILABLE;
		}
		
		MethodNullabilityInfo info = methodNullabilityAccessor.retrieve((IMethod) javaElement);
		return this.format(info);
	}

	/**
	 * 
	 * @param match
	 *            The match to format.
	 * @return A HTML string with the ratio and explicit checks and invocations.
	 */
	private String format(MethodNullabilityInfo match) {
		if (match != null && match.hasInvocations()) {
			return String.format(NULLABILITY_INFO, 100 * match.nullability(), match.getChecks(), match.getInvocations());
		}
		return NULLABILITY_NOT_AVAILABLE;
	}
	
}