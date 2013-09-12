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


public class Prerequisite {

	private Skill skill;
	private int requiredLevel;
	
	@Override
	public String toString() {
		if ( skill == null ) {
			return "Prerequisite[ <no skill set> ("+requiredLevel+")";
		}
		return "Prerequisite[ "+skill.getName()+" ("+requiredLevel+")";
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if ( ! ( obj instanceof Prerequisite) ) {
			return false;
		}
		
		final Prerequisite r= (Prerequisite) obj;
		if ( this.getSkill() != null && r.getSkill() != null ) {
			if ( ! this.getSkill().equals( r.getSkill() ) ) {
				return false;
			}
		} else if ( this.getSkill() != r.getSkill() ) {
			return false;
		}
		
		return this.getRequiredLevel() == r.getRequiredLevel();
	}
	
	public boolean isMetBy(ICharacter c ) {
		return c.hasSkill( this.skill , this.requiredLevel );
	}
	
	@Override
	public int hashCode() {
		int result = getRequiredLevel()*31;
		if ( getSkill() != null ) {
			result = ( result << 3 ) ^ ( 7+getSkill().hashCode()*3 ); 
		}
		return result;
	}

	public void setSkill(Skill skill) {
		this.skill = skill;
	}

	public Skill getSkill() {
		return skill;
	}

	public void setRequiredLevel(int requiredLevel) {
		this.requiredLevel = requiredLevel;
	}

	public int getRequiredLevel() {
		return requiredLevel;
	}
}
