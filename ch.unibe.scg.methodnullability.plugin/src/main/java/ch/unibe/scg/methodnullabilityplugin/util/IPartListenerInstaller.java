package ch.unibe.scg.methodnullabilityplugin.util;

import java.util.ArrayList;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import ch.unibe.scg.methodnullabilityplugin.Console;

/**
 * Contains helper methods regarding {@link IPartListener2}.
 */
public class IPartListenerInstaller {

	// returns an error string or null if a-ok
	public static String installOnWorkbench(IPartListener2 listener) {
		try {
			ArrayList<IWorkbenchWindow> nonnullwindows = getWorkbenchWindows();
			
			// will this ever fail?  
			for (IWorkbenchWindow window : nonnullwindows) {
				IPartService svc = window.getPartService();
				svc.addPartListener(listener);
			}
			
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		} catch (Exception e) {
			Console.err("General exception in 'installOnWorkbench': "
					+ e.getMessage());
			return e.getMessage();
		}
		return null;

	}

	public static ArrayList<IWorkbenchWindow> getWorkbenchWindows() {
		ArrayList<IWorkbenchWindow> output = new ArrayList<IWorkbenchWindow>();

		try {
			IWorkbench workbench = PlatformUI.getWorkbench(); // might throw
																// exception
			if (workbench == null) {
				throw new IllegalArgumentException("Workbench is null -- when trying to install IPartListener.");
			} else if (workbench.getWorkbenchWindowCount() == 0){
				throw new IllegalArgumentException("Workbench has 0 windows! -- when trying to install IPartListener.");
			} else {
				IWorkbenchWindow windows[] = workbench.getWorkbenchWindows();
				boolean hasNonNullWindow = false;
				for (IWorkbenchWindow window : windows) {
					if (window != null) {
						output.add(window);
						hasNonNullWindow = true;
					}
				}
				if (!hasNonNullWindow) {
					throw new IllegalArgumentException("Workbench has "+ workbench.getWorkbenchWindowCount() +" windows, but all are null -- when trying to install IPartListener.");
				}
			}
		} catch (IllegalStateException e) {
			throw new IllegalArgumentException("getWorkbench() failed: " + e.getMessage() + ", when trying to install IPartListener. ");
		}
		return output;
	}
	
	// returns all editors in any workbench
	public static ArrayList<IEditorPart> getCurrentEditors() {
		ArrayList<IEditorPart> eds = new ArrayList<IEditorPart>();
		try {
			ArrayList<IWorkbenchWindow> windows = getWorkbenchWindows();
			for (IWorkbenchWindow window : windows) {
				if (window != null) {
					IWorkbenchPage[] pages = window.getPages();
					for (IWorkbenchPage page : pages) {
						if (page != null) {
							IEditorReference[] edRefs = page
									.getEditorReferences();
							for (IEditorReference edRef : edRefs) {
								if (edRef != null) {
									IEditorPart ed = edRef.getEditor(false);
									if (ed != null) {
										eds.add(ed);
									}
								}
							}
						}
					}
				}
			}
		} catch (IllegalArgumentException e) {
			Console.err(e);
		} catch (Exception e) {
			Console.err(e);
		}
		return eds;
	}

}
