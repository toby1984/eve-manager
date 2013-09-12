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
package de.codesourcery.eve.skills.market;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.IMarketDataProvider.UpdateMode;

public class MarketFilter {

	// region
	private final Region region;

	// quantity
	private final boolean filterByMinQuantity;
	private final long minQuantity;

	private final IMarketDataProvider.UpdateMode updateMode;

	// type
	private PriceInfo.Type orderType;

	MarketFilter(Region region) {
		if (region == null) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		this.region = region;
		filterByMinQuantity = false;
		minQuantity = -1;
		this.updateMode = UpdateMode.DEFAULT; 
	}

	@Override
	public String toString() {
		return "MarketFilter[ type="+orderType+
		", update_mode = "+updateMode+
		", region="+region+" ]";
	}
	
	public IMarketDataProvider.UpdateMode getUpdateMode() {
		return updateMode;
	}

	public boolean equals(Object obj) {

		final MarketFilter other = (MarketFilter) obj;

		if ( ! this.region.getID().equals( other.region.getID() ) ) {
			return false;
		}

		if ( this.filterByMinQuantity != other.filterByMinQuantity ) {
			return false;
		}

		if ( this.filterByMinQuantity ) {
			if ( this.minQuantity != other.minQuantity ) {
				return false;
			}
		}

		return this.orderType.equals( other.orderType );
	}

	@Override
	public int hashCode() {

		long result = region.getID()*31+region.getID();
		
		if ( filterByMinQuantity ) {
			result = result + minQuantity*31;
		}

		result += orderType.ordinal()*31;

		return (int) result;
	}
	
	MarketFilter(Region region , PriceInfo.Type orderType , 
			boolean filterByMinQuantity, 
			long minQuantity) 
	{
		this( region, orderType , filterByMinQuantity , minQuantity , 
				UpdateMode.DEFAULT );
	}

	MarketFilter(Region region , PriceInfo.Type orderType , 
			boolean filterByMinQuantity, 
			long minQuantity,
			IMarketDataProvider.UpdateMode mode) 
	{
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( orderType == null ) {
			throw new IllegalArgumentException("orderType cannot be NULL");
		}
		
		if ( mode == null ) {
			throw new IllegalArgumentException("update mode cannot be NULL");
		}
		
		if ( filterByMinQuantity && minQuantity < 1 ) {
			throw new IllegalArgumentException("minQuantity must be >= 1");
		}
		
		this.region = region;
		this.orderType = orderType;
		this.filterByMinQuantity = filterByMinQuantity;
		this.minQuantity = minQuantity;
		this.updateMode = mode;
	}

	public boolean isFilterByMinQuantity() {
		return filterByMinQuantity;
	}

	public long getMinQuantity() {
		return minQuantity;
	}

	protected void setOrderType(PriceInfo.Type orderType) {
		if ( orderType == null ) {
			throw new IllegalArgumentException("orderType cannot be NULL");
		}
		this.orderType = orderType;
	}

	public boolean hasOrderType(PriceInfo.Type  type) {
		return this.orderType == type;
	}
	
	public PriceInfo.Type getOrderType() {
		return orderType;
	}

	public Region getRegion() {
		return region;
	}
	
}
