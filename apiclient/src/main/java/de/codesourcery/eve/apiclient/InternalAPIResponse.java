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
package de.codesourcery.eve.apiclient;

import java.util.Date;

import de.codesourcery.eve.apiclient.parsers.IResponseParser;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;


/**
 * Encapsulates an EVE API client response (immutable).
 *
 * Must be immutable , stored in a cache.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class InternalAPIResponse {

	private final String payload;
	private final Date timestamp;
	
	/**
	 * The server time as returned
	 * from the server.
	 */
	private final EveDate serverTime;
	/**
	 * The server time until
	 * this response data 
	 * is cached by the server 
	 * (all queries before
	 * this time will just return the 
	 * same result so it's no 
	 * use trying).
	 */
	private final EveDate cachedUntilServerTime;
	
	/**
	 * INTERNAL USE ONLY.
	 * 
	 * @param payload
	 * @param timestamp
	 * @param serverTime
	 * @param cachedUntil
	 */
	public InternalAPIResponse(String payload, Date timestamp,EveDate serverTime,EveDate cachedUntil) {
		this.payload = payload;
		this.timestamp = timestamp;
		this.serverTime = serverTime;
		this.cachedUntilServerTime = cachedUntil;
	}

	/**
	 * Create instance.
	 * 
	 * @param payload the payload, never <code>null</code>
	 * @param timestamp the timestamp (local time) when this response was received
	 * @param parser the parser that will be queried to retrieve response data. The parser 
	 * must <b>NOT</b> report an API error !
	 * @throws  IllegalArgumentException if <code>payload</code> is <code>null</code>
	 * or the parser reported an error
	 * 
	 * @see IResponseParser#getError()
	 */
	public InternalAPIResponse(String payload, Date timestamp, IResponseParser<?> parser) 
	{
		
		if ( payload == null ) {
			throw new IllegalArgumentException("payload cannot be NULL");
		}
		
		if ( parser.getError() != null ) {
			throw new IllegalArgumentException("Internal error - parser " +
					"reported error "+parser.getError()+
					" , refusing to construct InternalAPIResponse");
		}
		
		this.payload = payload;
		this.timestamp = timestamp;
		this.serverTime = parser.getServerTime();
		this.cachedUntilServerTime = parser.getCachedUntilServerTime();
	}
	
	/**
	 * Returns payload size in bytes.
	 * 
	 * @return
	 */
	public int getPayloadSize() {
		return payload.length()*2;
	}
	
	private long toUnixTimestamp(Date d) {
		return d != null ? d.getTime() : 0;
	}
	
	/**
	 * Returns this response's age in seconds.
	 * 
	 * @param systemClock
	 * @return
	 */
	public int getAgeInSeconds(ISystemClock systemClock) {
		return (int) ( ( systemClock.getCurrentTimeMillis() - toUnixTimestamp( timestamp) ) / 1000.0f );
	}
	
	/**
	 * Compares the payload size of this
	 * response with another.
	 * 
	 * @param other
	 * @return
	 */
	public boolean hasLargerPayloadThan(InternalAPIResponse other) {
		return this.getPayload().length() > other.getPayload().length();
	}

	/**
	 * Returns whether the API request that
	 * created this response may be issued again.
	 * 
	 * @param clock system clock
	 * @return true if current serverTime > cachedUntil 
	 */
	public boolean mayBeRequestedAgain(ISystemClock clock) {
		
		if ( getServerTime() == null || 
			 getCachedUntilServerTime() == null ) 
		{
			return true;
		}
		
		return this.cachedUntilServerTime.before( new EveDate( clock ) );
	}
	
	/**
	 * Returns whether this response
	 * holds the latest available data
	 * from the server.
	 *  
	 * @param now
	 * @return <code>true/<code> if no newer data
	 * is available from the server
	 */
	public boolean isUpToDate(ISystemClock now) {
		return ! mayBeRequestedAgain(now);
	}
	
	/**
	 * Returns the server response's payload (XML).
	 * 
	 * @return
	 */
	public String getPayload() {
		return payload;
	}

	/**
	 * Returns the timestamp when this response
	 * was received in LOCAL time.
	 * 
	 * @return receive timestamp in LOCAL time.
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns the time
	 * when the query that resulted in 
	 * this response may be issued again.
	 * 
	 * @return server time, may be <code>0</code>
	 */
	public EveDate getCachedUntilServerTime() {
		return cachedUntilServerTime;
	}

	/**
	 * Returns the time as returned by the server.
	 * 
	 * @return server time, may be 0.
	 */
	public EveDate getServerTime() {
		return serverTime;
	}

}
