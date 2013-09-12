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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.codesourcery.eve.apiclient.cache.DefaultCacheProvider;
import de.codesourcery.eve.apiclient.cache.IResponseCacheProvider;
import de.codesourcery.eve.apiclient.cache.InMemoryResponseCache;
import de.codesourcery.eve.apiclient.cache.NoCacheProvider;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.ServerStatus;
import de.codesourcery.eve.apiclient.datamodel.SkillInTraining;
import de.codesourcery.eve.apiclient.datamodel.SkillQueueEntry;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions.DataRetrievalStrategy;
import de.codesourcery.eve.apiclient.exceptions.APIErrorException;
import de.codesourcery.eve.apiclient.exceptions.APIUnavailableException;
import de.codesourcery.eve.apiclient.parsers.IResponseParser;
import de.codesourcery.eve.apiclient.parsers.ConquerableStationsParser.Outpost;
import de.codesourcery.eve.apiclient.utils.SwingEDTParserInvoker;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.NPCCorpStandings;
import de.codesourcery.eve.skills.datamodel.FactionStandings;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.IndustryJob;
import de.codesourcery.eve.skills.datamodel.MarketOrder;
import de.codesourcery.eve.skills.datamodel.MarketTransaction;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.datamodel.TransactionType;

/**
 * EVE Online API client.
 * 
 * Implementations must provide thread-safety as documented for each method.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IAPIClient
{

    /**
     * Entity type for resolving IDs from names or vice versa.
     * 
     * @author tobias.gierke@code-sourcery.de
     * @see IAPIClient#resolveNames(String[], RequestOptions)
     */
    public static enum EntityType
    {
        CORPORATION, CHARACTER, OUTPOST;
    }

    /**
     * Sets the cache provider to use.
     * 
     * @param cacheProvider
     *            cache provider , never <code>null</code>
     * @see InMemoryResponseCache
     * @see NoCacheProvider
     * @see DefaultCacheProvider
     */
    public void setCacheProvider(IResponseCacheProvider cacheProvider);

    /**
     * Sets the executor used to invoke the
     * {@link IResponseParser#parse(java.util.Date, String)} method.
     * 
     * Setting an invoker explicitly should be rarely necessary and was
     * originally implemented to confine execution of the parsers to a single
     * thread to fix issues with Hibernate in a multi-threaded swing
     * application.
     * 
     * @param executor
     *            The executor to use or <code>null</code> to invoke the parser
     *            from the calling thread.
     * 
     * @see SwingEDTParserInvoker
     */
    public void setResponseParserInvoker(IResponseParserInvoker executor);

    /**
     * Sets the data model to be used for retrieving entities from the static
     * EVE database export.
     * 
     * <pre>
	 * Some API requests yield responses that contain 
	 * references to entities that are part of 
	 * the static EVE database export and cannot be 
	 * queried using the API itself. 
	 * 
	 * It's ok to set no / a <code>null</code> data model , the
	 * client then will simply not try to resolve references
	 * to static data. 
	 * </pre>
     * 
     * @param daoProvider
     *            DAO provider to use, may be <code>null</code>
     */
    public void setStaticDataModel(IStaticDataModel daoProvider);

    /**
     * Disposes this client instance.
     * 
     * This method releases any required resources. After this method returns
     * the client is no longer in a usable state. This method must be
     * implemented in a thread-safe way.
     */
    public void dispose();

    /**
     * Sets this client's default error handler.
     * 
     * Depending on the client implementation this method may not be
     * thread-safe.
     * 
     * @param errorHandler
     */
    public void setDefaultErrorHandler(IErrorHandler errorHandler);

    /**
     * Sets this client's default data retrieval strategy.
     * 
     * @param defaultRetrievalStrategy
     *            data retrieval strategy to use
     */
    public void setDefaultRetrievalStrategy(DataRetrievalStrategy defaultRetrievalStrategy);

    /**
     * Registers an API request observer that is notified whenever an API
     * request is sent to/received from the EVE Online(tm) API server. The
     * observer is NOT invoked when a request is served from the cache.
     * 
     * @param observer
     * 
     * @see #removeRequestObserver(IAPIRequestObserver)
     * @see #setCacheProvider(IResponseCacheProvider)
     */
    public void addRequestObserver(IAPIRequestObserver observer);

    /**
     * Removes an API request observer.
     * 
     * @param observer
     *            observer to remove
     * @see #addRequestObserver(IAPIRequestObserver)
     */
    public void removeRequestObserver(IAPIRequestObserver observer);

    // ==================== Eve Online(tm) API ======================

    /**
     * Returns the server status.
     * 
     * @param options
     *            Request options.
     */
    public APIResponse<ServerStatus> getServerStatus(RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Returns all skills available in the game.
     * 
     * Note that the server's response is rather large (>300kb) you might want
     * to disable response caching for this request.
     * 
     * @param options
     *            Request options.
     * @return
     * @throws IOException
     */
    public APIResponse<SkillTree> getSkillTree(RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Returns a list of all available conquerable outposts.
     * 
     * @param options
     *            Request options.
     * @return
     */
    public APIResponse<List<Outpost>> getConquerableOutposts(RequestOptions options);

    /**
     * Returns the characters on a given user account.
     * 
     * This method must be implemented in a thread-safe way.
     * 
     * @param credentialsProvider
     * @param options
     *            Request options.
     * @return
     * @throws IOException
     * 
     * @see #setDefaultErrorHandler(IErrorHandler)
     */
    public APIResponse<Collection<IBaseCharacter>> getAvailableCharacters(
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Retrieves character data.
     * 
     * @param characterId
     * @param credentialsProvider
     * @param skillTree
     * @param options
     * @return a {@link ICharacter#isFullyInitialized() fully initialized}
     *         character instance
     * @throws IOException
     */
    public APIResponse<ICharacter> getCharacter(CharacterID characterId,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Retrieves a character's assets.
     * 
     * Note that this EVE Online(tm) API method is subject to the
     * "long caching style" ( 0-23 hours ) so it's REALLY recommended to cache
     * this requests response.
     * 
     * @param characterId
     * @param credentialsProvider
     * @param options
     * @return
     * @throws IOException
     */
    public APIResponse<AssetList> getAssetList(CharacterID characterId,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Retrieves data about the skill a character is currently training (if
     * any).
     * 
     * @param characterId
     * @param credentialsProvider
     * @param options
     * @return
     * @throws IOException
     */
    public APIResponse<SkillInTraining> getSkillInTraining(CharacterID characterId,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Retrieves standings for a corporation a character is in.
     * 
     * This method <b>requires</b> a {@link IDAOProvider} to be set.
     * 
     * @param characterId
     * @param credentialsProvider
     * @param options
     * @return
     * @throws IOException
     * @see {@link #setStaticDataModel(IDAOProvider)}
     */
    public APIResponse<FactionStandings> getFactionStandings(ICharacter character,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Retrieves standings for a character.
     * 
     * This method <b>requires</b> a {@link IDAOProvider} to be set.
     * 
     * @param characterId
     * @param credentialsProvider
     * @param options
     * @return
     * @throws IOException
     * @see {@link #setStaticDataModel(IDAOProvider)}
     */
    public APIResponse<NPCCorpStandings> getNPCCorpCharacterStandings(ICharacter character,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Resolves names for a list of IDs (character IDs,corporation IDs, etc.).
     * 
     * @param type
     *            the type of names to resolve. Note that the elements in the
     *            array DO NOT correspond to the ID with the same index in the
     *            <code>ids</code> argument array , they are merely used to
     *            determine what kind of server response is to be expected.
     *            Passing the wrong type(s) here will cause the IDs to not be
     *            resolved correctly (read: result will contain only NULL
     *            names).
     * @param ids
     *            the IDs to resolve, must not be <code>null</code> / empty or
     *            contain <code>null</code> elements
     * @param options
     * @return map with ID (key) and resolved name (value) or a
     *         <code>null</code> value if the ID could not be resolved to a name
     * @throws IOException
     */
    public APIResponse<Map<String, String>> resolveNames(EntityType[] types,
            String[] ids, RequestOptions options) throws APIUnavailableException,
            APIErrorException;

    /**
     * Returns a list of transaction types used in wallet journals.
     * 
     * @param options
     * @return
     */
    public APIResponse<List<TransactionType>> getTransactionTypes(RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Returns a character's industry jobs.
     * 
     * @param character
     * @param credentialsProvider
     * @param options
     * @return
     * @throws APIUnavailableException
     * @throws APIErrorException
     */
    public APIResponse<Collection<IndustryJob>> getCharacterIndustryJobs(
            ICharacter character, ICredentialsProvider credentialsProvider,
            RequestOptions options) throws APIUnavailableException, APIErrorException;

    /**
     * Returns a character's market orders (up to one week ago).
     * 
     * @param character
     * @param credProvider
     * @param options
     * @return
     * @throws APIUnavailableException
     * @throws APIErrorException
     */
    public APIResponse<List<MarketOrder>> getMarketOrders(ICharacter character,
            ICredentialsProvider credProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;

    /**
     * Returns a list of market (wallet) transactions for a character.
     * 
     * @param character
     * @param credProvider
     * @param options
     * @return
     * @throws APIUnavailableException
     * @throws APIErrorException
     */
    public APIResponse<List<MarketTransaction>> getMarketTransactions(
            ICharacter character, ICredentialsProvider credProvider,
            RequestOptions options) throws APIUnavailableException, APIErrorException;

    /**
     * Returns the characters current skill queue.
     * 
     * @param character
     * @param credProvider
     * @param options
     * @return
     * @throws APIUnavailableException
     * @throws APIErrorException
     */
    public APIResponse<List<SkillQueueEntry>> getSkillQueue(IBaseCharacter character,
            ICredentialsProvider credProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException;
}
