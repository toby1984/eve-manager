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
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.annotation.Resource;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IPriceInfoStore;
import de.codesourcery.eve.skills.market.IPriceQueryCallback;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.market.MarketFilterBuilder;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.production.OreRefiningData;
import de.codesourcery.eve.skills.production.OreRefiningData.OreVariant;
import de.codesourcery.eve.skills.production.OreRefiningData.RefiningOutcome;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.impl.TotalItemValueComponent.IDataProvider;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.Cell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.HorizontalGroup;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.VerticalGroup;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.PersistentDialogManager;
import de.codesourcery.eve.skills.ui.utils.PlainTextTransferable;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.DateHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISKAmount;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class OreChartComponent extends AbstractComponent
{
	private final JTable table = new JTable();

	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;

	@Resource(name="marketdata-provider")
	private IMarketDataProvider marketDataProvider;

	@Resource(name="priceinfo-store")
	private IPriceInfoStore priceInfoStore;
	
	@Resource(name="dialog-manager")
	private PersistentDialogManager dialogManager;

	@Resource(name="appconfig-provider")
	private IAppConfigProvider appConfigProvider;

	private OreRefiningData refiningData;
	private MyTableModel tableModel;

	@Resource(name="system-clock")
	private ISystemClock clock;

	private static final int FIRST_MINERAL_COLUMN=1; 

	private final JTable mineralPriceTable = new JTable();
	private final MineralPriceTableModel mineralPriceTableModel; 

	private final PriceCache priceCache;
	
	private JTextField oreHoldSize = new JTextField("8500");
	private JComboBox<String> oreChooser = new JComboBox<>();
	private JComboBox<OreVariant> oreVariantChooser = new JComboBox<>();
	
	private final IDataProvider<String> dataProvider = new IDataProvider<String>() {

		@Override
		public int getQuantity(String obj) 
		{
			try {
				return Integer.parseInt( oreHoldSize.getText() );
			} catch(Exception e) {
				oreHoldSize.setText("8500");
				return 8500;
			}
		}

		@Override
		public ISKAmount getPricePerUnit(String obj) 
		{
			final String oreName = (String) oreChooser.getSelectedItem();
			final InventoryType oreType = refiningData.getVariantType( oreName , getSelectedOreVariant() );
			final List<? extends ItemWithQuantity> refiningOutcome = refiningData.getRefiningOutcome( oreType );
			return getISKperM3( oreType , refiningOutcome );
		}
	};
	
	private TotalItemValueComponent<String> oreHoldValue = new TotalItemValueComponent<String>("Cargo hold value (ISK)", dataProvider);

	private final class PriceCache 
	{
		private final Map<Long,PriceInfo> priceInfoCache = new HashMap<Long,PriceInfo> ();

		public void flush() {
			priceInfoCache.clear();
		}

		public PriceInfo getSellPrice(InventoryType t) {

			PriceInfo info = priceInfoCache.get( t.getId() );
			if ( info == null ) {
				info = fetchSellPrice( t );
				priceInfoCache.put( t.getId() , info );
			}
			return info;
		}

		private PriceInfo fetchSellPrice(InventoryType t)
		{
			final MarketFilter filter =
				new MarketFilterBuilder( PriceInfo.Type.SELL , getDefaultRegion() ).end();

			try {
				final PriceInfoQueryResult result = marketDataProvider.getPriceInfo( filter , IPriceQueryCallback.NOP_INSTANCE , t );
				return result.sellPrice();
			}
			catch (PriceInfoUnavailableException e) {
				throw new RuntimeException(e);
			}
		}

		public void updatePrice(PriceInfo info, long amount)
		{

			info.setAveragePrice( amount );
			info.setTimestamp( new EveDate( clock ) );
			info.setSource( Source.USER_PROVIDED );

			priceInfoStore.store( info );
			this.priceInfoCache.remove( info.getItemType().getId() );
			mineralPriceTableModel.refresh();
			tableModel.refresh();
		}
	}

	protected Region getDefaultRegion() {
		final IRegionQueryCallback callback = 
			RegionSelectionDialog.createCallback( null , dataModel );
		return appConfigProvider.getAppConfig().getDefaultRegion( callback );
	}
	
	private final class TableRow {

		public final InventoryType oreType;
		public final String oreName;
		public final List<? extends ItemWithQuantity> data;

		public TableRow(InventoryType oreType, String oreName,List<? extends ItemWithQuantity> data) {
			this.oreType = oreType;
			this.oreName = oreName;
			this.data = data;
		}

		public ItemWithQuantity getYieldForMineral(int columnIndex) {

			final String mineralName = OreRefiningData.getMineralNames().get( columnIndex - FIRST_MINERAL_COLUMN );

			for ( ItemWithQuantity it : data ) {
				if ( it.getType().getName().equals( mineralName ) ) {
					return it;
				}
			}
			return new ItemWithQuantity( dataModel.getInventoryTypeByName( mineralName ) , 0 );
		}
	}
	
	private static final class CsvRow implements Comparable<CsvRow> 
	{
		private final String oreName;
		private final ISKAmount iskPerM3;
		
		public CsvRow(String oreName, ISKAmount iskPerM3) {
			this.oreName = oreName;
			this.iskPerM3 = iskPerM3;
		}
		
		@Override
		public int compareTo(CsvRow o)
		{
			return this.iskPerM3.compareTo( o.iskPerM3 );
		}
	}
	
	protected void saveSummaryToClipboard() 
	{
		
		final Clipboard clipboard = 
			Toolkit.getDefaultToolkit().getSystemClipboard(); 

		final StringBuffer buffer = 
			new StringBuffer();
		
		buffer.append( "Current date: "+DateHelper.format( new Date() )+"\n" );
		buffer.append("Region: "+getDefaultRegion().getName() +"\n");
		buffer.append("\n");
		
		for ( int i = 0 ; i < mineralPriceTableModel.getRowCount() ; i++ ) {
			final MineralPrice minPrice = mineralPriceTableModel.getRow( i );
			buffer.append( StringUtils.rightPad( minPrice.itemName() , 13 ) ).append(" : ");
			buffer.append( StringUtils.leftPad( AmountHelper.formatISKAmount( minPrice.getSellPrice() ) , 10  )+" ISK");
			buffer.append("\n");
		}
		
		buffer.append("\n");
		
		final List<CsvRow> data = new ArrayList<CsvRow>();
		
		for ( int i = 0 ; i < tableModel.getRowCount() ; i++ ) {
			
			final TableRow row = tableModel.getRow( i );
			final ISKAmount amount =tableModel.getISKperM3( row );

			data.add( new CsvRow( row.oreName , amount ) );
		}
		
		Collections.sort( data );
		
		for ( CsvRow r : data ) {
			buffer.append( StringUtils.rightPad( r.oreName , 13 ) ).append(" : ");
			buffer.append( StringUtils.leftPad(  AmountHelper.formatISKAmount( r.iskPerM3 ) , 10  )+" ISK / m3");
			buffer.append("\n");
		}
		
		clipboard.setContents( new PlainTextTransferable( buffer.toString() ) , null );
	}

	public OreChartComponent() {
		super();
		this.priceCache = new PriceCache();
		this.mineralPriceTableModel = new MineralPriceTableModel();
		mineralPriceTable.setModel( mineralPriceTableModel );
		registerChildren( oreHoldValue );
	}
	
	private OreVariant getSelectedOreVariant() {
		OreVariant result = (OreVariant) oreVariantChooser.getSelectedItem();
		return result == null ? OreVariant.BASIC : result;
	}

	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		if ( refiningData == null ) {
			refiningData = new OreRefiningData( dataModel );
		}
		if ( tableModel == null ) {
			tableModel = new MyTableModel();
		}
		tableModel.refresh();
		mineralPriceTableModel.refresh();
	}
	
	protected void refresh() {
		tableModel.refresh();
		mineralPriceTableModel.refresh();
		oreHoldValue.setItems( Arrays.asList("dummyValue" ) );
	}
	
	private final class MyTableModel extends AbstractTableModel<TableRow> {

		private List<TableRow> rows =
			new ArrayList<TableRow>();

		private final List<String> mineralNames;
		private final int lastMineralColumn;

		public MyTableModel() {
			super(new TableColumnBuilder()
			.add("Ore")
			.addAll( OreRefiningData.getMineralNames() , Integer.class )
			.add("ISK / m3" , ISKAmount.class ) );

			this.mineralNames = OreRefiningData.getMineralNames();
			lastMineralColumn=FIRST_MINERAL_COLUMN+this.mineralNames.size()-1;
		}

		public void refresh() 
		{
			rows.clear();

			final OreVariant oreVariant = getSelectedOreVariant();
			for ( String ore : refiningData.getOreNames( oreVariant ) ) 
			{
				final RefiningOutcome outcome =  refiningData.getRawOutcome( ore );

				rows.add( 
						new TableRow(  
								outcome.getType( oreVariant ) , 
								ore ,
								refiningData.getRefiningOutcome( ore ) 
						)
				);
			}
			modelDataChanged();
		}

		public boolean isHighestMineralYield(int modelRow,int modelColumn) {

			if ( modelColumn >= FIRST_MINERAL_COLUMN && modelColumn <= lastMineralColumn ) 
			{
				int maxYield= 0;
				int maxRow = 0;
				for ( int i = 0 ; i < rows.size() ; i++ ) 
				{
					final int yield =
						rows.get(i).getYieldForMineral( modelColumn  ).getQuantity();

					if ( yield > maxYield ) {
						maxYield = yield;
						maxRow = i;
					}
				}
				return modelRow == maxRow;
			}

			return false;
		}

		@Override
		protected Object getColumnValueAt(int modelRow,int modelColumn)
		{
			final TableRow r = getRow( modelRow );

			if ( modelColumn == 0 ) {
				return r.oreName;
			} else if ( modelColumn >= FIRST_MINERAL_COLUMN && modelColumn <= lastMineralColumn ) {
				return r.getYieldForMineral( modelColumn  ).getQuantity();
			} else if ( modelColumn == lastMineralColumn+1 ) {
				return getISKperM3( r );
			} else {
				throw new IllegalArgumentException("Unhandled column "+modelColumn);
			}
		}
		
		public ISKAmount getISKperM3(TableRow r) 
		{
			return OreChartComponent.this.getISKperM3( r.oreType , r.data );
		}

		@Override
		public TableRow getRow(int modelRow)
		{
			return rows.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return rows.size();
		}
	}
	
	protected ISKAmount getISKperM3(InventoryType oreType,List<? extends ItemWithQuantity> outcome) 
	{
		final double unitsPerM3 = 1.0d / oreType.getVolume();
		final double batchesPerM3 = unitsPerM3 / oreType.getPortionSize();

		final long avgSellPrive = calculateBatchSellPrice( outcome );

		return new ISKAmount( avgSellPrive ).multiplyBy( batchesPerM3 );
	}	
	
	protected long calculateBatchSellPrice(List<? extends ItemWithQuantity> items) 
	{
		long batchValue=0;
		for ( ItemWithQuantity x : items) 
		{
			long avgMineralSellPrive = priceCache.getSellPrice( x.getType() ).getAveragePrice();

			batchValue += (  avgMineralSellPrive * x.getQuantity() );
		}
		return batchValue;
	}	

	private final TableCellRenderer tableRenderer =
		new DefaultTableCellRenderer() {

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus,
				int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);

			if ( column < FIRST_MINERAL_COLUMN ) {
				setHorizontalAlignment( JLabel.LEADING);
			} else {
				setHorizontalAlignment( JLabel.TRAILING );	
			}

			if ( ! isSelected && tableModel.isHighestMineralYield( 
					table.convertRowIndexToModel( row ) ,
					table.convertColumnIndexToModel( column ) ) ) 
			{
				setBackground( Color.GREEN );
			} else {
				if ( isSelected ) {
					setBackground( table.getSelectionBackground() );
				} else {
					setBackground( table.getBackground() );
				}
			}

			if ( value instanceof ISKAmount ) {
				setText( AmountHelper.formatISKAmount( (ISKAmount) value) + " ISK" );
			} 
			return this;
		}
	};

	@Override
	protected JPanel createPanel()
	{
		final JPanel result = new JPanel();

		table.setFillsViewportHeight( true );
		table.setModel( this.tableModel );
		table.setRowSorter( tableModel.getRowSorter() );
		table.setDefaultRenderer( String.class , tableRenderer );
		table.setDefaultRenderer( ISKAmount.class , tableRenderer );
		table.setDefaultRenderer( Integer.class , tableRenderer );
		
		final PopupMenuBuilder builder = new PopupMenuBuilder();
		builder.addItem( "Save summary to clipboard" , new Runnable() {

			@Override
			public void run()
			{
				saveSummaryToClipboard();
			}
		} );

		builder.attach( table );
		
		mineralPriceTable.setFillsViewportHeight( true );

		final ImprovedSplitPane splitPane =
			new ImprovedSplitPane(JSplitPane.VERTICAL_SPLIT,
					new JScrollPane( mineralPriceTable ) ,
					new JScrollPane( table ) 
			);
		
		splitPane.setDividerLocation( 0.3 );
		
		// setup ore hold panel
		final JPanel oreHoldPanel = new JPanel();
		oreHoldPanel.setLayout(new GridBagLayout());
		
		oreHoldSize.setColumns( 10 );
		
		new GridLayoutBuilder()
		.add(  new HorizontalGroup(
						 new VerticalGroup( 
								new HorizontalGroup( 
										new Cell( "ohLabel", new JLabel("Ore hold size (m3):") ),
										new Cell( "ohSize" , oreHoldSize )
						         ), 
								 new HorizontalGroup( 
											new Cell( "oreTypeLabel" , new JLabel("Ore type:") ),
											new Cell( "oreChooser" , oreChooser ) 
							     ), 
								 new HorizontalGroup( 
											new Cell( "oreVariantLabel", new JLabel("Ore variant:") ),
											new Cell( "oreVariantChooser" , oreVariantChooser ) 
							     )			 
						 ),
						 new Cell( "oreHoldValue" , oreHoldValue.getPanel() ) 
				 )		     
		).enableDebugMode().addTo( oreHoldPanel );
		
		final List<String> oreNames = refiningData.getOreNames(OreVariant.BASIC);
		Collections.sort(oreNames);
		
		oreChooser.setModel( new DefaultComboBoxModel<>( new Vector<String>( oreNames ) ) );
		oreChooser.setSelectedItem( oreNames.get(0) );
		
		oreVariantChooser.setModel( new DefaultComboBoxModel<>( OreVariant.values() ) );
		oreVariantChooser.setSelectedItem( OreVariant.BASIC );
		
		final ActionListener refreshListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		};
		
		oreHoldSize.addActionListener( refreshListener );
		oreChooser.addActionListener( refreshListener );
		oreVariantChooser.addActionListener( refreshListener );
		
		new GridLayoutBuilder()
		.add( new VerticalGroup( new Cell(oreHoldPanel).noResize() , new Cell( splitPane ) ) )
		.addTo( result );
		
		refresh();
		
		return result;
	}
	
	private final class MineralPrice {

		public final PriceInfo price;

		public MineralPrice(PriceInfo price) {
			this.price = price;
		}

		public String itemName() {
			return price.getItemType().getName();
		}

		public ISKAmount getSellPrice() {
			return new ISKAmount( price.getAveragePrice() );
		}
	}

	private final class MineralPriceTableModel extends AbstractTableModel<MineralPrice> {

		private final List<MineralPrice> prices =
			new ArrayList<MineralPrice> ();

		public MineralPriceTableModel() {
			super( new TableColumnBuilder()
			.add("Mineral").add("Price" ) );
		}

		public void refresh() {

			prices.clear();
			for ( String mineralName  : OreRefiningData.getMineralNames() ) {
				final InventoryType type= dataModel.getInventoryTypeByName( mineralName );
				prices.add( new MineralPrice( priceCache.getSellPrice( type ) ) );
			}
			modelDataChanged();
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 1;
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int col)
		{
			final MineralPrice p = getRow( modelRowIndex );

			if ( col == 0 ) {
				return p.itemName();
			} else if ( col == 1 ) {
				return AmountHelper.formatISKAmount( p.getSellPrice() );
			} else {
				throw new IllegalArgumentException("Invalid column index "+col);
			}
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex)
		{
			if ( columnIndex == 1 ) {
				
				final String label = "You are about to change the average sell price " +
						" of "+getRow( rowIndex ).itemName()+" in region '"+getDefaultRegion().getName()+"'";
				
				if ( dialogManager.showPermanentWarningDialog("warn_ore_chart_price_editing", "Warning", label) ) {
					long iskAmount = AmountHelper.parseISKAmount( (String) value );
					if ( iskAmount >= 0 ) {
						priceCache.updatePrice( getRow( rowIndex ).price ,iskAmount );
					}
				}
			}
		}

		@Override
		public MineralPrice getRow(int modelRow)
		{
			return prices.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return prices.size();
		}

	}
}
