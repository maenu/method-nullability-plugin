package ch.unibe.scg.methodnullabilityplugin.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import ch.unibe.scg.methodnullabilityplugin.Console;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public abstract class LogASTVisitor extends ASTVisitor {

	public static ASTVisitor create(CompilationUnit compilationUnit) {
		try {
			ProxyFactory factory = new ProxyFactory();
			factory.setSuperclass(LogASTVisitor.class);
			factory.setFilter(
			    new MethodFilter() {
			        @Override
			        public boolean isHandled(Method method) {
			        	return method.getName().startsWith("visit");
			        }
			    }
			);
			
			MethodHandler handler = new MethodHandler() {
				
				
			    @Override
			    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
			    	Console.msg("Method: '" + thisMethod + "'");
			        for (Object arg : args) {
			        	Console.msg("\tArg val: " + arg);
			        	if (arg instanceof VariableDeclarationStatement) {
			        		String s = arg.toString();
			        		if (s.contains("IntStream chars=s.chars()")) {
			        			
			        			VariableDeclarationStatement vds = (VariableDeclarationStatement) arg;
			        			AtomicBoolean hasNullableAnnotationAlready = new AtomicBoolean(false);
			        			vds.accept(new ASTVisitor() {
			        				@Override
			        				public boolean visit(MarkerAnnotation node) {
			        					if (node.getTypeName().getFullyQualifiedName().equals(Nullable.class.getName())) {
			        						hasNullableAnnotationAlready.set(true);
			        						return false;
			        					}
			        					return true;
			        				}
								});
			        			
			        			if (!hasNullableAnnotationAlready.get()) {
			        				AST ast = vds.getAST();
				        			ASTRewrite rewrite= ASTRewrite.create(ast);
				        			
				        			List<?> fragments = vds.fragments();
				        			VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
				        			Expression e = fragment.getInitializer();
				        			if (e.getNodeType() == Expression.METHOD_INVOCATION) {
				        				MethodInvocation mi = (MethodInvocation) fragment.getInitializer();
				        				// TODO: get nullability..
				        				
				        				Type type = vds.getType();
				        				
				        				ListRewrite listRewrite= rewrite.getListRewrite(vds, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
				        				MarkerAnnotation markerAnnotation= ast.newMarkerAnnotation();
//				        				markerAnnotation.setTypeName(ast.newSimpleName(Nullable.class.getSimpleName()));
				        				markerAnnotation.setTypeName(ast.newName(Nullable.class.getName()));
				        				listRewrite.insertAt(markerAnnotation, 0, null);
				        				
				        				
				        				String source = compilationUnit.getSource();
				        				Document document= new Document(source);
				        				
				        				TextEdit edits = rewrite.rewriteAST(document, compilationUnit.getJavaProject().getOptions(true));
			        				   
				        				// computation of the new source code
				        				edits.apply(document);
				        				String newSource = document.get();

				        				// update of the compilation unit
				        				compilationUnit.getBuffer().setContents(newSource);
				        			}
			        			}
			        		}
			        	} else if (arg instanceof MethodInvocation) {
			        		String s = arg.toString();
			        		if (s.contains("httpclient.execute(null)")) {
			        			MethodInvocation mi = (MethodInvocation) arg;
			        			
			        			//compilationUnit.
			        			
			        			System.out.println(mi);
			        		}
			        	}
			        }
			        
			        return proceed.invoke(self, args);
			    }
			};
			
			ASTVisitor logVisitor;
			try {
				logVisitor = (ASTVisitor) factory.create(new Class<?>[0], new Object[0], handler);
				return logVisitor;
			} catch (NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
					| InvocationTargetException e) {
				Console.err(e);
				throw new IllegalArgumentException(e);
			}
		} catch (Throwable t) {
			Console.err("CATCH THROWABLE: " + t);
		}
		return new ASTVisitor() {
		};
	}
}
