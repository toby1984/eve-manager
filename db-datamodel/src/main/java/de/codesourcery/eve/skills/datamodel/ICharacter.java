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

import java.util.Date;
import java.util.Map;

import de.codesourcery.eve.skills.db.datamodel.Skill;

public interface ICharacter extends IBaseCharacter {
	
	public void reconcile(ICharacter character);

	/**
	 * Returns the date when this
	 * character's data has last been 
	 * reconciled with the server.
	 * 
	 * @return timestamp (local time)
	 */
	public Date getLastUpdateTimestamp();
	
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
	public long calcTrainingTime(SkillTree skillTree , Skill s,int targetLevel);
	
	public abstract TrainedSkill getSkillLevel(Skill skill);

	public abstract boolean canTrainSkill(Skill s);

	public abstract ImplantSet getImplantSet();

	public abstract Attributes getAttributes();

	public abstract int getSkillpoints();

	public abstract int getCurrentLevel(Skill s);

	public abstract int getCurrentSkillPoints(Skill s);

	public abstract ICharacter cloneCharacter();
	
	/**
	 * 
	 * @return Key is {@link Skill#getTypeId()} , value
	 * is Trained Skill
	 */
	public abstract Map<Integer,TrainedSkill> getSkills();

	public abstract boolean hasSkill(Skill s);

	public abstract boolean hasSkill(Skill s, int requiredLevel);

	public abstract void setSkill(Skill s, int trainedLevel);

	public CharacterDetails getCharacterDetails();

	/**
	 * Check whether this character's data
	 * has been fully initialized.
	 * 
	 * @return <code>true</code> if this character had
	 * all data reconciled with the EVE Online(tm) API.
	 * @see #setFullyInitialized() 
	 */
	public boolean isFullyInitialized();
	
	 /**
	 * Marks this character having been fully initialized.
	 * 
	 * @see #isFullyInitialized()
	 */
	public void setFullyInitialized();
		
}