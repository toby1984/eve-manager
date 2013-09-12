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


/**
 * Constants taken from Apocrypha 1.2 DB dump (unpublished activities excluded).
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public enum Activity implements Serializable {
	 NONE(0,"None" , "No activity" ),
	 MANUFACTURING(1,"Manufacturing" , "Manufacturing"),
	 RESEARCH_TECHNOLOGY(2,"Researching Technology" , "Technological research" , false ),
	 RESEARCH_TIME(3,"Researching Time Productivity" , "Researching productivity time" ),
	 RESEARCH_MATERIAL(4,"Researching Material Productivity" , "Researching material Productivity" ),
	 COPYING(5,"Copying" , "Copying" ),
	 REFINING(6,"Duplicating" , "The process of creating an item, by studying an already existing item" , false ),
	 REVERSE_ENGINEERING(7,"Reverse Engineering" , "The process of creating a blueprint from an item" ),
	 INVENTION(8,"Invention","The process of creating a more advanced item based on an existing item" );

/* 
 * Taken from Apocrypha 1.2 DB dump (unpublished activities excluded)
 * 
 +------------+-----------------------------------+--------+------------------------------------------------------------------------+-----------+
| activityID | activityName                      | iconNo | description                                                            | published |
+------------+-----------------------------------+--------+------------------------------------------------------------------------+-----------+
|          0 | None                              | NULL   | No activity                                                            |         1 |
|          1 | Manufacturing                     | 18_02  | Manufacturing                                                          |         1 |
|          2 | Researching Technology            | 33_02  | Technological research                                                 |         0 |
|          3 | Researching Time Productivity     | 33_02  | Researching productivity time                                          |         1 |
|          4 | Researching Material Productivity | 33_02  | Researching material productivity                                      |         1 |
|          5 | Copying                           | 33_02  | Copying                                                                |         1 |
|          6 | Duplicating                       | NULL   | The process of creating an item, by studying an already existing item. |         0 |
|          7 | Reverse Engineering               | 33_02  | The process of creating a blueprint from an item.                      |         1 |
|          8 | Invention                         | 33_02  | The process of creating a more advanced item based on an existing item |         1 |
+------------+-----------------------------------+--------+------------------------------------------------------------------------+-----------+*/

	private final int id;
	private final String name;
	private final String description;
	private final boolean isPublished;
	
	private Activity(int id,String name,String description) {
		this(id,name,description,true);
	}
	
	private Activity(int id,String name,String description,boolean isPublished) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.isPublished = isPublished;;
	}
	
	public static Activity fromTypeId(int id) {
		switch( id ) {
		case 0:
			return Activity.NONE;
		case 1:
			return Activity.MANUFACTURING;
		case 2:
			return Activity.RESEARCH_TECHNOLOGY;
		case 3:
			return Activity.RESEARCH_TIME;
		case 4:
			return Activity.RESEARCH_MATERIAL;
		case 5:
			return Activity.COPYING;
		case 6:
			return Activity.REFINING;
		case 7:
			return Activity.REVERSE_ENGINEERING;
		case 8:
			return Activity.INVENTION;
			
			default:
				throw new IllegalArgumentException("Unknown activity ID "+id);
		}
	}
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public boolean isPublished() {
		return isPublished;
	}

}
