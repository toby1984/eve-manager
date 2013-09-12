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
package de.codesourcery.eve.skills.dao;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.IAPIClient.EntityType;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.exceptions.UnresolvableIDException;
import de.codesourcery.eve.skills.datamodel.CorporationId;
import de.codesourcery.eve.skills.db.dao.ICorporationDAO;
import de.codesourcery.eve.skills.db.datamodel.Corporation;

/**
 * DAO that resolves corporation (names) using
 * an {@link IAPIClient} instance.
 * 
 * This implementation supports caching that
 * can be enabled by calling {@link #setCacheEnabled(boolean)}.
 * Caching is DISABLED by default.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class APICorporationDAO implements ICorporationDAO {

	public static final Logger log = Logger.getLogger(APICorporationDAO.class);

	private final IAPIClient apiClient;

	private volatile boolean isCacheEnabled = false;

	private final ConcurrentHashMap<Long,Corporation> cache =
		new ConcurrentHashMap<Long, Corporation>();

	public APICorporationDAO(IAPIClient apiClient) {
		if (apiClient == null) {
			throw new IllegalArgumentException("apiClient cannot be NULL");
		}
		this.apiClient = apiClient;
	}

	public static final EntityType[] TYPE = new EntityType[] { EntityType.CORPORATION };

	@Override
	public Corporation fetch(Long id) throws UnresolvableIDException {

		if (id == null) {
			throw new IllegalArgumentException("id cannot be NULL");
		}

		if ( isCacheEnabled ) {
			final Corporation result = cache.get( id );
			if ( result != null ) {
				return result;
			}
		}

		// do not hold lock during API call, might take ages to complete....
		Corporation result = fetchFromAPI( id );

		if ( isCacheEnabled ) {
			synchronized( cache ) {

				Corporation alreadyCached =
					cache.get( id );

				if ( alreadyCached == null ) {
					cache.put( id , result );
				} else {
					// another thread was faster and already resolved this ID,
					// return this thread's result instead.
					result = alreadyCached;
				}
			}
		}
		return result;
	}

	/**
	 * Enable/disable internal caching.
	 * 
	 * @param cacheEnabled enable/disable cache. Disabling
	 * the cache will discard any entries cached so far. 
	 */
	public void setCacheEnabled(boolean cacheEnabled) {
		log.info("setCacheEnabled(): cache on = "+cacheEnabled);
		this.isCacheEnabled = cacheEnabled;
		if ( ! isCacheEnabled ) {
			synchronized( cache ) {
				this.cache.clear();
			}
		} 
	}

	public Corporation fetchFromAPI(Long id) throws UnresolvableIDException {

		final String sId = Long.toString(id);
		final Map<String, String> names = apiClient.resolveNames( TYPE, 
				new String[] { sId }, RequestOptions.DEFAULT).getPayload();

		final String corpName = names.get(sId);
		if ( corpName == null ) {
			throw new UnresolvableIDException( sId , EntityType.CORPORATION );
		}

		final Corporation result = new Corporation();
		result.setId( new CorporationId( id ) );
		result.setName( corpName );
		return result;
	}

	@Override
	public List<Corporation> fetchAll() {
		throw new UnsupportedOperationException("fetchAll() not supported");
	}

}
