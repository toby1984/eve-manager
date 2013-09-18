package de.codesourcery.eve.skills.ui.components.impl.planning;

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

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IMarketDataProvider.IPriceInfoChangeListener;
import de.codesourcery.eve.skills.production.OreRefiningData;
import de.codesourcery.eve.skills.production.ShoppingListManager;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.impl.ReverseRefiningComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.BlueprintNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ManufacturingJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.RequiredMaterialNode;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.spreadsheet.CellAttributes;
import de.codesourcery.eve.skills.ui.spreadsheet.IHighlightingFunction;
import de.codesourcery.eve.skills.ui.spreadsheet.ITableCell;
import de.codesourcery.eve.skills.ui.spreadsheet.ITableFactory;
import de.codesourcery.eve.skills.ui.spreadsheet.SimpleCell;
import de.codesourcery.eve.skills.ui.spreadsheet.SpreadSheetCellRenderer;
import de.codesourcery.eve.skills.ui.spreadsheet.SpreadSheetTable;
import de.codesourcery.eve.skills.ui.spreadsheet.SpreadSheetTableModel;
import de.codesourcery.eve.skills.ui.spreadsheet.TableRow;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.Cell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.VerticalGroup;
import de.codesourcery.eve.skills.ui.utils.PlainTextTransferable;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISKAmount;

