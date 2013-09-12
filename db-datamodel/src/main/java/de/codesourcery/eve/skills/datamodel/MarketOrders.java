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
import java.util.Collections;
import java.util.List;

public class MarketOrders
{

	private final List<MarketOrder> orders = 
		new ArrayList<MarketOrder>();
	
	public List<MarketOrder> getBuyOrders() {
		return getOrders(PriceInfo.Type.BUY);
	}
	
	public List<MarketOrder> getSellOrders() {
		return getOrders(PriceInfo.Type.SELL);
	}
	
	public List<MarketOrder> getOrders() {
		return Collections.unmodifiableList( orders );
	}
	
	public List<MarketOrder> getOrders(PriceInfo.Type type) {
		
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		final ArrayList<MarketOrder> result = 
			new ArrayList<MarketOrder>();
		
		for ( MarketOrder order : orders ) {
			if ( type.matches( order.getType() ) ) {
				result.add( order );
			}
		}
		return result;
	}
	
	public void addMarketOrder(MarketOrder order) {
		
		if ( order == null ) {
			throw new IllegalArgumentException("order cannot be NULL");
		}
		
		orders.add( order );
	}
}
