/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.statemachines.AbstractState;
import ac.soton.eventb.statemachines.DiagramRoot;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.animation.DiagramAnimator;

/**
 * Stops diagram animation with ProB.
 * 
 * @author vitaly
 *
 */
public class StopAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
		if (editor instanceof IDiagramWorkbenchPart) {
			IDiagramWorkbenchPart diagramEditor = (IDiagramWorkbenchPart) editor;
			EObject element = diagramEditor.getDiagram().getElement();
			Resource resource = element.eResource();
			
			// find root diagram element
			EObject root = element;
			for (; root.eContainer() instanceof Machine == false; root = root.eContainer());
			
			// stop if running on root
			DiagramAnimator animator = DiagramAnimator.getAnimator();
			if (animator.isRunningFor(root)) {
				animator.stop();
				
				// clear animation artifacts
				TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(resource);
				if (editingDomain != null && !editingDomain.isReadOnly(resource)) {
					DiagramRoot diagramRoot = (DiagramRoot) root;
					CompoundCommand cc = new CompoundCommand();
					
					// clear active states
					for (EObject object : diagramRoot.getAllContained(StatemachinesPackage.Literals.ABSTRACT_STATE, true)) {
						if (object != null && ((AbstractState) object).isActive())
							cc.append(SetCommand.create(editingDomain, object,
								StatemachinesPackage.Literals.ABSTRACT_STATE__ACTIVE, 
								SetCommand.UNSET_VALUE));
					}
					
					// clear enabled transitions
					for (EObject object : diagramRoot.getAllContained(StatemachinesPackage.Literals.TRANSITION, true)) {
						if (object != null && ((Transition) object).getOperations() != null && !((Transition) object).getOperations().isEmpty())
							cc.append(SetCommand.create(editingDomain, object,
								StatemachinesPackage.Literals.TRANSITION__OPERATIONS,
								SetCommand.UNSET_VALUE));
					}
					
					editingDomain.getCommandStack().execute(cc);
					
					// switch to Event-B perspective
					IPerspectiveDescriptor perspective = HandlerUtil
							.getActiveWorkbenchWindow(event)
							.getWorkbench()
							.getPerspectiveRegistry()
							.findPerspectiveWithId("org.eventb.ui.perspective.eventb");
					if (perspective != null)
						editor.getSite().getPage().setPerspective(perspective);
				}
			}
		}
		return null;
	}

}
