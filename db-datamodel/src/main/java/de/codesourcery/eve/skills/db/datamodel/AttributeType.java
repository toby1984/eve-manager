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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public enum AttributeType {
	// DO _NOT_ change the type ID values ... used in parsing /char/CharacterSheet.xml.aspx 
	CHARISMA("charisma" , "Charisma"),
	MEMORY("memory" , "Memory" ),
	PERCEPTION("perception" , "Perception"),
	WILLPOWER("willpower" , "Willpower" ),
	INTELLIGENCE("intelligence" , "Intelligence" );
	
	private final String typeId;
	private final String displayName;
	
	private static final Map<String,AttributeType> typeMap =
		new HashMap<String, AttributeType>();
	
	public static AttributeType getByTypeId(String attributeName) {
		
		if (StringUtils.isBlank(attributeName)) {
			throw new IllegalArgumentException(
					"attribute typeId cannot be blank / NULL");
		}
		
		final AttributeType result = typeMap.get( attributeName.toLowerCase() );
		if ( result == null ) {
			throw new IllegalArgumentException("Unknown attribute '"+attributeName+"'");
		}
		return result;
	}
	
	static {
		register( CHARISMA );
		register( MEMORY );
		register( PERCEPTION );
		register( WILLPOWER );
		register( INTELLIGENCE );
	}
	
	private static void register(AttributeType t) {
		typeMap.put( t.typeId, t );
	}
	
	private AttributeType(String typeId,String displayName) {
		this.typeId = typeId;
		this.displayName = displayName;
	}
	
	public String getTypeId() {
		return typeId;
	}
	@Override
	public String toString() {
		return typeId;
	}

	public String getDisplayName() {
		return displayName;
	}
}
