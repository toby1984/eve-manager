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
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;

@Entity
@Table(name="invTypeMaterials")
public class TypeMaterial {

	/*
mysql> describe invTypeMaterials;
+----------------+---------+------+-----+---------+-------+
| Field          | Type    | Null | Key | Default | Extra |
+----------------+---------+------+-----+---------+-------+
| typeID         | int(11) | NO   | PRI | NULL    |       |
| materialTypeID | int(11) | NO   | PRI | NULL    |       |
| quantity       | int(11) | NO   |     | NULL    |       |
+----------------+---------+------+-----+---------+-------+
	 */

	@EmbeddedId
	private Id id2 = new Id();
	
	@ManyToOne
	@JoinColumn(name="materialTypeID", nullable=false,insertable=false,updatable=false)
	private InventoryType type;		
	
	@Column
	private int quantity;

	/**
	 * The item required to produce {@link #setId(InventoryType)}.
	 * 
	 * @return
	 */
	public InventoryType getType() {
		return type;
	}

	public int getQuantity() {
		return quantity;
	}

	@Embeddable
	public static class Id implements Serializable {
		
		private static final long serialVersionUID = -3286487012321326979L;

		@Column(name="typeID", nullable=false,insertable=false,updatable=false)
		private Long id;
		
		@Column(name="materialTypeID")
		private Long typeId;
		
		public Id() {
		}
		
		@Override
		public boolean equals(Object obj) {
			if ( ! ( obj instanceof Id ) ) {
				return false;
			}
			
			final Id other = (Id) obj;
			if ( ! ObjectUtils.equals( this.id , other.id ) ) {
				return false;
			}
			
			if ( ! ObjectUtils.equals( this.typeId , other.typeId ) ) {
				return false;
			}
			
			return ObjectUtils.equals( this.typeId , other.typeId );
		}
		
		private int hashCode(Object obj) {
			return obj == null ? 0 : obj.hashCode();
		}
		@Override
		public int hashCode() 
		{
			int result = 31;
			result += hashCode( id );
			result = result << 8 | (int) ( this.typeId * 31 );
			return result;
		}
	}	
}
