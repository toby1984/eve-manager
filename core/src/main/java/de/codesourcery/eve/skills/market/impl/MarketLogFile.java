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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.MarketLogEntry;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class MarketLogFile {

	private static final Logger log = Logger.getLogger(MarketLogFile.class);
	
	public interface IMarketLogVisitor {
		
		public void visit(MarketLogEntry entry);
	}
	
	public interface IMarketLogFilter {
		
		public static final IMarketLogFilter NOP_FILTER = new IMarketLogFilter() {

			@Override
			public boolean includeInResult(MarketLogEntry entry) {
				return true;
			}
		};
		
		public boolean includeInResult(MarketLogEntry entry);
	}

	private static final Comparator<MarketLogEntry> DATE_SORTER =
		new Comparator<MarketLogEntry>() {

		@Override
		public int compare(MarketLogEntry o1, MarketLogEntry o2) {
			return o1.getIssueDate().compareTo( o2.getIssueDate() );
		}
	};
	
	private static final Comparator<PriceInfo> PRICEINFO_DATE_SORTER =
		new Comparator<PriceInfo>() {

		@Override
		public int compare(PriceInfo o1, PriceInfo o2) {
			return o2.getTimestamp().compareTo( o1.getTimestamp() );
		}
	};
	
	private List<MarketLogEntry> entries;
	private final InventoryType type;
	private final Region region;
	
	public MarketLogFile(List<MarketLogEntry> entries, InventoryType type,
			Region region) 
	{
		
		if ( entries == null ) {
			throw new IllegalArgumentException("entries cannot be NULL");
		}
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		this.entries = new ArrayList<MarketLogEntry>( entries );
		Collections.sort( this.entries , DATE_SORTER );
		this.type = type;
		this.region = region;
	}
	
	/**
	 * Applies a filter, removing ALL entries that are not matched by 
	 * the filter.
	 * 
	 * @param filter
	 */
	public void applyFilter(IMarketLogFilter filter) {
		
		for ( Iterator<MarketLogEntry> it = this.entries.iterator() ; it.hasNext() ; ) {
			if ( ! filter.includeInResult( it.next() ) ) {
				it.remove();
			}
		}
	}
	
	public int removeEntriesOlderThan(EveDate date,PriceInfo.Type type) {
		
		int removed = 0;
		for (Iterator<MarketLogEntry> it = entries.iterator(); it.hasNext();) {
			final MarketLogEntry entry = it.next();
			if ( type.matches( entry.getType() ) && entry.getIssueDate().before( date ) ) {
				it.remove();
				removed++;
			}
		}
		return removed;
	}
	
	public double getMinPrice(final PriceInfo.Type type,final IMarketLogFilter filter) {
		
		final double[] minPrice =
			new double[] { Double.MAX_VALUE };
		
		visit( new IMarketLogVisitor() {

			@Override
			public void visit(MarketLogEntry entry) {
				if ( entry.getType().matches( type ) && filter.includeInResult( entry ) ) {
					if ( entry.getPrice() < minPrice[0] ) {
						minPrice[0] = entry.getPrice();
					}
				}
			}} );
		
		if ( minPrice[0] == Double.MAX_VALUE ) {
			return 0.0;
		}
		return minPrice[0];
	}
	
	public double getTradedVolume(final PriceInfo.Type type , final IMarketLogFilter filter) {
		
		if ( type == Type.ANY ) {
			throw new IllegalArgumentException("Traded volume makes no sense with order type "+type);
		}
		
		final double[] volume =
			new double[1];
		
		visit( new IMarketLogVisitor() {

			@Override
			public void visit(MarketLogEntry entry) {
				if ( entry.getType().matches( type ) && filter.includeInResult( entry ) ) {
					volume[0] += entry.getRemainingVolume();
				}
			}} );
		
		return volume[0];
	}
	
	public double getAveragePrice(final PriceInfo.Type type,final IMarketLogFilter filter) {
		
		final double[] avgPrice =
			new double[1];
		
		final double[] entryCount =
			new double[1];
		
		visit( new IMarketLogVisitor() {

			@Override
			public void visit(MarketLogEntry entry) {
				if ( entry.getType().matches( type ) && filter.includeInResult( entry ) ) {
					avgPrice[0] += entry.getPrice();
					entryCount[0] += 1;
				}
			}} );
		
		if ( entryCount[0] == 0 ) {
			return 0.0;
		}
		
		return avgPrice[0] / entryCount[0];
	}
	
	public double getStandardDeviation(final PriceInfo.Type type,final IMarketLogFilter filter) {
		
		/*
		 * For each data point:
		 * 
		 * 1. calculate the difference between this point and the mean value
		 * 2. square the difference (^2)
		 * 
		 * Finally:
		 * 
		 * - calculate the average of these values and
		 * take the square root
		 */
		
		final double average = 
			getAveragePrice( type , filter );
		
		final double[] deltaSum =
			new double[1];
		
		final int[] orderCount =
			new int[1];
		
		visit( new IMarketLogVisitor() {

			@Override
			public void visit(MarketLogEntry entry) {
				if ( entry.getType().matches( type ) && filter.includeInResult( entry ) ) {
					final double delta = entry.getPrice() - average;
					deltaSum[0] += ( Math.pow( delta , 2.0d ) );
					orderCount[0] += 1;
				}
			}} );
		
		if ( orderCount[0] == 0 ) {
			return 0;
		}
		
		deltaSum[0] /= (double) orderCount[0];
		return Math.sqrt( deltaSum[0] );
	}
	
	public double getMaxPrice(final PriceInfo.Type type,final IMarketLogFilter filter) {
		
		final double[] maxPrice =
			new double[] { Double.MIN_VALUE };
		
		visit( new IMarketLogVisitor() {

			@Override
			public void visit(MarketLogEntry entry) {
				if ( entry.getType().matches( type ) && filter.includeInResult( entry ) ) {
					if ( entry.getPrice() > maxPrice[0] ) {
						maxPrice[0] = entry.getPrice();
					}
				}
			}} );
		
		if ( maxPrice[0] == Double.MIN_VALUE ) {
			return 0.0;
		}
		
		return maxPrice[0];
	}
	
	public void visit(IMarketLogVisitor visitor) {

		if (visitor == null) {
			throw new IllegalArgumentException("visitor cannot be NULL");
		}
		
		for ( MarketLogEntry e : entries ) {
			visitor.visit( e );
		}
	}
	
	public List<MarketLogEntry> getOrders(IMarketLogFilter filter) {
		
		if (filter == null) {
			throw new IllegalArgumentException("filter cannot be NULL");
		}
		
		final List<MarketLogEntry> result = 
			new ArrayList<MarketLogEntry>();
		for ( MarketLogEntry e : entries ) {
			if ( filter.includeInResult( e ) ) {
				result.add( e );
			}
		}
		return Collections.unmodifiableList( result);
	}
	
	public int getOrderCount(PriceInfo.Type type) {
		
		int result = 0;
		for ( MarketLogEntry entry : entries ) {
			if ( type.matches( entry.getType() ) ) {
				result+= entry.getOrderCount();
			}
		}
		return result;
	}

	public List<MarketLogEntry> getOrders() {
		return Collections.unmodifiableList( entries );
	}

	public InventoryType getInventoryType() {
		return type;
	}

	public Region getRegion() {
		return region;
	}
	
	public boolean isEmpty() {
		return entries.isEmpty();
	}
	
	/**
	 * Aggregates market log entries by day.
	 * 
	 * @param entries
	 * @return aggregated market order data, at most one buy and one sell
	 * order entry will exist for any given day
	 */
	public List<PriceInfo> getAggregatedOrders(ISystemClock clock) {
		return getAggregatedOrders(MarketLogFile.IMarketLogFilter.NOP_FILTER,clock);
	}
	
	/**
	 * Aggregates market log entries by day.
	 * 
	 * @param entries
	 * @return aggregated market order data, at most one buy and one sell
	 * order entry will exist for any given day
	 */
	public List<PriceInfo> getAggregatedOrders(IMarketLogFilter filter,ISystemClock clock) {
		
		if (filter == null) {
			throw new IllegalArgumentException("filter cannot be NULL");
		}
		
		if ( clock == null ) {
			throw new IllegalArgumentException("clock cannot be NULL");
		}
		
		final Map<EveDate, List<MarketLogEntry>> sellOrdersByDay = 
			new HashMap<EveDate,List<MarketLogEntry>>();
		
		final Map<EveDate, List<MarketLogEntry>> buyOrdersByDay = 
			new HashMap<EveDate,List<MarketLogEntry>>();
		
		if ( log.isDebugEnabled() ) {
			log.debug("getAggregatedOrders(): Merging "+this.entries.size()+" prices");
		}
		
		for ( MarketLogEntry entry : entries ) {

			if ( ! filter.includeInResult(entry ) ) {
				continue;
			}
			
			final Map<EveDate, List<MarketLogEntry>> map= 
				entry.isBuyOrder() ? buyOrdersByDay : sellOrdersByDay;
			
			final EveDate strippedDate =
				entry.getIssueDate().stripTime( clock );
			
			List<MarketLogEntry> existing =
				map.get( strippedDate );
			
			if ( log.isTraceEnabled() ) {
				log.trace("getAggregatedOrders(): Got date "+strippedDate);
			}
			
			if ( existing == null ) {
				existing = new ArrayList<MarketLogEntry>();
				map.put( strippedDate  , existing );
			}
			
			existing.add( entry );
		}
		
		final ArrayList<PriceInfo> result = 
			new ArrayList<PriceInfo>();
		
		for ( List<MarketLogEntry> ordersOfOneDay : sellOrdersByDay.values() ) {
			result.add( mergeOneDay( ordersOfOneDay , clock ) );
		}
		
		for ( List<MarketLogEntry> ordersOfOneDay : buyOrdersByDay.values() ) {
			result.add( mergeOneDay( ordersOfOneDay , clock ) );
		}
		
		Collections.sort( result , PRICEINFO_DATE_SORTER );
		
		return result;
	}
	
	public static PriceInfo toPriceInfo(MarketLogEntry entry,
			Region region,
			InventoryType type,
			ISystemClock clock) 
	{
	
		if ( clock == null ) {
			throw new IllegalArgumentException("clock cannot be NULL");
		}
		
		if (entry == null) {
			throw new IllegalArgumentException("entry cannot be NULL");
		}
		
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		PriceInfo result = new PriceInfo(
				entry.getType(),
				type,
				Source.MARKET_LOG);
		
		result.setRegion( region );
		result.setTimestamp( entry.getIssueDate() );
		result.setOrderCount( entry.getOrderCount() );
		result.setAveragePrice( Math.round(100.0 * entry.getPrice() ) );
		result.setVolume( Math.round( entry.getVolume() ) );
		result.setRemainingVolume( Math.round( entry.getRemainingVolume() ) );
		result.setOrderId( entry.getOrderId() );
		
		return result;
	}
	
	protected PriceInfo mergeOneDay(List<MarketLogEntry> entries,ISystemClock clock) {
	
		final Iterator<MarketLogEntry> it = entries.iterator();
		
		final MarketLogEntry result = it.next();
		
		if ( log.isTraceEnabled() ) {
			log.trace("mergeOneDay(): Initial = "+result); 
		}
		double minPrice = result.getPrice();
		double maxPrice = result.getPrice();
		
		while ( it.hasNext() ) {
			final MarketLogEntry e = it.next();
			
			if ( log.isTraceEnabled() ) {
				log.trace("mergeOneDay(): Merging with "+e);
			}
			
			if ( minPrice > e.getPrice() ) {
				minPrice = e.getPrice();
			}
			
			if ( maxPrice < e.getPrice() ) {
				maxPrice = e.getPrice();
			}

			result.mergeWith( e );
			if ( log.isTraceEnabled() ) {
				log.trace("mergeOneDay(): result = "+result);
			}			
		}
		
		result.mergingFinished();
		
		PriceInfo info = 
			toPriceInfo( result , getRegion() , getInventoryType() , clock );

		info.setMinPrice( Math.round( minPrice * 100.0d) );
		info.setMaxPrice( Math.round( maxPrice * 100.0d) );
		
		if ( log.isTraceEnabled() ) {
			log.trace("mergeOneDay(): final result = "+ReflectionToStringBuilder.reflectionToString( info ) );
		}			
		return info;
	}

	public EveDate getStartDate() {
		
		EveDate result =
			null;
		
		for ( MarketLogEntry entry : this.entries ) {
			if ( result == null || entry.getIssueDate().before( result ) ) {
				result = entry.getIssueDate();
			}
		}
		return result;
	}

	public EveDate getEndDate() {
		
		EveDate result =
			null;
		
		for ( MarketLogEntry entry : this.entries ) {
			if ( result == null || entry.getIssueDate().after( result ) ) {
				result = entry.getIssueDate();
			}
		}
		return result;
	}

	public int size()
	{
		return entries.size();
	}
} 
