package ch.unibe.scg.methodnullabilityplugin;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ch.unibe.scg.methodnullabilityplugin.database.Database;
import ch.unibe.scg.methodnullabilityplugin.database.Nullability;

public class MethodNullabilityJavadocHover extends JavadocHover {

	private static final String TEMPLATE_CONTAINER = "<span style=\"color: #3F5FBF;\">%s</span>";
	private static final String TEMPLATE_RETURN = "<b>%.0f%%</b> check for null (<b>%d</b> out of <b>%d</b> times)";
	private static final String TEMPLATE_PARAMETER = "<b>%.0f%%</b> pass null (<b>%d</b> out of <b>%d</b> times)";

	private Database database;

	public MethodNullabilityJavadocHover() throws SQLException {
		super();
		this.database = new Database(MethodNullabilityPlugin.getDefault().getDatabaseUrl());
	}

	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		JavadocBrowserInformationControlInput input = (JavadocBrowserInformationControlInput) super.getHoverInfo2(
				textViewer, hoverRegion);
		// ensure a single method is hovered
		IJavaElement[] javaElements = this.getJavaElementsAt(textViewer, hoverRegion);
		if (javaElements == null) {
			return input;
		}
		if (javaElements.length != 1) {
			return input;
		}
		if (!(javaElements[0] instanceof IMethod)) {
			return input;
		}
		IMethod method = (IMethod) javaElements[0];
		Map<Integer, Nullability> nullabilities = Collections.emptyMap();
		try {
			nullabilities = database.search(method);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return input;
		}
		if (nullabilities.isEmpty()) {
			return input;
		}
		// remove primitive types
		String returnType;
		try {
			returnType = method.getReturnType();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return input;
		}
		String[] parameterTypes = method.getParameterTypes();
		if (isPrimitive(returnType)) {
			nullabilities.remove(-1);
		}
		for (int i = 0; i < parameterTypes.length; i++) {
			if (isPrimitive(parameterTypes[i])) {
				nullabilities.remove(i);
			}
		}
		if (nullabilities.isEmpty()) {
			return input;
		}
		// massage documentation
		Document document = Jsoup.parse(input.getHtml());
		massage(document, parameterTypes, nullabilities);
		input = new JavadocBrowserInformationControlInput((JavadocBrowserInformationControlInput) input.getPrevious(),
				input.getElement(), document.outerHtml(), input.getLeadingImageWidth());
		return input;
	}

	private void massage(Document document, String[] parameterTypes, Map<Integer, Nullability> nullabilities) {
		document.selectFirst("html").attr("style", "width = 100px !important;");
		document.selectFirst("body").attr("style", "width = 100px !important;");
		if (nullabilities.keySet().stream().anyMatch(i -> i > -1)) {
			massageParameters(document, parameterTypes, nullabilities);
		}
		if (nullabilities.containsKey(-1)) {
			massageReturn(document, nullabilities.get(-1));
		}
	}

	private void massageParameters(Document document, String[] parameterTypes,
			Map<Integer, Nullability> nullabilities) {
		Element dt = document.selectFirst("dt:matches(Parameters:)");
		if (dt == null) {
			dt = document.createElement("dt");
			dt.text("Parameters:");
			document.selectFirst("body").appendChild(dt);
		}
		Element insertion = dt;
		Element dd = insertion.nextElementSibling();
		for (int i = 0; i < parameterTypes.length; i++) {
			if (dd == null || !dd.is("dd")) {
				insertion.after("<dd><code></code> - </dd>");
				dd = insertion.nextElementSibling();
			}
			if (nullabilities.containsKey(i)) {
				Nullability nullability = nullabilities.get(i);
				String message = String.format(TEMPLATE_CONTAINER, message(TEMPLATE_PARAMETER, nullability));
				dd.append(message);
			}
			insertion = dd;
			dd = insertion.nextElementSibling();
		}
	}

	private void massageReturn(Document document, Nullability nullability) {
		String message = String.format(TEMPLATE_CONTAINER, message(TEMPLATE_RETURN, nullability));
		Element dd = document.selectFirst("dt:matches(Returns:) + dd");
		if (dd == null) {
			document.selectFirst("body").append("<dt>Returns:</dt><dd>" + message + "</dd>");
		} else {
			dd.append(message);
		}
	}

	private String message(String template, Nullability nullability) {
		return String.format(template, 100 * nullability.nullability, nullability.evidence, nullability.support);
	}

	public static boolean isPrimitive(String type) {
		switch (type) {
		case Signature.SIG_BOOLEAN:
		case Signature.SIG_BYTE:
		case Signature.SIG_CHAR:
		case Signature.SIG_DOUBLE:
		case Signature.SIG_FLOAT:
		case Signature.SIG_INT:
		case Signature.SIG_LONG:
		case Signature.SIG_SHORT:
		case Signature.SIG_VOID:
			return true;
		default:
			return false;
		}
	}

}