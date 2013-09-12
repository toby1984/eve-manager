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

import java.util.Date;
import java.util.List;

import junit.framework.AssertionFailedError;
import de.codesourcery.eve.apiclient.exceptions.APIErrorException;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;

public class GetAvailableCharactersParserTest extends AbstractParserTest {

	private final String XML1 = "<?xml version='1.0' encoding='UTF-8'?>" + 
			"<eveapi version=\"2\">" + 
			"  <currentTime>2009-06-01 13:54:11</currentTime>" + 
			"  <result>" + 
			"    <rowset name=\"characters\" key=\"characterID\" columns=\"name,characterID,corporationName,corporationID\">" + 
			"      <row name=\"twink123123\" characterID=\"123456\" corporationName=\"School of Applied Knowledge\" corporationID=\"37\" />" + 
			"      <row name=\"twink37\" characterID=\"1335\" corporationName=\"Royal Amarr Institute\" corporationID=\"48\" />" + 
			"    </rowset>" + 
			"  </result>" + 
			"  <cachedUntil>2009-06-01 14:54:11</cachedUntil>" + 
			"</eveapi>";
	
	private final String XML_WITH_ERROR  = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
			"<eveapi version=\"2\">\n" + 
			"  <currentTime>2008-12-16 17:12:07</currentTime>\n" + 
			"  <error code=\"106\">Must provide userID parameter for authentication.</error>\n" + 
			"  <cachedUntil>2008-12-16 17:17:07</cachedUntil>\n" + 
			"</eveapi>\n" + 
			"";
	
	protected void assertContainsCharacterWithID(List<IBaseCharacter> chars, CharacterID id) {
		for ( IBaseCharacter c : chars ) {
			if ( id.equals( c.getCharacterId() ) ) {
				return;
			}
		}
		throw new AssertionFailedError("Found no character with ID "+id);
	}
	
	public void testParse1() {
		
		final GetAvailableCharactersParser  parser =
			new GetAvailableCharactersParser( systemClock() );
		
		parser.parse( new Date() , XML1 );
		
		assertEquals( createDate("2009-06-01 13:54:11") , parser.getServerTime() );
		assertEquals( createDate("2009-06-01 14:54:11") , parser.getCachedUntilServerTime() );
		
		final List<IBaseCharacter> result = parser.getResult();
		assertNotNull( result );
		assertEquals( 2 , result.size() );
		assertContainsCharacterWithID( result , new CharacterID("123456") );
		assertContainsCharacterWithID( result , new CharacterID("1335") );
	}
	
	public void testParseWithError() {
		
		final GetAvailableCharactersParser  parser =
			new GetAvailableCharactersParser( systemClock()  );
		
		final Date now = new Date();
		try {
			parser.parse( now , XML_WITH_ERROR );
			fail("Should have failed");
		} catch(APIErrorException e) {
			assertNotNull( e.getError() );
			assertEquals( 106 , e.getError().getErrorCode() );
			assertEquals( "Must provide userID parameter for authentication." , e.getError().getErrorMessage() );
		}
	}
	
}
