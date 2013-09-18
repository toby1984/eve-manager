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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.xml.xpath.XPathExpression;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.codesourcery.eve.apiclient.utils.XMLParseHelper;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IPriceInfoStore;
import de.codesourcery.eve.skills.market.IPriceQueryCallback;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.util.IStatusCallback;
import de.codesourcery.eve.skills.util.Misc;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class EveCentralMarketDataProvider extends XMLParseHelper implements IMarketDataProvider , DisposableBean {

	public static final Logger LOG = Logger.getLogger(EveCentralMarketDataProvider.class);

	/**
	 * Time to wait before retrying 
	 * to obtain a missing price from eve central.
	 */
	public static final long RETRY_DELAY_ON_MISSING_PRICES = 2 * 60 * 60 * 1000; // 2 hours

	private ISystemClock systemClock;

	private final List<IPriceInfoChangeListener> listeners = new ArrayList<IPriceInfoChangeListener>();

	private IEveCentralClient eveCentralClient;

	private volatile boolean isOfflineMode;
	
	private IPriceInfoStore priceInfoStore;

	/**
	 * key = market filter
	 * value = Map<Inventory time ID,Unix timestamp of last attempt to fetch price from eve central>
	 */
	private final Map<MarketFilter,Map<Long,Long>> unknownPrices = new HashMap<MarketFilter,Map<Long,Long>>(); 	

	protected final class UPDATE_NONE_STRATEGY implements IUpdateStrategy {

		@Override
		public boolean requiresUpdate(InventoryType item,PriceInfo existingInfo) 
		{
			return false;
		}

		@Override
		public void merge(MarketFilter filter, PriceInfoQueryResult result,PriceInfo existing) {
		}

		@Override
		public String toString() { return "Update nothing."; }
	};

	private abstract class AbstractUpdateStrategy implements IUpdateStrategy {

		private final PriceInfo.Type filterType;

		protected AbstractUpdateStrategy(PriceInfo.Type filterType ) {
			if ( filterType == null ) {
				throw new IllegalArgumentException("filterType cannot be NULL");
			}
			this.filterType = filterType;
		}

		protected PriceInfo.Type getFilterType() {
			return filterType;
		}

		@Override
		public boolean requiresUpdate(InventoryType item,
				PriceInfo existingInfo) 
		{
			if ( existingInfo == null ) {
				return true;
			}
			return filterType.matches( existingInfo.getPriceType() );
		}

		@Override
		public final void merge(MarketFilter filter, PriceInfoQueryResult result,
				PriceInfo existing) 
		{
			result.merge( filter.getOrderType() , existing );			
		}

	}

	protected final class UpdateMissingOrOutdatedPrices extends AbstractUpdateStrategy {

		protected UpdateMissingOrOutdatedPrices(Type filterType) {
			super(filterType);
		}

		@Override
		public boolean requiresUpdate(InventoryType item,
				PriceInfo existingInfo) 
		{
			if ( ! super.requiresUpdate( item,existingInfo ) ) {
				return false;
			}

			// only try to update data that was
			// provided by the user.
			return existingInfo == null || isOutdated( existingInfo );
		}

		@Override
		public String toString() { return "Missing or outdated and not user-provided"; }

	};

	protected final class UpdateUserProvidedPrices extends AbstractUpdateStrategy {

		protected UpdateUserProvidedPrices(Type filterType) {
			super(filterType);
		}

		@Override
		public boolean requiresUpdate(InventoryType item,
				PriceInfo existingInfo) 
		{
			if ( ! super.requiresUpdate( item,existingInfo ) ) {
				return false;
			}

			// only try to update data that was
			// provided by the user.
			return existingInfo != null && existingInfo.isUserProvided();
		}

		@Override
		public String toString() { return "Missing or outdated and not user-provided"; }

	};

	protected final class DEFAULT_STRATEGY  extends AbstractUpdateStrategy {

		protected DEFAULT_STRATEGY(Type filterType) {
			super(filterType);
		}

		@Override
		public boolean requiresUpdate(InventoryType item, PriceInfo existingInfo) 
		{

			if ( ! super.requiresUpdate( item,existingInfo ) ) {
				return false;
			}

			// only try to update data that is either missing
			// or not provided by the user and outdated
			
			if ( LOG.isDebugEnabled() ) {
				if ( existingInfo == null ) {
					LOG.debug("requiresUpdate(): No price available for "+item);
				} else if ( ! existingInfo.isUserProvided() && isOutdated( existingInfo ) ) {
					LOG.debug("requiresUpdate(): Price for "+item+" is outdated and not user-provided");
				}
			}
			return existingInfo == null || ( ! existingInfo.isUserProvided() && isOutdated( existingInfo ) );
		}

		@Override
		public String toString() { return "Missing or outdated and not user-provided"; }

	};

	protected final class UPDATE_ALL_STRATEGY extends AbstractUpdateStrategy {

		protected UPDATE_ALL_STRATEGY(Type filterType) {
			super(filterType);
		}

		@Override
		public boolean requiresUpdate(InventoryType item,PriceInfo existingInfo) 
		{
			return true;
		}

		@Override
		public String toString() { return "Update all kinds of prices"; }		

	};

	protected final class UPDATE_MISSING_STRATEGY extends AbstractUpdateStrategy {

		protected UPDATE_MISSING_STRATEGY(Type filterType) {
			super(filterType);
		}

		@Override
		public boolean requiresUpdate(InventoryType item,
				PriceInfo existingInfo) 
		{
			if ( ! super.requiresUpdate( item,existingInfo ) ) {
				return false;
			}
			return existingInfo == null ;
		}

		@Override
		public String toString() { return "Update only missing prices"; }	

	};

	private final class UPDATE_OUTDATED_STRATEGY extends AbstractUpdateStrategy {

		protected UPDATE_OUTDATED_STRATEGY(Type filterType) {
			super(filterType);
		}

		@Override
		public boolean requiresUpdate(InventoryType item,PriceInfo existingInfo) 
		{
			if ( ! super.requiresUpdate( item,existingInfo ) ) {
				return false;
			}
			return existingInfo != null && isOutdated( existingInfo );
		}

		@Override
		public String toString() { return "Update only outdated prices"; }			

	};		

	public EveCentralMarketDataProvider() {
	}

	public void setSystemClock(ISystemClock systemClock) {
		if (systemClock == null) {
			throw new IllegalArgumentException("systemClock cannot be NULL");
		}
		this.systemClock = systemClock;
	}

	@Override
	public PriceInfoQueryResult getPriceInfo(MarketFilter filter,IPriceQueryCallback callback,InventoryType type) throws PriceInfoUnavailableException 
	{
		final PriceInfoQueryResult result = getPriceInfos(filter, callback ,  type ).get( type );

		if ( result == null ) {
			throw new PriceInfoUnavailableException("No price info" , type );
			// return new PriceInfoQueryResult( type );
		}
		return result;
	}

	/*
<!-- This is the new API :-) -->
−
<evec_api version="2.0" method="marketstat_xml">
−
<marketstat>
−
<type id="34">
−
<all>
<volume>41337268582</volume>
<avg>36.8872779152</avg>
<max>11155.08</max>
<min>0.3</min>
<stddev>552.601683331</stddev>
<median>4.15</median>
</all>
−
<buy>
<volume>23271663605</volume>
<avg>3.96983095868</avg>
<max>4.3</max>
<min>0.3</min>
<stddev>0.572456683074</stddev>
<median>3.87</median>
</buy>
−
<sell>
<volume>18065604977</volume>
<avg>4.40662120896</avg>
<max>11155.08</max>
<min>3.9</min>
<stddev>640.33788886</stddev>
<median>4.18</median>
</sell>
</type>		 
	 */

	protected PriceInfoQueryResult getCachedEntry( MarketFilter filter , InventoryType type) 
	{
		final List<PriceInfo> data = priceInfoStore.get( filter, type );
		return new PriceInfoQueryResult( type , data );
	}

	protected void storeCacheEntry(PriceInfo entry) 
	{
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("storeCacheEntry(): entry = "+entry);
		}
		priceInfoStore.save( entry );
	}

	protected boolean isPriceMissingOnEveCentral( MarketFilter filter,InventoryType type) {
		final Map<Long, Long> missingItemsById = unknownPrices.get( filter );
		if ( missingItemsById == null ) {
			return false;
		}
		if ( missingItemsById.get( type.getId() ) != null) {
			return true;
		}
		return false;
	}

	protected void forgetPriceMissingOnEveCentral( MarketFilter filter,InventoryType type) {
		final Map<Long, Long> missingItemsById = unknownPrices.get( filter );
		if ( missingItemsById == null ) {
			return;
		}
		missingItemsById.remove( type.getId() );
	}

	protected void rememberPriceMissingOnEveCentral( MarketFilter filter,InventoryType type) {

		if ( LOG.isDebugEnabled() ) {
			LOG.debug("rememberPriceMissingOnEveCentral(): item "+type+" , filter "+filter);
		}

		Map<Long, Long> missingItemsById = unknownPrices.get( filter );
		if ( missingItemsById == null ) {
			missingItemsById= new HashMap<Long, Long>();
			unknownPrices.put( filter , missingItemsById );
		}
		missingItemsById.put(type.getId() , System.currentTimeMillis() );
	}

	protected boolean mayQueryAgainForMissingPrice(MarketFilter filter,InventoryType type) {

		final Map<Long, Long> missingItemsById = unknownPrices.get( filter );
		if ( missingItemsById == null ) {
			return true;
		}

		final Long timestamp =
			missingItemsById.get( type.getId() );

		if (  timestamp == null ) {
			return true;
		}
		final long delta = System.currentTimeMillis() - timestamp;
		return delta >= RETRY_DELAY_ON_MISSING_PRICES;
	}

	private void runOnEDT(Runnable r) {
		Misc.runOnEventThread( r );
	}

	@Override
	public Map<InventoryType , PriceInfoQueryResult> getPriceInfos(
			final MarketFilter filter, 
			final IPriceQueryCallback callback,
			final InventoryType... items
	)
	throws PriceInfoUnavailableException 
	{
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("getPriceInfos(): filter = "+filter+", items = "+items);
		}

		if ( ArrayUtils.isEmpty( items ) ) {
			return Collections.emptyMap();
		}

		final AtomicReference<Map<InventoryType, PriceInfoQueryResult>> resultHolder =
			new AtomicReference<Map<InventoryType, PriceInfoQueryResult>>(
					new ConcurrentHashMap<InventoryType, PriceInfoQueryResult>() 
			);

		final IUpdateStrategy updateStrategy = createUpdateStrategy( filter.getUpdateMode() , filter.getOrderType()  );

		final Vector<NameValuePair> params = new Vector<NameValuePair>();

		/*
		 * NEEDS to be run on the EDT since Hibernate
		 * lazy-fetching might kick in and
		 * the Hibernate session is confined to the EDT.
		 */
		runOnEDT(  new Runnable() {

			@Override
			public void run()
			{
				if ( LOG.isDebugEnabled() ) {
					LOG.debug("getPriceInfos(): update_strategy = "+updateStrategy);
				}

				for ( InventoryType t : items ) {

					// make sure we don't query over and over
					// for prices that are unavailable anyway
					if ( isPriceMissingOnEveCentral( filter , t ) ) 
					{
						if ( ! mayQueryAgainForMissingPrice( filter , t ) ) 
						{
							if ( LOG.isDebugEnabled() ) {
								LOG.debug("getPriceInfos(): " +
										"Price for "+t+" " +
										"unavailable on eve-central , filter "+filter);
							}
							continue;
						}

						if ( LOG.isDebugEnabled() ) {
							LOG.debug("getPriceInfos(): [ retrying ] " +
									"Price for "+t+" " +
									"unavailable on eve-central , filter "+filter);
						}				
					}

					final PriceInfoQueryResult cached =  getCachedEntry( filter , t );

					resultHolder.get().put( t , cached );

					if ( LOG.isDebugEnabled() ) {

						if ( cached.isEmpty() ) { 
							LOG.debug("getPriceInfos(): [ NOT CACHED ] type = "+t.getId()+" , name = "+t.getName() );
						} else {
							LOG.debug("getPriceInfos(): [ CACHE HIT ] "+cached );
						}
					}

					final boolean requiresUpdate;
					switch ( filter.getOrderType() ) 
					{
						case BUY:
							requiresUpdate=updateStrategy.requiresUpdate(t, cached.hasBuyPrice() ? cached.buyPrice() : null );
							break;
						case SELL:
							requiresUpdate=updateStrategy.requiresUpdate(t, cached.hasSellPrice() ? cached.sellPrice() : null );
							break;
						case ANY:
							requiresUpdate= ( updateStrategy.requiresUpdate(t, cached.hasBuyPrice()  ? cached.buyPrice() : null ) || 
									          updateStrategy.requiresUpdate(t, cached.hasSellPrice() ? cached.sellPrice() : null ) );
							break;
						default:				
							throw new RuntimeException("Unhandled switch/case: "+filter.getOrderType() );
					}

					if ( LOG.isDebugEnabled() ) {
						LOG.debug("getPriceInfos(): [ "+updateStrategy+"] requires_update => "+requiresUpdate+" , type="+t.getName());
					}

					if ( requiresUpdate ) {
						params.add( new BasicNameValuePair("typeid" , t.getId().toString() ) );
					} 
				}
			}
		} );		

		if ( params.isEmpty() || isOfflineMode() ) { // all entries served from cache
			return resultHolder.get();
		}

		addFilterToRequest( params , filter );

		/*
		 * Query data from eve central
		 */
		final String responseXmlFromServer = eveCentralClient.sendRequestToServer( params );
		final AtomicReference<String> xml = new AtomicReference<String>(  responseXmlFromServer );

		/*
		 * NEEDS to be run on the EDT since Hibernate
		 * lazy-fetching might kick in and
		 * the Hibernate session is confined to the EDT.
		 */
		return runOnEventThread( new PriceCallable() {

			public Map<InventoryType , PriceInfoQueryResult> call() throws PriceInfoUnavailableException 
			{
				final Map<InventoryType, PriceInfoQueryResult> realResult = resultHolder.get();

				final Map<Long , List<PriceInfo>> result =  parsePriceInfo( filter , xml.get() );

				// group prices by item types

				List<PriceInfo> updated = new ArrayList<>();
				try 
				{
					for ( InventoryType type : items ) 
					{
						List<PriceInfo> info = result.get( type.getId() );

						if ( info == null || info.isEmpty() ) 
						{ 
							// failed to fetch data, query user 
							rememberPriceMissingOnEveCentral( filter , type );
							info = queryPriceFromUser(filter, callback, type );
						}

						forgetPriceMissingOnEveCentral(filter , type );

						for( PriceInfo dataFromServer : info ) 
						{
							dataFromServer.setRegion( filter.getRegion() );
							dataFromServer.setTimestamp( new EveDate( systemClock) );
							dataFromServer.setInventoryType(type );

							final PriceInfoQueryResult cachedResult = realResult.get( type );

							if ( LOG.isDebugEnabled() ) {
								LOG.debug("getPriceInfos(): from server: "+dataFromServer+" , cached="+cachedResult);
							}

							PriceInfo existing;
							switch( filter.getOrderType() ) 
							{
							case BUY:
								existing = cachedResult.hasBuyPrice() ? cachedResult.buyPrice() : null;
								if ( updateStrategy.requiresUpdate( type , existing ) ) {
									LOG.debug("getPriceInfos(): merging buy price.");
									realResult.put( type , cachedResult.merge( filter.getOrderType() , dataFromServer ) );
									storeCacheEntry( dataFromServer );
									updated.add( dataFromServer );
								}
								break;
							case SELL:
								existing = cachedResult.hasSellPrice() ? cachedResult.sellPrice() : null;
								if ( updateStrategy.requiresUpdate( type , existing ) ) {
									LOG.debug("getPriceInfos(): merging sell price.");
									realResult.put( type , cachedResult.merge( filter.getOrderType() , dataFromServer ) );
									storeCacheEntry( dataFromServer );
									updated.add( dataFromServer );
								}
								break;		
							case ANY:
								existing = cachedResult.hasBuyPrice() ? cachedResult.buyPrice() : null;
								if ( updateStrategy.requiresUpdate( type , existing ) ) {
									LOG.debug("getPriceInfos(): merging buy price.");
									realResult.put( type , cachedResult.merge( PriceInfo.Type.BUY , dataFromServer ) );
									storeCacheEntry( dataFromServer );
									updated.add( dataFromServer );
								}
								existing = cachedResult.hasSellPrice() ? cachedResult.sellPrice() : null;
								if ( updateStrategy.requiresUpdate( type , existing ) ) {
									LOG.debug("getPriceInfos(): merging sell price.");
									realResult.put( type , cachedResult.merge( PriceInfo.Type.SELL , dataFromServer ) );
									storeCacheEntry( dataFromServer );
									updated.add( dataFromServer );
								}						
								break;
							default:
								throw new RuntimeException("Unhandled switch/case: "+filter.getOrderType());
							}
						} 
					}
				} finally {
					fireItemPriceChanged( updated );
				}
				return realResult;
			}
		});
	}

	private interface PriceCallable {

		public Map<InventoryType , PriceInfoQueryResult> call() throws PriceInfoUnavailableException;
	}

	private static Map<InventoryType , PriceInfoQueryResult>  runOnEventThread(final PriceCallable r) throws PriceInfoUnavailableException
	{
		if (SwingUtilities.isEventDispatchThread()) 
		{
			return r.call();
		} 
		
		final AtomicReference<Map<InventoryType , PriceInfoQueryResult> > result = new AtomicReference<Map<InventoryType , PriceInfoQueryResult>>();
		try {
			SwingUtilities.invokeAndWait(  new Runnable() {

						@Override
						public void run()
						{
							try {
								result.set( r.call() );
							}
							catch (PriceInfoUnavailableException e) {
								throw new RuntimeException(e);
							}
						}
					});
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (InvocationTargetException e) 
		{
			Throwable wrapped = e.getTargetException();
			if ( wrapped instanceof RuntimeException) {
				if ( wrapped.getCause() instanceof PriceInfoUnavailableException) {
					throw (PriceInfoUnavailableException) wrapped.getCause();
				}
				throw (RuntimeException) wrapped;
			} else if ( e.getTargetException() instanceof Error ) {
				throw (Error) wrapped;
			}
			throw new RuntimeException(e.getTargetException());
		}
		return result.get();
	}

	private List<PriceInfo> queryPriceFromUser(MarketFilter filter,IPriceQueryCallback callback, InventoryType type) throws PriceInfoUnavailableException 
	{
		if ( callback != null ) {
			return  callback.getPriceInfo( filter , "Please enter cost information:", type );
		} 
		LOG.error("getPriceInfos(): Unable to obtain price for item "+type.getId() );
		throw new PriceInfoUnavailableException("Unable to obtain price" , type );
	}

	private void addFilterToRequest(List<NameValuePair> params, MarketFilter filter) {

		if ( filter.isFilterByMinQuantity() ) {
			params.add( new BasicNameValuePair("minQ" , Long.toString( filter.getMinQuantity() ) ) );
		}

		params.add( new BasicNameValuePair("regionlimit" , Long.toString( filter.getRegion().getID() ) ) );
	}

	protected Map<Long , List<PriceInfo>> parsePriceInfo(MarketFilter filter, String xmlResponse) 
	{
		if ( LOG.isDebugEnabled() ) {
			LOG.debug("parsePriceInfo(): filter = "+filter);
		}

		final Document document =  parseXML( xmlResponse );

		final XPathExpression TYPE_NODES = super.compileXPathExpression( "/evec_api/marketstat/type");

		final Map<Long,List<PriceInfo>> result = new HashMap<Long,List<PriceInfo>>();

		for ( Element node : super.selectElements( document , TYPE_NODES ) ) 
		{
			final Long typeId = getLongAttributeValue(node , "id" );

			Element itemNode;
			switch( filter.getOrderType() ) {
				case BUY:
					itemNode = getChild( node , "buy" );
					addToMap( result , parsePriceInfo( typeId , PriceInfo.Type.BUY, itemNode ) );
					break;
				case SELL:
					itemNode = getChild( node , "sell" );
					addToMap( result , parsePriceInfo( typeId , PriceInfo.Type.SELL , itemNode ) );
					break;
				case ANY:
					itemNode = getChild( node , "buy" );
					addToMap( result , parsePriceInfo( typeId , PriceInfo.Type.BUY , itemNode ) );
					itemNode = getChild( node , "sell" );
					addToMap( result , parsePriceInfo( typeId , PriceInfo.Type.SELL , itemNode ) );
					break;
				default:
					throw new RuntimeException("Unhandled order type: "+filter.getOrderType() );
			}
		}
		return result;
	}

	protected static void addToMap(Map<Long,List<PriceInfo>> map , PriceInfoWithId info) 
	{
		if ( info.getAveragePrice() <= 0 ) {
			return;
		}

		List<PriceInfo> existing = map.get( info.getTypeID() );

		if ( existing == null ) {
			existing = new ArrayList<PriceInfo>();
			map.put( info.getTypeID() , existing );
		}
		existing.add( info );
	}

	protected static class PriceInfoWithId extends PriceInfo {

		private Long itemType;

		protected PriceInfoWithId(Long itemType , Type infoType) {
			super(infoType,Source.EVE_CENTRAL );
			if ( itemType == null ) {
				throw new IllegalArgumentException("itemType cannot be NULL");
			}
			this.itemType = itemType;
		}

		public Long getTypeID() {
			return itemType;
		}
	}

	private PriceInfoWithId parsePriceInfo(Long itemTypeId , PriceInfo.Type priceType , Element rootNode) {
		/*
<all>
<volume>41337268582</volume>
<avg>36.8872779152</avg>
<max>11155.08</max>
<min>0.3</min>
<stddev>552.601683331</stddev>
<median>4.15</median>
</all>		 
		 */

		final PriceInfoWithId result = new PriceInfoWithId( itemTypeId , priceType );
		result.setAveragePrice( parseISKValue( getChildValue( rootNode , "median" ) ) );
		result.setMinPrice( parseISKValue( getChildValue( rootNode , "min" ) ) );
		result.setMaxPrice( parseISKValue( getChildValue( rootNode , "max" ) ) );
		return result;
	}

	protected long parseISKValue(String value) {

		final Double dValue = Double.parseDouble( value );
		return Math.round( dValue * 100.0f );
	}

	public void setPriceInfoStore(IPriceInfoStore priceInfoStore) {
		this.priceInfoStore = priceInfoStore;
	}

	@Override
	public void updatePriceInfo(MarketFilter filter, List<InventoryType> items, IPriceQueryCallback callback, IUpdateStrategy updateStrategy) throws PriceInfoUnavailableException 
			{
		if ( filter == null ) {
			throw new IllegalArgumentException("filter cannot be NULL");
		}

		if (items == null) {
			throw new IllegalArgumentException("items cannot be NULL");
		}

		if ( updateStrategy == null ) {
			throw new IllegalArgumentException("updateStrategy cannot be NULL");
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debug("updatePriceInfo(): filter = "+filter+", "+
					" , items = "+items+" , update_strategy = "+updateStrategy);
		}

		for ( InventoryType type : items ) {

			if ( LOG.isTraceEnabled() ) {
				LOG.debug("updatePriceInfo(): querying price for "+type);
			}			
			getPriceInfo( filter , callback , type );
		}

			}

	@Override
	public void addChangeListener(IPriceInfoChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (listeners) {
			listeners.add( listener );
		}
	}

	protected void fireItemPriceChanged(PriceInfo info) {
		fireItemPriceChanged( Collections.singleton( info ) );
	}
	
	protected void fireItemPriceChanged(Collection<PriceInfo> infos) 
	{
		final Map<Region,Set<InventoryType>> infosByRegion = new HashMap<>();
		
		for ( PriceInfo i : infos ) {
			Set<InventoryType> list = infosByRegion.get(i.getRegion());
			if ( list == null ) {
				list = new HashSet<>();
				infosByRegion.put( i.getRegion() , list );
			}
			list.add( i.getItemType() );
		}

		for ( Entry<Region, Set<InventoryType>> entry : infosByRegion.entrySet() ) 
		{
			fireItemPriceChanged( entry.getKey() , entry.getValue() );
		}
	}	

	protected void fireItemPriceChanged(Region region,Set<InventoryType> items ) 
	{
		LOG.debug("fireItemPriceChanged(): items = "+items );

		final List<IPriceInfoChangeListener> cloned;
		synchronized (listeners) {
			cloned = new ArrayList<IPriceInfoChangeListener>( listeners );
		}

		for ( IPriceInfoChangeListener listener : cloned) {
			listener.priceChanged( this , region , items );
		}
	}

	@Override
	public void removeChangeListener(IPriceInfoChangeListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (listeners) {
			listeners.add( listener );
		}
	}

	protected boolean isOutdated(PriceInfo info) {
		return PriceInfoQueryResult.isOutdated( info , systemClock );
	}

	@Override
	public IUpdateStrategy createUpdateStrategy(UpdateMode mode,PriceInfo.Type typeFilter) {

		switch( mode ) 
		{
			case DEFAULT:
				return new DEFAULT_STRATEGY( typeFilter );
			case UPDATE_ALL:
				return new UPDATE_ALL_STRATEGY( typeFilter );
			case UPDATE_MISSING:
				return new UPDATE_MISSING_STRATEGY( typeFilter );
			case UPDATE_NONE:
				return new UPDATE_NONE_STRATEGY();
			case UPDATE_OUTDATED:
				return new UPDATE_OUTDATED_STRATEGY( typeFilter );
			case UPDATE_USERPROVIDED:
				return new UpdateUserProvidedPrices( typeFilter );
			case UPDATE_MISSING_OR_OUTDATED:
				return new UpdateMissingOrOutdatedPrices( typeFilter );
			default:
				throw new RuntimeException("Unhandled switch/case: "+mode);
		}
	}

	@Override
	public void addStatusCallback(IStatusCallback callback)
	{
		this.eveCentralClient.addStatusCallback( callback );
	}

	public void setEveCentralClient(IEveCentralClient client) {
		if ( client == null ) {
			throw new IllegalArgumentException("client cannot be NULL");
		}
		this.eveCentralClient = client;
	}
	
	@Override
	public void dispose()
	{
		try {
			this.priceInfoStore.persist();
		}
		catch (IOException e) {
			LOG.error("dispose(): Failed to persist price info store ?");
		}
		this.eveCentralClient.dispose();
	}

	@Override
	public void removeStatusCallback(IStatusCallback callback)
	{
		this.eveCentralClient.removeStatusCallback( callback );		
	}

	@Override
	public void destroy() throws Exception
	{
		dispose();
	}

	@Override
	public boolean isOfflineMode() {
		return isOfflineMode;
	}

	@Override
	public void setOfflineMode(boolean yesNo) {
		LOG.info("setOfflineMode(): offline = "+yesNo);
		this.isOfflineMode = yesNo;
	}

	@Override
	public void store(Collection<PriceInfo> info) 
	{
		for ( PriceInfo i : info ) {
			store( i );
		}
	}

	@Override
	public void store(PriceInfo info) 
	{
		this.priceInfoStore.save(info);
		fireItemPriceChanged( info );
	}
	
	public Map<Long,InventoryType>  getAllKnownInventoryTypes(Region region,IInventoryTypeDAO dao) {
		return this.priceInfoStore.getAllKnownInventoryTypes(region, dao);
	}
}