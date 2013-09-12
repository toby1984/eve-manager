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
package de.codesourcery.eve.skills.production.impl;

import static org.easymock.EasyMock.*;
import static org.easymock.classextension.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.accountdata.IUserAccountChangeListener;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.assets.IAssetManager;
import de.codesourcery.eve.skills.assets.IAssetManager.IAssetChangeListener;
import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Requirements;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.BlueprintType;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.production.BlueprintWithAttributes;

public class FileBlueprintLibraryTest extends TestCase 
{
	private FileBlueprintLibrary library;
	
	private File tmpFile;
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		if ( tmpFile != null ) {
//			tmpFile.delete();
			tmpFile = null;
		}
	}
	
	public void testStoreLoad() throws IOException {

		final InventoryType invType = new InventoryType();
		invType.setTypeId( 42L );
		
		final BlueprintType type =
			new BlueprintType() {
			@Override
			public InventoryType getBlueprintType()
			{
				return invType;
			}
		};
		
		final Blueprint blueprint = new Blueprint( type ) {

			@Override
			protected Map<Activity, Requirements> fetchRequirements()
			{
				return Collections.emptyMap();
			}
		} ;
		
		final IStaticDataModel dataModel =
			createMock( IStaticDataModel.class );
		
		expect( dataModel.getInventoryType( 42L ) ).andReturn( invType ).anyTimes();
		
		expect( dataModel.getBlueprint( invType ) ).andReturn( blueprint ).anyTimes();
		
		replay( dataModel );
		
		final ICharacter character = createMock( ICharacter.class );
		
		final CharacterID charId = new CharacterID("testchar");
		
		expect( character.getCharacterId() ).andReturn( charId ).anyTimes();
		
		replay( character );
		
		final IAssetManager assetManager = createMock( IAssetManager.class );
		
		expect( assetManager.getAssets( character ) ).andReturn( new AssetList() ).atLeastOnce();
		
		assetManager.addAssetChangeListener( isA( IAssetChangeListener.class ) );
		expectLastCall().anyTimes();
		
		assetManager.removeAssetChangeListener( isA( IAssetChangeListener.class ) );
		expectLastCall().anyTimes();
		
		replay( assetManager );
		
		final IUserAccountStore accountStore = createMock( IUserAccountStore.class );
		
		accountStore.addChangeLister( isA( IUserAccountChangeListener.class ) );
		expectLastCall().anyTimes();
		
		accountStore.removeChangeLister( isA( IUserAccountChangeListener.class ) );
		expectLastCall().anyTimes();
		
		replay( accountStore );
		
		tmpFile = File.createTempFile("tmp","blueprints");
		
		library = new FileBlueprintLibrary( dataModel , assetManager , accountStore );
		library.setInputFile( tmpFile );
		
		library.addBlueprint( character,blueprint,1,2);
		
		assertTrue( library.ownsBlueprint( character , blueprint ) );
		
		final List<? extends BlueprintWithAttributes> existing = library.getBlueprints( character );
		assertNotNull( existing );
		assertEquals(1 , existing.size() );
		
		final BlueprintWithAttributes bp = existing.get(0);
		
		assertEquals( 1 , bp.getMeLevel() );
		assertEquals( 2 , bp.getPeLevel() );
		assertEquals( charId , bp.getOwningCharacterId() );
		assertEquals( invType.getTypeId() , bp.getBlueprintTypeId() );
		
		library.persist();
		
		/*
		 * Re-read 
		 */
		library = new FileBlueprintLibrary( dataModel , assetManager , accountStore );
		library.setInputFile( tmpFile );
		
		assertTrue( library.ownsBlueprint( character , blueprint ) );
		
		final List<? extends BlueprintWithAttributes> existing2 = library.getBlueprints( character );
		assertNotNull( existing2 );
		assertEquals(1 , existing2.size() );
		
		final BlueprintWithAttributes bp2 = existing2.get(0);
		
		assertEquals( 1 , bp2.getMeLevel() );
		assertEquals( 2 , bp2.getPeLevel() );
		assertEquals( charId , bp2.getOwningCharacterId() );
		assertEquals( invType.getTypeId() , bp2.getBlueprintTypeId() );		
	}
}
