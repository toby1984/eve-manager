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

import org.apache.commons.lang.ObjectUtils;

@Entity
@Table(name="mapRegions")
// @org.hibernate.annotations.Proxy(lazy=false)
public class Region {
	
	public static final Comparator<Region> BY_NAME_COMPARATOR =
		new Comparator<Region>() {

			@Override
			public int compare(Region o1, Region o2)
			{
				return o1.getName().compareTo( o2.getName() );
			}
		};
	/*
	 mysql> desc mapRegions;
+------------+--------------+------+-----+---------+-------+
| Field      | Type         | Null | Key | Default | Extra |
+------------+--------------+------+-----+---------+-------+
| regionID   | int(11)      | NO   | PRI | NULL    |       |
| regionName | varchar(100) | YES  |     | NULL    |       |
| x          | double       | YES  |     | NULL    |       |
| y          | double       | YES  |     | NULL    |       |
| z          | double       | YES  |     | NULL    |       |
| xMin       | double       | YES  |     | NULL    |       |
| xMax       | double       | YES  |     | NULL    |       |
| yMin       | double       | YES  |     | NULL    |       |
| yMax       | double       | YES  |     | NULL    |       |
| zMin       | double       | YES  |     | NULL    |       |
| zMax       | double       | YES  |     | NULL    |       |
| factionID  | int(11)      | YES  | MUL | NULL    |       |
| radius     | double       | YES  |     | NULL    |       |
+------------+--------------+------+-----+---------+-------+
	 */	

	@Id
	private Long regionID;
	
	@Column(name="regionName")
	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setID(Long regionID) {
		this.regionID = regionID;
	}

	public Long getID() {
		return regionID;
	}
	
	public boolean equals(Object obj) {
		if ( obj instanceof Region) {
			final Region r = (Region) obj;
			if ( this.regionID != null ) {
				return this.regionID.equals( r.getID() );
			} else if ( r.getID() != null ) {
				return r.getID().equals( this.regionID );
			}
			return this.regionID == r.regionID;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.regionID != null ? this.regionID.hashCode() : 0;
	}
	
	protected static Long getRegionId(Region r) {
		return r != null ? r.getID() : null;
	}
	
	public static boolean isSameRegion(Region r1,Region r2) {
		final Long id1 = getRegionId( r1 );
		final Long id2 = getRegionId( r2 );
		return ObjectUtils.equals( id1 , id2 );
	}
	
	@Override
	public String toString() {
		return "Region "+getName()+" ("+getID()+")";
	}

}
