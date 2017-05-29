package ch.unibe.scg.methodnullabilityplugin;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

import ch.unibe.scg.methodnullabilityplugin.util.IPartListenerInstaller;

/**
 * Available for future extensions.
 */
public class PartListener implements IPartListener2 {

	public PartListener() {
		// install listener for editor events
		String errStr = IPartListenerInstaller.installOnWorkbench(this);
		if (errStr != null) {
			Console.err("install fail: " + errStr);
			throw new RuntimeException(errStr);
		}
		
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
