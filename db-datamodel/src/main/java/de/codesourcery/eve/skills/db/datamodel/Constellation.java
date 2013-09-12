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
import javax.persistence.Table;

@Entity
@Table(name="mapConstellations")
// @org.hibernate.annotations.Proxy(lazy=false)
public class Constellation {

	/*
	 mysql> desc mapConstellations;
+-------------------+--------------+------+-----+---------+-------+
| Field             | Type         | Null | Key | Default | Extra |
+-------------------+--------------+------+-----+---------+-------+
| regionID          | int(11)      | YES  | MUL | NULL    |       |
| constellationID   | int(11)      | NO   | PRI | NULL    |       |
| constellationName | varchar(100) | YES  |     | NULL    |       |
| x                 | double       | YES  |     | NULL    |       |
| y                 | double       | YES  |     | NULL    |       |
| z                 | double       | YES  |     | NULL    |       |
| xMin              | double       | YES  |     | NULL    |       |
| xMax              | double       | YES  |     | NULL    |       |
| yMin              | double       | YES  |     | NULL    |       |
| yMax              | double       | YES  |     | NULL    |       |
| zMin              | double       | YES  |     | NULL    |       |
| zMax              | double       | YES  |     | NULL    |       |
| factionID         | int(11)      | YES  | MUL | NULL    |       |
| radius            | double       | YES  |     | NULL    |       |
+-------------------+--------------+------+-----+---------+-------+
	 */
	
	@Id
	private Long constellationID;
	
	@Column(name="constellationName")
	private String name;
	
//	@OneToOne
//	@JoinColumn(name="factionID",nullable=true)
//	private Faction faction;
//	
//	@ManyToOne
//	@JoinColumn(name="regionID",insertable=false,updatable=false)
//	private Region region;

	public Long getID() {
		return constellationID;
	}

//	public Faction getFaction() {
//		return faction;
//	}
//	
//	public Region getRegion() {
//		return region;
//	}

	public String getName() {
		return name;
	}
	
}
