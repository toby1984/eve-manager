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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.market.MarketLogEntry;
import de.codesourcery.eve.skills.market.impl.MarketLogFile;
import de.codesourcery.eve.skills.market.impl.MarketLogFile.IMarketLogFilter;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.DateHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class ImportMarketLogFileComponent extends AbstractEditorComponent {

	private static final Logger log = 
		Logger.getLogger(MyModel.class);

	// table column indices
	private static final int VALID_IDX = 0;
	private static final int TYPE_IDX = 1;
	private static final int DATE_IDX = 2;
	private static final int VOLUME_IDX=3;
	private static final int REMAINING_VOLUME_IDX=4;
	private static final int PRICE_IDX = 5;

	// input file
	private final MarketLogFile logFile;
	
	@Resource(name="appconfig-provider")
	private IAppConfigProvider configProvider;
	
	@Resource(name="system-clock")
	private ISystemClock  systemClock;
	
	// crap filter
	private final JCheckBox priceFilter = 
		new JCheckBox("Filter by min./max. price ?",false);

	private final JTextField minPriceFilter = new JTextField("1.0");
	private final JTextField maxPriceFilter = new JTextField("1.0");

	private final JCheckBox orderVolumeFilter = new JCheckBox("Filter by order volume ?",false);
	private final JTextField minOrderSize = new JTextField("1.0");
	private final JTextField maxOrderSize = new JTextField("1.0");

	private final JComboBox requiredOrderType =
		new JComboBox();

	// text are showing min / avg. / max price and standard deviation

	//	private final JTextArea infoArea = new JTextArea( 15 , 30 );
	private final JTextArea infoArea = new JTextArea();

	// Table showing imported data
	private final JTable table = new JTable();
	private final MyModel model = new MyModel();

	// other stuff
	private PopupMenuBuilder popupMenuBuilder;
	
	private final ActionListener actionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			importFilter.updateFilter();
			
			if ( e.getSource() == requiredOrderType ) {
				model.getRowSorter().setRowFilter( rowFilter );
			}
		}
	};

	private final MyFilter importFilter = new MyFilter();
	
	private final RowFilter<AbstractTableModel<LogEntryWrapper>, Integer> rowFilter =
		new RowFilter<AbstractTableModel<LogEntryWrapper>, Integer>() {

		@Override
		public boolean include( javax.swing.RowFilter.Entry<? extends AbstractTableModel<LogEntryWrapper>, ? extends Integer> entry) 
		{
			
				Integer row = entry.getIdentifier();
				LogEntryWrapper wrapper = 
					entry.getModel().getRow( row );
				
				return wrapper.entry().getType().matches( (Type) requiredOrderType.getSelectedItem() );
		} 
	};

	private final class MyFilter implements IMarketLogFilter {

		private boolean filterByPrice=false;
		private double minPrice;
		private double maxPrice;

		private boolean filterByVolume =false;
		private double minVolume;
		private double maxVolume;

		private PriceInfo.Type type = PriceInfo.Type.ANY;

		public MyFilter() {
		}

		@Override
		public boolean includeInResult(MarketLogEntry entry) {
			
			final LogEntryWrapper wrapper =
				model.getWrapperFor( entry );
			
			if ( wrapper != null && wrapper.isEnteredByUser() ) {
				return wrapper.valid;
			}

			if ( filterByPrice ) {
				if ( entry.getPrice() < minPrice || entry.getPrice() > maxPrice ) {
					return false;
				}
			}

			if ( filterByVolume ) {
				if ( entry.getVolume() < minVolume || entry.getVolume() > maxVolume ) {
					return false;
				}
			}

			return entry.getType().matches( this.type );
		}

		public void updateFilter() {

			double value;
			boolean filterChanged = false;

			if ( priceFilter.isSelected() != filterByPrice) {
				filterChanged=true;
				this.filterByPrice = priceFilter.isSelected();
			} 

			if ( this.filterByPrice ) {
				value =readDouble( minPriceFilter );
				if ( value != minPrice ) {
					this.minPrice = value;
					filterChanged = true;
				}

				value =readDouble( maxPriceFilter );
				if ( value != maxPrice ) {
					this.maxPrice = value;
					filterChanged = true;
				}

				if ( this.minPrice > this.maxPrice ) {
					double tmp = this.maxPrice;
					this.maxPrice = this.minPrice;
					this.minPrice = tmp;
				}
			}

			if ( orderVolumeFilter.isSelected() != filterByVolume ) {
				this.filterByVolume = orderVolumeFilter.isSelected();
				filterChanged = true;
			}

			if ( filterByVolume ) {
				value =readDouble( minOrderSize );
				if ( value != minVolume ) {
					this.minVolume = value;
					filterChanged = true;
				}

				value =readDouble( maxOrderSize);
				if ( value != maxVolume) {
					this.maxVolume= value;
					filterChanged = true;
				}

				if ( this.minVolume > this.maxVolume ) {
					double tmp = this.maxVolume;
					this.maxVolume = this.minVolume;
					this.minVolume= tmp;
				}
			}

			if ( this.type != getRequiredOrderType() ) {
				this.type = getRequiredOrderType();
				filterChanged = true;
			}

			if ( filterChanged ) {
				model.filterChanged();
			}

			updateInfoTextArea();
		}

		protected Double readDouble(JTextField field) {
			return Double.parseDouble( field.getText() );
		}

		protected void assertValidDouble(JTextField textField) {
			final String value = textField.getText();
			if ( value != null ) {
				String trimmed = value.trim();
				if ( trimmed.length() != value.length() ) {
					textField.setText( trimmed );
				}
				try {
					Double.parseDouble( trimmed );
				} catch(Exception e) {
					textField.setText("1.0");
				}
			} else {
				textField.setText("1.0");
			}
		}
	}

	public ImportMarketLogFileComponent(MarketLogFile logFile) {
		if ( logFile == null ) {
			throw new IllegalArgumentException("logFile cannot be NULL");
		}
		this.logFile = logFile;
	}

	protected PriceInfo.Type getRequiredOrderType() {
		return (Type) requiredOrderType.getSelectedItem();
	}

	protected void updateInfoTextArea() {

		final StringBuilder result =
			new StringBuilder();

		final MarketLogFile logFile =
			getMarketLogFile();

		result.append("Item: "+logFile.getInventoryType().getName()+
				" ("+logFile.getInventoryType().getId()+")\n");
		result.append("Region: "+logFile.getRegion().getName()+"\n\n");

		result.append("Start date: "+ DateHelper.format( logFile.getStartDate().getLocalTime() )+"\n");
		result.append("End   date: "+ DateHelper.format( logFile.getEndDate().getLocalTime() )+"\n\n");

		result.append("Order count: "+logFile.getOrderCount( Type.ANY )+"\n\n");

		double minPrice = this.logFile.getMinPrice( getRequiredOrderType() ,
				IMarketLogFilter.NOP_FILTER );

		double maxPrice = this.logFile.getMaxPrice( getRequiredOrderType() ,
				IMarketLogFilter.NOP_FILTER );

		double avgPrice = this.logFile.getAveragePrice( getRequiredOrderType() ,
				IMarketLogFilter.NOP_FILTER );

		double stdDeviation = this.logFile.getStandardDeviation( getRequiredOrderType() ,
				IMarketLogFilter.NOP_FILTER );

		result.append("---- unfiltered ----\n\n");

		result.append("Minimum price: "+AmountHelper.formatISKAmount( minPrice )+" ISK\n");
		result.append("Average price: "+AmountHelper.formatISKAmount( avgPrice )+" ISK\n");
		result.append("Maximum price: "+AmountHelper.formatISKAmount( maxPrice )+" ISK\n\n");

		result.append("Standard deviation: "+AmountHelper.formatISKAmount( stdDeviation )+" ISK\n\n");

		result.append("---- filtered ----\n\n");

		minPrice = this.logFile.getMinPrice( getRequiredOrderType() , this.importFilter );

		maxPrice = this.logFile.getMaxPrice( getRequiredOrderType() , this.importFilter );

		avgPrice = this.logFile.getAveragePrice( getRequiredOrderType() , this.importFilter );

		stdDeviation = this.logFile.getStandardDeviation( getRequiredOrderType() , this.importFilter );

		result.append("Minimum price: "+AmountHelper.formatISKAmount( minPrice )+" ISK\n");
		result.append("Average price: "+AmountHelper.formatISKAmount( avgPrice )+" ISK\n");
		result.append("Maximum price: "+AmountHelper.formatISKAmount( maxPrice )+" ISK\n\n");

		result.append("Standard deviation: "+AmountHelper.formatISKAmount( stdDeviation )+" ISK\n\n");

		this.infoArea.setText( result.toString() );
		this.infoArea.setCaretPosition(0);
	}

	@Override
	protected JPanel createPanelHook() {

		final JPanel result = new JPanel();
		
		result.setLayout( new GridBagLayout() );

		final JPanel crapFilterPanel = new JPanel();
		crapFilterPanel.setLayout( new GridBagLayout() );
		crapFilterPanel.setBorder( BorderFactory.createTitledBorder("Import filter" ) );

		crapFilterPanel.add( priceFilter , constraints(0,0).width(3).anchorWest().noResizing().end() );
		priceFilter.addActionListener( actionListener );

		linkComponentEnabledStates( priceFilter , minPriceFilter );
		linkComponentEnabledStates( priceFilter , maxPriceFilter );

		final List<PriceInfo.Type> types = Arrays.asList( PriceInfo.Type.values() );
		requiredOrderType.setModel( new DefaultComboBoxModel<Type>( types ) );
		requiredOrderType.setSelectedItem( PriceInfo.Type.ANY );
		requiredOrderType.addActionListener( actionListener );

		minPriceFilter.setEnabled(false);
		maxPriceFilter.setEnabled(false);

		minPriceFilter.setColumns( 10 );
		maxPriceFilter.setColumns( 10 );

		minPriceFilter.addActionListener( actionListener );
		maxPriceFilter.addActionListener( actionListener );

		crapFilterPanel.add( minPriceFilter , constraints(1,1).anchorWest().noResizing().end() );
		crapFilterPanel.add( maxPriceFilter , constraints(2,1).anchorWest().noResizing().end() );

		// order volume filter
		crapFilterPanel.add( orderVolumeFilter , constraints(0,2).width(3).anchorWest().noResizing().end() );
		orderVolumeFilter.addActionListener( actionListener );

		linkComponentEnabledStates( orderVolumeFilter , minOrderSize );
		linkComponentEnabledStates( orderVolumeFilter , maxOrderSize );

		minOrderSize.addActionListener( actionListener );
		maxOrderSize.addActionListener( actionListener );

		minOrderSize.setEnabled(false);
		maxOrderSize.setEnabled(false);

		minOrderSize.setColumns( 10 );
		maxOrderSize.setColumns( 10 );

		crapFilterPanel.add( minOrderSize , constraints(1,3).anchorWest().noResizing().end() );
		crapFilterPanel.add( maxOrderSize , constraints(2,3).anchorWest().noResizing().end() );

		crapFilterPanel.add( new JLabel("Imported order types" ) , constraints(1,4).anchorWest().noResizing().end() );
		crapFilterPanel.add( this.requiredOrderType , constraints(2,4).anchorWest().noResizing().end() );

		result.add( crapFilterPanel , constraints(0,0).width(1).weightX(0.1).weightY(0.1).anchorWest().resizeBoth().end() );

		// add text area
		infoArea.setEditable( false );
		infoArea.setLineWrap( true );
		infoArea.setWrapStyleWord( true );

		result.add( new JScrollPane( infoArea ) , 
				constraints(1,0).width(1).resizeBoth().end() );

		// add table
		table.setModel( model );
		table.setDefaultRenderer( Double.class , new MyRenderer() );
		table.setDefaultRenderer( String.class , new MyRenderer() );
		table.setDefaultRenderer( EveDate.class , new MyRenderer() );
		table.setDefaultRenderer( Boolean.class , new MyBooleanRenderer() );
		table.setDefaultRenderer( Date.class , new MyRenderer() );

		table.setRowSorter( model.getRowSorter() );

		final JPanel splitPane = 
			createSplitPanePanel( result , new JScrollPane( table ) );

		popupMenuBuilder = new PopupMenuBuilder()
		.attach( table )
		.addItem( "Clear user overrides" , new AbstractAction() {

			@Override
			public boolean isEnabled() { return model.hasUserOverrides(); }
			
			@Override
			public void actionPerformed(ActionEvent e) { model.clearUserOverrides(); } 
		})
		.addSeparatorIfNotEmpty()
		.addItem( "Add selected prices to import", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) { markRowsManually( true ); }
			
			@Override
			public boolean isEnabled() { return hasMultipleLinesSelection(); }
		})
		.addItem( "Remove selected prices from import", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) { markRowsManually( false ); }
			
			@Override
			public boolean isEnabled() { return hasMultipleLinesSelection(); }
		});		
		
		updateInfoTextArea();

		return splitPane;
	}
	
	public List<PriceInfo> getPriceInfosForImport() {
		
		final ArrayList<PriceInfo> result =  new ArrayList<PriceInfo>();
		
		if ( wasCancelled() ) {
			return result;
		}
		
		final IMarketLogFilter removalFilter = new IMarketLogFilter() {

			@Override
			public boolean includeInResult(MarketLogEntry entry)
			{
				final LogEntryWrapper wrapper = model.getWrapperFor( entry );
				if ( wrapper.isEnteredByUser() ) {
					return wrapper.isValid(); // user did override filter
				}
				return importFilter.includeInResult( entry );
			}} ;
			
		return getMarketLogFile().getAggregatedOrders(  removalFilter , systemClock );
	}
	
	protected boolean hasMultipleLinesSelection() {
		final int[] selectedRows = table.getSelectedRows();
		return selectedRows != null && selectedRows.length > 1;
	}
	protected void markRowsManually(boolean selectForImport) {
		
		int[] selectedRows = table.getSelectedRows();
		if ( ArrayUtils.isEmpty(selectedRows ) ) {
			return;
		}
		
		int minRow=-1;
		int maxRow = -1;
		for ( int viewRow : selectedRows ) {
			
			final int modelRow =
				table.convertRowIndexToModel( viewRow );
			
			model.getRow( modelRow ).setEnteredByUser(true);
			model.getRow( modelRow ).setValid( selectForImport );
			
			if ( minRow == -1 || viewRow < minRow ) {
				minRow = viewRow;
			}
			
			if ( maxRow == -1 || viewRow > maxRow ) {
				maxRow = viewRow;
			}
		}

		model.rowsChanged( minRow , maxRow );
	}

	private JPanel createSplitPanePanel(JComponent top,JScrollPane bottom) {

		final JScrollPane resultPane =
			new JScrollPane(top);

		resultPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );

		resultPane.setMinimumSize( new Dimension(400, 150) );
		resultPane.setPreferredSize( new Dimension(600, 200) );

		bottom.setMinimumSize( new Dimension(100, 200) );
		bottom.setPreferredSize( new Dimension(600, 300) );

		final JSplitPane pane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT, top , bottom );

		//		pane.setResizeWeight( 0.5 );
		pane.setDividerLocation( 200 );

		pane.setContinuousLayout(true);


		final JPanel contentPanel =
			new JPanel();

		contentPanel.setLayout( new BorderLayout() );
		contentPanel.add( pane );
		return contentPanel;
	}

	private static final class LogEntryWrapper {

		private boolean enteredByUser=false;
		private boolean valid=true;
		private final MarketLogEntry entry;

		public LogEntryWrapper(MarketLogEntry entry) {
			this.entry = entry;
		}

		public void setValid(boolean yesNo) {
			this.valid = yesNo;
		}

		public boolean isValid() {
			return valid;
		}

		public MarketLogEntry entry() {
			return entry;
		}

		public void setEnteredByUser(boolean enteredByUser) {
			this.enteredByUser = enteredByUser;
		}

		public boolean isEnteredByUser() {
			return enteredByUser;
		}
	}

	@Override
	protected void onAttachHook(IComponentCallback callback) {
		
		if ( popupMenuBuilder != null ) {
			popupMenuBuilder.attach( table );
		}
		
		model.modelDataChanged();
	}
	
	@Override
	protected void onDetachHook() {
		popupMenuBuilder.detach( table );
	} 
	
	@Override
	protected void disposeHook() {
		if ( popupMenuBuilder != null ) {
			popupMenuBuilder.detach( table );
		}
	}

	public MarketLogFile getMarketLogFile() {
		return logFile;
	}

	private final class MyModel extends AbstractTableModel<LogEntryWrapper> {

		private final List<LogEntryWrapper> data = new ArrayList<LogEntryWrapper>();

		public MyModel() {
			super( new TableColumnBuilder()
			.add("Import ?",Boolean.class)
			.add("Type")
			.add("Date",EveDate.class, EveDate.COMPARATOR)			
			.add("Volume", Double.class)
			.add("Volume remaining", Double.class )
			.add("Price",Double.class) );
		}
		
		public void rowsChanged(int minRow, int maxRow) {
			fireEvent( new TableModelEvent( this,minRow,maxRow,TableModelEvent.ALL_COLUMNS , TableModelEvent.UPDATE ) );
		}

		public void clearUserOverrides() {

			synchronized ( data ) {
				for ( LogEntryWrapper wrapper : data ) {
					wrapper.setEnteredByUser( false );
				}
			}
			
			filterChanged();
		}

		public boolean hasUserOverrides() {
			synchronized ( data ) {
				for ( LogEntryWrapper wrapper : data ) {
					if ( wrapper.isEnteredByUser() ) {
						return true;
					}
				}
			}
			return false;
		}

		private LogEntryWrapper getWrapperFor(MarketLogEntry entry) {
			
			synchronized ( data ) {
				for ( LogEntryWrapper wrapper : data ) {
					if ( wrapper.entry() == entry ) {
						return wrapper;
					}
				}
			}
			return null;
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex) 
		{
			final LogEntryWrapper wrapper = getRow( modelRowIndex );
			final MarketLogEntry entry = wrapper.entry();

			switch( modelColumnIndex ) 
			{
			case VALID_IDX:
				return wrapper.isValid();
			case DATE_IDX:
				return entry.getIssueDate();
			case TYPE_IDX:
				return entry.isBuyOrder() ? "BUY" : "SELL";
			case VOLUME_IDX:
				return entry.getVolume();
			case REMAINING_VOLUME_IDX:
				return entry.getRemainingVolume();
			case PRICE_IDX:
				return entry.getPrice();
			default:
				throw new IllegalArgumentException("Invalid column index "+modelColumnIndex);
			}
		}

		public void filterChanged() {

			int firstRow = -1;
			int lastRow = -1;

			int row = 0;
			synchronized( data ) {
				for ( LogEntryWrapper wrapper : data ) 
				{
					final boolean valid;

					if ( ! wrapper.isEnteredByUser() ) {
						valid =
							importFilter.includeInResult( wrapper.entry() );
					} else {
						valid = wrapper.isValid();
					}

					if ( valid != wrapper.isValid() ) {
						wrapper.setValid(valid);
						if ( firstRow == -1 || row < firstRow ) {
							firstRow = row;
						}
						if ( lastRow == -1 || row > lastRow ) {
							lastRow = row;
						}
					}

					row++;
				}
			}

			if ( firstRow == -1 ) {
				return; // no rows changed
			}

			final TableModelEvent event =
				new TableModelEvent(this,firstRow,lastRow,TableModelEvent.ALL_COLUMNS,TableModelEvent.UPDATE);

			fireEvent( event );
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == VALID_IDX;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {

			if ( columnIndex != VALID_IDX ) {
				throw new IllegalArgumentException("Invalid column "+columnIndex);
			}

			LogEntryWrapper wrapper = getRow( rowIndex );
			wrapper.setValid((Boolean) value);
			wrapper.setEnteredByUser( true );
			notifyRowChanged( rowIndex );
			
			// TODO: Baaad... updateInfoTextArea() should be registered
			// as listener with this model
			updateInfoTextArea();
		}

		@Override
		public LogEntryWrapper getRow(int modelRow) {

			synchronized(data) {
				if ( modelRow < 0 || modelRow > data.size() ) {
					throw new IllegalArgumentException("Invalid model row "+modelRow);
				}

				return data.get( modelRow );
			}
		}

		@Override
		public int getRowCount() {
			synchronized(data) {
				return data.size();
			}
		}

		@Override
		public void modelDataChanged() {

			synchronized(data) {
				
				data.clear();

				for ( MarketLogEntry entry : getMarketLogFile().getOrders( importFilter ) ) {
					data.add( new LogEntryWrapper( entry ) );
				}

				Collections.sort( data , new Comparator<LogEntryWrapper>() {

					@Override
					public int compare(LogEntryWrapper o1, LogEntryWrapper o2) {
						return o2.entry().getIssueDate().compareTo( o1.entry().getIssueDate() );
					}
				});

			}

			super.modelDataChanged();
		}

	}

	private final ThreadLocal<NumberFormat> ORDER_VOLUME_FORMAT = new ThreadLocal<NumberFormat>() {
		protected NumberFormat initialValue() {
			return AmountHelper.createOrderVolumeNumberFormat();
		};
	};

	private final class MyRenderer extends DefaultTableCellRenderer {

		public MyRenderer() {
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int viewRow,
				int viewColumn) 
		{

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					viewRow, viewColumn);

			final int modelColumn = table.convertColumnIndexToModel( viewColumn );
			
			final int modelRow =
				table.convertRowIndexToModel( viewRow );
			
			if ( modelColumn == VOLUME_IDX || modelColumn == REMAINING_VOLUME_IDX ) {
				setText( ORDER_VOLUME_FORMAT.get().format( value ) );
			} else if ( modelColumn == DATE_IDX ) {
				setText(
						DateHelper.format( ((EveDate) value).getLocalTime() ) 
				);
			}

			final LogEntryWrapper wrapper =
				model.getRow( modelRow );
			
			if ( ! isSelected ) {
			
				if ( wrapper.isValid() ) {
					this.setBackground( table.getBackground() );
				} else {
					this.setBackground( Color.LIGHT_GRAY );		
				}
			
			} else {
				this.setBackground( table.getSelectionBackground() );
			}

			return this;
		}

	}

	private class MyBooleanRenderer extends JCheckBox implements TableCellRenderer, UIResource
	{
		private final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

		public MyBooleanRenderer() {
			super();
			setHorizontalAlignment(JLabel.CENTER);
			setBorderPainted(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int viewRow, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				super.setBackground(table.getSelectionBackground());
			}
			else {
				
				final int modelRow =
					table.convertRowIndexToModel( viewRow );
				
				final LogEntryWrapper wrapper =
					ImportMarketLogFileComponent.this.model.getRow( modelRow );
				
				if ( wrapper.isEnteredByUser() ) {
					setBackground( Color.ORANGE );
				} else {
					if ( wrapper.isValid() ) {
						setBackground(table.getBackground());
					} else {
						setBackground( Color.LIGHT_GRAY);
					}
				}
				
				setForeground(table.getForeground());
			}

			setSelected((value != null && ((Boolean)value).booleanValue()));

			if (hasFocus) {
				setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
			} else {
				setBorder(noFocusBorder);
			}

			return this;
		}
	}

	@Override
	protected JButton createCancelButton() {
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton() {
		return new JButton("Import data");
	}

	@Override
	protected boolean hasValidInput()
	{
		return true;
	}

}
