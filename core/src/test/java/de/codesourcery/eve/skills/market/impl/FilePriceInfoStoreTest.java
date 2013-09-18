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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.easymock.classextension.EasyMock;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.market.MarketFilterBuilder;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;
import de.codesourcery.eve.skills.utils.MockSystemClock;

public class FilePriceInfoStoreTest extends TestHelper {

	private File tmpDir;
	private ISystemClock clock = new MockSystemClock();
	
	@Override
	protected void setUp() throws Exception {
		
		super.setUp();
		
		tmpDir =
			createTempDir();
	}

	public void testStore() throws Exception {
		
		final EveDate date = parseDate("2009-10-09 00:00:00" );
		
		PriceInfo info =
			createPriceInfo(Type.BUY , ITEM1 , REGION1 );
		
		info.setMinPrice( 1 );
		info.setAveragePrice( 2 );
		info.setMaxPrice( 3 );
		info.setTimestamp( date );
		
		FilePriceInfoStore store = new FilePriceInfoStore( clock  );
		store.setBaseDir( tmpDir );
		
		store.save( info );
		store.persist();
		
		store = new FilePriceInfoStore( clock );
		store.setBaseDir( tmpDir );
		
		final MarketFilter filter =
			new MarketFilterBuilder( Type.BUY , REGION1 ).end();
		
		List<PriceInfo> result = store.get( filter , ITEM1 );
		assertNotNull( result );
		assertEquals(1 , result.size() );
		
		PriceInfo loaded = result.get(0);
		
		assertSame( loaded.getRegion() , info.getRegion() );
		assertEquals( date , loaded.getTimestamp() );
		assertEquals( info.getMinPrice() , loaded.getMinPrice() );
		assertEquals( info.getMaxPrice() , loaded.getMaxPrice() );
		assertEquals( info.getAveragePrice() , loaded.getAveragePrice() );
		assertSame( info.getItemType() , loaded.getItemType() );
	}
	
	public void testUpdateExistingEntry() throws Exception {
		
		final EveDate date = parseDate("2009-10-09 00:00:00" );
		
		PriceInfo info =
			createPriceInfo(Type.BUY , ITEM3 , REGION1 );
		
		info.setMinPrice( 1 );
		info.setAveragePrice( 2 );
		info.setMaxPrice( 3 );
		info.setTimestamp( date );
		
		FilePriceInfoStore store = new FilePriceInfoStore( clock  );
		store.setBaseDir( tmpDir );
		
		store.save( info );
		
		info.setAveragePrice( 5 );
		store.save( info );
		
		info.setAveragePrice( 7 );
		store.save( info );
		
		store.persist();
		
		File outputFile = store.getFileForItem( REGION1 , ITEM3 ).getFile();
		
		final List<String> lines = new ArrayList<String>();
		BufferedReader reader =
			new BufferedReader(new FileReader( outputFile ) );
		
		try {
			String line;
		while ( ( line = reader.readLine() ) != null ) {
			lines.add( line );
		}
		} finally {
			reader.close();
		}
		
		assertEquals( "Expected exactly one line , got : "+lines ,1 , lines.size() );
		
		store = new FilePriceInfoStore( clock  );
		store.setBaseDir( tmpDir );
		
		final MarketFilter filter =
			new MarketFilterBuilder( Type.BUY , REGION1 ).end();
		
		List<PriceInfo> result = store.get( filter , ITEM3 );
		
		assertNotNull( result );
		assertEquals( 1 ,result.size() );
		
		PriceInfo loaded = result.get(0);
		
		assertSame( loaded.getRegion() , info.getRegion() );
		assertEquals( date , loaded.getTimestamp() );
		assertEquals( info.getMinPrice() , loaded.getMinPrice() );
		assertEquals( info.getMaxPrice() , loaded.getMaxPrice() );
		assertEquals( 7  , loaded.getAveragePrice() );
		assertSame( info.getItemType() , loaded.getItemType() );
	}
	
