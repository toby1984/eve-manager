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
@Table(name="invUniqueNames")
@org.hibernate.annotations.Proxy(lazy=false)
public class EveName
{

	/*
	 mysql> desc eveNames;
+------------+---------------------+------+-----+---------+-------+
| Field      | Type                | Null | Key | Default | Extra |
+------------+---------------------+------+-----+---------+-------+
| itemID     | int(11)             | NO   | PRI | NULL    |       |
| itemName   | varchar(100)        | YES  |     | NULL    |       |
| categoryID | tinyint(3) unsigned | YES  | MUL | NULL    |       |
| groupID    | smallint(6)         | YES  | MUL | NULL    |       |
| typeID     | smallint(6)         | YES  | MUL | NULL    |       |
+------------+---------------------+------+-----+---------+-------+
	 */
	
	@Id
	@Column(name="itemID")
	private Long id;
	
	@Column(name="itemName")
	private String name;

	public EveName() {
	}
	
	public EveName(Long id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public Long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
	
	
}
