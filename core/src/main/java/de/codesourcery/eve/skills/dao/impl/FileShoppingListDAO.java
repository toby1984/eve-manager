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
package de.codesourcery.eve.skills.dao.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.codesourcery.eve.skills.dao.IShoppingListDAO;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ShoppingList;
import de.codesourcery.eve.skills.datamodel.ShoppingList.ShoppingListEntry;
import de.codesourcery.utils.xml.XmlHelper;

public class FileShoppingListDAO implements IShoppingListDAO, InitializingBean
{

    private static final Logger log = Logger.getLogger( FileShoppingListDAO.class );

    private static final String OUTPUT_ENCODING = "UTF-8";

    private IStaticDataModel dataModel;
    private File dataFile;

    // guarded-by: entries
    private final List<ShoppingList> entries = new ArrayList<ShoppingList>();

    // guarded-by: entries
    private boolean initialized = false;

    public FileShoppingListDAO() {
    }

    private void init()
    {
        synchronized (entries)
        {

            if ( ! initialized )
            {
                entries.addAll( loadFromFile() );
                initialized = true;
            }

        }
    }

    /*
     * <?xml version="1.0" ?> <shoppinglists> <shoppinglist title="blubb">
     * <description>a test</description> <entries> <entry itemId="12345"
     * totalQuantity="1236823" purchasedQuantity="1234"/> <entry itemId="12345"
     * totalQuantity="1236823" purchasedQuantity="1234"/> </entries>
     * </shoppinglist> </shoppinglists>
     */

    private Collection<? extends ShoppingList> loadFromFile()
    {

        if ( ! dataFile.exists() || dataFile.length() == 0 )
        {
            log.warn( "loadFromFile(): Input file " + dataFile.getAbsolutePath()
                    + " does not exist / is empty" );
            return Collections.emptyList();
        }

        try
        {
            log.info( "loadFromFile(): Loading data from " + dataFile.getAbsolutePath() );

            final Document dom = XmlHelper.parseFile( dataFile );

            final List<ShoppingList> result = new ArrayList<ShoppingList>();

            for (Element listNode : XmlHelper.getNodeChildren( dom, "shoppinglists","shoppinglist", false ))
            {

                final String title = listNode.getAttribute( "title" );

                final ShoppingList list = new ShoppingList( title );

                final String desc =
                        XmlHelper.getDirectChildValue( listNode, "description", false );
                if ( ! StringUtils.isBlank( desc ) )
                {
                    list.setDescription( desc );
                }

                result.add( list );

                final Element entriesNode =
                        XmlHelper.getElement( listNode, "entries", true );

                for (Element entryNode : XmlHelper.getNodeChildren( entriesNode, "entry",
                    false ))
                {

                    final long itemId =
                            Long.parseLong( entryNode.getAttribute( "itemId" ) );

                    final int quantity =
                            Integer.parseInt( entryNode.getAttribute( "totalQuantity" ) );

                    final int purchasedQuantity =
                            Integer.parseInt( entryNode
                                    .getAttribute( "purchasedQuantity" ) );

                    final ShoppingListEntry entry =
                            list
                                    .addEntry( dataModel.getInventoryType( itemId ),
                                        quantity );

                    entry.setQuantity( quantity );
                    entry.setPurchasedQuantity( purchasedQuantity );
                }
            }

            return result;
        }
        catch (FileNotFoundException e)
        {
            log.error( "loadFromFile(): Failed to load data from "
                    + dataFile.getAbsolutePath(), e );
            throw new RuntimeException( e );
        }
        catch (Exception e)
        {
            log.error( "loadFromFile(): Failed to load data from "
                    + dataFile.getAbsolutePath(), e );
            throw new RuntimeException( e );
        }
    }

