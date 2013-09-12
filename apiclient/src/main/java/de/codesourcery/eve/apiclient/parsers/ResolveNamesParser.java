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
package de.codesourcery.eve.apiclient.parsers;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;

import de.codesourcery.eve.apiclient.IAPIClient.EntityType;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class ResolveNamesParser extends AbstractResponseParser<Map<String,String>> {

	public static final URI URI = toURI("/eve/CharacterName.xml.aspx");

	private final Set<EntityType> types;
	private final Map<String,String> result = new HashMap<String,String>();

	public ResolveNamesParser(String[] ids,Set<EntityType> idTypes,ISystemClock clock) {
		
		super(clock);
		
		if ( ArrayUtils.isEmpty( ids ) ) {
			throw new IllegalArgumentException("ids cannot be NULL / empty ");
		}
		
		if ( idTypes == null || idTypes.isEmpty()  ) {
			throw new IllegalArgumentException("entity types cannot be NULL / empty ");
		}
		
		this.types = idTypes;
		
		for ( String id : ids ) {
			if ( id == null ) {
				throw new IllegalArgumentException("NULL id ?");
			}
			result.put( id , null );
		}
	}
	
	/*
	 * <eveapi version="2">
	 *   <currentTime>2009-06-18 16:08:23</currentTime>
	 *   <result>
	 *     <rowset name="characters" key="characterID" columns="name,characterID">
	 *        <row name="CCP Garthagk" characterID="797400947"/>
	 *      </rowset>
	 *   </result> 
	 *   <cachedUntil>2009-07-18 16:08:23</cachedUntil>
	 * </eveapi>
	 */
	@Override
	void parseHook(Document document) throws UnparseableResponseException {

		for ( EntityType type : this.types ) {
			if ( type == EntityType.CHARACTER ) {
				parseRowSet( parseRowSet( "characters" , document , false ) );
			} else if ( type == EntityType.CORPORATION ) {
				parseRowSet( parseRowSet( "corporations" , document , false ) );
			} else {
				throw new RuntimeException("Internal error, unhandled entity type "+type);
			}
		}
	}
	
	protected void parseRowSet(RowSet set) {
		
		if ( set == null ) {
			return;
		}
		
		for ( Row r : set ) {
			final String name= r.get("name");
			// scan attributes for matching ID
			boolean foundMatch = false;
			String value;
			for ( String attr : r.getColumnNames() ) {
				if ( "name".equals( attr ) ) {
					continue;
				}
				value = 
					r.get( attr );
				if ( this.result.containsKey( value ) ) {
					foundMatch = true;
					if ( result.put( value , name ) != null ) {
						throw new UnparseableResponseException("Response contains more than one entry with ID "+value+" ?");
					}
					break;
				}
			}
			if ( ! foundMatch ) {
				throw new UnparseableResponseException("Found no attribute containing one of the requested IDs in row "+r);
			}
		}
	}

	@Override
	public URI getRelativeURI() {
		return URI;
	}

	@Override
	public Map<String,String> getResult() throws IllegalStateException {
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset() {
		final Set<String> keys = new HashSet<String>( result.keySet() );
		for ( String key : keys ) {
			result.put( key , null );
		}
	}

}
