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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class FilePriceInfoStore extends InMemoryPriceInfoStore {

	public FilePriceInfoStore(ISystemClock clock) {
		super(clock );
	}

	public static final Logger log = Logger.getLogger(FilePriceInfoStore.class);

	private File baseDir;

	private final Map<Region,Set<Long>> loadedPrices = 
		new HashMap<Region,Set<Long>>();

	@Override
	protected void persistHook() throws IOException {

		log.info("persist(): Called.");

		final Set<Region> regions = 
			this.priceInfoCache.getCachedRegions();
		
		for ( Region r : regions ) {

			final List<List<PriceInfo>> dataForRegion = 
				priceInfoCache.getDataForRegion(r);

			if ( log.isTraceEnabled() ) {
				log.debug("persistHook(): Persisting "+
						dataForRegion.size()+" items in region "+r);
			}
			
			if ( dataForRegion.isEmpty() ) {
				continue;
			}
			
			for ( List<PriceInfo> entriesForItem : dataForRegion ) {

				final InventoryType item = 
					entriesForItem.get(0).getItemType();

				getFileForItem( r , item ).save( entriesForItem );
			}
		}
	}

	@Override
	protected void shutdownHook() {
		try {
			persistHook();
		} catch(IOException e) {
			throw new RuntimeException(e);
		} finally {
			this.priceInfoCache.clear();
			this.loadedPrices.clear();
		}
	}

	@Override
	public List<PriceInfo> get(MarketFilter filter, InventoryType itemType) 
	{

		if ( filter == null ) {
			throw new IllegalArgumentException("filter cannot be NULL");
		}

		if ( itemType == null ) {
			throw new IllegalArgumentException("itemType cannot be NULL");
		}

		List<PriceInfo> result = 
			super.get( filter , itemType );

		if ( result != null ) {
			return result;
		}

		if ( log.isDebugEnabled() ) {
			log.debug("internalGetPriceInfo(): [ 1st LVL CACHE MISS ] invType = "+itemType);
		}

		synchronized( loadedPrices ) {

			if ( isPriceInfoLoaded( filter.getRegion() , itemType ) ) {
				// price info already loaded but 1st level reported a miss anyway
				// -- must be unavailable 
				if ( log.isDebugEnabled() ) {
					log.debug("internalGetPriceInfo(): [ 2nd LVL CACHE MISS ] invType = "+itemType);
				}
				return null;
			}

			try {
				loadPriceInfo( filter.getRegion() , itemType );
			} 
			catch (IOException e) {
				log.error("get(): Failed to load price info",e);
				throw new RuntimeException(e);
			}
		}
		return super.get( filter , itemType );
	}

	@Override
	public List<PriceInfo> getPriceHistory(Region region, Type type,
			InventoryType item)
	{
		if ( ! isPriceInfoLoaded(region, item ) ) {
			try {
				loadPriceInfo( region, item );
			}
			catch (IOException e) {
				log.error("getPriceHistory(): Failed to load data for region "+
						region+",type "+type+" , item "+item,e);
				throw new RuntimeException(e);
			}
		}
		return super.getPriceHistory(region, type, item);
	}
	
	protected boolean isPriceInfoLoaded(Region r,InventoryType itemType) {

		Set<Long> itemIds = this.loadedPrices.get( r );
		if ( itemIds == null ) {
			return false;
		}
		return itemIds.contains( itemType.getTypeId() );
	}

	protected void rememberPriceInfoLoaded(Region r,InventoryType itemType) {
		Set<Long> itemIds = this.loadedPrices.get( r );
		if ( itemIds == null ) {
			itemIds = new HashSet<Long>();
			this.loadedPrices.put( r  , itemIds );
		}
		itemIds.add( itemType.getId() );
	}

	protected void loadPriceInfo(Region r,InventoryType itemType) throws IOException {

		if ( log.isDebugEnabled() ) {
			log.debug("loadPriceInfo(): Region = "+r+" , item = "+itemType );
		}

		final PriceInfoFile inputFile =
			getFileForItem( r , itemType );

		if ( inputFile.exists() ) {
			try {
				priceInfoCache.storePriceInfos( r , itemType , inputFile.load( getSystemClock() ) );
			}
			catch (ParseException e) {
				throw new IOException("Parsing file "+inputFile+" failed",e);
			} 
		}

		// remember we've tried to load prices for this region and item
		// anything that is not found afterwards must be really missing
		// (not just because it hasn't been loaded from disk yet)
		rememberPriceInfoLoaded( r , itemType );
	}

	PriceInfoFile getFileForItem(Region region , InventoryType type) {

		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		} 

		/* 
		 *  name = /<Base dir>/<Region ID>/<item ID>_priceinfo.csv
		 * ADJUST getAvailableItemIds() when changing the file-naming scheme !!! 
		 */
		
		final File result=
			new File( baseDir , region.getID().toString()+
					File.separatorChar+
					type.getTypeId().toString()+"_priceinfo.csv"); 

		if ( log.isDebugEnabled() ) {
			log.debug("getFileForItem(): region = "+region+", item = "+type+" => "+result.getAbsolutePath());
		}
		
		return new PriceInfoFile( region, type , result );
	}

	protected Set<Long> getAvailableItemIds(Region region) {

		File dir = new File(baseDir, region.getID().toString() );
		File[] files = dir.listFiles();
		
		final Set<Long> result =
			new HashSet<Long>();
		
		if ( files != null ) {
			
			/*
			 * ADJUST getFileForItem() when changing the file-naming scheme !!!
			 */
			for ( File file : dir.listFiles() ) {
				if ( ! file.isFile() ) {
					continue;
				}
				final String name= file.getName();
				if ( ! name.endsWith("_priceinfo.csv" ) ) {
					continue;
				}
				result.add(
					Long.parseLong( name.substring(0, name.indexOf( '_' ) ) ) 
				);
			}
		}

		return result;
	}

	public void setBaseDir(File baseDir) {
		if (baseDir == null) {
			throw new IllegalArgumentException(
			"baseDir cannot be NULL");
		}
		this.baseDir = baseDir;
	}

	@Override
	public Map<Long,List<PriceInfo>> getLatestPriceInfos(Region region, Type kind,
			Collection<InventoryType> items) 
			{

		final Map<Long,List<PriceInfo>> result =
			super.getLatestPriceInfos(region, kind, items);

		// look for price infos that have not been loaded from disk yet
		for ( InventoryType item : items ) 
		{
		
			if ( result.containsKey( item.getId() ) ) {
				continue; // ok, already loaded
			}
			
			final PriceInfoFile dataFile =
				getFileForItem( region , item );

			if ( dataFile.exists() ) {
				try {
					if ( log.isDebugEnabled() ) {
						log.debug("getLatestPriceInfos(): " +
								"Loading price infos for region "+region+", item "+item);
					}

					loadPriceInfo( region , item );

					final Map<Long,List<PriceInfo>> tmpMap =
						super.getLatestPriceInfos( region , kind , Arrays.asList( item ) );

					result.putAll( tmpMap );
				}
				catch (IOException e) {
					throw new RuntimeException("Failed to load price info",e);
				}
			}
		}

		return result;
			}

	public Map<Long , InventoryType> getAllKnownInventoryTypes(Region region,IInventoryTypeDAO dao) {

		final Map<Long,InventoryType> result =
			super.getAllKnownInventoryTypes( region , dao );

		for ( Long id : getAvailableItemIds( region ) ) {
			if ( ! result.containsKey( id ) ) {
				result.put( id , dao.fetch( id ) );
			}
		}
		return result;
	}
}
