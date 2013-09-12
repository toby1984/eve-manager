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

import java.net.URI;
import java.util.Date;

import de.codesourcery.eve.apiclient.HttpAPIClient;
import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIError;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.utils.EveDate;

/**
 * Parses XML the EVE Online(tm) API returned.
 * 
 * Parser implementors will most likely want to subclass 
 * {@link AbstractResponseParser} instead of
 * implementing the whole interface themselves.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IResponseParser<T> {
	
	/**
	 * Returns the relative URI to use
	 * when querying the API server.
	 * 
	 * The absolute URI is calculated by adding
	 * this relative URI to {@link HttpAPIClient#setBaseURI(java.net.URI)}. 
	 * @return
	 */
	public URI getRelativeURI();
	
	/**
	 * Parse XML.
	 * 
	 * @param responseTimestamp Time when this response was received (LOCAL time)
	 * @param xml
	 * @throws UnparseableResponseException
	 * @throws IllegalStateException if a call to this
	 * method already returned successfully.
	 * @throws UnparseableResponseException
	 */
	public InternalAPIResponse parse(Date responseTimestamp , String xml) throws UnparseableResponseException;
	
	/**
	 * Returns the API's XML version as returned
	 * from the server.
	 * @return
	 * @throws IllegalStateException if called before {@link IResponseParser#parse(String)} has been invoked
	 * on this parser
	 */
	public int getAPIVersion() throws IllegalStateException;

	/**
	 * Returns the time on the server.
	 * @return server time as unix timestamp
	 * @throws IllegalStateException if called before {@link IResponseParser#parse(String)} has been invoked
	 * on this parser	 
	 */
	public EveDate getServerTime() throws IllegalStateException;
	
	/**
	 * Returns the server time the
	 * server will keep on returning the
	 * same result for the request.
	 * 
	 * @return server time as unix timestamp
	 * @throws IllegalStateException if called before {@link IResponseParser#parse(String)} has been invoked
	 * on this parser	 
	 */
	public EveDate getCachedUntilServerTime() throws IllegalStateException;
	
	/**
	 * Returns any API error the server may have returned.
	 * @return API error or <code>null</code> if no error was reported
	 * @throws IllegalStateException if called before {@link IResponseParser#parse(String)} has been invoked
	 * on this parser		 
	 */
	public APIError getError() throws IllegalStateException;
	
	/**
	 * Resets the parser's internal state.
	 * This method gets invoked each time before 
	 * {@link #parse(Date, String)} is called so
	 * it should execute as quickly as possibly.
	 */
	public void reset();
	
	/**
	 * Returns the parser's result.
	 * @return
	 * @throws IllegalStateException if called before {@link IResponseParser#parse(String)} has been invoked
	 * on this parser	 	 
	 */
	public T getResult() throws IllegalStateException;
}
