package de.codesourcery.eve.skills.db.datamodel;

public enum AttributeCategory {

	/*
select concat(concat(concat(concat(concat(concat(concat(concat(concat(upper(categoryName),"("),categoryID),","),"\""),categoryName),"\""),",\""),categoryDescription),"\")") from dgmAttributeCategories;
	 
	 */
	FITTING(1,"Fitting","Fitting capabilities of a ship"),
	SHIELD(2,"Shield","Shield attributes of ships"),
	ARMOR(3,"Armor","Armor attributes of ships"),
	STRUCTURE(4,"Structure","Structure attributes of ships"),
	CAPACITOR(5,"Capacitor","Capacitor attributes for ships"),
	TARGETING(6,"Targeting","Targeting Attributes for ships"),
	MISCELLANEOUS(7,"Miscellaneous","Misc. attributes"),
	REQUIRED_SKILLS(8,"Required Skills","Skill requirements"),
	NULL(9,"NULL","Attributes already checked and not going into a category"),
	DRONES(10,"Drones","All you need to know about drones"),
	AI(12,"AI","Attribs for the AI configuration");
	
	private long id;
	private String name;
	private String description;
	
	private AttributeCategory(long id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public long getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public static AttributeCategory valueOf(Long id) 
	{
		if ( id == null ) {
			return AttributeCategory.NULL;
		}
		for ( AttributeCategory cat : values() ) {
			if ( cat.id == id ) {
				return cat;
			}
		}
		throw new IllegalArgumentException("Unknown attribute category ID #"+id);
	}
}