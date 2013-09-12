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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.NPCCorpStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IPriceQueryCallback;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.market.MarketFilterBuilder;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.production.RefiningCalculator;
import de.codesourcery.eve.skills.production.RefiningResult;
import de.codesourcery.eve.skills.production.RefiningResults;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.impl.TotalItemValueComponent.IDataProvider;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.renderer.FixedBooleanTableCellRenderer;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.PlainTextTransferable;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class RefiningComponent extends AbstractRefiningComponent
{
	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;

	private JButton addButton =
		new JButton("Add items...");
	
	// items to refine
	private final ItemsToRefineModel refinedItemsModel = new ItemsToRefineModel();
	private final JTable refinedItemsTable = new JTable( refinedItemsModel );
	
	private final TotalItemVolumeComponent<Object> selectedVolume =
		new TotalItemVolumeComponent<Object>(
			new de.codesourcery.eve.skills.ui.components.impl.TotalItemVolumeComponent.IDataProvider<Object>() 
			{

			@Override
			public int getQuantity(Object r)
			{
				if ( r instanceof RefiningResult ) {
					return ((RefiningResult ) r).getYourQuantityMinusStationTax();
				} else if ( r instanceof ItemToRefine ) {
					return ((ItemToRefine ) r).getQuantity();
				} else {
					throw new RuntimeException("Unreachabe code reached");
				}
			}

			@Override
			public double getVolumePerUnit(Object r)
			{
				if ( r instanceof RefiningResult ) {
					return ((RefiningResult ) r).getType().getVolume();
				} else if ( r instanceof ItemToRefine ) {
					return ((ItemToRefine ) r).item.getType().getVolume();
				} else {
					throw new RuntimeException("Unreachabe code reached");
				}				
			}
			}
	);
	
	// selected ISK value (total)
	private final TotalItemValueComponent<Object> selectedISKValue = new TotalItemValueComponent<Object>(
			new IDataProvider<Object>() {

				@Override
				public ISKAmount getPricePerUnit(Object r )
				{
					if ( r instanceof RefiningResult ) {
						ISKAmount sellValue = ((RefiningResult ) r).getSellValue();
						if ( sellValue != null ) {
							return sellValue;
						}
					} else if ( r instanceof ItemToRefine ) {
						final ISKAmount sellValue = ((ItemToRefine ) r).getRefinedValue();
						if ( sellValue != null ) {
							return sellValue;
						}
					} else {
						throw new RuntimeException("Unreachabe code reached");
					}
					
					return ISKAmount.ZERO_ISK;
				}

				@Override
				public int getQuantity(Object r)
				{
					return 1;
				}
			}) ;
	
	// refining results
	private RefiningResultsModel refiningResultsModel =
		new RefiningResultsModel( refinedItemsModel );

	private JTable refiningResultsTable = new JTable(refiningResultsModel);

	@Resource(name="api-client")
	private IAPIClient apiClient;

	@Resource(name="appconfig-provider")
	private IAppConfigProvider appConfigProvider;
	
	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;
	
	@Resource(name="marketdata-provider")
	private IMarketDataProvider marketDataProvider;

	public RefiningComponent(ICharacter selectedCharacter) {
		super();
		registerChildren( selectedISKValue , selectedVolume);
		setSelectedCharacter( selectedCharacter );
	}
	
	@Override
	public String getTitle()
	{
		final String val = super.getTitle();
		return NO_TITLE.equals( val ) ? "Refining" : val;
	}

	public void setSelectedCharacter(ICharacter selectedCharacter)
	{
		super.setSelectedCharacter( selectedCharacter );
		refiningResultsModel.update();
	}

	protected void updateSelectedTotalValues(Collection<Object> data) {
		selectedISKValue.setItems( data );
		selectedVolume.setItems( data );
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		super.onAttachHook( callback );
		this.refiningResultsModel.update();
	}

	@Override
	protected void onDetachHook()
	{
		super.onDetachHook();
	}
	
	private Collection<Object> getSelectedRows(JTable table) 
	{
		final Collection<Object> result =
			new ArrayList<Object>();
		
		final int[] rows = table.getSelectedRows();
		final TableModel tableModel = table.getModel();
		for ( int viewRow : rows ) 
		{
			
			final int modelRow = table.convertRowIndexToModel( viewRow );
			
			if ( tableModel == refinedItemsModel ) {
				 result.add( refinedItemsModel.getRow( modelRow ) );
			} else if ( tableModel == this.refiningResultsModel ) {
				result.add( refiningResultsModel.getRow( modelRow ) );
			} else {
				throw new IllegalArgumentException("Internal error - table has unsupported model");
			}
		}
		return result;
	}

	@Override
	protected JPanel createPanel()
	{
		
		refinedItemsTable.setRowSorter( refinedItemsModel.getRowSorter() );
		refinedItemsTable.setFillsViewportHeight( true );
		
		refinedItemsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				updateSelectedTotalValues( getSelectedRows( refinedItemsTable ) ); 
			}
		});
		
		final TableCellRenderer refinedItemsRenderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
						row, column);
				
				setHorizontalAlignment( JLabel.TRAILING );
				
				if ( value instanceof ISKAmount ) {
					setText( AmountHelper.formatISKAmount( (ISKAmount) value ) +" ISK");
				} else {
					setText("0.0 ISK");
				}
				
				return this;
			}
		};
		
		FixedBooleanTableCellRenderer.attach( refinedItemsTable );
		refinedItemsTable.setDefaultRenderer( ISKAmount.class , refinedItemsRenderer );
		
		refiningResultsTable.setRowSorter( refiningResultsModel.getRowSorter() );
		refiningResultsTable.setFillsViewportHeight( true );
		
		final PopupMenuBuilder menuBuilder =
			new PopupMenuBuilder();
		
		menuBuilder.addItem("Copy to clipboard (text)" , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				putRefiningResultsOnClipboard();
			}
			
			@Override
			public boolean isEnabled()
			{
				return refiningResultsModel.getRowCount() > 0;
			}
		});
		
		menuBuilder.attach( refiningResultsTable );
		
		final PopupMenuBuilder menuBuilder2 =
			new PopupMenuBuilder();
		
		menuBuilder2.addItem("Remove" , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				final int[] viewRows = 
					refinedItemsTable.getSelectedRows();
				
				final int[] modelRows = new int[ viewRows.length ];
				for ( int i = 0 ; i < viewRows.length ; i++ ) {
					modelRows[i] = refinedItemsTable.convertRowIndexToModel( viewRows[i] );
				}
				refinedItemsModel.removeRows( modelRows );
			}
			
			@Override
			public boolean isEnabled()
			{
				return ! ArrayUtils.isEmpty( refinedItemsTable.getSelectedRows() );
			}
		} );
		
		menuBuilder2.attach( refinedItemsTable );
		
		refiningResultsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				updateSelectedTotalValues( getSelectedRows( refiningResultsTable ) ); 
			}
		});
		
		final DefaultTableCellRenderer resultsRenderer = new DefaultTableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
						row, column);
				
				if ( column != 0 ) {
					setHorizontalAlignment(JLabel.TRAILING );
				} else {
					setHorizontalAlignment(JLabel.LEADING);
				}
				
				if ( value instanceof ISKAmount) {
					setText( AmountHelper.formatISKAmount( (ISKAmount) value)+" ISK" );
				} else if ( value == null ) {
					setText( "<price unavailable>" );
				}
				return this;
			}
		};
		
		refiningResultsTable.setDefaultRenderer( String.class , resultsRenderer );
		refiningResultsTable.setDefaultRenderer( Integer.class , resultsRenderer );
		refiningResultsTable.setDefaultRenderer( ISKAmount.class , resultsRenderer );
		
		final ImprovedSplitPane splitPane =
			new ImprovedSplitPane(
				JSplitPane.VERTICAL_SPLIT ,
				new JScrollPane( refinedItemsTable ),
				new JScrollPane( refiningResultsTable )
			);
		
		splitPane.setDividerLocation( 0.3 );
		
		/*
		 * Button panel
		 */

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new GridBagLayout() );
		
		buttonPanel.add( addButton , constraints(0,0).anchorWest().useRelativeWidth().noResizing().end() );
		
		addButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				final ItemChooserComponent comp =
					new ItemChooserComponent( ItemChooserComponent.SelectionMode.MULTIPLE_SELECTION );
				
				comp.setModal( true );
				ComponentWrapper.wrapComponent("Add item to refine" , comp ).setVisible( true );	
				
				if ( ! comp.wasCancelled() && ! comp.getSelectedItems().isEmpty() ) 
				{
					final List<ItemWithQuantity> currentItems = refinedItemsModel.getItemsWithQuantity();
					for ( InventoryType t : comp.getSelectedItems() ) {
						currentItems.add( new ItemWithQuantity( t , 1 ) );
					}
					refinedItemsModel.setItemsToRefine( currentItems );
				}
			}
		});
		
		/*
		 * Create result panel.
		 */
		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );
		
		panel.add( createSelectionPanel()  , 
				constraints(0,0).weightX(0.8).useRelativeWidth().anchorSouth().weightY(0).resizeHorizontally().end() 
		);
		panel.add( 
				selectedISKValue.getPanel() , 
				constraints( 1 , 0 ).weightX(0.2).weightY(0).resizeHorizontally().anchorNorth().end() 
		);
		
		panel.add( buttonPanel , constraints( 0 , 1 ).weightX(0.8).width(1).weightY(0).anchorWest().end() );
		panel.add( selectedVolume.getPanel() , constraints( 1 , 1 ).weightX(0.2).width(1).weightY(0).anchorWest().end() );
		
		panel.add( splitPane , constraints(0,2).width(2).resizeBoth().end() );

		this.refiningResultsModel.update();
		
		return panel;
	}

	protected void putRefiningResultsOnClipboard()
	{
		
		final StringBuffer text =
			new StringBuffer();

		text
		.append( StringUtils.rightPad( "Item" , 35 ) )
		.append( StringUtils.rightPad( "Volume" , 15 ) )
		.append( StringUtils.leftPad(  "Quantity" , 15 ) )
		.append( StringUtils.leftPad( "Sell value" , 15 ) )
		.append("\n\n");
		
		final List<RefiningResult> rows = refiningResultsModel.myData;
		final DecimalFormat VOL = new DecimalFormat("###,###,###,###,###.0###");
		for (Iterator<RefiningResult> it = rows.iterator(); it.hasNext();) 
		{
			final RefiningResult row = it.next();

			final double volume = row.getYourQuantityMinusStationTax() * row.getType().getVolume();
			
			text.append( StringUtils.rightPad( row.getType().getName() , 35 ) )
				.append( StringUtils.leftPad(  VOL.format( volume ) , 15 ) )
				.append( StringUtils.leftPad(  ""+row.getYourQuantityMinusStationTax() , 15 ) )
				.append( StringUtils.leftPad( AmountHelper.formatISKAmount( row.getSellValue() ) , 15 ) );
			
			if ( it.hasNext() ) {
				text.append("\n");
			}
		}
		
		new PlainTextTransferable( text ).putOnClipboard();
	}

	public void setItemsToRefine(List<? extends ItemWithQuantity> items) {
		refinedItemsModel.setItemsToRefine( items );
	}

	private final class RefiningResultsModel extends AbstractTableModel<RefiningResult> {

		private final DecimalFormat PERCENT_FORMAT =
			new DecimalFormat("##0.0##");
		
		private final List<RefiningResult> myData=
			new ArrayList<RefiningResult>();

		private final RefiningCalculator calculator =
			new RefiningCalculator( dataModel );

		public RefiningResultsModel(ItemsToRefineModel parentModel) 
		{
			super(new TableColumnBuilder()
			        .add("Item")
					.add("Perfect (units)",Integer.class)
					.add("You (units)",Integer.class) 
					.add("Refining waste %")
					.add("Station takes (units)",Integer.class)
					.add("Remaining",Integer.class)
					.add("Sell value" , ISKAmount.class ) );
			
			parentModel.addTableModelListener( new TableModelListener() {

				@Override
				public void tableChanged(TableModelEvent e)
				{
					update();
				}} );
		}
		
		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final RefiningResult row =
				getRow( modelRowIndex );

			switch( modelColumnIndex ) {
				case 0:
					return row.getType().getName();
				case 1:
					return row.getPerfectQuantity();
				case 2:
					return row.getYourQuantity();
				case 3:
					if ( row.getPerfectQuantity() == 0 ) {
						return "0.0";
					}
					final double p = 100.0d - 
					( (double) row.getYourQuantity() / (double) row.getPerfectQuantity() ) *100.0d;
					return PERCENT_FORMAT.format( p );
				case 4:
					final int stationTakes = Math.round( row.getYourQuantity() * row.getStationTaxFactor() );
					return stationTakes;
				case 5:
					return row.getYourQuantityMinusStationTax();
				case 6:
					final int quantity =
						row.getYourQuantityMinusStationTax();
					
					final ISKAmount amount = getSellPrice( row.getType() );
					if ( amount == null ) {
						return null;
					}
					return amount.multiplyBy( quantity );
				default:
					throw new RuntimeException("Invalid model column "+modelColumnIndex );
			}
		}

		protected ISKAmount getSellPrice( InventoryType item ) {

			final IRegionQueryCallback regionCallback = 
				RegionSelectionDialog.createCallback( null , dataModel );
			
			final Region defaultRegion = 
				appConfigProvider.getAppConfig().getDefaultRegion(regionCallback );
			
			final MarketFilter filter =
				new MarketFilterBuilder(PriceInfo.Type.SELL , defaultRegion ).end();
			
			try {
				final PriceInfoQueryResult info = 
					marketDataProvider.getPriceInfo(filter ,IPriceQueryCallback.NOP_INSTANCE , item );
				
				if ( ! info.hasSellPrice() ) {
					return null;
				}
				
				return new ISKAmount( info.sellPrice().getAveragePrice() );
			}
			catch (PriceInfoUnavailableException e) {
				return null;
			}
		}
		
		public void update() {

			final ICharacter c = getSelectedCharacter();
			if ( c == null ) {
				synchronized (myData ) {
					this.myData.clear();
				}
				modelDataChanged();
				return;
			}

			final Station station =
				getSelectedStation();

			if ( station == null ) {
				synchronized ( myData ) {
					this.myData.clear();
				}
				modelDataChanged();
				return;
			}

			submitTask( new UITask() {

				private List<RefiningResults> results;
				private float stationOwnerStanding=0.0f;
				private NPCCorpStandings charStandings;

				@Override
				public void run() throws Exception
				{
					
					charStandings = apiClient.getNPCCorpCharacterStandings( c ,
							userAccountStore.getAccountByCharacterID( c.getCharacterId() ) , 
							RequestOptions.DEFAULT ).getPayload();
				}
			
				@Override
				public String getId()
				{
					return "calc_refining_results_"+c.getCharacterId();
				}
				
				@Override
				public void successHook() throws Exception
				{
					final Standing<NPCCorporation> standing = charStandings.getNPCCorpStanding( station.getOwner() );
					if ( standing != null ) {
						stationOwnerStanding = standing.getValue();
					}
					
					final List<ItemWithQuantity> items =
						new ArrayList<ItemWithQuantity>();
					
					for ( ItemToRefine r : RefiningComponent.this.refinedItemsModel.getItemsToRefine() ) {
						items.add( r.item );
					}
					
					results =  calculator.refine( items , c , station  , stationOwnerStanding );
					
					updateModel(results,c, station, stationOwnerStanding );
				}
				
				@Override
				public void failureHook(Throwable t) throws Exception
				{
					displayError("Failed to calculate refining results",t);
				}
			}, true );
		}

		private void updateModel(final List<RefiningResults> results , 
				final ICharacter c, final Station station, float standing)
		{
			final Map<Long,RefiningResult> resultsByItemType =
				new HashMap<Long,RefiningResult>();

			for ( RefiningResults result : results ) 
			{
				double sellValue = 0.0d;
				
				for ( RefiningResult aResult : result.getResults() ) 
				{
					ISKAmount price = getSellPrice( aResult.getType() );
					double thisValue = 0.0d;
					if ( price != null ) {
						thisValue = aResult.getYourQuantityMinusStationTax() * price.toDouble();
						sellValue += thisValue;
					}
					
					final RefiningResult existing = 
						resultsByItemType.get( aResult.getType().getId() );

					if ( existing == null ) {
						aResult.setSellValue( new ISKAmount( thisValue ) );
						resultsByItemType.put( aResult.getType().getId() , aResult );
					}
					else 
					{
						final ISKAmount existingAmount =
							existing.getSellValue();
						
						if ( existingAmount != null ) {
							existing.setSellValue( existingAmount.addTo( new ISKAmount( thisValue ) )  );
						} else {
							existing.setSellValue( new ISKAmount( thisValue ) );
						}
						existing.setPerfectQuantity( existing.getPerfectQuantity() + aResult.getPerfectQuantity() );
						existing.setYourQuantity( existing.getYourQuantity() + aResult.getYourQuantity() );
					}
				}
				
				refinedItemsModel.setRefinable( result.getRefinedItem() , 
						! result.getResults().isEmpty() , new ISKAmount( sellValue ) );
			}
			
			// sort alphabetically
			final List<RefiningResult> sorted =
				new ArrayList<RefiningResult>( resultsByItemType.values() );
			
			Collections.sort( sorted, new Comparator<RefiningResult>() {

				@Override
				public int compare(RefiningResult o1, RefiningResult o2)
				{
					return o1.getType().getName().compareTo( o2.getType().getName() );
				}} 
			);
			
			synchronized ( myData ) {
				myData.clear();
				myData.addAll( sorted );
			}
			modelDataChanged();
			refinedItemsModel.modelDataChanged();
		}

		@Override
		public RefiningResult getRow(int modelRow)
		{
			synchronized ( myData ) {
				return myData.get( modelRow );
			}
		}

		@Override
		public int getRowCount()
		{
			synchronized ( myData ) {
				return myData.size();
			}
		}

	}
	
	// ==============================
	
	private static final class ItemToRefine 
	{
		private final ItemWithQuantity item;
		private volatile boolean isRefinable;
		private volatile ISKAmount refinedValue;
		
		public ItemToRefine(ItemWithQuantity item) {
			this.item = item;
		}
		
		public ItemWithQuantity item() { return item; }
		public boolean isRefinable() { return isRefinable; }
		public void update(boolean isRefinable,ISKAmount refinedValue) { 
			this.isRefinable = isRefinable;
			this.refinedValue = refinedValue;
		}
		
		public int getQuantity() {
			return item.getQuantity();
		}
		
		public ISKAmount getRefinedValue() { return refinedValue; }
	}
	
	private final class ItemsToRefineModel extends AbstractTableModel<ItemToRefine> {

		private final List<ItemToRefine> itemsToRefine =
			new ArrayList<ItemToRefine>();

		public ItemsToRefineModel() {
			super(new TableColumnBuilder().add("Item")
					.add("Min. batch size",Integer.class)
					.add("Reprocessable ?",Boolean.class)
					.add("Volume" , Double.class )
					.add("Quantity" , Integer.class ) 
					.add("Sell value" , ISKAmount.class )
					.add("Total sell value" , ISKAmount.class )
			);
		}
		
		@Override
		public void removeRows(int... rowIndices)
		{
			final Set<Integer> removedIndices =
				new HashSet<Integer>();
			
			for ( int idx : rowIndices ) {
				removedIndices.add( idx );
			}
			
			final List<ItemToRefine> removedItems = 
				new ArrayList<ItemToRefine>();
			
			int row = 0;
			for ( ItemToRefine item : itemsToRefine ) {
				if ( removedIndices.contains( row ) ) {
					removedItems.add( item );
				}
				row++;
			}
			
			for ( ItemToRefine toBeRemoved : removedItems) {
				itemsToRefine.remove( toBeRemoved );
			}
			
			modelDataChanged();
		}
		
		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final ItemToRefine row =
				getRow( modelRowIndex );

			switch( modelColumnIndex ) {
				case 0: // item name
					return row.item().getType().getName();
				case 1: // batch size
					return row.item().getType().getPortionSize();
				case 2: // reprocessable ?
					return row.isRefinable();
				case 3: // volume
					return row.item.getQuantity() * row.item().getVolume();
				case 4: // quantity
					return row.item().getQuantity();
				case 5: // refined value
					if ( row.refinedValue == null ) {
						return ISKAmount.ZERO_ISK;
					}
					return row.refinedValue.divideBy( row.item.getQuantity() );
				case 6:
					return row.refinedValue != null ? row.refinedValue : ISKAmount.ZERO_ISK;
				default:
					throw new RuntimeException("Invalid model column "+modelColumnIndex );
			}
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			if ( columnIndex == 3 || columnIndex == 4 ) {
				return getRow( rowIndex ).isRefinable();
			}
			return false;
		}
		
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex)
		{
			final ItemWithQuantity item = getRow( rowIndex ).item;
			
			if ( columnIndex == 3 ) {
				final double newValue = (Double) value;
				if (newValue >= 1 ) 
				{
					final int quantity = (int) Math.floor( newValue / item.getType().getVolume() );
					item.setQuantity( quantity );
					refiningResultsModel.update();
				}
			} 
			else if ( columnIndex == 4 ) 
			{
				final int newValue = ((Integer) value).intValue();
				if ( newValue >= 0 && item.getQuantity() != newValue ) {
					item.setQuantity( newValue );
					refiningResultsModel.update();
				}
			}
		}
		
		public void setRefinable(InventoryType type,boolean isRefinable,ISKAmount refinedValue) 
		{
			int index = 0;
			synchronized(itemsToRefine) {
			for ( ItemToRefine item : itemsToRefine ) 
			{
				if ( item.item().getType().equals( type ) ) 
				{
					item.update( isRefinable , refinedValue );
					fireEvent( new TableModelEvent(this , index,index , 1 , TableModelEvent.UPDATE ) );
					return;
				}
				index++;
			}
			}
			
			throw new RuntimeException("Concurrency problem ? Unable to find "+type+" in internal list");
		}
		
		public List<ItemToRefine> getItemsToRefine()
		{
			synchronized ( itemsToRefine) {
				return itemsToRefine;
			}
		}
		
		public void setItemsToRefine(List<? extends ItemWithQuantity> items) 
		{
			if ( items == null ) {
				throw new IllegalArgumentException("items cannot be NULL");
			}
			
			final Map<Long , ItemToRefine> itemsById =
				new HashMap<Long , ItemToRefine>();
			
			for ( ItemWithQuantity item : items ) 
			{
				final ItemToRefine existing = 
					itemsById.get( item.getType().getId() );
				
				if ( existing != null ) {
					existing.item.incQuantity( item.getQuantity() );
				} else {
					itemsById.put( item.getType().getId() , new ItemToRefine( item ) );
				}
			}

			setItemsToRefine(itemsById.values());
		}
		
		private List<ItemWithQuantity> getItemsWithQuantity() 
		{
			synchronized (itemsToRefine) 
			{
				List<ItemWithQuantity> result =
					new ArrayList<ItemWithQuantity>();
					
				for ( ItemToRefine item : itemsToRefine ) {
					result.add( item.item );
				}
				return result;
			}
		}

		private void setItemsToRefine(Collection<ItemToRefine> items)
		{
			synchronized (itemsToRefine) {
				this.itemsToRefine.clear();
				this.itemsToRefine.addAll( items );
			}
			modelDataChanged();
		}

		@Override
		public ItemToRefine getRow(int modelRow)
		{
			synchronized ( itemsToRefine ) {
				return itemsToRefine.get( modelRow );
			}
		}

		@Override
		public int getRowCount()
		{
			synchronized ( itemsToRefine ) {
				return itemsToRefine.size();
			}
		}

	}	
}
