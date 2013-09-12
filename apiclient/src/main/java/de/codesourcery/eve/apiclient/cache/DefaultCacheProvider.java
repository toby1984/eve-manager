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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache provider that manages a {@link InMemoryResponseCache} instance.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class DefaultCacheProvider implements IResponseCacheProvider {

	private final Map<URI,IResponseCache> caches =
		new ConcurrentHashMap<URI,IResponseCache>();
	
	private final Properties options = 
		new Properties();
	
	@Override
	public IResponseCache getCache(URI baseURI) 
	{
		synchronized( caches ) {
			
			IResponseCache result = caches.get( baseURI );
			if ( result == null ) {
				result = new InMemoryResponseCache();
				result.setCacheOptions( options );
				caches.put( baseURI , result );
			}
			return result;
		}
	}

	@Override
	public void shutdown() {
		
		synchronized( caches ) {
			for ( IResponseCache cache : caches.values() ) {
				cache.shutdown();
			}
		}	
	}

	@Override
	public void flushAllCaches() {
		synchronized( caches ) {
			for ( IResponseCache cache : caches.values() ) {
				cache.clear();
			}
		}		
	}

	@Override
	public void setCacheOptions(Properties options)
	{
		if ( options == null ) {
			throw new IllegalArgumentException("options cannot be NULL");
		}
		
		synchronized( caches ) 
		{
			this.options.clear();
			this.options.putAll( options );
			for (IResponseCache cache : caches.values() ) {
				cache.setCacheOptions( options );
			}
		}
		
	}

}
