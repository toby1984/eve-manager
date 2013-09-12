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
package de.codesourcery.eve.skills.datamodel;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.db.datamodel.Skill;

public class Character extends BaseCharacter implements ICharacter {

	private static final Logger log = Logger.getLogger(Character.class);
	
	private final Map<Integer,TrainedSkill> skills =
		new HashMap<Integer, TrainedSkill>();

	private Date lastUpdateTimestamp;
	private ImplantSet implants = new ImplantSet();
	private Attributes attributes = new Attributes( this );
	private CharacterDetails characterDetails = new CharacterDetails();
	private boolean fullyInitialized = false;
	
	public void reconcile(ICharacter payload) {
		
		// reconcile lastUpdate
		this.lastUpdateTimestamp = payload.getLastUpdateTimestamp();
		
		// reconcile skills: this -> other
		for ( Iterator<Integer> it = skills.keySet().iterator() ; it.hasNext() ; ) {
			
			final TrainedSkill trained = skills.get( it.next() );
			
			if ( payload.hasSkill( trained.getSkill() ) ) {
				final TrainedSkill other =
					payload.getSkillLevel( trained.getSkill() );
				trained.setSkillPoints( other.getSkillpoints() );
			} else {
				it.remove(); // not trained, remove it
			}
		}
		
		for ( Map.Entry<Integer,TrainedSkill> other : payload.getSkills().entrySet() ) {
			final TrainedSkill skill  = other.getValue();
			if ( ! this.skills.containsKey( skill.getSkill().getTypeId() ) ) {
				TrainedSkill newSkill =
					new TrainedSkill( skill.getSkill() , skill.getSkillpoints() );
				this.skills.put( skill.getSkill().getTypeId() , newSkill );
			}
		}		
		
		implants.reconcile( payload );
		attributes.reconcile( payload );
		characterDetails.reconcile( payload );
	}
	
	public Character(String name) {
		super(name);
	}
	
	public Character(String name,CharacterID id) {
		super(name,id);
	}
	
	public Character() {
	}
	
