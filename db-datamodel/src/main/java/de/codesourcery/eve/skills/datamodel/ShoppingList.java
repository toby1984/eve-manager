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
package de.codesourcery.eve.skills.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.ShoppingList.ShoppingListEntry;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

public class ShoppingList implements Iterable<ShoppingListEntry>
{
    private String title;
    private String notes;

    private final List<ShoppingListEntry> entries = new ArrayList<ShoppingListEntry>();

    public final class ShoppingListEntry
    {
        private final InventoryType type;
        private int quantity;
        private int purchasedQuantity;

        protected ShoppingListEntry(ItemWithQuantity item) {
            this( item.getType(), item.getQuantity() );
        }

        public void updateWith(ShoppingListEntry other)
        {
            if ( other == null )
            {
                throw new IllegalArgumentException( "entry cannot be NULL" );
            }

            if ( ! this.hasSameTypeAs( other ) )
            {
                throw new IllegalArgumentException( "Won't update " + this
                        + " from entry " + other + " with different item type" );
            }

            setQuantity( other.quantity );
            setPurchasedQuantity( other.purchasedQuantity );
        }

        public boolean matches(ShoppingListEntry entry)
        {
            return this.type.getTypeId().equals( entry.type.getTypeId() )
                    && this.quantity == entry.quantity
                    && this.purchasedQuantity == entry.purchasedQuantity;
        }

        protected ShoppingListEntry(InventoryType type, int quantity) {
            if ( type == null )
            {
                throw new IllegalArgumentException( "inventory type cannot be NULL" );
            }
            if ( quantity <= 0 )
            {
                throw new IllegalArgumentException( "Quantity must be > 0" );
            }
            this.type = type;
            this.quantity = quantity;
        }

        public boolean hasSameTypeAs(ShoppingListEntry other)
        {
            return this.type.getTypeId().equals( other.getType().getTypeId() );
        }

        protected ShoppingListEntry(ShoppingListEntry entry) {
            this.type = entry.type;
            this.quantity = entry.quantity;
            this.purchasedQuantity = entry.purchasedQuantity;
        }

        public InventoryType getType()
        {
            return type;
        }

        public int getQuantity()
        {
            return quantity;
        }

        public boolean isPurchased()
        {
            return this.quantity == this.purchasedQuantity;
        }

        public void setQuantity(int quantity)
        {
            if ( quantity <= 0 )
            {
                throw new IllegalArgumentException( "Quantity must be > 0" );
            }
            this.quantity = quantity;
        }

        public ShoppingList getShoppingList()
        {
            return ShoppingList.this;
        }

        public void setPurchased()
        {
            this.purchasedQuantity = this.quantity;
        }

        public void setPurchasedQuantity(int purchasedQuantity)
        {
            if ( purchasedQuantity < 0 )
            {
                throw new IllegalArgumentException( "Quantity must be >= 0" );
            }
            this.purchasedQuantity = purchasedQuantity;
        }

        public int getPurchasedQuantity()
        {
            return purchasedQuantity;
        }

    }

    public ShoppingList(ShoppingList other) {
        if ( other == null )
        {
            throw new IllegalArgumentException( "shopping list cannot be NULL" );
        }

        this.title = other.getTitle();
        this.notes = other.getDescription();
        for (ShoppingListEntry entry : other.entries)
        {
            internalAddEntry( new ShoppingListEntry( entry ) );
        }
    }

    public ShoppingList(String title) {

        if ( StringUtils.isBlank( title ) )
        {
            throw new IllegalArgumentException( "title cannot be blank." );
        }

        this.setTitle( title );
    }

    public ShoppingListEntry addEntry(ShoppingListEntry entry)
    {
        // clone instance to be sure we add an
        // unreferenced instance
        return internalAddEntry( new ShoppingListEntry( entry ) );
    }

    protected ShoppingListEntry internalAddEntry(ShoppingListEntry entry)
    {
        if ( entry == null )
        {
            throw new IllegalArgumentException( "entry cannot be NULL" );
        }
        this.entries.add( entry );
        return entry;
    }

    public ShoppingListEntry addEntry(ItemWithQuantity item)
    {
        if ( item == null )
        {
            throw new IllegalArgumentException( "item type cannot be NULL" );
        }

        return internalAddEntry( new ShoppingListEntry( item ) );
    }

    public ShoppingListEntry addEntry(InventoryType type, int quantity)
    {
        return internalAddEntry( new ShoppingListEntry( type, quantity ) );
    }

    public List<ShoppingListEntry> getEntries()
    {
        return Collections.unmodifiableList( this.entries );
    }

    public void setTitle(String title)
    {
        if ( StringUtils.isBlank( title ) )
        {
            throw new IllegalArgumentException( "title cannot be blank." );
        }
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public void setDescription(String description)
    {
        this.notes = description;
    }

    public String getDescription()
    {
        return notes;
    }

    public int size()
    {
        return entries.size();
    }

    public void removeEntry(ShoppingListEntry e)
    {
        if ( e == null )
        {
            throw new IllegalArgumentException( "e cannot be NULL" );
        }
        entries.remove( e );
    }

    public boolean isEmpty()
    {
        return entries.isEmpty();
    }

    @Override
    public Iterator<ShoppingListEntry> iterator()
    {
        return Collections.unmodifiableList( this.entries ).iterator();
    }

}
