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
package de.codesourcery.eve.skills.market.impl;

import java.util.Arrays;
import java.util.List;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.market.MarketLogEntry;
import de.codesourcery.eve.skills.utils.EveDate;

public class MarketLogFileTest extends TestHelper 
{

	private MarketLogFile file;
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		
	}
	
	public void testTwoEntries() {

		final MarketLogEntry entry1 = new MarketLogEntry();
		entry1.setBuyOrder( true );
		entry1.setIssueDate( currentDateWithoutMillis() );
		entry1.setMinVolume( 1 );
		entry1.setOrderId( 1 );
		entry1.setPrice(2);
		entry1.setRemainingVolume( 1000 );
		entry1.setVolume( 2000 );
		
		final MarketLogEntry entry2 = new MarketLogEntry();
		entry2.setBuyOrder( true );
		entry2.setIssueDate( currentDateWithoutMillis( 100 * 1000 ) );
		entry2.setMinVolume( 1 );
		entry2.setOrderId( 2 );
		entry2.setPrice(3);
		entry2.setRemainingVolume( 1000 );
		entry2.setVolume( 4000 );
		
		final List<MarketLogEntry> input = Arrays.asList( entry1 , entry2 );
		file = new MarketLogFile( input , ITEM1, REGION1 );
		
		assertFalse( file.getOrders() == input );
		assertEquals( 2 , file.getOrders().size() );
		
		assertSame( entry1 , input.get(0) );
		assertSame( entry2 , input.get(1) );
	}
	
	public void testMergeTwoEntries() {

		final EveDate now = currentDateWithoutMillis();
		
		final MarketLogEntry entry1 = new MarketLogEntry();
		entry1.setBuyOrder( true );
		
		entry1.setIssueDate( now );
		entry1.setMinVolume( 1 );
		entry1.setOrderId( 1 );
		entry1.setPrice(100);
		entry1.setRemainingVolume( 1000 );
		entry1.setVolume( 2000 );
		
		final MarketLogEntry entry2 = new MarketLogEntry();
		entry2.setBuyOrder( true );
		entry2.setIssueDate( currentDateWithoutMillis( 100 ) );
		entry2.setMinVolume( 1 );
		entry2.setOrderId( 2 );
		entry2.setPrice(200);
		entry2.setRemainingVolume( 1000 );
		entry2.setVolume( 4000 );
		
		final List<MarketLogEntry> input = Arrays.asList( entry1 , entry2 );
		file = new MarketLogFile( input , ITEM1, REGION1 );
		
		assertFalse( file.getOrders() == input );
		assertEquals( 2 , file.getOrders().size() );
		
		assertSame( entry1 , input.get(0) );
		assertSame( entry2 , input.get(1) );
		
		final List<PriceInfo> aggregatedOrders = file.getAggregatedOrders( systemClock() );
		
		assertEquals( 1 , aggregatedOrders.size() );
		
		final PriceInfo info = aggregatedOrders.get(0);
		assertEquals( 2 , info.getOrderCount() );
		assertEquals( 4000 + 2000 , info.getVolume() );
		assertEquals( 1000 + 1000 , info.getRemainingVolume() );
		assertEquals( 100 * ( (100+200) / 2 ) , info.getAveragePrice() );
		assertEquals( 100 * 100 , info.getMinPrice() );
		assertEquals( 200 * 100 , info.getMaxPrice() );
		
	}
}
