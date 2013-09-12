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

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.AssertionFailedError;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.apiclient.parsers.IResponseParser;
import de.codesourcery.eve.skills.utils.EveDate;

public class FilesystemResponseCacheTest extends AbstractCacheTest {
	
	private FilesystemResponseCache cache;

	private static final Method findMethod(String name) {
		
		final Class<?> clasz =
			FilesystemResponseCache.class;
		
		Method result = null;
		Class<?> current = clasz;
		do {
			result = findMethod( current , name );
			if ( result != null ) {
				return result;
			}
		} while ( current != null );
		throw new RuntimeException("Unable to find method "+name);
	}
	
	private static Method findMethod(Class<?> clasz,String name) {
		for ( Method m : clasz.getDeclaredMethods() ) {
			final int mods = m.getModifiers();
			
			if ( Modifier.isStatic( mods ) ||
				 Modifier.isFinal( mods ) ) 
			{
				continue;
			}
			
			if ( m.getName().equals( name ) ) {
				return m;
			}
		}
		return null;
	}
	
	private File tmpDir;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		File tmpFile =
			File.createTempFile("cachetest","dir");

		tmpFile.delete();
		
		if ( ! tmpFile.mkdir() ) {
			throw new IOException("Unable to create tmp dir "+tmpFile);
		}
		
