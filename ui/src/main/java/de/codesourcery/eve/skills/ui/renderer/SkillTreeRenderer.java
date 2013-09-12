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
package de.codesourcery.eve.skills.ui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.apache.commons.lang.StringUtils;

import sun.swing.DefaultLookup;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.datamodel.TrainedSkill;
import de.codesourcery.eve.skills.db.dao.ISkillTreeDAO;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.SkillGroup;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.utils.SkillLabel;
import de.codesourcery.eve.skills.ui.utils.SkillLabel.IRenderCallback;
import de.codesourcery.eve.skills.util.Misc;
import de.codesourcery.eve.skills.utils.DateHelper;

public class SkillTreeRenderer extends DefaultTreeCellRenderer {

	private static final Color GREEN = new Color( 0 , 150 , 0 );
	private static final boolean DEBUG = false;

	private ISelectionProvider<ICharacter> charProvider;
	private boolean renderCurrentSkillPoints = false;
	private final ISkillTreeDAO skillTreeDAO;

	public SkillTreeRenderer(ISkillTreeDAO dao , ISelectionProvider<ICharacter> charProvider) {
		if (charProvider == null) {
			throw new IllegalArgumentException("charProvider cannot be NULL");
		}
		if ( dao == null ) {
			throw new IllegalArgumentException("SkillTree DAO cannot be NULL");
		}
		
		this.skillTreeDAO = dao;
		this.charProvider = charProvider;
	}

	private final SkillLabel skillLabel = new SkillLabel();
	
	@Override
	public Component getTreeCellRendererComponent(final JTree tree, Object n,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) 
	{

		final ITreeNode node = (ITreeNode) n;
		final Object value = node.getValue();

		if ( value == null ) {

			super.getTreeCellRendererComponent(tree, n , sel, expanded, leaf,
					row, hasFocus);

			setText(null);
			return this;
		}

		final ICharacter character = charProvider.getSelectedItem();

		final String label; 
		if ( value instanceof Prerequisite ) {

			super.getTreeCellRendererComponent(tree, n , sel, expanded, leaf,
					row, hasFocus);

			final Prerequisite  r = (Prerequisite ) value;

			String sCurrentLvl="";
			if ( character.hasSkill( r.getSkill() ) ) 
			{
				final int currentLvl = character.getSkillLevel( r.getSkill() ).getLevel();
				if ( currentLvl < r.getRequiredLevel() ) {
					sCurrentLvl = " ( current: "+currentLvl+" )";
				}
			} 
			label = 
				"req: "+r.getSkill().getName()+" lvl "+r.getRequiredLevel()+sCurrentLvl;

			setForeground( getColorForSkill( r , character ) );
			setText( label );

			setToolTipText( toToolTip( character , r.getSkill() ) );

		} else if ( value instanceof SkillGroup) {

			super.getTreeCellRendererComponent(tree, n , sel, expanded, leaf,
					row, hasFocus);

			final SkillGroup cat = (SkillGroup) value;

			if ( character != null ) {

				final long current = 
					cat.getSkillpoints( character );

				final long maximumSkillpoints = 
					cat.getMaximumSkillpoints();

				final String percent =
					skillPointDeltaToString( current, maximumSkillpoints );

				label = cat.getName()+"          "+
				StringUtils.leftPad( percent , 30 - cat.getName().length() );
			} else {
				label = cat.getName();
			}
		}
		else if ( value instanceof Skill ) 
		{

			final Skill s = (Skill) value;

			final int skillLevel;
			final String skillPoints;
			TrainedSkill trained = null;
			if ( character != null ) {

				trained =
					character.getSkillLevel( s );

				final long current = trained.getSkillpoints();
				final long max = s.getMaximumSkillpoints();

				skillLevel = trained.getLevel();
				if ( renderCurrentSkillPoints ) {
					skillPoints = "        "+skillPointDeltaToString( current , max );
				} else {
					skillPoints="";
				}
			} else {
				skillLevel = 0;
				skillPoints="";
			}

			String partiallyTrained = "";
			if ( trained != null && skillLevel != Skill.MAX_LEVEL && trained.isPartiallyTrained() ) {
				final int nextLevel = skillLevel+1;
				int percent = Math.round( trained.getFractionOfLevelTrained( nextLevel ) );
				if ( percent == 100 ) {
					percent = 99;
				}
				partiallyTrained = " [ "+percent+"% of lvl "+nextLevel+" trained ]";
			}
			
			label =
				s.getName()+" (Rank "+s.getRank()+")"+skillPoints+partiallyTrained;

			IRenderCallback callback = new IRenderCallback() {
				
				@Override
				public Icon getClosedIcon() {
					return (Icon) DefaultLookup.get(tree, tree.getUI(), "Tree.closedIcon");
//					return DefaultLookup.getIcon(tree, tree.getUI(), "Tree.closedIcon");
				}

				@Override
				public Icon getLeafIcon() {
					return (Icon) DefaultLookup.get(tree, tree.getUI() , "Tree.leafIcon" );
//					return DefaultLookup.getIcon(tree, tree.getUI() , "Tree.leafIcon" );
				}

				@Override
				public Icon getOpenIcon() {
					return (Icon) DefaultLookup.get(tree, tree.getUI() , "Tree.openIcon" );
//					return DefaultLookup.getIcon(tree, tree.getUI() , "Tree.openIcon" );					
				}

				@Override
				public Color getTextNonSelectionColor() {
					return (Color) DefaultLookup.get(tree, tree.getUI() , "Tree.textForeground");
//					return DefaultLookup.getColor(tree, tree.getUI() , "Tree.textForeground");
				}

				@Override
				public Color getTextSelectionColor() {
					return (Color) DefaultLookup.get(tree, tree.getUI() , "Tree.selectionForeground");
//					return DefaultLookup.getColor(tree, tree.getUI() , "Tree.selectionForeground");
				}
				
				@Override
				public Color getBackgroundSelectionColor() {
					return (Color) DefaultLookup.get(tree, tree.getUI() , "Tree.selectionBackground");
//					return DefaultLookup.getColor(tree, tree.getUI() , "Tree.selectionBackground");
				}

				@Override
				public Color getBackgroundNonSelectionColor() {
					return (Color) DefaultLookup.get(tree, tree.getUI() , "Tree.textBackground");
//					return DefaultLookup.getColor(tree, tree.getUI() , "Tree.textBackground");
				}				
			};
				
			skillLabel.setText( label );
			skillLabel.setupTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus, callback);
			if ( trained != null ) {
				skillLabel.setTrainedSkill( trained );
			} else {
				skillLabel.setTrainedSkill( new TrainedSkill( s , 0 ) );
			}
			skillLabel.setForeground( getColorForSkill( s , character) );
			
			skillLabel.setToolTipText( toToolTip( character , s ) );

			return skillLabel;
		} else {
			label = null;
		}

