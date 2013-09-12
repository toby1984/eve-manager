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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.utils.ICipherProvider;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Cache provider that uses {@link FilesystemResponseCache} instances as storage
 * backend.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class FilesystemCacheProvider implements IResponseCacheProvider {

	private static final Logger log = Logger
			.getLogger(FilesystemCacheProvider.class);

	private final File cacheDirectory;

	// guarded-by: caches
	private final Map<URI, FilesystemResponseCache> caches = new HashMap<URI, FilesystemResponseCache>();

	private final ISystemClock systemClock;
	private final ICipherProvider cipherProvider;

	private final Properties options = 
		new Properties();

	public FilesystemCacheProvider(File cacheDirectory,ISystemClock systemClock) throws IOException {
		this(cacheDirectory, null,systemClock);
	}

	public FilesystemCacheProvider(File cacheDirectory,
			ICipherProvider cipherProvier,ISystemClock systemClock) throws IOException {

		if ( systemClock == null ) {
			throw new IllegalArgumentException("systemClock cannot be NULL");
		} 
		
		if (cacheDirectory == null) {
			throw new IllegalArgumentException("cacheDirectory cannot be NULL");
		}

		this.systemClock = systemClock;
		this.cipherProvider = cipherProvier;

		if (!cacheDirectory.exists()) {
			log.info("FilesystemCacheProvider(): CREATING cache directory "
					+ cacheDirectory.getAbsolutePath());
			if (!cacheDirectory.mkdirs()) {
				throw new IOException("Unable to create cache directory: "
						+ cacheDirectory);
			}
		}

		log.info("FilesystemCacheProvider(): Using cache directory "
				+ cacheDirectory.getAbsolutePath());

		this.cacheDirectory = cacheDirectory;
	}

	@Override
	public void flushAllCaches() {
		synchronized (caches) {
			for (FilesystemResponseCache cache : caches.values()) {
				cache.clear();
			}
		}
	}

	@Override
	public IResponseCache getCache(URI baseURI) {

		synchronized (caches) {

			FilesystemResponseCache result = caches.get(baseURI);

			if (result == null) {

				log.info("getCache(): Creating new cache for URI " + baseURI);

				try {
					result = new FilesystemResponseCache(cacheDirectory,
							cipherProvider,systemClock);
					result.setCacheOptions( options );
					caches.put(baseURI, result);
				} catch (Exception e) {
					log.error("getCache(): Failed to create cache for "
							+ baseURI, e);
					throw new RuntimeException(e);
				}

			}
			return result;
		}
	}

	protected final String toFileName(URI baseURI) {
		final String hostPart = baseURI.getHost().replaceAll("[\\.]", "_");

		final String localPart = baseURI.getPath().replaceAll("[ /]", "_");
		return "cache_" + hostPart + localPart + ".xml";
	}

	@Override
	public void shutdown() {
		synchronized (caches) {
			log.info("shutdown(): Flushing " + caches.size() + " caches.");
			for (FilesystemResponseCache cache : caches.values()) {
				cache.shutdown();
				cache.clear();
			}
			caches.clear();
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
			for (FilesystemResponseCache cache : caches.values()) {
				cache.setCacheOptions( options );
			}
		}
		
	}

}
