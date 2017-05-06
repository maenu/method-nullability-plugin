package ch.unibe.scg.methodnullabilityplugin.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

import ch.unibe.scg.methodnullabilityplugin.Activator;
import ch.unibe.scg.methodnullabilityplugin.Console;

/**
 * Contains misc utility methods.
 */
public class Util {
	
	public static final String MARKER_ID = Activator.PLUGIN_ID + ".marker";

	public static boolean isMethodWithReferenceTypeReturnValue(IJavaElement javaElement) throws JavaModelException {
		if (javaElement instanceof IMethod) {
			IMethod m = (IMethod) javaElement;
			return !m.isConstructor() 
					&& !isPrimitive(m.getReturnType()) 
						&& !m.getReturnType().equals(Signature.SIG_VOID);
		}
		return false;
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
			return true;

		default:
			return false;
		}
	}
	
	public static void deleteMarkers(IFile file) {
		try {
			IMarker[] marks = file.findMarkers(MARKER_ID, false,
					IResource.DEPTH_ZERO);
			for (int i = 0; i < marks.length; i++) {
				Console.trace("deleting marker: " + marks[i] + " [attrs=" + marks[i].getAttributes().entrySet() + "]");
				marks[i].delete();
			}
		} catch (CoreException e) {
			Console.err(e);
		}
	}
	
	@SuppressWarnings("null")
	public static IEditorPart getEditor(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		IEditorPart editor = null;
		if (part != null && part instanceof IEditorPart) {
			editor = part.getAdapter(IEditorPart.class);
		}
		return editor;
	}
	
	public static boolean hasNullableAnnotation(ASTNode vds) {
		AtomicBoolean hasNullableAnnotationAlready = new AtomicBoolean(false);
		vds.accept(new ASTVisitor() {
			@Override
			public boolean visit(MarkerAnnotation node) {
				if (node.getTypeName().getFullyQualifiedName().equals(Nullable.class.getName())) {
					hasNullableAnnotationAlready.set(true);
					return false;
				}
				return true;
			}
		});
		return hasNullableAnnotationAlready.get();
	}
}