		this.tmpDir = tmpFile;
		cache = new FilesystemResponseCache( tmpDir , systemClock );
	}
	
	@Override
	protected void tearDown() throws Exception {
		
		super.tearDown();
		
		if ( tmpDir == null ) {
			return;
		}
		
		final FileFilter filter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getName().endsWith(".xml");
			}};
		
		for ( File f : tmpDir.listFiles( filter ) ) {
			f.delete();
		}
		
		tmpDir.delete();
	}
	
	private InternalAPIResponse createMockResponse(int size) {
		
		if ( ( size % 2 ) != 0 ) {
			throw new IllegalArgumentException("Invalid size, must be dividable by 2");
		}
		
		final Date now = new Date();
		
		final EveDate serverTime= EveDate.fromServerTime( new Date( now.getTime() - 2*60*60*1000 ) , systemClock );
		final EveDate cachedUntil = serverTime.addMilliseconds( 24*60*60*1000 );
		
		final IResponseParser<?> parser = createMock(IResponseParser.class);
		
		expect( parser.getError() ).andReturn( null ).anyTimes();
		expect( parser.getServerTime() ).andReturn( serverTime ).anyTimes();
		expect( parser.getCachedUntilServerTime() ).andReturn( cachedUntil ).anyTimes();
		
		replay( parser );
		
		final char[] payload = new char[ size >> 2 ];
		for ( int i = 0 ; i < payload.length ; i++ ) {
			payload[i]= 'X';
		}
		
		return new InternalAPIResponse( new String( payload ) ,
					now , parser ); 
	}
	
	private APIQuery createQuery(String relativeURI , String... keys ) throws Exception {
		
		final URI baseURI = new URI("http://test.de");
		
		final Map<String, Object> params = new HashMap<String, Object>();
		if ( keys != null ) {
			for ( String k : keys ) {
				params.put( k , "dummy" );
			}
		}
		return new APIQuery( baseURI , relativeURI , params ); 
	}
	
	protected void assertCacheDirEmpty() {
		
		final String[] files = tmpDir.list();
		if ( files.length > 0 ) {
			throw new AssertionFailedError("Cache dir should be empty but contains "+
					ObjectUtils.toString( files ) );
		}
	}
	
	protected void assertCacheDirNotContains(APIQuery query) {
		
		File file = cache.getFilenameForEntry( query );
		
		if ( file.exists() ) {
			throw new AssertionFailedError("Cache dir should NOT contain file "+file.getAbsolutePath());
		}
		
	}	
	
	protected void assertCacheDirContains(APIQuery query) {
		
		File file = cache.getFilenameForEntry( query );
		
		if ( ! file.exists() ) {
			throw new AssertionFailedError("Cache dir should contain file "+file.getAbsolutePath());
		}
		
		if ( file.length() == 0 ) {
			throw new AssertionFailedError("Cache dir should contain non-empty file "+file.getAbsolutePath());
		}
	}	
	
	public void testSimpleStore() throws Exception {
		
		/*
		final Constructor<FilesystemResponseCache2> c = 
			FilesystemResponseCache2.class.getConstructor( File.class );
		
		ConstructorArgs args = new ConstructorArgs( c , tmpDir );
		Method[] methods = new Method[] { findMethod("...."};
		*/
		
		final APIQuery query = createQuery( "/server/test.xml" );
		InternalAPIResponse response = createMockResponse( 1024 );
		
		cache.put( query , response );
		
		assertCacheDirNotContains( query );
		
		InternalAPIResponse cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response.getPayload() , cached.getPayload() );

		cache.clear();
		
		assertCacheDirContains( query );
		
		cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response.getPayload() , cached.getPayload() );
	}
	
	public void testPutInImmediateWriteModeStoresOnDisk() throws Exception {
		
		final Properties properties = new Properties();
		properties.setProperty( FilesystemResponseCache.OPTION_WRITE_IMMEDIATELY , "true" );
		
		cache.setCacheOptions( properties );
		final APIQuery query = createQuery( "/server/test.xml" );
		InternalAPIResponse response = createMockResponse( 1024 );
		
		assertCacheDirNotContains( query );
		
		cache.put( query , response );
		
		assertCacheDirContains( query );
		
		InternalAPIResponse cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response.getPayload() , cached.getPayload() );
		
		FilesystemResponseCache cache2 = new FilesystemResponseCache( tmpDir , systemClock );

		InternalAPIResponse cached2 = cache2.get( query );
		assertNotNull( cached2 );
		assertEquals( response.getPayload() , cached2.getPayload() );		
	}
	
	public void testGetDeletesCorruptCachefiles() throws Exception {
		
		final Properties properties = new Properties();
		properties.setProperty( FilesystemResponseCache.OPTION_WRITE_IMMEDIATELY , "true" );
		
		cache.setCacheOptions( properties );
		final APIQuery query = createQuery( "/server/test.xml" );
		InternalAPIResponse response = createMockResponse( 1024 );
		
		assertCacheDirNotContains( query );
		
		cache.put( query , response );
		
		assertCacheDirContains( query );
		
		final File file = cache.getFilenameForEntry( query );
		assertTrue( file.delete() );

		final FileWriter writer = new FileWriter( file , false );
		writer.write("blubb");
		writer.close();
		
		FilesystemResponseCache cache2 = new FilesystemResponseCache( tmpDir , systemClock );

		InternalAPIResponse cached2 = cache2.get( query );
		assertNull( cached2 );
		assertFalse( file.exists() );
	}
	
	public void testPutOverwritesCorruptCachefiles() throws Exception {
		
		final Properties properties = new Properties();
		properties.setProperty( FilesystemResponseCache.OPTION_WRITE_IMMEDIATELY , "true" );
		
		cache.setCacheOptions( properties );
		final APIQuery query = createQuery( "/server/test.xml" );
		InternalAPIResponse response = createMockResponse( 1024 );
		
		assertCacheDirNotContains( query );
		
		cache.put( query , response );
		
		assertCacheDirContains( query );
		
		final File file = cache.getFilenameForEntry( query );
		assertTrue( file.delete() );

		final FileWriter writer = new FileWriter( file , false );
		writer.write("blubb");
		writer.close();
		
		cache.put( query , response );

		FilesystemResponseCache cache2 = new FilesystemResponseCache( tmpDir , systemClock );

		InternalAPIResponse cached2 = cache2.get( query );
		assertNotNull( cached2 );
		assertEquals( response.getPayload() , cached2.getPayload() );			
	}
	
	public void testSimpleStore2() throws Exception {
		
		final APIQuery query = createQuery( "/server/test.xml" );
		InternalAPIResponse response = createMockResponse( 1024 );
		
		cache.put( query , response );
		
		assertCacheDirNotContains( query );
		
		InternalAPIResponse cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response.getPayload() , cached.getPayload() );

		cache.shutdown();
		
		assertCacheDirContains( query );
		
		cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response.getPayload() , cached.getPayload() );
		
		cache = new FilesystemResponseCache( this.tmpDir , systemClock);
		
		cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response.getPayload() , cached.getPayload() );		
	}	
	
	public void testReplaceEntry() throws Exception {
		
		/*
		final Constructor<FilesystemResponseCache2> c = 
			FilesystemResponseCache2.class.getConstructor( File.class );
		
		ConstructorArgs args = new ConstructorArgs( c , tmpDir );
		Method[] methods = new Method[] { findMethod("...."};
		*/
		
		final APIQuery query = createQuery( "/server/test.xml" );
		InternalAPIResponse response = createMockResponse( 1024 );
		
		cache.put( query , response );
		
		assertCacheDirNotContains( query );
		
		cache.clear();
		
		assertCacheDirContains( query );
		
		InternalAPIResponse cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response.getPayload() , cached.getPayload() );		
		
		final InternalAPIResponse response2 = createMockResponse( 512 );
		
		cache.put( query , response2 );
		
		assertCacheDirNotContains( query );
		
		cache.clear();
		
		cached = cache.get( query );
		assertNotNull( cached );
		assertEquals( response2.getPayload() , cached.getPayload() );			
	}	
}
