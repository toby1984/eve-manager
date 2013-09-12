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

import de.codesourcery.eve.skills.utils.ISKAmount;

@Entity
@Table(name = "ramAssemblyLineTypes")
// @org.hibernate.annotations.Proxy(lazy=false)
public class AssemblyLineType
{
	/*
	 * mysql> desc ramAssemblyLineTypes;
	 * +------------------------+--------------
	 * -------+------+-----+---------+-------+ | Field | Type | Null | Key |
	 * Default | Extra |
	 * +------------------------+---------------------+------+--
	 * ---+---------+-------+ | assemblyLineTypeID | tinyint(3) unsigned | NO |
	 * PRI | NULL | | | assemblyLineTypeName | varchar(100) | YES | | NULL | | |
	 * description | varchar(1000) | YES | | NULL | | | baseTimeMultiplier |
	 * double | YES | | NULL | | | baseMaterialMultiplier | double | YES | |
	 * NULL | | | volume | double | YES | | NULL | | | activityID | tinyint(3)
	 * unsigned | YES | MUL | NULL | | | minCostPerHour | double | YES | | NULL
	 * | |
	 * +------------------------+---------------------+------+-----+---------
	 * +-------+
	 */

	@Id
	@Column(name = "assemblyLineTypeID")
	private Long id;

	@Column(name = "assemblyLineTypeName")
	private String typeName;

	@Column(name = "description")
	private String description;

	@Column(name = "baseMaterialMultiplier")
	private double baseTimeMultiplier;

	@Column(name = "volume")
	private Double volume;

	@Column(name = "activityID")
	private int activityID;

	@Column(name = "minCostPerHour")
	private Double minCostPerHour;

	public Long getId()
	{
		return id;
	}

	public String getTypeName()
	{
		return typeName;
	}

	public String getDescription()
	{
		return description;
	}

	public double getBaseTimeMultiplier()
	{
		return baseTimeMultiplier;
	}

	public double getVolume()
	{
		return volume != null ? volume : 0;
	}

	public Activity getActivity()
	{
		return Activity.fromTypeId( activityID );
	}

	public ISKAmount getMinCostPerHour()
	{
		return minCostPerHour != null ? new ISKAmount( minCostPerHour ) : ISKAmount.ZERO_ISK;
	}

}