    protected void writeToFile()
    {

        XMLStreamWriter writer = null;
        try
        {

            if ( ! dataFile.exists() )
            {
                final File parent = dataFile.getParentFile();
                if ( parent != null && ! parent.exists() )
                {
                    if ( ! parent.mkdirs() )
                    {
                        log.error( "writeToFile(): Failed to create parent directory "
                                + parent.getAbsolutePath() );
                    }
                    else
                    {
                        log.info( "writeToFile(): Created parent directory "
                                + parent.getAbsolutePath() );
                    }
                }
            }

            final XMLOutputFactory factory = XMLOutputFactory.newInstance();

            log.info( "writeToFile(): Writing to " + dataFile.getAbsolutePath() );
            final FileOutputStream outStream = new FileOutputStream( dataFile );
            writer = factory.createXMLStreamWriter( outStream, OUTPUT_ENCODING );

            writer.writeStartDocument( OUTPUT_ENCODING, "1.0" );
            writer.writeStartElement( "shoppinglists" ); // <shoppinglists>
            synchronized (this.entries)
            {
                for (ShoppingList list : this.entries)
                {
                    writer.writeStartElement( "shoppinglist" );
                    writer.writeAttribute( "title", list.getTitle() );
                    if ( ! StringUtils.isBlank( list.getDescription() ) )
                    {
                        writer.writeStartElement( "description" );
                        writer.writeCharacters( list.getDescription() );
                        writer.writeEndElement(); // </description>
                    }

                    writer.writeStartElement( "entries" );
                    for (ShoppingListEntry entry : list.getEntries())
                    {
                        writer.writeStartElement( "entry" );
                        writer.writeAttribute( "itemId", Long.toString( entry.getType()
                                .getId() ) );
                        writer.writeAttribute( "totalQuantity", Long.toString( entry
                                .getQuantity() ) );
                        writer.writeAttribute( "purchasedQuantity", Long.toString( entry
                                .getPurchasedQuantity() ) );

                        writer.writeEndElement(); // </entry>
                    }
                    writer.writeEndElement(); // </entries>
                    writer.writeEndElement(); // </shoppinglist>
                }
            }
            writer.writeEndElement(); // </shoppinglists>
            writer.writeEndDocument();

            writer.flush();
            writer.close();
        }
        catch (FileNotFoundException e)
        {
            log.error( "writeToFile(): Caught ", e );
            throw new RuntimeException( "Unable to save shopping list to " + dataFile, e );
        }
        catch (XMLStreamException e)
        {
            log.error( "writeToFile(): Caught ", e );
            throw new RuntimeException( "Unable to save shopping list to " + dataFile, e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch (XMLStreamException e)
                {
                    log.error( "writeToFile(): Caught ", e );
                }
            }
        }
    }

    @Override
    public boolean delete(ShoppingList list)
    {

        if ( list == null )
        {
            throw new IllegalArgumentException( "list cannot be NULL" );
        }

        init();

        synchronized (entries)
        {
            for (Iterator<ShoppingList> it = entries.iterator(); it.hasNext();)
            {
                if ( it.next() == list )
                {
                    it.remove();
                    writeToFile();
                    return true;
                }
            }
        }
        log.warn( "delete(): Failed to remove shopping list " + list );
        return false;
    }

    @Override
    public List<ShoppingList> getAll()
    {
        init();
        return Collections.unmodifiableList( entries );
    }

    @Override
    public void store(ShoppingList list)
    {

        if ( list == null )
        {
            throw new IllegalArgumentException( "list cannot be NULL" );
        }

        init();

        synchronized (entries)
        {

            boolean found = false;
            for (ShoppingList existing : entries)
            {
                if ( existing == list )
                {
                    found = true;
                    break;
                }
            }

            if ( ! found )
            {
                log.debug( "store(): Adding new shopping list " + list );
                entries.add( list );
            }
            else
            {
                log.debug( "store(): Shopping list already registered." );
            }
            writeToFile();
        }
    }

    public void setDataFile(File dataFile)
    {
        if ( dataFile == null )
        {
            throw new IllegalArgumentException( "dataFile cannot be NULL" );
        }
        this.dataFile = dataFile;
    }

    public void setDataModel(IStaticDataModel dataModel)
    {
        this.dataModel = dataModel;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        if ( dataFile == null )
        {
            throw new BeanInitializationException( "dataFile not set" );
        }

        if ( dataModel == null )
        {
            throw new BeanInitializationException( "dataModel not set" );
        }
    }

}
