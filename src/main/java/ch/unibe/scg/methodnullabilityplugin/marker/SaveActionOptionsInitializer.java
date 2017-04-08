package ch.unibe.scg.methodnullabilityplugin.marker;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

import ch.unibe.scg.methodnullabilityplugin.Console;

public class SaveActionOptionsInitializer implements ICleanUpOptionsInitializer {

	@Override
	public void setDefaultOptions(CleanUpOptions options) {
		Console.msg("SaveActionOptionsInitializer.options: " + options.getKeys());
	}

}
