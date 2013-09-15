package de.codesourcery.eve.skills.db.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="dgmAttributeTypes")
@org.hibernate.annotations.Proxy(lazy=false)
public class ItemAttributeType 
{
	/*
  `attributeID` smallint(6) NOT NULL,
  `attributeName` varchar(100) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `iconID` int(11) DEFAULT NULL,
  `defaultValue` double DEFAULT NULL,
  `published` tinyint(1) DEFAULT NULL,
  `displayName` varchar(100) DEFAULT NULL,
  `unitID` tinyint(3) unsigned DEFAULT NULL,
  `stackable` tinyint(1) DEFAULT NULL,
  `highIsGood` tinyint(1) DEFAULT NULL,
  `categoryID` tinyint(3) unsigned DEFAULT NULL,
  PRIMARY KEY (`attributeID`)	 
	 */
	@Id
	@Column(name="attributeID")
	private long id;
	
	@Column(name="attributeName")
	private String attributeName;
	
	@Column(name="displayName")
	private String displayName;
	
	@Column(name="published")
	private Integer published;
	
	@SuppressWarnings("unused")
	@Column(name="unitID")
	private Integer unitId;
	
	@Column(name="defaultValue")
	private Double defaultValue;
	
	@Column(name="stackable")
	private Integer stackable;
	
	@Column(name="highIsGood")
	private Integer highIsGood;
	
	@Column(name="description",length=1024)
	private String description;
	
	@Column(name="categoryID",nullable=true)
	private Long categoryId;
	
	private transient AttributeCategory category;

	public Long getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return 31  + (int) (id ^ (id >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof ItemAttributeType) {
			return id == ((ItemAttributeType) obj).id; 
		}
		return false;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isPublished() {
		return published != null && published > 0;
	}

	public Double getDefaultValue() {
		return defaultValue;
	}

	public boolean isStackable() {
		return stackable != null && stackable > 0;
	}

	public boolean getHighIsGood() {
		return highIsGood != null && highIsGood > 0;
	}

	public String getDescription() {
		return description;
	}

	public void setId(long typeId) {
		this.id = typeId;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setPublished(Integer published) {
		this.published = published;
	}

	public void setDefaultValue(Double defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setStackable(Integer stackable) {
		this.stackable = stackable;
	}

	public void setHighIsGood(Integer highIsGood) {
		this.highIsGood = highIsGood;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setCategory(AttributeCategory category) {
		this.category = category;
	}
	
	public AttributeCategory getCategory() {
		if ( category == null ) {
			if ( categoryId == null ) {
				throw new IllegalStateException("No category nor categoryID set on attribute type "+this.attributeName+" ("+this.id+")");
			}
			category = AttributeCategory.valueOf( categoryId );
		}
		return category;
	}
	
	public boolean hasCategory(AttributeCategory cat) {
		return cat.equals( this.category );
	}
}