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

public class PlannedSkill {

	private final Skill skill;
	private int plannedTo;
	
	public PlannedSkill(Skill skill, int plannedLevel ) {
		if (skill == null) {
			throw new IllegalArgumentException("skill cannot be NULL");
		}
		this.skill = skill;
		setPlannedTo( plannedLevel );
	}

	public Skill getSkill() {
		return skill;
	}

	public int getPlannedTo() {
		return plannedTo;
	}

	public void setPlannedTo(int plannedTo) {
		if ( plannedTo < 1 || plannedTo > 5 ) {
			throw new IllegalArgumentException("Invalid plannedTo lvl "+plannedTo);
		}
		this.plannedTo = plannedTo;
	}
	
}
