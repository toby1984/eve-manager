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

import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.utils.EveDate;

public class PriceInfo {

	private static final Logger log = Logger.getLogger(PriceInfo.class);
	
	public enum Source {
		USER_PROVIDED("u","User-provided"),
		EVE_CENTRAL("e", "EVE-central"),
		MARKET_LOG("m", "market logfile");
		
		private final String typeId;
		private final String displayName;
		
		private Source(String typeId,String displayName) {
			this.typeId = typeId;
			this.displayName = displayName;
		}
		
		public static Source getSource(String typeId) {
			
			for ( Source s : values() ) {
				if ( s.typeId.equals( typeId ) ) {
					return s;
				}
			}
			
			// TODO: Legacy hack to be backwards-compatible to
			// TODO: old file format (will be upgraded when saving)
			if ( "true".equals( typeId ) ) {
				return USER_PROVIDED;
			} else if ( "false".equals( typeId ) ) {
				return EVE_CENTRAL;
			}
			throw new IllegalArgumentException("Unknown price source "+typeId);
		}
		
		public String getTypeId() { return typeId; }
		public String getDisplayName() { return displayName; }
	};
	
	public enum Type {
		/**
		 * Data derived from SELL orders.
		 */
		SELL("s"),
		/**
		 * Data derived from BUY orders.
		 */
		BUY("b"),
		/**
		 * Data derived from both SELL and BUY orders.
		 */
		ANY("any");
		
		private final String id;
		
		private Type(String id) {
			this.id = id;
		}
		
		public String getId() {
			return id;
		}
		
		public static Type fromTypeId(String id) {
			for ( Type t : values() ) {
				if ( t.id.equals( id ) ) {
					return t;
				}
			}
			throw new IllegalArgumentException("Invalid type '"+id+"'");
		}
		
		public void assertNotAny() {
			if ( this == ANY ) {
				throw new IllegalArgumentException("PriceType ANY is not supported here");
			}
		}
		
		public boolean matches(Type t) {
			if (t == null) {
				throw new IllegalArgumentException("type cannot be NULL");
			}
			if ( this == ANY ) {
				return BUY == t || SELL == t;
			} else if ( t == ANY ) {
				return ( this == BUY || this == SELL );
			}
			return this == t;
		}
	}
	
	private InventoryType itemType;
	
	private long orderId;
	private Region region;
	private EveDate timestamp; 
	private long remainingVolume;
	private long totalVolume;
	private long orderCount;
	private long minPrice;
	private long averagePrice;
	private long maxPrice;
	private Source source;
	
	private Type infoType; 
	
	public boolean hasRegion(Region other) {
		return this.region != null && other != null
		 && this.region.getID().equals( other.getID() );
	}
	
	protected PriceInfo(Type infoType,Source source) {
		if (infoType == null ) {
			throw new IllegalArgumentException("price info type cannot be NULL");
		}
		if ( source == null ) {
			throw new IllegalArgumentException("source  cannot be NULL");
		}
		infoType.assertNotAny();
		this.source = source;
		this.infoType = infoType;
	}
	
	public PriceInfo(Type infoType , InventoryType itemType ,Source source) {
		this( infoType , source );
		this.itemType = itemType;
	}
	
	public static PriceInfo findSellPrice(List<PriceInfo> info) {
		return findPriceInfo( info, PriceInfo.Type.SELL );
	}	
	
	public static PriceInfo findBuyPrice(List<PriceInfo> info) {
		return findPriceInfo( info, PriceInfo.Type.BUY );
	}	
	
