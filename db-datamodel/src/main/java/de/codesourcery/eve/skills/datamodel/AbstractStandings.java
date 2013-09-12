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

import de.codesourcery.eve.skills.db.dao.ISkillTreeDAO;
import de.codesourcery.eve.skills.db.datamodel.Skill;

public class AbstractStandings
{

	public float calcEffectiveStanding(ICharacter c , ISkillTreeDAO skillTree, Standing<?> standing) {
		
		/*
		Effective Standing = <Your Standing> + ((10 - <Your Standing>) * (0.04 * (<Connections Skill Level> OR <Diplomacy Skill Level>)))
		Connections_Skill_Level is used when Your_Standing is larger than 0.0 Diplomacy_Skill_Level is used when Your_Standing is less than 0.0 
		 */
		
		if ( standing == null ) {
			return 0.0f;
		}
		
		final Skill skill;
		final float value =  standing.getValue();
		if ( value > 0.0 ) {
			skill =
				Skill.getConnectionsSkill( skillTree.getSkillTree() );
		} else {
			skill =
				Skill.getDiplomacySkill(skillTree.getSkillTree() );
		}
		
		return value + ( (10.0f - value ) * ( 0.04f* c.getSkillLevel( skill ).getLevel() ) );
	}
}
