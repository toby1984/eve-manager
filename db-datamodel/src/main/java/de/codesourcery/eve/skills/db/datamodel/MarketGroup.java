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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.ObjectUtils;

@Entity
@Table(name="invMarketGroups")
// @org.hibernate.annotations.Proxy(lazy=false)
public class MarketGroup {

	/*
	+-----------------+---------------+------+-----+---------+-------+
| Field           | Type          | Null | Key | Default | Extra |
+-----------------+---------------+------+-----+---------+-------+
| marketGroupID   | smallint(6)   | NO   | PRI | NULL    |       |
| parentGroupID   | smallint(6)   | YES  | MUL | NULL    |       |
| marketGroupName | varchar(100)  | YES  |     | NULL    |       |
| description     | varchar(3000) | YES  |     | NULL    |       |
| graphicID       | smallint(6)   | YES  | MUL | NULL    |       |
| hasTypes        | tinyint(1)    | YES  |     | NULL    |       |
+-----------------+---------------+------+-----+---------+-------+
*/
	@Id
	@Column(name="marketGroupID")
	private Long id;
	
	@Column(name="marketGroupName")
	private String name;
	
	@OneToOne(optional=true,cascade={CascadeType.ALL})
	@JoinColumn(name="parentGroupID", nullable=true)
	private MarketGroup parent;
	
	@Column(name="description")
	private String description;
	
	@Column(name="hasTypes")
	private int containsItems;

	public Long getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		return obj instanceof MarketGroup && ObjectUtils.equals( this.id , ((MarketGroup) obj).id );
	}
	
	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the path from a given <code>MarketGroup</code> to it's root
	 * <code>MarketGroup</code>.
	 * 
	 * @param group market group to find path for
	 * @return path with the first element being the top-level market group
	 * and the last element being the input <code>MarketGroup</code> 
	 */	
	public List<MarketGroup> getPathToRoot() 
	{
		final List<MarketGroup> result = new ArrayList<>();
		MarketGroup current = this;
		while( current != null ) {
			result.add( current );
			current = current.getParent();
		}
		Collections.reverse(result);
		return result;
	}	

	public MarketGroup getParent() {
		return parent;
	}
	
	public boolean isBlueprintsGroup() {
		if ( getName() == null ) {
			return false;
		}
		return getName().toLowerCase().contains("blueprint") || getDescription() != null && getDescription().toLowerCase().contains("blueprint");
	}

	public void setParent(MarketGroup parent) {
		this.parent = parent;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "MarketGroup[ "+getName()+" ("+this.id+") , parent="+parent+" ]";
	} 
	
	public boolean containsItems() {
		return containsItems != 0;
	}

	public void setContainsItems(boolean yesNo) {
		this.containsItems = yesNo ? 1 : 0;
	}
	
	// TODO: Dirty !!! Don't know how to determine item type in a better way ... :/
	public static boolean isMemberOfGroup(InventoryType type , String name) {
		
		MarketGroup group = type.getMarketGroup();
		while ( group != null ) {
			if ( group.getName().equalsIgnoreCase( name ) ) {
				return true;
			}
			group = group.getParent();
		}
		return false;
	}
}