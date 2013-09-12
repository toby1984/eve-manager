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
package de.codesourcery.eve.apiclient.cache;

import java.util.Map;
import java.util.Properties;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;

/**
 * A cache for responses from the EVE Online(tm) API.
 *
 * @author tobias.gierke@code-sourcery.de
 * 
 * @see IAPIClient#setCacheProvider(IResponseCacheProvider)
 */
public interface IResponseCache {

	/**
	 * Retrieves a cached API query result.
	 *  
	 * @param query
	 * @return cached result or <code>null</code>
	 */
	public abstract InternalAPIResponse get(APIQuery query);

	/**
	 * Stores an API query's result in this cache.
	 * 
	 * @param query
	 * @param response
	 */
	public abstract void put(APIQuery query, InternalAPIResponse response);

	/**
	 * Try to remove stale entries from this cache.
	 * 
	 * @param force set to <code>true</code> to aggressively 
	 * free as much memory as possible.
	 */
	public abstract void purgeCache(boolean force);

	/**
	 * Removes a specific query result from this cache.
	 * @param query
	 */
	public abstract void evict(APIQuery query);

	/**
	 * Removes all data from this cache.
	 */
	public abstract void clear();
	
	/**
	 * Destroys this cache.
	 * 
	 * After this method returns the 
	 * cache is no longer in a usable state.
	 */
	public abstract void shutdown();	
	
	/**
	 * Configure cache options.
	 * 
	 * @param options
	 */
	public void setCacheOptions(Properties options);

}