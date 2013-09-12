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

import java.awt.Color;
import java.awt.GridBagLayout;

import javax.annotation.Resource;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.CorpStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.db.datamodel.Faction;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.impl.StandingsTableModel;
import de.codesourcery.eve.skills.ui.utils.UITask;

public class CorpFactionStandingsComponent extends AbstractComponent implements ICharacterSelectionProviderAware {

	public static final Logger log = Logger
	.getLogger(CorpFactionStandingsComponent.class);

	@Resource(name="api-client")
	private IAPIClient apiClient;

	@Resource(name="useraccount-store")
	private IUserAccountStore accountStore;

	// GUI
	private final JTable table = new JTable();
	private final StandingsTableModel<Faction> tableModel =
		new StandingsTableModel<Faction>("Faction");
	
	private ISelectionProvider<ICharacter> selectionProvider;

	private final ISelectionListener<ICharacter> listener = 
		new ISelectionListener<ICharacter>() {

			@Override
			public void selectionChanged(ICharacter selected) {
				refresh( selected );
			}};

	@Override
	protected JPanel createPanel() {

		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );

		table.setModel( tableModel );
		table.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
		table.setRowSorter( tableModel.getRowSorter() );
		
		final JScrollPane pane = new JScrollPane( table );
		result.add( pane , constraints(0,0).resizeBoth().end() );
		return result;
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback) {
		this.selectionProvider.addSelectionListener( listener );
		refresh( this.selectionProvider.getSelectedItem() );
	}
	
	@Override
	protected void onDetachHook() {
		if ( this.selectionProvider != null ) {
			this.selectionProvider.removeSelectionListener( listener );
		}
	}
	
	private void refresh(final ICharacter character ) {

		if ( character == null ) {
			tableModel.refresh( null );
			return;	
		}

		submitTask( "faction_standings", new UITask() {

			private volatile APIResponse<CorpStandings> corpStandings;

			@Override
			public void run() throws Exception {

				displayStatus("Fetching corp faction standings for "+character.getName());
				
				final UserAccount account =
					accountStore.getAccountByCharacterID( character.getCharacterId() );

				corpStandings = apiClient.getCorpStandings( 
						character ,
						account ,
						RequestOptions.DEFAULT );
			}

			@Override
			public void failureHook(Throwable t) throws Exception
			{
				displayError( "Failed to retrieve faction standings",t);
			}

			@Override
			public void successHook() throws Exception
			{
				tableModel.refresh( corpStandings.getPayload().getFactionStandings() );
			}

			@Override
			public String getId()
			{
				return "fetch_corp_faction_standings_"+character.getCharacterId().getValue();
			}

		} );
	}

	@Override
	public String getTitle() {
		
		if ( this.selectionProvider != null ) {
			final ICharacter character =
				this.selectionProvider.getSelectedItem();

			if ( character != null ) {
				return "Status for "+character.getName();
			}
		}
		
		return super.getTitle();
	}
	
	public void setSelectionProvider(
			ISelectionProvider<ICharacter> selectionProvider) {
		assertDetached();
		this.selectionProvider = selectionProvider;
	}


}
