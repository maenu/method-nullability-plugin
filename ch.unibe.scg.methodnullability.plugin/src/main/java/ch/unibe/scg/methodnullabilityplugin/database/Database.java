package ch.unibe.scg.methodnullabilityplugin.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class Database {

	public static final String TABLE = "method_nullability";

	private Connection connection;

	public Database(String url) throws SQLException {
		this.connection = DriverManager.getConnection(url);
	}

	public Map<Integer, Nullability> search(String clazz, String method) {
		Map<Integer, Nullability> nullabilities = new HashMap<>();
		try (Statement indexesStatement = connection.createStatement();
				ResultSet indexes = indexesStatement
						.executeQuery("select index_, support, evidence from " + Database.TABLE + " where class = '"
								+ clazz + "' and method = '" + method + "' order by index_")) {
			while (indexes.next()) {
				int index = indexes.getInt(1);
				int support = indexes.getInt(2);
				int evidence = indexes.getInt(3);
				nullabilities.put(index, new Nullability(support, evidence));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return nullabilities;
	}

	public Map<Integer, Nullability> search(IMethod method) throws JavaModelException {
		String clazz = Signature.getTypeErasure(method.getDeclaringType().getFullyQualifiedName());
		String returnType = Signature.getTypeErasure(method.getReturnType()).replace('.', '/');
		String name = method.getElementName();
		if (method.isConstructor()) {
			name = "<init>";
		}
		List<String> parameterTypes = Stream.of(method.getParameterTypes()).map(Signature::getTypeErasure)
				.map(s -> s.replace('.', '/')).collect(Collectors.toList());
		String methodFull = String.format("%s(%s)%s", name, String.join("", parameterTypes), returnType);
		return this.search(clazz, methodFull);
	}

	@Override
	protected void finalize() throws Throwable {
		connection.close();
	}

}
