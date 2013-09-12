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

import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="invGroups")
// @org.hibernate.annotations.Proxy(lazy=false)
public class InventoryGroup {

	/*
invGroups

This table contain groups for items. 
Groups belong to Categories. 
Also here listed parameters common to all items in group.

Column 	Links to 	Used 	Note
------------------------------------
groupID              | *                        | Unique ID of group. Should be primary key.
categoryID           | invCategories.categoryID | * 	Category this group belongs to.
groupName            | *                        | Name of group.
description          | *                        | Description of group.
graphicID            | eveGraphics.graphicID    | * 	Graphic used for group, if applicable.
useBasePrice         | *                        | Should game use base price for items in this group.
allowManufacture     | *                        | Items in this group can be manufactured.
allowRecycler        | *                        | Items in this group can be recycled.
anchored             | *                        | Items in this group are anchored.
anchorable           | *                        | Items in this group can be anchored.
fittableNonSingleton | *                        | Items in this group can be fit in quantities (i.e. Smartbombs).
published            | *                        | 1 if group is in game.


mysql> desc invGroups;
+----------------------+---------------------+------+-----+---------+-------+
| Field                | Type                | Null | Key | Default | Extra |
+----------------------+---------------------+------+-----+---------+-------+
| groupID              | smallint(6)         | NO   | PRI | NULL    |       |
| categoryID           | tinyint(3) unsigned | YES  | MUL | NULL    |       |
| groupName            | varchar(100)        | YES  |     | NULL    |       |
| description          | varchar(3000)       | YES  |     | NULL    |       |
| graphicID            | smallint(6)         | YES  | MUL | NULL    |       |
| useBasePrice         | tinyint(1)          | YES  |     | NULL    |       |
| allowManufacture     | tinyint(1)          | YES  |     | NULL    |       |
| allowRecycler        | tinyint(1)          | YES  |     | NULL    |       |
| anchored             | tinyint(1)          | YES  |     | NULL    |       |
| anchorable           | tinyint(1)          | YES  |     | NULL    |       |
| fittableNonSingleton | tinyint(1)          | YES  |     | NULL    |       |
| published            | tinyint(1)          | YES  |     | NULL    |       |
+----------------------+---------------------+------+-----+---------+-------+
	 */
	
	@Id
	private Long groupID;

	@org.hibernate.annotations.Type(
			type="de.codesourcery.eve.skills.db.dao.CategoryUserType"
	)
	@Column(name="categoryID")
	private InventoryCategory categoryID;
	
	@Column
	private String groupName;
	
	@Column(length=3000)
	private String description;
	
	@Column(name="allowManufacture")
	private boolean canManufacture;
	
	@Column(name="allowRecycler")
	private boolean canRecycle;
	
	@Column(name="published")
	private Integer isPublished;
	
	public static final Comparator<InventoryGroup> BY_NAME_COMPARATOR = new Comparator<InventoryGroup>() {

		@Override
		public int compare(InventoryGroup o1, InventoryGroup o2)
		{
			return o1.getGroupName().compareTo( o2.getGroupName() );
		}
	};
	
	public Long getId() {
		return groupID;
	}

	public Long getGroupID() {
		return groupID;
	}

	public void setGroupID(Long groupID) {
		this.groupID = groupID;
	}

	public InventoryCategory getCategory() {
		return categoryID;
	}

	public void setCategory(InventoryCategory category) {
		this.categoryID = category;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setSupportsManufacturing(boolean canManufacture) {
		this.canManufacture = canManufacture;
	}

	public boolean supportsManufacturing() {
		return canManufacture;
	}

	public void setSupportsRecycling(boolean canRecycle) {
		this.canRecycle = canRecycle;
	}

	public boolean supportsRecycling() {
		return canRecycle;
	}

	@Override
	public String toString()
	{
		return getGroupName();
	}

	public boolean isPublished() {
		return this.isPublished != null && this.isPublished.intValue() != 0;
	}
}
