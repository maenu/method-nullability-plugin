package ch.unibe.scg.methodnullabilityplugin;

import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;

import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityAccessor;

public class MethodNullabilityServiceFactory extends AbstractServiceFactory {

	@SuppressWarnings("rawtypes")
	@Override
	public Object create(Class serviceInterface, IServiceLocator parentLocator, IServiceLocator locator) {

		if (MethodNullabilityAccessor.class.equals(serviceInterface)) {
			return MethodNullabilityAccessorLazyHolder.INSTANCE;
		}

		return null;
	}

	/**
	 * This holder class ensures thread safe singleton instance and lazy
	 * initialization.
	 * 
	 */
	private static class MethodNullabilityAccessorLazyHolder {
		private static final MethodNullabilityAccessor INSTANCE = new MethodNullabilityAccessor();
	}
}
