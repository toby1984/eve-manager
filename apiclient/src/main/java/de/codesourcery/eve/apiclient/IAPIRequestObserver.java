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

import java.net.URI;

import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.skills.utils.EveDate;

/**
 * Observer for EVE Online(tm) API requests.
 *  
 * This observer is ONLY invoked when a request
 * is NOT served from the cache. 
 * @author tobias.gierke@code-sourcery.de
 * @see IAPIClient#addRequestObserver(IAPIRequestObserver)
 */
public interface IAPIRequestObserver {

	public void requestStarted(URI baseURI , APIQuery query );
	
	public void requestFailed(URI baseURI , APIQuery query , Throwable exception);
	
	/**
	 * 
	 * @param baseURI
	 * @param query
	 * @param cachedUntilLocalTime LOCAL Time when the response expires, may be <code>null</code>
	 * if the expiry date is unknown for some reason
	 */
	public void requestFinished(URI baseURI , APIQuery query,EveDate cachedUntilLocalTime);
}
