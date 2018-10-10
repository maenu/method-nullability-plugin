package ch.unibe.scg.methodnullabilityplugin.task;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.util.ExternalAnnotationUtil;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;

import ch.unibe.scg.methodnullabilityplugin.database.Database;

public class EeaGenerator implements AutoCloseable {

	private static final ExternalAnnotationUtil.MergeStrategy MERGE_STRATEGY = ExternalAnnotationUtil.MergeStrategy.OVERWRITE_ANNOTATIONS;

	private Connection connection;
	private IWorkspaceRoot workspaceRoot;
	private IPath annotationsPath;
	private int inserted;
	private int skipped;

	public EeaGenerator(String url, IWorkspaceRoot workspaceRoot, IPath annotationsPath)
			throws SQLException, IOException {
		this.connection = DriverManager.getConnection(url);
		this.workspaceRoot = workspaceRoot;
		this.annotationsPath = annotationsPath;
	}

	public void execute(double maxNullabilityNonNull, double infNullabilityNullable)
			throws SQLException, IllegalArgumentException, CoreException, IOException {
		this.inserted = 0;
		this.skipped = 0;
		AtomicInteger inserted = new AtomicInteger(0);
		AtomicInteger skipped = new AtomicInteger(0);
		try (Statement methodsStatement = connection.createStatement();
				ResultSet methods = methodsStatement.executeQuery(
						"select distinct class, method from " + Database.TABLE + " order by class, method")) {
			while (methods.next()) {
				String clazz = methods.getString(1);
				String method = methods.getString(2);
				String type = clazz.replace('.', '/');
				String[] methodParts = method.split("\\(|\\)");
				String selector = methodParts[0];
				String originalSignature = "(" + methodParts[1] + ")" + methodParts[2];
				String returnType = methodParts[2];
				List<String> parameterTypes = Stream
						.of(Signature.getParameterTypes(originalSignature.replace('/', '.')))
						.map(s -> s.replace('.', '/')).collect(Collectors.toList());
				IFile eea = getEea(clazz);
				try (Statement indexesStatement = connection.createStatement();
						ResultSet indexes = indexesStatement.executeQuery(
								"select index_, support, evidence from " + Database.TABLE + " where class = '" + clazz
										+ "' and method = '" + method + "' order by index_")) {
					while (indexes.next()) {
						int index = indexes.getInt(1);
						int support = indexes.getInt(2);
						int evidence = indexes.getInt(3);
						double nullability = (double) evidence / support;
						char annotation = ExternalAnnotationUtil.NO_ANNOTATION;
						if (nullability > infNullabilityNullable) {
							annotation = ExternalAnnotationUtil.NULLABLE;
						} else if (nullability <= maxNullabilityNonNull) {
							annotation = ExternalAnnotationUtil.NONNULL;
						} else {
							skipped.incrementAndGet();
							continue;
						}
						if (index == -1) {
							String annotated = annotate(returnType, annotation);
							ExternalAnnotationUtil.annotateMethodReturnType(type, eea, selector, originalSignature,
									annotated, MERGE_STRATEGY, null);
						} else {
							String annotated = annotate(parameterTypes.get(index), annotation);
							ExternalAnnotationUtil.annotateMethodParameterType(type, eea, selector, originalSignature,
									annotated, index, MERGE_STRATEGY, null);
						}
						inserted.incrementAndGet();
					}
				}
			}
		}
		this.inserted = inserted.get();
		this.skipped = skipped.get();
	}

	private IFile getEea(String clazz) {
		return workspaceRoot.getFile(annotationsPath.append(clazz.replace('.', '/'))
				.addFileExtension(ExternalAnnotationProvider.ANNOTATION_FILE_EXTENSION));
	}

	public int getInserted() {
		return inserted;
	}

	public int getSkipped() {
		return skipped;
	}

	@Override
	public void close() throws Exception {
		this.connection.close();
	}

	private String annotate(String type, char annotation) {
		return type.substring(0, 1) + String.valueOf(annotation) + type.substring(1);
	}

}
