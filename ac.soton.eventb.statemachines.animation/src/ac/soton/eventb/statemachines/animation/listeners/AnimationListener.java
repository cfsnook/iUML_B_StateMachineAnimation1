/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eventb.emf.core.machine.Event;

import ac.soton.eventb.statemachines.AbstractState;
import ac.soton.eventb.statemachines.DiagramRoot;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.animation.DiagramAnimator;
import ac.soton.eventb.statemachines.animation.util.TranslationUtil;
import de.prob.core.IAnimationListener;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;
import de.prob.core.domainobjects.Variable;

/**
 * Diagram animation listener.
 * 
 * @author vitaly
 *
 */
public class AnimationListener implements IAnimationListener {

	@Override
	public void currentStateChanged(State currentState, Operation operation) {
		DiagramAnimator animator = DiagramAnimator.getAnimator();
		
		if (animator.isRunning()) {
			DiagramRoot diagramRoot = (DiagramRoot) animator.getRoot();
			TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(diagramRoot);
			
			// retrieve state information from proB
			Map<String, Variable> variables = currentState.getValues();
			List<Operation> operations = currentState.getEnabledOperations();
			
			// map of operation names and executable ProB operations
			Map<String, EList<Operation>> enabledOperations = new HashMap<String, EList<Operation>>();
			for (Operation op : operations) {
				if (!enabledOperations.containsKey(op.getName()))
					enabledOperations.put(op.getName(), new BasicEList<Operation>());
				enabledOperations.get(op.getName()).add(op);
			}
			
			CompoundCommand cc = new CompoundCommand();
			
			// update states
			for (EObject object : diagramRoot.getAllContained(StatemachinesPackage.Literals.ABSTRACT_STATE, true)) {
				if (object == null)
					continue;
				String name = TranslationUtil.getAbstractStateName((AbstractState) object);
				Variable stateStatusVar = variables.get(name);
				cc.append(SetCommand.create(editingDomain, object,
						StatemachinesPackage.Literals.ABSTRACT_STATE__ACTIVE, 
						stateStatusVar != null && Boolean.valueOf(stateStatusVar.getValue()).booleanValue()));
			}
			
			// update transitions
			for (EObject object : diagramRoot.getAllContained(StatemachinesPackage.Literals.TRANSITION, true)) {
				if (object == null)
					continue;
				Transition transition = (Transition) object;
				
				// collect enabled operations
				EList<Operation> ops = new BasicEList<Operation>();
				for (Event event : transition.getElaborates()) {
					if (enabledOperations.containsKey(event.getName()))
						ops.addAll(enabledOperations.get(event.getName()));
				}

				// set operations
				cc.append(SetCommand.create(editingDomain, transition,
						StatemachinesPackage.Literals.TRANSITION__OPERATIONS,
						ops));
			}
			
			editingDomain.getCommandStack().execute(cc);
		}
	}
	
}
