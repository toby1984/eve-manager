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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.NPCCorpStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.components.impl.planning.AssemblyLineChooser;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.Cell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.HorizontalGroup;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.LayoutHints;

public class CharacterStandingsComponent extends AbstractComponent implements ICharacterSelectionProviderAware
{
	private ISelectionProvider<ICharacter> provider;
	
	private final JTable table = new JTable();
	private final MyTableModel model = new MyTableModel();
	
	@Resource(name="api-client")
	private IAPIClient apiClient;
	
	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;
	
	private ISelectionListener<ICharacter> listener = new ISelectionListener<ICharacter>() {

		@Override
		public void selectionChanged(ICharacter selected)
		{
			model.refresh();
		}
	};
	
	private final class MyTableModel extends AbstractTableModel<Standing<NPCCorporation>> {

			private final DecimalFormat STANDING = new DecimalFormat("#0.00");
		
			private List<Standing<NPCCorporation>> standings =
				new ArrayList<Standing<NPCCorporation>>();
			
			public MyTableModel() {
				super( new TableColumnBuilder().add("Corporation").add("Standing" ) );
			}

			@Override
			protected Object getColumnValueAt(int modelRowIndex,
					int modelColumnIndex)
			{
				
				final Standing<NPCCorporation> row = getRow( modelRowIndex );
				
				switch( modelColumnIndex ) {
					case 0:
						return row.getFrom().getName();
					case 1:
						return STANDING.format( row.getValue() );
					default:
						throw new IllegalArgumentException("Invalid column "+modelColumnIndex);
				}
			}

			@Override
			public Standing<NPCCorporation> getRow(int modelRow)
			{
				return standings.get( modelRow );
			}

			@Override
			public int getRowCount()
			{
				return standings.size();
			}
			
			public void refresh() {
				
				final ICharacter c = provider.getSelectedItem();
				if ( c == null ) {
					standings.clear();
					modelDataChanged();
					return;
				}
				
				submitTask( new UITask() {

					private APIResponse<NPCCorpStandings> characterStandings;
					
					@Override
					public String getId()
					{
						return "fetch_char_standings_"+c.getCharacterId().getValue();
					}

					@Override
					public void run() throws Exception
					{
						displayStatus("Fetching NPC corp standings for "+c.getName());
						
						characterStandings =
							queryCharacterStandings( c );
					}
					
					@Override
					public void successHook() throws Exception
					{
						
						final Collection<Standing<NPCCorporation>> corpStandings = 
							characterStandings.getPayload().getNPCCorpStandings();

						standings.clear();
						standings.addAll( corpStandings );
						modelDataChanged();
					}
					
					@Override
					public void failureHook(Throwable t) throws Exception
					{
						displayError("Fetching character standings failed",t);
					}
				}, true );
			}
		
	};

	protected APIResponse<NPCCorpStandings> queryCharacterStandings(ICharacter c) 
	{
		return apiClient.getNPCCorpCharacterStandings( c ,
				userAccountStore.getAccountByCharacterID( c.getCharacterId() ),
				RequestOptions.DEFAULT );
	}
	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		provider.addSelectionListener( listener );
		model.refresh();
	}
	
	@Override
	protected void onDetachHook()
	{
		provider.removeSelectionListener( listener );
	}
		
	@Override
	protected JPanel createPanel()
	{
		
		final JPanel result = new JPanel();
		
		// TODO: DEBUG:
		
		final JButton dummy = new JButton("Available assembly lines");
		dummy.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				
				final ICharacter c = provider.getSelectedItem();
				if ( c != null ) {
					
					final AssemblyLineChooser comp = 
						new AssemblyLineChooser( queryCharacterStandings( c ).getPayload() );
					
					comp.setModal( true );
					
					ComponentWrapper.wrapComponent( "Assembly lines" , comp ).setVisible( true );
				}
			}} );
		
		
		// setup table
		table.setModel( this.model );
		table.setRowSorter( this.model.getRowSorter() );
		
		result.add( new JScrollPane( table ) , constraints().resizeBoth().useRemainingSpace().end() );
		
		new GridLayoutBuilder()
		.add( new HorizontalGroup( new Cell( dummy , LayoutHints.NO_RESIZING ) ) )
		.add(  new HorizontalGroup( new Cell( new JScrollPane( table ) ) ) )
		.addTo(result);
		
		return result;
	}
	
	public void setSelectionProvider(ISelectionProvider<ICharacter> provider) {
		if ( provider == null ) {
			throw new IllegalArgumentException("provider cannot be NULL");
		}
		this.provider = provider;
	}

}
