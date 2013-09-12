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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="staOperationServices")
// @org.hibernate.annotations.Proxy(lazy=false)
public class StationOperatonServices
{
/*
 mysql> desc staOperationServices;
+-------------+---------------------+------+-----+---------+-------+
| Field       | Type                | Null | Key | Default | Extra |
+-------------+---------------------+------+-----+---------+-------+
| operationID | tinyint(3) unsigned | NO   | PRI | NULL    |       |
| serviceID   | int(11)             | NO   | PRI | NULL    |       |
+-------------+---------------------+------+-----+---------+-------+
 */
	
	@Embeddable
	public static class Id implements Serializable {
		
		private static final long serialVersionUID = 1L;

		@Column(name="operationID")
		private Integer operationID;
		
		@org.hibernate.annotations.Type(
				type = "de.codesourcery.eve.skills.db.dao.StationServiceUserType"
			)
		@Column(name="serviceID",nullable=true)
		private StationService serviceID;
		
		public Id() {
		}
		
		public Id(Integer id,StationService service) {
			this.operationID = id;
			this.serviceID = service;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if ( obj instanceof Id) {
				final Id id = (Id) obj;
				return this.operationID.equals( id.operationID ) && this.serviceID.equals( id.serviceID );
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return operationID.hashCode() + 31*serviceID.hashCode();
		}
	}
	
	@EmbeddedId
	private Id id;

	public StationService getService()
	{
		return id != null ? id.serviceID : null;
	}
}
