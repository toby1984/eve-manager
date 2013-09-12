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

import de.codesourcery.eve.apiclient.datamodel.ServerStatus;
import de.codesourcery.eve.apiclient.exceptions.APIErrorException;

public class ServerStatusParserTest extends AbstractParserTest {

	private final String XML1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + 
			"<eveapi version=\"2\">\n" + 
			"  <currentTime>2009-06-01 13:39:37</currentTime>\n" + 
			"  <result>" + 
			"    <serverOpen>True</serverOpen>" + 
			"    <onlinePlayers>31235</onlinePlayers>" + 
			"  </result>" + 
			"  <cachedUntil>2009-06-01 13:42:37</cachedUntil>" + 
			"</eveapi>";
	
	private final String XML2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + 
	"<eveapi version=\"2\">\n" + 
	"  <currentTime>2009-06-01 13:39:37</currentTime>\n" + 
	"  <result>" + 
	"    <serverOpen>False</serverOpen>" + 
	"    <onlinePlayers>12345</onlinePlayers>" + 
	"  </result>" + 
	"  <cachedUntil>2009-06-01 13:42:37</cachedUntil>" + 
	"</eveapi>";	
	
	private final String XML_WITH_ERROR  = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
			"<eveapi version=\"2\">\n" + 
			"  <currentTime>2008-12-16 17:12:07</currentTime>\n" + 
			"  <error code=\"106\">Must provide userID parameter for authentication.</error>\n" + 
			"  <cachedUntil>2008-12-16 17:17:07</cachedUntil>\n" + 
			"</eveapi>\n" + 
			"";
	
	public void testParseWithError() {
		
		final ServerStatusParser parser =
			new ServerStatusParser(systemClock() );
		
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
	
	public void testParse1() {
		
		final ServerStatusParser parser =
			new ServerStatusParser( systemClock() );
		
		final Date now = new Date();
		parser.parse( now , XML1 );
		
		final ServerStatus result = parser.getResult();
		assertNotNull( result );
		assertEquals( 31235 , result.getPlayerCount() );
		assertTrue( result.isServerOpen() );
		
		assertNull( parser.getError() );
		assertEquals( 2 , parser.getAPIVersion() );
		assertEquals( createDate( "2009-06-01 13:39:37" ) , parser.getServerTime() );
		assertEquals( createDate( "2009-06-01 13:42:37" ) , parser.getCachedUntilServerTime() );
	}
	
	public void testParse2() {
		
		final ServerStatusParser parser =
			new ServerStatusParser( systemClock() );
		
		final Date now = new Date();
		parser.parse( now , XML2 );
		
		final ServerStatus result = parser.getResult();
		assertNotNull( result );
		assertEquals( 12345 , result.getPlayerCount() );
		assertFalse( result.isServerOpen() );
		
		assertNull( parser.getError() );
		assertEquals( 2 , parser.getAPIVersion() );
		assertEquals( createDate( "2009-06-01 13:39:37" ) , parser.getServerTime() );
		assertEquals( createDate( "2009-06-01 13:42:37" ) , parser.getCachedUntilServerTime() );		
	}	
}
