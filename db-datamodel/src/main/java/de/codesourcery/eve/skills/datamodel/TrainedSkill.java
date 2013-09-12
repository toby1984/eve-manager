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

import de.codesourcery.eve.skills.db.datamodel.Skill;


public class TrainedSkill {

	private final Skill skill;
	private int skillPoints;
	
	public TrainedSkill(Skill skill, int skillpoints ) {
		if (skill == null) {
			throw new IllegalArgumentException("skill cannot be NULL");
		}
		this.skill = skill;
		setSkillPoints( skillpoints );
	}

	public Skill getSkill() {
		return skill;
	}

	public int getSkillpoints() {
		return skillPoints;
	}
	
	public boolean isPartiallyTrained(int lvl) {
		final float fraction = getFractionOfLevelTrained( lvl );
		return fraction != 0.0f && fraction != 100.0f;
	}
	
	public boolean isPartiallyTrained() {
		
		if ( getLevel() == Skill.MAX_LEVEL ) {
			return false;
		}
		
		final float fraction = getFractionOfLevelTrained(getLevel()+1);
		return fraction != 0.0f && fraction != 100.0f;
	}
	
	public float getFractionOfLevelTrained(int lvl ) {
		
		if ( lvl < 1 || lvl > Skill.MAX_LEVEL ) {
			throw new IllegalArgumentException("Invalid skill level: "+lvl+
					" , must be >0 && < "+Skill.MAX_LEVEL);
		}
		
		if ( skill.getSkillpointsForLevel( lvl ) == skillPoints ) {
			return 100.0f;
		}
		
		final int lowerBound=
			skill.getSkillpointsForLevel( lvl - 1 );
			
		final int upperBound =			
			skill.getSkillpointsForLevel( lvl  );
		 
		if ( skillPoints < lowerBound ) {
			return 0.0f;
		}
		
		if ( skillPoints > upperBound ) {
			return 100.0f;
		} 
		
		final float trainedOfNextLevel = skillPoints - lowerBound;
		final float spsThisLevel = upperBound - lowerBound;
		final float fraction = 100.0f*( trainedOfNextLevel / spsThisLevel);
		return fraction;
	}
	
	public int getLevel() {
		return skill.getLevelForSkillpoints( skillPoints );
	}
	
	public TrainedSkill setLevel(int level) {
		skillPoints = skill.getSkillpointsForLevel( level );
		return this;
	}

	public TrainedSkill setSkillPoints(int skillpoints) {
		if ( skillpoints < 0 ) {
			throw new IllegalArgumentException("Invalid skillPoints value: "+skillpoints);
		}
		this.skillPoints = skillpoints;
		return this;
	}	
	
	public boolean isFullyTrained() {
		return skill.getMaximumSkillpoints() == skillPoints;
	}
	
	@Override
	public String toString() {
		return "TrainedSkill[ skillPoints="+skillPoints+" , skill="+skill+" ]";
	}
}
