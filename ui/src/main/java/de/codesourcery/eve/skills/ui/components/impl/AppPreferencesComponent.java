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
package de.codesourcery.eve.skills.ui.components.impl;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions.DataRetrievalStrategy;
import de.codesourcery.eve.skills.db.dao.IRegionDAO;
import de.codesourcery.eve.skills.db.dao.IStaticDataModelProvider;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.model.SimpleListModel;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;

public class AppPreferencesComponent extends AbstractEditorComponent {

	@Resource(name="appconfig-provider")
	private IAppConfigProvider appConfigProvider;
	
	@Resource(name="region-dao")
	private IRegionDAO regionDAO;
	
	@Resource(name="datamodel-provider")
	private IStaticDataModelProvider dataModelProvider;
	
	private JCheckBox clearUserAccountStorePassword =
		new JCheckBox();
	
	private JCheckBox reenableDisabledDialogs = new JCheckBox();
	
	private JCheckBox eveCentralEnabled = new JCheckBox();
	
	private final JComboBox defaultRegion = new JComboBox();
	private JComboBox retrievalStrategy;
	private DefaultComboBoxModel<RequestOptions.DataRetrievalStrategy> comboModel;
	private SimpleListModel<RequestOptions.DataRetrievalStrategy> retrievalStrategyModel;
		
	public AppPreferencesComponent() {
		super.setModal( true );
	}
	
	private final ListCellRenderer LIST_RENDERER = new DefaultListCellRenderer() {
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			
			final RequestOptions.DataRetrievalStrategy obj =
				(RequestOptions.DataRetrievalStrategy) value;
			
			final String label;
			switch( obj) {
				case DEFAULT:
					label = "Default";
					break;
				case FETCH_LATEST:
					label = "Fetch latest";
					break;
				case FETCH_LATEST_FALLBACK_CACHE:
					label = "Fetch latest (fallback: cache)";
					break;
				case FORCE_UPDATE:
					label = "Always";
					break;
				case OFFLINE:
					label = "Offline mode";
					break;
				case PREFER_CACHE:
					label = "Prefer cache";
					break;					
				default:
					label = obj.toString();
			}
			
			setText( label );
			return this;
		}
	};
	@Override
	protected JButton createCancelButton() {
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton() {
		return new JButton("Save");
	}
	
	@Override
	protected boolean hasValidInput()
	{
		return true;
	}

	@Override
	protected void okButtonClickedHook() {

		getAppConfig().setClientRetrievalStrategy( comboModel.getSelectedItem() ); 
		
		final Region selectedRegion =
			(Region) defaultRegion.getSelectedItem();
		
		getAppConfig().setDefaultRegion( selectedRegion );
		
		if ( reenableDisabledDialogs.isSelected() ) {
			getAppConfig().reenableAllDialogs();
		}
		
		// reset password
		if ( this.clearUserAccountStorePassword.isEnabled() &&
				this.clearUserAccountStorePassword.isSelected() ) 
		{
			getAppConfig().setUserAccountStorePassword( null );
		}
		
		// enable/disable eve central
		getAppConfig().setEveCentralEnabled( eveCentralEnabled.isSelected() );
		
		try {
			appConfigProvider.save();
		} 
		catch (IOException e) {
			displayError("Failed to save application config: "+e.getMessage() ,e );
		}
	}
	
	protected de.codesourcery.eve.skills.ui.config.AppConfig getAppConfig() {
		return appConfigProvider.getAppConfig();
	}
	
	@Override
	protected JPanel createPanelHook() {
		
		final JPanel panel = new JPanel();
		
		panel.setLayout( new GridBagLayout() );
		
		// 
		int y = 0;
		
		// clear account store password
		panel.add( new JLabel("Clear account store password ?") , 
				constraints(0,0).useRelativeWidth().end() );
		
		panel.add( clearUserAccountStorePassword , 
				constraints(1,y++ ).useRemainingWidth().anchorEast().end() 
		);
		
		clearUserAccountStorePassword.setEnabled(
			getAppConfig().hasUserAccountStorePassword() 
		);
		
		// re-enable warning dialogs
		panel.add( new JLabel( "Re-enable all disabled dialog windows" ) , 
				constraints(0,y).useRelativeWidth().end() );
		
		panel.add( reenableDisabledDialogs  , 
				constraints(1,y++ ).useRemainingWidth().anchorEast().end() 
		);
		
		// EVE central enable / disable
		// eveCentralEnabled 
		
		panel.add( new JLabel( "Fetch prices from EVE central" ) , 
				constraints(0,y).useRelativeWidth().end() );
		
		panel.add( eveCentralEnabled , 
				constraints(1,y++ ).useRemainingWidth().anchorEast().end() 
		);		
		
		eveCentralEnabled.setSelected( getAppConfig().isEveCentralEnabled() );
		
		// populate combobox with API client data retrieval strategy
		retrievalStrategyModel =
			new SimpleListModel<DataRetrievalStrategy>( DataRetrievalStrategy.values() );
		
		comboModel = new DefaultComboBoxModel<DataRetrievalStrategy>( retrievalStrategyModel );
		comboModel.setSelectedItem( getAppConfig().getClientRetrievalStrategy() );
		
		retrievalStrategy = new JComboBox( comboModel ) {
			@Override
			public ListCellRenderer getRenderer() {
				return LIST_RENDERER;
			}
		};
		
		panel.add( new JLabel("API data retrieval strategy") ,
				constraints(0,y).useRelativeWidth().end() );
		panel.add( retrievalStrategy , constraints(1,y++).useRemainingWidth().end() );
	
		/* 
		 * Add region selection box.
		 */
		
		final List<Region> regions =
			regionDAO.fetchAll();
		
		Collections.sort( regions , new Comparator<Region>() {

			@Override
			public int compare(Region o1, Region o2) {
				return o1.getName().compareTo(o2.getName());
			}} );
		
		final IRegionQueryCallback callback =
			RegionSelectionDialog.createCallback( regionDAO );
		
		final Region selected;
		if ( getAppConfig().hasDefaultRegion() ) {
			selected = getAppConfig().getDefaultRegion( callback );
		} else {
			selected = regions.get(0);
		}
		defaultRegion.setModel( new DefaultComboBoxModel<Region>( regions ) );
		defaultRegion.setRenderer( new RegionComboBoxRenderer() );
		defaultRegion.setSelectedItem( selected );
		
		panel.add( new JLabel("Region for querying market prices") ,
				constraints(0,y).useRelativeWidth().end() );
		panel.add( defaultRegion , constraints(1,y++).useRemainingWidth().end() );
		
		return panel;
	}
	
	private static final class RegionComboBoxRenderer extends DefaultListCellRenderer {
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) 
		{
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
		
			if ( value != null ) {
				setText( ((Region) value).getName() );
			}
			return this;
		}
	}
	
	@Override
	public String getTitle() {
		return "Edit application preferences";
	}

}
