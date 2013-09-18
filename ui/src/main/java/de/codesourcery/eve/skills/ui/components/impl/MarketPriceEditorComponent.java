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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
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
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.dao.IInventoryTypeDAO;
import de.codesourcery.eve.skills.db.dao.IRegionDAO;
import de.codesourcery.eve.skills.db.dao.IStaticDataModelProvider;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IMarketDataProvider.IPriceInfoChangeListener;
import de.codesourcery.eve.skills.market.IMarketDataProvider.IUpdateStrategy;
import de.codesourcery.eve.skills.market.IMarketDataProvider.UpdateMode;
import de.codesourcery.eve.skills.market.IPriceQueryCallback;
import de.codesourcery.eve.skills.market.MarketFilter;
import de.codesourcery.eve.skills.market.MarketFilterBuilder;
import de.codesourcery.eve.skills.market.MarketLogEntry;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.market.impl.EveMarketLogParser;
import de.codesourcery.eve.skills.market.impl.MarketLogFile;
import de.codesourcery.eve.skills.market.impl.MarketLogFile.IMarketLogVisitor;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.impl.ByLabelFilterComponent.IItemLabelProvider;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.IViewFilter;
import de.codesourcery.eve.skills.ui.model.ListViewModel;
import de.codesourcery.eve.skills.ui.model.impl.PriceInfoTableModel;
import de.codesourcery.eve.skills.ui.model.impl.PriceInfoTableModel.TableEntry;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.PersistentDialogManager;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class MarketPriceEditorComponent extends AbstractComponent
{
    private static final Logger log = Logger.getLogger( MarketPriceEditorComponent.class );

    private static final Comparator<? super TableEntry> BY_ITEMNAME_COMPARATOR =
            new Comparator<TableEntry>() {

                @Override
                public int compare(TableEntry o1, TableEntry o2)
                {
                    return o1.getItem().getName().compareTo( o2.getItem().getName() );
                }
            };

    private final JTextArea selectedItemDescription = new JTextArea( "No item selected." );

    private final PriceInfoTableModel tableModel;
    private final MyModel viewModel = new MyModel();

    private final JTable table = new JTable();
    private final JScrollPane tablePane = new JScrollPane( table );

    private final JButton importMarketLogButton =
            new JButton( "Import EVE market logfiles" );

    private final ByLabelFilterComponent<TableEntry> itemNameFilter =
            new ByLabelFilterComponent<TableEntry>( "Item type filter",
                    new IItemLabelProvider<TableEntry>() {

                        @Override
                        public String getLabelFor(TableEntry obj)
                        {
                            return obj.getItemName();
                        }

                        @Override
                        public void viewFilterChanged(IViewFilter<TableEntry> filter)
                        {
                            tableModel.viewFilterChanged();
                        }
                    } );

    @Resource(name = "dialog-manager")
    private PersistentDialogManager dialogManager;

    @Resource(name = "datamodel-provider")
    private IStaticDataModelProvider dataModelProvider;

    @Resource(name = "system-clock")
    private ISystemClock systemClock;

    @Resource(name = "marketdata-provider")
    private IMarketDataProvider marketDataProvider;

    @Resource(name = "appconfig-provider")
    private IAppConfigProvider appConfigProvider;

    @Resource(name = "inventory-type-dao")
    private IInventoryTypeDAO itemDAO;

    @Resource(name = "region-dao")
    private IRegionDAO regionDAO;
    
    @Resource(name="static-datamodel")
    private IStaticDataModel staticDataModel;

    private int oldTooltipDismissalDelay = - 1;
    private final JPopupMenu popupMenu = new JPopupMenu();

    private final class MyModel extends ListViewModel<TableEntry> implements IPriceInfoChangeListener
    {
        @Override
        public void priceChanged(final IMarketDataProvider caller, final Region region,final Set<InventoryType> type)
        {
            refreshTable();
        }
    }

    protected List<PriceInfo> fetchPriceInfo(MarketFilter filter, InventoryType item)
    {
        try
        {
            final PriceInfoQueryResult result = marketDataProvider.getPriceInfo( filter, null , item );
            List<PriceInfo> list = new ArrayList<>();
            if ( result.hasBuyPrice() ) {
            	list.add( result.buyPrice() );
            }
            if ( result.hasSellPrice() ) {
            	list.add( result.sellPrice() );
            }
            return list;
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
    }
    
    /*
     * Mouse listener that gets attached to the table and displays a
     * context-sensitive popup-menu.
     * 
     * Depending on where in the table the user clicked, the menu offers price
     * update actions for
     * 
     * - missing buy , sell or buy+sell prices - outdated buy,sell or buy+sell
     * prices
     */
    private final MouseListener popupListener = new MouseAdapter() {

        @Override
        public void mousePressed(MouseEvent e)
        {
            maybeShowPopup( e );
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            maybeShowPopup( e );
        }

    };

    public MarketPriceEditorComponent(IMarketDataProvider marketDataProvider) {
        super();
        this.marketDataProvider = marketDataProvider;

        this.tableModel = new PriceInfoTableModel( marketDataProvider, getDefaultRegion(), this.viewModel, systemClock );
        registerChildren( this.itemNameFilter );
    }

    /**
     * Checks whether a mouse-click event should trigger a context-sensitive
     * popup menu and renders the menu if so.
     * 
     * @param e
     */
    private void maybeShowPopup(MouseEvent e)
    {

        if ( ! e.isPopupTrigger() )
        {
            return;
        }

        final int[] selectedRows = table.getSelectedRows();

        if ( ArrayUtils.isEmpty( selectedRows ) )
        {

            final int row = table.rowAtPoint( e.getPoint() );
            if ( row < 0 )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "maybeShowPopup(): Outside of table." );
                }
                return;
            }

            table.getSelectionModel().setSelectionInterval( row, row );
        }

        // translate row indices
        for (int i = 0; i < selectedRows.length; i++)
        {
            selectedRows[i] =
                    table.getRowSorter().convertRowIndexToModel( selectedRows[i] );
        }
        if ( populatePopupMenu( e, selectedRows ) )
        {
            popupMenu.show( e.getComponent(), e.getX(), e.getY() );
        }
    }

    @Override
    protected JPanel createPanel()
    {

        // add search textfield
        final JPanel inputPanel = new JPanel();
        inputPanel.setBorder( lineBorder( Color.black ) );
        inputPanel.setLayout( new GridBagLayout() );

        inputPanel.add( itemNameFilter.getPanel(), constraints( 0, 0 ).anchorWest()
                .noResizing().end() );

        importMarketLogButton.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                importMarketLogs();
            }

        } );

        inputPanel.add( importMarketLogButton, constraints( 0, 1 ).anchorWest()
                .noResizing().end() );

        // add item description text area
        selectedItemDescription.setEditable( false );
        selectedItemDescription.setWrapStyleWord( true );
        selectedItemDescription.setLineWrap( true );

        final JScrollPane selectedItemDescPanel =
                new JScrollPane( selectedItemDescription );
        selectedItemDescPanel.setBorder( BorderFactory
                .createTitledBorder( "Selected item" ) );

        // setup table
        tableModel.setViewFilter( this.itemNameFilter.getViewFilter() );
        table.setModel( tableModel );
        table.setRowSorter( tableModel.getRowSorter() );
        table.setDefaultRenderer( String.class, new StalePriceInfoHighlighter() );
        table.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        table.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e)
            {

                if ( ! e.getValueIsAdjusting() )
                {
                    final int viewRow = table.getSelectedRow();
                    if ( viewRow != - 1 )
                    {
                        final int modelRow = table.convertRowIndexToModel( viewRow );
                        updateItemDescriptionDisplay( modelRow );
                    }
                }
            }

        } );

        table.setFillsViewportHeight( true );
        table.addMouseListener( popupListener );

        table.setFillsViewportHeight( true );

        // setup split pane
        final JPanel result = new JPanel();

        final JPanel topPanel = new JPanel();

        new GridLayoutBuilder().add(
            new GridLayoutBuilder.HorizontalGroup(
                    new GridLayoutBuilder.Cell( inputPanel ), new GridLayoutBuilder.Cell(
                            selectedItemDescPanel ) ) ).addTo( topPanel );

        final JSplitPane pane =
                new ImprovedSplitPane( JSplitPane.VERTICAL_SPLIT, topPanel, tablePane );

        pane.setDividerLocation( 0.3d ); // only works because of the splitpane
        // hack
        pane.setContinuousLayout( true );

        result.setLayout( new GridBagLayout() );
        result.add( pane, constraints().resizeBoth().useRemainingSpace().end() );

        return result;
    }

    private void importMarketLogs()
    {

        File inputDir = null;
        if ( appConfigProvider.getAppConfig().hasLastMarketLogImportDirectory() )
        {
            inputDir = appConfigProvider.getAppConfig().getLastMarketLogImportDirectory();

            if ( ! inputDir.exists() || ! inputDir.isDirectory() )
            {
                inputDir = null;
            }
        }

        final JFileChooser chooser =
                inputDir != null ? new JFileChooser( inputDir ) : new JFileChooser();

        chooser.setFileHidingEnabled( false );
        chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        chooser.setMultiSelectionEnabled( true );

        if ( chooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
        {
            File[] files = chooser.getSelectedFiles();
            if ( ! ArrayUtils.isEmpty( files ) )
            {
                appConfigProvider.getAppConfig().setLastMarketLogImportDirectory(
                    files[0].getParentFile() );
                try
                {
                    appConfigProvider.save();
                }
                catch (IOException e)
                {
                    log.error( "importMarketLogs(): Failed to save configuration", e );
                }
                importMarketLogs( files );
            }
        }

    }

    private void importMarketLogs(File[] inputFiles)
    {
        if ( ArrayUtils.isEmpty( inputFiles ) )
        {
            return;
        }

        for (final File current : inputFiles)
        {
            final MarketLogFile logFile;
            try
            {
                logFile=new EveMarketLogParser( dataModelProvider.getStaticDataModel(),systemClock ).parseFile( current );
            }
            catch (Exception e)
            {
                log.error( "importMarketLogs(): Failed to parse " + current, e );
                displayError( "Failed to parse logfile " + current.getAbsolutePath()
                        + ": " + e.getMessage() );
                continue;
            }

            // make sure the user is aware when importing data from a file
            // other than the current default region /
            // is warned when importing data that conflicts
            // with already existing data.
            if ( ! reallyImportFile( logFile ) || logFile.isEmpty() )
            {
                log.debug( "importMarketLogs(): Skipping file "
                        + current.getAbsoluteFile() + " [ empty / user choice ]" );
                continue;
            }

            final ImportMarketLogFileComponent comp =
                    new ImportMarketLogFileComponent( logFile );

            comp.setModal( true );

            final Window window =
                    ComponentWrapper.wrapComponent( current.getName(), comp );

            if ( window == null )
            { // component may refuse to display
                // because the file holds data for a region other
                // from the current default region and the
                // user chose to cancel the operation
                continue;
            }

            window.setVisible( true );

            if ( comp.wasCancelled() )
            {
                continue; // user skipped this file
            }

            log.info( "importMarketLogs(): Importing data from file "+ current.getAbsolutePath() );
            this.marketDataProvider.store( comp.getPriceInfosForImport() );
        }
    }

    protected boolean reallyImportFile(MarketLogFile logFile)
    {
        if ( ! Region.isSameRegion( logFile.getRegion(), getDefaultRegion() ) )
        {
            final String label =
                    "<HTML><BODY>" + "WARNING !!! <BR><BR>"
                            + "This market logfile holds data from region '"
                            + logFile.getRegion().getName() + "' <BR>while your current "
                            + " default region is set to '"
                            + getDefaultRegion().getName() + "'.</BODY></HTML>";

            if ( ! dialogManager.showTemporaryWarningDialog(
                "marketlog_import_default_region_mismatch", "Region mismatch", label ) )
            {
                // user pressed the 'cancel' button
                return false;
            }
        }

        if ( ! checkForOrdersConflictingWithExistingData( logFile, PriceInfo.Type.BUY ) )
        {
            return false;
        }

        if ( ! checkForOrdersConflictingWithExistingData( logFile, PriceInfo.Type.SELL ) )
        {
            return false;
        }

        return true;
    }

    protected boolean checkForOrdersConflictingWithExistingData(MarketLogFile logFile,final PriceInfo.Type priceType)
    {

        // get date of latest price currently known
    	MarketFilter filter = new MarketFilterBuilder(priceType,logFile.getRegion()).end();
        final List<PriceInfo> history = fetchPriceInfo( filter , logFile.getInventoryType() );

        if ( history.isEmpty() || logFile.isEmpty() )
        {
            return true;
        }

        EveDate latestHistoryDate = null;
        for (PriceInfo info : history)
        {
            if ( latestHistoryDate == null|| info.getTimestamp().compareTo( latestHistoryDate ) >= 0 )
            {
                latestHistoryDate = info.getTimestamp();
            }
        }

        // get earliest price timestamp from market log
        final EveDate[] earliestFileDate = new EveDate[1];

        logFile.visit( new IMarketLogVisitor() {

            @Override
            public void visit(MarketLogEntry entry)
            {

                if ( ! priceType.matches( entry.getType() ) )
                {
                    return;
                }

                if ( earliestFileDate[0] == null
                        || entry.getIssueDate().compareTo( earliestFileDate[0] ) < 0 )
                {
                    earliestFileDate[0] = entry.getIssueDate();
                }
            }
        } );

        if ( earliestFileDate[0] == null )
        {
            return true;
        }

        // check for overlapping of price data from file
        // with already known prices

        if ( earliestFileDate[0].compareTo( latestHistoryDate ) <= 0 )
        {

            String typeLabel;
            switch ( priceType )
            {
                case BUY:
                    typeLabel = "buy";
                    break;
                case SELL:
                    typeLabel = "sell";
                    break;
                default:
                    throw new RuntimeException( "Unhandled switch/case" );
            }

            final String label =
                    "<HTML><BODY>"
                            + "<BR><BR>"
                            + "WARNING: This market logfile contains "
                            + typeLabel
                            + " orders that conflict <BR> with "
                            + "data already present in the application's database.<BR><BR>";

            final String[] options =
                    { "Ignore conflicting " + typeLabel + " orders", "Cancel" };

            final int userChoice =
                    JOptionPane.showOptionDialog( null, label,
                        "Orders from market log conflict with existing data",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                        options, options[0] );

            if ( userChoice == 0 )
            {
                final int size = logFile.size();
                final int removedCount =
                        logFile.removeEntriesOlderThan( latestHistoryDate, priceType );

                JOptionPane.showMessageDialog( null, "Removed " + removedCount + " of "
                        + size + " " + typeLabel + " orders total ( left: "
                        + logFile.size() + " )" );
            }
            else
            {
                return false;
            }
        }
        return true;
    }

    private void updateItemDescriptionDisplay(final int index)
    {
        final String text;
        if ( index >= 0 )
        {

            final TableEntry rowData = tableModel.getRow( index );
            final InventoryType item = rowData.getItem();

            final DecimalFormat FORMAT = new DecimalFormat( "#########0.000" );

            String volume = FORMAT.format( item.getVolume() ) + " m³";

            final double numberOfUnitsPerCubicMeter = 1.0d / item.getVolume();

            if ( numberOfUnitsPerCubicMeter > 1 )
            {
                volume += " ( " + (int) numberOfUnitsPerCubicMeter + " units / m³)";
            }
            text =
                    item.getName() + "\n\n" + item.getDescription() + "\n\nVolume: "
                            + volume + "\nItem ID: " + item.getId();
        }
        else
        {
            text = "No item selected.";
        }
        selectedItemDescription.setText( text );
        selectedItemDescription.setCaretPosition( 0 );
    }

    /**
     * Populates the context-sensitive popup menu by examining the current table
     * selection.
     * 
     * @param event
     * @param selectedRows
     * @return <code>true</code> if the updated popup menu hast at least one
     *         menu item
     */
    protected boolean populatePopupMenu(MouseEvent event, int[] selectedRows)
    {

        if ( log.isDebugEnabled() )
        {
            log.debug( "maybeShowPopup(): selection = "
                    + ObjectUtils.toString( selectedRows ) );
        }

        popupMenu.removeAll();

        final int column = table.columnAtPoint( event.getPoint() );
        if ( column < 0 )
        {
            log.warn( "maybeShowPopup(): Invalid column at point " + event.getPoint() );
            return false;
        }

        log.debug( "maybeShowPopup(): column = " + column );

        // gather all selected rows
        final List<TableEntry> data = new ArrayList<TableEntry>();

        for (int row : selectedRows)
        {
            final TableEntry row2 = tableModel.getRow( row );
            System.out.println( ">>> selected row: " + row2.getItemName() );
            data.add( row2 );
        }

        populatePopupMenuFor( column, data );

        return popupMenu.getComponentCount() > 0;
    }

    protected void populatePopupMenuFor(int clickedColumn, List<TableEntry> entries)
    {

        if ( log.isDebugEnabled() )
        {
            log.debug( "populatePopupMenuFor(): clicked_col = " + clickedColumn
                    + " , selected = " + entries );
        }

        boolean gotOutdatedBuyPrice = false;
        boolean gotOutdatedSellPrice = false;
        boolean gotUnknownBuyPrices = false;
        boolean gotUnknownSellPrices = false;
        boolean gotUserProvidedPrices = false;

        final Map<Long, InventoryType> updateBuyPrices =
                new HashMap<Long, InventoryType>();
        final Map<Long, InventoryType> updateSellPrices =
                new HashMap<Long, InventoryType>();

        for (TableEntry e : entries)
        {

            boolean stale = isStale( e.getBuyPrice() ); // method is NULL-safe

            if ( ! e.hasBuyPrice() || stale )
            {

                if ( stale )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "populatePopupMenuFor(): stale buy price > " + e );
                    }
                    gotOutdatedBuyPrice = true;
                }

                if ( ! e.hasBuyPrice() )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "populatePopupMenuFor(): Lacking buy price for "
                                + e.getItemName() );
                    }
                    gotUnknownBuyPrices = true;
                }

                updateBuyPrices.put( e.getItem().getId(), e.getItem() );
            }
            else if ( e.getBuyPrice().isUserProvided() )
            {
                updateBuyPrices.put( e.getItem().getId(), e.getItem() );
                gotUserProvidedPrices = true;
            }

            stale = isStale( e.getSellPrice() ); // method is NULL-safe

            if ( ! e.hasSellPrice() || stale )
            {

                if ( stale )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "populatePopupMenuFor(): stale sell price > " + e );
                    }
                    gotOutdatedSellPrice = true;
                }

                if ( ! e.hasSellPrice() )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "populatePopupMenuFor(): Lacking sell price for "
                                + e.getItemName() );
                    }
                    gotUnknownSellPrices = true;
                }

                updateSellPrices.put( e.getItem().getId(), e.getItem() );
            }
            else if ( e.getSellPrice().isUserProvided() )
            {
                updateSellPrices.put( e.getItem().getId(), e.getItem() );
                gotUserProvidedPrices = true;
            }

        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "populatePopupMenuFor(): gotOutdatedBuyPrice="
                    + gotOutdatedBuyPrice + " , gotOutdatedSellPrice = "
                    + gotOutdatedSellPrice + " , gotUnknownBuyPrices = "
                    + gotUnknownBuyPrices + " , gotUnknownSellPrices = "
                    + gotUnknownSellPrices );
        }

        /*
         * Create and join update request entries.
         */

        final Map<Long, PriceInfoUpdateRequest> requests =
                new HashMap<Long, PriceInfoUpdateRequest>();

        for (InventoryType item : updateBuyPrices.values())
        {

            PriceInfoUpdateRequest existing = requests.get( item.getId() );

            if ( existing == null )
            {
                existing = new PriceInfoUpdateRequest();
                existing.item = item;
                existing.type = PriceInfo.Type.BUY;
                requests.put( item.getId(), existing );
            }
        }

        for (InventoryType item : updateSellPrices.values())
        {

            PriceInfoUpdateRequest existing = requests.get( item.getId() );

            if ( existing == null )
            {
                existing = new PriceInfoUpdateRequest();
                existing.item = item;
                existing.type = PriceInfo.Type.SELL;
                requests.put( item.getId(), existing );
            }
            else
            {
                if ( existing.type == PriceInfo.Type.BUY )
                {
                    existing.type = PriceInfo.Type.ANY;
                }
            }
        }

        log.debug( "populatePopupMenuFor(): " + " buy prices requiring update: "
                + updateBuyPrices.size() + " , sell prices requiring update: "
                + updateSellPrices.size() );

        final IMenuAction updateOutdatedBuyPrices = new IMenuAction() {

            @Override
            public void run() throws PriceInfoUnavailableException
            {
                updateMultiplePrices( requests, PriceInfo.Type.BUY,
                    UpdateMode.UPDATE_OUTDATED );
            }
        };

        final IMenuAction updateMissingBuyPrices = new IMenuAction() {

            @Override
            public void run() throws Exception
            {
                updateMultiplePrices( requests, PriceInfo.Type.BUY,
                    UpdateMode.UPDATE_MISSING );
            }
        };

        final IMenuAction updateMissingOrStaleBuyPrices = new IMenuAction() {

            @Override
            public void run() throws PriceInfoUnavailableException
            {
                updateMultiplePrices( requests, PriceInfo.Type.BUY,
                    UpdateMode.UPDATE_OUTDATED );
            }
        };

        final IMenuAction updateMissingOrStaleSellPrices = new IMenuAction() {

            @Override
            public void run() throws PriceInfoUnavailableException
            {
                updateMultiplePrices( requests, PriceInfo.Type.SELL,
                    UpdateMode.UPDATE_MISSING_OR_OUTDATED );
            }
        };

        final IMenuAction updateOutdatedSellPrices = new IMenuAction() {

            @Override
            public void run() throws PriceInfoUnavailableException
            {
                updateMultiplePrices( requests, PriceInfo.Type.SELL,
                    UpdateMode.UPDATE_OUTDATED );
            }
        };

        final IMenuAction updateMissingSellPrices = new IMenuAction() {

            @Override
            public void run() throws PriceInfoUnavailableException
            {
                updateMultiplePrices( requests, PriceInfo.Type.SELL,
                    UpdateMode.UPDATE_MISSING );
            }
        };

        final IMenuAction updateUserPricesAction = new IMenuAction() {

            @Override
            public void run() throws PriceInfoUnavailableException
            {
                updateMultiplePrices( requests, PriceInfo.Type.ANY,
                    UpdateMode.UPDATE_USERPROVIDED );
            }
        };

        final InventoryType singleItem =
                entries.size() == 1 ? entries.get( 0 ).getItem() : null;

        if ( ! updateBuyPrices.isEmpty() )
        {

            String label;
            if ( singleItem != null )
            {
                label = "Fetch missing buy price for " + singleItem.getName();
            }
            else
            {
                label = "Fetch missing buy prices";
            }

            popupMenu.add( createMenuItem( label, updateMissingBuyPrices,
                gotUnknownBuyPrices ) );

            if ( singleItem != null )
            {
                label = "Update outdated buy price for " + singleItem.getName();
            }
            else
            {
                label = "Update outdated buy prices";
            }
            popupMenu.add( createMenuItem( label, updateOutdatedBuyPrices,
                gotOutdatedBuyPrice ) );

            if ( singleItem == null )
            {
                popupMenu.add( createMenuItem( "Update all missing/outdated buy prices",
                    updateMissingOrStaleBuyPrices, gotUnknownBuyPrices
                            | gotOutdatedBuyPrice ) );
            }
        }

        if ( ! updateSellPrices.isEmpty() )
        {

            String label;
            if ( singleItem != null )
            {
                label = "Fetch missing sell price for " + singleItem.getName();
            }
            else
            {
                label = "Fetch missing sell prices";
            }

            popupMenu.add( createMenuItem( label, updateMissingSellPrices,
                gotUnknownSellPrices ) );

            if ( singleItem != null )
            {
                label = "Update outdated sell price for " + singleItem.getName();
            }
            else
            {
                label = "Update outdated sell prices";
            }
            popupMenu.add( createMenuItem( label, updateOutdatedSellPrices,
                gotOutdatedSellPrice ) );

            if ( singleItem == null )
            {
                popupMenu.add( createMenuItem( "Update all missing/outdated sell prices",
                    updateMissingOrStaleSellPrices, gotUnknownSellPrices
                            | gotOutdatedSellPrice ) );
            }
        }

        if ( gotUserProvidedPrices )
        {

            if ( popupMenu.getComponentCount() > 0 )
            {
                popupMenu.addSeparator();
            }

            final String label = "Update user-provided prices from EVE-central";
            popupMenu.add( createMenuItem( label, updateUserPricesAction, true ) );

        }

        if ( singleItem != null )
        {
            if ( popupMenu.getComponentCount() > 0 )
            {
                popupMenu.addSeparator();
            }

            final IMenuAction action = new IMenuAction() {

                @Override
                public void run()
                {

                    final PriceHistoryComponent comp =
                            new PriceHistoryComponent( singleItem, getDefaultRegion(),
                                    PriceInfo.Type.ANY );

                    comp.setModal( true );

                    final Window window =
                            ComponentWrapper.wrapComponent( "Price history of "
                                    + singleItem.getName(), comp );

                    window.setVisible( true );
                }
            };

            final String label = "Plot price history";
            popupMenu.add( createMenuItem( label, action, true ) );
        }
    }

    protected static <T> void nullSafeAdd(List<T> list, Collection<T> data)
    {
        if ( data == null )
        {
            return;
        }
        list.addAll( data );
    }

    protected void updateMultiplePrices(Map<Long, PriceInfoUpdateRequest> requests,
            PriceInfo.Type type, UpdateMode mode) throws PriceInfoUnavailableException
    {

        if ( log.isDebugEnabled() )
        {
            log.debug( "updateMultiplePrices(): " + requests.size()
                    + " items , query type: " + type + " mode = " + mode );
        }

        final Map<Type, Set<InventoryType>> pricesByQueryType =
                new HashMap<Type, Set<InventoryType>>();

        for (PriceInfoUpdateRequest request : requests.values())
        {

            Set<InventoryType> existing = pricesByQueryType.get( request.item );

            if ( existing == null )
            {
                existing = new HashSet<InventoryType>();
                pricesByQueryType.put( type, existing );
            }
            existing.add( request.item );
        }

        if ( log.isDebugEnabled() )
        {
            for (Map.Entry<Type, Set<InventoryType>> entry : pricesByQueryType.entrySet())
            {
                log.debug( "updateMultiplePrices(): query type " + entry.getKey() + " : "
                        + entry.getValue().size() + " items." );
            }
        }

        final ArrayList<InventoryType> queryBoth = new ArrayList<InventoryType>();
        final ArrayList<InventoryType> queryBuy = new ArrayList<InventoryType>();
        final ArrayList<InventoryType> querySell = new ArrayList<InventoryType>();

        nullSafeAdd( queryBoth, pricesByQueryType.get( PriceInfo.Type.ANY ) );
        nullSafeAdd( queryBuy, pricesByQueryType.get( PriceInfo.Type.BUY ) );
        nullSafeAdd( querySell, pricesByQueryType.get( PriceInfo.Type.SELL ) );

        for (Iterator<InventoryType> it = queryBuy.iterator(); it.hasNext();)
        {
            final InventoryType item = it.next();

            if ( querySell.contains( item ) )
            {
                it.remove();
                querySell.remove( item );
                queryBoth.add( item );
            }
        }

        for (Iterator<InventoryType> it = querySell.iterator(); it.hasNext();)
        {
            final InventoryType item = it.next();

            if ( queryBuy.contains( item ) )
            {
                it.remove();
                queryBuy.remove( item );
                queryBoth.add( item );
            }
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "updateMultiplePrices(): [ after merge ] Type.ALL  = "
                    + queryBoth.size() );
            log.debug( "updateMultiplePrices(): [ after merge ] Type.BUY  = "
                    + queryBuy.size() );
            log.debug( "updateMultiplePrices(): [ after merge ] Type.SELL = "
                    + querySell.size() );
        }

        final IPriceQueryCallback callback = IPriceQueryCallback.NOP_INSTANCE;

        if ( ! queryBoth.isEmpty() )
        {

            MarketFilter filter =
                    new MarketFilterBuilder( PriceInfo.Type.ANY, getDefaultRegion() )
                            .updateMode( mode ).end();

            final IUpdateStrategy updateStrategy =
                    marketDataProvider.createUpdateStrategy( mode, PriceInfo.Type.ANY );

            if ( log.isDebugEnabled() )
            {
                log.debug( "updateMultiplePrices(): Updating Type.ALL item prices." );
            }
            marketDataProvider.updatePriceInfo( filter, queryBoth, callback,
                updateStrategy );
        }

        if ( ! querySell.isEmpty() )
        {

            final MarketFilter filter =
                    new MarketFilterBuilder( PriceInfo.Type.SELL, getDefaultRegion() )
                            .updateMode( mode ).end();

            if ( log.isDebugEnabled() )
            {
                log.debug( "updateMultiplePrices(): Updating Type.SELL item prices." );
            }

            final IUpdateStrategy updateStrategy =
                    marketDataProvider.createUpdateStrategy( mode, PriceInfo.Type.SELL );

            marketDataProvider.updatePriceInfo( filter, querySell, callback,
                updateStrategy );
        }

        if ( ! queryBuy.isEmpty() )
        {

            final MarketFilter filter =
                    new MarketFilterBuilder( PriceInfo.Type.BUY, getDefaultRegion() )
                            .updateMode( mode ).end();

            if ( log.isDebugEnabled() )
            {
                log.debug( "updateMultiplePrices(): Updating Type.BUY item prices." );
            }

            final IUpdateStrategy updateStrategy =
                    marketDataProvider.createUpdateStrategy( mode, PriceInfo.Type.BUY );

            marketDataProvider.updatePriceInfo( filter, queryBuy, callback,
                updateStrategy );
        }

    }

    private final class PriceInfoUpdateRequest
    {
        public InventoryType item;
        public PriceInfo.Type type;
    }

    private interface IMenuAction
    {
        public void run() throws Exception;
    }

    protected JMenuItem createMenuItem(String text, final IMenuAction r)
    {
        return createMenuItem( text, r, true );
    }

    protected JMenuItem createMenuItem(final String text, final IMenuAction r,
            final boolean enabled)
    {

        final JMenuItem item = new JMenuItem( new AbstractAction() {

            @Override
            public boolean isEnabled()
            {
                return enabled;
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {

                submitTask( new UITask() {

                    @Override
                    public String getId()
                    {
                        return text;
                    }

                    @Override
                    public void run() throws Exception
                    {
                        try
                        {
                            r.run();
                        }
                        catch (PriceInfoUnavailableException e)
                        {
                            displayError( "Found no price for "
                                    + e.getItemType().getName() );
                        }
                        catch (Exception e1)
                        {
                            displayError( "Error: " + e1.getMessage(), e1 );
                        }
                    }
                } );

            }
        } );

        item.setEnabled( enabled );
        item.setText( text );
        return item;
    }

    protected Region getDefaultRegion()
    {

        final IRegionQueryCallback callback =
                RegionSelectionDialog.createCallback( null, regionDAO );

        return appConfigProvider.getAppConfig().getDefaultRegion( callback );
    }

    protected IStaticDataModel getDataModel()
    {
        return this.dataModelProvider.getStaticDataModel();
    }

    protected static PriceInfo findPriceInfo(Type type, List<PriceInfo> infos)
    {
        for (PriceInfo info : infos)
        {
            if ( info.hasType( type ) )
            {
                return info;
            }
        }
        return null;
    }

    private void refreshTable()
    {
        final Map<Long, List<PriceInfo>> priceInfos = fetchPrices();

        List<TableEntry> priceInfo = new ArrayList<>();
		for (Map.Entry<Long, List<PriceInfo>> entry : priceInfos.entrySet())
        {
            final List<PriceInfo> data = entry.getValue();

            if ( data.size() > 2 )
            {
                throw new RuntimeException( "Internal error, got " + data.size()
                        + " price infos for item ID " + + entry.getKey() );
            }

            final InventoryType item = staticDataModel.getInventoryType( entry.getKey() );

            final PriceInfo buyPrice = findPriceInfo( Type.BUY, data );
            final PriceInfo sellPrice = findPriceInfo( Type.SELL, data );

            if ( log.isTraceEnabled() )
            {
                log.trace( "refreshTable(): [ " + item.getName() + " ] , buy price = "
                        + buyPrice );
                log.trace( "refreshTable(): [ " + item.getName() + " ] , sell price = "
                        + sellPrice );
            }
            priceInfo.add( new TableEntry( item, buyPrice, sellPrice ) );
        }

        Collections.sort( priceInfo, BY_ITEMNAME_COMPARATOR );
        viewModel.setData( priceInfo );
    }

	private Map<Long, List<PriceInfo>> fetchPrices() 
	{
        final Map<Long, List<PriceInfo>> priceInfos = new HashMap<>();
        
		final Map<Long, InventoryType> knownPrices = this.marketDataProvider.getAllKnownInventoryTypes( getDefaultRegion(), itemDAO );        
		final MarketFilter filter = new MarketFilterBuilder( Type.ANY , getDefaultRegion() ).end();
		try {
			final IPriceQueryCallback callback = new IPriceQueryCallback() {
				
				@Override
				public List<PriceInfo> getPriceInfo(MarketFilter filter, String message,InventoryType item) throws PriceInfoUnavailableException 
				{
					return new ArrayList<>();
				}
			};
			
			final Map<InventoryType, PriceInfoQueryResult> prices = marketDataProvider.getPriceInfos( filter , callback , knownPrices.values().toArray(new InventoryType[ knownPrices.size() ]) );
			for ( Entry<InventoryType, PriceInfoQueryResult> entry : prices.entrySet() ) 
			{
				final List<PriceInfo> list = new ArrayList<>();
				PriceInfoQueryResult queryResult = entry.getValue();
				if ( queryResult.hasBuyPrice() ) {
					list.add( queryResult.buyPrice() );
				}
				if ( queryResult.hasSellPrice() ) {
					list.add( queryResult.sellPrice() );
				}				
				priceInfos.put( entry.getKey().getId() , list );
			}
		} 
		catch (PriceInfoUnavailableException e) 
		{
			for ( InventoryType t : knownPrices.values() ) 
			{
				priceInfos.put( t.getId() , new ArrayList<PriceInfo>() );
			}
		}
		return priceInfos;
	}

    protected final class StalePriceInfoHighlighter extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int viewRow, int column)
        {

            super.getTableCellRendererComponent( table, value, isSelected, hasFocus,
                viewRow, column );

            final int modelRow = table.convertRowIndexToModel( viewRow );
            final TableEntry rowData = tableModel.getRow( modelRow );

            boolean isStale = false;
            if ( column == PriceInfoTableModel.BUY_PRICE_TIMESTAMP_IDX
                    && rowData.hasBuyPrice() && isStale( rowData.getBuyPrice() ) )
            {
                isStale = true;
            }

            if ( column == PriceInfoTableModel.SELL_PRICE_TIMESTAMP_IDX
                    && rowData.hasSellPrice() && isStale( rowData.getSellPrice() ) )
            {
                isStale = true;
            }

            if ( ! isSelected )
            {

                if ( ( viewRow % 2 ) == 0 )
                {
                    setBackground( Color.LIGHT_GRAY );
                }

                if ( isStale )
                {
                    setBackground( Color.YELLOW );
                }
                else
                {
                    setBackground( table.getBackground() );
                }
            }

            return this;
        }
    }

    protected boolean isStale(PriceInfo info)
    {
        if ( info == null )
        {
            return false;
        }
        return PriceInfoQueryResult.isOutdated( info, systemClock );
    }

    @Override
    protected void disposeHook()
    {
        marketDataProvider.removeChangeListener( viewModel );
        tableModel.dispose();

        if ( oldTooltipDismissalDelay != - 1 )
        {
            ToolTipManager.sharedInstance().setDismissDelay( oldTooltipDismissalDelay );
        }
    }

    @Override
    protected void onAttachHook(IComponentCallback callback)
    {
        marketDataProvider.addChangeListener( viewModel );

        oldTooltipDismissalDelay =
                de.codesourcery.eve.skills.ui.utils.Misc
                        .setTooltipDismissalDelay( 8 * 1000 );

        runOnEventThread( new Runnable() {

            @Override
            public void run()
            {
                refreshTable();
            }
        } );
    }

    @Override
    protected void onDetachHook()
    {
        marketDataProvider.removeChangeListener( viewModel );

        if ( oldTooltipDismissalDelay != - 1 )
        {
            ToolTipManager.sharedInstance().setDismissDelay( oldTooltipDismissalDelay );
        }
    }

}
