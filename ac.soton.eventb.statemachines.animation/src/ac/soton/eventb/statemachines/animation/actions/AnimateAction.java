/**
 * Copyright (c) 2011 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IMachineRoot;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;

import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.animation.DiagramAnimator;
import ac.soton.eventb.statemachines.animation.StatemachineAnimationPlugin;
import de.prob.exceptions.ProBException;

/**
 * Starts diagram animation with ProB.
 * 
 * @author vitaly
 *
 */
public class AnimateAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
		if (editor instanceof IDiagramWorkbenchPart) {
			IDiagramWorkbenchPart diagramEditor = (IDiagramWorkbenchPart) editor;
			EObject element = diagramEditor.getDiagram().getElement();
			Resource resource = element.eResource();
			
			if (element instanceof Statemachine && resource != null && resource.isLoaded()) {
				TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(resource);
				
				if (editingDomain != null && !editingDomain.isReadOnly(resource)) {
					// find root statemachine
					Statemachine root = (Statemachine) element;
					for (; root.eContainer() instanceof State && root.eContainer().eContainer() instanceof Statemachine; root = (Statemachine) root.eContainer().eContainer());
					
					// find rodin element
					IFile file = WorkspaceSynchronizer.getFile(resource);
					IRodinProject rodinProject = RodinCore.getRodinDB().getRodinProject(file.getProject().getName());
					IMachineRoot machine = (IMachineRoot) rodinProject.getRodinFile(file.getName()).getRoot();
					
					try {
						// run animation
						DiagramAnimator diagramAnimator = DiagramAnimator.getAnimator();
						diagramAnimator.start(machine, root);
						
						// switch to ProB perspective
						IPerspectiveDescriptor perspective = HandlerUtil
								.getActiveWorkbenchWindow(event)
								.getWorkbench()
								.getPerspectiveRegistry()
								.findPerspectiveWithId("de.prob.ui.perspective");
						if (perspective != null) {
							editor.getSite().getPage().setPerspective(perspective);
							editor.getSite().getPage().setEditorAreaVisible(true);
						}
					} catch (ProBException e) {
						StatemachineAnimationPlugin.getDefault().getLog().log(
								new Status(IStatus.ERROR, StatemachineAnimationPlugin.PLUGIN_ID,
										"Animation startup failed for: " + file.getFullPath().toString(), e));
					}
				}
			}
		}
		return null;
	}

}
