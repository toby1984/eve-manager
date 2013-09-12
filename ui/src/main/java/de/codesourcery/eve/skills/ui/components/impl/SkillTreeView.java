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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.IComponent;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.ITreeFilter;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.util.IStatusCallback;

public class SkillTreeView extends AbstractComponent implements ActionListener,ICharacterSelectionProviderAware {

	private final JToolBar skillTreeToolbar = new JToolBar();

	private final JCheckBox renderSkillpoints=
		new JCheckBox("Render current skillpoints ?", false );	

	private final JCheckBox showPrerequisites =
		new JCheckBox("Show prerequisites", true );

	private final JRadioButton showTrainableSkills = 
		new JRadioButton ("Show trained and trainable skills ?", false );

	private final JRadioButton showOnlyTrainedSkills = 
		new JRadioButton ("Show only trained skills ?", false );

	private final JRadioButton showAllSkills = 
		new JRadioButton ("Show all skills ?", true );

	// ================================= data ============================

	private ISelectionProvider<ICharacter> provider;
	private SkillTreeComponent skillTreeComponent;

	private volatile ICharacter selectedCharacter;

	private final ISelectionListener<ICharacter> listener = new ISelectionListener<ICharacter>() {

		@Override
		public void selectionChanged(ICharacter selected) {
			SkillTreeView.this.selectionChanged( selected );
		}
	};
	
	@Override
	protected void onAttachHook(IComponentCallback callback) {
		if ( this.provider != null ) {
			this.provider.addSelectionListener( listener );
			this.selectedCharacter = this.provider.getSelectedItem();
		}
	}
	
	@Override
	protected void onDetachHook() {
		if ( this.provider != null ) {
			this.provider.removeSelectionListener( listener );
		}
	}

	public void setSelectionProvider(ISelectionProvider<ICharacter> provider) {
		assertDetached();
		if ( this.provider != null ) {
			this.provider.removeSelectionListener( listener );
		}
		this.provider = provider;
	}

	public void setSkillTreeComponent(SkillTreeComponent comp) {
		this.skillTreeComponent = comp;
	}

	@Override
	protected void disposeHook() {
		if ( this.skillTreeComponent != null ) {
			this.skillTreeComponent.dispose();
		}
		if ( this.provider != null ) {
			this.provider.removeSelectionListener( listener );
		}
	}

	protected void selectionChanged(ICharacter selected) {
		selectedCharacter = selected;
	}

	protected JPanel createPanel() {

		final JPanel contentPanel = new JPanel();

		contentPanel.setLayout( new GridBagLayout() );

		// add toolbar panel
		renderSkillpoints.addActionListener( this );
		showPrerequisites.addActionListener( this );
		showAllSkills.addActionListener( this );
		showOnlyTrainedSkills.addActionListener( this );
		showTrainableSkills.addActionListener( this );

		ButtonGroup group = new ButtonGroup();
		group.add( showOnlyTrainedSkills );
		group.add( showTrainableSkills );
		group.add( showAllSkills );

		skillTreeToolbar.add( renderSkillpoints );
		skillTreeToolbar.add( showPrerequisites );
		skillTreeToolbar.add( showOnlyTrainedSkills );
		skillTreeToolbar.add( showTrainableSkills );
		skillTreeToolbar.add( showAllSkills );

		skillTreeToolbar.setFloatable( false );
		contentPanel.add( skillTreeToolbar , 
				constraints(0,0 ).noResizing().end() );

		// add skill tree component
		skillTreeComponent.setRenderCurrentSkillPoints( this.renderSkillpoints.isSelected() );
		this.skillTreeComponent.setSelectionProvider( provider );
		this.skillTreeComponent.onAttach( new IComponentCallback() {

			@Override
			public void dispose(IComponent caller) {
			}

			@Override
			public IStatusCallback getStatusCallback()
			{
				return getComponentCallback().getStatusCallback();
			}
		});	

		final JPanel treePanel = this.skillTreeComponent.getPanel();
		contentPanel.add( treePanel , 
				constraints( 0,1 ).weightX(1.0).weightY(1.0).resizeBoth().end() );

		return contentPanel;
	}

	protected SkillTreeComponent getSkillTreeComponent() {
		return skillTreeComponent;
	}

	@Override
	public void actionPerformed(ActionEvent event) 
	{
		if ( event.getSource() == renderSkillpoints ) {
			skillTreeComponent.setRenderCurrentSkillPoints( this.renderSkillpoints.isSelected() );	
		} else {
			updateViewFilter();
		}
	}

	private final class TrainedSkillsFilter implements ITreeFilter {

		@Override
		public boolean isHidden(ITreeNode node) {
			
			final ICharacter character = selectedCharacter;
			final Skill skill = getSkill( node );
			
			if ( skill != null && character != null ) {
				return ! selectedCharacter.hasSkill( skill );
			}
			return false;				
		}
	}

	private final class TrainableSkillsFilter implements ITreeFilter {

		@Override
		public boolean isHidden(ITreeNode node) {

			final ICharacter character = selectedCharacter;
			final Skill skill = getSkill( node );
			if ( skill != null && character != null) {
				if ( character.hasSkill( skill ) || character.canTrainSkill( skill ) )
				{
					return false;
				}
				return true;
			}
			return false;				
		}
	}
	
	private final class PrerequisiteFilter implements ITreeFilter {

		@Override
		public boolean isHidden(ITreeNode node) {
			return node.getValue() instanceof Prerequisite;
		}
	}

	protected static Skill getSkill(ITreeNode node) {
		final Object o = node.getValue();
		if ( o instanceof Skill) {
			return (Skill) o;
		}
		return null;
	}
	
	private static final class CombiningFilter implements ITreeFilter {

		private final List<ITreeFilter> filters = new ArrayList<ITreeFilter>();
		
		public CombiningFilter() {
		}
		
		@Override
		public String toString() {
			return "CombiningFilter[ "+filters+" ]";
		}
		
		private void addFilter(ITreeFilter filter) {
			if (filter == null) {
				throw new IllegalArgumentException("filter cannot be NULL");
			}
			this.filters.add( filter );
		}
		
		@Override
		public boolean isHidden(ITreeNode node) {
			for ( ITreeFilter f : filters ) {
				if ( f.isHidden( node ) ) {
					return true;
				}
			}
			return false;
		}
		
	}

	private void updateViewFilter() {

		final CombiningFilter filter =
			new CombiningFilter();
		
		if ( showAllSkills.isSelected() ) {
			// ok
		} else if ( showOnlyTrainedSkills.isSelected() ){
			filter.addFilter( new TrainedSkillsFilter() );
		} else if ( showTrainableSkills.isSelected() ) {
			filter.addFilter( new TrainableSkillsFilter() );
		} else {
			throw new RuntimeException("Unreachable code reached ?!");
		}

		if ( !showPrerequisites.isSelected() ) {
			filter.addFilter( new PrerequisiteFilter() );
		} 
		
		skillTreeComponent.setViewFilter( filter );
	}

}
