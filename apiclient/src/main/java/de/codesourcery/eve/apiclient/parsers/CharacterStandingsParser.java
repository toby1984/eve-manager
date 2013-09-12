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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.CharacterStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * TODO: No longer works , need to implement Contact List API (since Tyrannis expansion).
 * @author tobi
 *
 */
public class CharacterStandingsParser extends AbstractResponseParser<CharacterStandings>
{
	public static final Logger log = Logger.getLogger(CharacterStandingsParser.class);
	
	public static final URI URI = toURI("/char/Standings.xml.aspx");

	private final ICharacter character;
	private final IStaticDataModel dataModel;
	
	private CharacterStandings charStandings;
	
	/*
	/*
<?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2008-09-03 12:20:19</currentTime>
  <result>
    <standingsTo>
      <rowset name="characters" key="toID" columns="toID,toName,standing">
        <row toID="123456" toName="Test Ally" standing="1" />
        <row toID="234567" toName="Test Friend" standing="0.5" />
        <row toID="345678" toName="Test Enemy" standing="-0.8" />
      </rowset>
      <rowset name="corporations" key="toID" columns="toID,toName,standing">
        <row toID="456789" toName="Test Bad Guy Corp" standing="-1" />
      </rowset>
    </standingsTo>
    <standingsFrom>
      <rowset name="agents" key="fromID" columns="fromID,fromName,standing">
        <row fromID="3009841" fromName="Pausent Ansin" standing="0.1" />
        <row fromID="3009846" fromName="Charie Octienne" standing="0.19" />
      </rowset>
      <rowset name="NPCCorporations" key="fromID" columns="fromID,fromName,standing">
        <row fromID="1000061" fromName="Freedom Extension" standing="0" />
        <row fromID="1000064" fromName="Carthum Conglomerate" standing="0.34" />
        <row fromID="1000094" fromName="TransStellar Shipping" standing="0.02" />
      </rowset>
      <rowset name="factions" key="fromID" columns="fromID,fromName,standing">
        <row fromID="500003" fromName="Amarr Empire" standing="-0.1" />
        <row fromID="500020" fromName="Serpentis" standing="-1" />
      </rowset>
    </standingsFrom>
  </result>
  <cachedUntil>2008-09-03 15:20:19</cachedUntil>
</eveapi>
	 
	 */
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException
	{

		final CharacterStandings tmpStandings = 
			new CharacterStandings( character );
		
		charStandings = tmpStandings;
		
		if ( 1 != 2 ) {
			log.warn("parseHook(): TODO: Character standings do not work , API change since Tyrannis ... fix me");
			return;
		}
		
		final Element parent = getChild( getResultElement( document ) , "standingsFrom" );
		final Element rowSetNode = getRowSetNode( parent , "NPCCorporations" );
		
		for ( Row r : parseRowSet( "NPCCorporations", rowSetNode ) ) 
		{
			long corpId = r.getLong( "fromID" );
			final float standing = r.getFloat( "standing" );
			
			final Standing<NPCCorporation> s =
				new Standing<NPCCorporation>( dataModel.getNPCCorporation( corpId ) );
			
			s.setValue( standing );
			
			tmpStandings.addNPCCorporationStanding( s );
		}
	}
	
	public CharacterStandingsParser(ICharacter character,IStaticDataModel dataModel , ISystemClock clock) {
		super( clock );
		if ( character == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.dataModel = dataModel;
		this.character = character;
	}

	@Override
	public URI getRelativeURI()
	{
		return URI;
	}

	@Override
	public CharacterStandings getResult() throws IllegalStateException
	{
		assertResponseParsed();
		return charStandings;
	}

	@Override
	public void reset()
	{
		charStandings = null;
	}


}
