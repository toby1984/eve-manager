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

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.apiclient.parsers.IResponseParser;
import de.codesourcery.eve.skills.utils.EveDate;


public class InMemoryResponseCacheTest extends AbstractCacheTest {
	
	public static final String PAYLOAD = "test";
	
	private InMemoryResponseCache cache;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		cache = new InMemoryResponseCache();
	}
	
	protected IResponseParser<?> createMockParser() {
		
		final IResponseParser<?> result =
			createMock( IResponseParser.class );
		
		final EveDate serverTime = new EveDate(systemClock);
		final EveDate cachedUntil = serverTime;
		
		expect( result.getError() ).andReturn( null ).anyTimes();
		expect( result.getServerTime() ).andReturn( serverTime ).anyTimes();
		expect( result.getCachedUntilServerTime() ).andReturn( cachedUntil ).anyTimes();
		
		
		replay( result );
		return result;
	}
	
	public void testDifferentReleativeURIs() throws Exception {
		
		final Map<String, Object> params =
			new HashMap<String, Object>();
		
		params.put("key1" , "value1" );
		
		final APIQuery query1 = new APIQuery(
				new URI("http://localhost"),
				"/just/a/test",
				params );
		
		final APIQuery query2 = new APIQuery(
				new URI("http://localhost"),
				"/just/a/test2",
				params );		
		
		final InternalAPIResponse response1 =
			new InternalAPIResponse( PAYLOAD , new Date() , createMockParser() );
		
		cache.put( query1 , response1 );
		assertNull( cache.get( query2 ) );
		
		assertSame( response1 , cache.get( query1 ) );
		assertEquals( 2*PAYLOAD.length() , cache.getSize() );
	}
	
	public void testCacheHit() throws Exception {
		
		final Map<String, Object> params1 =
			new HashMap<String, Object>();
		
		params1.put("key1" , "value1" );
		
		final Map<String, Object> params2 =
			new HashMap<String, Object>();
		
		params2.put("key1" , "value2" );
		
		final APIQuery query1 = new APIQuery(
				new URI("http://localhost"),
				"/just/a/test",
				params1 );
		
		final APIQuery query2 = new APIQuery(
				new URI("http://localhost"),
				"/just/a/test",
				params2 );		
		
		final InternalAPIResponse response1 =
			new InternalAPIResponse( PAYLOAD , new Date() , createMockParser() );
		
		final InternalAPIResponse response2 =
			new InternalAPIResponse( PAYLOAD , new Date() , createMockParser() );		
		
		cache.put( query1 , response1 );
		cache.put( query2 , response2 );
		
		assertSame( response1 , cache.get( query1 ) );
		assertSame( response2 , cache.get( query2 ) );
		assertEquals( 2*2*PAYLOAD.length() , cache.getSize() );
	}
	
	public void testEvict() throws Exception {
		
		final Map<String, Object> params =
			new HashMap<String, Object>();
		
		final APIQuery query = new APIQuery(
				new URI("http://localhost"),
				"/just/a/test",
				params );
		
		final InternalAPIResponse response =
			new InternalAPIResponse( PAYLOAD , new Date() , createMockParser() );
		
		System.out.println( query.getHashString()+" <<< stored ");
		cache.put( query , response );
		
		assertSame( response , cache.get( query ) );
		assertEquals( 2*PAYLOAD.length() , cache.getSize() );
		
		System.out.println( query.getHashString()+" <<< evicted");
		cache.evict( query );
		
		System.out.println( query.getHashString()+" <<< fetching");
		final InternalAPIResponse removed = 
			cache.get( query );
		
		System.out.println("got: "+removed);
		
		assertNull( removed );
	}	
	
	public void testFlush() throws Exception {
		
		final Map<String, Object> params =
			new HashMap<String, Object>();
		
		final APIQuery query = new APIQuery(
				new URI("http://localhost"),
				"/just/a/test",
				params );
		
		final InternalAPIResponse response =
			new InternalAPIResponse( PAYLOAD , new Date() , createMockParser() );
		
		cache.put( query , response );
		
		assertSame( response , cache.get( query ) );
		assertEquals( 2*PAYLOAD.length() , cache.getSize() );
		
		cache.clear();
		
		assertNull( cache.get( query ) );
		assertEquals( 0 , cache.getSize() );
	}		

}
