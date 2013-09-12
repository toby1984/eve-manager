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
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.BaseCharacter;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Parses the server's response to a 
 * /account/Characters.xml.aspx POST.
 *  
 * API version 1.0
 * @author tobias.gierke@code-sourcery.de
 */
public class GetAvailableCharactersParser extends AbstractResponseParser<List<IBaseCharacter>> {
	
	private static final URI URI = toURI("/account/Characters.xml.aspx");
	
	private List<IBaseCharacter> result = new ArrayList<IBaseCharacter>();

	public GetAvailableCharactersParser(ISystemClock clock) {
		super(clock);
	}

	/*
		<?xml version='1.0' encoding='UTF-8'?>
		<eveapi version="1">
		  <currentTime>2007-12-12 11:48:50</currentTime>
		  <result>
		    <rowset name="characters" key="characterID" columns="name,characterID,corporationName,corporationID">
		      <row name="Mary" characterID="150267069"
		           corporationName="Starbase Anchoring Corp" corporationID="150279367" />
		      <row name="Marcus" characterID="150302299"
		           corporationName="Marcus Corp" corporationID="150333466" />
		      <row name="Dieinafire" characterID="150340823"
		           corporationName="Center for Advanced Studies" corporationID="1000169" />
		    </rowset>
		  </result>
		  <cachedUntil>2007-12-12 12:48:50</cachedUntil>
		</eveapi>	 
	 */
	@Override
	protected void parseHook(Document document) throws UnparseableResponseException
	{
		final RowSet rowSet =
			parseRowSet( "characters" , document );
		
		for ( Row r : rowSet ) {
			final de.codesourcery.eve.skills.datamodel.BaseCharacter bc = new BaseCharacter();
			bc.setCharacterId( new CharacterID( r.get("characterID" ) ) );
			bc.setName( r.get("name" ) );
			result.add( bc );
		}
	}
	
	public List<IBaseCharacter> getResult() {
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset() {
		result.clear();
	}

	@Override
	public URI getRelativeURI() {
		return URI;
	}

}
