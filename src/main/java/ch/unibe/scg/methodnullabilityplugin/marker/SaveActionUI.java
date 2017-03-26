package ch.unibe.scg.methodnullabilityplugin.marker;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;
import org.eclipse.swt.widgets.Composite;

import ch.unibe.scg.methodnullabilityplugin.Console;

public class SaveActionUI implements ICleanUpConfigurationUI {

	@Override
	public void setOptions(CleanUpOptions options) {
		Console.msg("SaveActionUI.options: " + options.getKeys());
	}

	@Override
	public Composite createContents(Composite parent) {
		Console.msg("SaveActionUI.createContents: " + parent);
		return null;
	}

	@Override
	public int getCleanUpCount() {
		Console.msg("SaveActionUI.getCleanUpCount: ");
		return 1;
	}

	@Override
	public int getSelectedCleanUpCount() {
		Console.msg("SaveActionUI.getSelectedCleanUpCount: ");
		return 1;
	}

	@Override
	public String getPreview() {
		Console.msg("SaveActionUI.getPreview: ");
		return "Nullability Save Action Preview";
	}

}