/**
 * Component that renders a spreadsheet-like
 * (sorry, JTable just doesn't cut it here...)
 * table displaying the costs associated with
 * production of a given {@link ManufacturingJobRequest}.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class CostStatementComponent extends AbstractComponent 
{
	private static final Logger log = Logger.getLogger(CostStatementComponent.class);
	
	private static final IHighlightingFunction unknownCostHighlightingFunction = new IHighlightingFunction() 
	{
			@Override
			public boolean requiresHighlighting(ITableCell cell)
			{
				if ( ! ( cell instanceof CostPositionCell ) ) {
					return false;
				}
				final CostPositionCell costPositionCell = (CostPositionCell) cell;
				return costPositionCell.getCostPosition().getPricePerUnit().equals( ISKAmount.ZERO_ISK ) || 
					costPositionCell.getCostPosition().hasUnknownCost();
			}
	};

	// region used for price updates
	private Region defaultRegion = null;
	
	private volatile ManufacturingJobNode jobRequest;
	private volatile CostStatement statement;
	
	private final SpreadSheetTable table = new SpreadSheetTable();
	private TreeNodeCostCalculator costCalculator;

	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;

	@Resource(name="marketdata-provider")
	private IMarketDataProvider marketDataProvider;
	
	@Resource(name="shoppinglist-manager")
	private ShoppingListManager shoppingListManager;
	
	private final IPriceInfoChangeListener priceChangeListener = new IPriceInfoChangeListener() {

		@Override
		public void priceChanged(IMarketDataProvider caller, Region region, Set<InventoryType> type)
		{
			System.out.println("priceChanged(): Prices changed ... IMPLEMENT COST STATEMENT UPDATE");
			refresh();
		}
	};

	public void setCostCalculator(TreeNodeCostCalculator costCalculator)
	{
		if ( costCalculator == null ) {
			throw new IllegalArgumentException("costCalculator cannot be NULL");
		} 

		this.costCalculator = costCalculator;
	}

	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		marketDataProvider.addChangeListener( priceChangeListener );
	}

	@Override
	protected void onDetachHook()
	{
		marketDataProvider.removeChangeListener( priceChangeListener );
	}

	@Override
	protected void disposeHook()
	{
		marketDataProvider.removeChangeListener( priceChangeListener );
	}

	protected int getAsIntValue(JTextComponent comp) {
		return Integer.parseInt( comp.getText() );
	}

	@Override
	protected JPanel createPanel()
	{
		final JPanel result = new JPanel();
		
		new SpreadSheetCellRenderer().attachTo( table );
		
		table.setFillsViewportHeight( true );
		table.setBorder(BorderFactory.createLineBorder(Color.BLACK ) );
		
		final VerticalGroup vGroup = new VerticalGroup( new Cell( new JScrollPane( table ) ) );
		
		new GridLayoutBuilder().add(vGroup ).addTo( result );
		
		final PopupMenuBuilder builder =
			new PopupMenuBuilder();
		
		builder.addItem("Calculate required ore amounts" , new AbstractAction() {

			private List<ItemWithQuantity> items;
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				calculateRequiredOres( items );
			}
			
			@Override
			public boolean isEnabled()
			{
				if ( jobRequest != null && statement != null && table.getModel().getRowCount() > 0 ) {
					items = getAllMineralsFromCostStatement();
					return ! items.isEmpty();
				} 
				return false;
			}
		} );
		
		builder.addItem("Create shopping list...", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				createNewShoppingList();
			}

		});
		
		builder.addItem("Put on clipboard (text)" , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				putOnClipboard();
			}
		} );
		
		builder.addItem("Show resource status" , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				ManufacturingJobRequest request =
					jobRequest.getManufacturingJobRequest();
				
				final ResourceStatusComponent comp =
					new ResourceStatusComponent( "Production of "+
							request.getQuantity()+" x "+
							request.getBlueprint().getProductType().getName() 
				);
				
				comp.setData( request .getCharacter() , getRequiredMaterialsFromCostStatement() );
				
				comp.setModal( true );
				ComponentWrapper.wrapComponent( "Resource status" , comp ).setVisible( true );
			}
		} );		
		
		builder.attach( table );
		
		refresh();
		return result;
	}
	
	private void createNewShoppingList()
	{
		final ManufacturingJobRequest job = jobRequest.getManufacturingJobRequest();
		
		final List<ItemWithQuantity> items =
			new ArrayList<ItemWithQuantity>();
		
		for ( CostPosition p : statement.getCostPositions() ) 
		{
			
			final ITreeNode n = p.getTreeNode();
			
			if ( n instanceof RequiredMaterialNode ) 
			{
				items.add( 
					new ItemWithQuantity(
						((RequiredMaterialNode ) n).getRequiredMaterial().getType(),
						p.getQuantity() 
					) 
				);
			} 
			else if ( n instanceof BlueprintNode ) {
				final BlueprintNode node = (BlueprintNode) n;
				
				if ( ( (BlueprintNode) n ).isRequiresOriginal() ) {
				items.add( 
						new ItemWithQuantity(
							node.getBlueprint().getType().getBlueprintType(),
							p.getQuantity() 
						) 
					);
				}
			}
		}
		
		final ShoppingListEditorComponent comp =  new ShoppingListEditorComponent( "Production of "+job.getQuantity()+" x "+ job.getBlueprint().getProductType().getName(), "" , items );
		
		comp.setModal( true );
		ComponentWrapper.wrapComponent( comp ).setVisible( true );
		if ( ! comp.wasCancelled() && ! comp.getShoppingList().isEmpty() )
		{
			shoppingListManager.addShoppingList( comp.getShoppingList() );
		}
	}
	
	protected void calculateRequiredOres(List<ItemWithQuantity> items) {
		
		final ReverseRefiningComponent component = new ReverseRefiningComponent();
		
		component.setSelectedCharacter( jobRequest.getManufacturingJobRequest().getCharacter() );
		component.setRequiredMinerals( items );
		
		ComponentWrapper.wrapComponent( component ).setVisible( true );
	}
	
	private List<ItemWithQuantity> getRequiredMaterialsFromCostStatement()
	{
		final List<ItemWithQuantity> items = new ArrayList<ItemWithQuantity>();
		
		for ( CostPosition p : statement.getCostPositions() )
		{
			if ( p.getKind() != CostPosition.Kind.FIXED_COSTS ) {
				continue;
			}
			
			if ( p.getTreeNode() == null || ! ( p.getTreeNode() instanceof RequiredMaterialNode) ) {
				continue;
			}
			final RequiredMaterialNode node = (RequiredMaterialNode) p.getTreeNode();
			
			final InventoryType material = node.getRequiredMaterial().getType();
			items.add( new ItemWithQuantity( material, p.getQuantity() ) );
		}
		return items;
	}
	
	private List<ItemWithQuantity> getAllMineralsFromCostStatement()
	{
		final List<ItemWithQuantity> items = new ArrayList<ItemWithQuantity>();
		
		System.out.println("---------------- "+statement+" ------------");
		
		for ( CostPosition p : statement.getCostPositions() ) 
		{
			if ( p.getTreeNode() == null || ! ( p.getTreeNode() instanceof RequiredMaterialNode) ) {
				continue;
			}
			final RequiredMaterialNode node = (RequiredMaterialNode) p.getTreeNode();
			
			final InventoryType material = node.getRequiredMaterial().getType();
			if ( ! OreRefiningData.isMineral( material ) ) {
				continue;
			}
			final ItemWithQuantity mineral = new ItemWithQuantity( material, p.getQuantity() );
			items.add( mineral );
		}
		return items;
	}

	public void setManufacturingJobRequest(final ManufacturingJobNode node) 
	{
		this.jobRequest = node;
		refresh();
	}

	public void refresh() 
	{
		SwingUtilities.invokeLater( new Runnable() {

			@Override
			public void run()
			{
				if ( jobRequest != null ) 
				{
					System.out.println("\n==== BEFORE refresh() =====\n"+statement);
					populateProductionCostTable();
					System.out.println("\n==== AFTER refresh() =====\n"+statement);
				}
			}
		});
	}

	protected static ITableCell cell(CostPosition pos , ISKAmount amount) {
		return new CostPositionCell( pos , toString( amount ) ); 
	}
	
	protected interface ICellEditingCallback {
		public void setValue(ITableCell cell,Object value);
	}
	
	protected static ITableCell editableCell(CostPosition pos , ISKAmount amount, final ICellEditingCallback callback) 
	{
		return new CostPositionCell( pos , toString( amount ) ) 
		{
			@Override
			public void setValue(Object value) 
			{
				callback.setValue( this , value );
			}
			
			@Override
			public boolean isEditable() {
				return true;
			}
		};
	}	
	
	protected static ITableCell cell(ISKAmount amount) {
		return cell( toString( amount ) );
	}

	protected static ITableCell cell(String text) {
		return new SimpleCell( text );
	}

	protected static ITableCell emptyCell() {
		return new SimpleCell( "" );
	}
	
	private String toString(ITableCell cell) {
		return cell.getValue() != null ? cell.getValue().toString() : "";
	}
	
	protected void putOnClipboard() {
		
		// create text
		final SpreadSheetTableModel model =
			(SpreadSheetTableModel ) this.table.getModel();
		
		int maxColumnCount = 1;
		
		final Map<Integer,Integer> maxColumnWidth = 
			new HashMap<Integer,Integer> ();
		
		for ( TableRow r : model.getRows() ) 
		{
			if ( r.getCellCount() > maxColumnCount ) {
				maxColumnCount = r.getCellCount();
			}
			
			for ( int i = 0 ; i < r.getCellCount() ; i++ ) 
			{
				
				Integer maxWidth = maxColumnWidth.get( i );
				if ( maxWidth == null ) {
					maxWidth = new Integer(0);
					maxColumnWidth.put( i , maxWidth );
				}
				final ITableCell cell = r.getCell( i );
				if ( cell.getValue() != null ) 
				{
					final int len =
						toString( cell ).length();
					
					if ( len > maxWidth.intValue() ) {
						maxWidth = new Integer( len );
						maxColumnWidth.put( i , maxWidth );
					}
				}
			}
		}

		final StringBuffer text = new StringBuffer();
		
		for ( TableRow r : model.getRows() ) {
			for ( int i = 0 ; i < r.getCellCount() ; i++ ) 
			{
				final int width = maxColumnWidth.get( i );
				final ITableCell cell = r.getCell( i );
				if ( i == 0 ) {
					text.append( StringUtils.rightPad( toString( cell ) , width ) ).append(" | ");
				} else {
					text.append( StringUtils.leftPad( toString( cell ) , width ) ).append(" | ");
				}
			}
			text.append("\n");
		}
		
		
		// put on clipboard
		final Clipboard clipboard =  Toolkit.getDefaultToolkit().getSystemClipboard(); 

		clipboard.setContents( new PlainTextTransferable( text.toString() ) , null );
	}

	protected Region getDefaultRegion() 
	{
		if ( defaultRegion == null ) 
		{
			IRegionQueryCallback callback = RegionSelectionDialog.createCallback( null , dataModel );
			defaultRegion = callback.getRegion("Please selected the region for which to set buy prices");
		}
		return defaultRegion;
	}
		
	
	protected void populateProductionCostTable() 
	{
		if ( jobRequest == null || costCalculator == null ) {
			System.out.println("populateProductionCostTable(): Cannot update - dependencies not set.");
			return;
		}

		final SpreadSheetTableModel tableModel = new SpreadSheetTableModel( new ITableFactory() {

			@Override
			public ITableCell createEmptyCell(int row, int column)
			{
				return new SimpleCell();
			}

			@Override
			public TableRow createRow(SpreadSheetTableModel tableModel)
			{
				return new TableRow( tableModel );
			}

		} );

		final ProductionCostStatementGenerator calc = new ProductionCostStatementGenerator( jobRequest , costCalculator , dataModel );

		statement =  calc.createCostStatement();

		tableModel.addRow( 
				rightAligned( bold( cell( "Product:") ) ), 
				rightAligned( bold( cell( jobRequest.getProduct().getName() ) ) ) 
		);

		tableModel.addRow( 
				rightAligned( bold ( cell( "Produced quantity:") ) ) , 
				rightAligned( bold ( cell( ""+jobRequest.getQuantity() ) ) )
		);

		tableModel.addEmptyRow();

		tableModel.addRow( 
				centered( bold( cell( "Cost type" ) ) ),
				centered( bold( cell( "Quantity" ) ) ),
				centered( bold( cell( "Description" ) ) ), 
				centered( bold( cell( "Price per unit" ) ) ),
				centered( bold( cell( "Total costs" ) ) ) 
		);
		
		// sort by type & description
		final List<CostPosition> sorted = new ArrayList<CostPosition>( statement.getCostPositions() );
		
		Collections.sort( sorted , new Comparator<CostPosition>() 
		{
			@Override
			public int compare(CostPosition o1, CostPosition o2)
			{
				
				if ( o1.getKind() != o2.getKind() ) {
					if ( o1.getKind() == CostPosition.Kind.FIXED_COSTS ) {
						return -1;
					} else if ( o1.getKind() == CostPosition.Kind.VARIABLE_COSTS) {
						return 0;
					}
					return 1;
				}
				return o1.getDescription().compareTo( o2.getDescription() );
			}} );
		
		// 
		ISKAmount totalFixedCost = ISKAmount.ZERO_ISK;
		ISKAmount totalVariableCost = ISKAmount.ZERO_ISK;
		ISKAmount totalOneTimeCost = ISKAmount.ZERO_ISK;
		for ( final CostPosition p : sorted ) 
		{
			final ISKAmount amount = getTotalAmount( p );
			
			final ICellEditingCallback cellEditCb = new ICellEditingCallback() {
				
				@Override
				public void setValue(ITableCell cell, Object value) 
				{
					final CostPositionCell cp = (CostPositionCell) cell;
					final InventoryType itemType = cp.getCostPosition().getItemType();
					if ( itemType != null && value instanceof String && StringUtils.isNotBlank((String) value) ) 
					{
						try 
						{
							final long parsed = AmountHelper.parseISKAmount( (String) value );
							
							final PriceInfo priceInfo = new PriceInfo(Type.BUY,itemType,Source.USER_PROVIDED);
							priceInfo.setRegion( getDefaultRegion() );
							priceInfo.setMinPrice( parsed );
							priceInfo.setAveragePrice( parsed );
							priceInfo.setMaxPrice( parsed );
							priceInfo.setTimestamp( new EveDate( getSystemClock() ) );
							
							marketDataProvider.store( priceInfo );
						} 
						catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			tableModel.addRow(
					centered( cell( classify( p) ) ),
					rightAligned( cell( ""+p.getQuantity() ) ),
					leftAligned(  cell( p.getDescription() ) ),
					rightAligned( highlightUnknownCost( editableCell( p , p.getPricePerUnit() , cellEditCb) ) ),
					rightAligned( highlightUnknownCost( cell( p , amount ) ) )
			);

			if ( p.getKind() == Kind.ONE_TIME_COSTS ) {
				totalOneTimeCost = amount.addTo( totalOneTimeCost );
			} else if ( p.getKind() == Kind.FIXED_COSTS ) {
				totalFixedCost = amount.addTo( totalFixedCost );
			} else if ( p.getKind() == Kind.VARIABLE_COSTS ) {
				totalVariableCost = amount.addTo( totalVariableCost );
			} else {
				throw new RuntimeException("Internal error, unhandled cost position kind "+p.getKind() );
			}
		}

		tableModel.addEmptyRow();

		tableModel.addRow( 
				emptyCell(),
				emptyCell(),
				emptyCell(),
				leftAligned( bold( cell("Total (fixed)") ) ),
				bold( rightAligned( cell( totalFixedCost ) ) )
		);

		tableModel.addRow( 
				emptyCell(),
				emptyCell(),
				emptyCell(),
				leftAligned( bold( cell("Total (variable)") ) ),
				bold( rightAligned( cell( totalVariableCost ) ) )
		);

		tableModel.addRow( 
				emptyCell(),
				emptyCell(),
				emptyCell(),
				leftAligned( bold( cell("Total (one-time)") ) ),
				bold( rightAligned( cell( totalOneTimeCost ) ) )
		);

		tableModel.addEmptyRow();

		tableModel.addRow( 
				emptyCell(),
				emptyCell(),
				emptyCell(),
				leftAligned( bold( cell("Total (fixed + variable)") ) ),
				bold( rightAligned( cell( totalVariableCost.addTo( totalFixedCost ) ) ) )
		);

		tableModel.addEmptyRow();
		
		final ISKAmount pricePerUnit =
			new ISKAmount( totalVariableCost.addTo( totalFixedCost ).toDouble() / 
					jobRequest.getManufacturingJobRequest().getQuantity() );
		
		tableModel.addRow( 
				emptyCell(),
				emptyCell(),
				emptyCell(),
				leftAligned( bold( cell("Price per unit") ) ),
				bold( rightAligned( cell( pricePerUnit ) ) )
		);
		
		table.setModel( tableModel );
		final JTextField tf = new JTextField();
		table.setDefaultEditor( ITableCell.class , new DefaultCellEditor(tf) );	
	}

	protected static ITableCell centered(ITableCell cell) {
		return align( cell, CellAttributes.Alignment.CENTERED );
	}

	protected static ITableCell leftAligned(ITableCell cell) {
		return align( cell, CellAttributes.Alignment.LEFT);
	}

	protected static ITableCell rightAligned(ITableCell cell) {
		return align( cell, CellAttributes.Alignment.RIGHT );
	}

	protected static ITableCell align(ITableCell cell , CellAttributes.Alignment alignment) {
		cell.getAttributes().setAttribute( CellAttributes.ALIGNMENT , alignment );
		return cell;
	}
	
	protected static ITableCell highlightUnknownCost(ITableCell cell)
	{
		if ( cell instanceof CostPositionCell ) {
			cell.getAttributes().setAttribute( CellAttributes.HIGHLIGHTING_FUNCTION, unknownCostHighlightingFunction );
			return cell;
		} 
		throw new IllegalArgumentException("Invalid cell class "+cell.getClass().getName() );
	}
	
	protected static ITableCell bold(ITableCell cell) {
		cell.getAttributes().setAttribute( CellAttributes.RENDER_BOLD, Boolean.TRUE );
		return cell;
	}

	private String classify(CostPosition p)
	{
		switch(p.getKind() )
		{
			case FIXED_COSTS:
				return "fixed";
			case VARIABLE_COSTS:
				return "variable";
			case ONE_TIME_COSTS:
				return "one-time";
			default:
				throw new IllegalArgumentException("Don't know how to render cost position with kind "+p.getKind());
		}
	}

	private ISKAmount getTotalAmount(CostPosition p) 
	{
		if ( p.getType() == CostPosition.Type.TOTAL ) {
			return p.getPricePerUnit();
		}
		return p.getPricePerUnit().multiplyBy( p.getQuantity() );
	}

}
