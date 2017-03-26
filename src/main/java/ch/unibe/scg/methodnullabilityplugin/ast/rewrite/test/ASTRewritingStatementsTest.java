/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ch.unibe.scg.methodnullabilityplugin.ast.rewrite.test;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PlatformUI;

public class ASTRewritingStatementsTest {

	protected IPackageFragmentRoot sourceFolder;
	
	protected int apiLevel = AST.JLS8;
	
	public static void execute(IJavaProject jp) {
		ASTRewritingStatementsTest t = new ASTRewritingStatementsTest();
		try {
			t.setUp(jp);
			t.testBug417923e_since_8();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void setUp(IJavaProject jp) throws Exception {	
		this.sourceFolder = getPackageFragmentRoot(jp, "method-nullability-plugin", "src/main/java");
	}

	public void testBug417923e_since_8() throws Exception {
		IPackageFragment pack1= this.sourceFolder.createPackageFragment("test1", true, null);
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void foo() {\n");
		buf.append("    	String s = \"sdf\";\n");
		buf.append("    	IntStream chars = s.chars();\n");
		buf.append("    }\n");
		buf.append("}\n");
		
		ICompilationUnit cu = pack1.getCompilationUnit("X.java");
		if (cu != null) {
			cu.delete(true, null);
		}
		cu = pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		AST ast = astRoot.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDecl= typeDecl.getMethods()[0];
		Block block= methodDecl.getBody();
		List<?> statements= block.statements();

		for (int i = 0; i < statements.size(); ++i) {
			VariableDeclarationStatement statement = (VariableDeclarationStatement) statements.get(i);
			List<?> fragments = statement.fragments();
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
			Expression e = fragment.getInitializer();
			if (e.getNodeType() == Expression.STRING_LITERAL) {
				StringLiteral creation = (StringLiteral) fragment.getInitializer();
			} else if (e.getNodeType() == Expression.METHOD_INVOCATION) {
				MethodInvocation mi = (MethodInvocation) fragment.getInitializer();
				// TODO: get nullability..
				
				Type type = statement.getType();
				
				ListRewrite listRewrite= rewrite.getListRewrite(statement, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
				MarkerAnnotation markerAnnotation= ast.newMarkerAnnotation();
				markerAnnotation.setTypeName(ast.newSimpleName("Nullable"));
				listRewrite.insertAt(markerAnnotation, 0, null);
			}
			
			
			
//			Dimension dim = (Dimension) arrayType.dimensions().get(0);
//			ListRewrite listRewrite= rewrite.getListRewrite(dim, Dimension.ANNOTATIONS_PROPERTY);
//			MarkerAnnotation markerAnnotation= ast.newMarkerAnnotation();
//			markerAnnotation.setTypeName(ast.newSimpleName("Nullable"));
//			listRewrite.insertAt(markerAnnotation, 0, null);
		}
		
		String preview= evaluateRewrite(cu, rewrite);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void foo() {\n");
		buf.append("    	String s = \"sdf\";\n");
		buf.append("    	@Nullable\n");
		buf.append("		IntStream chars = s.chars();\n");
		buf.append("    }\n");
		buf.append("}\n");
		
		if (!preview.equals(buf.toString())) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Returns the specified package fragment root in the given project, or
	 * <code>null</code> if it does not exist.
	 * If relative, the rootPath must be specified as a project relative path.
	 * The empty path refers to the package fragment root that is the project
	 * folder itself.
	 * If absolute, the rootPath refers to either an external jar, or a resource
	 * internal to the workspace
	 */
	public IPackageFragmentRoot getPackageFragmentRoot(
		IJavaProject project,
			String projectName,
		String rootPath)
		throws JavaModelException {

		if (project == null) {
			project = getJavaProject(projectName);
			if (project == null) {
				return null;
			}
		}
		IPath path = new Path(rootPath);
		if (path.isAbsolute()) {
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IResource resource = workspaceRoot.findMember(path);
			IPackageFragmentRoot root;
			if (resource == null) {
				// external jar
				root = project.getPackageFragmentRoot(rootPath);
			} else {
				// resource in the workspace
				root = project.getPackageFragmentRoot(resource);
			}
			return root;
		} else {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			if (roots == null || roots.length == 0) {
				return null;
			}
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				if (!root.isExternal()
					&& root.getUnderlyingResource().getProjectRelativePath().equals(path)) {
					return root;
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the Java Project with the given name in this test
	 * suite's model. This is a convenience method.
	 */
	public IJavaProject getJavaProject(String name) {
		IProject project = getProject(name);
		return JavaCore.create(project);
	}
	
	protected IProject getProject(String project) {
		return getWorkspaceRoot().getProject(project);
	}
	public IWorkspaceRoot getWorkspaceRoot() {
		return getWorkspace().getRoot();
	}
	/**
	 * Returns the IWorkspace this test suite is running on.
	 */
	public IWorkspace getWorkspace() {
		// TODO at work: "Workspace is closed"
		return ResourcesPlugin.getWorkspace();
	}
	
	protected String evaluateRewrite(ICompilationUnit cu, ASTRewrite rewrite) throws Exception { 
	  Document document1= new Document(cu.getSource()); 
	  TextEdit res= rewrite.rewriteAST(document1, cu.getJavaProject().getOptions(true)); 
	  res.apply(document1); 
	  String content1= document1.get(); 
	 
	  Document document2= new Document(cu.getSource()); 
	  TextEdit res2= rewrite.rewriteAST(); 
	  res2.apply(document2); 
	  String content2= document2.get(); 
	 
	  if (!content1.equals(content2)) {
		  throw new IllegalArgumentException();
	  }
	 
	  return content1; 
	} 
	
	protected CompilationUnit createAST(ICompilationUnit cu) {
		return createAST(this.apiLevel, cu, false, false);
	}
	protected CompilationUnit createAST(ICompilationUnit cu, boolean statementsRecovery) {
		return createAST(this.apiLevel, cu, false, statementsRecovery);
	}
	protected CompilationUnit createAST(ICompilationUnit cu, boolean resolveBindings, boolean statementsRecovery) {
		return createAST(this.apiLevel, cu, resolveBindings, statementsRecovery);
	}

	protected CompilationUnit createAST(int JLSLevel, ICompilationUnit cu, boolean resolveBindings, boolean statementsRecovery) {
		ASTParser parser= ASTParser.newParser(JLSLevel);
		parser.setSource(cu);
		parser.setResolveBindings(resolveBindings);
		parser.setStatementsRecovery(statementsRecovery);
		return (CompilationUnit) parser.createAST(null);
	}
}
