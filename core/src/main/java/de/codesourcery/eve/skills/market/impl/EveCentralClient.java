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
package de.codesourcery.eve.skills.market.impl;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.util.IStatusCallback;
import de.codesourcery.eve.skills.util.StatusCallbackHelper;
import de.codesourcery.eve.skills.util.IStatusCallback.MessageType;

public class EveCentralClient implements IEveCentralClient
{

	private static final Logger log = Logger.getLogger(EveCentralClient.class);

	// http://api.eve-central.com/api/marketstat
	private static final URI MARKET_STATS_URI = URI.create( "api/marketstat" );

	private final ReentrantReadWriteLock readWriteLock = 
		new ReentrantReadWriteLock();

	private final Lock readLock  = readWriteLock.readLock();
	private final Lock writeLock = readWriteLock.writeLock();

	private final StatusCallbackHelper callbackHelper =
		new StatusCallbackHelper();

	// guarded-by: readLock / writeLock
	private HttpClient client;

	private ThreadSafeClientConnManager connectionManager;
	private URI serverURI = URI.create( "http://api.eve-central.com/" );

	public void setServerURI(String serverURI) {
		if ( serverURI == null ) {
			throw new IllegalArgumentException("server URI cannot be NULL");
		}
		this.serverURI = URI.create( serverURI );
	}

	public void dispose() 
	{
		writeLock.lock();
		try
		{
			if ( this.client != null ) 
			{
				this.client.getConnectionManager().shutdown();
				this.client = null;
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Must <b>only</b> be called while {@link #CLIENT_LOCK}
	 * is being held.
	 * 
	 * @return
	 */
	protected ThreadSafeClientConnManager getConnectionManager() 
	{

		if ( connectionManager == null ) 
		{
			// Create and initialize HTTP parameters
			final HttpParams params = new BasicHttpParams();
			ConnManagerParams.setMaxTotalConnections(params, 30);

			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

			// Create and initialize scheme registry 
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register( new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

			// Create an HttpClient with the ThreadSafeClientConnManager.
			// This connection manager must be used if more than one thread will
			// be using the HttpClient.
			connectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);
		}
		return connectionManager;
	}

	protected HttpClient createClient() {
		return new DefaultHttpClient( getConnectionManager() , new BasicHttpParams() );
	}

	protected final HttpClient getClient() 
	{
		writeLock.lock();
		try {
			if ( client == null ) {
				client = createClient();
			}
			return client;
		} finally {
			writeLock.unlock();
		}
	}

	public String sendRequestToServer(List<NameValuePair> requestParams) {
		return sendRequestToServer(MARKET_STATS_URI, requestParams );
	}

	protected String sendRequestToServer(URI relativeURI ,List<NameValuePair> requestParams) 
	{

		System.out.println("Querying EVE-central ( params: "+requestParams+" )");

		final StringBuffer uriString = new StringBuffer( relativeURI.toString() );

		if ( ! requestParams.isEmpty() ) {
			uriString.append("?");
		}

		for ( Iterator<NameValuePair> it =requestParams.iterator()
				; it.hasNext() ; ) 
		{
			final NameValuePair entry = it.next();
			// TODO: NO URL encoding is done here....
			uriString.append( entry.getName() ).append("=").append( entry.getValue() );
			if ( it.hasNext() ) {
				uriString.append("&");
			}
		}

		final URI fullURI = serverURI.resolve( uriString.toString() );

		String result;
		try {
			notifyStatusCallback(MessageType.INFO , 
			"Fetching prices from eve-central ...");

			System.out.println("USING URI: "+fullURI);
			result = sendRequest(fullURI);

			notifyStatusCallback(MessageType.INFO , 
					"Received prices from eve-central");
			return result;
		} catch(RuntimeException e) {
			notifyStatusCallback(MessageType.ERROR , 
					"Failed to retrieve data from eve-central ("+e.getMessage()+")");
			throw e;
		}
	}

	protected String sendRequest(final URI fullURI) {

		log.info("sendRequestToServer(): Sending request to server, URI = "+fullURI);

		final HttpClient theClient = getClient(); // getClient() calls writeLock.lock() !!!

		/*
		 * Do NOT hold any lock while invoking getClient() , 
		 * the method tries to aquire to write lock.
		 */
		readLock.lock();
		try {
			final HttpGet request = new HttpGet( fullURI );
			String result;
			try {

				result = theClient.execute( request , new BasicResponseHandler() );
			} 
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			log.info("sendRequestToServer(): Response received.");
			if ( log.isTraceEnabled() ) {
				log.trace("sendRequestToServer(): response = "+result);
			}
			return result;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void addStatusCallback(IStatusCallback callback) {
		callbackHelper.addStatusCallback(callback);
	}

	@Override
	public void removeStatusCallback(IStatusCallback callback) {
		callbackHelper.removeStatusCallback(callback);
	}

	protected void notifyStatusCallback(MessageType type, String message) {
		callbackHelper.notifyStatusCallbacks( type , message);
	}
}
