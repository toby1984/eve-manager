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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.skills.datamodel.TransactionType;

public class TransactionTypeParserTest extends AbstractParserTest
{

	public static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
			"<eveapi version=\"2\">\n" + 
			"  <currentTime>2009-05-13 01:55:56</currentTime>\n" + 
			"  <result>\n" + 
			"    <rowset name=\"refTypes\" key=\"refTypeID\" columns=\"refTypeID,refTypeName\">\n" + 
			"      <row refTypeID=\"0\" refTypeName=\"Undefined\"/>\n" + 
			"      <row refTypeID=\"1\" refTypeName=\"Player Trading\"/>\n" + 
			"      <row refTypeID=\"2\" refTypeName=\"Market Transaction\"/>\n" + 
			"      <row refTypeID=\"90\" refTypeName=\"DNA Modification Fee\"/>\n" + 
			"    </rowset>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2009-05-14 01:55:56</cachedUntil>\n" + 
			"</eveapi>";
	
	public void testParsing() {
		
		final TransactionTypeParser parser =
			new TransactionTypeParser( systemClock() );
		
		final Date now = new Date();
		final InternalAPIResponse response = 
			parser.parse( now , XML);
		
		assertEquals( createDate("2009-05-14 01:55:56") , response.getCachedUntilServerTime() );
		assertEquals( createDate("2009-05-13 01:55:56") , response.getServerTime() );
		
		final List<TransactionType> result = parser.getResult();
		assertNotNull( result );
		assertEquals( 4, result.size( ) );
		final Map<Long,TransactionType> map = new HashMap<Long,TransactionType>();
		
		for ( TransactionType t : result ) {
			assertNull( map.put( t.getId() , t ) );
		}
		
		assertNotNull( map.get( 0L ) );
		assertEquals( "Undefined" , map.get( 0L ).getName() );
		
		assertNotNull( map.get( 1L ) );
		assertEquals( "Player Trading" , map.get( 1L ).getName() );
		
		assertNotNull( map.get( 2L ) );
		assertEquals( "Market Transaction" , map.get( 2L ).getName() );
		
		assertNotNull( map.get( 90L ) );
		assertEquals( "DNA Modification Fee" , map.get( 90L ).getName() );
	}
}
