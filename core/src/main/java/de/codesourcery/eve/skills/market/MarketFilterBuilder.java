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

public class MarketFilterBuilder {

	private boolean filterByMinQuantity=false;
	private long minQuantity=0;
	private Region region;
	private PriceInfo.Type type;
	private UpdateMode updateMode = UpdateMode.DEFAULT;
	
	public MarketFilterBuilder(PriceInfo.Type type,Region region) {
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		this.type = type;
		this.region = region;
	}
	
	public MarketFilterBuilder updateMode(UpdateMode mode) {
		if ( mode == null ) {
			throw new IllegalArgumentException("mode cannot be NULL");
		}
		this.updateMode = mode;
		return this;
	}
	
	public MarketFilter end() {
		return new MarketFilter(region,type, filterByMinQuantity , minQuantity  , updateMode );
	}
	
	public MarketFilterBuilder minQuantity(long quantity) {
		if ( quantity < 1 ) {
			throw new IllegalArgumentException("Invalid min. quantity "+quantity);
		}
		minQuantity = quantity;
		filterByMinQuantity = true;
		return this;
	}
	
	
}
