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

import static de.codesourcery.eve.skills.db.datamodel.SolarSystem.*;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.IAPIClient.EntityType;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.apiclient.parsers.ConquerableStationsParser.Outpost;
import de.codesourcery.eve.skills.datamodel.Asset;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.EveFlags;
import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class AssetListParser extends AbstractResponseParser<AssetList>
{

    private static final Logger log = Logger.getLogger( AssetListParser.class );

    public static final URI URI = toURI( "/char/AssetList.xml.aspx" );

    private AssetList result = new AssetList();

    private final IStaticDataModel provider;
    private final CharacterID characterId;
    private final IAPIClient apiClient;

    /**
     * 
     * @param provider
     *            DAO provider to resolve references to item types etc. May be
     *            <code>null</code> , in this case such references are NOT
     *            resolved and will most likely remain <code>null</code>.
     */
    public AssetListParser(CharacterID characterId, IStaticDataModel provider,
            IAPIClient apiClient, ISystemClock clock) {
        super( clock );
        if ( characterId == null )
        {
            throw new IllegalArgumentException( "character ID cannot be NULL" );
        }
        this.apiClient = apiClient;
        this.characterId = characterId;
        this.provider = provider;
    }

    private static final Object LOCK = new Object();

    // TODO: Caching should really be moved somewhere else...
    private static volatile boolean initialized = false;
    private static final Map<Long, Outpost> OUTPOSTS_BY_ID =
            new ConcurrentHashMap<Long, Outpost>();

    protected ILocation resolveOutpost(long id)
    {
        if ( ! initialized )
        {
            synchronized (LOCK)
            {
                if ( ! initialized )
                {
                    OUTPOSTS_BY_ID.clear();
                    log.debug( "resolveOutpost(): Fetching outpost data..." );
                    final APIResponse<List<Outpost>> outposts =
                            apiClient.getConquerableOutposts( RequestOptions.DEFAULT
                                    .withBypassInvoker( true ) );
                    System.out.println( ">>> fetched data of "
                            + outposts.getPayload().size() + " outposts." );
                    for (Outpost outpost : outposts.getPayload())
                    {
                        OUTPOSTS_BY_ID.put( outpost.getId(), outpost );
                    }
                    initialized = true;
                }
            }
        }

        final ILocation result = OUTPOSTS_BY_ID.get( id );
        if ( result == null )
        {
            log.error( "resolveOutpost(): Unable to resolve outpost with ID " + id );
            throw new UnparseableResponseException( "Unable to resolve outpost with ID "
                    + id );
        }
        return result;
    }

    /*
     * <result> <rowset name="assets" key="itemID"
     * columns="itemID,locationID,typeID,quantity,flag,singleton">
     * 
     * <row itemID="150354641" locationID="30000380" typeID="11019" quantity="1"
     * flag="0" singleton="1"> <rowset name="contents" key="itemID"
     * columns="itemID,typeID,quantity,flag,singleton"> <row itemID="150354709"
     * typeID="16275" quantity="200000" flag="5" singleton="0" /> <row
     * itemID="150354710" typeID="16272" quantity="150000" flag="5"
     * singleton="0" /> <row itemID="150354711" typeID="16273" quantity="150000"
     * flag="5" singleton="0" /> <row itemID="150354712" typeID="24597"
     * quantity="1000" flag="5" singleton="0" /> <row itemID="150354713"
     * typeID="24596" quantity="1000" flag="5" singleton="0" /> <row
     * itemID="150354714" typeID="24595" quantity="1000" flag="5" singleton="0"
     * /> <row itemID="150354715" typeID="24594" quantity="1000" flag="5"
     * singleton="0" /> <row itemID="150354716" typeID="24593" quantity="1000"
     * flag="5" singleton="0" /> <row itemID="150354717" typeID="24592"
     * quantity="1000" flag="5" singleton="0" /> <row itemID="150354718"
     * typeID="16274" quantity="450000" flag="5" singleton="0" /> <row
     * itemID="150354719" typeID="9848" quantity="1000" flag="5" singleton="0"
     * /> <row itemID="150354720" typeID="9832" quantity="8000" flag="5"
     * singleton="0" /> <row itemID="150354721" typeID="3689" quantity="5000"
     * flag="5" singleton="0" /> <row itemID="150354722" typeID="3683"
     * quantity="25000" flag="5" singleton="0" /> <row itemID="150354723"
     * typeID="44" quantity="4000" flag="5" singleton="0" /> </rowset> </row>
     * 
     * <row itemID="150354706" locationID="30001984" typeID="11019" quantity="1"
     * flag="0" singleton="1"> <rowset name="contents" key="itemID"
     * columns="itemID,typeID,quantity,flag,singleton"> <row itemID="150354741"
     * typeID="24593" quantity="400" flag="5" singleton="0" /> <row
     * itemID="150354742" typeID="24592" quantity="400" flag="5" singleton="0"
     * /> <row itemID="150354755" typeID="16275" quantity="199000" flag="5"
     * singleton="0" /> <row itemID="150354837" typeID="24597" quantity="400"
     * flag="5" singleton="0" /> <row itemID="150354838" typeID="24596"
     * quantity="400" flag="5" singleton="0" /> <row itemID="150354839"
     * typeID="24595" quantity="400" flag="5" singleton="0" /> <row
     * itemID="150354840" typeID="24594" quantity="400" flag="5" singleton="0"
     * /> <row itemID="150356329" typeID="14343" quantity="1" flag="5"
     * singleton="0" /> </rowset> </row>
     * 
     * <row itemID="150212056" locationID="60001078" typeID="25851"
     * quantity="10" flag="4" singleton="0" /> <row itemID="150212057"
     * locationID="60001078" typeID="20424" quantity="20" flag="4" singleton="0"
     * /> <row itemID="150212058" locationID="60001078" typeID="20421"
     * quantity="20" flag="4" singleton="0" />
     * 
     * <row itemID="150357641" locationID="30001984" typeID="23" quantity="1"
     * flag="0" singleton="1"> <rowset name="contents" key="itemID"
     * columns="itemID,typeID,quantity,flag,singleton"> <row itemID="150357740"
     * typeID="16275" quantity="9166" flag="0" singleton="0" /> </rowset> </row>
     * 
     * <row itemID="150212062" locationID="60001078" typeID="944" quantity="1"
     * flag="4" singleton="1" /> <row itemID="150212063" locationID="60001078"
     * typeID="597" quantity="1" flag="4" singleton="0" /> </rowset> </result>
     */

    private static final class ElementIterator implements Iterator<Element>
    {

        private int index = 0;
        private final NodeList children;

        private Element currentElement = null;

        public ElementIterator(Element e) {
            this.children = e.getChildNodes();
            currentElement = findNextElement();
        }

        private Element findNextElement()
        {
            final int len = children.getLength();
            for (int i = index; i < len; i++)
            {
                final Node n = children.item( i );
                if ( n.getNodeType() == Node.ELEMENT_NODE )
                {
                    index = i + 1;
                    return (Element) n;
                }
            }
            return null;
        }

        @Override
        public boolean hasNext()
        {
            return currentElement != null;
        }

        @Override
        public Element next()
        {

            if ( currentElement == null )
            {
                throw new NoSuchElementException();
            }

            final Element result = currentElement;
            currentElement = findNextElement();
            return result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException( "remove()" );
        }

    }

    @Override
    void parseHook(Document document) throws UnparseableResponseException
    {

        final Element resultNode = getResultElement( document );

        final Element rowsetNode = getChild( resultNode, "rowset" );

        for (ElementIterator it = new ElementIterator( rowsetNode ); it.hasNext();)
        {
            final Element n = it.next();
            parseElement( n, result );
        }

    }

    /*
     * <result> <rowset name="assets" key="itemID"
     * columns="itemID,locationID,typeID,quantity,flag,singleton"> <row
     * itemID="150354706" locationID="30001984" typeID="11019" quantity="1"
     * flag="0" singleton="1"> <rowset name="contents" key="itemID"
     * columns="itemID,typeID,quantity,flag,singleton"> <row itemID="150354741"
     * typeID="24593" quantity="400" flag="5" singleton="0" /> </rowset> </row>
     */
    private void parseElement(Element rowOrRowsetNode, AssetList parent)
    {

        if ( "rowset".equals( rowOrRowsetNode.getNodeName() ) )
        {

            for (ElementIterator it = new ElementIterator( rowOrRowsetNode ); it
                    .hasNext();)
            {
                final Element n = it.next();
                parseElement( n, parent );
            }

        }
        else if ( "row".equals( rowOrRowsetNode.getNodeName() ) )
        {
            parent.add( parseRow( rowOrRowsetNode ) );
        }
        else
        {
            throw new UnparseableResponseException( "Unexpected tag "
                    + rowOrRowsetNode.getNodeName() + " below <result>" );
        }

    }

    private Asset parseRow(Element rowNode)
    {

        final Asset result =
                new Asset( characterId, getLongAttributeValue( rowNode, "itemID" ) );

        // resolve location
        final long locationId = getLongAttributeValue( rowNode, "locationID", false );
        if ( locationId > 0 )
        { // check first: it's ok if the provider is not set!

            if ( this.provider != null )
            {
                if ( Station.isStationId( locationId ) )
                {
                    // log.debug("parseRow(): Resolving station "+locationId);
                    result.setLocation( provider.getStation( locationId ) );
                }
                else if ( isSolarSystemId( locationId ) )
                {
                    // log.debug("parseRow(): Resolving solar system "+locationId);
                    result.setLocation( provider.getSolarSystem( locationId ) );
                }
                else if ( isOutpostId( locationId ) )
                {
                    result.setLocation( resolveOutpost( locationId ) );
                }
                else
                {
                    throw new UnparseableResponseException(
                            "Unable to determine location with ID " + locationId );
                }
            }
        }

        // parse flags
        result.setFlags( EveFlags.fromTypeId( getIntAttributeValue( rowNode, "flag" ) ) );

        // resolve type
        final long typeId = getLongAttributeValue( rowNode, "typeID" );
        if ( provider != null )
        { // check first: it's ok if the provider is not set!
            result.setType( provider.getInventoryType( typeId ) );
        }

        // quantity
        result.setQuantity( getIntAttributeValue( rowNode, "quantity" ) );

        // singleton
        result.setIsPackaged( getIntAttributeValue( rowNode, "singleton", false ) == 0 );

        for (ElementIterator it = new ElementIterator( rowNode ); it.hasNext();)
        {
            final Element node = it.next();
            parseElement( node, result.getContents() );
        }

        return result;
    }

    @Override
    public URI getRelativeURI()
    {
        return URI;
    }

    @Override
    public AssetList getResult() throws IllegalStateException
    {
        assertResponseParsed();
        return result;
    }

    @Override
    public void reset()
    {
        result = new AssetList();
    }

}