	public Character(IBaseCharacter c) {
		setCharacterId( c.getCharacterId() );
		setName( c.getName() );
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#getSkillLevel(de.codesourcery.eve.skills.datamodel.Skill)
	 */
	public TrainedSkill getSkillLevel( Skill skill ) {
		
		if (skill == null) {
			throw new IllegalArgumentException("skill cannot be NULL");
		}
		
		TrainedSkill result =
			skills.get( skill.getTypeId() );
		
		if ( result != null ) {
			return result;
		}
		
		result = 
			new TrainedSkill( skill , 0 );
		
		skills.put( skill.getTypeId(), result );
		return result;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#canTrainSkill(de.codesourcery.eve.skills.datamodel.Skill)
	 */
	public boolean canTrainSkill(Skill s ) {
		return s.canBeTrainedBy( this );
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#getImplantSet()
	 */
	public ImplantSet getImplantSet() {
		return implants;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#getAttributes()
	 */
	public Attributes getAttributes() {
		return attributes;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#getSkillpoints()
	 */
	public int getSkillpoints() {
		int result = 0;
		for ( TrainedSkill sp : skills.values() ) {
			result += sp.getSkillpoints();
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#getCurrentLevel(de.codesourcery.eve.skills.datamodel.Skill)
	 */
	public int getCurrentLevel(Skill s) {
		final TrainedSkill trained = skills.get( s.getTypeId() );
		return trained != null ? trained.getLevel() : 0;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#getCurrentSkillPoints(de.codesourcery.eve.skills.datamodel.Skill)
	 */
	public int getCurrentSkillPoints(Skill s) {
		final TrainedSkill trained = skills.get( s.getTypeId() );
		return trained != null ? trained.getSkillpoints() : 0;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#cloneCharacter()
	 */
	public ICharacter cloneCharacter() {
		
		final Character result = new Character();
		super.cloneCharacter( result );
		
		result.lastUpdateTimestamp = this.lastUpdateTimestamp;
		
		for ( Map.Entry<Integer,TrainedSkill> entry : skills.entrySet() ) {
			final TrainedSkill orig = entry.getValue();
			final TrainedSkill clone =
				new TrainedSkill( orig.getSkill() , orig.getSkillpoints() );
			result.skills.put( entry.getKey(), clone );
		}
		
		result.attributes = attributes.cloneAttributes();
		result.implants = implants.cloneImplants();
		result.characterDetails = this.characterDetails.cloneDetails();
		return result;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#hasSkill(de.codesourcery.eve.skills.datamodel.Skill)
	 */
	public boolean hasSkill(Skill s) {
		return hasSkill( s ,  1 );
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#hasSkill(de.codesourcery.eve.skills.datamodel.Skill, int)
	 */
	public boolean hasSkill(Skill s,int requiredLevel) {
		TrainedSkill trained = skills.get( s.getTypeId() );
		if ( trained == null ) {
			return false;
		}
		return trained.getLevel() >= requiredLevel;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#setSkill(de.codesourcery.eve.skills.datamodel.Skill, int)
	 */
	public void setSkill(Skill s,int trainedLevel) 
	{
		TrainedSkill trained = skills.get( s.getTypeId() );
		if ( trained == null ) {
			final int skillPoints = 
				s.getSkillpointsForLevel( trainedLevel );
			trained = new TrainedSkill( s , skillPoints );
			skills.put( s.getTypeId() , trained );
			return;
		}
		
		trained.setLevel( trainedLevel );
	}
	
	public void setTrainedSkill(Skill s,int skillPoints ) 
	{
		TrainedSkill trained = skills.get( s.getTypeId() );
		if ( trained == null ) {
			trained = new TrainedSkill( s , skillPoints );
			skills.put( s.getTypeId() , trained );
			return;
		}
		
		trained.setSkillPoints( skillPoints );
	}	
	
	@Override
	public String toString() {
		return "Characer[ id="+getCharacterId()+", name="+getName()+"]";
	}

	@Override
	public Date getLastUpdateTimestamp() {
		return lastUpdateTimestamp;
	}
	
	public void setLastUpdateTimestamp(Date lastUpdateTimestamp) {
		if (lastUpdateTimestamp == null) {
			throw new IllegalArgumentException(
					"timestamp cannot be NULL");
		}
		this.lastUpdateTimestamp = lastUpdateTimestamp;
	}

	@Override
	public CharacterDetails getCharacterDetails() {
		return this.characterDetails;
	}
	
	public void setCharacterDetails(CharacterDetails details) {
		if (details == null) {
			throw new IllegalArgumentException(
					"details cannot be NULL");
		}
		this.characterDetails = details;
	}

	@Override
	public boolean isFullyInitialized() {
		return fullyInitialized;
	}

	@Override
	public void setFullyInitialized() {
		fullyInitialized = true;
	}

	@Override
	public Map<Integer, TrainedSkill> getSkills() {
		return Collections.unmodifiableMap( this.skills );
	}

	/**
	 * Calculates the time required
	 * for this character to
	 * reach a specific lvl of
	 * a given skill.
	 * 
	 * This method takes into account training
	 * all the skill's prerequisites (if not
	 * already trained) , any implants 
	 * the character has and any partially
	 * trained skills.
	 * 
	 * @param s
	 * @param targetLevel
	 * @return training duration in milliseconds
	 */
	public long calcTrainingTime(SkillTree tree , Skill s,int targetLevel) {

		if ( targetLevel < 1 || targetLevel > Skill.MAX_LEVEL ) {
			throw new IllegalArgumentException("Invalid target skill level "+targetLevel);
		}

		if ( hasSkill( s , targetLevel ) ) {
			return 0;
		}
		
		final int requiredSp = s.getSkillpointsForLevel( targetLevel );
		final int delta = requiredSp - getSkillLevel( s ).getSkillpoints();
		
		final float spPerHour = 
			getAttributes().calcTrainingSpeed( tree , s );

		long durationInMillis =
			Math.round( ( delta / spPerHour ) * 60 * 60 * 1000 );
		
		final Map<Skill,Integer> prerequisites =
			gatherPrerequisites( s );
		
		for ( Map.Entry<Skill,Integer> entry : prerequisites.entrySet() ) {
			durationInMillis+= calcTrainingTime( tree, entry.getKey() , entry.getValue() );
		}
		return durationInMillis;
	}

	private Map<Skill,Integer> gatherPrerequisites(Skill s) {
		final Map<Skill,Integer> result =
			new HashMap<Skill, Integer>();
		gatherPrerequisites(s  , result );
		return result;
	}
	
	private void gatherPrerequisites(Skill s,Map<Skill,Integer> visited) {
		for ( Prerequisite r : s.getPrerequisites() ) {
			Integer requiredLevel = visited.get( r.getSkill() );
			if ( requiredLevel == null ) {
				visited.put( r.getSkill() , r.getRequiredLevel() );
				gatherPrerequisites( r.getSkill() , visited );
			} else if ( r.getRequiredLevel() > requiredLevel.intValue() ) {
				visited.put( r.getSkill() , r.getRequiredLevel() );
			}
		}
	}
	
}
