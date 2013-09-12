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
import de.codesourcery.eve.apiclient.IResponseParserInvoker;
import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.exceptions.ResponseNotCachedException;

/**
 * Per-request options that control client behaviour.
 * 
 * @author tobias.gierke@code-sourcery.de
 * @see IAPIClient#setDefaultRetrievalStrategy(DataRetrievalStrategy)
 */
public class RequestOptions
{

    /**
     * Default request options.
     * 
     * Retrieval mode is set to {@link DataRetrievalStrategy#FETCH_LATEST} ,
     * responses may be cached and the default error handler is in use.
     */
    public static final RequestOptions DEFAULT =
            new RequestOptions( DataRetrievalStrategy.DEFAULT, true, null );

    /**
     * Controls cache handling for API server requests.
     * 
     * @author tobias.gierke@code-sourcery.de
     * @see APIResponse#isUpToDate()
     * @see IAPIClient#setDefaultRetrievalStrategy(DataRetrievalStrategy)
     */
    public enum DataRetrievalStrategy
    {
        /**
         * Requests will only be answered from the cache. If a cache-miss occurs
         * , a {@link ResponseNotCachedException} will be thrown.
         */
        OFFLINE,
        /**
         * Requests will be answered from the cache as long as no update may be
         * fetched from the server ( serverTime < cachedUntil ). If the
         * cachedUntil date indicates an update from the server <b>might</b> be
         * available, the server will be queried.
         * 
         * @see InternalAPIResponse#mayBeQueriedAgain(de.codesourcery.eve.skills.util.ISystemClock)
         */
        FETCH_LATEST,
        /**
         * Like {@link #FETCH_LATEST} but if a server / parsing error occurs and
         * the cache already holds a response for the current request, the
         * request is served from the cache instead.
         */
        FETCH_LATEST_FALLBACK_CACHE,
        /**
         * Queries the server even if a cached response exists and its
         * cachedUntil timestamp indicates that no newer data is available from
         * the server.
         * 
         * Note that some EVE Online(tm) API calls seem to return an error when
         * queried again before the caching interval expires.
         * 
         * @see APIResponse#isUpToDate()
         */
        FORCE_UPDATE,
        /**
         * Requests will be answered from the cache even if the cachedUntil
         * timestamp suggests an update might be available from the server. If a
         * cache-miss occurs the server is queried.
         */
        PREFER_CACHE,
        /**
         * Use the retrieval mode that is configured as client-wide default.
         * 
         * The default retrieval strategy may be changed using
         * {@link IAPIClient#setDefaultRetrievalMode(DataRetrievalStrategy)}.
         */
        DEFAULT;
    }

    private final DataRetrievalStrategy retrievalStrategy;
    private final IErrorHandler errorHandler;
    private final boolean isResponseCacheable;
    private final boolean isBypassInvoker;

    /**
     * (returns altered instance!) Sets whether the request should bypass any
     * currently configured {@link IResponseParserInvoker}.
     * 
     * @param yesNo
     *            Pass <code>true</code> if the request should be executed
     *            directly.
     * @return
     */
    public RequestOptions withBypassInvoker(boolean yesNo)
    {
        return new RequestOptions( this.retrievalStrategy, this.isResponseCacheable,
                yesNo, errorHandler );
    }

    /**
     * Returns the retrieval strategy to use for the current request.
     * 
     * @return
     */
    public DataRetrievalStrategy getDataRetrievalStrategy()
    {
        return retrievalStrategy;
    }

    @Override
    public String toString()
    {
        return "RequestOptions[ data_retrieval_strategy = " + retrievalStrategy
                + " , response_cacheable = " + isResponseCacheable + " , error_handler="
                + errorHandler + " ]";
    }

    /**
     * Returns the error handler to use for the current request.
     * 
     * @return error handler or <code>null</code> if the default error handler
     *         should be used.
     */
    public IErrorHandler getErrorHandler()
    {
        return errorHandler;
    }

    /**
     * Returns whether the server's response may be cached by the client.
     * 
     * @return <code>true</code> if the client may cache the current requests
     *         response.
     */
    public boolean isResponseCacheable()
    {
        return isResponseCacheable;
    }

    /**
     * Create request options.
     * 
     * @param retrievalStrategy
     *            Data retrieval strategy to use, never <code>null</code>
     * @param isResponseCacheable
     * @param errorHandler
     *            error handler to use, <code>null</code> if the client's
     *            default error handler should be used
     */
    public RequestOptions(DataRetrievalStrategy retrievalStrategy,
            boolean isResponseCacheable, IErrorHandler errorHandler) {
        this( retrievalStrategy, isResponseCacheable, false, errorHandler );
    }

    /**
     * Create request options.
     * 
     * @param retrievalStrategy
     *            Data retrieval strategy to use, never <code>null</code>
     * @param isResponseCacheable
     *            Whether the response may be cached
     * @param isBypassInvoker
     *            Whether this request should not be passed to any configured
     *            {@link IResponseParserInvoker} but be executed directly
     *            instead.
     * @param errorHandler
     *            error handler to use, <code>null</code> if the client's
     *            default error handler should be used
     */
    public RequestOptions(DataRetrievalStrategy retrievalStrategy,
            boolean isResponseCacheable, boolean isBypassInvoker,
            IErrorHandler errorHandler) {
        if ( retrievalStrategy == null )
        {
            throw new IllegalArgumentException( "data retrieval strategy cannot be NULL" );
        }
        this.retrievalStrategy = retrievalStrategy;
        this.isResponseCacheable = isResponseCacheable;
        this.errorHandler = errorHandler;
        this.isBypassInvoker = isBypassInvoker;
    }

    /**
     * Controls whether this request should bypass any custom
     * {@link IResponseParserInvoker} that is currently configured on the
     * client.
     * 
     * Default behaviour is to always execute requests using the configured
     * invoker.
     * 
     * @return
     * @see IAPIClient#setResponseParserInvoker(de.codesourcery.eve.apiclient.IResponseParserInvoker)
     */
    public boolean isBypassInvoker()
    {
        return isBypassInvoker;
    }

    /**
     * Create request options.
     * 
     * Response will be cached, default error handler is in effect.
     * 
     * @param retrievalStrategy
     *            Data retrieval strategy to use, never <code>null</code>
     */
    public RequestOptions(DataRetrievalStrategy retrievalStrategy) {
        this( retrievalStrategy, true, null );
    }

    /**
     * Returns whether this request options instance has a given retrieval
     * strategy.
     * 
     * @param strategy
     * @return
     */
    public boolean hasDataRetrievalStrategy(DataRetrievalStrategy strategy)
    {
        return this.retrievalStrategy == strategy;
    }

    public IErrorHandler getErrorHandler(IErrorHandler defaultHandler)
    {
        return this.errorHandler == null ? defaultHandler : this.errorHandler;
    }
}
