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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.datamodel.Prerequisite;

@Entity
@Table(name="ramTypeRequirements")
// @org.hibernate.annotations.Proxy(lazy=false)
public class TypeActivityMaterials {

	/*
	mysql> desc ramTypeRequirements;
+----------------+---------------------+------+-----+---------+-------+
| Field          | Type                | Null | Key | Default | Extra |
+----------------+---------------------+------+-----+---------+-------+
| typeID         | int(11)             | NO   | PRI | NULL    |       |
| activityID     | tinyint(3) unsigned | NO   | PRI | NULL    |       |
| requiredTypeID | int(11)             | NO   | PRI | NULL    |       |
| quantity       | int(11)             | YES  |     | NULL    |       |
| damagePerJob   | double              | YES  |     | NULL    |       |
| recycle        | tinyint(1)          | YES  |     | NULL    |       |
+----------------+---------------------+------+-----+---------+-------+ 
	 */
	
	@Embeddable
	public static class Id implements Serializable {
		
		private static final long serialVersionUID = -3286487012321326979L;

		@Column(name="typeID")
		private Long typeID;
		
		@Column(name="activityID")
		private Integer activityID;
		
		@Column(name="requiredTypeID")
		private Long requiredTypeID;
		
		public Id() {
		}
		
		@Override
		public boolean equals(Object obj) {
			if ( ! ( obj instanceof Id ) ) {
				return false;
			}
			
			final Id other = (Id) obj;
			if ( ! ObjectUtils.equals( this.typeID , other.typeID ) ) {
				return false;
			}
			
			if ( ! ObjectUtils.equals( this.activityID , other.activityID ) ) {
				return false;
			}
			
			return ObjectUtils.equals( this.requiredTypeID , other.requiredTypeID );
		}
		
		private int hashCode(Object obj) {
			return obj == null ? 0 : obj.hashCode();
		}
		@Override
		public int hashCode() {
			return hashCode( typeID ) + hashCode( activityID ) *31+ hashCode( requiredTypeID) *2*31;
		}
	}
	
	@EmbeddedId
	private Id id = new Id();
	
	@ManyToOne
	@JoinColumn(name="typeID", nullable=false,insertable=false,updatable=false)
	private InventoryType type;
	
	@org.hibernate.annotations.Type(
		type = "de.codesourcery.eve.skills.db.dao.ActivityUserType"
	)
	@Column(name="activityID",nullable=false,insertable=false,updatable=false)
	private Activity activity;
	
	@ManyToOne
	@JoinColumn(name="requiredTypeID", nullable=false,insertable=false,updatable=false)	
	private InventoryType requiredType;
	
	@Column
	private int quantity;
	
	@Column
	private double damagePerJob;
	
	@Column(name="recycle")
	private boolean recycle;
	
	/**
	 * The value of this field can no
	 * longer (since Tyrannis) be derived
	 * from the database since it only
	 * depends from which of the two
	 * tables ( invTypeMaterials or ramTypeRequirements)
	 * the data was read. 
	 * 
	 * It is currently populated inside of {@link BlueprintTypeDAO}.
	 */
	private transient boolean subjectToManufacturingWaste;
	
	@Override
	public String toString() {
		return "TypeActivityMaterials[ "+activity+" , type="+
			type+", required = "+requiredType+" , quantity="+quantity+" ]";
	}

	public boolean isSkill() {
		return type != null && type.isSkill();
	}
	
	public InventoryType getType() {
		return type;
	}

	public void setType(InventoryType type) {
		this.type = type;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public InventoryType getRequiredType() {
		return requiredType;
	}
	
	public void setRequiredType(InventoryType requiredType) {
		this.requiredType = requiredType;
	}

	public Id getId() {
		return id;
	}
	
	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getDamagePerJob() {
		return damagePerJob;
	}

	public void setDamagePerJob(double damagePerJob) {
		this.damagePerJob = damagePerJob;
	}

	public boolean isRecycle() {
		return recycle;
	}

	public void setRecycle(boolean recycle) {
		this.recycle = recycle;
	}

	public void setSubjectToManufacturingWaste(boolean subjectToManufacturingWaste) {
		this.subjectToManufacturingWaste = subjectToManufacturingWaste;
	}

	public boolean isSubjectToManufacturingWaste() {
		return subjectToManufacturingWaste;
	}
	
}
