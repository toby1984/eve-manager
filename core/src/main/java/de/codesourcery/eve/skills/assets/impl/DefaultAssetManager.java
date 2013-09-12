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
package de.codesourcery.eve.skills.assets.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.assets.IAssetManager;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.utils.EveDate;

public class DefaultAssetManager implements IAssetManager
{

	private static final Logger log = Logger
			.getLogger(DefaultAssetManager.class);
	
	private IAPIClient apiClient;
	private IUserAccountStore userAccountStore;
	
	// guarded-by: listeners
	private final List<IAssetChangeListener> listeners =
		new ArrayList<IAssetChangeListener>();
	
	// guarded-by: lastUpdateFromServer
	private final Map<CharacterID,EveDate> lastUpdateFromServer = 
		new HashMap<CharacterID, EveDate>();
	
	public DefaultAssetManager() {
	}
	
	@Override
	public AssetList getAssets(ICharacter character)
	{
		log.info("getAssets(): Fetching assets of character "+character);
		
		final UserAccount account = 
			userAccountStore.getAccountByCharacterID( character.getCharacterId() );
		
		final APIResponse<AssetList> assetList = 
			apiClient.getAssetList( character.getCharacterId(),
				account ,
				RequestOptions.DEFAULT );
		
		boolean notifyListeners = false;
		if ( log.isDebugEnabled() ) {
			log.debug("getAssets(): Assets of "+character+" fetched, checking last_update date");
		}
		synchronized ( lastUpdateFromServer) {
			
			final EveDate lastUpdate =
				lastUpdateFromServer.get( character.getCharacterId() );
			
			if ( log.isDebugEnabled() ) {
				log.debug("getAssets(): character "+character.getCharacterId()+" , last_update_from_server="+
						lastUpdate);
			}
			if ( lastUpdate == null || lastUpdate.before( assetList.getResponseServerTime() ) ) 
			{
				lastUpdateFromServer.put( character.getCharacterId() , assetList.getResponseServerTime() );
				notifyListeners = true;
			}
		}
		
		if ( log.isDebugEnabled() ) {
			log.debug("getAssets(): notifyListeners = "+notifyListeners);
		}
		
		if ( notifyListeners ) {
			List<IAssetChangeListener> copy;
			synchronized ( listeners ) {
				copy = new ArrayList<IAssetChangeListener>( listeners );
			}
			for ( IAssetChangeListener l : copy ) {
				try {
					l.assetsChanged( character );
				} catch(Exception e) {
					log.error("getAssets(): Asset change listener failed",e);
				}
			}
		}
		return assetList.getPayload();
	}

	@Override
	public AssetList getAssets(ICharacter character, ILocation location)
	{
		return getAssets( character ).getAssetsByLocation( location );
	}

	@Override
	public AssetList getAssets(ICharacter character, InventoryType item)
	{
		return getAssets( character ).getAssetsByType( item );
	}

	public void setApiClient(IAPIClient apiClient)
	{
		this.apiClient = apiClient;
	}

	public void setUserAccountStore(IUserAccountStore userAccountStore)
	{
		this.userAccountStore = userAccountStore;
	}

	@Override
	public void addAssetChangeListener(IAssetChangeListener listener)
	{
		if ( listener == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized ( listeners ) {
			listeners.add( listener );
		}
	}

	@Override
	public void removeAssetChangeListener(IAssetChangeListener listener)
	{
		if ( listener == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized ( listeners ) {
			listeners.remove( listener );
		}		
	}

	@Override
	public EveDate getTimestamp(ICharacter character)
	{
		if ( character == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		
		synchronized(lastUpdateFromServer) {
			return lastUpdateFromServer.get( character.getCharacterId() );
		}
	}

}
