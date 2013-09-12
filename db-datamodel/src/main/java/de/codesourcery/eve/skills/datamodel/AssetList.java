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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.exceptions.AssetNotFoundException;

public class AssetList implements Iterable<Asset> {

	public static final Logger log = Logger.getLogger(AssetList.class);

	private final Map<Long,Asset> assets = new HashMap<Long,Asset>();
	
	public interface IMergeStrategy {

		public boolean mayBeMerged(Asset a,Asset b);
	}

	/**
	 * The asset that 'owns' this asset list, might be <code>null</code>.
	 * 
	 * An asset may have at most one owner at any given time. This
	 * field is used to make sure no asset is part of two different asset lists
	 * at the same time. 
	 */
	private final Asset owner;

	public AssetList() {
		owner = null;
	}
	
	public AssetList(Asset owner) {
		this.owner = owner;
	}

	public Asset getOwner() {
		return owner;
	}

	public ILocation getLocation() {
		return owner != null ? owner.getLocation() : null;
	}

	public AssetList getAssetsByType(InventoryType type) {

		AssetList result = new AssetList();
		for ( Asset a : this ) {
			if ( a.getType().equals(type) ) {
				result.add( a );
			}
		}
		return result;
	}

	public List<InventoryType> getInventoryTypes() {

		final Set<InventoryType> result =
			new HashSet<InventoryType>();

		for ( Asset a : this ) {
			result.add( a.getType() );
		}

		final List<InventoryType> realResult =
			new ArrayList<InventoryType>( result );

		Collections.sort( realResult , new Comparator<InventoryType>() {

			@Override
			public int compare(InventoryType o1, InventoryType o2)
			{
				return o1.getName().compareTo( o2.getName() );
			}} );

		return realResult;
	}

	public List<ILocation> getLocations() {

		final Set<ILocation> result =
			new HashSet<ILocation>();

		for ( Asset a : this ) {
			result.addAll( a.getLocations() );
		}

		final List<ILocation> realResult =
			new ArrayList<ILocation>( result );

		Collections.sort( realResult , new Comparator<ILocation>() {

			@Override
			public int compare(ILocation o1, ILocation o2)
			{
				return o1.getDisplayName().compareTo( o2.getDisplayName() );
			}} );

		return realResult;
	}

	public AssetList getAssetsByLocation(ILocation location) {

		AssetList result = new AssetList();
		for ( Asset a : this ) {
			if ( a.hasLocation( location ) ) {
				result.add( a );
			}
		}
		return result;
	}	

	@Override
	public Iterator<Asset> iterator() {
		return assets.values().iterator();
	} 

	private final class DepthFirstIterator implements Iterator<Asset> {

		private Stack<Asset> stack =
			new Stack<Asset>();

		public DepthFirstIterator() {
			stack.addAll( assets.values() );
		}

		@Override
		public boolean hasNext() {
			return ! stack.isEmpty();
		}

		@Override
		public Asset next() {
			final Asset result = stack.pop();
			for ( Asset child : result.getContents() ) {
				stack.push( child );
			}
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove()");
		}

	}

	/**
	 * Creates a new asset list with
	 * assets grouped by item types.
	 * @return
	 */

	public AssetList createMergedAssetListByType(final boolean ignoreDifferentPackaging,
			boolean ignoreDifferentLocations) 
	{
		return createMergedAssetListByType( 
			createMergeStrategy( ignoreDifferentPackaging , ignoreDifferentLocations ) 
		);
	}

	public AssetList createMergedAssetListByType(IMergeStrategy strategy) {

		if ( log.isTraceEnabled() ) {
			log.trace("createMergedAssetListByType(): Merging "+assets.size()+" top-level assets.");
		}

		// gather assets in linear list.
		// items may be stored in containers which themselves
		// are items as well. The following loops traverses
		// the 'item tree' and gathers all nodes (assets)
		// regardless of whether they're currently stored
		// in some other item (container,ship,etc.) or not
		final Map<Long,List<Asset>> mergedByType =
			new HashMap<Long,List<Asset>>();
		
		int totalCount=0;

outer:		
		for ( Iterator<Asset> it = new DepthFirstIterator() ; it.hasNext() ; ) {
			
			final Asset a = it.next();
			totalCount++;

			final Long typeId = a.getType().getTypeId();

			List<Asset> values = mergedByType.get( typeId );

			if ( values == null ) {
				values = new ArrayList<Asset>();
				mergedByType.put( typeId , values );
			}

			for ( Asset existing : values ) {
				if ( strategy.mayBeMerged( existing , a ) ) {
					Asset newAsset = existing.merge( a , a.getItemId() );
					values.remove(existing );
					values.add( newAsset );
					continue outer;
				}
			}
			values.add( a );
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("createMergedAssetListByType(): "+totalCount+" assets total.");
		}

		final AssetList result = new AssetList();
		for ( List<Asset> candidates : mergedByType.values() ) {
			result.addAll( candidates );
		}

		return result;
	}

	protected IMergeStrategy createMergeStrategy(final boolean ignoreDifferentPackaging,
			final boolean ignoreDifferentLocations) 
	{
	
		return new IMergeStrategy() {

			@Override
			public boolean mayBeMerged(Asset a, Asset b)
			{
				
				if ( ! ignoreDifferentPackaging ) {
					if ( a.isPackaged() != b.isPackaged() ) {
						return false;
					}
				}
				
				if ( ! ignoreDifferentLocations ) {
					if ( ! a.hasSameLocationsAs( b ) ) {
						return false;
					}
				}
				return true;
			}};
	}

