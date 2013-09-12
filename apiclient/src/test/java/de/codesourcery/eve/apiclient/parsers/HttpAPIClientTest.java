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
package de.codesourcery.eve.apiclient.parsers;

import static org.easymock.EasyMock.*;
import static org.easymock.classextension.EasyMock.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.commons.lang.ObjectUtils;
import org.apache.http.client.ClientProtocolException;

import de.codesourcery.eve.apiclient.HttpAPIClient;
import de.codesourcery.eve.apiclient.ICredentialsProvider;
import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.cache.IResponseCache;
import de.codesourcery.eve.apiclient.cache.IResponseCacheProvider;
import de.codesourcery.eve.apiclient.cache.InMemoryResponseCache;
import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyRole;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions.DataRetrievalStrategy;
import de.codesourcery.eve.apiclient.exceptions.APIException;
import de.codesourcery.eve.apiclient.exceptions.APIUnavailableException;
import de.codesourcery.eve.apiclient.exceptions.ResponseNotCachedException;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class HttpAPIClientTest extends TestCase {

	private static final URI BASE_URI =
		toURI("http://localhost");

	private static final String STALE_RESPONSE =
		"<?xml version='1.0' encoding='utf-8'?>\n" + 
		"<eveapi version=\"2\">\n" + 
		"  <currentTime>2009-05-31 19:28:05</currentTime>\n" + 
		"  <result />\n" + 
		"  <cachedUntil>2009-04-01 18:28:05</cachedUntil>\n" + // outdated response
		"</eveapi>";
	
	private static final String CURRENT_RESPONSE =
		"<?xml version='1.0' encoding='utf-8'?>\n" + 
		"<eveapi version=\"2\">\n" + 
		"  <currentTime>2009-05-31 19:28:05</currentTime>\n" + 
		"  <result />\n" + 
		"  <cachedUntil>2009-07-01 18:28:05</cachedUntil>\n" + 
		"</eveapi>";	
	
	private static final String CURRENT_RESPONSE2 =
		"<?xml version='1.0' encoding='utf-8'?>\n" + 
		"<eveapi version=\"2\">\n" + 
		"  <currentTime>2009-05-31 19:28:05</currentTime>\n" + 
		"  <result />\n" + 
		"  <cachedUntil>2009-08-01 18:28:05</cachedUntil>\n" + 
		"</eveapi>";		

	private TestClient client;
	private final MockClock SYSTEM_CLOCK = new MockClock();
	
	protected EveDate currentTime() {
		return new EveDate( SYSTEM_CLOCK );
	}

	public void testOfflineModeCacheMiss() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();

		final AbstractResponseParser<String> parser =
			createMockParser();

		expect( parser.getRelativeURI() ).andReturn( testRelativeURI ).once();

		replay( parser );

		// run test
		client.setServerResponse( STALE_RESPONSE );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.OFFLINE );

		try {
			client.sendRequest2( 
					null , 
					parser , 
					params , 
					KeyRole.NONE_REQUIRED , 
					RequestOptions.DEFAULT );
			fail("Should have failed");
		} catch(ResponseNotCachedException e) {
			// ok
		}

		verify( parser );
	}

	public void testOfflineModeCacheHit() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache entry
		createMockCacheEntry( 
				createQuery( testRelativeURI , params ), 
				STALE_RESPONSE , 
				currentTime(), // server time
				createEarlierTime( SYSTEM_CLOCK.currentTime ) // cached until
		);
		
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.OFFLINE );
		
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( STALE_RESPONSE ).once();

		replay( parser2 );
		
		// run test
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );

		assertNotNull( response );
		assertEquals( STALE_RESPONSE , response.getPayload() );
		System.out.println( "response_cached_until = "+response.getCachedUntil() );
		assertFalse( response.isUpToDate( SYSTEM_CLOCK ) );
		verify( parser2 );
	}	
	
	public void testFetchLatestCacheMiss() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache entry
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FETCH_LATEST );
		
		client.setServerResponse( CURRENT_RESPONSE );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( CURRENT_RESPONSE ).once();

		replay( parser2 );
		
		// run test
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );

		assertNotNull( response );
		assertEquals( CURRENT_RESPONSE , response.getPayload() );
		assertTrue( response.isUpToDate( SYSTEM_CLOCK ) );
		verify( parser2 );
	}		
	
	public void testFetchLatestCacheMissResponseCachingEnabled() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache
		final InMemoryResponseCache mockCache = 
			new InMemoryResponseCache();
		
		final IResponseCacheProvider provider =
			createMock(IResponseCacheProvider.class );
		
		expect( provider.getCache( BASE_URI ) ).andReturn( mockCache ).anyTimes();
		
		replay( provider );
		
		// create fake cache entry
		client.setCacheProvider( provider );
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FETCH_LATEST );
		
		client.setServerResponse( CURRENT_RESPONSE );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( CURRENT_RESPONSE ).once();

		replay( parser2 );
		
		// run test
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );

		assertNotNull( response );
		
		assertEquals( CURRENT_RESPONSE , response.getPayload() );
		assertTrue( response.isUpToDate( SYSTEM_CLOCK ) );
		
		final InternalAPIResponse cached = mockCache.get(  createQuery( testRelativeURI , params ) );
		assertNotNull( cached );
		assertSame( CURRENT_RESPONSE , cached.getPayload()  );
		
		verify( parser2 );
		verify( provider );
	}	
	
	public void testFetchLatestCacheMissResponseCachingDisabled() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache
		final InMemoryResponseCache mockCache = 
			new InMemoryResponseCache();
		
		final IResponseCacheProvider provider =
			createMock(IResponseCacheProvider.class );
		
		expect( provider.getCache( BASE_URI ) ).andReturn( mockCache ).anyTimes();
		
		replay( provider );
		
		// create fake cache entry
		client.setCacheProvider( provider );
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FETCH_LATEST );
		
		client.setServerResponse( CURRENT_RESPONSE );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( CURRENT_RESPONSE ).once();

		replay( parser2 );
		
		// run test
		
		final RequestOptions options =
			new RequestOptions( 
					DataRetrievalStrategy.DEFAULT , 
					false , // response caching disabled 
					null  );
		
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				options);

		assertNotNull( response );
		
		assertEquals( CURRENT_RESPONSE , response.getPayload() );
		assertTrue( response.isUpToDate( SYSTEM_CLOCK ) );
		
		final InternalAPIResponse cached = mockCache.get(  createQuery( testRelativeURI , params ) );
		assertNull( cached );
		
		verify( parser2 );
		verify( provider );
	}	
	
	public void testFetchLatestCacheHit() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache entry
		createMockCacheEntry( 
				createQuery( testRelativeURI , params ), 
				CURRENT_RESPONSE , 
				currentTime(), // server time
				createLaterTime( SYSTEM_CLOCK.currentTime ) // cached until
		);
		
		// setup client
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FETCH_LATEST );
		
		client.setServerResponse( CURRENT_RESPONSE );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( CURRENT_RESPONSE ).once();

		replay( parser2 );
		
		// run test
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );

		assertNotNull( response );
		assertEquals( CURRENT_RESPONSE , response.getPayload() );
		assertTrue( response.isUpToDate( SYSTEM_CLOCK ) );
		verify( parser2 );
	}	
	
	public void testFetchLatestWithError() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// setup client
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FETCH_LATEST );
		
		final IOException error =
			new IOException();
		
		client.setServerResponse( error ); // request failed
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		replay( parser2 );
		
		// run test
		try {
		client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );
		} catch(APIUnavailableException e) {
			assertSame( error , e.getCause() );
		}

		verify( parser2 );
	}	
	
	public void testFetchLatestWithFallbackOnError() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// setup client
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FETCH_LATEST_FALLBACK_CACHE);
		
		createMockCacheEntry( 
				createQuery( testRelativeURI , params ), 
				STALE_RESPONSE , 
				currentTime(), // server time
				createEarlierTime( SYSTEM_CLOCK.currentTime ) // cached until
		);		
		
		final IOException error =
			new IOException();
		
		client.setServerResponse( error ); // request failed
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( STALE_RESPONSE ).once();
		
		replay( parser2 );
		
		// run test
		final APIResponse<String> response =
		client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );
		
		assertNotNull( response );
		assertEquals( STALE_RESPONSE , response.getPayload() );
		assertFalse( response.isUpToDate( SYSTEM_CLOCK ) );		

		verify( parser2 );
	}		
	
	public void testFetchLatestStaleCacheHit() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache entry
		createMockCacheEntry( 
				createQuery( testRelativeURI , params ), 
				STALE_RESPONSE , 
				currentTime(), // server time
				createEarlierTime( SYSTEM_CLOCK.currentTime ) // cached until
		);
		
		// setup client
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FETCH_LATEST );
		
		client.setServerResponse( CURRENT_RESPONSE );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( CURRENT_RESPONSE ).once();

		replay( parser2 );
		
		// run test
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );

		assertNotNull( response );
		assertEquals( CURRENT_RESPONSE , response.getPayload() );
		assertTrue( response.isUpToDate( SYSTEM_CLOCK ) );
		verify( parser2 );
	}	
	
	public void testForcedFetchCacheHit() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache entry
		createMockCacheEntry( 
				createQuery( testRelativeURI , params ), 
				CURRENT_RESPONSE , 
				currentTime(), // server time
				createLaterTime( SYSTEM_CLOCK.currentTime ) // cached until
		);
		
		// setup client
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.FORCE_UPDATE );
		
		client.setServerResponse( CURRENT_RESPONSE2 );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( CURRENT_RESPONSE2 ).once();

		replay( parser2 );
		
		// run test
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );

		assertNotNull( response );
		assertEquals( CURRENT_RESPONSE2 , response.getPayload() );
		assertTrue( response.isUpToDate( SYSTEM_CLOCK ) );
		verify( parser2 );
	}	

	public void testFetchPreferCacheWithStaleCacheHit() throws Exception {

		SYSTEM_CLOCK.setTime("2009-05-06 11:12:13");

		final URI testRelativeURI =
			new URI("/just/a/test");		

		final Map<String,Object> params =
			new HashMap<String,Object>();
		
		// create fake cache entry
		createMockCacheEntry( 
				createQuery( testRelativeURI , params ), 
				STALE_RESPONSE , 
				currentTime(), // server time
				createEarlierTime( SYSTEM_CLOCK.currentTime ) // cached until
		);
		
		// setup client
		client.setDefaultRetrievalStrategy( DataRetrievalStrategy.PREFER_CACHE );
		
		client.setServerResponse( CURRENT_RESPONSE );
		client.setExpectedURI( BASE_URI.resolve( testRelativeURI ) );
		client.setExpectedParams( params );
		
		// setup parser
		final AbstractResponseParser<String> parser2 =
			createMockParser();

		expect( parser2.getRelativeURI() ).andReturn( testRelativeURI ).once();

		parser2.reset();
		parser2.parseHook( isA( org.w3c.dom.Document.class ) );
		expect( parser2.getResult() ).andReturn( STALE_RESPONSE ).once();

		replay( parser2 );
		
		// run test
		final APIResponse<String> response = client.sendRequest2( 
				null , 
				parser2 , 
				params , 
				KeyRole.NONE_REQUIRED , 
				RequestOptions.DEFAULT );

		assertNotNull( response );
		assertEquals( STALE_RESPONSE , response.getPayload() );
		assertFalse( response.isUpToDate( SYSTEM_CLOCK ) );
		verify( parser2 );
	}		
	// ======================= test setup helper methods ==================================
	
	private static URI toURI(String s) {
		try {
			return new URI(s);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		client = new TestClient(BASE_URI);
	}	
	
	private EveDate createLaterTime(Date input) {
		return EveDate.fromLocalTime( input , SYSTEM_CLOCK ).addMilliseconds( 10000 ); 
	}	
	
	private EveDate createEarlierTime(Date input) {
		return EveDate.fromLocalTime( input.getTime() + ( 37*24*60*60*1000 ) , SYSTEM_CLOCK ); 
	}
	
	private static Method findParserMethod(String name,Class<?>... args) throws Exception {
		try {
			return AbstractResponseParser.class.getDeclaredMethod( name , args );
		} catch (Exception e) {
			try {
				return AbstractResponseParser.class.getMethod( name , args );
			} catch(Exception e2) {

				for ( Method m : AbstractResponseParser.class.getMethods() ) {


					if ( ! m.getName().equals( name ) ) {
						System.out.println("# Name mismatch: "+m);
						continue;
					}

					if ( ! ObjectUtils.equals( m.getParameterTypes() , args ) ) {
						System.out.println("# Param mismatch: "+m);
						continue;
					}

					final int modifiers = m.getModifiers();

					if ( Modifier.isStatic( modifiers ) ||
							Modifier.isPrivate( modifiers ) ||
							Modifier.isFinal( modifiers ) )
					{
						System.out.println("# Modifier mismatch: "+m);
						continue;
					}

					return m;
				}
				throw e2;
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected AbstractResponseParser<String>  createMockParser() throws Exception {
		AbstractResponseParser<String>   mock =
		createMock( AbstractResponseParser.class ,
				findParserMethod("getRelativeURI", new Class<?>[0] ),
				findParserMethod("reset", new Class<?>[0] ),
				findParserMethod("getResult", new Class<?>[0] ),
				findParserMethod("getSystemClock", new Class<?>[0] ),
				findParserMethod("parseHook" , new Class<?>[] { org.w3c.dom.Document.class } ));
		
		expect( mock.getSystemClock() ).andReturn(SYSTEM_CLOCK ).anyTimes();
		return mock;
	}
	
	protected InternalAPIResponse createMockResponse(String response,
			EveDate serverTime,
			EveDate cachedUntil) 
	{
		final IResponseParser<?> mockParser =
			createMock( IResponseParser.class );
		
		expect( mockParser.getError() ).andReturn( null ).anyTimes();
		expect( mockParser.getCachedUntilServerTime() ).andReturn( cachedUntil ).once();
		expect( mockParser.getServerTime() ).andReturn( serverTime ).once();
		replay( mockParser );
		
		System.out.println("MockResponse{ server_time="+serverTime+" , cached_until="+cachedUntil );
		
		return new InternalAPIResponse(response, SYSTEM_CLOCK.currentTime , mockParser );
	}
	
	protected void createMockCacheEntry(APIQuery query ,
			String response,
			EveDate serverTime,
			EveDate cachedUntil) 
	{
		createMockCacheEntry( query , createMockResponse( response, serverTime , cachedUntil ) );
	}
	
	private void createMockCacheEntry(APIQuery query , InternalAPIResponse response) 
	{
		
		final IResponseCache cache =
			createNiceMock(IResponseCache.class );
		
		if ( response != null ) {
			expect( cache.get( query ) ).andReturn( response ).anyTimes();
		}
		
		replay( cache );
			
		final IResponseCacheProvider provider =
			createMock(IResponseCacheProvider.class);
		
		expect( provider.getCache( BASE_URI ) ).andReturn( cache ).anyTimes();
		replay( provider );
		
		client.setCacheProvider( provider );
	}
	
	protected APIQuery createQuery(URI relativeURI,Map<String,Object> params) {
		return new APIQuery( BASE_URI , relativeURI.toString() , params );
	}
	
	// ======================== Test classes ==================================

	public final class TestClient extends HttpAPIClient {

		private URI expectedURI;
		private Map<String,Object> expectedParams;
		private String serverResponse;
		private IOException serverError;

		public TestClient(URI baseURI) {
			super( baseURI );
		}

		public void setExpectedURI(URI expectedURI) {
			this.expectedURI = expectedURI;
		}

		public void setExpectedParams(Map<String,Object> expectedParams) {
			this.expectedParams = expectedParams;
		}

		@Override
		protected String sendRequestToServer(URI uri,
				Map<String, Object> requestParams)
		throws ClientProtocolException, IOException 
		{
			assertEquals( expectedURI , uri );
			assertEquals( this.expectedParams , requestParams );
			
			if ( serverError != null ) {
				throw serverError;
			}
			return serverResponse;
		}

		@Override
		protected ISystemClock getSystemClock() {
			return  SYSTEM_CLOCK;
		}
		
		public void setServerResponse(IOException ex) {
			this.serverError = ex;
		}

		public void setServerResponse(String resp) {
			this.serverResponse = resp;
		}
		
		public <X> APIResponse<X> sendRequest2(
				ICredentialsProvider credentials,
				IResponseParser<X> parser,
				Map<String,Object> params,
				KeyRole keyRole,
				RequestOptions options) throws IOException 
				{

			final InternalAPIResponse response = 
				super.sendRequest( credentials , parser, params, keyRole, options);

			return new APIResponse<X>( response , parser.getResult() , getSystemClock() );
		}
	}

	public static void main(String[] args) {
		List<String> ids = new ArrayList<String>();
		for ( String id : TimeZone.getAvailableIDs() ) {
			ids.add( id );
		}
		
		Collections.sort( ids );
		for ( String id : TimeZone.getAvailableIDs() ) {
			System.out.println( id );
		}
	} 
	
	public static final class MockClock implements ISystemClock {

		private Date currentTime = new Date();
		private final TimeZone timeZone;
		public MockClock() {
			timeZone = TimeZone.getTimeZone("GMT+1");
		}
		
		public void setTime(String time) {
			try {
				this.currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse( time );
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public long getCurrentTimeMillis() {
			return currentTime.getTime();
		}

		@Override
		public TimeZone getLocalTimezone() {
			return timeZone;
		}

	}
}
