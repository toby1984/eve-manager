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

import de.codesourcery.eve.skills.datamodel.NPCCorpStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.EveName;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;

public class CharacterStandingsParserTest extends AbstractParserTest
{

	public static final String  XML = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
			"<eveapi version=\"2\">\n" + 
			"  <currentTime>2008-09-03 12:20:19</currentTime>\n" + 
			"  <result>\n" + 
			"    <standingsTo>\n" + 
			"      <rowset name=\"characters\" key=\"toID\" columns=\"toID,toName,standing\">\n" + 
			"        <row toID=\"123456\" toName=\"Test Ally\" standing=\"1\" />\n" + 
			"        <row toID=\"234567\" toName=\"Test Friend\" standing=\"0.5\" />\n" + 
			"        <row toID=\"345678\" toName=\"Test Enemy\" standing=\"-0.8\" />\n" + 
			"      </rowset>\n" + 
			"      <rowset name=\"corporations\" key=\"toID\" columns=\"toID,toName,standing\">\n" + 
			"        <row toID=\"456789\" toName=\"Test Bad Guy Corp\" standing=\"-1\" />\n" + 
			"      </rowset>\n" + 
			"    </standingsTo>\n" + 
			"    <standingsFrom>\n" + 
			"      <rowset name=\"agents\" key=\"fromID\" columns=\"fromID,fromName,standing\">\n" + 
			"        <row fromID=\"3009841\" fromName=\"Pausent Ansin\" standing=\"0.1\" />\n" + 
			"        <row fromID=\"3009846\" fromName=\"Charie Octienne\" standing=\"0.19\" />\n" + 
			"      </rowset>\n" + 
			"      <rowset name=\"NPCCorporations\" key=\"fromID\" columns=\"fromID,fromName,standing\">\n" + 
			"        <row fromID=\"1000061\" fromName=\"Freedom Extension\" standing=\"0\" />\n" + 
			"        <row fromID=\"1000064\" fromName=\"Carthum Conglomerate\" standing=\"0.34\" />\n" + 
			"        <row fromID=\"1000094\" fromName=\"TransStellar Shipping\" standing=\"0.02\" />\n" + 
			"      </rowset>\n" + 
			"      <rowset name=\"factions\" key=\"fromID\" columns=\"fromID,fromName,standing\">\n" + 
			"        <row fromID=\"500003\" fromName=\"Amarr Empire\" standing=\"-0.1\" />\n" + 
			"        <row fromID=\"500020\" fromName=\"Serpentis\" standing=\"-1\" />\n" + 
			"      </rowset>\n" + 
			"    </standingsFrom>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2008-09-03 15:20:19</cachedUntil>\n" + 
			"</eveapi>";
	
	public void test() {
	
		// TODO: No longer works with Tyrannis , use Contact List API instead...
		
//		final EveName n1 = new EveName(1L,"Corp #1");
//		final EveName n2 = new EveName(2L,"Corp #2");
//		final EveName n3 = new EveName(3L,"Corp #3");
//		
//		final NPCCorporation corp1 = new NPCCorporation(1000061L , n1 );
//		final NPCCorporation corp2 = new NPCCorporation(1000064L , n2 );
//		final NPCCorporation corp3 = new NPCCorporation(1000094L , n3 );
//		
//		final IStaticDataModel dataModel =
//			createMock( IStaticDataModel.class );
//		
//		expect( dataModel.getNPCCorporation( 1000061L ) ).andReturn( corp1 ).atLeastOnce();
//		expect( dataModel.getNPCCorporation( 1000064L ) ).andReturn( corp2 ).atLeastOnce();
//		expect( dataModel.getNPCCorporation( 1000094L ) ).andReturn( corp3 ).atLeastOnce();
//		
//		replay( dataModel );
//		
//		final ICharacter character = createNiceMock(ICharacter.class);
//		
//		replay( character );
//		
//		final CharacterStandingsParser parser =
//			new CharacterStandingsParser(character , dataModel , systemClock() );
//
//		parser.parse(new Date() , XML );
//		
//		CharacterStandings standings = parser.getResult();
//		
//		assertNotNull( standings );
//		assertSame( character , standings.getCharacter() );
//		
//		final Standing<NPCCorporation> s1 = standings.getNPCCorpStanding( corp1 );
//		assertNotNull( s1 );
//		assertEquals( 0.0f , s1.getValue() );
//		assertSame( corp1 , s1.getFrom() );
//	
//		final Standing<NPCCorporation> s2 = standings.getNPCCorpStanding( corp2 );
//		assertNotNull( s2 );
//		assertEquals( 0.34f , s2.getValue() );
//		assertSame( corp2 , s2.getFrom() );
//		
//		final Standing<NPCCorporation> s3 = standings.getNPCCorpStanding( corp3 );
//		assertNotNull( s3 );
//		assertEquals( 0.02f , s3.getValue() );
//		assertSame( corp3 , s3.getFrom() );
//		
//		verify( dataModel );
		
	}
}
