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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;

/**
 * An asset in EVE Online(tm).
 *
 * Assets are <b>always</b> associated with
 * at least one character that owns them.
 * Assets may themselves contain other
 * assets ( e.g. containeers, ships ... see {@link #getContents()}).
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class Asset extends ItemWithQuantity {
	
	/*
Name 	Type 	Description
itemID 	int 	Unique ID for this item. This is only guaranteed to be unique within this page load. IDs are recycled over time and it is possible for this to happen. Also, items are not guaranteed to maintain the same itemID over time. When they are repackaged, stacks are split or merged, when they're assembled, and other actions can cause itemIDs to change.
locationID 	int 	References a solar system or station. Note that this column is not present in the sub-asset lists, i.e. for things inside of other things.
typeID 	int 	The type of this item. References the invTypes table.
quantity 	int 	How many items are in this stack.
flag 	int 	Indicates something about this item's storage location. The flag is used to differentiate between hangar divisions, drone bay, fitting location, and similar. Please see the Inventory Flags documentation.
singleton 	bool 	If true, indicates that this item is a singleton. This means that the item is not packaged.
	 */

	private final List<ILocation> locations = new ArrayList<ILocation>();
	
	private final Set<CharacterID> ownedByCharacterIds =
		new HashSet<CharacterID>();
	
	private final long itemId;
	private EveFlags flags;
	private final AssetList contents = new AssetList(this);
	private boolean packaged;

	private Asset container;
	
	private Asset(Set<CharacterID> characterIds , long itemId) 
	{
		if ( itemId < 1 ) {
			throw new IllegalArgumentException("Invalid item ID "+itemId);
		}
		if ( characterIds == null || characterIds.isEmpty() ) {
			throw new IllegalArgumentException("characterId cannot be NULL / empty ");
		}
		this.ownedByCharacterIds.addAll( characterIds );
		this.itemId = itemId;
	}
	
	/**
	 * Create instance.
	 * 
	 * @param characterId ID of the character that owns this asset 
	 * @param itemId the assets inventory type ID
	 */
	public Asset(CharacterID characterId , long itemId) 
	{
		if ( itemId < 1 ) {
			throw new IllegalArgumentException("Invalid item ID "+itemId);
		}
		if ( characterId == null ) {
			throw new IllegalArgumentException("characterId cannot be NULL");
		}
		this.ownedByCharacterIds.add( characterId );
		this.itemId = itemId;
	}
	
	public CharacterID getCharacterId()
	{
		if ( ownedByCharacterIds.size() > 1 ) {
			throw new IllegalStateException("getCharacterId() called on asset with multiple character IDs ?");
		}
		return ownedByCharacterIds.iterator().next();
	}
	
	public boolean hasMultipleCharacterIds() {
		return ownedByCharacterIds.size() > 1;
	}
	
	public Set<CharacterID> getCharacterIds()
	{
		return Collections.unmodifiableSet( ownedByCharacterIds );
	}
	
	@Override
	public String toString()
	{
		return "Asset[ id="+itemId+" , type="+getType()+", quantity="+getQuantity()+",container="+container+"]"; 
	}
	
	public Asset merge(Asset other,long itemId) {
		
		if ( ! ObjectUtils.equals( getType() , other.getType() ) ) {
			throw new IllegalArgumentException("Won't merge assets with different types");
		}
		
		final Asset result = new Asset( this.getCharacterIds() , itemId);
		result.ownedByCharacterIds.addAll( other.getCharacterIds() );
		
		result.setType( this.getType() );
		result.setQuantity(  this.getQuantity() + other.getQuantity() );
		
		if ( this.flags != other.flags ) {
			result.flags = EveFlags.NONE;
		} else {
			result.flags = this.flags;
		}
		
		if ( this.packaged != other.packaged ) {
			result.packaged = false;
		} else {
			result.packaged = this.packaged;
		}
		
		if ( this.container != null || other.container != null ) {
			if ( this.container != other.container ) {
				result.container = null;
			} else {
				result.container = this.container;
			}
		}
		
		result.addLocations( this.getLocations() );
		result.addLocations( other.getLocations() );
		
		return result;
	}

	public AssetList getContents() {
		return contents;
	}

	public long getItemId() {
		return itemId;
	}

	public void setIsPackaged(boolean packaged) {
		this.packaged = packaged;
	}

	public boolean isPackaged() {
		return packaged;
	}

	public void setLocation(ILocation location) {
		if ( location == null ) {
			throw new IllegalArgumentException("location cannot be NULL");
		} else if ( location == ILocation.ANY_LOCATION || location.isAnyLocation() ) {
			throw new IllegalArgumentException("location cannot be ANY");
		}
		this.locations.clear();
		this.locations.add( location );
	}

	public boolean hasMultipleLocations() {
		return locations.size() > 1;
	}
	
	public void addLocations(Collection<ILocation> locations) {
		for ( ILocation l : locations ) {
			addLocation(l);
		}
	}
	
	public void addLocation(ILocation loc) {
		if ( loc == null ) {
			throw new IllegalArgumentException("location cannot be NULL");
		}
		
		for ( ILocation existing : this.locations ) {
			if ( existing.getDisplayName().equals( loc.getDisplayName() ) ) {
				return;
			}
		}
		this.locations.add( loc );
	}
	
	public List<ILocation> getLocations() {
		
		if ( getContainer() != null ) {
			return getContainer().getLocations();
		}
		return Collections.unmodifiableList( this.locations );
	}
	
	public ILocation getLocation() {

		// if this item is part of a container, always returns the container's location
		// (since this must obviously be the items location as well)
		if ( getContainer() != null ) {
			final ILocation result = getContainer().getLocation();
			if ( result != null ) {
				return result;
			}
		}
		
		if ( hasMultipleLocations() ) {
			throw new IllegalStateException("getLocation() called on asset with multiple locations");
		}
		
		if ( locations.isEmpty() ) {
			return null;
		}
		return this.locations.get(0);
	}

	public void setFlags(EveFlags flags) {
		this.flags = flags;
	}

	public EveFlags getFlags() {
		return flags;
	}

	public void setContainer(Asset container) {
		this.container = container;
	}

	public Asset getContainer() {
		return container;
	}

	public boolean hasLocation(ILocation expected) {
		
		if ( getContainer() != null ) {
			return getContainer().hasLocation( expected );
		}
		
		if ( ! locations.isEmpty() ) {
			for ( ILocation loc : locations ) {
				if ( loc.getDisplayName().equals( expected.getDisplayName() ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasSameLocationsAs(Asset b)
	{
		
		for ( ILocation loc : getLocations() ) {
			if ( ! b.hasLocation( loc ) ) {
				return false;
			}
		}
		
		for ( ILocation loc : b.getLocations() ) {
			if ( ! hasLocation( loc ) ) {
				return false;
			}
		}
		return true;
	}
}
