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

import java.util.Date;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;

public class SortedPriceListTest extends TestHelper {

	private SortedPriceList prices;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	
		prices = new SortedPriceList();
	}
	
	public void testNewIsEmpty() {
		assertTrue( prices.isEmpty() );
	}
	
	public void testAddOneBuyPrice() {
		
		final PriceInfo priceInfo = 
			createPriceInfo( Type.BUY , ITEM1 , REGION1 );
		
		prices.store( priceInfo );
		
		assertFalse( prices.isEmpty() );
		
		assertSame( priceInfo , prices.getLatestPrice( Type.BUY ) );
		assertNull( prices.getLatestPrice( Type.SELL) );
		
		assertEquals( 1 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo ) );
	}
	
	public void testAddOneSellPrice() {
		
		final PriceInfo priceInfo = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		prices.store( priceInfo );
		
		assertFalse( prices.isEmpty() );
		
		assertSame( priceInfo , prices.getLatestPrice( Type.SELL ) );
		assertNull( prices.getLatestPrice( Type.BUY) );
		
		assertEquals( 1 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo ) );
	}
	
	public void testCannotCreatePriceInfoWithTypeAny() {
		
		try {
			createPriceInfo( Type.ANY, ITEM1 , REGION1 );
			fail("Should have failed");
		} catch(IllegalArgumentException ok) {
			// expected
		}
	}
	
	public void testAddTwoBuyPricesFromDifferentDays() {
		
		final PriceInfo priceInfo1 = 
			createPriceInfo( Type.BUY , ITEM1 , REGION1 );
		
		priceInfo1.setTimestamp( createDate( 0 ) );
		
		final PriceInfo priceInfo2 = 
			createPriceInfo( Type.BUY , ITEM1 , REGION1 );
		
		priceInfo2.setTimestamp( createDate( 1 ) );
		
		prices.store( priceInfo1 );
		prices.store( priceInfo2 );
		
		assertSame( priceInfo2 , prices.getLatestPrice( Type.BUY ) );
		assertNull( prices.getLatestPrice( Type.SELL) );
		
		assertEquals( 2 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo1 ) );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );
	}
	
	public void testAddTwoSellPricesFromDifferentDays() {
		
		final PriceInfo priceInfo1 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo1.setTimestamp( createDate( 0 ) );
		
		final PriceInfo priceInfo2 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo2.setTimestamp( createDate( 1 ) );
		
		prices.store( priceInfo1 );
		prices.store( priceInfo2 );
		
		assertSame( priceInfo2 , prices.getLatestPrice( Type.SELL ) );
		assertNull( prices.getLatestPrice( Type.BUY) );
		
		assertEquals( 2 , prices.getAllPrices().size() );
		
		assertTrue( prices.getAllPrices().contains( priceInfo1 ) );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );		
	}	
	
	public void testAddTwoBuyPricesFromSameDay() {
		
		final PriceInfo priceInfo1 = 
			createPriceInfo( Type.BUY , ITEM1 , REGION1 );
		
		priceInfo1.setTimestamp( createDate( 0 ) );
		
		final PriceInfo priceInfo2 = 
			createPriceInfo( Type.BUY , ITEM1 , REGION1 );
		
		priceInfo2.setTimestamp( eveDateFromLocalTime( new Date( System.currentTimeMillis()+ 5000 ) ) );
		
		prices.store( priceInfo1 );
		prices.store( priceInfo2 );
		
		assertSame( priceInfo2 , prices.getLatestPrice( Type.BUY ) );
		assertNull( prices.getLatestPrice( Type.SELL) );
		
		assertEquals( 1 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );
	}
	
	public void testAddTwoBuyPricesFromSameDay2() {
		
		final PriceInfo priceInfo1 = 
			createPriceInfo( Type.BUY , ITEM1 , REGION1 );
		
		priceInfo1.setTimestamp( createDate( 0 ) );
		
		final PriceInfo priceInfo2 = 
			createPriceInfo( Type.BUY , ITEM1 , REGION1 );
		
		priceInfo2.setTimestamp( eveDateFromLocalTime( new Date( System.currentTimeMillis()+ 5000 ) ) );
		
		prices.store( priceInfo2 ); // inverted order compared to testAddTwoBuyPricesFromSameDay()
		prices.store( priceInfo1 );
		
		assertSame( priceInfo2 , prices.getLatestPrice( Type.BUY ) );
		assertNull( prices.getLatestPrice( Type.SELL) );
		
		assertEquals( 1 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );
	}
	
	public void testAddTwoSellPricesFromSameDay() {
		
		final PriceInfo priceInfo1 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo1.setTimestamp( createDate( 0 ) );
		
		final PriceInfo priceInfo2 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo2.setTimestamp( eveDateFromLocalTime( new Date( System.currentTimeMillis()+ 5000 ) ) );
		
		prices.store( priceInfo1 );
		prices.store( priceInfo2 );
		
		assertSame( priceInfo2 , prices.getLatestPrice( Type.SELL ) );
		assertNull( prices.getLatestPrice( Type.BUY) );
		
		assertEquals( 1 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );
	}
	
	public void testAddTwoSellPricesFromSameDay2() {
		
		final PriceInfo priceInfo1 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo1.setTimestamp( createDate( 0 ) );
		
		final PriceInfo priceInfo2 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo2.setTimestamp( eveDateFromLocalTime( new Date( System.currentTimeMillis()+ 5000 ) ) );
		
		prices.store( priceInfo2 ); // inverted order compared to testAddTwoSellPricesFromSameDay()
		prices.store( priceInfo1 );
		
		assertSame( priceInfo2 , prices.getLatestPrice( Type.SELL ) );
		assertNull( prices.getLatestPrice( Type.BUY) );
		
		assertEquals( 1 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );
	}
	
	public void testAddRemoveEntry() {
		
		final PriceInfo priceInfo1 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo1.setTimestamp( createDate( 0 ) );
		
		final PriceInfo priceInfo2 = 
			createPriceInfo( Type.SELL , ITEM1 , REGION1 );
		
		priceInfo2.setTimestamp( createDate(1) );
		
		prices.store( priceInfo1 );
		prices.store( priceInfo2 );
		
		assertSame( priceInfo2 , prices.getLatestPrice( Type.SELL ) );
		assertNull( prices.getLatestPrice( Type.BUY) );
		
		assertEquals( 2 , prices.getAllPrices().size() );
		assertTrue( prices.getAllPrices().contains( priceInfo1 ) );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );
		
		prices.remove( priceInfo1 );
		
		assertEquals( 1 , prices.getAllPrices().size() );
		assertFalse( prices.getAllPrices().contains( priceInfo1 ) );
		assertTrue( prices.getAllPrices().contains( priceInfo2 ) );
		
		prices.remove( priceInfo2 );
		
		assertEquals( 0 , prices.getAllPrices().size() );
		assertTrue( prices.isEmpty() );
	}
	
}
