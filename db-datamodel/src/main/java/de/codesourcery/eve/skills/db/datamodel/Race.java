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
@Table(name="chrRaces")
// @org.hibernate.annotations.Proxy(lazy=false)
public class Race {

	/*
	 mysql> desc chrRaces;
+------------------+---------------------+------+-----+---------+-------+
| Field            | Type                | Null | Key | Default | Extra |
+------------------+---------------------+------+-----+---------+-------+
| raceID           | tinyint(3) unsigned | NO   | PRI | NULL    |       |
| raceName         | varchar(100)        | YES  |     | NULL    |       |
| description      | varchar(1000)       | YES  |     | NULL    |       |
| graphicID        | smallint(6)         | YES  | MUL | NULL    |       |
| shortDescription | varchar(500)        | YES  |     | NULL    |       |
+------------------+---------------------+------+-----+---------+-------+
5 rows in set (0.00 sec)
	 */
	
	public static final Race UNKNOWN_RACE;
	
	static {
		UNKNOWN_RACE = new Race() {
			@Override
			public void setDescription(String arg0) {
				// nop
			}
			
			@Override
			public void setName(String arg0) {
				// nop
			}
			
			@Override
			public void setID(Long arg0) {
				// nop
			}
		};
		UNKNOWN_RACE.raceID = Long.MAX_VALUE;
		UNKNOWN_RACE.name = "<unknown race>";
		UNKNOWN_RACE.description = "<race not contained in EVE online database dump>";
	}

	@Id
	private Long raceID;
	
	@Column(name="raceName")
	private String name;
	
	@Column(length=1000)
	private String description;

	public void setID(Long raceID) {
		this.raceID = raceID;
	}

	public Long getID() {
		return raceID;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
