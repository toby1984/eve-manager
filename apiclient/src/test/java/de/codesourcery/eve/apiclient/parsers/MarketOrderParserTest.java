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

import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.MarketOrder;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.MarketOrder.OrderState;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Station;

public class MarketOrderParserTest extends AbstractParserTest
{

	public static final String XML = "<eveapi version=\"2\">\n" + 
			"<currentTime>2008-02-04 13:28:18</currentTime>\n" + 
			"<result>\n" + 
			"<rowset name=\"orders\" key=\"orderID\" columns=\"orderID,charID,stationID," + 
			"volEntered,volRemaining,minVolume,orderState,typeID,range,accountKey,duration,escrow,price,bid,issued\">\n" + 
			"<row orderID=\"639477821\" charID=\"118406849\" stationID=\"60004357\"" +
			"         volEntered=\"25\" volRemaining=\"24\" minVolume=\"1\" " +
			"         orderState=\"0\" typeID=\"26082\" range=\"32767\" accountKey=\"1000\" " +
			"         duration=\"3\" escrow=\"5.00\" price=\"3200000.00\" bid=\"0\" " +
			"         issued=\"2008-02-02 16:39:25\"/>\n" +
			
			"<row orderID=\"639587440\" charID=\"118406849\" stationID=\"60003760\" " +
			"      volEntered=\"25\" volRemaining=\"4\" minVolume=\"1\" orderState=\"0\" " +
			"      typeID=\"26082\" range=\"32767\" accountKey=\"1000\" duration=\"1\" " +
			"      escrow=\"2.00\" price=\"3399999.98\" bid=\"1\" issued=\"2008-02-03 22:35:54\"/>\n" + 
			"</rowset>\n" + 
			"</result>\n" + 
			"<cachedUntil>2008-02-04 14:28:18</cachedUntil>\n" + 
			"</eveapi>";
	
	
	public void testParsing() {
		
		final Station STATION1 = new Station();
		final Station STATION2 = new Station();
		final InventoryType ITEM1 = new InventoryType();
		
		final IStaticDataModel dataModel =
			createMock( IStaticDataModel.class );
		
		expect( dataModel.getStation( new Long("60004357" ) ) ).andReturn( STATION1 ).once();
		expect( dataModel.getStation( new Long("60003760" ) ) ).andReturn( STATION2 ).once();
		
		expect( dataModel.getInventoryType( new Long("26082") ) ).andReturn( ITEM1 ).times(2);
		
		replay( dataModel );
		
		final MarketOrderParser parser =
			new MarketOrderParser( dataModel , systemClock() );
		
		parser.parse( new Date() , XML );
		
		final List<MarketOrder> orders =
			parser.getResult();
		
		assertNotNull( orders );
		assertEquals( 2 , orders.size() );
		
		MarketOrder order1 =
			orders.get(0);
		
		MarketOrder order2 =
			orders.get(1);
		
		if ( order1.getOrderID() == 639477821L ) {
			// ok
		} else if ( order2.getOrderID() == 639477821L ) {
			MarketOrder tmp = order1;
			order1 = order2;
			order2 = tmp;
		} else {
			fail("Unable to locate expected orders ?");
		}
		
		/*
"<row orderID=\"639477821\" charID=\"118406849\" 
stationID=\"60004357\" volEntered=\"25\" volRemaining=\"24\" 
minVolume=\"1\" orderState=\"0\" typeID=\"26082\" 
range=\"32767\" accountKey=\"1000\" duration=\"3\" 
escrow=\"5.00\" price=\"3200000.00\" bid=\"0\" issued=\"2008-02-02 16:39:25\"/>\n" + 
		 
		 */
		assertEquals( 639477821L , order1.getOrderID() );
		assertEquals( new CharacterID("118406849") , order1.getCharacterID() );
		assertSame( STATION1 , order1.getStation() );
		assertEquals( 25L , order1.getVolumeEntered() );
		assertEquals( 24L , order1.getVolumeRemaining() );
		assertEquals( 1 , order1.getMinVolume() );
		assertEquals( OrderState.OPEN , order1.getState() );
		assertEquals( ITEM1 , order1.getItemType() );
		assertEquals( 32767 , order1.getRange() );
		assertEquals( 1000 , order1.getAccountKey() );
		assertEquals( 3 , order1.getDurationInDays() );
		assertEquals( 500 , order1.getMoneyInEscrow() );
		assertEquals( 320000000L , order1.getPrice() );
		assertEquals( PriceInfo.Type.SELL , order1.getType() );
		assertEquals( createDate("2008-02-02 16:39:25" ) , order1.getIssueDate() );
		
/*
"<row orderID=\"639587440\" charID=\"118406849\" 
stationID=\"60003760\" volEntered=\"25\" volRemaining=\"4\" 
minVolume=\"1\" orderState=\"0\" typeID=\"26082\" 
range=\"32767\" accountKey=\"1000\" duration=\"1\" 
escrow=\"2.00\" price=\"3399999.98\" bid=\"1\" issued=\"2008-02-03 22:35:54\"/>\n" + 
 */
		
		assertEquals( 639587440L , order2.getOrderID() );
		assertEquals( new CharacterID("118406849") , order2.getCharacterID() );
		assertSame( STATION2 , order2.getStation() );
		assertEquals( 25L , order2.getVolumeEntered() );
		assertEquals( 4L , order2.getVolumeRemaining() );
		assertEquals( 1 , order2.getMinVolume() );
		assertEquals( OrderState.OPEN , order2.getState() );
		assertEquals( ITEM1 , order2.getItemType() );
		assertEquals( 32767 , order2.getRange() );
		assertEquals( 1000 , order2.getAccountKey() );
		assertEquals( 1 , order2.getDurationInDays() );
		assertEquals( 200 , order2.getMoneyInEscrow() );
		assertEquals( 339999998L , order2.getPrice() );
		assertEquals( PriceInfo.Type.BUY , order2.getType() );
		assertEquals( createDate("2008-02-03 22:35:54" ) , order2.getIssueDate() );
		
	}
}