	private long merge(AssetList target,
			List<Asset> candidates ,
			IMergeStrategy strategy,
			long startItemId) 
	{

		long lastItemId = startItemId+1;
		
		if ( candidates.isEmpty() ) {
			throw new IllegalArgumentException("Cannot merge empty list");
		}

		final List<Asset> copy = 
			new ArrayList<Asset>( candidates );

		boolean somethingMerged = false;
		do {

			int index = 0;
			somethingMerged = false;
outer:			
			for (  ; copy.size() > 1 && index < copy.size() ; index++) {

				final Asset asset1 = copy.get(index);

				final int start = ( index + 1 ) % copy.size();
				int j = start;
				do {
					final Asset asset2 = copy.get( j );
					if ( strategy.mayBeMerged( asset1,asset2 ) ) 
					{
						final Asset merged = asset1.merge( asset2 , lastItemId++ );
						
						copy.remove( asset1 );
						copy.remove( asset2 );
						
//						merged.setContainer( null );
						
						copy.add( 0 , merged );
						somethingMerged=true;
						index=0;
						break outer;
					}
					j = (j+1) % copy.size();
				} while ( copy.size() > 1 && j != start );
			}
		} while ( somethingMerged );

		target.addAll( copy );
		
		return lastItemId;
	}

	public Asset item(int index) {

		final int s = size();
		if ( index < 0 || index >= s ) {
			throw new ArrayIndexOutOfBoundsException("No such element: "+index+"( size: "+s+" )");
		}

		Iterator<Asset> it = assets.values().iterator();
		for ( int i = 0 ; it.hasNext() ; i++ ) {
			final Asset a = it.next();
			if ( i == index ) {
				return a;
			}
		}

		// might be triggered by concurrent manipulation of assets map
		throw new RuntimeException("Unreachable code reached / concurrent modification ?");
	}

	/**
	 * Adds a top-level asset (an asset that is not contained in
	 * another asset) to this list.
	 * 
	 * EVE online does not allow nesting non-empty containers,
	 * so does this method.
	 * 
	 * @param asset
	 * @return
	 * @throws IllegalStateException When trying to add a non-toplevel
	 *  asset (an asset that is already stored in some other item/container)
	 * @see Asset#getContainer()
	 */
	public Asset add(Asset asset) {

		if (asset == null) {
			throw new IllegalArgumentException(
			"asset cannot be NULL");
		}
		
		if ( asset.getItemId() <= 0 ) {
			throw new IllegalArgumentException("Won't add asset with invalid item ID "+asset.getItemId());
		}

		if ( isInOtherContainer( asset ) ) {
			
			final String msg = "Won't add asset "+asset+
			" , it is already part of different container "+asset.getContainer()+" but "+
			" this asset list holds data for "+this.owner;
			
			log.error("add(): "+msg);
			
			throw new IllegalStateException( msg );
		}

		final Asset replacedAsset =
			this.assets.put( asset.getItemId() , asset );
		
		if ( this.owner != null ) {
			asset.setContainer( this.owner );
		}
		
		if ( replacedAsset != null && replacedAsset.getContainer() == this.owner ) {
			replacedAsset.setContainer( null );
		}
		return replacedAsset;
	}

	private boolean isInOtherContainer(Asset a) {
		return this.owner != null && a.getContainer() != null && ! equals( a.getContainer() , this.owner );
	}
	
	private boolean equals(Asset a,Asset b) {
		if ( a != null && b != null ) {
			return a.getItemId() == b.getItemId();
		}
		return a == b;
	}
	
	public void addAll(Collection<Asset> assets) {
		for ( Asset a : assets ) {
			add( a );
		}
	}
	
	public void remove(Asset asset) {

		if (asset == null) {
			throw new IllegalArgumentException("asset cannot be NULL");
		}

		if ( asset.getContainer() == null ) {
			return;
		}

		final Asset removed =
			this.assets.remove( asset.getItemId() );

		if ( removed != null ) {
			removed.setContainer( null );
		}
	}

	public Asset searchAsset(long itemId) {

		Asset result = null;
		for ( Asset a : this.assets.values() ) {
			if ( a.getItemId() == itemId ) {
				return a;
			}
			result = a.getContents().searchAsset( itemId );
			if ( result != null ) {
				return result;
			}
		}

		return null;
	}

	public int size() {
		return assets.size();
	}

	public boolean isEmpty() {
		return assets.isEmpty();
	}

	public Asset getAsset(long assetId) {
		final Asset result = assets.get( assetId );
		if ( result == null ) {
			throw new AssetNotFoundException( assetId );
		}
		return result;
	}

	/**
	 * Returns assets contained in this list.
	 * 
	 * @param includeChildContainers Whether to include items
	 * stored in containers or just the top-level assets / items 
	 * @return
	 */
	public List<Asset> getAssets( boolean includeChildContainers) 
	{
		if ( ! includeChildContainers ) {
			return new ArrayList<Asset>( this.assets.values() );
		}

		final List<Asset> result = new ArrayList<Asset>();
		gatherAssets( result );
		return result;
	}

	protected void gatherAssets(Collection<Asset> result) {

		for ( Asset a : this.assets.values() ) {
			result.add( a );
			a.getContents().gatherAssets( result );
		}

	}

	public void add(AssetList contents)
	{
		for ( Asset a : contents ) {
			add( a );
		}
	}
}