	private static PriceInfo findPriceInfo(List<PriceInfo> info,PriceInfo.Type type) {
		
		if ( type == PriceInfo.Type.ANY ) {
			throw new IllegalArgumentException("Priceinfo type "+type+" is not allowed here");
		}
		
		if ( info == null || info.isEmpty() ) {
			return null;
		}
		
		PriceInfo result = null;
		for ( PriceInfo i : info ) {
			if ( i.getPriceType() == type ) {
				if ( result != null ) {
					throw new IllegalArgumentException("Input list contains more than one "+type+" price ?");
				}
				result = i;
			}
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("findPriceInfo(): type="+type+" => \n\n"+info+"\n\nresult = "+result);
		}
		return result;
	}
	
	public PriceInfo(PriceInfo cached) {
		this.itemType = cached.itemType;
		
		this.timestamp = cached.timestamp;
		this.minPrice = cached.minPrice;
		this.averagePrice = cached.averagePrice;
		this.maxPrice = cached.maxPrice;
		
		this.infoType = cached.infoType;
		this.region = cached.region;
	}
	
	public boolean isUserProvided() {
		return this.source == Source.USER_PROVIDED;
	}
	
	public boolean isFromSameDayAs(PriceInfo info) {
		return this.timestamp.isSameDay( info.timestamp );
	}
	
	public boolean isSupersededBy(PriceInfo info) {
	
		if ( ! info.isNewerThan( this ) ) {
			return false;
		}
		
		if ( this.isUserProvided() != info.isUserProvided() ) {
			if ( this.isUserProvided() && ! info.isUserProvided() ) {
				return false;
			}
		}
		
		if ( ! this.getItemType().getId().equals( info.getItemType().getId() ) ) {
			return false;
		}
		
		if ( this.getPriceType() != info.getPriceType() ) {
			return false;
		}
		
		if ( ! this.getRegion().getID().equals( info.getRegion().getID() ) ) {
			return false;
		}
		// => 
		return getTimestamp().isSameDay( info.getTimestamp() );
	}
	
	public boolean isNewerThan(PriceInfo info) {
		return this.getTimestamp().after( info.getTimestamp() );
	}

	public void setInventoryType(InventoryType type) {
		if ( type == null) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		this.itemType = type;
	}
	
	public boolean hasType(Type type) {
		return getPriceType() == type;
	}
	
	public Type getPriceType() {
		return infoType;
	}
	
	public InventoryType getItemType() {
		return itemType;
	}
	
	private void assertValidPrice(long price) {
		if ( price < 0 ) {
			throw new IllegalArgumentException("Price cannot be negative");
		}
	}
	public void setAveragePrice(long averagePrice) {
		assertValidPrice( averagePrice );
		this.averagePrice = averagePrice;
	}
	
	public long getAveragePrice() {
		return averagePrice;
	}

	public void setMinPrice(long minPrice) {
		assertValidPrice( minPrice );
		this.minPrice = minPrice;
	}

	public long getMinPrice() {
		return minPrice;
	}

	public void setMaxPrice(long maxPrice) {
		assertValidPrice( maxPrice );
		this.maxPrice = maxPrice;
	}

	public long getMaxPrice() {
		return maxPrice;
	}

	public void setTimestamp(EveDate timestamp) {
		this.timestamp = timestamp;
	}

	public EveDate getTimestamp() {
		return timestamp;
	}
	
	@Override
	public String toString() {
		return "PriceInfo[ type="+getPriceType()+", source="+source+", region="+region+" , item = "+getItemType().getName()+" , min = "+minPrice+" , avg="+averagePrice+" , max="+maxPrice+" , timestamp="+timestamp+" ]";
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public Region getRegion() {
		return region;
	}

	public void setVolume(long volume) {
		this.totalVolume = volume;
	}

	public long getVolume() {
		return totalVolume;
	}

	public void setOrderCount(long orderCount) {
		this.orderCount = orderCount;
	}

	public long getOrderCount() {
		return orderCount;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public Source getSource() {
		return source;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public long getOrderId() {
		return orderId;
	}

	public void setRemainingVolume(long remainingVolume) {
		this.remainingVolume = remainingVolume;
	}

	public long getRemainingVolume() {
		return remainingVolume;
	}
}
