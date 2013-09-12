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
package de.codesourcery.eve.skills.db.datamodel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.datamodel.TrainedSkill;

public class Skill {
	
	public static final int MAX_LEVEL = 5;

	private int typeId;
	private SkillGroup category;
	private String name;
	private String description;
	private int rank;
	private AttributeType primaryAttribute;
	private AttributeType secondaryAttribute;
	private Boolean isPublished; 
	private final List<Prerequisite> prerequisites = new ArrayList<Prerequisite>();
	
	private static final NumberFormat SKILLPOINTS_FORMAT = 
		new DecimalFormat("###,###,##0");
	
	public static String skillPointsToString(int skillpoints) {
		synchronized (SKILLPOINTS_FORMAT) {
			return SKILLPOINTS_FORMAT.format( skillpoints );
		}
	}

	public static Skill getRefiningSkill(SkillTree skillTree) {
		return skillTree.getSkill( 3385 );
	}

	public static Skill getRefineryEfficiencySkill(SkillTree skillTree) {
		return skillTree.getSkill( 3389 );
	}
	
	public static Skill getOreProcessingSkill(InventoryType type,
			SkillTree skillTree) 
	{
		return skillTree.getSkillByName( type.getName()+" Processing" );
	}

	public static Skill getScrapMetalProcessingSkill(SkillTree skillTree) {
 		return skillTree.getSkill( 12196);
	}
	
	public static Skill getConnectionsSkill(SkillTree skillTree)
	{
		return skillTree.getSkill( 3359 );
	}
	
	public static Skill getDiplomacySkill(SkillTree skillTree)
	{
		return skillTree.getSkill( 3357 );
	}
	
	public static Skill getProductionEfficiencySkill(SkillTree tree) {
		return tree.getSkill( 3388 );
	}
	
	public static Skill getResearchSkill(SkillTree tree) {
		return tree.getSkill( 3403 );
	}
	
	public static Skill getMetallurgySkill(SkillTree tree) {
		return tree.getSkill( 3409 );
	}
	
	public static Skill getIndustrySkill(SkillTree tree) {
		return tree.getSkill( 3380);
	}
	
	public static Skill getScienceSkill(SkillTree skillTree) {
		return skillTree.getSkill( 3402 );
	}
	
	public Skill(int typeId,Boolean isPublished) {
		setTypeId( typeId );
		this.isPublished = isPublished;
	}		
	
	public boolean canBeTrainedBy(ICharacter c) {
		
		if ( c.hasSkill( this ) ) {
			return false;
		}
		
		return _canBeTrainedBy( this , c );
	}
	
	public static boolean canBeTrainedBy(Skill skill , ICharacter c) {
		return skill.canBeTrainedBy( c );
	}
	
	private static boolean _canBeTrainedBy(Skill skill , ICharacter c) {
		
		for ( Prerequisite preRequisite : skill.prerequisites ) {
			
			if ( ! preRequisite.isMetBy( c ) ) {
				return false;
			}
			
		}
		return true;
	}	
	
	public long calcTrainingTime(SkillTree tree, ICharacter c , int toLevel) {
		
		if ( c.getCurrentLevel( this ) >= toLevel ) {
			return 0;
		}
		
		final long currentSP = 
			c.getCurrentSkillPoints( this );
		
		final long delta = getSkillpointsForLevel( toLevel ) - currentSP;
		
		/*
		( SP_Needed - Current_SP ) / ( Pri_Attrib + ( Sec_Attrib / 2 ) )
		*/
		
		final float primary =
			c.getAttributes().getValue( tree , getPrimaryAttribute() );
		
		final float secondary =
			c.getAttributes().getValue( tree , getSecondaryAttribute() );		
		
		final float minutes =
			delta / (  primary + ( secondary / 2.0f) );
		
		return Math.round( minutes * 60 );
	}
	
	@Override
	public String toString() {
		if ( isPublished() ) {
			return "Skill[ "+name+" (x"+rank+") , prereqs = "+prerequisites+" ]";
		}
		return "Skill[ (NOT PUBLISHED) "+name+" (x"+rank+") , prereqs = "+prerequisites+" ]";		
	}
	
	public boolean hasPrerequisites() {
		return ! prerequisites.isEmpty();
	}
	
	public Skill(int typeId , SkillGroup cat,boolean isPublished) {
		this(typeId,isPublished);
		setCategory(cat);
	}
	
	public void setCategory( SkillGroup cat) {
		if (cat == null) {
			throw new IllegalArgumentException("category cannot be NULL");
		}
		this.category = cat;
	}
	
	public void addPrerequisite(Prerequisite req) {
		prerequisites.add( req );
	}
	
	public List<Prerequisite> getPrerequisites() {
		return Collections.unmodifiableList( prerequisites );
	}

	@Override
	public boolean equals(Object obj) {
		
		if ( ! ( obj instanceof Skill) ) {
			return false;
		}
		
		final Skill s = (Skill) obj;
		return ObjectUtils.equals( this.typeId , s.typeId );
	}
	
	@Override
	public int hashCode() {
		return this.typeId;
	}

	public String getId() {
		return name;
	}

	public void setId(String id) {
		this.name = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("NULL/blank skill name ?");
		}
		this.name= name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
	
	public SkillGroup getCategory() {
		return category;
	}
	
	public int getLevelForSkillpoints(int skillpoints) {
		
		if ( skillpoints < 0 || skillpoints > getMaximumSkillpoints() ) {
			throw new IllegalArgumentException("Invalid skillpoints: "+skillpoints);
		}
		
		for ( int lvl = MAX_LEVEL ; lvl >= 1 ; lvl-- ) {
			if ( skillpoints >= getSkillpointsForLevel( lvl ) ) {
				return lvl;
			}
		}
		return 0;
	}
	
	public int getSkillpointsForLevel(int lvl) {
		
		int result;
		
		switch( lvl ) {
			case 0:
				return 0;
			case 1:
				result = 250;
				break;
			case 2: 
				result = 1414;
				break;
			case 3:
				result = 8000;
				break;
			case 4:
				result = 45255;
				break;
			case 5:
				result = 256000;
				break;
			default:
				throw new IllegalArgumentException("Invalid level "+lvl);
		}
		
		// may be triggered by 'disabled' skills in the data file
		if ( rank <= 0 ) {
			throw new RuntimeException("Internal error - skill "+this+" has invalid rank "+rank);
		}
		
		result = result*rank;
		return result;
	}

	public int getMaximumSkillpoints() {
		return getSkillpointsForLevel( MAX_LEVEL );
	}

	public void setPrimaryAttribute(AttributeType primaryAttribute) {
		this.primaryAttribute = primaryAttribute;
	}

	public AttributeType getPrimaryAttribute() {
		return primaryAttribute;
	}

	public void setSecondaryAttribute(AttributeType secondaryAttribute) {
		this.secondaryAttribute = secondaryAttribute;
	}

	public AttributeType getSecondaryAttribute() {
		return secondaryAttribute;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public int getTypeId() {
		return typeId;
	}
	
	public void assertIsPublishedFlagSet() {
		if ( this.isPublished == null ) {
			throw new RuntimeException("Skill "+this+" does not have the 'published' flag set");
		}
	}

	public void updateIsPublished(Boolean published) 
	{
		if ( this.isPublished == null && published != null ) {
			this.isPublished = published;
		}
	}
	
	public boolean isPublished() {
		return Boolean.TRUE.equals( this.isPublished );
	}
}
