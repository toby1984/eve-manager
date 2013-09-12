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
package de.codesourcery.eve.skills.market.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.MarketFilter;

/**
 * Cache that holds inventory type price information
 * for all <code>Region</code>s.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
class PriceInfoCache {

	public static final Logger log = Logger.getLogger(PriceInfoCache.class);

	// Map<Region,Map<InventoryType#getId(),CacheEntry>
	protected final Map<Region,Map<Long,CacheEntry>> cache =
		new ConcurrentHashMap<Region,Map<Long,CacheEntry>>();

	protected class CacheEntry {

		protected Long itemTypeId;
		protected Region region;

		/*
		 * hint: List entries are always sorted descending
		 * by timestamp (latest entries come first).
		 */
		protected final SortedPriceList priceInfos = new SortedPriceList();

		public CacheEntry(Long itemTypeId,Region region) {
			if (itemTypeId == null) {
				throw new IllegalArgumentException(
				"itemTypeId cannot be NULL");
			}
			this.itemTypeId = itemTypeId;
			this.region = region;
		}

		public synchronized InventoryType getItemType(IInventoryTypeDAO dao) {
			if ( priceInfos.isEmpty() ) {
				return dao.fetch( itemTypeId );
			}
			return priceInfos.getItemType();
		}

		public synchronized List<PriceInfo> getPriceInfos() {
			return priceInfos.getAllPrices();
		}

		public synchronized boolean isEmpty() {
			return priceInfos.isEmpty();
		}

		public List<PriceInfo> getLatestPriceInfos(Type requestedType) {
			return priceInfos.getLatestPriceInfos( requestedType );
		}

		public void addPriceInfo(PriceInfo info) {

			if ( log.isDebugEnabled() ) {
				log.debug("addPriceInfo(): Adding "+info);
			}
			
			if (info == null) {
				throw new IllegalArgumentException("priceInfo cannot be NULL");
			}

			if ( this.region != null ) {
				if ( ! Region.isSameRegion( this.region , info.getRegion() ) ) {
					throw new IllegalArgumentException("PriceInfo region mismatch");
				}
			} else if ( ! this.itemTypeId.equals( info.getItemType().getId() ) ) {
				throw new IllegalArgumentException("Item type mismatch.");
			}

			priceInfos.store( info );
			
			if ( log.isTraceEnabled() ) {
				log.debug("addPriceInfo(): After store: "+priceInfos.getDebugString());
			}
		}

		public List<PriceInfo> getLatestPriceInfos(MarketFilter filter) {
			if (filter == null) {
				throw new IllegalArgumentException("filter cannot be NULL");
			}
			return priceInfos.getLatestPriceInfos( filter.getOrderType() );
		}
		
		public List<PriceInfo> getPriceHistory(Type type) {
			
			if (type == null) {
				throw new IllegalArgumentException("type cannot be NULL");
			}
			
			return priceInfos.getPriceHistory( type );
		}	

		public void addPriceInfos(Collection<PriceInfo> info) {
			if ( log.isDebugEnabled() ) {
				log.debug("addPriceInfos(): Storing "+info.size()+" prices "+
						" for item ID "+itemTypeId+" , region "+region.getName() );
			}
			this.priceInfos.store( info );
		}

		public synchronized void evict(PriceInfo info) {
			priceInfos.remove( info );
		}

	}

	public Set<Region> getCachedRegions() {
		return new HashSet<Region>( this.cache.keySet() );
	}

	public List<List<PriceInfo>> getDataForRegion(Region r) {

		if (r == null) {
			throw new IllegalArgumentException("region cannot be NULL");
		}

		final List<List<PriceInfo>> result =
			new ArrayList<List<PriceInfo>>();

		final Map<Long, CacheEntry> entries = getCacheEntriesForRegion( r );
		if ( entries != null ) {
			for ( Map.Entry<Long,CacheEntry> e : entries.entrySet() ) {
				result.add( e.getValue().getPriceInfos() );
			}
		}

		return result;
	}

	public void clear() {
		this.cache.clear();
	}
	
	protected CacheEntry getCacheEntry(Region r , InventoryType type,boolean createIfMissing) {
		
		CacheEntry entry = getCacheEntry(r, type);
		if ( entry == null && createIfMissing) {
			entry = new CacheEntry( type.getId() , r );
			Map<Long, CacheEntry> map =
				getCacheEntriesForRegion( r );

			if ( map == null ) {
				map = new ConcurrentHashMap<Long, CacheEntry>();
				storeCacheEntriesForRegion( r , map );
			}
			map.put( type.getId() , entry );
		}
		return entry;
	}

	protected CacheEntry getCacheEntry(Region r , InventoryType type) {
		
		final Map<Long,CacheEntry> entriesByItem = this.cache.get( r );
		if ( entriesByItem == null ) {
			return null;
		}
		return entriesByItem.get( type.getId() );
	}

	public List<PriceInfo> getLatestPriceInfos(MarketFilter filter, InventoryType item) {

		if ( log.isDebugEnabled() ) {
			log.debug("getPriceInfo(): Looking for item="+item+" , filter = "+filter);
		}

		CacheEntry entry = getCacheEntry( filter ,item );
		if ( entry == null ) {
			if ( log.isDebugEnabled() ) {
				log.debug("getPriceInfo(): [ miss ] Found no cache entry "+
						" for region "+filter.getRegion()+" , item "+item);
			}			
			return null;
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("getPriceInfo(): Found cache entry for this filter and item.");
		}		
		return entry.getLatestPriceInfos( filter.getOrderType() );
	}

	public List<PriceInfo> getPriceHistory( Region region,PriceInfo.Type type, InventoryType item) {
		if ( item == null ) {
			throw new IllegalArgumentException("item cannot be NULL");
		}

		final CacheEntry entry = getCacheEntry( region , item );
		if ( entry == null ) {
			return Collections.emptyList();
		}
		return entry.getPriceHistory( type );
	}

	protected CacheEntry getCacheEntry(MarketFilter filter, InventoryType item) {

		if ( filter == null ) {
			throw new IllegalArgumentException("filter cannot be NULL");
		}

		return getCacheEntry( filter.getRegion() , item );
	}

	public void storePriceInfo(PriceInfo info) {

		if (info == null) {
			throw new IllegalArgumentException("priceInfo cannot be NULL");
		}

		if ( info.getRegion() == null ) {
			throw new IllegalArgumentException("priceInfo needs to have a Region set");
		}

		if ( log.isDebugEnabled() ) {
			log.debug("storePriceInfo(): price = "+info);
		}

		getCacheEntry( info.getRegion() , info.getItemType() , true ).addPriceInfo( info );
		
	}
	
	public void storePriceInfos(Region region,InventoryType item , Collection<PriceInfo> infos) {
		
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		if ( item == null ) {
			throw new IllegalArgumentException("item cannot be NULL");
		}
		
		if ( log.isDebugEnabled() ) {
			log.debug("storePriceInfos(): Storing "+
				infos.size()+" prices for region "+
				region.getName()+" , item "+item.getName()+" ("+
				item.getTypeId()+")");
		}
		
		getCacheEntry( region  , item , true ).addPriceInfos( infos );
	}

	public void evict(PriceInfo info) {

		CacheEntry entry = getCacheEntry( info.getRegion() , info.getItemType() );
		if ( entry != null ) {
			entry.evict( info );
		}
	}

	public Map<Long,List<PriceInfo> > getLatestPriceInfos(Region region,Type type ,
			Collection<InventoryType> items) { 

		final Map<Long, CacheEntry> entriesByItem =
			getCacheEntriesForRegion( region );

		if ( entriesByItem == null ) {
			return new HashMap<Long,List<PriceInfo>>();
		}

		final Map<Long,List<PriceInfo>> result =
			new HashMap<Long,List<PriceInfo>>();

		final Set<Long> itemIds =
			new HashSet<Long>();

		for ( InventoryType item : items ) {
			itemIds.add( item.getId() );
		}

		for ( Map.Entry<Long,CacheEntry> entry : entriesByItem.entrySet() ) {

			final Long itemId =
				entry.getKey();

			if ( ! itemIds.contains( itemId ) ) {
				continue;
			}

			result.put( entry.getKey() , entry.getValue().getLatestPriceInfos( type ) );
		}
		return result;
	}

	protected void storeCacheEntriesForRegion(Region region , Map<Long, CacheEntry> entries) {
		this.cache.put( region , entries );
	}

	protected Map<Long, CacheEntry> getCacheEntriesForRegion(Region region) {
		return this.cache.get( region );
	}

	public Map<Long,InventoryType> getAllKnownInventoryTypes(Region region,IInventoryTypeDAO dao) {

		Map<Long, CacheEntry> entries = 
			getCacheEntriesForRegion( region );

		if ( entries == null ) {
			return new HashMap<Long,InventoryType>();
		}

		final Map<Long,InventoryType> result =
			new HashMap<Long, InventoryType>();

		for ( Map.Entry<Long,CacheEntry> entry : entries.entrySet() ) {
			if ( ! entry.getValue().isEmpty() ) {
				result.put( entry.getKey() , entry.getValue().getItemType( dao ) );
			}
		}

		return result;
	}

}
