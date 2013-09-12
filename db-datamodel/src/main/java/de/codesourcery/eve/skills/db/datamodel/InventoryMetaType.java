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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name="invMetaTypes")
// @org.hibernate.annotations.Proxy(lazy=false)
public class InventoryMetaType
{

	/*mysql> desc invMetaTypes
    -> ;
+--------------+-------------+------+-----+---------+-------+
| Field        | Type        | Null | Key | Default | Extra |
+--------------+-------------+------+-----+---------+-------+
| typeID       | smallint(6) | NO   | PRI | NULL    |       |
| parentTypeID | smallint(6) | YES  | MUL | NULL    |       |
| metaGroupID  | smallint(6) | YES  | MUL | NULL    |       |
+--------------+-------------+------+-----+---------+-------+

	 */
	
	@Id
	@Column(name="typeID")
	private Long id;	
	
	@OneToOne
	@PrimaryKeyJoinColumn(name="typeID")
	private InventoryType type;
	
	@ManyToOne
	@JoinColumn(name="parentTypeID",nullable=true,updatable=false)	
	private InventoryType parentType;
	
	private int metaGroupId;

	public InventoryType getType()
	{
		return type;
	}

	public InventoryType getParentType()
	{
		return parentType;
	}

	public int getMetaGroupId()
	{
		return metaGroupId;
	}
	
}
