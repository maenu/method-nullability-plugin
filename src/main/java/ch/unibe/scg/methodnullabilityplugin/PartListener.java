package ch.unibe.scg.methodnullabilityplugin;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

import ch.unibe.scg.methodnullabilityplugin.util.IPartListenerInstaller;

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
//		Display.getDefault().asyncExec(new Runnable() {
//
//			@Override
//			public void run() {
//				ArrayList<IEditorPart> eds = IPartListenerInstaller
//						.getCurrentEditors();
//				for (IEditorPart ed : eds) {
////					NullabilityMarker.add(ed);
////					EditorOnSaveListener.create(ed);
////					if (ed instanceof JavaEditor) {
////						addDocumentListener((JavaEditor) ed);
////					}
//				}
//			}
//		});
		
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
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		Console.msg("opening part: " + partRef.getPartName());
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

}