		if ( DEBUG && value != null ) {
			final String[] className = value.getClass().getName().split("\\.");
			final String name = "["+className[ className.length -1 ]+"]";
			if ( label != null ) {
				setText( name+" "+label);
			} else {
				setText( name );
			}
		} else {
			setText( label);
		}

		return this;
	}

	private static final String skillPointDeltaToString(long current,long maximumSkillpoints) {

		final float percent = 100.0f* ( (float) current / (float) maximumSkillpoints );
		final NumberFormat FORMAT =
			new DecimalFormat("##0.00");

		if ( current > 0 ) {
			final String label = Skill.skillPointsToString( (int) current ) +" SP ( "+
			FORMAT.format( percent )+" % )";
			return label;
		}
		return "";
	}
	
	private final String toToolTip(ICharacter character , Skill skill) {
		final String desc = Misc.wrap( skill.getDescription() , "\n" , 55 );
		final String lvls = getTrainingTimes( character , skill );
		if ( ! lvls.equals("" ) ) {
			return toHTML( desc +"\n\n"+lvls );
		}
		return toHTML( desc );
	}
	
	private String getTrainingTimes(ICharacter character,Skill s) {
		
		if ( character == null ) {
			return "";
		}
		
		final SkillTree tree = this.skillTreeDAO.getSkillTree();
		
		final StringBuilder result = 
			new StringBuilder();
		
		for ( int lvl = 1 ; lvl <= Skill.MAX_LEVEL ; lvl++ ) {

			if ( character.hasSkill( s , lvl ) ) {
				continue;
			}
			
			final long durationInMillis =
				character.calcTrainingTime( tree  , s , lvl );
			
			result.append("To lvl "+lvl+" : ").append( DateHelper.durationToString(durationInMillis) );
			
			if ( (lvl+1) <= Skill.MAX_LEVEL ) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	private static final String toHTML(String s) {
		if ( s == null ) {
			return "<HTML></HTML>";
		}

		return "<HTML>"+s.replaceAll("\n" , "<BR />")+"</HTML>";
	}

	private Color getColorForSkill(Prerequisite r, ICharacter character) {

		final Skill s = r.getSkill();
		if ( character == null ) {
			return Color.RED;
		}

		if ( character.hasSkill( s , r.getRequiredLevel() ) ) {
			return GREEN;
		} else if ( character.canTrainSkill( s ) || character.hasSkill( s ) ) {
			return Color.ORANGE;
		}  
		return Color.RED;
	}

	private Color getColorForSkill(Skill  s, ICharacter character) {

		if ( character == null ) {
			return Color.RED;
		}

		if ( character.hasSkill( s ) ) {
			return GREEN;
		} else if ( character.canTrainSkill( s ) ) {
			return Color.ORANGE;
		} 
		return Color.RED;
	}

	public void setRenderCurrentSkillPoints(boolean yesNo) {
		this.renderCurrentSkillPoints = yesNo;
	}
	
}
