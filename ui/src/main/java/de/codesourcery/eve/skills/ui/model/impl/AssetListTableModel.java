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
package de.codesourcery.eve.skills.ui.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.Asset;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.AbstractViewFilter;
import de.codesourcery.eve.skills.ui.model.IViewFilterAware;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;

public abstract class AssetListTableModel extends AbstractTableModel<Asset> implements IViewFilterAware<Asset> {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(AssetListTableModel.class);
	
	private AssetList assetList;
	private final List<Asset> assets = new ArrayList<Asset>();
	
	public AssetListTableModel(AssetList list,TableColumnBuilder columns) {

		super( columns );

		if (list == null) {
			throw new IllegalArgumentException("list cannot be NULL");
		}

		setAssetList( list , true );
	}
	
	public AssetList getAssetList() {
		return assetList;
	}
	
	@Override
	public Asset getRow(int modelRow) {
		synchronized(assets) {
			return assets.get( modelRow );
		}
	}
	
	public void setAssetList(AssetList list,boolean addContainerContents) 
	{
		
		if ( list == null ) {
			throw new IllegalArgumentException("List cannot be null");
		}
	
		synchronized( assets ) {
			assets.clear();
			this.assetList = list;
			assets.addAll( list.getAssets( addContainerContents ) );
		}
		
		modelDataChanged();
	}
	
	protected void disposeHook() {
		synchronized( assets ) {
			assets.clear();
		}
	}
	
	public void clear() {
		synchronized( assets ) {
			assets.clear();
		}
		modelDataChanged();
	}
	
	private static final class ChildFilter extends AbstractViewFilter<Asset> {

		public ChildFilter() {
		}

		@Override
		public boolean isHiddenUnfiltered(Asset value) {
			return value.getContainer() != null;
		}

	}

	public void setIncludeItemsFromChildContainers(boolean includeContainerContents) {

		if ( ! hasViewFilter() ) {
			if ( ! includeContainerContents ) {
				setViewFilter( new ChildFilter() );
			}
			return;
		} 
		
		if ( getViewFilter() instanceof ChildFilter ) {
			setViewFilter( null );
			return;
		}

		final ChildFilter childFilter = getViewFilter().getFilter( ChildFilter.class );
		if ( ! includeContainerContents ) {
			removeViewFilter( childFilter );
		} 
	}

	@Override
	public final int getRowCount() {
		synchronized( assets ) {
			return assets.size();
		}
	}

	protected abstract Object getColumnValue(Asset asset, int columnIndex);

	@Override
	public final Object getColumnValueAt(int rowIndex, int columnIndex) {
		final Asset a;
		synchronized( assets ) {
			a = assets.get( rowIndex );
		}
		return getColumnValue( a , columnIndex );
	}

}
