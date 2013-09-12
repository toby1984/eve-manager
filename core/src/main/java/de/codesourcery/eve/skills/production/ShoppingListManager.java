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
package de.codesourcery.eve.skills.production;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import de.codesourcery.eve.skills.dao.IShoppingListDAO;
import de.codesourcery.eve.skills.datamodel.ShoppingList;
import de.codesourcery.eve.skills.datamodel.ShoppingList.ShoppingListEntry;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

public class ShoppingListManager implements InitializingBean
{

    private static final Logger log = Logger.getLogger( ShoppingListManager.class );

    private IShoppingListDAO shoppingListDAO;

    private final List<IShoppingListManagerListener> listeners =
            new ArrayList<IShoppingListManagerListener>();

    public interface IShoppingListManagerListener
    {

        public void shoppingListAdded(ShoppingList list);

        public void shoppingListRemoved(ShoppingList list);

        public void shoppingListChanged(ShoppingList list);

        public void listEntryAdded(ShoppingList list, ShoppingListEntry newEntry);

        public void listEntryRemoved(ShoppingList list, ShoppingListEntry removedEntry);

        public void listEntryChanged(ShoppingList list, ShoppingListEntry changedEntry);
    }

    public ShoppingListEntry addEntry(ShoppingList list, InventoryType type, int quantity)
    {
        final ShoppingListEntry newEntry = list.addEntry( type, quantity );

        this.shoppingListDAO.store( list );
        notifyListeners( list, newEntry, NotificationType.ENTRY_ADDED );
        return newEntry;
    }

    public void unsetPurchased(ShoppingList list, InventoryType type)
    {

        for (ShoppingListEntry entry : list)
        {

            if ( ! entry.getType().equals( type ) )
            {
                continue;
            }

            if ( entry.isPurchased() )
            {
                entry.setPurchasedQuantity( 0 );
                shoppingListEntryChanged( list, entry );
            }
        }
    }

    public void setPurchased(ShoppingList list, InventoryType type)
    {

        for (ShoppingListEntry entry : list)
        {

            if ( ! entry.getType().equals( type ) )
            {
                continue;
            }

            if ( entry.getPurchasedQuantity() < entry.getQuantity() )
            {
                entry.setPurchased();
                shoppingListEntryChanged( list, entry );
            }
        }
    }

    public void removeEntry(ShoppingList list, ShoppingListEntry entry)
    {
        list.removeEntry( entry );
        this.shoppingListDAO.store( list );
        notifyListeners( list, entry, NotificationType.ENTRY_REMOVED );
    }

    public void shoppingListEntryChanged(ShoppingList list, ShoppingListEntry entry)
    {
        this.shoppingListDAO.store( list );
        notifyListeners( list, entry, NotificationType.ENTRY_CHANGED );
    }

    public void addShoppingList(ShoppingList list)
    {
        if ( list == null )
        {
            throw new IllegalArgumentException( "shopping list cannot be NULL" );
        }
        this.shoppingListDAO.store( list );
        notifyListeners( list, NotificationType.LIST_ADDED );
    }

    public void shoppingListChanged(ShoppingList list)
    {
        if ( list == null )
        {
            throw new IllegalArgumentException( "list cannot be NULL" );
        }
        log.debug( "shoppingListChanged(): " + list );
        this.shoppingListDAO.store( list );
        notifyListeners( list, NotificationType.LIST_CHANGED );
    }

    public void removeShoppingList(ShoppingList list)
    {

        if ( list == null )
        {
            throw new IllegalArgumentException( "list cannot be NULL" );
        }

        if ( this.shoppingListDAO.delete( list ) )
        {
            notifyListeners( list, NotificationType.LIST_REMOVED );
        }
    }

    private enum NotificationType
    {
        LIST_ADDED, LIST_REMOVED, LIST_CHANGED, ENTRY_ADDED, ENTRY_REMOVED, ENTRY_CHANGED;
    }

    protected void notifyListeners(ShoppingList list, NotificationType type)
    {
        notifyListeners( list, null, type );
    }

    protected void notifyListeners(ShoppingList list, ShoppingListEntry entry,
            NotificationType type)
    {
        if ( list == null )
        {
            throw new IllegalArgumentException( "list cannot be NULL" );
        }

        List<IShoppingListManagerListener> copy;
        synchronized (listeners)
        {
            copy = new ArrayList<IShoppingListManagerListener>( this.listeners );
        }

        log.debug( "notifyListeners(): event=" + type + ", list=" + list );

        for (IShoppingListManagerListener l : copy)
        {
            switch ( type )
            {
                case ENTRY_ADDED:
                    if ( entry == null )
                    {
                        throw new IllegalArgumentException( "entry cannot be NULL" );
                    }
                    l.listEntryAdded( list, entry );
                    break;
                case ENTRY_REMOVED:
                    if ( entry == null )
                    {
                        throw new IllegalArgumentException( "entry cannot be NULL" );
                    }
                    l.listEntryRemoved( list, entry );
                    break;
                case ENTRY_CHANGED:
                    if ( entry == null )
                    {
                        throw new IllegalArgumentException( "entry cannot be NULL" );
                    }
                    l.listEntryChanged( list, entry );
                    break;
                case LIST_ADDED:
                    l.shoppingListAdded( list );
                    break;
                case LIST_CHANGED:
                    l.shoppingListChanged( list );
                    break;
                case LIST_REMOVED:
                    l.shoppingListRemoved( list );
                    break;
                default:
                    throw new RuntimeException( "Internal error" );
            }
        }
    }

    public void addChangeListener(IShoppingListManagerListener l)
    {
        if ( l == null )
        {
            throw new IllegalArgumentException( "listener cannot be NULL" );
        }
        synchronized (listeners)
        {
            listeners.add( l );
        }
    }

    public void removeChangeListener(IShoppingListManagerListener l)
    {
        if ( l == null )
        {
            throw new IllegalArgumentException( "listener cannot be NULL" );
        }
        synchronized (listeners)
        {
            listeners.remove( l );
        }
    }

    public List<ShoppingList> getEntries()
    {
        return this.shoppingListDAO.getAll();
    }

    public void setShoppingListDAO(IShoppingListDAO shoppingListDAO)
    {
        this.shoppingListDAO = shoppingListDAO;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        if ( this.shoppingListDAO == null )
        {
            throw new BeanInitializationException( "shoppingListDAO needs to be set" );
        }
    }

}
