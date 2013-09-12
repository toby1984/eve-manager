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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import de.codesourcery.eve.skills.datamodel.INamedEntity;

@Entity
@Table(name="chrFactions")
// @org.hibernate.annotations.Proxy(lazy=false)
public class Faction implements INamedEntity {

	/*
	mysql> desc chrFactions;
	+----------------------+---------------+------+-----+---------+-------+
	| Field                | Type          | Null | Key | Default | Extra |
	+----------------------+---------------+------+-----+---------+-------+
	| factionID            | int(11)       | NO   | PRI | NULL    |       |
	| factionName          | varchar(100)  | YES  |     | NULL    |       |
	| description          | varchar(1000) | YES  |     | NULL    |       |
	| raceIDs              | int(11)       | YES  |     | NULL    |       |
	| solarSystemID        | int(11)       | YES  | MUL | NULL    |       |
	| corporationID        | int(11)       | YES  | MUL | NULL    |       |
	| sizeFactor           | double        | YES  |     | NULL    |       |
	| stationCount         | smallint(6)   | YES  |     | NULL    |       |
	| stationSystemCount   | smallint(6)   | YES  |     | NULL    |       |
	| militiaCorporationID | int(11)       | YES  | MUL | NULL    |       |
	+----------------------+---------------+------+-----+---------+-------+
	10 rows in set (0.00 sec)
		 */
	
	@Id
	private Long factionID;
	
	@Column(name="factionName")
	private String name;
	
	@Column(length=1000)
	private String description;
	
	@OneToOne
//	@OneToOne(optional=true) // hmmm...somehow database dump does not contain all races returned by the API ....
	@PrimaryKeyJoinColumn
	private Race race;
	
//	@OneToOne
//	@JoinColumn(name="solarSystemID")
//	private SolarSystem solarSystem;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setID(Long factionID) {
		this.factionID = factionID;
	}

	public Long getID() {
		return factionID;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public Race getRace() {
		if ( this.race == race ) {
			return Race.UNKNOWN_RACE;
		}
		return race;
	}

//	public void setSolarSystem(SolarSystem solarSystem) {
//		this.solarSystem = solarSystem;
//	}
//
//	public SolarSystem getSolarSystem() {
//		return solarSystem;
//	}
}
