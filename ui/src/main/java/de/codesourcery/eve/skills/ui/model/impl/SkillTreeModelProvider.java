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
package de.codesourcery.eve.skills.ui.model.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.db.dao.ISkillTreeDAO;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.SkillGroup;
import de.codesourcery.eve.skills.ui.model.DefaultTreeModel;
import de.codesourcery.eve.skills.ui.model.DefaultTreeNode;
import de.codesourcery.eve.skills.ui.model.ITreeFilter;
import de.codesourcery.eve.skills.ui.model.ITreeModel;
import de.codesourcery.eve.skills.ui.model.ITreeModelProvider;
import de.codesourcery.eve.skills.ui.model.ITreeNode;

public class SkillTreeModelProvider implements ITreeModelProvider {

	private final Object LOCK = new Object();
	
	private static final Comparator<ITreeNode> SORTER =
		new TreeSorter();
	
	private final ISkillTreeDAO provider;
	private ITreeModel model;
	
	public SkillTreeModelProvider(ISkillTreeDAO provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider cannot be NULL");
		}
		this.provider = provider;
	}
	
	@Override
	public ITreeModel getTreeModel(ITreeFilter filter) {
		synchronized (LOCK) {
			if ( model == null ) {
				final ITreeNode rootNode =
					createTree( filter );
				model = new DefaultTreeModel();
				model.setRoot( rootNode );
				model.sortChildren( rootNode , SORTER , true );
			}
		}
		return model;
	}

	protected ITreeNode createTree(ITreeFilter filter) {
		
		final ITreeNode root = new DefaultTreeNode();
		
		final List<SkillGroup> categories = 
			new ArrayList<SkillGroup>( provider.getSkillTree().getSkillGroups() );
		
		for ( SkillGroup cat : categories ) {
			
			final DefaultTreeNode catNode = 
				new DefaultTreeNode( cat ); 
		
			if ( filter != null && filter.isHidden( catNode ) ) {
				continue;
			}
			root.addChild( catNode );
			
			for ( Skill s : cat.getSkills() ) {
				addSkill( new HashSet<Skill>() , filter , catNode , s );
			}
		}
		
		return root;
	}
	
	private void addSkill(Set<Skill> visited , ITreeFilter filter , ITreeNode parent,Skill s) {
		
		if ( ! s.isPublished() || visited.contains( s ) ) {
			return;
		}
		
		visited.add( s );
		
		ITreeNode skillNode = new DefaultTreeNode( s );
		
		if ( filter != null && filter.isHidden( skillNode ) ) {
			return;
		}
		
		Object parentValue = parent.getValue();
		Prerequisite parentReq = null;
		if ( parentValue != null && parentValue instanceof Prerequisite) {
			parentReq = (Prerequisite ) parentValue;
			if ( ! parentReq.getSkill().isPublished() ) {
				return;
			}
			if ( parentReq.getSkill().equals( s ) ) {
				skillNode = parent;
			} else {
				parent.addChild( skillNode );
			}
		} else {
			parent.addChild( skillNode );
		}
		
		for ( Prerequisite r : s.getPrerequisites() ) {

			if ( ! r.getSkill().isPublished() ) {
				continue;
			}
			final DefaultTreeNode preReqNode = new DefaultTreeNode( r );
			
			if ( filter != null && filter.isHidden( preReqNode ) ) {
				continue;
			}
			
			if ( parentReq != null && r.getSkill().equals( parentReq.getSkill() ) ) {
				continue;
			}
			
			skillNode.addChild( preReqNode );
			
			if ( r.getSkill().hasPrerequisites() ) { // recurse
				addSkill( visited , filter , preReqNode , r.getSkill() );
			}
		}
	}
	
	private static final class TreeSorter implements Comparator<ITreeNode> {

		@Override
		public int compare(ITreeNode n1, ITreeNode n2) {
			if ( ! n1.hasValue() || ! n2.hasValue() ) {
				if ( ! n1.hasValue() && n2.hasValue() ) {
					return -1;
				} else if ( n1.hasValue() && ! n2.hasValue() ) {
					return 1;
				}
				return 0;
			}
			
			// both have a value
			final Object v1 = n1.getValue();
			final Object v2 = n2.getValue();
			
			if ( v1 instanceof SkillGroup) {
				return ((SkillGroup) v1).getName().compareTo( ((SkillGroup) v2).getName() );
			} else if ( v1 instanceof Skill ) {
				return ((Skill) v1).getName().compareTo( ((Skill) v2).getName() );
			}
			
			return 0;
		}
		
	}

}
