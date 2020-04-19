/**
 * Copyright (c) 2020-2020 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.participants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.EventBNamed;
import org.eventb.emf.core.machine.Event;

import ac.soton.eventb.probsupport.AnimationManager;
import ac.soton.eventb.probsupport.IAnimationParticipant;
import ac.soton.eventb.probsupport.data.Operation_;
import ac.soton.eventb.probsupport.data.State_;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.Transition;
import ac.soton.eventb.statemachines.TranslationKind;
import ac.soton.eventb.statemachines.diagram.part.StatemachinesDiagramEditor;

/**
 * This is the class that is registered with the prob support plugin
 * so that the statemachine animation can participate when an
 * animation is started.
 * 
 * @author cfsnook
 *
 */
public class StatemachineAnimationParticipant implements IAnimationParticipant {

	// map of the currently animated statemachine editors for each animated mchRoot
	private Map<IMachineRoot, List<StatemachinesDiagramEditor>> editorsMap = new HashMap<IMachineRoot, List<StatemachinesDiagramEditor>>();
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#startAnimating(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void startAnimating(IMachineRoot mchRoot) {
		String mchRootPath = mchRoot.getPath().toString();
		List<StatemachinesDiagramEditor> editors = new ArrayList<StatemachinesDiagramEditor>();
		//Find all the statemachines that are open as diagrams 
		// 		(these must come from the editors as each editor has a different local copy)
		for(IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages()){
	    	for(IEditorReference editorRef: page.getEditorReferences()){
	    		IEditorPart editor = editorRef.getEditor(true);
				if (editor instanceof StatemachinesDiagramEditor ){
					Statemachine statemachine = (Statemachine) ((StatemachinesDiagramEditor)editor).getDiagram().getElement();
					String smResourcePath = statemachine.eResource().getURI().toPlatformString(false);
					if (mchRootPath.equals(smResourcePath)) {
		    			if (editor.isDirty()){
		    				editor.doSave(new NullProgressMonitor());
		    			}
	    				//let the editor know that we are animating so that it doesn't try to save animation artifacts
			    		((StatemachinesDiagramEditor)editor).startAnimating();
			    		editors.add((StatemachinesDiagramEditor)editor);
					}
		    	}
	    	}
    	}
		editorsMap.put(mchRoot, editors);
	}
		updateAnimation(mchRoot);
	
	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#stopAnimating(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void stopAnimating(IMachineRoot mchRoot) {
		if (editorsMap.containsKey(mchRoot)) {
			for (StatemachinesDiagramEditor statemachineDiagramEditor : editorsMap.get(mchRoot)){
				clearAnimationArtifacts(statemachineDiagramEditor);
				statemachineDiagramEditor.stopAnimating();
			}
			editorsMap.remove(mchRoot);
		}
	}


	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.IAnimationParticipant#updateAnimation(org.eventb.core.IMachineRoot)
	 */
	@Override
	public void updateAnimation(IMachineRoot mchRoot) {	
		if (editorsMap.containsKey(mchRoot)) {
			for(StatemachinesDiagramEditor statemachineDiagramEditor : editorsMap.get(mchRoot)){					
				updateAnimationArtifacts(
						(Statemachine) statemachineDiagramEditor.getDiagram().getElement(),
						AnimationManager.getCurrentState(mchRoot), 
						AnimationManager.getEnabledOperations(mchRoot)
						);			
			}
		}
	}

	@Override
	public void restartAnimation(IMachineRoot mchRoot) {
		updateAnimation(mchRoot);
	}
	
	//////////////////////////////// private ///////////////////////////////
	
	/**
	 * Clears all animation data by un-setting the active states and transition operations attributes
	 * The resource is then set unmodified to prevent this appearing as an edit to the diagram
	 * 
	 * @param statemachine
	 */
	private void clearAnimationArtifacts(StatemachinesDiagramEditor statemachineDiagramEditor) {
		
		Diagram diagram = statemachineDiagramEditor.getDiagram();
		if (diagram == null) {		// the diagram may be closed by now
			return;
		}
		Statemachine statemachine = (Statemachine) diagram.getElement();
		TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(statemachine);
			
		CompoundCommand cc = new CompoundCommand();
		
		// clear active states
		for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATE, true)) {
			if (object != null) {
				cc.append(SetCommand.create(editingDomain, object,
					StatemachinesPackage.Literals.STATE__ACTIVE, 
					SetCommand.UNSET_VALUE));
				cc.append(SetCommand.create(editingDomain, object,
						StatemachinesPackage.Literals.STATE__ACTIVE_INSTANCES, 
						SetCommand.UNSET_VALUE));
			}
		}
		
		// clear enabled transitions
		for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.TRANSITION, true)) {
			if (object != null) { // && ((Transition) object).getOperations() != null && !((Transition) object).getOperations().isEmpty())
				cc.append(SetCommand.create(editingDomain, object,
					StatemachinesPackage.Literals.TRANSITION__OPERATIONS,
					SetCommand.UNSET_VALUE));
			}
		}
		editingDomain.getCommandStack().execute(cc);
		
		statemachine.eResource().setModified(false);
	}
	
	
	/**
	 * Updates the statemachines animation data attributes to indicate active states and enabled transitions
	 * 		(The diagram listeners will automatically update the diagram). 
	 * The resource is then set unmodified to prevent this appearing as an edit to the diagram.
	 * 
	 * @param statemachine
	 * @param currentState
	 * @param operations
	 */
	private void updateAnimationArtifacts(Statemachine statemachine, State_ currentState, List<Operation_> operations) {
		TransactionalEditingDomain editingDomain = TransactionUtil.getEditingDomain(statemachine);
		CompoundCommand cc = new CompoundCommand();

		// arrange enabled operations into a map of operation names to list of operation signatures
		Map<String, EList<Operation_>> enabledOperations = new HashMap<String, EList<Operation_>>();
		for (Operation_ op : operations) {
			if (!enabledOperations.containsKey(op.getName()))
				enabledOperations.put(op.getName(), new BasicEList<Operation_>());
			enabledOperations.get(op.getName()).add(op);
		}
		
		// map of active states - with instances currently in that state if lifted, or just TRUE if not lifted
		Map<String, Object> activeStates = getActiveStates(statemachine, currentState.getAllValues());
		
		boolean lifted = statemachine.getInstances()!=null;
		// update states
		for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATE, true)) {
			if (object == null) continue;
			String name = ((ac.soton.eventb.statemachines.State) object).getName();	
			EList<String> ins = new BasicEList<String>();					
			if (lifted){
				if (activeStates.get(name) instanceof String){
					ins.add((String)activeStates.get(name));
				}else{
					ins.add("\u2205");	// this may be already done by getActiveStates() but if not default to empty
				}
			}
			boolean active = activeStates.containsKey(name) && !"FALSE".equals(activeStates.get(name)) && !"\u2205".equals(activeStates.get(name));
			cc.append(SetCommand.create(editingDomain, object, StatemachinesPackage.Literals.STATE__ACTIVE_INSTANCES, ins));
			cc.append(SetCommand.create(editingDomain, object, StatemachinesPackage.Literals.STATE__ACTIVE, active));					
		}

		//update transitions so we know which are active below
		for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.TRANSITION, true)) {
			if (object == null) continue;
			Transition transition = (Transition) object;
			// collect enabled operations
			EList<Operation_> ops = new BasicEList<Operation_>();
			for (Event event : transition.getElaborates()) {
				if (enabledOperations.containsKey(event.getName()))
					ops.addAll(enabledOperations.get(event.getName()));
			}
			// set operations
			cc.append(SetCommand.create(editingDomain, transition, StatemachinesPackage.Literals.TRANSITION__OPERATIONS, ops));
		}	
		editingDomain.getCommandStack().execute(cc);
		statemachine.eResource().setModified(false);	//this was not an edit
	}
	
	/**
	 * Gets the active states as a Map from state name to active instances (or TRUE if not lifted)
	 * 
	 * @param animator
	 * @param statemachine
	 * @param variables
	 * @param type
	 * @return
	 */
	private Map<String,Object> getActiveStates(Statemachine statemachine, Map<String, String> variables) {
		//retrieve a map of active states to instances for all state-machine states	
		Map<String,Object> activeStates = new HashMap<String,Object>();
		TranslationKind type = statemachine.getTranslation();
		boolean lifted = statemachine.getInstances()!=null; 
		
		if (type == TranslationKind.SINGLEVAR) {
			// add state of root statemachine
			if (variables.containsKey(statemachine.getName())){
				String smValue = variables.get(statemachine.getName());
				if (lifted){
					activeStates.putAll(parseSmFn(smValue));					
				} else {
					activeStates.put(smValue, "TRUE");
				}
			}
			// add states of all nested statemachines
			for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true)) {
				if (object == null) continue;
				String statemachineName = ((EventBNamed) object).getName();
				if (variables.containsKey(statemachineName)) {
					String smValue = variables.get(statemachineName);
					if (lifted){
						activeStates.putAll(parseSmFn(smValue));					
					} else {
						activeStates.put(smValue, "TRUE");
					}
				}
			}
		}
		
		else if (type == TranslationKind.REFINEDVAR) {			//REFINEDVAR is no longer being supported - may remove at some time
			//find refinement level of this machine
//			Machine m = animator.getMachine();
//			int refinementLevel = 0;
//			while(m.getRefines().size() != 0){
//				m = m.getRefines().get(0);
//				refinementLevel++;
//			}
//			// add state of root statemachine
//			if (variables.containsKey((statemachine.getName() + "_" + refinementLevel))){
//				for(Variable variable : variables.values()){
//					if(variable.getValue().equals(variables.get(statemachine.getName() + "_" + refinementLevel).getValue()) && !variable.getIdentifier().equals(statemachine.getName() + "_" + refinementLevel))
//						activeStates.put(variable.getIdentifier(), "TRUE");
//				}
//			}
//			// add states of all nested statemachines
//			for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true)) {
//				if (object == null) continue;
//				String statemachineName = ((EventBNamed) object).getName() + "_" + refinementLevel;
//				if (variables.containsKey(statemachine))
//					activeStates.put(variables.get(statemachineName).getValue(), "TRUE");
//			}
//			
		} else if (type == TranslationKind.MULTIVAR) {
			for (EObject object : statemachine.getAllContained(StatemachinesPackage.Literals.STATE, true)) {
				if (object == null) continue;
				String stateName = ((ac.soton.eventb.statemachines.State) object).getName();	
				String stateStatusVar = variables.get(stateName);
				if (stateStatusVar != null){
					activeStates.put(stateName, stateStatusVar);					
				}
			}
		} else {
			//un-supported translation kind - do nothing
		}
		return activeStates;
	}

	/**
	 * Parses the input string to convert the string value of a function
	 * into a map from state names to a string representation of the set of instances in that state
	 * 
	 * ASSUMES that the state part is a simple string (i.e. contains no maplet).
	 * (the instance could be any type including maplets etc.)
	 * 
	 * @param smValue
	 * @return
	 */
	private Map<String, String> parseSmFn(String smValue) {
		Map<String,String> ret = new HashMap<String,String>();
		if (smValue.startsWith("{")) smValue = smValue.substring(1);
		if (smValue.startsWith("}",smValue.length()-1)) smValue = smValue.substring(0,smValue.length()-1);		
		String[] result = smValue.split(",");
	     for (int x=0; x<result.length; x++){
	 		if (result[x].startsWith("(")) result[x] = result[x].substring(1);
			if (result[x].startsWith(")",result[x].length()-1)) result[x] = result[x].substring(0,result[x].length()-1);
			int i = result[x].lastIndexOf("\u21a6");
			if (i<0) continue;
	    	String ins = result[x].substring(0,i);
	    	String stateName = result[x].substring(i+1);
	    	String instances = ret.get(stateName);
	    	if (instances==null){
	    		instances = "{"+ins+"}";
	    	}else{
	    		instances = instances.substring(0, instances.length()-1);
	    		instances = instances+","+ins+"}";
	    	}
	    	ret.put(stateName, instances);
	     }
		return ret;
	}
}
