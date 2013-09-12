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
import de.codesourcery.eve.skills.datamodel.NPCCorpStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * @author tobi
 */
public class NPCCorpCharacterStandingParser extends AbstractResponseParser<NPCCorpStandings>
{
	public static final Logger log = Logger.getLogger(NPCCorpCharacterStandingParser.class);
	
	public static final URI URI = toURI("/char/Standings.xml.aspx");

	private final ICharacter character;
	private final IStaticDataModel dataModel;
	
	private NPCCorpStandings charStandings;
	
	/* NEW
	 * 
	 * <?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2013-09-12 14:52:13</currentTime>
  <result>
    <characterNPCStandings>
      <rowset name="agents" key="fromID" columns="fromID,fromName,standing">
        <row fromID="3018924" fromName="Zidah Arvo" standing="1.93" />
      </rowset>
      <rowset name="NPCCorporations" key="fromID" columns="fromID,fromName,standing">
        <row fromID="1000020" fromName="Lai Dai Corporation" standing="0.00" />
      </rowset>
      <rowset name="factions" key="fromID" columns="fromID,fromName,standing">
        <row fromID="500001" fromName="Caldari State" standing="4.05" />
      </rowset>
    </characterNPCStandings>
  </result>
  <cachedUntil>2013-09-12 17:49:13</cachedUntil>
</eveapi>
	 */
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException
	{
		final NPCCorpStandings tmpStandings = 
			new NPCCorpStandings( character );
		
		charStandings = tmpStandings;
		
		final Element parent = getChild( getResultElement( document ) , "characterNPCStandings" );
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
	
	public NPCCorpCharacterStandingParser(ICharacter character,IStaticDataModel dataModel , ISystemClock clock) {
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
	public NPCCorpStandings getResult() throws IllegalStateException
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
