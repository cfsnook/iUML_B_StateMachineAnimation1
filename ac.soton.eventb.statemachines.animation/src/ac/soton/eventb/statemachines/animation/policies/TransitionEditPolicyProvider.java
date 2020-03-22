/**
 * Copyright (c) 2010-2020 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.policies;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editpolicies.SelectionEditPolicy;
import org.eclipse.gmf.runtime.common.core.service.IOperation;
import org.eclipse.gmf.runtime.common.core.service.IProviderChangeListener;
import org.eclipse.gmf.runtime.diagram.ui.menus.PopupMenu;
import org.eclipse.gmf.runtime.diagram.ui.services.editpolicy.CreateEditPoliciesOperation;
import org.eclipse.gmf.runtime.diagram.ui.services.editpolicy.IEditPolicyProvider;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.viewers.LabelProvider;
import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.persistence.EventBEMFUtils;

import ac.soton.eventb.probsupport.AnimationManager;
import ac.soton.eventb.probsupport.data.Operation_;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.diagram.edit.parts.TransitionEditPart;
import ac.soton.eventb.statemachines.diagram.edit.parts.TransitionGhostEditPart;

/**
 * Edit policy that adds animation context menu to enabled transitions.
 * 
 * @author vitaly
 *
 */
public class TransitionEditPolicyProvider implements IEditPolicyProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.gmf.runtime.diagram.ui.services.editpolicy.IEditPolicyProvider#createEditPolicies(org.eclipse.gef.EditPart)
	 */
	@Override
	public void createEditPolicies(EditPart editPart) {
		editPart.installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new SelectionEditPolicy() {
			
			@Override
			protected void showSelection() {
				Transition transition = (Transition) ((View) getHost().getModel()).getElement();
				Machine machine = (Machine) transition.getContaining(MachinePackage.Literals.MACHINE);
				IMachineRoot mchRoot = EventBEMFUtils.getRoot(machine);

				// if animation running and operations available
				if (AnimationManager.isRunning(mchRoot)
						&& transition.getOperations() != null 
						&& !transition.getOperations().isEmpty()) {
					
					getHost().getViewer().deselectAll(); 	//de-select the transition ready for next interaction
					
					List<Operation_> enabledOperations = AnimationManager.getEnabledOperations(mchRoot);
					List<Operation_> operations = new ArrayList<Operation_>();
					EList<Event> events = transition.getElaborates();
					for (Operation_ op : enabledOperations){
						String opName = op.getName();
						for (Event ev : events){
							if (opName.equals(ev.getName()) ){
								operations.add(op);
							}	
						}
					}
					// show selection menu
					PopupMenu menu = new PopupMenu(operations, new LabelProvider() {

						@Override
						public String getText(Object element) {
							Operation_ operation = (Operation_) element;
							List<String> arguments = operation.getArguments();
							String text = operation.getName() +
								(arguments == null || arguments.isEmpty() ? "" : " " + arguments.toString());
							return text;
						}});
					menu.show(getHost().getViewer().getControl());
					Object operation = menu.getResult();
					
					// execute selected
					if (operation != null) {
						AnimationManager.executeOperation(mchRoot, (Operation_)operation, false);
					}
				}
			}
			
			@Override
			protected void hideSelection() {
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gmf.runtime.common.core.service.IProvider#provides(org.eclipse.gmf.runtime.common.core.service.IOperation)
	 */
	@Override
	public boolean provides(IOperation operation) {
		if (operation instanceof CreateEditPoliciesOperation) {
			EditPart editPart = ((CreateEditPoliciesOperation) operation).getEditPart();
			if (editPart instanceof TransitionEditPart ||
					editPart instanceof TransitionGhostEditPart)
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gmf.runtime.common.core.service.IProvider#addProviderChangeListener(org.eclipse.gmf.runtime.common.core.service.IProviderChangeListener)
	 */
	@Override
	public void addProviderChangeListener(IProviderChangeListener listener) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gmf.runtime.common.core.service.IProvider#removeProviderChangeListener(org.eclipse.gmf.runtime.common.core.service.IProviderChangeListener)
	 */
	@Override
	public void removeProviderChangeListener(IProviderChangeListener listener) {
	}

}
