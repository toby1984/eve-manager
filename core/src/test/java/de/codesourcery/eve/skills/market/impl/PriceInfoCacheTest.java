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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.market.MarketFilterBuilder;
import de.codesourcery.eve.skills.utils.EveDate;

public class PriceInfoCacheTest extends TestHelper {

	private PriceInfoCache cache;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cache = new PriceInfoCache();
	}
	
	public void testStoreWithoutRegion() {

		PriceInfo info = createPriceInfo(Type.BUY , ITEM1 , null );
	
		try {
			cache.storePriceInfo( info );
			fail("Should have failed");
		} catch(IllegalArgumentException e) {
			// ok
		}
		
	}
	
	protected EveDate parseDate(String s) throws ParseException {
		return EveDate.fromLocalTime( new SimpleDateFormat("yyyy-MM-dd").parse( s ) , systemClock() );
	}
	
	public void testGetLatestBuyPriceInfos() throws ParseException {
		
		PriceInfo info1 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info1.setTimestamp( parseDate("2009-10-13" ) );
		
		PriceInfo info2 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info2.setTimestamp( parseDate("2009-10-11" ) );
		
		PriceInfo info3 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info3.setTimestamp( parseDate("2009-10-12" ) );
		
		cache.storePriceInfo( info1 );
		cache.storePriceInfo( info2 );
		cache.storePriceInfo( info3 );
		
		final Map<Long,List<PriceInfo>> map = 
			cache.getLatestPriceInfos( REGION1 , Type.BUY , Arrays.asList( ITEM1 ) );
		
		assertNotNull( map );
		assertEquals( 1 , map.size() );
		
		List<PriceInfo> latestPrices =
			map.get( ITEM1.getId() );
		
		assertNotNull( latestPrices );
		assertEquals( 1 , latestPrices.size() );
		assertSame( info1 , latestPrices.get( 0 ) );
	}
	
	public void testGetLatestSellPriceInfos() throws ParseException {
		
		// buy prices
		PriceInfo info11 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info11.setTimestamp( parseDate("2009-8-15" ) ); // <<< LATEST
		
		PriceInfo info22 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info22.setTimestamp( parseDate("2009-8-11" ) );
		
		PriceInfo info33 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info33.setTimestamp( parseDate("2009-07-06" ) );
		
		cache.storePriceInfo( info11 );
		cache.storePriceInfo( info22 );
		cache.storePriceInfo( info33 );
		
		// =========== sell prices ==============
		
		PriceInfo info1 = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info1.setTimestamp( parseDate("2009-10-13" ) ); // <<< LATEST
		
		PriceInfo info2 = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info2.setTimestamp( parseDate("2009-10-11" ) );
		
		PriceInfo info3 = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info3.setTimestamp( parseDate("2009-10-12" ) );
		
		cache.storePriceInfo( info1 );
		cache.storePriceInfo( info2 );
		cache.storePriceInfo( info3 );
		
		final Map<Long,List<PriceInfo>> map = 
			cache.getLatestPriceInfos( REGION1 , Type.ANY , Arrays.asList( ITEM1 ) );
		
		assertNotNull( map );
		assertEquals( 1 , map.size() );
		
		List<PriceInfo> latestPrices =
			map.get( ITEM1.getId() );
		
		assertNotNull( latestPrices );
		assertEquals( 2 , latestPrices.size() );
		
		final PriceInfo result1 =
			latestPrices.get(0);
		
		final PriceInfo result2 =
			latestPrices.get(1);
		
		if ( ( result1 == info11 && result2 == info1 ) ||
			 ( result1 == info1 && result2 == info11 ) ) 
		{
			// ok
		} else {
			fail("Unexpected result: "+latestPrices);
		}
	}	
	
	public void testGetLatestBuyAndSellPriceInfos() throws ParseException {
		
		PriceInfo info1 = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info1.setTimestamp( parseDate("2009-10-13" ) );
		
		PriceInfo info2 = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info2.setTimestamp( parseDate("2009-10-11" ) );
		
		PriceInfo info3 = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info3.setTimestamp( parseDate("2009-10-12" ) );
		
		cache.storePriceInfo( info1 );
		cache.storePriceInfo( info2 );
		cache.storePriceInfo( info3 );
		
		final Map<Long,List<PriceInfo>> map = 
			cache.getLatestPriceInfos( REGION1 , Type.SELL , Arrays.asList( ITEM1 ) );
		
		assertNotNull( map );
		assertEquals( 1 , map.size() );
		
		final List<PriceInfo> latestPrices =
			map.get( ITEM1.getId() );
		
		assertNotNull( latestPrices );
		assertEquals( 1 , latestPrices.size() );
		assertSame( info1 , latestPrices.get( 0 ) );
	}		
	
	public void testUpdate() {
		
		PriceInfo info1 = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info1.setTimestamp( eveDateFromLocalTime( new Date( new Date().getTime() - 48*60*60*1000 ) ) );
		
		info1.setAveragePrice( 10000 );
		cache.storePriceInfo( info1 );
		
		PriceInfo info = createPriceInfo(Type.SELL , ITEM1 , REGION1);
		info.setAveragePrice( 12000 );
		info.setTimestamp( eveDateFromLocalTime( new Date( new Date().getTime() - 24*60*60*1000 ) ) );
		
		cache.storePriceInfo( info );
		
		MarketFilter filter = 
			new MarketFilterBuilder(Type.SELL, REGION1).end();
		
		List<PriceInfo> result = cache.getLatestPriceInfos( filter , ITEM1 );
		
		assertNotNull( result );
		assertEquals( 1, result.size() );
		
		assertEquals( Type.SELL , result.get(0).getPriceType() );
		assertSame( ITEM1 , result.get(0).getItemType() );
		assertEquals( 12000 , result.get(0).getAveragePrice() );
		
		info.setAveragePrice( 42000 );
		info.setTimestamp( eveDateFromLocalTime( new Date( new Date().getTime() + 24*60*60*1000 ) ) );
		
		cache.storePriceInfo( info );
		
		result = cache.getLatestPriceInfos( filter , ITEM1 );
		
		assertNotNull( result );
		assertEquals( 1 , result.size() );
		assertEquals( Type.SELL , result.get(0).getPriceType() );
		assertSame( ITEM1 , result.get(0).getItemType() );		
		assertEquals( 42000 , result.get(0).getAveragePrice() );
	}
	
	public void testStore() {
		
		PriceInfo info = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		
		cache.storePriceInfo( info );
		
		MarketFilter filter = 
			new MarketFilterBuilder(Type.BUY , REGION1 ).end();
		
		List<PriceInfo> result = cache.getLatestPriceInfos( filter , ITEM1 );
		
		assertNotNull( result );
		assertEquals(1 , result.size());
		assertEquals( Type.BUY , result.get(0).getPriceType() );
		assertSame( ITEM1 , result.get(0).getItemType() );
	}
	
	public void testClear() {
		
		PriceInfo info = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		
		cache.storePriceInfo( info );
		
		MarketFilter filter = 
			new MarketFilterBuilder(Type.BUY , REGION1 ).end();
		
		List<PriceInfo> result = cache.getLatestPriceInfos( filter , ITEM1 );
		
		assertNotNull( result );
		assertEquals( Type.BUY , result.get(0).getPriceType() );
		assertSame( ITEM1 , result.get(0).getItemType() );
		
		cache.clear();
		assertEquals( null , cache.getLatestPriceInfos( filter , ITEM1 ) );
	}
	
	public void testEvict() {
		
		PriceInfo info = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		
		cache.storePriceInfo( info );
		
		PriceInfo info2 = createPriceInfo(Type.BUY , ITEM2 , REGION1);
		
		cache.storePriceInfo( info2 );
		
		final MarketFilter filter = 
			new MarketFilterBuilder(Type.BUY , REGION1 ).end();
		
		List<PriceInfo> result = cache.getLatestPriceInfos( filter , ITEM1 );
		
		assertNotNull( result );
		assertEquals( 1, result.size() );
		assertEquals( Type.BUY , result.get(0).getPriceType() );
		assertSame( ITEM1 , result.get(0).getItemType() );
		
		List<PriceInfo> result2 = cache.getLatestPriceInfos( filter , ITEM2 );
		
		assertNotNull( result2 );
		assertEquals( 1 , result2.size() );
		assertEquals( Type.BUY , result2.get(0).getPriceType() );
		assertSame( ITEM2 , result2.get(0).getItemType() );		
		
		cache.evict( info );
		
		assertTrue( cache.getLatestPriceInfos( filter , ITEM1 ).isEmpty() );
		assertEquals( 1 , cache.getLatestPriceInfos( filter , ITEM2 ).size() );
		assertSame( result2.get(0) , cache.getLatestPriceInfos( filter , ITEM2 ).get(0) ); 
	}
	
	public void testGetDataForRegion1() {
		
		PriceInfo info = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		
		cache.storePriceInfo( info );
		
		PriceInfo info2 = createPriceInfo(Type.BUY , ITEM2 , REGION1);
		
		cache.storePriceInfo( info2 );

		List<List<PriceInfo>> result = cache.getDataForRegion( REGION1 );

		assertNotNull( result );
		assertEquals( 2 , result.size() );
		
		List<PriceInfo> list1 = result.get(0);
		List<PriceInfo> list2 = result.get(1);
		
		if ( list1.contains( info ) ) {
			assertTrue( list2.contains( info2 ) );
		} else if ( list1.contains( info2 ) ) {
			assertTrue( list2.contains( info ) );
		} else {
			fail("List does not contain expected elements");
		}
	}
	
	public void testGetDataForRegion2() {
		
		PriceInfo info = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		
		cache.storePriceInfo( info );
		
		PriceInfo info2 = createPriceInfo(Type.BUY , ITEM2 , REGION2);
		
		cache.storePriceInfo( info2 );

		List<List<PriceInfo>> result = cache.getDataForRegion( REGION1 );

		assertNotNull( result );
		assertEquals( 1 , result.size() );
		
		assertTrue( result.get(0).contains( info ) );
	}
	
	protected static <T> Collection<T> asList(T... data) {
		Collection<T> result = new ArrayList<T>();
		if ( data != null ) {
			for  ( T obj : data ) {
				result.add( obj );
			}
		}
		return result;
	}
	
	public void testCollectionStore() throws Exception {
		
		PriceInfo info1 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info1.setAveragePrice( 10 );

		info1.setTimestamp( createDate( -1 ) );
		
		PriceInfo info2 = createPriceInfo(Type.BUY , ITEM1 , REGION1);
		info2.setAveragePrice( 11 );
		info2.setTimestamp( createDate(0) );
		
		cache.storePriceInfos( REGION1 , ITEM1 , asList( info1, info2 ) ); 
		
		MarketFilter filter = 
			new MarketFilterBuilder(Type.BUY  , REGION1 )
			.end();
		
		List<PriceInfo> result = cache.getLatestPriceInfos( filter , ITEM1 );
		
		assertNotNull( result );
		assertEquals( 1, result.size() );
		assertEquals( Type.BUY , result.get(0).getPriceType() );
		assertSame( ITEM1 , result.get(0).getItemType() );
		assertEquals( 11 , result.get(0).getAveragePrice() );
		
		Collection<PriceInfo> history =
			cache.getPriceHistory( REGION1 , Type.BUY , ITEM1 );
		
		assertNotNull( history );
		assertEquals( 2 , history.size() );
		assertTrue( history.contains( info1 ) );
		assertTrue( history.contains( info2 ) );
	}
}
