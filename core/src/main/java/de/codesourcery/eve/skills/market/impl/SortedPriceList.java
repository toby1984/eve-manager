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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

public final class SortedPriceList implements Iterable<PriceInfo> {

	public static final Logger log = Logger.getLogger(SortedPriceList.class);
	
	/*
	 * hint: List entries are always sorted descending
	 * by timestamp (latest entries come first).
	 */
	protected final List<PriceInfo> priceInfos =
		new ArrayList<PriceInfo>();

	public synchronized void store(PriceInfo changedData) {
		
		if (changedData == null) {
			throw new IllegalArgumentException("info cannot be NULL");
		}
		
		if ( priceInfos.isEmpty() ) {
			if ( log.isTraceEnabled() ) {
				log.trace("store(): [ empty list ] Adding priceinfo "+changedData);
			}			
			priceInfos.add( changedData );
			return;
		}
		
		int index=-1;
		for ( PriceInfo existing : priceInfos ) 
		{
			index++;
			
			if ( existing == changedData ) {
				return; // already in list
			}

			if ( ! existing.hasType( changedData.getPriceType() ) || 
				 ! existing.hasRegion( changedData.getRegion() ) )
			{
				if ( log.isTraceEnabled() ) {
					log.trace("store(): Not matched: "+existing);
				}				
				continue;
			}

			if ( existing.isFromSameDayAs( changedData ) ) {
				// replace existing entry from same day - 
				// we don't keep more than 1 price per day for an item
				if ( changedData.isNewerThan( existing ) ) {
					if ( log.isTraceEnabled() ) {
						log.trace("store(): [ replace at index "+index+" ] existing = "+existing+", other ="+changedData);
					}					
					priceInfos.remove(index);
					priceInfos.add( index , changedData );
				} else if ( log.isTraceEnabled() ) {
					log.trace("store(): [ entry not added ] existing = "+existing+", other ="+changedData);
				}
				return;
			} else if ( changedData.isNewerThan( existing ) ) {
				if ( log.isTraceEnabled() ) {
					log.trace("store(): [ inserted at index "+index+" ] existing = "+existing+", other ="+changedData);
				}						
				priceInfos.add( index , changedData );
				return;
			} 
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("store(): [ added at end of list ] price ="+changedData);
		}		
		priceInfos.add( changedData );
	}
	
	public synchronized boolean isEmpty() { return priceInfos.isEmpty(); }
	
	protected synchronized PriceInfo getLatestPrice(PriceInfo.Type type) {
		
		if ( type == PriceInfo.Type.ANY ) {
			throw new IllegalArgumentException("Unsupported price info type requested: "+type);
		}
		
		for ( PriceInfo info : priceInfos ) {
			if ( info.hasType( type ) ) {
				return info;
			}
		}
		return null;
	}
	
	public synchronized List<PriceInfo> getLatestPriceInfos(Type requestedType) {

		PriceInfo buyPrice = null;
		PriceInfo sellPrice = null;

		switch( requestedType ) {
			case BUY:
				buyPrice = getLatestPrice( requestedType );
				break;
			case SELL:
				sellPrice = getLatestPrice( requestedType );
				break;
			case ANY:
				buyPrice = getLatestPrice( PriceInfo.Type.BUY );
				sellPrice = getLatestPrice( PriceInfo.Type.SELL );
				break;
			default:
				throw new RuntimeException("Unhandled price type "+requestedType);
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("getLatestPriceInfos(): type = "+requestedType+" , buy = "+buyPrice+" , sell="+sellPrice);
		}

		if ( buyPrice == null && sellPrice == null ) {
			return Collections.emptyList();
		}

		final List<PriceInfo> result =
			new ArrayList<PriceInfo>();

		if ( buyPrice != null ) {
			result.add( buyPrice );
		}

		if ( sellPrice != null ) {
			result.add( sellPrice );
		}

		return result;
	}
	
	public synchronized List<PriceInfo> getAllPrices() {
		return Collections.unmodifiableList( priceInfos ); 
	}

	public synchronized void store(Collection<PriceInfo> infos) {
		
		if (infos == null) {
			throw new IllegalArgumentException("infos cannot be NULL");
		}
		
		for ( PriceInfo info : infos ) {
			store( info );
		}
	}

	@Override
	public synchronized Iterator<PriceInfo> iterator() {
		return Collections.unmodifiableList( new ArrayList<PriceInfo>( this.priceInfos ) ).iterator(); 
	}

	public synchronized InventoryType getItemType() {
		if ( isEmpty() ) {
			throw new IllegalStateException("getItemType() called although list is empty ?");
		}
		return priceInfos.get(0).getItemType();
	}

	public synchronized void remove(PriceInfo info) {
		
		if (info == null) {
			throw new IllegalArgumentException("info cannot be NULL");
		}
		
		if ( log.isDebugEnabled() ) {
			log.debug("remove(): Removing "+info);
		}
		
		for ( Iterator<PriceInfo> it = priceInfos.iterator() ; it.hasNext() ; ) {
			if ( it.next() == info ) {
				it.remove();
				break;
			}
		}
	}

	public synchronized List<PriceInfo> getPriceHistory(Type type) {
		
		final ArrayList<PriceInfo> result = 
			new ArrayList<PriceInfo>();
		
		for ( PriceInfo info : priceInfos ) {
			if ( info.getPriceType().matches( type ) ) {
				result.add( info );
			}
		}
		return result;
	}

	synchronized String getDebugString() {
		
		final StringBuilder stringBuilder = new StringBuilder();
		for ( PriceInfo info : priceInfos ) {
			stringBuilder.append( info.toString() ).append("\n");
		}
		return stringBuilder.toString();
	}
}