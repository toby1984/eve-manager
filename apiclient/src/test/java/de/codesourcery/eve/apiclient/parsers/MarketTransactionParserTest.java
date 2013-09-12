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
import java.util.List;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.MarketTransaction;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.utils.ISKAmount;
import de.codesourcery.eve.skills.utils.MockSystemClock;

public class MarketTransactionParserTest extends AbstractParserTest
{

	private final MockSystemClock CLOCK = new MockSystemClock();
	
	public static final String  XML = "<eveapi version=\"1\">\n" + 
			"  <currentTime>2007-06-18 22:38:52</currentTime>\n" + 
			"  <result>\n" + 
			"    <rowset name=\"transactions\" columns=\"transactionDateTime,transactionID,quantity,typeID,price,clientID,clientName,stationID,stationName,transactionType,transactionFor\">\n" + 
			"      <row transactionDateTime=\"2008-06-15 09:27:00\" transactionID=\"661583821\" quantity=\"1\"\n" + 
			"           typeName=\"Medium Hull Repairer I\" typeID=\"3653\" price=\"100000.00\" clientID=\"1113473668\"\n" + 
			"           clientName=\"Spencer Noffke\" stationID=\"60011749\"\n" + 
			"           stationName=\"Luminaire VII - Moon 6 - Federation Navy Assembly Plant\"\n" + 
			"           transactionType=\"sell\" transactionFor=\"personal\" />\n" + 
			"      <row transactionDateTime=\"2007-12-30 14:46:00\" transactionID=\"514892332\" quantity=\"486\"\n" + 
			"           typeName=\"Gallente Federation Starbase Charter\" typeID=\"24594\" price=\"801.02\" clientID=\"802387143\"\n" + 
			"           clientName=\"Wnsnte\" stationID=\"60011737\"\n" + 
			"           stationName=\"Oursulaert VII - Moon 1 - Federation Navy Testing Facilities\"\n" + 
			"           transactionType=\"buy\" transactionFor=\"corporation\" />\n" + 
			"    </rowset>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2007-06-18 22:36:09</cachedUntil>\n" + 
			"</eveapi>";
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}
	

	public void testParsing() throws Exception {
		
		final InventoryType ITEM1 =
			new InventoryType();
		
		ITEM1.setTypeId( 3653L );
		
		final InventoryType ITEM2 =
			new InventoryType();
		ITEM2.setTypeId( 24594L );
		
		final Station STATION1 =
			new Station();
		STATION1.setID( 60011749L );
		
		final Station STATION2 =
			new Station();
		STATION1.setID( 60011737L );
		
		IStaticDataModel dataModel =
			createMock(IStaticDataModel.class);
		
		expect( dataModel.getInventoryType( new Long(3653) ) ).andReturn( ITEM1 ).once();
		expect( dataModel.getInventoryType( new Long(24594) ) ).andReturn( ITEM2 ).once();
		
		expect( dataModel.getStation( new Long(60011749L) ) ).andReturn( STATION1 ).once();
		expect( dataModel.getStation( new Long(60011737L) ) ).andReturn( STATION2 ).once();
		
		replay( dataModel );
		final WalletTransactionsParser parser =
			new WalletTransactionsParser( dataModel , CLOCK );
		
		parser.parse( new Date() , XML );
		
		final List<MarketTransaction> result = parser.getResult();
		assertNotNull(result);
		assertEquals(2 , result.size() );
		
		MarketTransaction t1 = result.get(0);
		MarketTransaction t2 = result.get(1);
		
		if ( t1.getTransactionId() == 661583821) {
			// ok
		} else if ( t1.getTransactionId() == 661583821) {
			// swap
			final MarketTransaction tmp = t1;
			t1=t2;
			t1=tmp;
		} else {
			fail("Wrong transaction ID ?");
		}
		
		// compare transaction #1
		
		assertEquals( createDate("2008-06-15 09:27:00") , t1.getTransactionDate() );
		assertEquals( 661583821L , t1.getTransactionId() );
		assertEquals( 1 , t1.getQuantity() );
		assertEquals( new ISKAmount(100000.00d ) , t1.getPrice() );
		assertEquals( ""+1113473668L , t1.getClientId().asCharacterId().getValue() );
		assertSame( ITEM1 , t1.getItemType() );
		assertSame( STATION1 , t1.getStation() );
		assertEquals(PriceInfo.Type.SELL , t1.getOrderType() );
		assertFalse( t1.isCorporateTransaction() );
		assertEquals( "Spencer Noffke" , t1.getClientName() );
		
		
		// compare transaction #2
		
		assertEquals( createDate("2007-12-30 14:46:00") , t2.getTransactionDate() );
		assertEquals( 514892332L , t2.getTransactionId() );
		assertEquals( 486 , t2.getQuantity() );
		assertEquals( new ISKAmount(801.02d ) , t2.getPrice() );
		assertEquals( ""+802387143L , t2.getClientId().asCharacterId().getValue() );
		assertSame( ITEM2 , t2.getItemType() );
		assertSame( STATION2 , t2.getStation() );
		assertEquals(PriceInfo.Type.BUY , t2.getOrderType() );
		assertTrue( t2.isCorporateTransaction() );
		assertEquals( "Wnsnte" , t2.getClientName() );
	}
}
