package ch.unibe.scg.methodnullabilityplugin;

import org.eclipse.ui.IStartup;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		Console.msg("early startup...");
	}

}
