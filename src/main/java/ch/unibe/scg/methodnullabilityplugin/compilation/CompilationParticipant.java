package ch.unibe.scg.methodnullabilityplugin.compilation;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.ReconcileContext;

public class CompilationParticipant extends org.eclipse.jdt.core.compiler.CompilationParticipant {

	public CompilationParticipant() {
	}
	
	@Override
	public boolean isActive(IJavaProject project) {
//		Console.msg("\n\nCompilationParticipant.isActive ================");// + project.getProject().getName());
//		return true;
		return false;
	}

	@Override
	public void buildStarting(BuildContext[] files, boolean isBatch) {
//		Console.msg("\n\nCompilationParticipant.BuildStarting ================");// + Arrays.asList(files));
	}
	
	@Override
	public void buildFinished(IJavaProject project) {
//		Console.msg("\n\nCompilationParticipant.BuildFinished ================");// + project.getProject().getName());
		
		//Console.msg("PrimaryElement: " + String.valueOf(project.getPrimaryElement()));
		
	}
	
	@Override
	public void reconcile(ReconcileContext context) {
//		Console.msg("\n\nCompilationParticipant.reconcile ================");// + context);
//		Console.msg("File: " + context.getWorkingCopy().getElementName());
	}
}
