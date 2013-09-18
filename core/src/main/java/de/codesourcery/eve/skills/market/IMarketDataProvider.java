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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.util.IStatusCallback;

/**
 * Provides access to (exernal) market data.
 * 
 * Implementations must be thread-safe.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IMarketDataProvider {

	public static enum OrderType {
		SELL_ORDER,
		BUY_ORDER;
	}
	
	public static enum UpdateMode {
		DEFAULT,
		UPDATE_NONE,
		UPDATE_MISSING,
		UPDATE_OUTDATED,
		UPDATE_MISSING_OR_OUTDATED,
		UPDATE_USERPROVIDED,
		UPDATE_ALL;
	}
	
	public interface IUpdateStrategy {
		
		public boolean requiresUpdate(InventoryType item,PriceInfo existingInfo);
		
		public void merge(MarketFilter filter,PriceInfoQueryResult result , PriceInfo existing);
	}
	
	public interface IPriceInfoChangeListener 
	{
		public void priceChanged(IMarketDataProvider caller , Region region , Set<InventoryType> types);
	}

	public IUpdateStrategy createUpdateStrategy(UpdateMode mode,PriceInfo.Type filterType);
		
	public void addChangeListener(IPriceInfoChangeListener listener);
	
	public void removeChangeListener(IPriceInfoChangeListener listener);
	
	/**
	 * 
	 * @param filter
	 * @param callback callback to use if price cannot be determined
	 * automatically. Set to <code>null</code> to trigger
	 * a {@link PriceInfoUnavailableException} exception instead.
	 * @param items
	 * @return price info's depending on the filter, a filter of type
	 * {@link PriceInfo.Type#ANY} will yield two results, one sell and one
	 * buy price.	 
	 * @throws PriceInfoUnavailableException
	 */
	public Map<InventoryType ,  PriceInfoQueryResult> getPriceInfos(MarketFilter filter,  IPriceQueryCallback callback, InventoryType... items) throws PriceInfoUnavailableException;
	
	/**
	 * 
	 * @param filter
	 * @param callback callback to use if price cannot be determined
	 * automatically. Set to <code>null</code> to trigger
	 * a {@link PriceInfoUnavailableException} exception instead.	 
	 * @param item
	 * @return price info's depending on the filter, a filter of type
	 * {@link PriceInfo.Type#ANY} will yield two results, one sell and one
	 * buy price.
	 * @throws PriceInfoUnavailableException
	 */
	public PriceInfoQueryResult getPriceInfo(MarketFilter filter, IPriceQueryCallback callback,InventoryType item) throws PriceInfoUnavailableException;
	
	public void addStatusCallback(IStatusCallback callback);
	
	public void removeStatusCallback(IStatusCallback callback);
	
	public void dispose();
	
	public void updatePriceInfo(MarketFilter filter , 
			List<InventoryType> items,
			IPriceQueryCallback callback,
			IUpdateStrategy updateStrategy) throws PriceInfoUnavailableException;
	
	public void store(Collection<PriceInfo> info);
	
	public void store(PriceInfo info);
	
	public void setOfflineMode(boolean yesNo);
	
	public Map<Long,InventoryType>  getAllKnownInventoryTypes(Region region,IInventoryTypeDAO dao);
	
	public boolean isOfflineMode();
}