/**
 * Copyright (c) 2020-2020 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.persistence.EventBEMFUtils;

import ac.soton.eventb.emf.diagrams.util.custom.DiagramUtils;
import ac.soton.eventb.probsupport.handlers.AnimationStopHandler;
import ac.soton.eventb.statemachines.Statemachine;

/**
 * Stops ProB animation via the ProB support plugin
 *  when the command is made from a statemachine diagram
 * 
 * @author cfs
 *
 */
public class StatemachineAnimationStopHandler extends AnimationStopHandler {

	/* (non-Javadoc)
	 * @see ac.soton.eventb.probsupport.handlers.AnimationStopHandler#getRoot(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected IMachineRoot getRoot(ExecutionEvent event) throws ExecutionException{
		
		IEditorPart activeEditor = HandlerUtil.getActiveEditorChecked(event);
		if (!(activeEditor instanceof DiagramEditor)) return null;

		EObject element = ((DiagramEditor)activeEditor).getDiagram().getElement();
		if (!(element instanceof Statemachine)) return null;

		Machine machine = (Machine) DiagramUtils.getTranslationTarget((Statemachine)element);
		
		return EventBEMFUtils.getRoot(machine);
   	}
	
}
