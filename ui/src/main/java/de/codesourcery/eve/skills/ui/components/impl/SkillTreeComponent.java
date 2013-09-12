/**
 * Copyright 2004-2009 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.eve.skills.ui.components.impl;

import java.awt.GridBagLayout;

import javax.annotation.Resource;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.db.dao.ISkillTreeDAO;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.ITreeFilter;
import de.codesourcery.eve.skills.ui.model.ITreeModel;
import de.codesourcery.eve.skills.ui.model.ITreeModelProvider;
import de.codesourcery.eve.skills.ui.model.impl.SkillTreeModelProvider;
import de.codesourcery.eve.skills.ui.renderer.SkillTreeRenderer;

public class SkillTreeComponent extends AbstractComponent implements ISelectionListener<ICharacter> {

	public static final Logger log = Logger.getLogger(SkillTreeComponent.class);
	
	private ISelectionProvider<ICharacter> characterProvider;
	
	@Resource(name="skilltree-provider")
	private ISkillTreeDAO skillTreeProvider;
	
	private SkillTreeRenderer renderer;
	private ITreeModel treeModel; 
	private ITreeModelProvider viewModelProvider;
	private ITreeFilter viewFilter;
	
	private final JTree tree = new JTree();
	
	private boolean renderCurrentSkillPoints = false; 
	
	@Override
	protected void onAttachHook(IComponentCallback callback) {
		ToolTipManager.sharedInstance().registerComponent( tree );
		this.characterProvider.addSelectionListener( this );
	}
	
	@Override
	protected void onDetachHook() {
		ToolTipManager.sharedInstance().unregisterComponent( tree );
		this.characterProvider.removeSelectionListener( this );
	}
	
	@Override
	protected JPanel createPanel() {
		
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		
//		tree.setRootVisible(false);
		setupTree( tree );
		
		final JScrollPane pane = new JScrollPane( tree );
		
		result.add( pane , constraints(0,0).resizeBoth().end() );

		return result;
	}
	
	@Override
	protected void disposeHook() {
		ToolTipManager.sharedInstance().unregisterComponent( tree );
		if ( this.characterProvider != null ) {
			this.characterProvider.removeSelectionListener( this );
		}
	}
	
	protected void setupTree(JTree tree) {
		renderer = new SkillTreeRenderer( skillTreeProvider , characterProvider );
		viewModelProvider = new SkillTreeModelProvider( skillTreeProvider );
		treeModel = viewModelProvider.getTreeModel( viewFilter );
		tree.setModel( treeModel );
		tree.setCellRenderer( renderer );
	}
	
	public void setRenderCurrentSkillPoints(boolean yesNo) {
		
		if ( yesNo == this.renderCurrentSkillPoints ) {
			return;
		}
		
		this.renderCurrentSkillPoints = yesNo;
		if ( renderer != null ) {
			renderer.setRenderCurrentSkillPoints( yesNo );
		}
		
		if ( hasState(ComponentState.ATTACHED ) ) {
			tree.getParent().repaint();
		}
	}

	public void setViewFilter(ITreeFilter filter) {
		this.viewFilter = filter;
		setupTree( tree );
	}
	
	public void setSelectionProvider(ISelectionProvider<ICharacter> characterProvider) {
		
		// make sure the component is either
		// in state NEW or DETACHED , onDetach()
		// and onAttach() take care of properly attaching/removing listeners
		assertDetached();
				
		if ( this.characterProvider != null ) {
			this.characterProvider.removeSelectionListener( this );
			// listener will be (re-)attached when onAttach() gets called
		}
		this.characterProvider = characterProvider;
	}

	@Override
	public void selectionChanged(ICharacter selected) {
		
		runOnEventThread( new Runnable() {

			@Override
			public void run() {
				if ( treeModel != null ) {
					treeModel.modelChanged();
				}
			}} );
	}
	

}
