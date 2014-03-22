/**
 * Copyright (c) 2011 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IEventBRoot;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.animation.DiagramAnimator;
import ac.soton.eventb.statemachines.animation.StatemachineAnimationPlugin;
import de.bmotionstudio.gef.editor.BMotionStudioEditor;
import de.prob.exceptions.ProBException;

/**
 * Starts diagram animation with ProB.
 * 
 * @author vitaly
 *
 */
public class AnimateAction extends AbstractHandler {

	private static final String BMOTION_STUDIO_EXT = "bmso";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart activeEditor = HandlerUtil.getActiveEditorChecked(event);
		List<IFile> bmsFiles = new ArrayList<IFile>();
		if (activeEditor instanceof DiagramEditor) {
			DiagramEditor diagramEditor = (DiagramEditor) activeEditor;
			if (diagramEditor.isDirty()){
				diagramEditor.doSave(new NullProgressMonitor());
			}
			EObject element = diagramEditor.getDiagram().getElement();
			if (element instanceof Statemachine){
				Machine machine = (Machine) ((Statemachine)element).getContaining(MachinePackage.Literals.MACHINE);
				IEventBRoot root = getEventBRoot(machine);
				List<Statemachine> statemachines = new ArrayList<Statemachine>();
				//Find all the statemachines that are open as diagrams 
				// (these must come from the editors as each editor has a different local copy)
				for(IWorkbenchPage pg : HandlerUtil.getActiveWorkbenchWindow(event).getPages()){
			    	for(IEditorReference editorRef: pg.getEditorReferences()){
			    		IEditorPart editor = editorRef.getEditor(true);
			    		if (editor instanceof DiagramEditor) {
			    			diagramEditor = (DiagramEditor) editor;
			    			element = diagramEditor.getDiagram().getElement();
			    			Resource resource = element.eResource();
			    			if (element instanceof Statemachine && resource != null && resource.isLoaded() && root.equals(getEventBRoot((Statemachine)element))) {
			    					statemachines.add((Statemachine) element);
		    				}
		    	    	}
			    		//also look for BmotionStudio editors on the same machine
			    		if (editor instanceof BMotionStudioEditor) {
			    			BMotionStudioEditor bmsEditor = (BMotionStudioEditor) editor;
			    			Object pf = bmsEditor.getVisualization().getProjectFile();

			    			if (pf instanceof IFile){
				    			String machineName = bmsEditor.getVisualization().getMachineName();
				    			IProject project = ((IFile)pf).getProject();
			    				if ( 
			    					BMOTION_STUDIO_EXT.equals(((IFile)pf).getFileExtension()) &&
			    					machineName.startsWith(machine.getName()) &&
			    					root.getRodinProject().getProject().equals(project)
			    					){
			    					if (!bmsFiles.contains(pf)) bmsFiles.add(((IFile)pf));
			    				}
			    			}
		    	    	}
		    		}
		    	}

				try {
					// run animation
					DiagramAnimator diagramAnimator = DiagramAnimator.getAnimator();
					diagramAnimator.start(machine, statemachines, root, bmsFiles);
					// switch to ProB perspective
					IPerspectiveDescriptor perspective = HandlerUtil
							.getActiveWorkbenchWindow(event)
							.getWorkbench()
							.getPerspectiveRegistry()
							.findPerspectiveWithId("de.prob.ui.perspective");
					if (diagramAnimator.isRunningBMotionStudio()){
						// switch to BMotionStudio perspective
						IPerspectiveDescriptor bmsPerspective = PlatformUI
								.getWorkbench().getPerspectiveRegistry()
								.findPerspectiveWithId("de.bmotionstudio.gef.editor.perspective");
						if (bmsPerspective != null) perspective = bmsPerspective;
					}
					if (perspective != null) {
						activeEditor.getSite().getPage().setPerspective(perspective);
						activeEditor.getSite().getPage().setEditorAreaVisible(true);
					}
				} catch (ProBException e) {
					StatemachineAnimationPlugin.getDefault().getLog().log(
							new Status(IStatus.ERROR, StatemachineAnimationPlugin.PLUGIN_ID,
									"Animation startup failed for: " , e));
				}
			}
		}
		return null;
	}

	private static IEventBRoot getEventBRoot(EventBElement element){
		Resource resource = element.eResource();
		if (resource != null && resource.isLoaded()) {
			//TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(resource);
			IFile file = WorkspaceSynchronizer.getFile(resource);
			IRodinProject rodinProject = RodinCore.getRodinDB().getRodinProject(file.getProject().getName());
			IEventBRoot root = (IEventBRoot) rodinProject.getRodinFile(file.getName()).getRoot();
			return root;
		}
		return null;
   	}
	
}
