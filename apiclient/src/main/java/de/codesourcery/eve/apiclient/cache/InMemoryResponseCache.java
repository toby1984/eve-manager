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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Crude thread-safe cache implementation used
 * for caching server responses.
 *
 * <pre>
 * This cache always try to utilize it's maximum size. When the
 * maximum size is exceeded, the cache tries to purge as many 
 * stale ( cachedUntilTime > serverTime ) entries as necessary
 * to get below the threshhold. If the cache is still over limit
 * after all stale entries have been removed, the cache will
 * start removing entries by descending size, starting with the 
 * largest entry first.
 * 
 * Subclassers must make sure their implementation is thread-safe.
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class InMemoryResponseCache implements IResponseCache {

	private static final Logger log = Logger.getLogger(InMemoryResponseCache.class);

	// guarded-by: cache
	// key = APIQuery#getHashString()
	private final Map<String,InternalAPIResponse> cache =
		new HashMap<String, InternalAPIResponse>();

	private volatile long payloadSize = 0;

	/**
	 * Max. cache size in bytes.
	 */
	private volatile long maxSize;
	
	/**
	 * Config option: Size if in-memory cache in bytes.
	 * 
	 * Possible values: Long value.
	 * 
	 * @see #setCacheOptions(Map)
	 */
	public static final String OPTION_MEM_CACHE_SIZE= "memcache.cache_size";

	/**
	 * Create instance.
	 * 
	 * Creates a cache instance with default size (250 KB).
	 */
	public InMemoryResponseCache() {
		this( 250*1024 ); // 250 KB cache
	}

	/**
	 * Sets the maximum amount of 
	 * memory this cache should occupy.
	 * 
	 * @param maxSize max. cache size in bytes, at least 25 KB ( 25 * 1024 )
	 */
	private void setMaxCacheMemSize(long maxSize) {
		if ( maxSize < 25 * 1024 ) {
			throw new IllegalAccessError("Cache size must be at least 25 KB ("+(25*1024)+" bytes)");
		}
		log.info("setCacheSize(): New cache size: "+maxSize+" bytes.");
		final boolean shrink = this.maxSize > maxSize;
		this.maxSize = maxSize;
		if ( shrink ) {
			purgeCache(true);
		}
	}

	/**
	 * Create instance.
	 * 
	 * @param maxSize Maximum cache size in bytes, must be at least
	 * 25 KB (25 * 1024 bytes). 
	 */
	public InMemoryResponseCache(int maxSize) {
		if ( maxSize < 25*1024 ) {
			throw new IllegalArgumentException("Cache size must be at least 25 KB");
		}
		log.info("InMemoryResponseCache(): Cache created , size "+(maxSize/1024)+" KB");
		this.maxSize = maxSize;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.apiclient.IResponseCache#get(de.codesourcery.eve.apiclient.datamodel.APIQuery)
	 */
	public InternalAPIResponse get(APIQuery query) {
		if (query == null) {
			throw new IllegalArgumentException("query cannot be NULL");
		}

		InternalAPIResponse result=null;
		synchronized( cache ) {
			result = cache.get( query.getHashString() );
			if ( log.isTraceEnabled() ) {
				if ( result != null ) {
					log.trace("get(): [ 1ST LEVEL CACHE HIT ] query = "+query.getHashString());
				} else {
					log.trace("get(): [ 1ST LEVEL CACHE MISS ] query = "+query.getHashString());
				}
			}
			if ( result == null ) {
				result = loadFromBackingStore( query );
			}
		}
		return result;
	}

	protected long getMaxCacheMemSize() {
		return maxSize;
	}

	protected InternalAPIResponse loadFromBackingStore(APIQuery query) {
		return null;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.apiclient.IResponseCache#put(de.codesourcery.eve.apiclient.datamodel.APIQuery, de.codesourcery.eve.apiclient.InternalAPIResponse)
	 */
	public void put(APIQuery query, InternalAPIResponse response) {

		if ( query == null ) {
			throw new IllegalArgumentException("query cannot be NULL");
		}

		if ( response == null ) {
			throw new IllegalArgumentException("response cannot be NULL");
		}

		synchronized( cache ) 
		{
			final InternalAPIResponse evicted = cache.put( query.getHashString() , response );

			this.payloadSize += response.getPayloadSize();

			if ( evicted != null ) {
				this.payloadSize -= evicted.getPayloadSize();
				cacheEntryReplaced( query );
			}

			if ( this.payloadSize > getMaxCacheMemSize() ) {
				purgeCache(true);
			}
		}

		if ( log.isTraceEnabled() ) {
			log.trace("put(): entry added , cache size now: "+this.payloadSize+" bytes");
		}

	}

	protected void cacheEntryReplaced(APIQuery query) {

	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.apiclient.IResponseCache#purgeCache(boolean)
	 */
	public final void purgeCache(boolean force) {

		synchronized( cache ) {
			purgeItems(force);
		}
	}

	private static final ISystemClock clock = new ISystemClock() {

		@Override
		public long getCurrentTimeMillis() {
			return System.currentTimeMillis();
		}

		@Override
		public TimeZone getLocalTimezone() {
			return TimeZone.getDefault();
		}
	};

	protected void purgeItems(boolean force) {

		synchronized( cache ) {
			while ( getSize() > maxSize || force ) {

				String largestKey = null; 
				InternalAPIResponse largest=null;

				for(Iterator<String> it = this.cache.keySet().iterator() ; it.hasNext()  ;  ) 
				{
					final String query = it.next();

					final InternalAPIResponse response =
						this.cache.get( query );

					if ( response.mayBeRequestedAgain( clock ) ) {

						it.remove();

						if ( log.isTraceEnabled() ) {
							log.trace("purgeItems(): freed "+response.getPayloadSize()+" bytes.");
						}			
						payloadSize -= response.getPayloadSize();

						cacheEntryEvicted( query , response); 

						// not forced = remove as few entries as possible
						if ( getSize() <= maxSize && ! force) {
							return;
						}
						continue;
					}

					if ( largest == null || response.hasLargerPayloadThan( largest ) )
					{
						largestKey = query;
						largest = response;
					}
				}

				// stop , we've already reached a size below maxSize
				if ( getSize() <= maxSize ) {
					return;
				}

				if ( largest == null) { // should never happen 
					return;
				}

				// remove largest chunk from cache and try again
				final InternalAPIResponse removed =
					this.cache.remove( largestKey );
				if ( log.isTraceEnabled() ) {
					log.trace("purgeItems(): freed "+largest.getPayloadSize()+" bytes.");
				}				
				this.payloadSize -= largest.getPayloadSize();
				cacheEntryEvicted( largestKey , removed ); 
			}
		}
	}

	protected void cacheEntryEvicted(String queryHash , InternalAPIResponse entry) {

	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.apiclient.IResponseCache#evict(de.codesourcery.eve.apiclient.datamodel.APIQuery)
	 */
	public void evict(APIQuery query) {
		if ( query == null ) {
			throw new IllegalArgumentException("query cannot be NULL");
		}
		synchronized (cache) {
			final InternalAPIResponse evicted = cache.remove( query.getHashString() );
			if ( evicted != null ) {
				this.payloadSize -= evicted.getPayloadSize();
				if ( log.isTraceEnabled() ) {
					log.trace("evict(): freed "+evicted.getPayloadSize()+" bytes.");
				}				
				cacheEntryEvicted( query.getHashString() , evicted );
			}
		}
	}

	/**
	 * Returns the approximate payloadSize of the payload
	 * contained in this cache.
	 * 
	 * @return
	 */
	public long getSize() {
		return this.payloadSize; // UTF-16 => 2 bytes per character
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.apiclient.IResponseCache#clear()
	 */
	public void clear() {

		log.debug("flush(): Discarding all cached entries.");

		synchronized (cache) {
			for ( Map.Entry<String,InternalAPIResponse> entry : cache.entrySet() ) {
				cacheEntryEvicted( entry.getKey() , entry.getValue() );
			}
			cache.clear();
			this.payloadSize = 0;
		}
	}

	@Override
	public final void shutdown() {
		synchronized( cache ) {
			shutdownHook();
			clear();
		}
	}

	protected interface ICacheVisitor {
		public void visit(String apiQueryHashKey, InternalAPIResponse response) throws Exception;
	}

	protected void visitCache(ICacheVisitor v) throws Exception {
		synchronized( cache ) {
			for ( Map.Entry<String, InternalAPIResponse> entry : cache.entrySet() ) {
				v.visit( entry.getKey() , entry.getValue() );
			}
		}
	}

	protected void shutdownHook() {
	}

	@Override
	public void setCacheOptions(Properties options)
	{
		if ( options.containsKey( OPTION_MEM_CACHE_SIZE ) ) {
			final String value =
				options.getProperty(OPTION_MEM_CACHE_SIZE,Long.toString( 2000 * 1024 ) );
			
			try {
				final long cacheSizeInBytes = Long.parseLong( value.trim() );
				log.info("setCacheOptions(): Setting cache size to "+( cacheSizeInBytes / 1024 )+" KB");
				setMaxCacheMemSize( cacheSizeInBytes );
			} catch(Exception e) {
				log.error("setCacheOptions(): Failed to set memory size to '"+value+"'");
			}
		}
	}
}
