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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;

public class PriceInfoFileTest extends TestHelper {

	private File dataFile;
	
	@Override
	protected void setUp() throws Exception
	{
		
		dataFile =
			new File( createTempDir() , "dummy.csv" );
		
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		if ( dataFile != null ) {
			dataFile.delete();
			dataFile.deleteOnExit();
		}
		
		super.tearDown();
	}
	
	public void testEmptyFile() throws Exception {
		
		assertTrue( ! dataFile.exists() || dataFile.length() == 0 );
		
		final PriceInfoFile file =
			new PriceInfoFile( REGION1 , ITEM1 , dataFile );
		
		List<PriceInfo> entries = file.load( systemClock() );
		assertNotNull( entries );
		assertEquals(0 , entries.size() );
	}
	
	public void testStoreOneEntry() throws Exception {
		
		assertTrue( ! dataFile.exists() || dataFile.length() == 0 );
		
		PriceInfoFile file =
			new PriceInfoFile( REGION1 , ITEM1 , dataFile );
		
		final PriceInfo info = createPriceInfo(PriceInfo.Type.BUY, ITEM1 , REGION1 );
		
		info.setMinPrice( 1 );
		info.setAveragePrice( 2 );
		info.setMaxPrice( 3 );
		info.setOrderCount( 32 );
		info.setOrderId( 12345 );
		info.setRemainingVolume( 47 );
		info.setSource( Source.MARKET_LOG );
		info.setTimestamp( currentDateWithoutMillis() );
		info.setVolume( 99 );
		
		file.save( Arrays.asList( info ) );
		
		assertTrue( dataFile.exists() );
		
		System.out.println("File contains >"+readFile( dataFile )+"<");
		
		file =
			new PriceInfoFile( REGION1 , ITEM1 , dataFile );
		
		List<PriceInfo> entries = file.load( systemClock() );
		assertNotNull( entries );
		assertEquals(1 , entries.size() );
		
		final PriceInfo info2 = entries.get(0);
		
		assertEquals( 1L , info2.getMinPrice() );
		assertEquals( 2L , info2.getAveragePrice() );
		assertEquals( 3L , info2.getMaxPrice() );
		assertEquals( 32L , info2.getOrderCount() );
		assertEquals( 12345L , info2.getOrderId() );
		assertEquals( 47L , info2.getRemainingVolume() );
		assertEquals( Source.MARKET_LOG , info2.getSource() );
		assertEquals( info.getTimestamp() , info2.getTimestamp() );
		assertEquals( 99L , info2.getVolume() );
	}
	
	
	public void testStoreTwoEntries() throws Exception {
		
		assertTrue( ! dataFile.exists() || dataFile.length() == 0 );
		
		PriceInfoFile file =
			new PriceInfoFile( REGION1 , ITEM1 , dataFile );
		
		final PriceInfo info1 = createPriceInfo(PriceInfo.Type.BUY, ITEM1 , REGION1 );
		
		info1.setMinPrice( 1 );
		info1.setAveragePrice( 2 );
		info1.setMaxPrice( 3 );
		info1.setOrderCount( 32 );
		info1.setOrderId( 12345 );
		info1.setRemainingVolume( 47 );
		info1.setSource( Source.MARKET_LOG );
		info1.setTimestamp( currentDateWithoutMillis() );
		info1.setVolume( 99 );
		
		final PriceInfo info2 = createPriceInfo(PriceInfo.Type.BUY, ITEM1 , REGION1 );
		
		info2.setMinPrice( 3 );
		info2.setAveragePrice( 2 );
		info2.setMaxPrice( 1 );
		info2.setOrderCount( 33 );
		info2.setOrderId( 12346 );
		info2.setRemainingVolume( 48 );
		info2.setSource( Source.MARKET_LOG );
		info2.setTimestamp( currentDateWithoutMillis(60) ); // +60 seconds = latest price
		info2.setVolume( 95 );
		
		file.save( Arrays.asList( info1 , info2 ) );
		
		assertTrue( dataFile.exists() );
		
		System.out.println("File contains >"+readFile( dataFile )+"<");
		
		file =
			new PriceInfoFile( REGION1 , ITEM1 , dataFile );
		
		List<PriceInfo> entries = file.load( systemClock()  );
		assertNotNull( entries );
		assertEquals( 2 , entries.size() );
		
		// file entries must be sorted descending by date
		//, latest entry comes first
		final PriceInfo info22 = entries.get(1);
		final PriceInfo info11 = entries.get(0);
		
		assertEquals(PriceInfo.Type.BUY , info11.getPriceType() );
		assertEquals( 1L , info11.getMinPrice() );
		assertEquals( 2L , info11.getAveragePrice() );
		assertEquals( 3L , info11.getMaxPrice() );
		assertEquals( 32L , info11.getOrderCount() );
		assertEquals( 12345L , info11.getOrderId() );
		assertEquals( 47L , info11.getRemainingVolume() );
		assertEquals( Source.MARKET_LOG , info11.getSource() );
		assertEquals( info1.getTimestamp()  , info11.getTimestamp() );
		assertEquals( 99L , info11.getVolume() );
		
		assertEquals(PriceInfo.Type.BUY , info22.getPriceType() );		
		assertEquals( 3L , info22.getMinPrice() );
		assertEquals( 2L , info22.getAveragePrice() );
		assertEquals( 1L , info22.getMaxPrice() );
		assertEquals( 33L , info22.getOrderCount() );
		assertEquals( 12346L , info22.getOrderId() );
		assertEquals( 48L , info22.getRemainingVolume() );
		assertEquals( Source.MARKET_LOG , info22.getSource() );
		assertEquals( info2.getTimestamp() , info22.getTimestamp() );
		assertEquals( 95L , info22.getVolume() );		
	}
	
	
	
}
