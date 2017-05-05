package ch.unibe.scg.methodnullabilityplugin.eea;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.ExternalAnnotationUtil;
import org.eclipse.jdt.core.util.ExternalAnnotationUtil.MergeStrategy;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.util.KeyToSignature;

import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.eea.CsvAccessor.NullabilityRecord;

public class CsvToEeaConverter {

	private static final MergeStrategy MERGE_STRATEGY = MergeStrategy.OVERWRITE_ANNOTATIONS;
	
	private int numTotalCsvRecords = 0;
	private int numProcessedCsvRecords = 0;
	private int numEeaFiles = 0;
	
	public int getProcessedCsvRecords() {
		return numProcessedCsvRecords;
	}
	
	public int getTotalCsvRecords() {
		return numTotalCsvRecords;
	}
	
	public int getNumEeaFiles() {
		return numEeaFiles;
	}
	
	public void execute(String csvFilename, String eeaPath) throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(eeaPath);
		IPath annotationPath = null;
		if (resource == null) {
			annotationPath = new Path(eeaPath);
			if (!annotationPath.isValidPath(".")) {
				throw new IllegalArgumentException("Path to EEA root not valid: " + annotationPath);
			}
		} else {
			annotationPath = resource.getFullPath(); //new Path(rawAnnotationPath);
		}
		
		if (annotationPath == null) {
			throw new IllegalArgumentException("Path to EEA root not found: " + eeaPath);
		}

		
		List<NullabilityRecord> csvEntries = CsvAccessor.loadCsv(csvFilename);
		this.numTotalCsvRecords = csvEntries.size();
		Console.msg("read " + csvEntries.size() + " entries...");
		
		int recordsWithInvocations = 0;
		for (NullabilityRecord nr : csvEntries) {
			
			if (nr.hasInvocations()) {
				double nullability = nr.nullability();
				char annotation;
				if (nullability > 0) {
					annotation = ExternalAnnotationUtil.NULLABLE;
				} else {
					annotation = ExternalAnnotationUtil.NONNULL;
				}

				
				String fAffectedTypeName = nr.getClazz().replace('.', '/'); // "javassist/ClassMap";
				IFile fAnnotationFile = getAnnotationFile(root, nr.getClazz(), annotationPath);
				String fSelector = determineSelector(nr.getMethod());
				
				String fSignature = determineSignature(nr.getMethod(),  nr.getClazz()); // e.g. "(Ljava/lang/Object;)Ljava/lang/Object;";
				String fAnnotatedSignature = annotateSignature(fSignature, annotation); // "L0java/lang/Object;";
				
				ExternalAnnotationUtil.annotateMethodReturnType(fAffectedTypeName, fAnnotationFile, fSelector, fSignature, fAnnotatedSignature, MERGE_STRATEGY, null);
				recordsWithInvocations++;
			}
		}
		
