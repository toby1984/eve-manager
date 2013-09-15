package de.codesourcery.eve.skills.db.datamodel;

import org.apache.log4j.Logger;

public class ItemAttribute {
	
	private static final Logger LOG = Logger.getLogger(ItemAttribute.class);

	/*
	 * | dgmTypeAttributes | CREATE TABLE `dgmTypeAttributes` ( `typeID` int(11)
	 * NOT NULL, `attributeID` smallint(6) NOT NULL, `valueInt` int(11) DEFAULT
	 * NULL, `valueFloat` double DEFAULT NULL, PRIMARY KEY
	 * (`typeID`,`attributeID`) ) ENGINE=MyISAM DEFAULT CHARSET=utf8 |
	 */
	private ItemAttributeType type;
	private Integer intValue;
	private Double floatValue;

	public ItemAttribute(ItemAttributeType type) {
		if ( type == null ) {
			throw new IllegalArgumentException("type must not be NULL");
		}
		this.type = type;
	}
	
	public ItemAttributeType getType() {
		return type;
	}
	
	public boolean hasType(ItemAttributeType type) {
		return type.getId().equals( this.type.getId() );
	}

	public Integer getIntValue() {
		return intValue;
	}
	
	public int safeGetIntValue() {
		if ( this.intValue != null ) {
			return this.intValue;
		}
		if ( this.floatValue != null ) {
			return this.floatValue.intValue();
		}
		String msg = "Internal error, safeGetIntValue() invoked on " +
				"attribute "+this.getType().getAttributeName()+" that has no value set?";
		LOG.error("safeGetIntValue(): "+msg);
		throw new RuntimeException(msg);
	}
	
	public double safeGetFloatValue() {
		if ( this.floatValue != null ) {
			return this.floatValue;
		}		
		if ( this.intValue != null ) {
			return this.intValue;
		}
		String msg = "Internal error, safeGetFloatValue() invoked on " +
				"attribute "+this.getType().getAttributeName()+" that has no value set?";
		LOG.error("safeGetFloatValue(): "+msg);
		throw new RuntimeException(msg);
	}	
	
	public void setIntValue(Integer intValue) {
		this.intValue = intValue;
	}

	public Double getFloatValue() {
		return floatValue;
	}

	public void setFloatValue(Double floatValue) {
		this.floatValue = floatValue;
	}

	public AttributeCategory getCategory() {
		return type.getCategory();
	}
}
