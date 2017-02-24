package ch.unibe.scg.methodnullabilityplugin.database;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

public class DeclaringRootTypesFinder {

	/**
	 * Finds the root classes declaring the specified method.
	 * 
	 * E.g. <code>B b = ...; c = b.do();</code>, if A declared do() already,
	 * this class will find A as the root class of the invoked do() method,
	 * although it was invoked on a B instance.
	 * 
	 * @param method
	 *            The method of which to find the root classes
	 * @return All root types declaring the specified method. Might be multiple,
	 *         as two unrelated implemented interfaces might declare the same
	 *         method.
	 * @throws JavaModelException
	 *             If the access to method model failed
	 */
	public Set<IType> findDeclaringRootTypes(IMethod method) throws JavaModelException {
		ITypeHierarchy typeHierarchy = method.getDeclaringType().newSupertypeHierarchy(null);
		return this.findDeclaringRootTypes(typeHierarchy, typeHierarchy.getType(), method);
	}

	private Set<IType> findDeclaringRootTypes(ITypeHierarchy typeHierarchy, IType leaf, IMethod method)
			throws JavaModelException {
		Set<IType> superTypes = Stream.of(typeHierarchy.getSupertypes(leaf)).collect(Collectors.toSet());
		Set<IType> declaringSuperRootTypes = this.findDeclaringRootTypes(typeHierarchy, superTypes, method);
		if (!declaringSuperRootTypes.isEmpty()) {
			return declaringSuperRootTypes;
		}
		boolean found = Stream.of(leaf.getMethods()).anyMatch(candidate -> this.areEqual(method, candidate));
		if (found) {
			return Collections.singleton(leaf);
		}
		return Collections.emptySet();
	}

	private Set<IType> findDeclaringRootTypes(ITypeHierarchy typeHierarchy, Set<IType> leafs, IMethod method) {
		return leafs.stream().flatMap(leaf -> {
			try {
				return this.findDeclaringRootTypes(typeHierarchy, leaf, method).stream();
			} catch (JavaModelException exception) {
				throw new RuntimeException(exception);
			}
		}).collect(Collectors.toSet());
	}

	private boolean areEqual(IMethod a, IMethod b) {
		try {
			return b.getElementName().equals(a.getElementName()) && b.getSignature().equals(a.getSignature());
		} catch (JavaModelException exception) {
			throw new RuntimeException(exception);
		}
	}

}
