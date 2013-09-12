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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.codesourcery.eve.apiclient.IAPIClient.EntityType;

public class ResolveNamesParserTest extends AbstractParserTest {

	private ResolveNamesParser parser;

	public static final String  XML_ONE_CHARACTER = "<eveapi version=\"2\">\n" + 
	"<currentTime>2009-06-18 16:02:30</currentTime>\n" + 
	"<result>\n" + 
	"<rowset name=\"characters\" key=\"characterID\" columns=\"name,characterID\">\n" + 
	"<row name=\"CCP Garthagk\" characterID=\"797400947\"/>\n" + 
	"</rowset>\n" + 
	"</result>\n" + 
	"<cachedUntil>2009-07-18 16:02:30</cachedUntil>\n" + 
	"</eveapi>";

	public static final String  XML_TWO_CHARACTERS = "<eveapi version=\"2\">\n" + 
	"<currentTime>2009-06-18 16:02:30</currentTime>\n" + 
	"<result>\n" + 
	"<rowset name=\"characters\" key=\"characterID\" columns=\"name,characterID\">\n" + 
	"<row name=\"CCP Garthagk\" characterID=\"797400947\"/>\n" +
	"<row name=\"blubb\" characterID=\"42\"/>\n" + 	
	"</rowset>\n" + 
	"</result>\n" + 
	"<cachedUntil>2009-07-18 16:02:30</cachedUntil>\n" + 
	"</eveapi>";	

	public static final String  XML_ONE_CORP = "<eveapi version=\"2\">\n" + 
	"<currentTime>2009-06-18 16:02:30</currentTime>\n" + 
	"<result>\n" + 
	"<rowset name=\"corporations\" key=\"corporationID\" columns=\"name,corporationID\">\n" + 
	"<row name=\"corp 1\" corporationID=\"797400947\"/>\n" + 
	"</rowset>\n" + 
	"</result>\n" + 
	"<cachedUntil>2009-07-18 16:02:30</cachedUntil>\n" + 
	"</eveapi>";
	
	public static final String  XML_TWO_CORPS = "<eveapi version=\"2\">\n" + 
	"<currentTime>2009-06-18 16:02:30</currentTime>\n" + 
	"<result>\n" + 
	"<rowset name=\"corporations\" key=\"corporationID\" columns=\"name,corporationID\">\n" + 
	"<row name=\"corp 1\" corporationID=\"797400947\"/>\n" +
	"<row name=\"corp 2\" corporationID=\"42\"/>\n" +
	"</rowset>\n" + 
	"</result>\n" + 
	"<cachedUntil>2009-07-18 16:02:30</cachedUntil>\n" + 
	"</eveapi>";	

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		parser = null;
	}
	
	private static final <X> Set<X> toSet(X... data) {
		final Set<X> result = new HashSet<X>();
		if ( data == null ) {
			return null;
		}
		for ( X obj : data ) {
			result.add( obj );
		}
		return result;
	}
	
	public void testParserOneCorpID() {
		
		parser = new ResolveNamesParser(new String[] {"797400947" } ,
				toSet( EntityType.CORPORATION ) , systemClock() );

		parser.parse( new Date() , XML_ONE_CORP );

		final Map<String,String> names = parser.getResult();
		assertNotNull( names );
		assertEquals( "corp 1" , names.get( "797400947" ) );
	}	
	
	public void testParserTwoCorpIDs() {
		parser = new ResolveNamesParser( new String[] { "42" , "797400947" } ,
				toSet( EntityType.CORPORATION  ) , systemClock() );

		parser.parse( new Date() , XML_TWO_CORPS );

		final Map<String,String> names = parser.getResult();
		assertNotNull( names );
		assertEquals( "corp 1" , names.get( "797400947" ) );
		assertEquals( "corp 2" , names.get( "42" ) );
	}	

	public void testParserOneCharacterID() {
		parser = new ResolveNamesParser( new String[] {"797400947" } ,
				toSet( EntityType.CHARACTER ) , systemClock()  );

		parser.parse( new Date() , XML_ONE_CHARACTER );

		final Map<String,String> names = parser.getResult();
		assertNotNull( names );
		assertEquals( "CCP Garthagk" , names.get( "797400947" ) );
	}	

	public void testParserTwoCharacterIDs() {
		parser = new ResolveNamesParser(new String[] { "42" , "797400947" } ,
				toSet( EntityType.CHARACTER ) , systemClock() );

		parser.parse( new Date() , XML_TWO_CHARACTERS );

		final Map<String,String> names = parser.getResult();
		assertNotNull( names );
		assertEquals( "CCP Garthagk" , names.get( "797400947" ) );
		assertEquals( "blubb" , names.get( "42" ) );
	}		


	public void testParserWithNoNullIDsFails() {
		try {
			new ResolveNamesParser( null , toSet( EntityType.CHARACTER ) , systemClock() );
			fail("Should have failed");
		} catch(IllegalArgumentException e) {
			// ok
		}
	}

	public void testParserWithEmptyIdsFails() {
		try {
			new ResolveNamesParser(new String[] {} , toSet( EntityType.CHARACTER  ) , systemClock() );
			fail("Should have failed");
		} catch(IllegalArgumentException e) {
			// ok
		}
	}

	public void testParserWithNullId() {
		try {
			new ResolveNamesParser(new String[] {"a" , null }, toSet(EntityType.CHARACTER ) , systemClock() );
			fail("Should have failed");
		} catch(IllegalArgumentException e) {
			// ok
		}
	}	


}
