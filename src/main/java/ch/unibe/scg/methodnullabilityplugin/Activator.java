package ch.unibe.scg.methodnullabilityplugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	public static final String SAMPLE_IMAGE = "icons/sample.gif";
	public static final String PLUGIN_ID = "ch.unibe.scg.methodnullabilityplugin";

	private static BundleContext context;
	
	// The shared instance
	private static Activator plugin = null;
	
	static BundleContext getContext() {
		return context;
	}
	
	/**
	 * The constructor
	 */
	public Activator() {
		super();
		plugin = this;
	}
	
	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		Console.msg("start nullability plugin...");
		
		install();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}
	
	public static ImageDescriptor getImageDescriptor(String imgID) {
		// makes use of fact that img id is a relative path
		return imageDescriptorFromPlugin(PLUGIN_ID, imgID);
	}
	
	@SuppressWarnings("unused")
	private static PartListener editorListener = null;
	
	private static void install() {
		editorListener = new PartListener(true);
	}
}
