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
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.production.OreRefiningData.OreVariant;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;

public class ReverseRefiningComponent extends AbstractRefiningComponent
{
	private final MineralModel requiredMineralsModel = new MineralModel();
	private final JTable requiredMineralsTable = new JTable( requiredMineralsModel );
	
	private final OreModel oreModel = new OreModel();
	private final JTable oreTable = new JTable( oreModel );
	
	private final JComboBox oreVariants = new JComboBox( new DefaultComboBoxModel<OreVariant>( OreVariant.values()) );
	
	public ReverseRefiningComponent() {
		requiredMineralsModel.addTableModelListener( new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e)
			{
				recalculate();
			}} 
		);
	}
	
	@Override
	public String getTitle()
	{
		return "Reverse refining";
	}
	
	protected OreVariant getSelectedOreVariant() {
		OreVariant v = (OreVariant) oreVariants.getSelectedItem();
		if ( v == null ) {
			v = OreVariant.BASIC;
			oreVariants.setSelectedItem( v );
		}
		return v;
	}
	
	@Override
	protected JPanel createPanel()
	{
		final JPanel result =
			new JPanel();
		
		result.setLayout( new GridBagLayout() );

		final DefaultTableCellRenderer volumeRenderer = new DefaultTableCellRenderer() {
			
			private final DecimalFormat VOLUME_FORMAT =
				new DecimalFormat("###,###,###,##0.0#");
			
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
						row, column);
		
				final double volume = (Double) value;
				setHorizontalAlignment( SwingConstants.TRAILING );
				setText( VOLUME_FORMAT.format( volume ) + " m3" );
				return this;
			}
		};
		
		oreTable.setDefaultRenderer( Double.class , volumeRenderer );
		
		final JPanel comboPanel =
			new JPanel();
		
		comboPanel.setLayout( new GridBagLayout() );
		this.oreVariants.setPreferredSize( new Dimension( 150, 20 ) );
		
		this.oreVariants.setRenderer( new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				
				if ( value != null ) {
					setText( ( (OreVariant) value).getDisplayName() );
				}
				
				return this;
			}
		} );
		this.oreVariants.addItemListener( new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e)
			{
				recalculate();
			}} );

		
//		comboPanel.add( this.oreVariants , constraints(0,0).noResizing().end() );
		
		oreTable.setFillsViewportHeight( true );
		requiredMineralsTable.setFillsViewportHeight(true);
		
		final ImprovedSplitPane pane =
			new ImprovedSplitPane(JSplitPane.VERTICAL_SPLIT , 
					new JScrollPane( requiredMineralsTable),
					new JScrollPane( oreTable ) );
		
		pane.setDividerLocation( 0.3 );

//		new GridLayoutBuilder().add(
//				new GridLayoutBuilder.VerticalGroup(
//						new GridLayoutBuilder.FixedCell( createSelectionPanel() ),
//						new GridLayoutBuilder.FixedCell( oreVariants ),
//						new GridLayoutBuilder.Cell( pane )
//				)
//		).addTo( result );
		
		result.add( createSelectionPanel() , constraints(0,0).weightX(0).weightY(0).end() );
		result.add( oreVariants , constraints(0,1).weightX(0).weightY(0).end() );
		result.add( pane , constraints(0,2).end() );
		
		return result;
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		super.onAttachHook(callback);
		recalculate();
	}
	
	@Override
	public void setSelectedCharacter(ICharacter character)
	{
		super.setSelectedCharacter(character);
		recalculate();
	}
	
	@Override
	public void setSelectedStation(Station selectedStation)
	{
		super.setSelectedStation(selectedStation);
		recalculate();
	}
	
	protected void recalculate() {
	
		if ( getSelectedCharacter() == null || getSelectedStation() == null ) {
			return;
		}
		
		final List<ItemWithQuantity> requiredOres = 
			getRefiningCalculator().reverseRefine(
			requiredMineralsModel.getData(),
			getSelectedCharacter() ,
			getSelectedStation() , 
			getStandings().getValue(),
			getSelectedOreVariant()
		);
		
		this.oreModel.setData( requiredOres );
	}
	
	public void setRequiredMinerals(List<ItemWithQuantity> requiredMinerals) {
		this.requiredMineralsModel.setData( requiredMinerals );
	}
	
	private static class MineralModel extends AbstractTableModel<ItemWithQuantity> {

		private final List<ItemWithQuantity> requiredMinerals =
			new ArrayList<ItemWithQuantity> ();
		
		public MineralModel() {
			super(new TableColumnBuilder().add( "Mineral").add("Quantity", Integer.class ) );
		}
		
		public void setData(List<ItemWithQuantity> data) {
			
			final Map<InventoryType,ItemWithQuantity> merged =
				new HashMap<InventoryType,ItemWithQuantity>();
			
			for ( ItemWithQuantity item : data ) {
				ItemWithQuantity existing = merged.get( item.getType() );
				if ( existing == null ) {
					// ALWAYS store a clone , we will modify it when merging 
					merged.put( item.getType() , new ItemWithQuantity( item ) );
				} else {
					existing.mergeWith( item );
				}
			}
			requiredMinerals.clear();
			requiredMinerals.addAll( merged.values() );
			modelDataChanged();
		}
		
		public List<ItemWithQuantity> getData() {
			return this.requiredMinerals;
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final ItemWithQuantity row=
				getRow( modelRowIndex );
			
			switch( modelColumnIndex ) {
				case 0:
					return row.getType().getName();
				case 1:
					return row.getQuantity();
				default:
					throw new RuntimeException("Unreachable code reached");
			}
		}

		@Override
		public ItemWithQuantity getRow(int modelRow)
		{
			return requiredMinerals.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return requiredMinerals.size();
		}
		
	}
	
	private static class OreModel extends AbstractTableModel<ItemWithQuantity> {

		private final List<ItemWithQuantity> requiredMinerals =
			new ArrayList<ItemWithQuantity> ();
		
		public OreModel() {
			super(new TableColumnBuilder().add( "Ore")
					.add("Batch size",Integer.class)
					.add("Volume",Double.class)
					.add("Total volume",Double.class).add("Quantity", Integer.class ) );
		}
		
		public void setData(List<ItemWithQuantity> data) {
			requiredMinerals.clear();
			requiredMinerals.addAll( data );
			modelDataChanged();
		}
		
		public List<ItemWithQuantity> getData() {
			return this.requiredMinerals;
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final ItemWithQuantity row=
				getRow( modelRowIndex );
			
			switch( modelColumnIndex ) {
				case 0:
					return row.getType().getName();
				case 1:
					return row.getType().getPortionSize();
				case 2:
					return row.getType().getVolume();
				case 3:
					return row.getVolume();
				case 4:
					return row.getQuantity();
				default:
					throw new RuntimeException("Unreachable code reached");
			}
		}

		@Override
		public ItemWithQuantity getRow(int modelRow)
		{
			return requiredMinerals.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return requiredMinerals.size();
		}
		
	}
}
