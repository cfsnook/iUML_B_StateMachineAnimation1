/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eventb.core.IMachineRoot;

import de.prob.core.Animator;
import de.prob.core.command.LoadEventBModelCommand;
import de.prob.exceptions.ProBException;

/**
 * @author vitaly
 *
 */
public class DiagramAnimator {

	private static DiagramAnimator animator;
	private IMachineRoot machine;
	private EObject root;

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
	public EObject getRoot() {
		return root;
	}

	/**
	 * @return
	 */
	public IMachineRoot getMachine() {
		return machine;
	}

	/**
	 * @throws ProBException 
	 * 
	 */
	public void start(IMachineRoot machine, EObject root) throws ProBException {
		this.machine = machine;
		this.root = root;
		
		// start ProB
		Animator probAnimator = Animator.getAnimator();
		LoadEventBModelCommand.load(probAnimator, machine);
	}
	
	public void stop() {
		machine = null;
		root = null;
	}

	/**
	 * @return
	 */
	public boolean isRunning() {
		boolean targetSet = machine != null && root != null
				&& root.eResource() != null && root.eResource().isLoaded();
		return Animator.getAnimator().isRunning() && targetSet;
	}

	/**
	 * @param root2
	 * @return
	 */
	public boolean isRunningFor(EObject root2) {
		return isRunning() && EcoreUtil.getURI(root).equals(EcoreUtil.getURI(root2));
	}
	
}
