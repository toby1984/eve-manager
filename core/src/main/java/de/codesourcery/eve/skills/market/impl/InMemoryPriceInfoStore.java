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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.IPriceInfoStore;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class InMemoryPriceInfoStore implements IPriceInfoStore {

	public static final Logger log = Logger
			.getLogger(InMemoryPriceInfoStore.class);
	
	protected final PriceInfoCache priceInfoCache = new PriceInfoCache();
	
	private final ISystemClock systemClock;

	@Override
	public synchronized List<PriceInfo> get(MarketFilter filter, InventoryType itemType) {
		return priceInfoCache.getLatestPriceInfos( filter , itemType );
	}

	@Override
	public final synchronized void persist() throws IOException {
		log.info("persist(): Called.");
		persistHook();
	}
	
	public InMemoryPriceInfoStore(ISystemClock clock) {
		if ( clock == null ) {
			throw new IllegalArgumentException("clock cannot be NULL");
		}
		this.systemClock = clock;
	}
	
	public synchronized void evict(PriceInfo info) {
		priceInfoCache.evict( info );
	}

	protected ISystemClock getSystemClock()
	{
		return systemClock;
	}
	
	protected void persistHook() throws IOException {
	}

	@Override
	public final synchronized void shutdown() {
		
		log.info("shutdown(): Called.");
		try {
			shutdownHook();
		} 
		finally {
			priceInfoCache.clear();
		}
	}
	
	protected void shutdownHook() {
		
	}
	
	@Override
	public synchronized void save(PriceInfo info) {
		priceInfoCache.storePriceInfo( info );
	}

	@Override
	public Map<Long,List<PriceInfo>> getLatestPriceInfos(Region region,Type kind ,Collection<InventoryType> items) { 
		return priceInfoCache.getLatestPriceInfos( region, kind , items );
	}

	@Override
	public Map<Long,InventoryType> getAllKnownInventoryTypes(Region region,IInventoryTypeDAO dao) {
		return priceInfoCache.getAllKnownInventoryTypes( region ,  dao );
	}

	@Override
	public void save(Region region,InventoryType type , Collection<PriceInfo> infos)
	{

		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		if ( infos.isEmpty() ) {
			return;
		}
		
		final Iterator<PriceInfo> it = infos.iterator();
		if ( ! it.next().getItemType().getId().equals( type.getId() ) ) {
			throw new IllegalArgumentException("Item type mismatch");
		}
		
		if ( ! it.next().getRegion().getID().equals( region.getID() ) ) {
			throw new IllegalArgumentException("Item type mismatch");
		}
		
		this.priceInfoCache.storePriceInfos( region, type, infos);
	}

	@Override
	public List<PriceInfo> getPriceHistory(Region region, Type type,
			InventoryType item)
	{
		return priceInfoCache.getPriceHistory(region,type,item);
	}

}
