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
package de.codesourcery.eve.apiclient.datamodel;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.IErrorHandler;
import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions.DataRetrievalStrategy;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * A response from the EVE Online(tm) API.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class APIResponse<T> {

	private final EveDate responseServerTime;
	private final EveDate cachedUntil;
	private final T payload;
	
	/**
	 * Create instance.
	 * 
	 * @param payload parsed server response 
	 * @see DataRetrievalStrategy#FORCE_UPDATE
	 */
	public APIResponse(InternalAPIResponse response,
			T payload,
			ISystemClock systemClock) 
	{
	
		if ( payload == null ) {
			throw new IllegalArgumentException("payload cannot be NULL");
		}
		
		if ( response == null ) {
			throw new IllegalArgumentException("NULL response ?");
		}
		
		this.responseServerTime = response.getServerTime();
		this.payload = payload;
		this.cachedUntil = response.getCachedUntilServerTime();
	}
	
	public EveDate getResponseServerTime() {
		return responseServerTime;
	}
	
	/**
	 * Returns the time until the server-side
	 * cache expires.
	 * @return
	 */
	public EveDate getCachedUntil() {
		return cachedUntil;
	}
	
	/**
	 * Checks whether this response
	 * resembles the latest possible server state.
	 * 
	 * Depending on the {@link IErrorHandler} / {@link DataRetrievalStrategy}
	 * in use , the client may return cached data
	 * instead of querying the API server. 
	 * 
	 * @return <code>true</code> if no newer data
	 * is available from the API server.
	 * 
	 * @see IErrorHandler
	 * @see IAPIClient#setDefaultErrorHandler(IErrorHandler)
	 */
	public boolean isUpToDate(ISystemClock clock) {
		if ( this.cachedUntil == null ) {
			return true;
		}
		return new EveDate( clock ).before( this.cachedUntil );
	}

	/**
	 * Returns the response's payload.
	 * 
	 * @return
	 */
	public T getPayload() {
		return payload;
	}
	
	@Override
	public String toString() {
		return "APIResponse[ response_server_time="+responseServerTime+" , cachedUntil="+cachedUntil+" , payload="+payload+" ]";
	}
}
