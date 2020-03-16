package ac.soton.eventb.statemachines.animation.participants;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eventb.core.IMachineRoot;

import ac.soton.eventb.probsupport.IAnimationParticipant;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.animation.DiagramAnimator;
import ac.soton.eventb.statemachines.animation.actions.AnimateAction;
import ac.soton.eventb.statemachines.diagram.part.StatemachinesDiagramEditor;

public class StatemachineAnimationParticipant implements IAnimationParticipant {

	List<StatemachinesDiagramEditor> editors; // = new ArrayList<StatemachinesDiagramEditor>();
	
	@Override
	public void startAnimating(IMachineRoot mchRoot) {
		DiagramAnimator animator = DiagramAnimator.getAnimator();
		List<Statemachine> statemachines = new ArrayList<Statemachine>();
		editors = new ArrayList<StatemachinesDiagramEditor>();
		//Find all the statemachines that are open as diagrams 
		// (these must come from the editors as each editor has a different local copy)
		//for(IWorkbenchPage page : HandlerUtil.getActiveWorkbenchWindow(event).getPages()){
		for(IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages()){
	    	for(IEditorReference editorRef: page.getEditorReferences()){
	    		IEditorPart editor = editorRef.getEditor(true);
				if (editor instanceof StatemachinesDiagramEditor ){
					Statemachine statemachine = (Statemachine) ((StatemachinesDiagramEditor)editor).getDiagram().getElement();
					if (mchRoot.equals(AnimateAction.getEventBRoot(statemachine))) {
		    			if (editor.isDirty()){
		    				editor.doSave(new NullProgressMonitor());
		    			}
	    				statemachines.add(statemachine);
	    				animator.addStatemachine(statemachine);
	    				//let the editor know that we are animating so that it doesn't try to save animation artifacts
			    		((StatemachinesDiagramEditor)editor).startAnimating();
			    		editors.add((StatemachinesDiagramEditor)editor);
			    		
					}
		    	}
	    	}
    	}

//		try {
//			// run animation
//			DiagramAnimator.getAnimator().start(machine, statemachines, mchRoot, Collections.EMPTY_LIST);
//		} catch (ProBException e) {
//			for (StatemachinesDiagramEditor editor : editors){
//				editor.stopAnimating();
//			}
//			StatemachineAnimationPlugin.getDefault().getLog().log(
//					new Status(IStatus.ERROR, StatemachineAnimationPlugin.PLUGIN_ID,
//							"Animation startup failed for: " , e));
//		}

	}
	
	@Override
	public void stopAnimating(IMachineRoot mchRoot) {
		for (StatemachinesDiagramEditor editor : editors){
			editor.stopAnimating();
		}
		DiagramAnimator.getAnimator().reset();
	}

	@Override
	public void updateAnimation(IMachineRoot mchRoot) {
		// TODO use this instead of the ProB animation listener
	}
}