		this.numProcessedCsvRecords = recordsWithInvocations;
	}
	
	private String annotateSignature(String fSignature, char annotation) {
		// cf.	TypeRenderer rendererNonNull= new TypeRenderer(typeToAnnotate, offset, NONNULL);
		
		// e.g. "(Ljava/lang/Object;)Ljava/lang/Object;";
		int lastIndexOf = fSignature.lastIndexOf(")L");
		String returnType = fSignature.substring(lastIndexOf + 1);
		
		return returnType.replaceFirst("L", "L" + annotation);
	}

	private String determineSelector(String method) {
		int indexOf = method.indexOf(" ");
		String substring = method.substring(indexOf+1);
		return substring.substring(0, substring.lastIndexOf("("));
	}
	
	// extractGenericSignature
	private String determineSignature(String method, String clazz) {
		char[] uniqueKey = computeUniqueKey(method, clazz);
		String key = new String(uniqueKey);
		KeyToSignature parser = new KeyToSignature(key, KeyToSignature.SIGNATURE, true);
		parser.parse();
		return parser.toString();
	}
	
	// MethodBinding.computeUniqueKey(isLeaf=true)
	private char[] computeUniqueKey(String method, String clazz) {
//		boolean isLeaf = true;
		// declaring class
		char[] declaringKey = declaringClassComputeUniqueKey(clazz);
		// [L, j, a, v, a, s, s, i, s, t, /, C, l, a, s, s, M, a, p, ;]
		int declaringLength = declaringKey.length;

		// selector
		char[] selector = determineSelector(method).toCharArray();
		int selectorLength = selector == TypeConstants.INIT ? 0 : selector.length;

		// generic signature
		//TODO: char[] sig = genericSignature();
		char[] sig = null;
//		boolean isGeneric = false; //sig != null;

//		if (!isGeneric) {
		sig = signature(method);
//		}
		int signatureLength = sig.length;

		// thrown exceptions
//		int thrownExceptionsLength = 0; // this.thrownExceptions.length;
		int thrownExceptionsSignatureLength = 0;
//		char[][] thrownExceptionsSignatures = null;
//		boolean addThrownExceptions = thrownExceptionsLength > 0 && (!isGeneric || CharOperation.lastIndexOf('^', sig) < 0);
//		if (addThrownExceptions) {
//			thrownExceptionsSignatures = new char[thrownExceptionsLength][];
//			for (int i = 0; i < thrownExceptionsLength; i++) {
//				if (this.thrownExceptions[i] != null) {
//					thrownExceptionsSignatures[i] = this.thrownExceptions[i].signature();
//					thrownExceptionsSignatureLength += thrownExceptionsSignatures[i].length + 1;	// add one char for separator
//				}
//			}
//		}

		char[] uniqueKey = new char[declaringLength + 1 + selectorLength + signatureLength + thrownExceptionsSignatureLength];
		int index = 0;
		System.arraycopy(declaringKey, 0, uniqueKey, index, declaringLength);
		index = declaringLength;
		uniqueKey[index++] = '.';
		System.arraycopy(selector, 0, uniqueKey, index, selectorLength);
		index += selectorLength;
		System.arraycopy(sig, 0, uniqueKey, index, signatureLength);
//		if (thrownExceptionsSignatureLength > 0) {
//			index += signatureLength;
//			for (int i = 0; i < thrownExceptionsLength; i++) {
//				char[] thrownExceptionSignature = thrownExceptionsSignatures[i];
//				if (thrownExceptionSignature != null) {
//					uniqueKey[index++] = '|';
//					int length = thrownExceptionSignature.length;
//					System.arraycopy(thrownExceptionSignature, 0, uniqueKey, index, length);
//					index += length;
//				}
//			}
//		}
		return uniqueKey;
	}
	
	// ReferenceBinding.computeUniqueKey
	private char[] declaringClassComputeUniqueKey(String clazz) {
		//this.declaringClass.computeUniqueKey(false/*not a leaf*/);
//		public char[] signature() /* Ljava/lang/Object; */ {
//			if (this.signature != null)
//				return this.signature;
//
//			return this.signature = CharOperation.concat('L', constantPoolName(), ';');
//		}
		
		
//		public char[] constantPoolName() /* java/lang/Object */ {
//			if (this.constantPoolName != null) return this.constantPoolName;
//			return this.constantPoolName = CharOperation.concatWith(this.compoundName, '/');
//		}

		// taken from BinaryTypeBinding:261
		char[][] compoundName = CharOperation.splitOn('/', clazz.replace('.', '/').toCharArray());
		char[] constantPoolName = CharOperation.concatWith(compoundName, '/');
		// [j, a, v, a, s, s, i, s, t, /, C, l, a, s, s, M, a, p]
		return CharOperation.concat('L', constantPoolName, ';');
	}

	// taken from compiler.lookup.MethodBinding.signature
	public final char[] signature(String methodSignature) /* (ILjava/lang/Thread;)Ljava/lang/Object; */ {
		StringBuffer buffer = new StringBuffer();
		buffer.append('(');

		String[] targetParameters = getParameters(methodSignature); // this.parameters;
//		boolean isConstructor = false; // isConstructor(selector);
//		if (isConstructor) { // && this.declaringClass.isEnum()) { // insert String name,int ordinal
//			buffer.append(ConstantPool.JavaLangStringSignature);
//			buffer.append(TypeBinding.INT.signature());
//		}
//		boolean needSynthetics = isConstructor && this.declaringClass.isNestedType();
//		if (needSynthetics) {
//			// take into account the synthetic argument type signatures as well
//			ReferenceBinding[] syntheticArgumentTypes = this.declaringClass.syntheticEnclosingInstanceTypes();
//			if (syntheticArgumentTypes != null) {
//				for (int i = 0, count = syntheticArgumentTypes.length; i < count; i++) {
//					buffer.append(syntheticArgumentTypes[i].signature());
//				}
//			}
//
//			if (this instanceof SyntheticMethodBinding) {
//				targetParameters = ((SyntheticMethodBinding)this).targetMethod.parameters;
//			}
//		}

		for (int i = 0; i < targetParameters.length; i++) {
			String p = targetParameters[i];
			if (p != null && !p.isEmpty()) {
				buffer.append(getTargetParameterSignature(targetParameters[i]));
			}
		}
		
//		if (needSynthetics) {
//			SyntheticArgumentBinding[] syntheticOuterArguments = this.declaringClass.syntheticOuterLocalVariables();
//			int count = syntheticOuterArguments == null ? 0 : syntheticOuterArguments.length;
//			for (int i = 0; i < count; i++) {
//				buffer.append(syntheticOuterArguments[i].type.signature());
//			}
//			// move the extra padding arguments of the synthetic constructor invocation to the end
//			for (int i = targetParameters.length, extraLength = this.parameters.length; i < extraLength; i++) {
//				buffer.append(this.parameters[i].signature());
//			}
//		}
		buffer.append(')');
		String returnType = getReturnType(methodSignature);
		if (returnType != null)
			buffer.append(getReturnTypeSignature(returnType));
		int nameLength = buffer.length();
		char[] signature = new char[nameLength];
		buffer.getChars(0, nameLength, signature, 0);

		return signature;
	}
	
	private char[] getReturnTypeSignature(String returnType) {
		return getTargetParameterSignature(returnType);
	}

	private String getReturnType(String methodSignature) {
		return methodSignature.trim().split(" ")[0];
	}

	// BinaryTypeBinding.signature()
	// 	+-- ReferenceBinding.signature() 
	private char[] getTargetParameterSignature(String targetParameter) {
		char[] constantPoolName = constantPoolName(targetParameter);
		if (constantPoolName.length == 1) {
			return constantPoolName;
		}
		char[] signature = CharOperation.concat('L', constantPoolName, ';');
		return signature;
	}

	// ReferenceBinding.constantPoolName()
	private char[] constantPoolName(String targetParameter) {
		String[] split = targetParameter.split("\\.");
		if (split.length == 1) {
			// check BaseTypeBinding: cf. TypeBinding
			if (targetParameter.equals("boolean")) {
				return TypeBinding.BOOLEAN.constantPoolName();
			} else if (targetParameter.equals("double")) {
				return TypeBinding.DOUBLE.constantPoolName();
			} else if (targetParameter.equals("float")) {
				return TypeBinding.FLOAT.constantPoolName();
			} else if (targetParameter.equals("long")) {
				return TypeBinding.LONG.constantPoolName();
			} else if (targetParameter.equals("char")) {
				return TypeBinding.CHAR.constantPoolName();
			} else if (targetParameter.equals("short")) {
				return TypeBinding.SHORT.constantPoolName();
			} else if (targetParameter.equals("byte")) {
				return TypeBinding.BYTE.constantPoolName();
			} else if (targetParameter.equals("int")) {
				return TypeBinding.INT.constantPoolName();
			} else {
				throw new IllegalArgumentException("unrecognized targetParameter=" + targetParameter);
			}
		}
		
		// e.g. [[j, a, v, a], [l, a, n, g], [O, b, j, e, c, t]]
		char[][] compoundName = new char[split.length][];
		for (int i=0; i < split.length; i++) {
			compoundName[i] = split[i].toCharArray();
		}
		return CharOperation.concatWith(compoundName, '/');
	}

	private String[] getParameters(String methodSignature) {
		String params = methodSignature.substring(methodSignature.indexOf("(")+1, methodSignature.lastIndexOf(")"));
		params = params.replace(" ", "");
		return params.split(",");
	}

	public final boolean isConstructor(char[] selector) {
		return selector == TypeConstants.INIT;
	}
	
	// taken from ExternalAnnotationUtil.getAnnotationFile
	public static IFile getAnnotationFile(IWorkspaceRoot workspaceRoot, String qualifiedTypeName, IPath annotationPath) throws Exception {
//		
//		IType targetType = project.findType(qualifiedTypeName);
//		if (!targetType.exists())
//			return null;
//
		String binaryTypeName = qualifiedTypeName.replace('.', '/');
//		
//		IPackageFragmentRoot packageRoot = (IPackageFragmentRoot) targetType.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
//		IClasspathEntry entry = packageRoot.getResolvedClasspathEntry();
//		IPath annotationPath = ClasspathEntry.getExternalAnnotationPath(entry, project.getProject(), false);
//	
		
//		IWorkspaceRoot workspaceRoot = project.getProject().getWorkspace().getRoot();

		if (annotationPath.segmentCount() > 1) {
			IFile annotationZip = workspaceRoot.getFile(annotationPath);
			if (annotationZip.exists())
				return null;
		}
		
		annotationPath = annotationPath.append(binaryTypeName).addFileExtension(ExternalAnnotationProvider.ANNOTATION_FILE_EXTENSION);
		IFile annotationFile = workspaceRoot.getFile(annotationPath);
		if (annotationFile == null) {
			annotationFile = ResourcesPlugin.getWorkspace().getRoot().getFile(annotationPath);
		}
		if (annotationFile == null) {
			throw new IllegalArgumentException("Path to EEA file not found: " + annotationPath);
		}
		return annotationFile;
	}
	
	public void runTests() {
		String method = "java.lang.Object get(java.lang.Object)";
		String clazz = "javassist.ClassMap";
		String expected = "(Ljava/lang/Object;)Ljava/lang/Object;";
		runTest(method, clazz, expected);
		
		method = "java.lang.Class loadClass(java.lang.String, boolean)";
		clazz = "javassist.Loader";
		expected = "(Ljava/lang/String;Z)Ljava/lang/Class;";
		runTest(method, clazz, expected);
		
		method = "org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest, org.apache.http.protocol.HttpContext)";
		clazz = "org.apache.http.client.HttpClient";
		expected = "(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;";
		runTest(method, clazz, expected);
	}

	private void runTest(String method, String clazz, String expected) {
		String result = determineSignature(method, clazz);
		System.out.println("actual..: " + result);
		System.out.println("expected: " + expected);
		if (!result.equals(expected)) {
			throw new IllegalArgumentException(result + " vs. " + expected);
		}
	}
	
	public static void main(String[] args) throws Exception {
		String csvFilename;
		if (args.length == 1) {
			csvFilename = args[0];
		} else {
			csvFilename = "inter-intra_small.csv";
		}
		
		new CsvToEeaConverter().execute(csvFilename, "method-nullability-plugin/annot");
//		new CsvToEeaConverter().runTests();
	}
}