	public void testSupersedeEntry() throws Exception {
		
		final EveDate date = parseDate("2009-10-09 00:00:00" );
		
		PriceInfo info =
			createPriceInfo(Type.BUY , ITEM3 , REGION1 );
		
		info.setMinPrice( 1 );
		info.setAveragePrice( 2 );
		info.setMaxPrice( 3 );
		info.setTimestamp( date );
		
		FilePriceInfoStore store = new FilePriceInfoStore( clock  );
		store.setBaseDir( tmpDir );
		
		store.save( info );

		PriceInfo info2 =
			createPriceInfo(Type.BUY , ITEM3 , REGION1 );
		
		final EveDate date2 = parseDate("2009-10-09 00:10:01" );
		
		info2.setMinPrice( 1 );
		info2.setAveragePrice( 7 );
		info2.setMaxPrice( 3 );
		info2.setTimestamp( date2 );		
		
		store.save( info2 );
		
		store.persist();
		
		File outputFile = store.getFileForItem( REGION1 , ITEM3 ).getFile();
		
		final List<String> lines = new ArrayList<String>();
		BufferedReader reader =
			new BufferedReader(new FileReader( outputFile ) );
		
		try {
			String line;
		while ( ( line = reader.readLine() ) != null ) {
			lines.add( line );
		}
		} finally {
			reader.close();
		}
		
		assertEquals( "Expected exactly one line , got : "+lines ,1 , lines.size() );
		
		store = new FilePriceInfoStore( clock  );
		store.setBaseDir( tmpDir );
		
		final MarketFilter filter =
			new MarketFilterBuilder( Type.BUY , REGION1 ).end();
		
		List<PriceInfo> result = store.get( filter , ITEM3 );
		
		assertNotNull( result );
		assertEquals( 1 , result.size() );
		
		PriceInfo loaded = result.get(0);
		
		assertSame( loaded.getRegion() , info.getRegion() );
		assertEquals( date2 , loaded.getTimestamp() );
		assertEquals( info.getMinPrice() , loaded.getMinPrice() );
		assertEquals( info.getMaxPrice() , loaded.getMaxPrice() );
		assertEquals( 7  , loaded.getAveragePrice() );
		assertSame( info.getItemType() , loaded.getItemType() );
	}
	
	protected EveDate parseDate(String s) throws ParseException {
		DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return EveDate.fromLocalTime( DF.parse( s ) , clock );
	}
	
	public void testUpdate() throws Exception {
		
		final EveDate date = parseDate("2009-10-09 00:00:00" );
		
		PriceInfo info =
			createPriceInfo(Type.BUY , ITEM1 , REGION1 );
		
		info.setMinPrice( 1 );
		info.setAveragePrice( 2 );
		info.setMaxPrice( 3 );
		info.setTimestamp( date );
		
		final IMarketDataProvider provider= EasyMock.createStrictMock( IMarketDataProvider.class );
		
		EasyMock.replay( provider );
		
		FilePriceInfoStore store = new FilePriceInfoStore( clock  );
		store.setBaseDir( tmpDir );
		
		store.save( info );
		
		EasyMock.verify( provider );
		
		store.persist();
		
		store = new FilePriceInfoStore( clock );
		store.setBaseDir( tmpDir );
		
		final MarketFilter filter =
			new MarketFilterBuilder( Type.BUY , REGION1 ).end();
		
		List<PriceInfo> result = store.get( filter , ITEM1 );
		
		assertNotNull( result );
		assertEquals(  1 , result.size() );
		
		PriceInfo loaded = result.get(0);
		
		assertSame( loaded.getRegion() , info.getRegion() );
		assertEquals( date , loaded.getTimestamp() );
		assertEquals( info.getMinPrice() , loaded.getMinPrice() );
		assertEquals( info.getMaxPrice() , loaded.getMaxPrice() );
		assertEquals( info.getAveragePrice() , loaded.getAveragePrice() );
		assertSame( info.getItemType() , loaded.getItemType() );
		
		final EveDate date2 = parseDate("2009-10-10 00:00:00" );
		
		loaded.setTimestamp( date2 );
		loaded.setMinPrice(4);
		loaded.setAveragePrice(5);
		loaded.setMaxPrice(6);
		
		EasyMock.reset( provider );
		
		EasyMock.replay( provider );
		
		store.save( loaded );
		
		EasyMock.verify( provider );
		
		store.persist();
		
		store = new FilePriceInfoStore( clock );
		store.setBaseDir( tmpDir );
		
		result = store.get( filter , ITEM1 );
		loaded = result.get(0);
		
		assertSame( loaded.getRegion() , info.getRegion() );
		assertEquals( date2 , loaded.getTimestamp() );
		assertEquals( 4 , loaded.getMinPrice() );
		assertEquals( 6 , loaded.getMaxPrice() );
		assertEquals( 5 , loaded.getAveragePrice() );
		assertSame( info.getItemType() , loaded.getItemType() );		
		
	}	
}
