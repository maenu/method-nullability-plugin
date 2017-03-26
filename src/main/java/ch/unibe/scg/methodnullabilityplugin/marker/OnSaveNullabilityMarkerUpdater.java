package ch.unibe.scg.methodnullabilityplugin.marker;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;

import ch.unibe.scg.methodnullabilityplugin.Console;

public class OnSaveNullabilityMarkerUpdater implements IPropertyListener {

	private JavaEditor editor;
	// TODO: hackhack, improve this!!
	private static Map<IEditorPart, OnSaveNullabilityMarkerUpdater> updaters = new ConcurrentHashMap<>();
	
	private OnSaveNullabilityMarkerUpdater(JavaEditor editor) {
		this.editor = editor;
	}
	
	@Override
	public void propertyChanged(Object source, int propId) {
		Console.msg("propertyChanged(" + (propId == IEditorPart.PROP_DIRTY) + ", " + !editor.isDirty() + ")..");
		
		if (propId == IEditorPart.PROP_DIRTY && !editor.isDirty()) {
			Console.msg("UPDATE NULLABILITY MARKERS NOW IN ENTIRE AST !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			if (source instanceof IEditorPart) {
				Console.msg("going to update nullability markers in file " + ((IEditorPart) source).getTitle() + "...");
				NullabilityMarkers.update((IEditorPart) source);
			}
		} 
	}
	
	public static boolean create(IEditorPart editor) {
		if (editor != null && editor instanceof JavaEditor) {
			OnSaveNullabilityMarkerUpdater updater = new OnSaveNullabilityMarkerUpdater((JavaEditor) editor);
			editor.addPropertyListener(updater);
			updaters.put(editor, updater);
			Console.msg("ADDED OnSaveNullabilityMarkerUpdater to editor [" + editor.getTitle() + "]");
			return true;
		}
		return false;
	}
	
	public static boolean remove(IEditorPart editor) {
		if (editor != null && editor instanceof JavaEditor) {
			// TODO: at work: remove listener -> need manager to keep track of PropertyListener instances...
			// TODO: at work: cf. Isa.java
			OnSaveNullabilityMarkerUpdater updater = updaters.remove(editor);
			Objects.requireNonNull(updater);
			editor.removePropertyListener(updater);
			Console.msg("REMOVED OnSaveNullabilityMarkerUpdater to editor [" + editor.getTitle() + "]");
			return true;
		}
		return false;
	}
}
