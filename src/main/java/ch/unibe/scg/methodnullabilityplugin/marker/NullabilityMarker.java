package ch.unibe.scg.methodnullabilityplugin.marker;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.IEditorPart;

import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.util.ASTUtils;
import ch.unibe.scg.methodnullabilityplugin.util.Util;

public class NullabilityMarker {

	private NullabilityMarker() {}
	
	public static void add(IEditorPart ed) {
		if (ed instanceof JavaEditor) {
			//JavaBuilder.DEBUG  = true;
			ITypeRoot input = SelectionConverter.getInput((JavaEditor) ed);
			if (input instanceof CompilationUnit) {
				CompilationUnit compilationUnit = (CompilationUnit) input;
				ASTNode ast = ASTUtils.getAST(compilationUnit);
				ast.accept(methodVisitor(compilationUnit));
			}
		}
	}
	
	public static void delete(IEditorPart editor) {
		if (editor instanceof JavaEditor) {
			ITypeRoot input = SelectionConverter.getInput((JavaEditor) editor);
			if (input instanceof ICompilationUnit) {
				ICompilationUnit c = (ICompilationUnit) input;
				Util.deleteMarkers(ResourceUtil.getFile(c));
			}
		}
	}
	
	public static void update(IEditorPart editor) {
		delete(editor);
		add(editor);
	}
	
	private static ASTVisitor methodVisitor(CompilationUnit compilationUnit) {
		return new ASTVisitor() {

//			@Override
//			public boolean visit(Assignment node) {
//				Console.msg("2.Assignment: " + node);
//				return super.visit(node);
//			}
//			
//			// TODO: at work: filter variable assignments only  for nullability info!
//			@Override
//			public void endVisit(VariableDeclarationStatement node) {
//				Console.msg("2.VariableDeclarationStatement: " + node);
////				ASTRewritingStatementsTest.execute(compilationUnit.getJavaProject());
//				Console.msg("LogASTVisitor -------->");
//				node.accept(LogASTVisitor.create(compilationUnit));
//				Console.msg("LogASTVisitor <-------");
//				super.endVisit(node);
//			}
			
			
//			@Override
//			public boolean visit(ExpressionStatement node) {
//				Console.msg("2.ExpressionStatement: " + node);
//				return super.visit(node);
//			}
			
		    public boolean visit(MethodDeclaration method) {
		    	Console.trace("MethodDeclaration: '" + method.toString().replaceAll("\n", "") + "'\t\t | class: " + method.getClass());
		        method.getBody().accept(new NullabilityMarkerVisitor(compilationUnit));
		        return true;
		    }

		};
	}
	
}
