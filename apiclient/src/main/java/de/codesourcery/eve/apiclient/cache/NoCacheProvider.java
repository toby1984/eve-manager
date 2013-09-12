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

import java.net.URI;
import java.util.Properties;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;

/**
 * A cache provider that does no caching at all.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class NoCacheProvider implements IResponseCacheProvider , IResponseCache {

	public static final IResponseCacheProvider SINGLETON = new NoCacheProvider();
	
	public static IResponseCacheProvider getInstance() {
		return SINGLETON;
	}
	
	private NoCacheProvider() {
	}
	
	@Override
	public IResponseCache getCache(URI baseURI) {
		return this;
	}

	@Override
	public void evict(APIQuery query) {
		// no-op
	}

	@Override
	public void clear() {
	}

	@Override
	public InternalAPIResponse get(APIQuery query) {
		return null;
	}

	@Override
	public void purgeCache(boolean force) {
	}

	@Override
	public void put(APIQuery query, InternalAPIResponse response) {
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void flushAllCaches() {
	}

	@Override
	public void setCacheOptions(Properties options)
	{
	}

}
