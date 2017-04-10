package ch.unibe.scg.methodnullabilityplugin.marker;

import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.ui.PlatformUI;

import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityAccessor;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityInfo;
import ch.unibe.scg.methodnullabilityplugin.util.ASTUtils;
import ch.unibe.scg.methodnullabilityplugin.util.Util;

class NullableVisitor extends ASTVisitor {

	private static double NULLABILITY_THRESHOLD = 0.2;
	
	private CompilationUnit compilationUnit;
	private IFile file;
	private MethodNullabilityAccessor methodNullabilityAccessor;
	
	@SuppressWarnings("null")
	public NullableVisitor(org.eclipse.jdt.internal.core.CompilationUnit compilationUnit) {
		Objects.requireNonNull(compilationUnit);
		
		this.file = ResourceUtil.getFile(compilationUnit);
		this.compilationUnit = (CompilationUnit) ASTUtils.getAST(compilationUnit);

		this.methodNullabilityAccessor = PlatformUI.getWorkbench().getService(MethodNullabilityAccessor.class);
	}
	
	@Override
	public boolean visit(Assignment node) {
		Console.msg("nullable.Assignment: '" + node.toString().replaceAll("\n", "") + "'\t\t | class: " + node.getClass());
		Expression rightHandSide = node.getRightHandSide();
		
		if (rightHandSide.getNodeType() == ASTNode.METHOD_INVOCATION) {
			Console.msg("method invocation found: " + rightHandSide);
			if (rightHandSide instanceof MethodInvocation) {
				MethodInvocation mi = (MethodInvocation) rightHandSide;
				createNullabilityMarker(node, mi);
				//addNullableAnnotation(node, mi);
				
			} else {
				Console.err("unexpected type: " + rightHandSide);
				throw new IllegalArgumentException(rightHandSide.toString());
			}
		}
		
//		IJavaElement javaElement = methodBinding.getJavaElement();
//		if (isMethodWithReferenceTypeReturnValue(javaElement)) {
//			
//		}
		
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		Console.msg("nullable.VariableDeclarationStatement: '" + node.toString().replaceAll("\n", "") + "'\t\t | class: " + node.getClass());
		
		List fragments = node.fragments();
		VariableDeclarationFragment object = (VariableDeclarationFragment) fragments.get(0);
		Expression initializer = object.getInitializer();
		if (initializer != null && initializer.getNodeType() == ASTNode.METHOD_INVOCATION) {
			Console.msg("VDS: method invocation found: " + initializer);
			if (initializer instanceof MethodInvocation) {
				MethodInvocation mi = (MethodInvocation) initializer;
				createNullabilityMarker(node, mi);
				addNullableAnnotation(node, mi);
				
			} else {
				Console.err("unexpected type: " + initializer);
				throw new IllegalArgumentException(initializer.toString());
			}
		}
		
		return super.visit(node);
	}

	private void addNullableAnnotation(VariableDeclarationStatement node, MethodInvocation mi) {
		// TODO: at work Sunday night 09.04.17...
		// TODO: test mit vgl. LogASTVisitor nur methoden IntStream chars=s.chars() und codePoints()
	}

//	@Override 
//	public boolean visit(MethodInvocation node) { 
//		Console.trace("nullable.MethodInvocation: '" + node.toString().replaceAll("\n", "") + "'\t\t | class: " + node.getClass());
		
//		IMethodBinding methodBinding = node.resolveMethodBinding();
//		IJavaElement javaElement = methodBinding.getJavaElement();
//		if (isMethodWithReferenceTypeReturnValue(javaElement)) {
//			Console.trace("method " + javaElement.getElementName() + " is nullability-checkable!");
//			
//			try {
//				MethodNullabilityInfo info = methodNullabilityAccessor.retrieve((IMethod) javaElement);
//				if (info.nullability() > NULLABILITY_THRESHOLD) {
//					IMarker m = file.createMarker(Util.MARKER_ID);
//					String msg = String.format("Method '" + javaElement.getElementName() + "' should be checked for null [%.2f]", info.nullability());
//					m.setAttribute(IMarker.MESSAGE, msg);
//					m.setAttribute(IMarker.LINE_NUMBER, compilationUnit.getLineNumber(node.getStartPosition()));
//				}
//			} catch (CoreException e) {
//				Console.err(e);
//				throw new RuntimeException(e);
//			}
//			
//		} else {
//			if (javaElement != null) {
//				Console.trace("method " + javaElement.getElementName() + " is NOT nullability-checkable");
//			}
//		}
		
//		return super.visit(node); 
//    } 

	
	private void createNullabilityMarker(ASTNode node, MethodInvocation mi) {
		IMethodBinding methodBinding = mi.resolveMethodBinding();
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
			if (javaElement != null) {
				Console.trace("method " + javaElement.getElementName() + " is NOT nullability-checkable");
			}
		}
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
