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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;

/**
 * Holds item price information.
 * 
 * Implementations must be thread-safe.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IPriceInfoStore {
	
	public void save(Region region,InventoryType type , Collection<PriceInfo> info);
	
	public void save(PriceInfo info);
	
	/**
	 * 
	 * @param region
	 * @param kind
	 * @param items
	 * @return Map<Inventoy type ID , List<PriceInfo>>
	 */
	public Map<Long,List<PriceInfo>> getLatestPriceInfos(Region region,Type kind ,Collection<InventoryType> items);
	
	public Map<Long,InventoryType>  getAllKnownInventoryTypes(Region region,IInventoryTypeDAO dao);
		
	public List<PriceInfo> get(MarketFilter filter,InventoryType itemType);
	
	public List<PriceInfo> getPriceHistory( Region region,PriceInfo.Type type, InventoryType item);
	
	public void evict(PriceInfo info);
	
	public void persist() throws IOException;
	
	public void shutdown();
	
}
