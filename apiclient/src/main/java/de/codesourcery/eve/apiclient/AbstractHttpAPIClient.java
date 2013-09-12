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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IErrorHandler.IJoinPoint;
import de.codesourcery.eve.apiclient.cache.DefaultCacheProvider;
import de.codesourcery.eve.apiclient.cache.IResponseCacheProvider;
import de.codesourcery.eve.apiclient.datamodel.APIError;
import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyRole;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions.DataRetrievalStrategy;
import de.codesourcery.eve.apiclient.exceptions.APIErrorException;
import de.codesourcery.eve.apiclient.exceptions.APIException;
import de.codesourcery.eve.apiclient.exceptions.APIUnavailableException;
import de.codesourcery.eve.apiclient.exceptions.InvalidCredentialsException;
import de.codesourcery.eve.apiclient.exceptions.ResponseNotCachedException;
import de.codesourcery.eve.apiclient.exceptions.ShutdownException;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.apiclient.parsers.IResponseParser;
import de.codesourcery.eve.apiclient.utils.DefaultSystemClock;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Abstract base-class for an Eve Online(tm) API client that works over HTTP.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class AbstractHttpAPIClient implements IAPIClient
{
    private static final Logger LOG = Logger.getLogger( AbstractHttpAPIClient.class );

    private final ReadWriteLock RWLOCK = new ReentrantReadWriteLock();

    /*
     * the read lock is held during a HTTP request , this results in the
     * HTTPClient being used in a multi-threaded way.
     */
    private final Lock READLOCK = RWLOCK.readLock();

    /*
     * the write lock is held while modification of this client's base URI and
     * while the client is dispose()d.
     */
    private final Lock WRITELOCK = RWLOCK.writeLock();

    private final AtomicBoolean isDisposed = new AtomicBoolean( false );

    // guarded-by: WRITELOCK
    private URI baseURI;

    // cache is assumed to be thread-safe !!
    // guarded-by: WRITELOCK
    private IResponseCacheProvider cacheProvider = new DefaultCacheProvider();

    private final Object CLIENT_LOCK = new Object();
    // guarded-by: CLIENT_LOCK
    private HttpClient httpClient;

    // guarded by: CLIENT_LOCK
    private ThreadSafeClientConnManager connectionManager;

    private IStaticDataModel daoProvider;

    // guarded-by: observers
    private final List<IAPIRequestObserver> observers =
            new ArrayList<IAPIRequestObserver>();

    private static final ISystemClock aClock = new DefaultSystemClock();

    private IResponseParserInvoker responseParserInvoker;

    protected static final URI toURI(String s)
    {
        try
        {
            return new URI( s );
        }
        catch (Exception e)
        {
            throw new RuntimeException( e );
        }
    }

    public final void setResponseParserInvoker(IResponseParserInvoker executor)
    {
        if ( executor != null )
        {
            LOG.info( "setResponseParserInvoker(): Using custom respone parser invoker "
                    + executor.getClass().getName() );
        }
        else
        {
            LOG.info( "setResponseParserInvoker(): Invoking response parser directly" );
        }
        this.responseParserInvoker = executor;
    }

    @Override
    public void addRequestObserver(IAPIRequestObserver observer)
    {
        if ( observer == null )
        {
            throw new IllegalArgumentException( "observer cannot be NULL" );
        }

        synchronized (observers)
        {
            observers.add( observer );
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "addRequestObserver(): Added observer, new count = "
                        + observers.size() );
            }
        }

    }

    protected void notifyRequestStarted(final IRequestTemplate template,
            IResponseParser<?> parser)
    {
        final List<IAPIRequestObserver> copy;
        synchronized (observers)
        {
            copy = new ArrayList<IAPIRequestObserver>( this.observers );
        }
        for (IAPIRequestObserver o : copy)
        {
            try
            {
                o.requestStarted( parser.getRelativeURI(), template.getQuery() );
            }
            catch (Exception e)
            {
                LOG.error( "notifyRequestStarted(): observer " + o + " failed: "
                        + e.getMessage(), e );
            }
        }
    }

    protected void notifyRequestFinished(final IRequestTemplate template,
            IResponseParser<?> parser)
    {
        final List<IAPIRequestObserver> copy;
        synchronized (observers)
        {
            copy = new ArrayList<IAPIRequestObserver>( this.observers );
        }
        for (IAPIRequestObserver o : copy)
        {
            try
            {
                o.requestFinished( parser.getRelativeURI(), template.getQuery(), parser
                        .getCachedUntilServerTime() );
            }
            catch (Exception e)
            {
                LOG.error( "notifyRequestFinished(): observer " + o + " failed: "
                        + e.getMessage(), e );
            }
        }
    }

    protected void notifyRequestFailed(IRequestTemplate template,
            IResponseParser<?> parser, Throwable cause)
    {
        final List<IAPIRequestObserver> copy;
        synchronized (observers)
        {
            copy = new ArrayList<IAPIRequestObserver>( this.observers );
        }
        for (IAPIRequestObserver o : copy)
        {
            try
            {
                o.requestFailed( parser.getRelativeURI(), template.getQuery(), cause );
            }
            catch (Exception e)
            {
                LOG.error( "notifyRequestFailed(): observer " + o + " failed: "
                        + e.getMessage(), e );
            }
        }
    }

    @Override
    public void removeRequestObserver(IAPIRequestObserver observer)
    {

        if ( observer == null )
        {
            throw new IllegalArgumentException( "observer cannot be NULL" );
        }
        synchronized (observers)
        {
            observers.remove( observer );
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "removeRequestObserver(): Removed observer, new count = "
                        + observers.size() );
            }
        }
    }

    @Override
    public void setStaticDataModel(IStaticDataModel daoProvider)
    {
        if ( daoProvider == null )
        {
            throw new IllegalArgumentException( "daoProvider cannot be NULL" );
        }
        this.daoProvider = daoProvider;
    }

    /**
     * Returns the current DAO provider.
     * 
     * @return DAO provider or <code>null</code> if entity references should not
     *         be resolved.
     */
    protected IStaticDataModel getStaticDataModel()
    {
        return daoProvider;
    }

    /**
     * Error handler that always fails (re-throws the exception).
     */
    protected final IErrorHandler DEFAULT_ERROR_HANDLER = new IErrorHandler() {

        @Override
        public void handleError(IAPIClient client, IJoinPoint jp, APIQuery query,
                Exception ex) throws IOException, UnparseableResponseException
        {
            rethrowException( ex );
        }
    };

    private IErrorHandler defaultErrorHandler = DEFAULT_ERROR_HANDLER;

    private final AtomicReference<DataRetrievalStrategy> defaultRetrievalMode =
            new AtomicReference<DataRetrievalStrategy>(
                    DataRetrievalStrategy.FETCH_LATEST );

    @Override
    public void setCacheProvider(IResponseCacheProvider cacheProvider)
    {

        if ( cacheProvider == null )
        {
            throw new IllegalArgumentException( "cacheProvider cannot be NULL" );
        }

        WRITELOCK.lock();
        try
        {
            this.cacheProvider = cacheProvider;
        }
        finally
        {
            WRITELOCK.unlock();
        }
    }

    public AbstractHttpAPIClient(URI baseURI) {
        if ( baseURI == null )
        {
            throw new IllegalArgumentException( "URI cannot be NULL" );
        }
        this.baseURI = baseURI;
    }

    protected ISystemClock getSystemClock()
    {
        return aClock;
    }

    /**
     * Sets the base URI (protocol,host,port etc.) for use by this client.
     * 
     * This method is thread-safe.
     * 
     * @param baseURI
     */
    public void setBaseURI(URI baseURI)
    {

        LOG.debug( "setBaseURI(): using uri: " + baseURI );

        if ( baseURI == null )
        {
            throw new IllegalArgumentException( "URI cannot be NULL" );
        }

        WRITELOCK.lock();
        try
        {
            this.baseURI = baseURI;
        }
        finally
        {
            WRITELOCK.unlock();
        }
    }

    @Override
    public final void dispose()
    {

        LOG.debug( "dispose(): Called." );

        if ( this.isDisposed.compareAndSet( false, true ) )
        {
            synchronized (CLIENT_LOCK)
            {

                if ( cacheProvider != null )
                {
                    cacheProvider.shutdown();
                }

                if ( httpClient != null )
                {
                    httpClient.getConnectionManager().shutdown();
                    httpClient = null;
                }

                disposeHook();
            }
        }
    }

    protected final void assertNotDisposed()
    {
        if ( this.isDisposed.get() )
        {
            throw new ShutdownException( "API client already destroyed." );
        }
    }

    protected void disposeHook()
    {

    }

    protected final HttpClient getClient()
    {

        synchronized (CLIENT_LOCK)
        {
            if ( httpClient == null )
            {
                LOG.debug( "getClient(): Instantiating HTTP client." );
                httpClient = createClient();
            }
        }

        return httpClient;
    }

    private ThreadSafeClientConnManager getConnectionManager()
    {
        if ( connectionManager == null )
        {
            // Create and initialize HTTP parameters
            final HttpParams params = new BasicHttpParams();
            ConnManagerParams.setMaxTotalConnections( params, 30 );

            HttpProtocolParams.setVersion( params, HttpVersion.HTTP_1_1 );

            // Create and initialize scheme registry
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register( new Scheme( "http", PlainSocketFactory
                    .getSocketFactory(), 80 ) );
            
            // setup SSL
			try {
				SSLContext sslContext = SSLContext.getInstance("SSL");

				// set up a TrustManager that trusts everything
				sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				            public X509Certificate[] getAcceptedIssuers() {
				                    return null;
				            }

				            public void checkClientTrusted(X509Certificate[] certs,
				                            String authType) {
				            }

				            public void checkServerTrusted(X509Certificate[] certs,
				                            String authType) {
				            }
				} }, new SecureRandom());

				SSLSocketFactory sf = new SSLSocketFactory(sslContext);
				Scheme httpsScheme = new Scheme("https", 443, sf);
				schemeRegistry.register(httpsScheme);				
			} 
			catch (Exception e) 
			{
				LOG.error("getConnectionManager(): Failed to setup SSL protocol for http client",e);
				throw new RuntimeException(e);
			}

            // Create an HttpClient with the ThreadSafeClientConnManager.
            // This connection manager must be used if more than one thread will
            // be using the HttpClient.
            connectionManager = new ThreadSafeClientConnManager( params, schemeRegistry );
            

        }
        return connectionManager;
    }

    protected HttpClient createClient()
    {
        return new DefaultHttpClient( getConnectionManager(), new BasicHttpParams() );
    }
    
    /**
     * Send request.
     * 
     * @param credentialsProvider
     *            credentials provider, may be <code>null</code> if this method
     *            is invoked with a key role of {@link KeyRole#NONE_REQUIRED}
     * @param parser
     *            parser that should be used to parse the response
     * @param relativePath
     *            request path, relative to {@link #setBaseURI(URI)}
     * @param requestParams
     *            the request paramters WITHOUT apiKey / userID (will be
     *            automatically added if required)
     * @param requiredKeyRole
     *            role of the key required for the request,
     *            {@link KeyRole#NONE_REQUIRED} if the request does not require
     *            authentication
     * @param errorHandler
     *            Error handler to use, <code>null</code> to use the default
     *            handler
     * @param force
     *            set to <code>true</code> to force retrieval of a fresh result,
     *            otherwise a cached response might be returned. Note that
     *            forcing a request will do nothing if the server's last
     *            response included a cachedUntil field and the indicated time
     *            has not elapsed yet.
     * @param cacheResult
     *            controls whether the server's response will be cached or not
     * @return
     * @throws IOException
     */

    protected final InternalAPIResponse sendRequest(
            ICredentialsProvider credentialsProvider, final IResponseParser<?> parser,
            Map<String, Object> requestParams, APIKey.KeyRole requiredKeyRole,
            RequestOptions requestOptions) throws APIException
    {

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "sendRequest(): Sending request with  " + " required key role: "
                    + requiredKeyRole + " , request options = " + requestOptions );
        }

        // send request to server
        long processingTime = - getSystemClock().getCurrentTimeMillis();

        READLOCK.lock();
        try
        {
            try
            {

                /*
                 * If a custom parser invoker is configured wrap the original
                 * parser instance so we can delegate the parse(Date,String)
                 * invocation.
                 */
                @SuppressWarnings("unchecked")
                final IResponseParser<?> realParser;

                if ( requestOptions.isBypassInvoker() )
                {
                    realParser = parser;
                }
                else
                {
                    realParser =
                            this.responseParserInvoker != null ? new ResponseParserWrapper(
                                    parser )
                                    : parser;
                }

                return internalSendRequest( credentialsProvider, realParser,
                    requestParams, requiredKeyRole, requestOptions );
            }
            catch (IOException e)
            {
                throw new APIUnavailableException( e );
            }
        }
        finally
        {
            READLOCK.unlock();
            processingTime += getSystemClock().getCurrentTimeMillis();

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "internalSendRequest(): processing time = " + processingTime
                        + " milliseconds" );
            }
        }
    }

    protected void rethrowException(Exception ex) throws IOException
    {
        if ( ex instanceof IOException )
        {
            throw (IOException) ex;
        }
        else if ( ex instanceof UnparseableResponseException )
        {
            throw (UnparseableResponseException) ex;
        }
        else if ( ex instanceof APIErrorException )
        {
            translateAPIErrorException( (APIErrorException) ex );
        }

        throw new RuntimeException( ex );
    }

    protected void translateAPIErrorException(APIErrorException ex)
            throws APIErrorException
    {
        final APIError error = ex.getError();
        if ( error.getErrorCode() == APIError.AUTHENTICATION_FAILURE )
        {
            throw new InvalidCredentialsException();
        }
        throw ex;
    }

    protected UrlEncodedFormEntity toFormEntity(Map<String, Object> requestParams)
            throws UnsupportedEncodingException
    {

        final List<NameValuePair> pairs = new ArrayList<NameValuePair>();

        for (Iterator<Entry<String, Object>> keys = requestParams.entrySet().iterator(); keys
                .hasNext();)
        {
            final Entry<String, Object> e = keys.next();
            final String key = e.getKey();
            pairs.add( new BasicNameValuePair( key, e.getValue().toString() ) );
        }

        return new UrlEncodedFormEntity( pairs );
    }

    protected interface IRequestTemplate
    {

        public InternalAPIResponse execute() throws Exception;

        public APIQuery getQuery();
    }

    protected InternalAPIResponse executeRequestTemplate(final IResponseParser<?> parser,
            final RequestOptions requestOptions, final IRequestTemplate template)
            throws IOException
    {

        // array used here because anonymous inner classes cannot assign to
        // non-final local variables
        final InternalAPIResponse[] result = new InternalAPIResponse[] { null };

        final IErrorHandler errorHandler =
                requestOptions.getErrorHandler( this.defaultErrorHandler );

        final IJoinPoint jp = new IJoinPoint() {

            @Override
            public void retry() throws Exception
            {
                try
                {
                    notifyRequestStarted( template, parser );
                    result[0] = template.execute();
                    notifyRequestFinished( template, parser );
                }
                catch (Exception e)
                {
                    LOG.error( "executeRequestTemplate(): (retry) Caught ", e );

                    errorHandler.handleError( AbstractHttpAPIClient.this, this, template
                            .getQuery(), e );

                    if ( result[0] == null )
                    {

                        notifyRequestFailed( template, parser, e );

                        rethrowException( e ); // always fails
                        throw new RuntimeException( "Unreachable code reached" );
                    }

                    notifyRequestFinished( template, parser );
                }
            }
        };

        try
        {
            notifyRequestStarted( template, parser );

            result[0] = template.execute();

            notifyRequestFinished( template, parser );
        }
        catch (Exception e)
        {

            LOG.error( "executeRequestTemplate(): Caught ", e );

            DataRetrievalStrategy strategy = requestOptions.getDataRetrievalStrategy();

            if ( strategy == DataRetrievalStrategy.DEFAULT )
            {
                strategy = defaultRetrievalMode.get();
            }

            if ( strategy == DataRetrievalStrategy.FETCH_LATEST_FALLBACK_CACHE )
            {

                final InternalAPIResponse response =
                        getCachedResult( template.getQuery() );

                if ( response != null )
                {
                    final InternalAPIResponse apiResponse =
                            parseResponse( parser, response );

                    notifyRequestFinished( template, parser );

                    return apiResponse;
                }
                else
                {
                    LOG
                            .info( "executeRequestTemplate(): Cannot fall-back to cache, response not cached." );
                }
            }

            notifyRequestFailed( template, parser, e );

            try
            {
                errorHandler.handleError( this, jp, template.getQuery(), e );
            }
            catch (Exception e2)
            {
                rethrowException( e2 ); // always rethrows
            }

            if ( result[0] == null )
            {
                rethrowException( e ); // always rethrows
                throw new RuntimeException( "Unreachable code reached" );
            }
        }

        // should never happen...
        if ( result[0] == null )
        {
            throw new RuntimeException( "Internal error , NULL result ?" );
        }

        return result[0];
    }

    protected InternalAPIResponse parseResponse(IResponseParser<?> parser,
            InternalAPIResponse resp)
    {
        parser.reset();

        parser.parse( resp.getTimestamp(), resp.getPayload() );
        return resp;
    }

    private final class ResponseParserWrapper<T> implements IResponseParser<T>
    {

        public final IResponseParser<T> parser;

        public ResponseParserWrapper(IResponseParser<T> parser) {
            this.parser = parser;
        }

        @Override
        public int getAPIVersion() throws IllegalStateException
        {
            return parser.getAPIVersion();
        }

        @Override
        public EveDate getCachedUntilServerTime() throws IllegalStateException
        {
            return parser.getCachedUntilServerTime();
        }

        @Override
        public APIError getError() throws IllegalStateException
        {
            return parser.getError();
        }

        @Override
        public URI getRelativeURI()
        {
            return parser.getRelativeURI();
        }

        @Override
        public T getResult() throws IllegalStateException
        {
            return parser.getResult();
        }

        @Override
        public EveDate getServerTime() throws IllegalStateException
        {
            return parser.getServerTime();
        }

        @Override
        public InternalAPIResponse parse(Date responseTimestamp, String xml)
                throws UnparseableResponseException
        {
            return responseParserInvoker.runParser( this.parser, responseTimestamp, xml );
        }

        @Override
        public void reset()
        {
            parser.reset();
        }

    }

    /**
     * Method may be overridden by subclasses but must NEVER be invoked directly
     * (thread-safety is at stake).
     * 
     * @param credentialsProvider
     * @param parser
     * @param relativePath
     * @param requestParams
     * @param requiredKeyType
     * @param eh
     * @param force
     * @return
     * @throws IOException
     * @deprecated For subclassing only. Do not invoke directly.
     */
    @Deprecated
    protected InternalAPIResponse internalSendRequest(
            final ICredentialsProvider credentialsProvider,
            final IResponseParser<?> parser, final Map<String, Object> params,
            final APIKey.KeyRole requiredKeyRole, final RequestOptions requestOptions)
            throws IOException
    {

        // check preconditions
        assertNotDisposed();

        if ( requiredKeyRole == null )
        {
            throw new IllegalArgumentException( "key role cannot be NULL" );
        }

        if ( params == null )
        {
            throw new IllegalArgumentException( "request params cannot be NULL" );
        }

        if ( parser == null )
        {
            throw new IllegalArgumentException( "parser cannot be NULL" );
        }

        if ( requestOptions == null )
        {
            throw new IllegalArgumentException( "request options cannot be NULL" );
        }

        final DataRetrievalStrategy retrievalStrategy;
        if ( requestOptions.hasDataRetrievalStrategy( DataRetrievalStrategy.DEFAULT ) )
        {
            retrievalStrategy = this.defaultRetrievalMode.get();
        }
        else
        {
            retrievalStrategy = requestOptions.getDataRetrievalStrategy();
        }

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "internalSendRequest(): using retrieval strategy "
                    + retrievalStrategy );
        }

        // compose request URI
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "internalSendRequest(): Using parser = "
                    + parser.getClass().getName() );
        }

        final URI relativeURI = parser.getRelativeURI();
        final URI uri = baseURI.resolve( relativeURI );
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "internalSendRequest(): URI = " + uri );
        }

        // create request POST parameters
        final Map<String, Object> requestParams = new HashMap<String, Object>( params );

        addCredentialsToRequestParams( credentialsProvider, requiredKeyRole,
            requestParams );

        // check if a cached response exists
        final APIQuery query =
                new APIQuery( baseURI, relativeURI.toString(), requestParams );

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "internalSendRequest(): query = " + query );
        }

        final InternalAPIResponse cached = getCachedResult( query );

        if ( retrievalStrategy == DataRetrievalStrategy.OFFLINE )
        {
            if ( cached != null )
            {
                LOG
                        .debug( "internalSendRequest(): Returning cached result (offline mode)" );
                return parseResponse( parser, cached );
            }
            LOG.error( "internalSendRequest(): Response not cached (offline mode)" );
            throw new ResponseNotCachedException();
        }

        if ( cached != null )
        {

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "internalSendRequest(): Found cached response : " + cached );
            }

            final boolean canQueryAgain = cached.mayBeRequestedAgain( getSystemClock() );

            if ( retrievalStrategy == DataRetrievalStrategy.PREFER_CACHE )
            {
                if ( LOG.isDebugEnabled() )
                {
                    LOG
                            .debug( "internalSendRequest(): Returning cached result (prefer cache) [ stale = "
                                    + canQueryAgain + " ]" );
                }
                return parseResponse( parser, cached );
            }

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "internalSendRequest(): canQueryAgain = " + canQueryAgain
                        + " / cached_until_server_time= "
                        + cached.getCachedUntilServerTime() );
            }

            if ( ! canQueryAgain
                    && retrievalStrategy != DataRetrievalStrategy.FORCE_UPDATE )
            {
                LOG.debug( "internalSendRequest(): Returning cached response." );
                return parseResponse( parser, cached );
            }
        }

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "internalSendRequest(): request params = " + requestParams );
        }

        final IRequestTemplate template = new IRequestTemplate() {

            @Override
            public InternalAPIResponse execute() throws Exception
            {

                LOG.debug( "internalSendRequest(): Sending request to server." );

                final String body = sendRequestToServer( uri, requestParams );

                final Date responseTimestamp =
                        new Date( getSystemClock().getCurrentTimeMillis() );

                LOG.debug( "internalSendRequest(): Received response from server" );

                /*
                 * We might actually retry a previously failed parsing
                 * attempt,make sure the parser's internal state is reset
                 * beforehand.
                 */
                parser.reset();

                return parser.parse( responseTimestamp, body );
            }

            @Override
            public APIQuery getQuery()
            {
                return query;
            }
        };

        final InternalAPIResponse response =
                executeRequestTemplate( parser, requestOptions, template );

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "internalSendRequest(): API version = " + parser.getAPIVersion()
                    + " , timestamp = " + response.getTimestamp() + " , server time ="
                    + parser.getServerTime() + " , cached_until = "
                    + parser.getCachedUntilServerTime() );
        }

        /*
         * Store response in cache.
         * 
         * In an error occured , the error handler might have returned an
         * already cached response , do not store it again in this case.
         */
        if ( requestOptions.isResponseCacheable() )
        {
            if ( response != cached )
            {
                LOG.debug( "internalSendRequest(): storing response in cache." );
                storeResponse( query, response );
            }
            else
            {
                LOG
                        .debug( "internalSendRequest(): response not stored in cache (already cached)" );
            }

        }
        else if ( LOG.isDebugEnabled() )
        {
            LOG
                    .debug( "internalSendRequest(): response not stored in cache (not cacheable)" );
        }

        return response;
    }

    protected String sendRequestToServer(URI uri, Map<String, Object> requestParams)
            throws ClientProtocolException, IOException
    {
        final HttpPost request = new HttpPost( uri );
        request.setEntity( toFormEntity( requestParams ) );

        return getClient().execute( request, new BasicResponseHandler() );
    }

    private void addCredentialsToRequestParams(ICredentialsProvider credentialsProvider,
            APIKey.KeyRole requiredKeyRole, final Map<String, Object> requestParams)
    {
        if ( requiredKeyRole != KeyRole.NONE_REQUIRED )
        {

            if ( credentialsProvider == null )
            {
                throw new IllegalArgumentException(
                        "Credentials provider cannot be NULL with key role "
                                + requiredKeyRole );
            }

            // add appropriate credentials to request parameters
            
            // Eve API v1
//            final String userIdParam = "userID";
//            final String apiKeyParam = "apiKey";
            
            // Eve API v2
            final String userIdParam = "keyID";
            final String apiKeyParam = "vCode";
            
            requestParams.put( userIdParam , credentialsProvider.getUserId() );
            final APIKey apiKey = credentialsProvider.getKeyForRole( requiredKeyRole );
            requestParams.put( apiKeyParam , apiKey.getValue() );
        }
        else
        {
            LOG.debug( "internalSendRequest(): No API key required." );
        }
    }

    private void storeResponse(APIQuery query, InternalAPIResponse response)
    {
        cacheProvider.getCache( this.baseURI ).put( query, response );
    }

    private InternalAPIResponse getCachedResult(APIQuery query)
    {
        return cacheProvider.getCache( this.baseURI ).get( query );
    }

    protected final void addCharacterId(Map<String, Object> requestParams,
            CharacterID characterId)
    {
        if ( characterId == null )
        {
            throw new IllegalArgumentException( "NULL character ID ?" );
        }
        requestParams.put( "characterID", characterId.getValue() );
    }

    /**
     * NOT thread-safe.
     * 
     * @see #DEFAULT_ERROR_HANDLER
     */
    @Override
    public void setDefaultErrorHandler(IErrorHandler errorHandler)
    {
        if ( errorHandler == null )
        {
            throw new IllegalArgumentException( "default error handler cannot be NULL" );
        }
        this.defaultErrorHandler = errorHandler;
    }

    @Override
    public void setDefaultRetrievalStrategy(DataRetrievalStrategy defaultRetrievalMode)
    {
        if ( defaultRetrievalMode == null )
        {
            throw new IllegalArgumentException( "defaultRetrievalMode cannot be NULL" );
        }

        LOG.info( "setDefaultRetrievalStrategy(): default strategy = "
                + defaultRetrievalMode );

        if ( defaultRetrievalMode == DataRetrievalStrategy.DEFAULT )
        {
            this.defaultRetrievalMode.set( DataRetrievalStrategy.FETCH_LATEST );
        }
        else
        {
            this.defaultRetrievalMode.set( defaultRetrievalMode );
        }
    }

}
