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
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.utils.ISKAmount;
import de.codesourcery.planning.IResourceType;

@Entity
@Table(name="invTypes")
@org.hibernate.annotations.Table(appliesTo="invTypes",
	indexes = {
		@org.hibernate.annotations.Index(
			name="byNameIdx",
			columnNames= { "typeName" }
		),
		@org.hibernate.annotations.Index(
				name="byGroupIdx",
				columnNames= { "groupID" }
		),
		@org.hibernate.annotations.Index(
				name="byMarketGroupIdx",
				columnNames= { "marketGroupID" }
		)		
	}
)
// @org.hibernate.annotations.Proxy(lazy=false)
public class InventoryType implements IResourceType {

	/*
invTypes:

This table contain all ingame items.  It's source of all queries 
related to Ships, Modules, Skills, etc. 
It doesn't contain actual space objects data,
 only types of objects (i.e. Large Gas Planet I).

Column 	              | Links to 	Used 	Note
----------------------------------------------
typeID                | *                     | Unique ID of item in game. Should be primary key.
groupID               | invGroups.groupID     | * 	Group this item belongs to. It's not Market group. Also Groups belong to Categories.
typeName              | *                     | Name of item, i.e. Rogue Medium Commander Wreck.
description           | *                     | Description as seen in Info window in game.
graphicID             | eveGraphics.graphicID | 	* 	Link to graphic used in game for this item.
radius                | *                     | Radius of item, if applicable.
mass                  | *                     | Mass of item, if applicable.
volume                | *                     | Volume of item, if applicable.
capacity              | *                     | Holding capacity of item, if applicable.
portionSize           | *                     | Portion size of item, if applicable. Portion size is size of group for reprocessing purposes, for example.
raceID                | *                     | Which race(s) this item belongs to. Races are bitmask, by the way.
basePrice             |	*                     | Base price of item. Have no relation to actual market price.
published             | *                     | 1 if item is published in the game market.
marketGroupID         | invMarketGroups.marketGroupID | 	* 	Market group of item.
chanceOfDuplicating   | *                     | Chance of duplicating item. Duplication process is not implemented.

+---------------------+---------------------+------+-----+---------+-------+
| Field               | Type                | Null | Key | Default | Extra |
+---------------------+---------------------+------+-----+---------+-------+
| typeID              | smallint(6)         | NO   | PRI | NULL    |       |
| groupID             | smallint(6)         | YES  | MUL | NULL    |       |
| typeName            | varchar(100)        | YES  |     | NULL    |       |
| description         | varchar(3000)       | YES  |     | NULL    |       |
| graphicID           | smallint(6)         | YES  | MUL | NULL    |       |
| radius              | double              | YES  |     | NULL    |       |
| mass                | double              | YES  |     | NULL    |       |
| volume              | double              | YES  |     | NULL    |       |
| capacity            | double              | YES  |     | NULL    |       |
| portionSize         | int(11)             | YES  |     | NULL    |       |
| raceID              | tinyint(3) unsigned | YES  | MUL | NULL    |       |
| basePrice           | double              | YES  |     | NULL    |       |
| published           | tinyint(1)          | YES  |     | NULL    |       |
| marketGroupID       | smallint(6)         | YES  | MUL | NULL    |       |
| chanceOfDuplicating | double              | YES  |     | NULL    |       |
+---------------------+---------------------+------+-----+---------+-------+ */

	@Id
	@Column(name="typeID")
	private Long typeId;

	@JoinColumn(name="groupID")
	@OneToOne
	private InventoryGroup groupId;

	@OneToOne(optional=true)
	@JoinColumn(name="marketGroupID")
	private MarketGroup marketGroup;

	@Column(name="typeName")
	private String name;

	@Column(name="description",length=3000)
	private String description;

	@Column(name="volume")
	private double volume;

	@Column(name="capacity")
	private double capacity;

	@Column(name="portionSize")
	private int portionSize;
	
	@Column(name="published")
	private int isPublished;
	
	@Column(name="basePrice")
	private double basePrice;
	
	public static final Comparator<InventoryType> BY_NAME_COMPARATOR = 
		new Comparator<InventoryType> () {

			@Override
			public int compare(InventoryType o1, InventoryType o2)
			{
				return o1.getName().compareTo( o2.getName() );
			}
		};

	public Long getId() {
		return typeId;
	}

	public Long getTypeId() {
		return typeId;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if ( obj instanceof InventoryType) {
			return ObjectUtils.equals( this.getId() , ((InventoryType) obj).getId() );
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.typeId != null ? typeId.hashCode() : 0;
	}

	public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	public InventoryGroup getGroup() {
		return groupId;
	}

	public void setGroup(InventoryGroup groupId) {
		this.groupId = groupId;
	}

	public String getName() {
		return name;
	}

	public boolean isSkill() {
		return groupId != null && groupId.getCategory() != null && groupId.getCategory().isSkill(); 
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public int getPortionSize() {
		return portionSize;
	}

	public void setPortionSize(int portionSize) {
		this.portionSize = portionSize;
	}

	@Override
	public String toString() {
		return getName()+" ("+getId()+")";
	}

	public void setMarketGroup(MarketGroup marketGroup) {
		this.marketGroup = marketGroup;
	}

	public boolean isSoldThroughMarket() {
		return marketGroup != null;
	}
	
	public MarketGroup getMarketGroup() {
		return marketGroup;
	}
	
	public boolean isShip() {
		return MarketGroup.isMemberOfGroup( this , "Ships" );
	}

	public boolean isBattleCruiser() {
		return MarketGroup.isMemberOfGroup( this , "Battlecruisers" ); 
	}

	public boolean isBattleship() {
		return MarketGroup.isMemberOfGroup( this , "Battleships" ); 
	}

	public boolean isHulk() {
		return "hulk".equalsIgnoreCase( getName() );
	}

	public boolean isCruiser() {
		return MarketGroup.isMemberOfGroup( this , "Cruisers" ); 
	}

	public boolean isIndustrial() {
		return MarketGroup.isMemberOfGroup( this , "Industrials" ); 
	}

	public boolean isSkiff() {
		return "Skiff".equalsIgnoreCase( getName() );
	}

	public boolean isMackinaw() {
		return "Mackinaw".equalsIgnoreCase( getName() );
	}

	public boolean isFrigate() {
		return MarketGroup.isMemberOfGroup( this , "Frigates" );
	}

	public boolean isDestroyer() {
		return MarketGroup.isMemberOfGroup( this , "Destroyers" );
	}
	
	public boolean isOre() {
		return getGroup().getCategory() == InventoryCategory.ASTEROID;
	}

	public boolean isFreighter() {
		return MarketGroup.isMemberOfGroup( this , "Freighters" );
	}

	public boolean isPublished()
	{
		return isPublished != 0;
	}

	public ISKAmount getBasePrice()
	{
		return new ISKAmount( basePrice );
	}

	public boolean isBlueprint()
	{
		return MarketGroup.isMemberOfGroup( this , "Blueprints" );
	}
}
