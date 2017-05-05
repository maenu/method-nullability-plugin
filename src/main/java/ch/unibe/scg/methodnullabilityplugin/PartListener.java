package ch.unibe.scg.methodnullabilityplugin;

import java.util.ArrayList;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.IDocumentProvider;

import ch.unibe.scg.methodnullabilityplugin.marker.EditorOnSaveListener;
import ch.unibe.scg.methodnullabilityplugin.marker.NullabilityMarker;
import ch.unibe.scg.methodnullabilityplugin.util.IPartListenerInstaller;
import ch.unibe.scg.methodnullabilityplugin.util.Util;

public class PartListener implements IPartListener2 {

	public PartListener() {
		// install listener for editor events
		String errStr = IPartListenerInstaller.installOnWorkbench(this);
		if (errStr != null) {
			Console.err("install fail: " + errStr);
			throw new RuntimeException(errStr);
		}

		// install on currently open editors
		// run this in the UI thread?
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				ArrayList<IEditorPart> eds = IPartListenerInstaller
						.getCurrentEditors();
				for (IEditorPart ed : eds) {
					NullabilityMarker.add(ed);
					EditorOnSaveListener.create(ed);
//					if (ed instanceof JavaEditor) {
//						addDocumentListener((JavaEditor) ed);
//					}
				}
			}
		});
		
	}
	
	////////////////////////
	
	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		Console.msg("closing part: " + partRef.getPartName());

		// only testwise...
		IEditorPart editor = Util.getEditor(partRef);
		NullabilityMarker.delete(editor);
		EditorOnSaveListener.remove(editor);
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		Console.msg("opening part: " + partRef.getPartName());

		IEditorPart editor = Util.getEditor(partRef);
		
		if (editor != null && editor instanceof JavaEditor) {
			NullabilityMarker.add(editor);
			EditorOnSaveListener.create(editor);
//			addDocumentListener((JavaEditor) editor);
			
			// TODO: at work: live editing support..
//			IEditorInput editorInput = editor.getEditorInput();
//			IJavaElement javaElement = JavaUI.getEditorInputJavaElement(editorInput);
//			JavaCore.addElementChangedListener(new IElementChangedListener() {
//				
//				@Override
//				public void elementChanged(ElementChangedEvent event) {
//					Console.msg("elementChanged: " + event);
//					
//				}
//			});
//			
//			ITypeRoot input = SelectionConverter.getInput((JavaEditor) editor);
//			if (input instanceof CompilationUnit) {
//				CompilationUnit compilationUnit = (CompilationUnit) input;
//				ASTNode ast = ASTUtils.getAST(compilationUnit);
//			}
//			
		}
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {


	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {


	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {

	}

	private void addDocumentListener(JavaEditor ed) {
		IDocumentProvider documentProvider = ed.getDocumentProvider();
		IDocument document = documentProvider.getDocument(ed.getEditorInput());
		document.addDocumentListener(new IDocumentListener() {
				
				@Override
				public void documentChanged(DocumentEvent event) {
//					Console.msg("documentChanged: " + event.getText());
				}
				
				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
					Console.msg("documentAboutToBeChanged: " + event.getText());
				}
		});
	}
}
