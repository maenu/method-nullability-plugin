package ch.unibe.scg.methodnullabilityplugin.marker;

import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.ui.PlatformUI;

import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityAccessor;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityInfo;
import ch.unibe.scg.methodnullabilityplugin.util.ASTUtils;
import ch.unibe.scg.methodnullabilityplugin.util.Util;

class NullabilityMarkerVisitor extends ASTVisitor {

	private static double NULLABILITY_THRESHOLD = 0.2;
	
	private CompilationUnit compilationUnit;
	private IFile file;
	private MethodNullabilityAccessor methodNullabilityAccessor;
	
	public NullabilityMarkerVisitor(org.eclipse.jdt.internal.core.CompilationUnit compilationUnit) {
		Objects.requireNonNull(compilationUnit);
		
		this.file = ResourceUtil.getFile(compilationUnit);
		this.compilationUnit = (CompilationUnit) ASTUtils.getAST(compilationUnit);

		this.methodNullabilityAccessor = PlatformUI.getWorkbench().getService(MethodNullabilityAccessor.class);
	}
	
	
	@Override 
	public boolean visit(MethodInvocation node) { 
		Console.trace("block.MethodInvocation: '" + node.toString().replaceAll("\n", "") + "'\t\t | class: " + node.getClass());
		
		IMethodBinding methodBinding = node.resolveMethodBinding();
		IJavaElement javaElement = methodBinding.getJavaElement();
		if (isMethodWithReferenceTypeReturnValue(javaElement)) {
			Console.trace("method " + javaElement.getElementName() + " is nullability-checkable!");

			try {
				MethodNullabilityInfo info = methodNullabilityAccessor.retrieve((IMethod) javaElement);
				if (info.nullability() > NULLABILITY_THRESHOLD) {
					IMarker m = file.createMarker(Util.MARKER_ID);
					String msg = String.format("Method '" + javaElement.getElementName() + "' should be checked for null [%.2f]", info.nullability());
					m.setAttribute(IMarker.MESSAGE, msg);
					m.setAttribute(IMarker.LINE_NUMBER, compilationUnit.getLineNumber(node.getStartPosition()));
				}
			} catch (CoreException e) {
				Console.err(e);
				throw new RuntimeException(e);
			}
			
		} else {
			Console.trace("method " + javaElement.getElementName() + " is NOT nullability-checkable");
		}
		
		return super.visit(node); 
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
