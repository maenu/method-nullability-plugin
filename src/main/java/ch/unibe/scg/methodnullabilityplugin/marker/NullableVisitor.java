package ch.unibe.scg.methodnullabilityplugin.marker;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PlatformUI;

import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityAccessor;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityInfo;
import ch.unibe.scg.methodnullabilityplugin.util.ASTUtils;
import ch.unibe.scg.methodnullabilityplugin.util.Util;

class NullableVisitor extends ASTVisitor {

	private static double NULLABILITY_THRESHOLD = 0.2;
	
	private final CompilationUnit domCompilationUnit;
	private final org.eclipse.jdt.internal.core.CompilationUnit coreCompilationUnit;
	private final IFile file;
	private final MethodNullabilityAccessor methodNullabilityAccessor;
	
	private TextEdit textEdits = null;
	
	private Set<VariableDeclarationStatement> vdsToAnnotate;
	private Set<Assignment> assignmentsToAnnotate;
	
	@SuppressWarnings("null")
	private NullableVisitor(org.eclipse.jdt.internal.core.CompilationUnit compilationUnit) {
		Objects.requireNonNull(compilationUnit);
		
		this.file = ResourceUtil.getFile(compilationUnit);
		this.coreCompilationUnit = compilationUnit;
		this.domCompilationUnit = (CompilationUnit) ASTUtils.getAST(compilationUnit);

		this.methodNullabilityAccessor = PlatformUI.getWorkbench().getService(MethodNullabilityAccessor.class);
		
		this.vdsToAnnotate = new HashSet<>();
		this.assignmentsToAnnotate = new HashSet<>();
	}
	
	public static TextEdit execute(org.eclipse.jdt.internal.core.CompilationUnit compilationUnit, ASTNode astRoot) {
		NullableVisitor visitor = new NullableVisitor(compilationUnit);
		
		astRoot.accept(visitor);
		visitor.generateAnnotations(astRoot.getAST());
		
		return visitor.textEdits;
	}
	
