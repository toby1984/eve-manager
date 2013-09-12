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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.ServerStatus;
import de.codesourcery.eve.apiclient.datamodel.SkillInTraining;
import de.codesourcery.eve.apiclient.datamodel.SkillQueueEntry;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyRole;
import de.codesourcery.eve.apiclient.exceptions.APIErrorException;
import de.codesourcery.eve.apiclient.exceptions.APIUnavailableException;
import de.codesourcery.eve.apiclient.parsers.AssetListParser;
import de.codesourcery.eve.apiclient.parsers.CharacterIndustryJobsParser;
import de.codesourcery.eve.apiclient.parsers.CharacterSheetParser;
import de.codesourcery.eve.apiclient.parsers.NPCCorpCharacterStandingParser;
import de.codesourcery.eve.apiclient.parsers.ConquerableStationsParser;
import de.codesourcery.eve.apiclient.parsers.FactionStandingParser;
import de.codesourcery.eve.apiclient.parsers.GetAvailableCharactersParser;
import de.codesourcery.eve.apiclient.parsers.MarketOrderParser;
import de.codesourcery.eve.apiclient.parsers.ResolveNamesParser;
import de.codesourcery.eve.apiclient.parsers.ServerStatusParser;
import de.codesourcery.eve.apiclient.parsers.SkillInTrainingParser;
import de.codesourcery.eve.apiclient.parsers.SkillQueueParser;
import de.codesourcery.eve.apiclient.parsers.SkillTreeParser;
import de.codesourcery.eve.apiclient.parsers.TransactionTypeParser;
import de.codesourcery.eve.apiclient.parsers.WalletTransactionsParser;
import de.codesourcery.eve.apiclient.parsers.ConquerableStationsParser.Outpost;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.NPCCorpStandings;
import de.codesourcery.eve.skills.datamodel.FactionStandings;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IndustryJob;
import de.codesourcery.eve.skills.datamodel.MarketOrder;
import de.codesourcery.eve.skills.datamodel.MarketTransaction;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.datamodel.TransactionType;
import de.codesourcery.eve.skills.db.datamodel.Corporation;

