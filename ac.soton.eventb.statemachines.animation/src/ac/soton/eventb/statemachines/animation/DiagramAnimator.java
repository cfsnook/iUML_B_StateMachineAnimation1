/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IEventBRoot;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.diagram.part.StatemachinesDiagramEditorPlugin;
import ac.soton.eventb.statemachines.diagram.preferences.custom.IStatemachinesPreferenceConstants;
import de.prob.core.Animator;
import de.prob.core.command.LoadEventBModelCommand;
import de.prob.exceptions.ProBException;

/**
 * @author vitaly
 *
 */
public class DiagramAnimator {

	private static DiagramAnimator animator;
	private Machine machine;
	private List<Statemachine> rootStatemachines = new ArrayList<Statemachine>();
	private Boolean savePref = StatemachinesDiagramEditorPlugin.getInstance()
			.getPreferenceStore().getBoolean(IStatemachinesPreferenceConstants.PREF_AUTOSAVE_ON_DEACTIVATE);
	
	/**
	 * @return
	 */
	public static DiagramAnimator getAnimator() {
		if (animator == null)
			animator = new DiagramAnimator();
		return animator;
	}

	/**
	 * @return
	 */
	public List<Statemachine> getRootStatemachines() {
		return rootStatemachines;
	}

	/**
	 * @return
	 */
	public Machine getMachine() {
		return machine;
	}

	/**
	 * Starts the diagram animations including starting ProB animator for the given eventBRoot
	 * 
	 * rootStatemachines must be obtained from the elements of the open editors otherwise the diagrams will not update
	 * 
	 * 
	 * @throws ProBException 
	 * 
	 */
	public void start(Machine machine, List<Statemachine> rootStatemachines, IEventBRoot root) throws ProBException {
		this.machine = machine;
		this.rootStatemachines = rootStatemachines;
		//this.root = root;
		System.out.println("Starting ProB for " + machine);
		//turn off autosave
		if (savePref ==  null){
			StatemachinesDiagramEditorPlugin.getInstance().getPreferenceStore().setValue(IStatemachinesPreferenceConstants.PREF_AUTOSAVE_ON_DEACTIVATE, false);
		}
		// start ProB
		Animator probAnimator = Animator.getAnimator();
		LoadEventBModelCommand.load(probAnimator, root);
	}
	
	
	public void stop() {
		machine = null;
		rootStatemachines.clear();
		//restore autosave preference
		StatemachinesDiagramEditorPlugin.getInstance().getPreferenceStore().setValue(IStatemachinesPreferenceConstants.PREF_AUTOSAVE_ON_DEACTIVATE, savePref);
		savePref = null;
	}

	/**
	 * @return
	 */
	public boolean isRunning() {
		return Animator.getAnimator().isRunning() && machine != null;
	}

}
