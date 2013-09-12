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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;

import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.assets.IAssetManager;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.db.dao.IBlueprintTypeDAO;
import de.codesourcery.eve.skills.exceptions.NoSuchCharacterException;
import de.codesourcery.eve.skills.exceptions.NoTech1VariantException;
import de.codesourcery.eve.skills.production.BlueprintWithAttributes;
import de.codesourcery.eve.skills.production.IBlueprintLibrary;
import de.codesourcery.eve.skills.production.IBlueprintLibrary.IBlueprintLibraryChangeListener;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.renderer.FixedBooleanTableCellRenderer;
import de.codesourcery.eve.skills.ui.utils.LabelViewFilterPanel;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class BlueprintLibraryComponent extends AbstractComponent
{

	private static final Logger log = Logger
	.getLogger(BlueprintLibraryComponent.class);

	@Resource(name="blueprint-library")
	private IBlueprintLibrary blueprintLibrary;

	@Resource(name="asset-manager")
	private IAssetManager assetManager;

	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;

	@Resource(name="blueprint-type-dao")
	private IBlueprintTypeDAO blueprintTypeDAO;

	private final BlueprintModel model = new BlueprintModel();
	private final JTable blueprintsTable = new JTable();

	private final LabelViewFilterPanel<BlueprintWithAttributes> byNameFilter = 
		new LabelViewFilterPanel<BlueprintWithAttributes>("Filter by name", 25 ) 
		{

		@Override
		protected void filterChanged()
		{
			model.viewFilterChanged();			
		}

		@Override
		protected String getStringFor(BlueprintWithAttributes item)
		{
			return item.getBlueprint().getName();
		}
		};


		private final DefaultTableCellRenderer myRenderer =
			new DefaultTableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
						row, column);

				if ( value instanceof ISKAmount) {
					if ( ((ISKAmount) value).toDouble() == 0.0d ) {
						setText( "<unknown>" );
					} else {
						setText( AmountHelper.formatISKAmount( (ISKAmount) value) );
					}
				}
				return this;
			}
		};

		private final IBlueprintLibraryChangeListener changeListener =
			new IBlueprintLibraryChangeListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void blueprintsChanged(
					List<? extends BlueprintWithAttributes> addedBlueprints,
					List<? extends BlueprintWithAttributes> changedBlueprints,
					List<? extends BlueprintWithAttributes> removedBlueprints)
			{

				if ( log.isDebugEnabled() ) {
					log.debug("blueprintsChanged(): added="+addedBlueprints+",\nupdated="+changedBlueprints+",\nremoved="+
							removedBlueprints);
				}
				model.setData(
						(List<BlueprintWithAttributes>) blueprintLibrary.getBlueprints()
				);				
			}};

			public BlueprintLibraryComponent() {
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void onAttachHook(IComponentCallback callback)
			{
				super.onAttachHook(callback);
				blueprintLibrary.addChangeListener( changeListener );
				model.setData( (List<BlueprintWithAttributes>) blueprintLibrary.getBlueprints() );
				
			}

			@Override
			protected void onDetachHook()
			{
				super.onDetachHook();
				blueprintLibrary.removeChangeListener( changeListener );
			}

			@Override
			protected JPanel createPanel()
			{

				final JPanel result =
					new JPanel();

				result.setLayout( new GridBagLayout() );

				FixedBooleanTableCellRenderer.attach( blueprintsTable );
				model.setViewFilter( this.byNameFilter.getViewFilter() );
				blueprintsTable.setDefaultRenderer( ISKAmount.class , myRenderer );
				blueprintsTable.setModel( model );
				blueprintsTable.setFillsViewportHeight( true );
				blueprintsTable.setRowSorter( model.getRowSorter() );

				result.add( byNameFilter , constraints(0,0).noResizing().end() );
				result.add( new JScrollPane( blueprintsTable ) , constraints(0,1).resizeBoth().end() );
				return result;
			}

			/**
			 * Table model.
			 * 
			 * @author tobias.gierke@code-sourcery.de
			 */
			private final class BlueprintModel extends AbstractTableModel<BlueprintWithAttributes>
			{

				private static final int COL_ME_LEVEL = 5;
				private static final int COL_PE_LEVEL = 6;

				private final List<BlueprintWithAttributes> data =
					new ArrayList<BlueprintWithAttributes>();

				public BlueprintModel() {
					super(new TableColumnBuilder()
					.add("Blueprint name")
					.add("Tech level",Integer.class)
					.add("Category")
					.add("Group")
					.add("Character")
					.add("ME",Integer.class)
					.add("PE",Integer.class)
					.add("Owned" , Boolean.class )
					.add("Base price",ISKAmount.class));
				}

				public void setData(List<BlueprintWithAttributes> data) {
					this.data.clear();
					this.data.addAll( data );
					modelDataChanged();
				}

				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex)
				{
					return columnIndex == COL_ME_LEVEL || columnIndex == COL_PE_LEVEL;
				}

				@Override
				public void setValueAt(Object value, int rowIndex, int columnIndex)
				{
					if ( columnIndex != COL_ME_LEVEL & columnIndex != COL_PE_LEVEL ) {
						throw new IllegalArgumentException("Invalid column "+columnIndex);	
					}

					final BlueprintWithAttributes row = getRow( rowIndex );

					int meLevel = row.getMeLevel();
					int peLevel = row.getPeLevel();

					if ( columnIndex == COL_ME_LEVEL ) {
						meLevel = (Integer) value;
					} else if ( columnIndex == COL_ME_LEVEL ) {
						peLevel = (Integer) value;
					} else {
						throw new RuntimeException("Unreachable code reached");
					}

					blueprintLibrary.update(row, meLevel, peLevel);
				}

				@Override
				protected Object getColumnValueAt(int modelRowIndex,
						int modelColumnIndex)
				{
					final BlueprintWithAttributes row =
						getRow( modelRowIndex );

					switch( modelColumnIndex ) 
					{
						case 0: // BP name
							return row.getBlueprint().getName();
						case 1: // Techlevel
							return row.getBlueprint().getTechLevel();
						case 2: // category 
							return row.getBlueprint().getProductType().getGroup().getCategory().getCategoryName();
						case 3: // group
							return row.getBlueprint().getProductType().getGroup().getGroupName();
						case 4: // owning character
							final CharacterID owningCharacterId = row.getOwningCharacterId();
							try {
								final UserAccount account = 
									userAccountStore.getAccountByCharacterID( owningCharacterId );

								return account.getCharacterByID( owningCharacterId ).getName();

							} catch(NoSuchCharacterException e) {
								return "<character "+owningCharacterId+" found>";
							}
						case COL_ME_LEVEL: // ME level
							return row.getMeLevel();
						case COL_PE_LEVEL: // PE level
							return row.getPeLevel();
						case 7: // found in assets ?
							return row.isFoundInAssets();
						case 8: // base price
							if ( row.getBlueprint().getTechLevel() == 1 ) {
								return row.getBlueprint().getBasePrice();
							} 
							else if ( row.getBlueprint().getTechLevel() == 2 ) 
							{
								try {
									blueprintTypeDAO.getTech1Variation( row.getBlueprint() );
								} catch(NoTech1VariantException e) {
									// Tech2 BPO sold by NPC corporations (R.A.M. etc.)
									return row.getBlueprint().getBasePrice();
								}
							}
							return ISKAmount.ZERO_ISK; // flag to renderer: unknown price
						default:
							throw new RuntimeException("Invalid column "+modelColumnIndex);
					}
				}

				@Override
				public BlueprintWithAttributes getRow(int modelRow)
				{
					return data.get( modelRow );
				}

				@Override
				public int getRowCount()
				{
					return data.size();
				}

			}
}
