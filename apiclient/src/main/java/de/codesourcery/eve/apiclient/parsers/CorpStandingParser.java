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
import de.codesourcery.eve.skills.datamodel.CorpStandings;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.Corporation;
import de.codesourcery.eve.skills.db.datamodel.Faction;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class CorpStandingParser extends AbstractResponseParser<CorpStandings> {
	
	public static final Logger log = Logger.getLogger(CorpStandingParser.class);

	public static final URI URI = toURI("/corp/Standings.xml.aspx");

	private CorpStandings corpStandings;
	
	/*
<?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2008-09-02 18:08:40</currentTime>
  <result>
    <corporationStandings>
      <standingsTo>
        <rowset name="characters" key="toID" columns="toID,toName,standing">
        </rowset>
        <rowset name="corporations" key="toID" columns="toID,toName,standing">
        </rowset>
        <rowset name="alliances" key="toID" columns="toID,toName,standing">
        </rowset>
      </standingsTo>
      <standingsFrom>
        <rowset name="agents" key="fromID" columns="fromID,fromName,standing">
        </rowset>
        <rowset name="NPCCorporations" key="fromID" columns="fromID,fromName,standing">
        </rowset>
        <rowset name="factions" key="fromID" columns="fromID,fromName,standing">
        </rowset>
      </standingsFrom>
    </corporationStandings>
    <allianceStandings>
      <standingsTo>
        <rowset name="corporations" key="toID" columns="toID,toName,standing">
        </rowset>
        <rowset name="alliances" key="toID" columns="toID,toName,standing">
        </rowset>
      </standingsTo>
    </allianceStandings>
  </result>
  <cachedUntil>2008-09-02 21:08:41</cachedUntil>
</eveapi>
	 */
	
	private Corporation corporation;
	private IStaticDataModel staticDataModel;
	
	public CorpStandingParser(Corporation toCorporation , 
			IStaticDataModel staticDataModel,ISystemClock clock) 
	{
		super( clock );
		if (staticDataModel == null) {
			throw new IllegalArgumentException("Static datamodel cannot be NULL");
		}
		
		if ( toCorporation == null ) {
			throw new IllegalArgumentException("corporation cannot be NULL");
		}
		
		this.corporation = toCorporation;
		this.staticDataModel = staticDataModel;
	}
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException {
		
		corpStandings = new CorpStandings(corporation);

		final Element resultElement = getResultElement( document );
		
		// parse corp standings
		final Element corpStandingElement = 
			getChild( resultElement , "corporationStandings" );
		
		// TODO: parse standings from this corp to other entities
		// TODO: parse standings from this corp to characters
		// TODO: parse standings from this corp to alliances
		// TODO: parse standings from this corp to other corporations
		
		// parse standings from other entities towards this corp
		final Element standingsFrom =
			getChild( corpStandingElement , "standingsFrom" );

		// TODO: parse agents standings towards this corp
		// TODO: parse NPC corp standings towards this corp
		
		// parse faction standings towards this corp
		final Element factionStandings =
			getRowSetNode( standingsFrom , "factions" );
		
		final RowSet rowSet =
			parseRowSet("factions" , factionStandings );
		
		for ( int i = 0 ; i < rowSet.size() ; i++ ) 
		{
			
			final Row r = rowSet.getRow( i );

			final float standingValue =
				r.getFloat( "standing" ) ;
			
			final long factionId =
				r.getLong( "fromID" );
			
			final Standing<Faction> factionStanding =
				new Standing<Faction>( staticDataModel.getFaction( factionId ));

			factionStanding.setValue( standingValue );
			
			corpStandings.addFactionStanding( factionStanding );
		}
		
	}

	@Override
	public URI getRelativeURI() {
		return URI;
	}

	@Override
	public CorpStandings getResult() throws IllegalStateException {
		assertResponseParsed();
		return corpStandings;
	}

	@Override
	public void reset() {
		corpStandings = null;
	}

}
