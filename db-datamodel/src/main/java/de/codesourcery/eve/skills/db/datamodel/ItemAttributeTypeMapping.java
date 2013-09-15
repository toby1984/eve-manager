package de.codesourcery.eve.skills.db.datamodel;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang.ObjectUtils;

@Entity
@Table(name="dgmTypeAttributes")
@org.hibernate.annotations.Proxy(lazy=false)
public class ItemAttributeTypeMapping 
{
/*
 | dgmTypeAttributes | CREATE TABLE `dgmTypeAttributes` (
  `typeID` int(11) NOT NULL,
  `attributeID` smallint(6) NOT NULL,
  `valueInt` int(11) DEFAULT NULL,
  `valueFloat` double DEFAULT NULL,
  PRIMARY KEY (`typeID`,`attributeID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 |

 */
	
	@EmbeddedId
	private Id id;
	
	@Column(name="valueInt",nullable=true)
	private Integer valueInt;
	
	@Column(name="valueFloat",nullable=true)
	private Double valueFloat;
	
	@Embeddable
	public static class Id implements Serializable {
		
		private static final long serialVersionUID = -3286487012321326979L;

		@Column(name="typeID", nullable=false,insertable=false,updatable=false)
		private Long id;
		
		@Column(name="attributeID")
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