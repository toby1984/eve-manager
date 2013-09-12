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

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;

public class FilesystemCacheProviderTest extends AbstractCacheTest {

	private final File baseDir;
	
	private FilesystemCacheProvider provider;
	
	public FilesystemCacheProviderTest() throws IOException {
		File tmpFile = File.createTempFile("prefix","suffix");
		if ( ! tmpFile.exists() ) {
			throw new RuntimeException("Unable to create tmp file ?");
		}
		File tmpDir = tmpFile.getParentFile();
		tmpFile.delete();
		 baseDir = new File( tmpDir , "cachedir" );
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		if ( ! baseDir.exists() ) {
			if ( ! baseDir.mkdirs() ) {
				throw new RuntimeException("Unable to create tmp directory "+baseDir.getAbsolutePath() );
			}
			System.out.println("Temp dir created.");
		}
		provider = new FilesystemCacheProvider( baseDir , systemClock );
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if ( baseDir.exists() ) {
			File[] files = baseDir.listFiles();
			if ( files != null ) {
				for ( File f : files ) {
					f.delete();
				}
			}
			baseDir.delete();
		}
	}
	
	public void testOneCache() throws Exception {
		
		final URI URI1 =
			new URI("http://localhost/just/a/test" );
		
		final URI URI2=
			new URI("http://localhost/just/another/test");
		
		IResponseCache cache1 =
			provider.getCache( URI1 );
		
		final InternalAPIResponse response1 =
			new InternalAPIResponse( "<sampleXml>just a test</sampleXml>" ,
					createDate("2009-06-11 11:12:13" ), // timestamp
					createEveDate("2009-06-11 09:12:13" ), // serverTime
					createEveDate("2009-06-12 09:12:13" ) // cachedUntil
			);

		final APIQuery query = createQuery( "test/path" , new HashMap<String, Object>() );
		cache1.put( query , response1 );
		
		IResponseCache cache2 =
			provider.getCache( URI2 );
		
		final Map<String,Object> params2 =
			new HashMap<String, Object>();
		params2.put("key" , "value" );

		final InternalAPIResponse response2 =
			new InternalAPIResponse( "<sampleXml>example2</sampleXml>" ,
					createDate("2009-06-11 11:12:13" ), // timestamp
					createEveDate("2009-06-12 09:12:13" ), // serverTime
					createEveDate("2009-06-13 09:12:13" ) // cachedUntil
			);

		final APIQuery query2 = 
			createQuery( "test/path" , params2 );
		cache2.put( query2 , response2 );
		
		this.provider.shutdown();
		
		// ============= verify
		
		cache1 =
			provider.getCache( URI1 );
		cache2 =
			provider.getCache( URI2 );
		
		InternalAPIResponse response =
			cache1.get( query );

		assertNotNull( response );
		assertSameDate( "2009-06-11 11:12:13" , response.getTimestamp() );
		assertSameDate( "2009-06-11 09:12:13" , response.getServerTime() );
		assertSameDate( "2009-06-12 09:12:13" , response.getCachedUntilServerTime() );
		assertEquals("<sampleXml>just a test</sampleXml>" ,  response.getPayload() );
		
		response =
			cache2.get( query2 );

		assertNotNull( response );
		assertSameDate( "2009-06-11 11:12:13" , response.getTimestamp() );
		assertSameDate( "2009-06-12 09:12:13" , response.getServerTime() );
		assertSameDate( "2009-06-13 09:12:13" , response.getCachedUntilServerTime() );
		assertEquals("<sampleXml>example2</sampleXml>" ,  response.getPayload() );		
	}
	
}
