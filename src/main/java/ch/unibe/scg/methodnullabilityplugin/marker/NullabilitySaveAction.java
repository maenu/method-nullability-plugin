package ch.unibe.scg.methodnullabilityplugin.marker;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.fix.TextEditFix;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.text.edits.TextEdit;

import ch.unibe.scg.methodnullabilityplugin.util.ASTUtils;

public class NullabilitySaveAction extends AbstractCleanUp {

	public static TextEdit lastEdit = null;
	
	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
    	ICompilationUnit compilationUnit= context.getCompilationUnit(); //context.getAST();
    	if (compilationUnit == null)
    		return null;

    	ASTNode ast = ASTUtils.getAST(compilationUnit);
//		ast.accept(methodVisitor((CompilationUnit) compilationUnit));
//		ast.accept(LogASTVisitor.create((CompilationUnit) compilationUnit));
//		ast.accept(new NullableVisitor((CompilationUnit) compilationUnit));
		
		TextEdit textEdit = NullableVisitor.execute((CompilationUnit) compilationUnit, ast);
		
		
		// TODO: at work Sunday 25.3.17: 
		// - Nullabiltiy check bei allen methoden einbauen, nicht nur bei Isa... chars()
		// - refactor source
		
//		Objects.requireNonNull(lastEdit);
//		TextEdit edit = lastEdit;

		TextEditFix fix = new TextEditFix(textEdit, compilationUnit, "HORRAY!!!!!!!!!!!!!!!!!!!!!!!");
		
//		lastEdit = null;
		
		return fix;
	}

//	private ASTVisitor methodVisitor(CompilationUnit compilationUnit) {
//		return new ASTVisitor() {
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
//		};
//	}

}
