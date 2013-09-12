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

import java.util.List;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;

public interface IPriceQueryCallback {
	
	public static IPriceQueryCallback NOP_INSTANCE =
		new IPriceQueryCallback() {

			@Override
			public List<PriceInfo> getPriceInfo(MarketFilter filter,
					String message, InventoryType item)
					throws PriceInfoUnavailableException {
				throw new PriceInfoUnavailableException("Price for "+item.getName()+" is unavailable",item);
			}};
		

	/**
	 * Queries cost information from
	 * the user.
	 *  
	 * @param message Message to be displayed
	 * @param item the item for which the costs are queried,
	 * may be <code>null</code> when querying non item-related
	 * costs. 
	 * @return price info from user. Make sure the {@link PriceInfo#isUserProvided()}
	 * flag is set properly.
	 */
	public List<PriceInfo> getPriceInfo(MarketFilter filter , String message, InventoryType item) throws PriceInfoUnavailableException;
}
