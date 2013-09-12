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
@Table(name="crpNpcCorporations")
// @org.hibernate.annotations.Proxy(lazy=false)
public class NPCCorporation implements INamedEntity
{

	/*
mysql> desc crpNPCCorporations;
+--------------------+---------------+------+-----+---------+-------+
| Field              | Type          | Null | Key | Default | Extra |
+--------------------+---------------+------+-----+---------+-------+
| corporationID      | int(11)       | NO   | PRI | NULL    |       |
| size               | char(1)       | YES  |     | NULL    |       |
| extent             | char(1)       | YES  |     | NULL    |       |
| solarSystemID      | int(11)       | YES  | MUL | NULL    |       |
| investorID1        | int(11)       | YES  | MUL | NULL    |       |
| investorShares1    | tinyint(4)    | YES  |     | NULL    |       |
| investorID2        | int(11)       | YES  | MUL | NULL    |       |
| investorShares2    | tinyint(4)    | YES  |     | NULL    |       |
| investorID3        | int(11)       | YES  | MUL | NULL    |       |
| investorShares3    | tinyint(4)    | YES  |     | NULL    |       |
| investorID4        | int(11)       | YES  | MUL | NULL    |       |
| investorShares4    | tinyint(4)    | YES  |     | NULL    |       |
| friendID           | int(11)       | YES  | MUL | NULL    |       |
| enemyID            | int(11)       | YES  | MUL | NULL    |       |
| publicShares       | bigint(20)    | YES  |     | NULL    |       |
| initialPrice       | int(11)       | YES  |     | NULL    |       |
| minSecurity        | double        | YES  |     | NULL    |       |
| scattered          | tinyint(1)    | YES  |     | NULL    |       |
| fringe             | tinyint(4)    | YES  |     | NULL    |       |
| corridor           | tinyint(4)    | YES  |     | NULL    |       |
| hub                | tinyint(4)    | YES  |     | NULL    |       |
| border             | tinyint(4)    | YES  |     | NULL    |       |
| factionID          | int(11)       | YES  | MUL | NULL    |       |
| sizeFactor         | double        | YES  |     | NULL    |       |
| stationCount       | smallint(6)   | YES  |     | NULL    |       |
| stationSystemCount | smallint(6)   | YES  |     | NULL    |       |
| description        | varchar(4000) | YES  |     | NULL    |       |
+--------------------+---------------+------+-----+---------+-------+
	 
	 */
	
	@Id
	@Column(name="corporationID")
	private Long id;
	
	@OneToOne(optional=false)
	@PrimaryKeyJoinColumn(name="corporationID")
	private EveName name;

	public NPCCorporation() {
	}
	
	public NPCCorporation(Long id,EveName name) {
		this.id = id;
		this.name = name;
	}
	
	public Long getId()
	{
		return id;
	}

	public String getName()
	{
		return name != null ? name.getName() : null;
	}

	public Long getID()
	{
		return id;
	}
	
	
}