	private void generateAnnotations(AST ast) {
		Console.msg("generateAnnotations() -->");
		try {
			
			// test if branch ok.
			ASTRewrite rewrite = ASTRewrite.create(ast);
			
			for (VariableDeclarationStatement vds : vdsToAnnotate) {
				ListRewrite listRewrite = rewrite.getListRewrite(vds, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
				MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
				markerAnnotation.setTypeName(ast.newName(org.eclipse.jdt.annotation.Nullable.class.getName()));
				listRewrite.insertAt(markerAnnotation, 0, null);
			}
			

//			ClassMap cm = new ClassMap();
//			Object object = cm.get(new Object());
//			object.toString();
			
			// TODO at work: 25.04: find respective VariableDeclarationStatement and add nullable annotation
			// either local or member of class!!!
//			for (Assignment ass : assignmentsToAnnotate) {
//				
//
//				Expression placeholder= (Expression) rewrite.createCopyTarget(ass);
//
//				Assignment newExpression= ast.newAssignment();
//				newExpression.setLeftHandSide(ass.getLeftHandSide());
//				newExpression.setRightHandSide(ass.getRightHandSide());
//				newExpression.setOperator(Assignment.Operator.ASSIGN);
//
//				rewrite.replace(ass, newExpression, null);
//				
//				
//				
//				ListRewrite listRewrite = rewrite.getListRewrite(ass, null);
//				MarkerAnnotation markerAnnotation = ast.newMarkerAnnotation();
//				markerAnnotation.setTypeName(ast.newName(Nullable.class.getName()));
//				listRewrite.insertAt(markerAnnotation, 0, null);
//			}
			
			Console.msg("added " + vdsToAnnotate.size() + " annotations to source file.");
			
			String source = coreCompilationUnit.getSource();
			Document document= new Document(source);
			
			this.textEdits = rewrite.rewriteAST(document, coreCompilationUnit.getJavaProject().getOptions(true));
		
		} catch (JavaModelException jme) {
			Console.err(jme);
			throw new IllegalArgumentException(jme);
		}
		Console.msg("generateAnnotations() <--");
	}

	@Override
	public boolean visit(Assignment node) {
		Console.msg("NullableVisitor.Assignment: '" + node.toString().replaceAll("\n", ""));
		Expression rightHandSide = node.getRightHandSide();
		
		if (rightHandSide.getNodeType() == ASTNode.METHOD_INVOCATION) {
			Console.msg("Ass: method invocation found: " + rightHandSide);
			if (rightHandSide instanceof MethodInvocation) {
				MethodInvocation mi = (MethodInvocation) rightHandSide;
				IMethodBinding methodBinding = mi.resolveMethodBinding();
				IJavaElement javaElement = methodBinding.getJavaElement();
				MethodNullabilityInfo info = retrieveNullabilityInfo(javaElement);
				if (info.exists() && info.nullability() > NULLABILITY_THRESHOLD) {
					if (!Util.hasNullableAnnotation(node)) {
						Console.msg("Ass: add nullability info to: " + node.toString().replaceAll("\n", ""));
						addMarker(info, node, javaElement);
						collectForAnnotation(node);
					}
				}
				
			} else {
				Console.err("unexpected type: " + rightHandSide);
				throw new IllegalArgumentException(rightHandSide.toString());
			}
		}
		
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		Console.msg("NullableVisitor.VariableDeclarationStatement: '" + node.toString().replaceAll("\n", ""));
		
		List<?> fragments = node.fragments();
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
		if (fragment != null) {
			Expression initializer = fragment.getInitializer();
			if (initializer != null && initializer.getNodeType() == ASTNode.METHOD_INVOCATION) {
				Console.msg("VDS: method invocation found: " + initializer);
				if (initializer instanceof MethodInvocation) {
					MethodInvocation mi = (MethodInvocation) initializer;
					IMethodBinding methodBinding = mi.resolveMethodBinding();
					IJavaElement javaElement = methodBinding.getJavaElement();
					MethodNullabilityInfo info = retrieveNullabilityInfo(javaElement);
					if (info.exists() && info.nullability() > NULLABILITY_THRESHOLD) {
						if (!Util.hasNullableAnnotation(node)) {
							Console.msg("VDS: add nullability info to: " + node.toString().replaceAll("\n", ""));
							addMarker(info, node, javaElement);
							collectForAnnotation(node);
						}
					}
					
				} else {
					Console.err("unexpected type: " + initializer);
					throw new IllegalArgumentException(initializer.toString());
				}
			}
		}
		
		return super.visit(node);
	}

	private void collectForAnnotation(VariableDeclarationStatement node) {
		this.vdsToAnnotate.add(node);
	}
	
	private void collectForAnnotation(Assignment node) {
		this.assignmentsToAnnotate.add(node);
	}
	
//	private void generateNullableAnnotations(VariableDeclarationStatement node) {
//		try {
//			
//			VariableDeclarationStatement node2 = null;
//			
//			AST ast = node.getAST();
//			ASTRewrite rewrite= ASTRewrite.create(ast);
//			
//			ListRewrite listRewrite= rewrite.getListRewrite(node, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
//			MarkerAnnotation markerAnnotation= ast.newMarkerAnnotation();
//			markerAnnotation.setTypeName(ast.newName(Nullable.class.getName()));
//			listRewrite.insertAt(markerAnnotation, 0, null);
//			
//			ListRewrite listRewrite2= rewrite.getListRewrite(node2, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
//			MarkerAnnotation markerAnnotation2= ast.newMarkerAnnotation();
//			markerAnnotation.setTypeName(ast.newName(Nullable.class.getName()));
//			listRewrite2.insertAt(markerAnnotation, 0, null);
//			
//			String source = coreCompilationUnit.getSource();
//			Document document= new Document(source);
//			
//			TextEdit edits = rewrite.rewriteAST(document, coreCompilationUnit.getJavaProject().getOptions(true));
//			
//		} catch (JavaModelException jme) {
//			Console.err(jme);
//			throw new IllegalArgumentException("problem with node=" + node, jme);
//		}
//	}

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

	
	private void addMarker(MethodNullabilityInfo info, ASTNode node, IJavaElement javaElement) {
		try {
			IMarker m = file.createMarker(Util.MARKER_ID);
			String msg = String.format("Method '" + javaElement.getElementName() + "' should be checked for null [%.2f]", info.nullability());
			m.setAttribute(IMarker.MESSAGE, msg);
			m.setAttribute(IMarker.LINE_NUMBER, domCompilationUnit.getLineNumber(node.getStartPosition()));
		} catch (CoreException e) {
			Console.err(e);
			throw new IllegalArgumentException("problem with node=" + node, e);
		}
	}
	
	private MethodNullabilityInfo retrieveNullabilityInfo(IJavaElement javaElement) {
		return methodNullabilityAccessor.retrieve(javaElement);
	}
}
