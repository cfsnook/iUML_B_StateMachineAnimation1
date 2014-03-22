/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.core.IEventBRoot;
import org.eventb.emf.core.machine.Machine;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.diagram.part.StatemachinesDiagramEditorPlugin;
import ac.soton.eventb.statemachines.diagram.preferences.custom.IStatemachinesPreferenceConstants;
import de.bmotionstudio.gef.editor.Animation;
import de.bmotionstudio.gef.editor.BMotionEditorPlugin;
import de.bmotionstudio.gef.editor.BMotionStudioEditor;
import de.bmotionstudio.gef.editor.model.Visualization;
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
	private Boolean savePref = null;
	private boolean bms = false;
	
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
	public void start(Machine machine, List<Statemachine> rootStatemachines, IEventBRoot root, List<IFile> bmsFiles) throws ProBException {
		this.machine = machine;
		this.rootStatemachines = rootStatemachines;
		//this.root = root;
		System.out.println("Starting ProB for " + machine);
		//turn off autosave
		if (savePref ==  null){
			savePref = StatemachinesDiagramEditorPlugin.getInstance()
					.getPreferenceStore().getBoolean(IStatemachinesPreferenceConstants.PREF_AUTOSAVE_ON_DEACTIVATE);
		}
		StatemachinesDiagramEditorPlugin.getInstance().getPreferenceStore().setValue(IStatemachinesPreferenceConstants.PREF_AUTOSAVE_ON_DEACTIVATE, false);
		// start ProB
		Animator probAnimator = Animator.getAnimator();
		LoadEventBModelCommand.load(probAnimator, root);
		
		bms = false;
		// if BMotionStudio files supplied, run them.
		for (IFile bmsFile : bmsFiles){
			bms = runBMotionStudio(bmsFile, probAnimator);
		}
	}
	
	
	public void stop() {
		machine = null;
		rootStatemachines.clear();
		//restore autosave preference
		if (savePref!=null){
			StatemachinesDiagramEditorPlugin.getInstance().getPreferenceStore().setValue(IStatemachinesPreferenceConstants.PREF_AUTOSAVE_ON_DEACTIVATE, savePref);
		}
		savePref = null;
		bms = false;
	}

	/**
	 * @return
	 */
	public boolean isRunning() {
		return Animator.getAnimator().isRunning() && machine != null;
	}

	public boolean isRunningBMotionStudio(){
		return bms;
	}
	
	/**
	 * 
	 * @param bmsFile
	 * @param animator
	 * @return 
	 */
	private boolean runBMotionStudio(IFile bmsFile, Animator animator){
		Visualization visualization;
		try {
			visualization = createVisualizationRoot(bmsFile);
			Animation animation = new Animation(animator, visualization);
			BMotionStudioEditor bmsEditor = getBmotionStudioEditor(bmsFile);
			bmsEditor.createRunPage(visualization, animation);
			return true;
		} catch (CoreException e) {
			StatemachineAnimationPlugin.logError("Eclipse Core Exception while attempting to launch BMotion Studio", e);
		} catch (IOException e) {
			StatemachineAnimationPlugin.logError("IO Exception while attempting to launch BMotion Studio", e);
		} catch (ParserConfigurationException e) {
			StatemachineAnimationPlugin.logError("Parser Configuration Exception while attempting to launch BMotion Studio", e);
		} catch (SAXException e) {
			StatemachineAnimationPlugin.logError("SAX Exception while attempting to launch BMotion Studio", e);
		}
		return false;

	}
	
	/**
	 * given a BMotionStudio bms file, opens it in an editor and returns the editor
	 * @param bmsFile
	 * @return BMotionStudioEditor
	 * @throws PartInitException
	 */
	private BMotionStudioEditor getBmotionStudioEditor(IFile bmsFile) throws PartInitException{
	    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(bmsFile.getName());
		IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new FileEditorInput(bmsFile), desc.getId());
	    if (part instanceof BMotionStudioEditor) {
	        return (BMotionStudioEditor) part;
	    }else{
	    	return null;
	    }
}
	
	/**
	 * Return a visualisation object for the given BMotion Studio bms file
	 * 
	 * @param bmsFile
	 * @return Visualization
	 * @throws CoreException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
    private Visualization createVisualizationRoot(IFile bmsFile) throws CoreException, IOException, ParserConfigurationException, SAXException {
            XStream xstream = new XStream(new DomDriver()) {
                    @Override
                    protected MapperWrapper wrapMapper(MapperWrapper next) {
                            return new MapperWrapper(next) {                      
                            		@Override
                                    public boolean shouldSerializeMember(@SuppressWarnings("rawtypes") Class definedIn,String fieldName) {
                                            if (definedIn == Object.class) {
                                                    return false;
                                            }
                                            return super.shouldSerializeMember(definedIn, fieldName); 
                                    }
                            };
                    }
            };
            BMotionEditorPlugin.setAliases(xstream);
            Visualization visualization = (Visualization) xstream.fromXML(bmsFile.getContents());
            visualization.setProjectFile(bmsFile);
            visualization.setIsRunning(true);
            return visualization;
    }
    
}
