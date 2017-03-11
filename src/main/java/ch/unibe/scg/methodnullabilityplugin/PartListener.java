package ch.unibe.scg.methodnullabilityplugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityAccessor;
import ch.unibe.scg.methodnullabilityplugin.database.MethodNullabilityInfo;
import ch.unibe.scg.methodnullabilityplugin.hovers.MethodNullabilityJavadocHover;
import ch.unibe.scg.methodnullabilityplugin.util.ASTUtils;
import ch.unibe.scg.methodnullabilityplugin.util.IPartListenerInstaller;

public class PartListener implements IPartListener2 {

	private static final String MARKER_ID = Activator.PLUGIN_ID + ".marker";

	private static double NULLABILITY_THRESHOLD = 0.2;
	
	private MethodNullabilityAccessor methodNullabilityAccessor;
	
	public PartListener(boolean install) {

		String errStr;
		// install listener for editor events
		errStr = IPartListenerInstaller.installOnWorkbench(this);
		if (errStr != null) {
			Console.err("install fail: " + errStr);
		}

		this.methodNullabilityAccessor = PlatformUI.getWorkbench().getService(MethodNullabilityAccessor.class);
		
		// install on currently open editors
		// run this in the UI thread?
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				ArrayList<IEditorPart> eds = IPartListenerInstaller
						.getCurrentEditors();
				for (IEditorPart ed : eds) {
					registerEditor(ed);
				}
			}
		}
		);
		
	}
	
	private void registerEditor(IEditorPart ed) {
		if (ed instanceof CompilationUnitEditor) {
			CompilationUnitEditor cu = (CompilationUnitEditor) ed;
			
			
			ITypeRoot input = SelectionConverter.getInput(cu);
			
			if (input instanceof CompilationUnit) {
				CompilationUnit c = (CompilationUnit) input;
				IFile file = ResourceUtil.getFile(c);
				ASTNode ast = ASTUtils.getAST(c);
				org.eclipse.jdt.core.dom.CompilationUnit cUnitAstRoot = (org.eclipse.jdt.core.dom.CompilationUnit) ast;
				ast.accept(new ASTVisitor() {
					
					@Override 
	        		public boolean visit(MethodInvocation node) { 
	        			Console.msg("MethodInvocation: '" + node.toString().replaceAll("\n", "") + "'\t\t | class: " + node.getClass());
	        			return super.visit(node); 
	                } 
					
			        public boolean visit(MethodDeclaration method) {
			        	Console.msg("MethodDeclaration: '" + method.toString().replaceAll("\n", "") + "'\t\t | class: " + method.getClass());
			            Block methodBlock = method.getBody();
			            methodVisitor(methodBlock);
			            return true;
			        }
			        
			        public void methodVisitor(Block block) {
			        	Console.msg("methodVisitor for " + block.getParent());
//			            debug("entering met visitor", "1");
//			            ASTParser metparse = ASTParser.newParser(AST.JLS8);
//			            metparse.setSource(block2.toString().toCharArray());
//			            metparse.setKind(ASTParser.K_STATEMENTS);
//			            Block block = (Block) metparse.createAST(null);

			        	List<?> statements = block.statements();
			        	statements.forEach(o -> {
			        		Console.msg("statement: '" + o.toString().replaceAll("\n", "") + "'\t\t | class: " + o.getClass());
			        	});
			        	
			        	
			        	block.accept(new ASTVisitor() {
			        		@Override 
			        		public boolean visit(MethodInvocation node) { 
			        			Console.msg("block.MethodInvocation: '" + node.toString().replaceAll("\n", "") + "'\t\t | class: " + node.getClass());
			        			
			        			IMethodBinding methodBinding = node.resolveMethodBinding();
			        			IJavaElement javaElement = methodBinding.getJavaElement();
			        			if (isMethodWithReferenceTypeReturnValue(javaElement)) {
			        				Console.msg("method " + javaElement.getElementName() + " is nullability-checkable!");

			        				try {
			        					MethodNullabilityInfo info = methodNullabilityAccessor.retrieve((IMethod) javaElement);
			        					
			        					
			        					if (info.nullability() > NULLABILITY_THRESHOLD) {
											IMarker m = file.createMarker(MARKER_ID);
											String msg = String.format("Method '" + javaElement.getElementName() + "' should be checked for null [%.2f]", info.nullability());
											m.setAttribute(IMarker.MESSAGE, msg);
											m.setAttribute(IMarker.LINE_NUMBER, cUnitAstRoot.getLineNumber(node.getStartPosition()));
			        					}
			        				} catch (CoreException e) {
										e.printStackTrace();
									}
			        				
			        			} else {
			        				Console.msg("method " + javaElement.getElementName() + " is NOT nullability-checkable!");
			        			}
			        			
			        			return super.visit(node); 
			                } 
			        		@Override 
			        		public boolean visit(MethodDeclaration node) { 
			        			Console.msg("block.MethodDeclaration: '" + node.toString().replaceAll("\n", "") + "'\t\t | class: " + node.getClass());
			        			return super.visit(node); 
			                } 
			        		
			        		private boolean isMethodWithReferenceTypeReturnValue(IJavaElement javaElement) {
		        				try {
									return MethodNullabilityJavadocHover.isMethodWithReferenceTypeReturnValue(javaElement);
								} catch (JavaModelException e) {
									e.printStackTrace();
									return false;
								}
			        		}
			        		
						});
			        }
			        
				});
			}
			
//			try {
//				IJavaElement[] children = input.getChildren();
//				for (IJavaElement je : children) {
//					System.out.println(je.getElementName());
//					System.out.println(je.getJavaModel());
//				}
//			} catch (JavaModelException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}
	
	private static IEditorPart getEditor(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		IEditorPart editor = null;
		if (part != null && part instanceof IEditorPart) {
			editor = (IEditorPart) part.getAdapter(IEditorPart.class);
		}
		return editor;
	}
	
	
//	private static void drawBoxesOnEditor(IEditorPart editor) {
//		BoxConstrainedEditorOverlay bceo = BoxConstrainedEditorOverlay.getBCEO(editor);
//		if (bceo != null) {
//			bceo.drawBoxes();
//		}
//	}
	
	////////////////////////
	
	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		//drawBoxesOnEditor(getEditor(partRef));
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {


	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		Console.msg("closing part: " + partRef.getPartName());

		// only testwise...
		IEditorPart editor = getEditor(partRef);
		if (editor instanceof CompilationUnitEditor) {
			CompilationUnitEditor cu = (CompilationUnitEditor) editor;
			
			
			ITypeRoot input = SelectionConverter.getInput(cu);
			
			if (input instanceof CompilationUnit) {
				CompilationUnit c = (CompilationUnit) input;
				IFile file = ResourceUtil.getFile(c);

				try {
					IMarker[] marks = file.findMarkers(MARKER_ID, false,
							IResource.DEPTH_ZERO);
					for (int i = 0; i < marks.length; i++) {
						Console.msg("deleting marker: " + marks[i] + " [attrs=" + marks[i].getAttributes().entrySet() + "]");
						marks[i].delete();
					}
				} catch (CoreException e) {
					Console.err(e);
				}
			}
		}
	}

	
	
	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {


	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		Console.msg("opening part: " + partRef.getPartName());
//		installDance(partRef);
		IEditorPart editor = getEditor(partRef);
		registerEditor(editor);
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {


	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {


	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO we probably need to worry about this, yo

	}

}
