package nullablemethoddetection;

import java.util.List;

public class Query {

	private String declaringType;
	private String returnType;
	private String name;
	private List<String> parameterTypes;

	public Query(String declaringType, String returnType, String name, List<String> parameterTypes) {
		this.declaringType = declaringType;
		this.returnType = returnType;
		this.name = name;
		this.parameterTypes = parameterTypes;
	}

	public String getDeclaringType() {
		return declaringType;
	}

	public String getReturnType() {
		return returnType;
	}

	public String getName() {
		return this.name;
	}

	public List<String> getParameterTypes() {
		return parameterTypes;
	}

}