/**
 * Eve Online(tm) API client that works over HTTP.
 * 
 * <pre>
 * Usage:
 * 
 *  {@literal
		final APIKey limitedKey = 
			APIKey.createLimitedKey( "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef" ); 

		final Credentials credentials = 
			new Credentials( 12345467 , limitedKey );
		
		final HttpAPIClient client = 
			new HttpAPIClient();
		
		final APIResponse<ServerStatus> serverStatus = 
			client.getServerStatus( RequestOptions.DEFAULT );
		
		System.out.println("Response : "+serverStatus);
		
		if ( serverStatus.getPayload().isOpen() ) {
			
			final APIResponse<Collection<IBaseCharacter>> responseFromServer = 
				client.getAvailableCharacters( credentials , RequestOptions.DEFAULT );
			
			for ( IBaseCharacter c : responseFromServer.getPayload() ) {
				System.out.println("# Got character: "+c.getName()+" / character ID "+c.getCharacterId() );
			}
			
		} else {
			System.out.println("Server currently not open.");
		}
		
		client.dispose(); 
 * }
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class HttpAPIClient extends AbstractHttpAPIClient
{

    /**
     * Constructs a client that connects to the default EVE Online(tm) API
     * server.
     * 
     * The default base URI currently is <code>http://api.eve-online.com</code>
     * 
     * @see #setBaseURI(URI)
     * @see #HttpAPIClient(URI)
     * @see #HttpAPIClient(String)
     */
    public HttpAPIClient() {
    	//  	https://api.eveonline.com/account/Characters.xml.aspx
        super( toURI( "https://api.eveonline.com" ) );
    }

    /**
     * Create client.
     * 
     * @see #setBaseURI(URI)
     * @see #HttpAPIClient(String)
     */
    public HttpAPIClient(URI baseURI) {
        super( baseURI );
    }

    /**
     * Create client.
     * 
     * @see #setBaseURI(URI)
     * @see #HttpAPIClient(URI)
     */
    public HttpAPIClient(String baseURI) throws URISyntaxException {
        this( new URI( baseURI ) );
    }

    /**
     * 
     * (thread-safe).
     */
    @Override
    public APIResponse<Collection<IBaseCharacter>> getAvailableCharacters(
            ICredentialsProvider credentialsProvider, RequestOptions requestOptions)
            throws APIUnavailableException, APIErrorException
    {

        final GetAvailableCharactersParser parser =
                new GetAvailableCharactersParser( getSystemClock() );

        final InternalAPIResponse response =
                sendRequest( credentialsProvider, parser, new HashMap<String, Object>(),
                    KeyRole.LIMITED_ACCESS, requestOptions );

        return new APIResponse<Collection<IBaseCharacter>>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<ServerStatus> getServerStatus(RequestOptions requestOptions)
            throws APIUnavailableException, APIErrorException
    {
        final ServerStatusParser parser = new ServerStatusParser( getSystemClock() );

        final InternalAPIResponse response = sendRequest( null, // no
            // authentication
            // required
            parser, new HashMap<String, Object>(), // no additional request
            // parameters
            KeyRole.NONE_REQUIRED, // no authentication required
            requestOptions );

        return new APIResponse<ServerStatus>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<SkillTree> getSkillTree(RequestOptions requestOptions)
            throws APIUnavailableException, APIErrorException
    {
        final SkillTreeParser parser = new SkillTreeParser( getSystemClock() );

        final InternalAPIResponse response = sendRequest( null, // no
            // authentication
            // required
            parser, new HashMap<String, Object>(), // no additional request
            // parameters
            KeyRole.NONE_REQUIRED, // no authentication required
            requestOptions );

        return new APIResponse<SkillTree>( response, parser.getResult(), getSystemClock() );

    }

    @Override
    public APIResponse<ICharacter> getCharacter(CharacterID characterId,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException
    {

        final CharacterSheetParser parser =
                new CharacterSheetParser( getStaticDataModel(), getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, characterId );

        final InternalAPIResponse response =
                sendRequest( credentialsProvider, parser, params, // request
                    // parameters
                    KeyRole.LIMITED_ACCESS, // limited API key required
                    options );

        final ICharacter character = parser.getResult();
        character.setFullyInitialized();
        return new APIResponse<ICharacter>( response, character, getSystemClock() );
    }

    @Override
    public APIResponse<AssetList> getAssetList(CharacterID characterId,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException
    {

        final AssetListParser parser =
                new AssetListParser( characterId, getStaticDataModel(), this,
                        getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, characterId );

        final InternalAPIResponse response =
                sendRequest( credentialsProvider, parser, params, // request
                    // parameters
                    KeyRole.FULL_ACCESS, // full API key required
                    options );

        return new APIResponse<AssetList>( response, parser.getResult(), getSystemClock() );
    }

    @Override
    public APIResponse<SkillInTraining> getSkillInTraining(CharacterID characterId,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException
    {
        final SkillInTrainingParser parser =
                new SkillInTrainingParser( getStaticDataModel(), getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, characterId );

        final InternalAPIResponse response =
                sendRequest( credentialsProvider, parser, params, // request
                    // parameters
                    KeyRole.LIMITED_ACCESS, // limited API key required
                    options );

        return new APIResponse<SkillInTraining>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<FactionStandings> getFactionStandings(ICharacter character,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException
    {

        if ( getStaticDataModel() == null )
        {
            throw new IllegalStateException(
                    "This method requires a DAO provider to be set" );
        }

        final FactionStandingParser parser =
                new FactionStandingParser( character , getStaticDataModel(), getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, character.getCharacterId() );

        final InternalAPIResponse response =
                sendRequest( credentialsProvider, parser, params, // request
                    // parameters
                    KeyRole.FULL_ACCESS, // limited API key required
                    options );

        return new APIResponse<FactionStandings>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<Map<String, String>> resolveNames(EntityType[] types,
            String[] ids, RequestOptions options) throws APIUnavailableException,
            APIErrorException
    {

        if ( ArrayUtils.isEmpty( types ) )
        {
            throw new IllegalArgumentException( "entity type(s) cannot be NULL/empty" );
        }

        if ( ids == null )
        {
            throw new IllegalArgumentException( "ids cannot be NULL" );
        }

        final Set<EntityType> typeSet = new HashSet<EntityType>();

        for (EntityType t : types)
        {
            typeSet.add( t );
        }

        final ResolveNamesParser parser =
                new ResolveNamesParser( ids, typeSet, getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        // make sure we don't transmit duplicate IDs
        final Set<String> idSet = new HashSet<String>();

        for (String id : ids)
        {
            if ( id == null )
            {
                throw new IllegalArgumentException( "NULL element in ID array ?" );
            }
            idSet.add( id.replaceAll( " ", "" ) ); // get rid of all spaces
        }

        final StringBuilder builder = new StringBuilder();

        for (Iterator<String> it = idSet.iterator(); it.hasNext();)
        {
            final String id = it.next();
            builder.append( id );
            if ( it.hasNext() )
            {
                builder.append( ',' );
            }
        }
        params.put( "ids", builder.toString() );

        final InternalAPIResponse response = sendRequest( null, parser, params, // request
            // parameters
            KeyRole.NONE_REQUIRED, // no API key required
            options );

        return new APIResponse<Map<String, String>>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<Collection<IndustryJob>> getCharacterIndustryJobs(
            ICharacter character, ICredentialsProvider credentialsProvider,
            RequestOptions options) throws APIUnavailableException, APIErrorException
    {
        final APIResponse<AssetList> assets =
                getAssetList( character.getCharacterId(), credentialsProvider,
                    RequestOptions.DEFAULT );

        final CharacterIndustryJobsParser parser =
                new CharacterIndustryJobsParser( assets.getPayload(),
                        getStaticDataModel(), getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, character.getCharacterId() );

        final InternalAPIResponse response =
                sendRequest( credentialsProvider, parser, params, // request
                    // parameters
                    KeyRole.FULL_ACCESS, // limited API key required
                    options );

        return new APIResponse<Collection<IndustryJob>>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<List<TransactionType>> getTransactionTypes(RequestOptions options)
    {

        final TransactionTypeParser parser = new TransactionTypeParser( getSystemClock() );

        final InternalAPIResponse response =
                sendRequest( null, parser, new HashMap<String, Object>(), // request
                    // parameters
                    KeyRole.NONE_REQUIRED, // limited API key required
                    options );

        return new APIResponse<List<TransactionType>>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<List<MarketOrder>> getMarketOrders(ICharacter character,
            ICredentialsProvider credProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException
    {

        final MarketOrderParser parser =
                new MarketOrderParser( getStaticDataModel(), getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, character.getCharacterId() );

        final InternalAPIResponse response = sendRequest( credProvider, parser, params, // request
            // parameters
            KeyRole.FULL_ACCESS, // full access API key required
            options );

        return new APIResponse<List<MarketOrder>>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<List<MarketTransaction>> getMarketTransactions(
            ICharacter character, ICredentialsProvider credProvider,
            RequestOptions options) throws APIUnavailableException, APIErrorException
    {
        final WalletTransactionsParser parser =
                new WalletTransactionsParser( getStaticDataModel(), getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, character.getCharacterId() );

        final InternalAPIResponse response = sendRequest( credProvider, parser, params, // request
            // parameters
            KeyRole.FULL_ACCESS, // full access API key required
            options );

        return new APIResponse<List<MarketTransaction>>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<List<SkillQueueEntry>> getSkillQueue(IBaseCharacter character,
            ICredentialsProvider credProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException
    {
        final SkillQueueParser parser =
                new SkillQueueParser( getSystemClock(), getStaticDataModel()
                        .getSkillTree() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, character.getCharacterId() );

        final InternalAPIResponse response = sendRequest( credProvider, parser, params, // request
            // parameters
            KeyRole.LIMITED_ACCESS, // limited access API key required
            options );

        return new APIResponse<List<SkillQueueEntry>>( response, parser.getResult(),
                getSystemClock() );
    }

    @Override
    public APIResponse<NPCCorpStandings> getNPCCorpCharacterStandings(ICharacter character,
            ICredentialsProvider credentialsProvider, RequestOptions options)
            throws APIUnavailableException, APIErrorException
    {

        if ( getStaticDataModel() == null )
        {
            throw new IllegalStateException(
                    "This method requires a DAO provider to be set" );
        }

        final NPCCorpCharacterStandingParser parser =
                new NPCCorpCharacterStandingParser( character, getStaticDataModel(),
                        getSystemClock() );

        final Map<String, Object> params = new HashMap<String, Object>();

        addCharacterId( params, character.getCharacterId() );

        final InternalAPIResponse response =
                sendRequest( credentialsProvider, parser, params, // request
                    // parameters
                    KeyRole.LIMITED_ACCESS, // limited API key required
                    options );

        return new APIResponse<NPCCorpStandings>( response, parser.getResult(),
                getSystemClock() );

    }

    @Override
    public APIResponse<List<Outpost>> getConquerableOutposts(RequestOptions options)
    {
        if ( getStaticDataModel() == null )
        {
            throw new IllegalStateException(
                    "This method requires a DAO provider to be set" );
        }

        final ConquerableStationsParser parser =
                new ConquerableStationsParser( getStaticDataModel(), getSystemClock() );

        final InternalAPIResponse response =
                sendRequest( null, parser, new HashMap<String, Object>(),
                    KeyRole.NONE_REQUIRED, // no API key required
                    options );

        return new APIResponse<List<Outpost>>( response, parser.getResult(),
                getSystemClock() );
    }
}