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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.planning.IResourceQuantity;
import de.codesourcery.planning.IResourceType;

public class ItemWithQuantity implements IResourceQuantity 
{
	private InventoryType type;
	private int quantity;

	public ItemWithQuantity() {
	}

	public ItemWithQuantity(ItemWithQuantity other) {
		this.type = other.type;
		this.quantity = other.quantity;
	}
	
	public ItemWithQuantity(InventoryType type, int quantity) {
		
		if ( quantity <0 ) {
			throw new IllegalArgumentException("quantity cannot be negative");
		}
		
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		this.type = type;
		this.quantity = quantity;
	}
	
	@Override
	public String toString()
	{
		return "type="+( type != null ? type.getName() : "<not set>" )+" , quantity="+quantity; 
	}
	
	public double getVolume() {
		return quantity * type.getVolume();
	}
	
	/**
	 * Merge item quantites by type.
	 * 
	 * This method does NOT alter the input data in any way , instead
	 * cloned instances are used.
	 * 
	 * @param requiredMinerals
	 * @return Collection with NEW instances , at most one of each input type , with
	 * their quantities merged if multiple input items had the same type.
	 */
	public static Collection<ItemWithQuantity> mergeByType(List<ItemWithQuantity> requiredMinerals) {
		return mergeByTypeMap( requiredMinerals ).values();
	}
	
	/**
	 * Merge item quantites by type.
	 * 
	 * This method does NOT alter the input data in any way , instead
	 * cloned instances are used.
	 * 
	 * @param requiredMinerals
	 * @return merging result
	 */	
	public static Map<InventoryType,ItemWithQuantity>  mergeByTypeMap(List<ItemWithQuantity> requiredMinerals) {
		// merge item quantities by type
		final Map<InventoryType,ItemWithQuantity >  itemsByType =
			new HashMap<InventoryType,ItemWithQuantity>();
		
		for ( ItemWithQuantity mineral : requiredMinerals ) 
		{
			ItemWithQuantity existing = itemsByType.get( mineral.getType() );
			if ( existing == null ) {
				// create new instance because we might alter it later when merging
				existing = new ItemWithQuantity( mineral );
				itemsByType.put( mineral.getType() , existing );
			} else {
				existing.mergeWith( mineral );
			}
		}
		return itemsByType;
	}

	public void setType(InventoryType type)
	{
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		this.type = type;
	}

	public InventoryType getType()
	{
		return type;
	}
	
	public void mergeWith( ItemWithQuantity other) {
		if ( ! ObjectUtils.equals( this.type , other ) ) {
			throw new IllegalArgumentException("Cannot merge items with different types, this="+type+",other="+other.type);
		}
		if ( other.quantity < 0 ) {
			throw new IllegalArgumentException("Other item has negative quantity ?");
		}
		this.quantity += other.quantity;
	}
	
	public void decQuantity(int value) {
		if ( value < 0 ) {
			throw new IllegalArgumentException("Won't decrement by negative value");
		}
		// do not check for a negative result here,
		// is's ok 
		this.quantity = this.quantity - value;
	}
	
	public void incQuantity(int value) {
		if ( value < 0 ) {
			throw new IllegalArgumentException("Won't increment by negative value");
		}
		setQuantity( getQuantity() + value );
	}

	public void setQuantity(int quantity)
	{
		if ( quantity <0 ) {
			throw new IllegalArgumentException("quantity cannot be negative");
		}
		this.quantity = quantity;
	}

	public int getQuantity()
	{
		return quantity;
	}

	@Override
	public double getResourceQuantity()
	{
		return getQuantity();
	}

	@Override
	public IResourceType getResourceType()
	{
		return getType();
	}
}
