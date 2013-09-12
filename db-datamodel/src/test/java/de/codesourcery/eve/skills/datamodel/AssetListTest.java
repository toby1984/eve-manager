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
package de.codesourcery.eve.skills.datamodel;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import de.codesourcery.eve.skills.datamodel.AssetList.IMergeStrategy;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Station;

public class AssetListTest extends TestCase 
{
	
	private AtomicLong itemId = new AtomicLong(1);

	private static final InventoryType ITEM1;
	private static final InventoryType ITEM2;
	private static final InventoryType ITEM3;
	
	private static final Station LOCATION1;
	private static final Station LOCATION2;
	
	static {
		ITEM1 = new InventoryType();
		ITEM1.setTypeId( new Long(1) );
		ITEM1.setName("Item #1");
		
		ITEM2 = new InventoryType();
		ITEM2.setTypeId( new Long(2) );
		ITEM2.setName("Item #2");
		
		ITEM3 = new InventoryType();
		ITEM3.setTypeId( new Long(3) );
		ITEM3.setName("Item #3");
		
		LOCATION1 = new Station();
		LOCATION1.setID( 1L );
		LOCATION1.setName("Station #1" );
		
		LOCATION2 = new Station();
		LOCATION2.setID( 2L );
		LOCATION2.setName("Station #2" );
		
	}
	
	protected Asset createAsset(InventoryType item) {
		final Asset asset = new Asset(new CharacterID( "id" ) ,
						itemId.incrementAndGet() );
		asset.setType( item );
		return asset;
	}
	
	public void testMergingWorks() {
		
		final Asset asset1 = createAsset(  ITEM1 );
		asset1.setQuantity( 10 );
		asset1.setIsPackaged( false );
		asset1.setFlags( EveFlags.NONE );
		asset1.setLocation( ILocation.UNKNOWN_LOCATION );
		
		final Asset asset2 = createAsset(  ITEM1 );
		asset2.setQuantity( 10 );
		asset2.setIsPackaged( false );
		asset2.setFlags( EveFlags.NONE );
		asset2.setLocation( ILocation.UNKNOWN_LOCATION );

		final Asset merged = asset1.merge( asset2 , 36);
		
		assertEquals( 36L , merged.getItemId() );
		assertSame( ITEM1 , merged.getType() );
		assertSame(ILocation.UNKNOWN_LOCATION , merged.getLocation() );
		assertEquals( 20 , merged.getQuantity() );
		assertNull( merged.getContainer() );
	}
	
	public void testMergingDifferentLocationsWorks() {
		
		final Asset asset1 = createAsset(  ITEM1 );
		asset1.setQuantity( 10 );
		asset1.setIsPackaged( false );
		asset1.setFlags( EveFlags.NONE );
		asset1.setLocation( LOCATION1 );
		
		final Asset asset2 = createAsset(  ITEM1 );
		asset2.setQuantity( 10 );
		asset2.setIsPackaged( false );
		asset2.setFlags( EveFlags.NONE );
		asset2.setLocation( LOCATION2);

		final Asset merged = asset1.merge( asset2 ,1 );
		
		assertSame( ITEM1 , merged.getType() );
		
		assertTrue( merged.hasLocation( LOCATION1 ) );
		assertTrue( merged.hasLocation( LOCATION2 ) );
		
		assertEquals( 20 , merged.getQuantity() );
		assertNull( merged.getContainer() );
	}
	
	public void testMergingDifferentSameLocationsWorks() {
		
		final Asset asset1 = createAsset(  ITEM1 );
		asset1.setQuantity( 10 );
		asset1.setIsPackaged( false );
		asset1.setFlags( EveFlags.NONE );
		asset1.setLocation( LOCATION1 );
		
		final Asset asset2 = createAsset(  ITEM1 );
		asset2.setQuantity( 10 );
		asset2.setIsPackaged( false );
		asset2.setFlags( EveFlags.NONE );
		asset2.setLocation( LOCATION1);

		final Asset merged = asset1.merge( asset2 ,1 );
		
		assertSame( ITEM1 , merged.getType() );
		
		assertTrue( merged.hasLocation( LOCATION1 ) );
		assertFalse( merged.hasLocation( LOCATION2 ) );
		
		assertEquals( 20 , merged.getQuantity() );
		assertNull( merged.getContainer() );
	}
	
	public void testMergingDifferentItemsFails() {
		
		final Asset asset1 = createAsset(  ITEM1 );
		asset1.setQuantity( 10 );
		asset1.setIsPackaged( false );
		asset1.setFlags( EveFlags.NONE );
		asset1.setLocation( ILocation.UNKNOWN_LOCATION );
		
		final Asset asset2 = createAsset(  ITEM2 );
		asset2.setQuantity( 10 );
		asset2.setIsPackaged( false );
		asset2.setFlags( EveFlags.NONE );
		asset2.setLocation( ILocation.UNKNOWN_LOCATION );

		try {
			asset1.merge( asset2 , 1);
			fail("Merging different item types should not be possible....");
		} catch(IllegalArgumentException e) {
			// ok
		}
	}
	
	public void testMergeThreeItems() {
		
		AssetList list = new AssetList();
		
		final Asset asset1 = createAsset(  ITEM1 );
		asset1.setQuantity( 10 );
		asset1.setIsPackaged( true );
		asset1.setFlags( EveFlags.NONE );
		asset1.setLocation( LOCATION1 );
		
		final Asset asset2 = createAsset(  ITEM1 );
		asset2.setQuantity( 10 );
		asset2.setIsPackaged( true );
		asset2.setFlags( EveFlags.NONE );
		asset2.setLocation( LOCATION1 );
		
		final Asset asset3 = createAsset(  ITEM1 );
		asset3.setQuantity( 10 );
		asset3.setIsPackaged( true );
		asset3.setFlags( EveFlags.NONE );
		asset3.setLocation( LOCATION1 );
		
		list.addAll( Arrays.asList( asset1,asset2,asset3  ) );
		
		final IMergeStrategy mockStrategy =
			EasyMock.createMock( IMergeStrategy.class );
		
		EasyMock.expect( mockStrategy.mayBeMerged( EasyMock.isA(Asset.class) , EasyMock.isA(Asset.class) ) ).andReturn( true ).atLeastOnce();
		
		EasyMock.replay( mockStrategy );
		
		AssetList merged = list.createMergedAssetListByType( mockStrategy );
		
		assertNotNull( merged );
		assertEquals( 1 , merged.size() );
		final Asset mergedAsset = merged.getAssets(true).get(0);
		assertSame(ITEM1 , mergedAsset.getType() );
		assertEquals( 30 , mergedAsset.getQuantity() );
		assertTrue( mergedAsset.isPackaged() );
		assertTrue( mergedAsset.hasLocation( LOCATION1 ) );
		
		EasyMock.verify( mockStrategy );
	}
}
