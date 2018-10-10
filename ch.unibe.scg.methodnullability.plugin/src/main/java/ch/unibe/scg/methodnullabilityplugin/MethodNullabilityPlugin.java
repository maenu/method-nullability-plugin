package ch.unibe.scg.methodnullabilityplugin;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class MethodNullabilityPlugin extends AbstractUIPlugin {

	{
		// load SQLite JDBC driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static final String PLUGIN_ID = "ch.unibe.scg.methodnullabilityplugin";
	private static MethodNullabilityPlugin plugin = null;

	public static MethodNullabilityPlugin getDefault() {
		return plugin;
	}

	private String databaseUrl;

	public MethodNullabilityPlugin() {
		super();
		plugin = this;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		IPath fileLocation = Platform.getStateLocation(bundleContext.getBundle()).append("method-nullability")
				.addFileExtension("db");
		databaseUrl = "jdbc:sqlite:" + FileLocator.toFileURL(new URL("file://" + fileLocation.toOSString())).getFile();
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		databaseUrl = null;
	}

	public String getDatabaseUrl() {
		return databaseUrl;
	}

}
