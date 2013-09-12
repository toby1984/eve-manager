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

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;

import java.util.Date;

import de.codesourcery.eve.skills.datamodel.CorporationId;
import de.codesourcery.eve.skills.datamodel.FactionStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.Corporation;
import de.codesourcery.eve.skills.db.datamodel.Faction;

public class CorpStandingParserTest extends AbstractParserTest {

	public static final String XML = "<?xml version='1.0' encoding='UTF-8'?>\n<eveapi version=\"2\">\n" + 
			"  <currentTime>2013-09-12 14:52:13</currentTime>\n" + 
			"  <result>\n" + 
			"    <characterNPCStandings>\n" + 
			"      <rowset name=\"factions\" key=\"fromID\" columns=\"fromID,fromName,standing\">\n" + 
			"        <row fromID=\"1\" fromName=\"faction 1\" standing=\"1.5\" />\n" + 
			"        <row fromID=\"2\" fromName=\"faction 2\" standing=\"2.5\" />\n" + 
			"      </rowset>\n" + 
			"    </characterNPCStandings>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2013-09-12 17:49:13</cachedUntil>\n" + 
			"</eveapi>"; 
	
	public void testParse() {
	
		final IStaticDataModel factionDAO =
			createMock( IStaticDataModel.class );
		
		final Faction faction1 = new Faction();
		faction1.setID( new Long(1) );
		faction1.setName( "faction 1" );
		
		final Faction faction2 = new Faction();
		faction2.setID( new Long(2) );
		faction2.setName( "faction 2" );
		
		expect( factionDAO.getFaction( new Long(1) ) ).andReturn( faction1 ).once();
		expect( factionDAO.getFaction( new Long(2) ) ).andReturn( faction2 ).once();
		replay( factionDAO );
		
		final ICharacter character = new de.codesourcery.eve.skills.datamodel.Character("test");
		
		final FactionStandingParser parser = new FactionStandingParser(character , factionDAO , systemClock() );

		parser.parse(new Date() , XML );
		FactionStandings standings = parser.getResult();
		
		assertNotNull( standings );
		assertSame( character , standings.getCharacter() );
		assertEquals( 1.5f , standings.getFactionStanding( faction1 ).getValue() );
		assertEquals( 2.5f , standings.getFactionStanding( faction2 ).getValue() );
		
		verify( factionDAO );
	}
}
