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

import de.codesourcery.eve.skills.db.datamodel.Corporation;


/**
 * Various character details (account balance,race,gender,corp, etc.).
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class CharacterDetails {
	
	public static enum Race {
		AMARR("Amarr"),
		MINMATAR("Minmatar"),
		CALDARI("Caldari"),
		GALLENTE("Gallente");
		
		private final String typeId;
		
		private Race(String s) {
			this.typeId = s;
		}
		
		public static Race fromTypeId(String s) {
			for ( Race r : values() ) {
				if ( r.typeId.equalsIgnoreCase( s ) ) {
					return r;
				}
			}
			throw new IllegalArgumentException("Unknown race type '"+s+"'");
		}
	}
	
	public static enum Gender {
		MALE("male"),
		FEMALE("female");

		private final String typeId;
		
		private Gender(String s) {
			this.typeId = s;
		}
		
		public static Gender fromTypeId(String s) {
			for ( Gender r : values() ) {
				if ( r.typeId.equalsIgnoreCase( s ) ) {
					return r;
				}
			}
			throw new IllegalArgumentException("Unknown gender '"+s+"'");
		}		
	}
	
	private volatile Race race;
	private volatile String bloodLine;
	private volatile Gender gender;
	private volatile Corporation corporation;
	private volatile String cloneName;
	private volatile int cloneSkillPoints;
	private volatile long balance;
	
	public void reconcile(ICharacter payload) {
		
		CharacterDetails other = payload.getCharacterDetails();
		this.race = other.getRace();
		this.bloodLine = other.getBloodLine();
		this.gender = other.getGender();
		this.corporation = other.getCorporation();
		this.cloneName = other.cloneName;
		this.cloneSkillPoints = other.cloneSkillPoints;
		this.balance = other.balance;
	}	

	public CharacterDetails cloneDetails() {
		final CharacterDetails result =
			new CharacterDetails ();
		result.race = this.race;
		result.bloodLine = this.bloodLine;
		result.gender = this.gender;
		if ( this.corporation != null ) {
			result.corporation = corporation.cloneCorporation();
		}
		result.cloneName = this.cloneName;
		result.cloneSkillPoints = this.cloneSkillPoints;
		result.balance = this.balance;
		return result;
	}
	public void setRace(Race race) {
		this.race = race;
	}

	public Race getRace() {
		return race;
	}

	public void setBloodLine(String bloodLine) {
		this.bloodLine = bloodLine;
	}

	public String getBloodLine() {
		return bloodLine;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Gender getGender() {
		return gender;
	}

	public void setCorporation(Corporation corp) {
		this.corporation = corp;
	}

	public Corporation getCorporation() {
		return corporation;
	}

	public void setCloneName(String cloneName) {
		this.cloneName = cloneName;
	}

	public String getCloneName() {
		return cloneName;
	}

	public void setCloneSkillPoints(int cloneSkillPoints) {
		this.cloneSkillPoints = cloneSkillPoints;
	}

	public int getCloneSkillPoints() {
		return cloneSkillPoints;
	}

	/**
	 * Set balance in "ISK cents".
	 * 
	 * The 'real' balance is <code>getBalance() / 100</code>.
	 * 
	 * @param balance
	 */
	public void setBalance(long balance) {
		this.balance = balance;
	}

	/**
	 * Get balance in "ISK cents".
	 * 
	 * The 'real' balance is <code>getBalance() / 100</code>.
	 * 
	 * @param balance
	 */	
	public long getBalance() {
		return balance;
	}

}
