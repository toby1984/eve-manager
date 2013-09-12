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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="staStationTypes")
// @org.hibernate.annotations.Proxy(lazy=false)
public class StationType
{

	/*
	 mysql> desc staStationTypes;
+------------------------+---------------------+------+-----+---------+-------+
| Field                  | Type                | Null | Key | Default | Extra |
+------------------------+---------------------+------+-----+---------+-------+
| stationTypeID          | smallint(6)         | NO   | PRI | NULL    |       | 
| dockingBayGraphicID    | smallint(6)         | YES  | MUL | NULL    |       | 
| hangarGraphicID        | smallint(6)         | YES  | MUL | NULL    |       | 
| dockEntryX             | double              | YES  |     | NULL    |       | 
| dockEntryY             | double              | YES  |     | NULL    |       | 
| dockEntryZ             | double              | YES  |     | NULL    |       |
| dockOrientationX       | double              | YES  |     | NULL    |       |
| dockOrientationY       | double              | YES  |     | NULL    |       |
| dockOrientationZ       | double              | YES  |     | NULL    |       |
| operationID            | tinyint(3) unsigned | YES  | MUL | NULL    |       |
| officeSlots            | tinyint(4)          | YES  |     | NULL    |       |
| reprocessingEfficiency | double              | YES  |     | NULL    |       |
| conquerable            | tinyint(1)          | YES  |     | NULL    |       |
+------------------------+---------------------+------+-----+---------+-------+
	 */
	
	@Id
	@Column(name="stationTypeID")
	private Integer id;
	
	@Column(name="reprocessingEfficiency")
	private Double reprocessingEfficiency;
	
	@Column(name="operationID")
	private Integer operationID;
	
//	@org.hibernate.annotations.CollectionOfElements(fetch=FetchType.EAGER,
//			targetElement = StationService.class
//	)
	@org.hibernate.annotations.Type(
		type = "de.codesourcery.eve.skills.db.dao.StationServiceUserType"
	)	
	@OneToMany(targetEntity=StationOperatonServices.class)
//	@JoinTable(name="staOperationServices", 
//			joinColumns= { @JoinColumn(name="operatonID",referencedColumnName="operationID" , nullable=true) },
//			inverseJoinColumns = { @JoinColumn(name="serviceID") }
//	)
	private List<StationService> services = new ArrayList<StationService>();

	public List<StationService> getServices()
	{
		return services;
	}

	public double getReprocessingEfficiency()
	{
		return reprocessingEfficiency;
	}
}
