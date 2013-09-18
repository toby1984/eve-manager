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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.InitializingBean;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IPriceQueryCallback;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.ui.config.AppConfig;
import de.codesourcery.eve.skills.ui.config.IAppConfigChangeListener;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.util.IStatusCallback;

public class DefaultMarketDataProvider implements IMarketDataProvider , InitializingBean {

	private IMarketDataProvider wrappedProvider;
	
	private IAppConfigProvider applicationConfig;
	
	private final IAppConfigChangeListener configChangeListener = 
		new IAppConfigChangeListener() {
			
			@Override
			public void appConfigChanged(AppConfig config, String... properties) 
			{
				if ( ArrayUtils.contains( properties , AppConfig.PROP_EVE_CENTRAL_ENABLED ) ) {
					setOfflineMode( ! config.isEveCentralEnabled() );
				}
			}
		};

	@Override
	public void addChangeListener(IPriceInfoChangeListener listener) {
		wrappedProvider.addChangeListener( listener );
	}

	@Override
	public void addStatusCallback(IStatusCallback callback) {
		wrappedProvider.addStatusCallback( callback );		
	}

	@Override
	public IUpdateStrategy createUpdateStrategy(UpdateMode mode, Type filterType) {
		return wrappedProvider.createUpdateStrategy( mode , filterType );
	}

	@Override
	public void dispose() {
		wrappedProvider.dispose();
	}

	@Override
	public PriceInfoQueryResult getPriceInfo(MarketFilter filter, IPriceQueryCallback callback, InventoryType item) throws PriceInfoUnavailableException 
	{
		return wrappedProvider.getPriceInfo(filter, callback, item);
	}

	@Override
	public Map<InventoryType, PriceInfoQueryResult> getPriceInfos(
			MarketFilter filter, IPriceQueryCallback callback,
			InventoryType... items) throws PriceInfoUnavailableException 
	{
		return wrappedProvider.getPriceInfos(filter, callback, items);
	}

	@Override
	public boolean isOfflineMode() {
		return wrappedProvider.isOfflineMode();
	}
	
	@Override
	public void store(PriceInfo info) {
		wrappedProvider.store(info);
	}
	
	@Override
	public void store(Collection<PriceInfo> info) 
	{
		wrappedProvider.store(info);
	}

	@Override
	public void removeChangeListener(IPriceInfoChangeListener listener) {
		wrappedProvider.removeChangeListener( listener );		
	}

	@Override
	public void removeStatusCallback(IStatusCallback callback) {
		wrappedProvider.removeStatusCallback( callback );		
	}

	@Override
	public void setOfflineMode(boolean yesNo) {
		wrappedProvider.setOfflineMode( yesNo );		
	}

	@Override
	public void updatePriceInfo(MarketFilter filter, List<InventoryType> items,
			IPriceQueryCallback callback, IUpdateStrategy updateStrategy)
			throws PriceInfoUnavailableException 
	{
		wrappedProvider.updatePriceInfo( filter , items , callback , updateStrategy );
	}

	public void setWrappedProvider(IMarketDataProvider wrappedProvider) {
		this.wrappedProvider = wrappedProvider;
	}
	
	public void setApplicationConfigProvider(IAppConfigProvider applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		applicationConfig.addChangeListener( this.configChangeListener );	
		this.wrappedProvider.setOfflineMode( ! applicationConfig.getAppConfig().isEveCentralEnabled() );
	}

	@Override
	public Map<Long, InventoryType> getAllKnownInventoryTypes(Region region,IInventoryTypeDAO dao) 
	{
		return wrappedProvider.getAllKnownInventoryTypes(region, dao);
	}
}