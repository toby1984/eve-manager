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
package de.codesourcery.eve.apiclient.datamodel;

import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.utils.EveDate;

/**
 * A skill queue entry.
 * 
 * This class implements {@link Comparable} ,
 * instances are compared by ascending skill queue position.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public class SkillQueueEntry implements Comparable<SkillQueueEntry>
{
	private int position;
	private Skill skill;
	private int plannedToLevel;
	private int startSkillpoints;
	private int endSkillpoints;
	private EveDate startTime;
	private EveDate endTime;

	public int getPosition()
	{
		return position;
	}

	public void setPosition(int position)
	{
		this.position = position;
	}

	public Skill getSkill()
	{
		return skill;
	}

	public void setSkill(Skill skill)
	{
		this.skill = skill;
	}

	public int getPlannedToLevel()
	{
		return plannedToLevel;
	}

	public void setPlannedToLevel(int plannedToLevel)
	{
		this.plannedToLevel = plannedToLevel;
	}

	public int getStartSkillpoints()
	{
		return startSkillpoints;
	}

	public void setStartSkillpoints(int startSkillpoints)
	{
		this.startSkillpoints = startSkillpoints;
	}

	public int getEndSkillpoints()
	{
		return endSkillpoints;
	}

	public void setEndSkillpoints(int endSkillpoints)
	{
		this.endSkillpoints = endSkillpoints;
	}

	public EveDate getStartTime()
	{
		return startTime;
	}

	public void setStartTime(EveDate startTime)
	{
		this.startTime = startTime;
	}

	public EveDate getEndTime()
	{
		return endTime;
	}

	public void setEndTime(EveDate endTime)
	{
		this.endTime = endTime;
	}

	@Override
	public int compareTo(SkillQueueEntry o)
	{
		return this.position - o.position; 
	}

}
