package ch.unibe.scg.methodnullabilityplugin;

import java.util.ArrayList;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

import ch.unibe.scg.methodnullabilityplugin.marker.NullabilityMarkers;
import ch.unibe.scg.methodnullabilityplugin.marker.OnSaveNullabilityMarkerUpdater;
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
					NullabilityMarkers.add(ed);
					OnSaveNullabilityMarkerUpdater.create(ed);
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
		NullabilityMarkers.delete(editor);
		OnSaveNullabilityMarkerUpdater.remove(editor);
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		Console.msg("opening part: " + partRef.getPartName());

		IEditorPart editor = Util.getEditor(partRef);
		
		if (editor != null && editor instanceof JavaEditor) {
			NullabilityMarkers.add(editor);
			OnSaveNullabilityMarkerUpdater.create(editor);
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
}
