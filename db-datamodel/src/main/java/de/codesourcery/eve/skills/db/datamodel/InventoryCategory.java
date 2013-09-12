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

public enum InventoryCategory 
{
SYSTEM(0,"#System",false),                                  
OWNER(1,"Owner",false),                                     
CELESTIAL(2,"Celestial",true),                              
STATION(3,"Station",false),                                 
MATERIAL(4,"Material",true),                                
ACCESSORIES(5,"Accessories",true),                          
SHIP(6,"Ship",true),                                        
MODULE(7,"Module",true),                                    
CHARGE(8,"Charge",true),                                    
BLUEPRINT(9,"Blueprint",true),                              
TRADING(10,"Trading",false),                                
ENTITY(11,"Entity",false),                                  
BONUS(14,"Bonus",false),                                    
SKILL(16,"Skill",true),                                     
COMMODITY(17,"Commodity",true),                             
DRONE(18,"Drone",true),                                     
IMPLANT(20,"Implant",true),                                 
DEPLOYABLE(22,"Deployable",true),                           
STRUCTURE(23,"Structure",true),                             
REACTION(24,"Reaction",true),                               
ASTEROID(25,"Asteroid",true),                               
WORLDSPACE(26,"WorldSpace",false),                          
ABSTRACT(29,"Abstract",false),                              
APPAREL(30,"Apparel",true),                                 
SUBSYSTEM(32,"Subsystem",true),                             
ANCIENT_RELICS(34,"Ancient Relics",true),                   
DECRYPTORS(35,"Decryptors",true),                           
INFRASTRUCTURE_UPGRADES(39,"Infrastructure Upgrades",true), 
SOVEREIGNTY_STRUCTURES(40,"Sovereignty Structures",true),   
PLANETARY_INTERACTION(41,"Planetary Interaction",true),     
PLANETARY_RESOURCES(42,"Planetary Resources",true),         
PLANETARY_COMMODITIES(43,"Planetary Commodities",true),     
ORBITALS(46,"Orbitals",true),                               
PLACEABLES(49,"Placeables",false),                          
EFFECTS(53,"Effects",false),                                
LIGHTS(54,"Lights",false),                                  
CELLS(59,"Cells",false),                                    
SPECIAL_EDITION_ASSETS(63,"Special Edition Assets",true),   
INFANTRY(350001,"Infantry",false);                          
/*
mysql>select concat( replace( ucase(categoryName) , ' ' , '_') ,'(',categoryID, ',', '"' , categoryName , '"' , ',' ,case when published = 0 then 'false' else 'true' end , '),' ) from invCategories; 
+------------+-------------------------+-------------------------------------------------------+--------+-----------+
| categoryID | categoryName            | description                                           | iconID | published |
+------------+-------------------------+-------------------------------------------------------+--------+-----------+
|          2 | Celestial               |                                                       |   NULL |         1 |
|          4 | Material                |                                                       |     22 |         1 |
|          5 | Accessories             |                                                       |     33 |         1 |
|          6 | Ship                    |                                                       |   NULL |         1 |
|          7 | Module                  |                                                       |     67 |         1 |
|          8 | Charge                  |                                                       |   NULL |         1 |
|          9 | Blueprint               |                                                       |     21 |         1 |
|         16 | Skill                   | Where all the skills go under.                        |     33 |         1 |
|         17 | Commodity               |                                                       |      0 |         1 |
|         18 | Drone                   | Player owned and controlled drones.                   |      0 |         1 |
|         20 | Implant                 | Implant                                               |      0 |         1 |
|         22 | Deployable              |                                                       |      0 |         1 |
|         23 | Structure               | Player owned structure related objects                |      0 |         1 |
|         24 | Reaction                |                                                       |      0 |         1 |
|         25 | Asteroid                |                                                       |   NULL |         1 |
|         32 | Subsystem               | Subsystems for tech 3 ships                           |   NULL |         1 |
|         34 | Ancient Relics          |                                                       |   NULL |         1 |
|         35 | Decryptors              |                                                       |   NULL |         1 |
|         39 | Infrastructure Upgrades |                                                       |   NULL |         1 |
|         40 | Sovereignty Structures  |                                                       |   NULL |         1 |
|         41 | Planetary Interaction   | Stuff for planetary interaction                       |   NULL |         1 |
|         42 | Planetary Resources     | These are Items that can be extracted from a planet.  |   NULL |         1 |
|         43 | Planetary Commodities   |                                                       |   NULL |         1 |
+------------+-------------------------+-------------------------------------------------------+--------+-----------+
	 */

	private final boolean published;
	private final Long id;
	private final String name;

	public static final Comparator<InventoryCategory> BY_NAME_COMPARATOR = 
		new Comparator<InventoryCategory> () {

		@Override
		public int compare(InventoryCategory o1, InventoryCategory o2)
		{
			return o1.getCategoryName().compareTo( o2.getCategoryName() );
		}
	};

	private InventoryCategory(long id,String name) {
		this( id,name,true);
	}


	private InventoryCategory(long id,String name,boolean published) {
		this.id = id;
		this.name = name;
		this.published = published;
	}
	
	public boolean isPublished()
	{
		return published;
	}

	public Long getId() {
		return id;
	}

	public String getCategoryName() {
		return name;
	}

	public boolean isSkill() {
		return this == SKILL;
	}

	public static InventoryCategory fromTypeId(Long key) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be NULL");
		}
		for ( InventoryCategory cat : values() ) {
			if (cat.getId().equals( key ) ) {
				return cat;
			}
		}
		throw new IllegalArgumentException("Unknown category ID "+key);
	}
}