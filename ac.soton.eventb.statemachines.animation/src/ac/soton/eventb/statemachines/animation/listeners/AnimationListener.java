/**
 * Copyright (c) 2011 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.TranslationKind;
import ac.soton.eventb.statemachines.animation.DiagramAnimator;
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
	public static List<TransactionalEditingDomain> editingDomains = new ArrayList<TransactionalEditingDomain>();
	@Override
	public void currentStateChanged(State currentState, Operation operation) {
		DiagramAnimator animator = DiagramAnimator.getAnimator();
		
		if (animator.isRunning()) {
			//TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(machine.eResource()); //getEditingDomain(machine);
			for(Statemachine statemachine : animator.getRootStatemachines()){	
				TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(statemachine);
				Map<String, Variable> variables = currentState.getValues();
				List<Operation> operations = currentState.getEnabledOperations();
				Map<String, EList<Operation>> enabledOperations = new HashMap<String, EList<Operation>>();
				Set<String> activeStates = new HashSet<String>();
				CompoundCommand cc = new CompoundCommand();
				// retrieve translation type
				TranslationKind type = statemachine.getTranslation();
			
				// retrieve state information from proB
				// map of operation names and executable ProB operations
				for (Operation op : operations) {
					if (!enabledOperations.containsKey(op.getName()))
						enabledOperations.put(op.getName(), new BasicEList<Operation>());
					enabledOperations.get(op.getName()).add(op);
				}
				
				// only enumeration or refined enumeration translation: retrieve a set of active states for all statemachine variables			
				if (type == TranslationKind.SINGLEVAR) {
					// add state of root statemachine
					if (variables.containsKey(statemachine.getName()))
						activeStates.add(variables.get(statemachine.getName()).getValue());
					
					// add states of all nested statemachines
					for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true)) {
						if (object == null)
							continue;
						String statemachineName = ((EventBNamed) object).getName();
						if (variables.containsKey(statemachineName))
							activeStates.add(variables.get(statemachineName).getValue());
					}
				}
				else if (type == TranslationKind.REFINEDVAR) {
					//find refinement level of this machine
					Machine m = animator.getMachine();
					int refinementLevel = 0;
					while(m.getRefines().size() != 0){
						m = m.getRefines().get(0);
						refinementLevel++;
					}
					// add state of root statemachine
					if (variables.containsKey((statemachine.getName() + "_" + refinementLevel))){
						for(Variable variable : variables.values()){
							if(variable.getValue().equals(variables.get(statemachine.getName() + "_" + refinementLevel).getValue()) && !variable.getIdentifier().equals(statemachine.getName() + "_" + refinementLevel))
								activeStates.add(variable.getIdentifier());
						}
					}
					// add states of all nested statemachines
					for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true)) {
						if (object == null)
							continue;
						String statemachineName = ((EventBNamed) object).getName() + "_" + refinementLevel;
						if (variables.containsKey(statemachine))
							activeStates.add(variables.get(statemachineName).getValue());
					}
				}
				
				// update states
				for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATE, true)) {
					if (object == null)
						continue;
					String name = ((ac.soton.eventb.statemachines.State) object).getName();
					if (type == TranslationKind.MULTIVAR) {
						Variable stateStatusVar = variables.get(name);
						cc.append(SetCommand.create(editingDomain, object,
								StatemachinesPackage.Literals.STATE__ACTIVE, 
								stateStatusVar != null && Boolean.valueOf(stateStatusVar.getValue()).booleanValue()));
					} else if (type == TranslationKind.SINGLEVAR || type == TranslationKind.REFINEDVAR) {
						cc.append(SetCommand.create(editingDomain, object,
								StatemachinesPackage.Literals.STATE__ACTIVE, 
								activeStates.contains(name)));
					}
				}
				
				//update states so we know which are active below
				editingDomain.getCommandStack().execute(cc);
				cc = new CompoundCommand();
				
				for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.TRANSITION, true)) {
					if (object == null) continue;
					Transition transition = (Transition) object;
					// collect enabled operations
					EList<Operation> ops = new BasicEList<Operation>();
					for (Event event : transition.getElaborates()) {
						if (enabledOperations.containsKey(event.getName()))
							ops.addAll(enabledOperations.get(event.getName()));
					}
					// set operations
					cc.append(SetCommand.create(editingDomain, transition, StatemachinesPackage.Literals.TRANSITION__OPERATIONS, ops));
				}	
				
				editingDomain.getCommandStack().execute(cc);
				
			}	
		}
	}
}

