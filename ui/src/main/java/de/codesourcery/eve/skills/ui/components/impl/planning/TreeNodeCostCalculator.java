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
package de.codesourcery.eve.skills.ui.components.impl.planning;

import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.MarketFilterBuilder;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.BlueprintNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.InventionJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ManufacturingJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.RequiredMaterialNode;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class TreeNodeCostCalculator
{
	private final IAppConfigProvider appConfigProvider;
	private final IMarketDataProvider marketDataProvider;
	private final IRegionQueryCallback queryCallback;
	
	public TreeNodeCostCalculator(IRegionQueryCallback queryCallback ,
			IAppConfigProvider appConfigProvider , 
			IMarketDataProvider marketDataProvider) 
	{
		if ( marketDataProvider == null ) {
			throw new IllegalArgumentException(
					"marketDataProvider cannot be NULL");
		}
		
		if ( queryCallback == null ) {
			throw new IllegalArgumentException("queryCallback cannot be NULL");
		}
		
		if ( appConfigProvider == null ) {
			throw new IllegalArgumentException("appConfigProvider cannot be NULL");
		}
		
		this.queryCallback = queryCallback;
		this.marketDataProvider = marketDataProvider;
		this.appConfigProvider = appConfigProvider;
	}
	
	public ISKAmount getFixedCosts(ITreeNode node) throws PriceInfoUnavailableException  
	{
		if ( node instanceof RequiredMaterialNode ) {
			return getAveragePrice( ((RequiredMaterialNode) node).getRequiredMaterial().getType() );
		} else if ( node instanceof InventionJobNode ) {
			// TODO: Calculate installation costs for this invention job
		} else if ( node instanceof ManufacturingJobNode ) {
			// TODO: Calculate installation costs for this manufacturing job
		} 
		return ISKAmount.ZERO_ISK;
	}
	
	protected ISKAmount getAveragePrice(InventoryType item) throws PriceInfoUnavailableException {
		
		final Region region = appConfigProvider.getAppConfig().getDefaultRegion( queryCallback );
		PriceInfoQueryResult result = marketDataProvider.getPriceInfo( 
				new MarketFilterBuilder( Type.BUY, region ).end() , 
				null,
				item );
		
		if ( result.hasBuyPrice() ) {
			return new ISKAmount( result.buyPrice().getAveragePrice() );
		}
		throw new PriceInfoUnavailableException("No buy price for "+item,item);
	}
	
	public ISKAmount getVariableCosts(ITreeNode node) 
	{
		if ( node instanceof InventionJobNode ) {
			// TODO: Calculate hourly costs for this invention job
		} else if ( node instanceof ManufacturingJobNode ) {
			// TODO: Calculate hourly costs for this manufacturing job
		} 
		return ISKAmount.ZERO_ISK;
	}
	
	public ISKAmount getOneTimeCosts(ITreeNode node) 
	{
		if ( node instanceof BlueprintNode ) {
			final BlueprintNode  n = (BlueprintNode) node;
			if ( n.getBlueprint().getTechLevel() == 1 && n.isRequiresOriginal() ) {
				return n.getBlueprint().getBasePrice();
			}
		} 
		return ISKAmount.ZERO_ISK;
	}
}
