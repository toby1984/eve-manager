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
package de.codesourcery.eve.skills.production;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.assets.IAssetManager;
import de.codesourcery.eve.skills.assets.IAssetManager.IAssetChangeListener;
import de.codesourcery.eve.skills.datamodel.Asset;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.IResource;
import de.codesourcery.planning.IResourceFactory;
import de.codesourcery.planning.IResourceManager;
import de.codesourcery.planning.IResourceType;
import de.codesourcery.planning.impl.SimpleResource;
import de.codesourcery.planning.impl.SimpleResourceManager;

public class ResourceManagerFactory
{

	private static final Logger log = Logger
			.getLogger(ResourceManagerFactory.class);
	
	private final IAssetManager assetManager;
	
	// guarded-by: managers
	private final Map<CharacterID,IResourceManager> managers =
		new HashMap<CharacterID,IResourceManager> ();
	
	public ResourceManagerFactory(IAssetManager assetManager) {
		if ( assetManager == null ) {
			throw new IllegalArgumentException("assetManager cannot be NULL");
		} 
		this.assetManager = assetManager;
	}
	
	public IResourceManager getResourceManager(ICharacter character) {
	
		if ( character == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		
		synchronized( managers ) 
		{
			IResourceManager result = managers.get( character.getCharacterId() );
			if ( result == null ) {
				result = new MyResourceManager( character );
				managers.put( character.getCharacterId() , result );
			}
			return result;
		}
	}

	private final class MyResourceManager extends SimpleResourceManager implements IAssetChangeListener 
	{
		private volatile Date timestamp=new Date();
		
		private final ICharacter character;
		
		private final IResourceFactory resourceFactory =
			new IResourceFactory() {

				@Override
				public IResource cloneResource(IResource r)
				{
					return new SimpleResource( r.getType() , r.getLocation() , r.getAmount() );
				}

				@Override
				public IResource createResource(IResourceType type,
						IProductionLocation location)
				{
					return new SimpleResource( type , location ,0 );
				}

				@Override
				public Date getTimestamp()
				{
					return timestamp;
				}
		};
		
		public MyResourceManager(ICharacter character) {
			if ( character == null ) {
				throw new IllegalArgumentException("characterId cannot be NULL");
			}
			this.character = character;
		}
		
		@Override
		protected IResourceFactory getResourceFactory()
		{
			return resourceFactory;
		}
		
		@Override
		public void assetsChanged(ICharacter dummy)
		{
			updateResources( 
				assetManager.getAssets( this.character ).createMergedAssetListByType(true,false) , 
				assetManager.getTimestamp( this.character ) 
			);
		}
		
		private void updateResources( AssetList assets, EveDate timestamp ) {
			
			final List<IResource> resources =
				new ArrayList<IResource>();
			
			for ( Asset a : assets ) {
				convertToResource( a , resources );
			}
			
			internalSetResources( resources );
			this.timestamp = timestamp.getLocalTime();
		}
		
		private void convertToResource(Asset a ,List<IResource> resources) {
			if ( a.getLocation() instanceof Station ) {
				resources.add( new SimpleResource( a.getType(), (Station) a.getLocation(), a.getQuantity() ) );
			} else {
				log.error("convertToResource(): Hmmmm.... asset "+a.getType()+" , owned by " +
						a.getCharacterIds()+" is not located at a station ? location = "+a.getLocation()+
						" - ignoring it.");
			}
			
		}
		
		@Override
		protected void initHook()
		{
			updateResources( 
					assetManager.getAssets( this.character ).createMergedAssetListByType(true,false) , 
					assetManager.getTimestamp( this.character ) 
				);
		}

		@Override
		public Date getTimestamp()
		{
			return timestamp;
		}

		@Override
		public IResourceManager snapshot()
		{
			final MyResourceManager result = new MyResourceManager(this.character);
			result.timestamp = timestamp;
			cloneInstance( result );
			return result;
		}
		
	}
}
