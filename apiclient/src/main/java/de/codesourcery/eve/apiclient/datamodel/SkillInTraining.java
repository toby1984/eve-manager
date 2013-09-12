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

public class SkillInTraining {
	
	/*
	 * <eveapi version="2"> <currentTime>2008-08-17 06:43:00</currentTime>
	 * <result> <currentTQTime offset="0">2008-08-17 06:43:00</currentTQTime>
	 * <trainingEndTime>2008-08-17 15:29:44</trainingEndTime>
	 * <trainingStartTime>2008-08-15 04:01:16</trainingStartTime>
	 * <trainingTypeID>3305</trainingTypeID>
	 * <trainingStartSP>24000</trainingStartSP>
	 * <trainingDestinationSP>135765</trainingDestinationSP>
	 * <trainingToLevel>4</trainingToLevel> <skillInTraining>1</skillInTraining>
	 * </result> <cachedUntil>2008-08-17 06:58:00</cachedUntil> </eveapi>
	 */

	private EveDate currentTQTime; // server time
	private EveDate trainingStartTime; // server time
	private EveDate trainingEndTime; // server time
	private Skill skill;
	private int trainingStartSP;
	private int trainingDestinationSP;
	private int plannedLevel;
	private boolean skillInTraining;

	public EveDate getCurrentTQTime() {
		return currentTQTime;
	}

	public void setCurrentTQTime(EveDate currentTQTime) {
		this.currentTQTime = currentTQTime;
	}

	public EveDate getTrainingStartTime() {
		return trainingStartTime;
	}

	public void setTrainingStartTime(EveDate trainingStartTime) {
		this.trainingStartTime = trainingStartTime;
	}

	public EveDate getTrainingEndTime() {
		return trainingEndTime;
	}

	public void setTrainingEndTime(EveDate trainingEndTime) {
		this.trainingEndTime = trainingEndTime;
	}

	public Skill getSkill() {
		return skill;
	}

	public void setSkill(Skill skill) {
		if ( skill == null ) {
			throw new IllegalArgumentException("Skill cannot be NULL");
		}
		this.skill = skill;
	}

	public int getTrainingStartSP() {
		return trainingStartSP;
	}

	public void setTrainingStartSP(int trainingStartSP) {
		this.trainingStartSP = trainingStartSP;
	}

	public int getTrainingDestinationSP() {
		return trainingDestinationSP;
	}

	public void setTrainingDestinationSP(int trainingDestinationSP) {
		this.trainingDestinationSP = trainingDestinationSP;
	}

	public int getPlannedLevel() {
		return plannedLevel;
	}

	public void setPlannedLevel(int plannedLevel) {
		this.plannedLevel = plannedLevel;
	}

	public boolean isSkillInTraining() {
		return skillInTraining;
	}

	public void setSkillInTraining(boolean skillInTraining) {
		this.skillInTraining = skillInTraining;
	}

}
