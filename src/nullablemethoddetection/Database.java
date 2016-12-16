package nullablemethoddetection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class Database {

	private static final Path DATA = Paths
			.get("/Users/maenu/Development/university/masters/null-infection/thesis/experiments/nullable-methods.csv");

	private DeclaringRootTypesFinder declaringRootClassFinder;

	public Database() {
		this.declaringRootClassFinder = new DeclaringRootTypesFinder();
	}

	public List<Result> query(IMethod method) throws JavaModelException {
		String groupId = "";
		String artifactId = "";
		String version = "";
		Set<String> declaringTypes = this.declaringRootClassFinder.findDeclaringRootTypes(method).stream()
				.map(type -> type.getFullyQualifiedName()).collect(Collectors.toSet());
		String name = method.getElementName();
		String returnType = Signature.toString(method.getReturnType());
		List<String> parameterTypes = Stream.of(method.getParameterTypes()).map(Signature::toString)
				.collect(Collectors.toList());
		return declaringTypes.stream().map(declaringType -> this.query(groupId, artifactId, version, declaringType,
				name, returnType, parameterTypes)).collect(Collectors.toList());
	}

	private Result query(String groupId, String artifactId, String version, String declaringType, String name,
			String returnType, List<String> parameterTypes) {
		return null;
	}

	private String toPattern(Query query) {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("\"%s\"", query.getDeclaringType()));
		builder.append(",");
		builder.append(String.format("\"%s \"", query.getReturnType()));
		return builder.toString();
	}

}
