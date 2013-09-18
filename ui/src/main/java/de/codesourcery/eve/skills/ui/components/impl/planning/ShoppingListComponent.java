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
package de.codesourcery.eve.skills.ui.components.impl.planning;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.ShoppingList;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.datamodel.ShoppingList.ShoppingListEntry;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IPriceInfoStore;
import de.codesourcery.eve.skills.market.IPriceQueryCallback;
import de.codesourcery.eve.skills.market.MarketFilterBuilder;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.market.IMarketDataProvider.IPriceInfoChangeListener;
import de.codesourcery.eve.skills.production.ShoppingListManager;
import de.codesourcery.eve.skills.production.ShoppingListManager.IShoppingListManagerListener;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.impl.TotalItemValueComponent;
import de.codesourcery.eve.skills.ui.components.impl.TotalItemVolumeComponent;
import de.codesourcery.eve.skills.ui.components.impl.TotalItemValueComponent.IDataProvider;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.DefaultTreeModel;
import de.codesourcery.eve.skills.ui.model.DefaultTreeNode;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.renderer.FixedBooleanTableCellRenderer;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.Misc;
import de.codesourcery.eve.skills.ui.utils.PlainTextTransferable;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.Cell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.FixedCell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.HorizontalGroup;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.VerticalGroup;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISKAmount;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class ShoppingListComponent extends AbstractComponent
{
    private final MyTreeModel treeModel = new MyTreeModel();
    private final JTree tree = new JTree( treeModel );

    private final MyTableModel tableModel = new MyTableModel();

    private final JTable table = new JTable( tableModel );

    @Resource(name = "shoppinglist-manager")
    private ShoppingListManager manager;

    @Resource(name = "marketdata-provider")
    private IMarketDataProvider marketDataProvider;

    @Resource(name = "appconfig-provider")
    private IAppConfigProvider configProvider;

    @Resource(name = "region-dao")
    private de.codesourcery.eve.skills.db.dao.IRegionDAO regionDAO;

    private INodeEditorComponent currentNodeEditor = null;
    private final JPanel componentPanel = new JPanel();

    private final TotalItemValueComponent<TableRow> totalValue;
    private final TotalItemVolumeComponent<TableRow> totalVolume;

    @Resource(name = "system-clock")
    private ISystemClock systemClock;

    private TreeCellRenderer cellRenderer = new DefaultTreeCellRenderer() {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row,
                hasFocus );

            if ( ! ( value instanceof ITreeNode ) )
            {
                return this;
            }

            final Object nodeValue = ( (ITreeNode) value ).getValue();

            if ( nodeValue instanceof ShoppingList )
            {
                setText( ( (ShoppingList) nodeValue ).getTitle() );
            }
            else if ( nodeValue instanceof ShoppingListEntry )
            {
                final ShoppingListEntry entry = (ShoppingListEntry) nodeValue;

                setText( entry.getType().getName() + " x " + entry.getQuantity() );
            }
            return this;
        }
    };

    private enum TableViewMode
    {
        CURRENTLY_SELECTED_SHOPPINGLIST, ALL_SHOPPINGLISTS
    }

    private final TableViewModePanel tableViewModePanel = new TableViewModePanel();

    private final class TableViewModePanel extends JPanel
    {

        private volatile TableViewMode currentViewMode = TableViewMode.ALL_SHOPPINGLISTS;

        private final JRadioButton button1 = new JRadioButton( "Combined data" );

        private final JRadioButton button2 =
                new JRadioButton( "Currently selected list only" );

        public TableViewModePanel() {
            super();
            setLayout( new GridBagLayout() );
            ButtonGroup group = new ButtonGroup();
            button1.setSelected( true );
            button2.setSelected( false );

            group.add( button1 );
            group.add( button2 );

            add( button1, constraints( 0, 0 ).noResizing().useRelativeWidth().end() );
            add( button2, constraints( 1, 0 ).noResizing().useRemainingWidth().end() );

            final ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if ( e.getSource() == button1 )
                    {
                        currentViewMode = TableViewMode.ALL_SHOPPINGLISTS;
                        tableModel.refresh();
                    }
                    else if ( e.getSource() == button2 )
                    {
                        currentViewMode = TableViewMode.CURRENTLY_SELECTED_SHOPPINGLIST;
                        tableModel.refresh();
                    }
                }
            };

            button1.addActionListener( listener );
            button2.addActionListener( listener );
        }

        public TableViewMode getTableViewMode()
        {
            return currentViewMode;
        }
    }

    private interface INodeEditorComponent
    {

        public JPanel getPanel();

        public void setSelectedNode(ITreeNode node);

        public ITreeNode getSelectedNode();

        public boolean accepts(ITreeNode node);

        public void dispose();
    }

    public ShoppingListComponent() {
        super();

        final TotalItemValueComponent.IDataProvider<TableRow> priceProvider =
                new IDataProvider<TableRow>() {

                    @Override
                    public ISKAmount getPricePerUnit(TableRow obj)
                    {
                        final PriceInfo buyPrice = getBuyPrice( obj.type );
                        if ( buyPrice == null )
                        {
                            return ISKAmount.ZERO_ISK;
                        }
                        return new ISKAmount( buyPrice.getAveragePrice() );
                    }

                    @Override
                    public int getQuantity(TableRow obj)
                    {
                        return obj.totalQuantity;
                    }
                };
        this.totalValue = new TotalItemValueComponent<TableRow>( priceProvider );

        final TotalItemVolumeComponent.IDataProvider<TableRow> volumeProvider =
                new TotalItemVolumeComponent.IDataProvider<TableRow>() {

                    @Override
                    public int getQuantity(TableRow obj)
                    {
                        return obj.totalQuantity;
                    }

                    @Override
                    public double getVolumePerUnit(TableRow obj)
                    {
                        return obj.type.getVolume();
                    }
                };
        this.totalVolume = new TotalItemVolumeComponent<TableRow>( volumeProvider );

        super.registerChildren( totalValue, totalVolume );
    }

    protected Region getDefaultRegion()
    {
        final IRegionQueryCallback callback =
                RegionSelectionDialog.createCallback( null, regionDAO );

        return configProvider.getAppConfig().getDefaultRegion( callback );
    }

    protected PriceInfo getBuyPrice(InventoryType item)
    {

        try
        {
            final PriceInfoQueryResult result =
                    marketDataProvider.getPriceInfo( new MarketFilterBuilder( Type.BUY,
                            getDefaultRegion() ).end(), IPriceQueryCallback.NOP_INSTANCE,
                        item );

            if ( result.hasBuyPrice() )
            {
                return result.buyPrice();
            }
        }
        catch (PriceInfoUnavailableException e)
        {
            // ok
        }
        return null;
    }

    private abstract class AbstractNodeEditor<X> implements INodeEditorComponent
    {

        protected ITreeNode currentNode;;

        @Override
        public final ITreeNode getSelectedNode()
        {
            return currentNode;
        }

        @Override
        public void dispose()
        {
        }

        @SuppressWarnings("unchecked")
        protected final X getSelectedValue()
        {
            if ( currentNode == null )
            {
                return null;
            }
            return (X) currentNode.getValue();
        }

        @Override
        public void setSelectedNode(ITreeNode node)
        {
            this.currentNode = node;
        }

    }

    private final class ShoppingListEntryNodeEditor extends
            AbstractNodeEditor<ShoppingListEntry> implements IShoppingListManagerListener
    {

        private final JPanel panel = new JPanel();

        private final JTextField itemType = new JTextField();
        private final JTextField quantity = new JTextField();
        private final JTextField purchasedQuantity = new JTextField();
        private final JTextField buyPrice = new JTextField();

        protected int getInt(JTextField field, int defaultValue)
        {
            final String value = field.getText();
            if ( StringUtils.isBlank( value ) )
            {
                return defaultValue;
            }
            try
            {
                return Integer.parseInt( value );
            }
            catch (Exception e)
            {
            }
            return defaultValue;
        }

        @Override
        public void dispose()
        {
            manager.removeChangeListener( this );
        }

        public ShoppingListEntryNodeEditor() {

            manager.addChangeListener( this );

            itemType.setEditable( false );
            itemType.setHorizontalAlignment( JTextField.TRAILING );
            itemType.setColumns( 30 );

            quantity.setColumns( 15 );
            quantity.setHorizontalAlignment( JTextField.TRAILING );
            quantity.addActionListener( new ActionListener() {

                private void fixPurchasedQuantity()
                {

                    final int purchased = getInt( purchasedQuantity, - 1 );
                    if ( purchased != - 1 )
                    {
                        if ( getSelectedValue().getQuantity() < purchased )
                        {
                            purchasedQuantity.setText( ""
                                    + getSelectedValue().getQuantity() );
                            getSelectedValue().setPurchasedQuantity(
                                getSelectedValue().getQuantity() );
                        }
                    }
                }

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    int newValue = getInt( quantity, - 1 );
                    final ShoppingListEntry entry = getSelectedValue();
                    if ( newValue == - 1 || newValue < 1 )
                    {
                        quantity.setText( "" + entry.getQuantity() );
                        return;
                    }
                    entry.setQuantity( newValue );
                    fixPurchasedQuantity();

                    manager.shoppingListEntryChanged( entry.getShoppingList(), entry );
                }
            } );

            purchasedQuantity.setColumns( 15 );
            purchasedQuantity.setHorizontalAlignment( JTextField.TRAILING );
            purchasedQuantity.addActionListener( new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    int newValue = getInt( purchasedQuantity, - 1 );
                    final ShoppingListEntry entry = getSelectedValue();
                    if ( newValue < 0 )
                    {
                        purchasedQuantity.setText( "" + entry.getPurchasedQuantity() );
                        return;
                    }
                    else if ( newValue > entry.getQuantity() )
                    {
                        newValue = entry.getQuantity();
                        purchasedQuantity.setText( "" + newValue );
                    }
                    entry.setPurchasedQuantity( newValue );
                    manager.shoppingListEntryChanged( entry.getShoppingList(), entry );
                }
            } );

            buyPrice.addActionListener( new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    final ShoppingListEntry entry = getSelectedValue();

                    if ( ! updateBuyPrice( entry.getType(), buyPrice.getText() ) )
                    {
                        buyPrice.setText( "0.00" );
                    }
                }
            } );
            panel.setLayout( new GridBagLayout() );

            panel.add( new JLabel( "Item", SwingConstants.RIGHT ), constraints( 0, 0 )
                    .useRelativeWidth().end() );
            panel.add( itemType, constraints( 1, 0 ).anchorWest().end() );

            panel.add( new JLabel( "Quantity", SwingConstants.RIGHT ), constraints( 0, 1 )
                    .useRelativeWidth().end() );
            panel.add( quantity, constraints( 1, 1 ).anchorWest().noResizing().end() );

            panel.add( new JLabel( "Purchased quantity", SwingConstants.RIGHT ),
                constraints( 0, 2 ).useRelativeWidth().end() );

            panel.add( purchasedQuantity, constraints( 1, 2 ).anchorWest().noResizing()
                    .end() );

            panel.add( new JLabel( "Avg. buy price", SwingConstants.RIGHT ), constraints(
                0, 3 ).useRelativeWidth().end() );

            panel.add( buyPrice, constraints( 1, 3 ).anchorWest().noResizing().end() );
        }

        @Override
        public void setSelectedNode(ITreeNode node)
        {
            if ( ! accepts( node ) )
            {
                throw new IllegalArgumentException( "Unsupported node " + node );
            }

            super.setSelectedNode( node );
            refresh();
        }

        private void refresh()
        {

            final InventoryType item = getSelectedValue().getType();

            this.itemType.setText( item.getName() );
            this.quantity.setText( "" + getSelectedValue().getQuantity() );
            this.purchasedQuantity.setText( ""
                    + getSelectedValue().getPurchasedQuantity() );

            final PriceInfo buyPrice = getBuyPrice( item );

            if ( buyPrice != null )
            {
                this.buyPrice.setText( AmountHelper.formatISKAmount( buyPrice
                        .getAveragePrice() ) );
            }
            else
            {
                this.buyPrice.setText( "< unavailable >" );
            }
        }

        @Override
        public boolean accepts(ITreeNode node)
        {
            return node.getValue() instanceof ShoppingListEntry;
        }

        @Override
        public JPanel getPanel()
        {
            return panel;
        }

        @Override
        public void listEntryAdded(ShoppingList list, ShoppingListEntry newEntry)
        {
        }

        @Override
        public void listEntryChanged(ShoppingList list, ShoppingListEntry changedEntry)
        {
            if ( getSelectedValue() == changedEntry )
            {
                refresh();
            }
        }

        @Override
        public void listEntryRemoved(ShoppingList list, ShoppingListEntry removedEntry)
        {
        }

        @Override
        public void shoppingListAdded(ShoppingList list)
        {
        }

        @Override
        public void shoppingListChanged(ShoppingList list)
        {
        }

        @Override
        public void shoppingListRemoved(ShoppingList list)
        {
        }

    }

    private final class ShoppingListNodeEditor extends AbstractNodeEditor<ShoppingList>
    {

        private final JPanel panel = new JPanel();

        private final JTextField title = new JTextField();
        private final JTextArea description = new JTextArea( 5 , 20 );
        private final JButton saveButton = new JButton( "Save changes" );

        private boolean isDirty()
        {
            return ! StringUtils.equals( getSelectedValue().getTitle(), title.getText() )
                    || ! StringUtils.equals( getSelectedValue().getDescription(),
                        description.getText() );
        }

        protected void updateButtonState()
        {
            saveButton.setEnabled( isDirty() );
        }

        public ShoppingListNodeEditor() {

            saveButton.addActionListener( new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {

                    final ShoppingList list = getSelectedValue();
                    boolean isDirty = isDirty();

                    if ( ! StringUtils.equals( list.getTitle(), title.getText() ) )
                    {
                        list.setTitle( title.getText() );
                    }

                    if ( ! StringUtils.equals( list.getDescription(), description
                            .getText() ) )
                    {
                        list.setDescription( description.getText() );
                    }

                    if ( isDirty )
                    {
                        manager.shoppingListChanged( list );
                    }
                }
            } );

            title.setColumns( 35 );
            title.addActionListener( new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    updateButtonState();
                }
            } );

            description.getDocument().addDocumentListener( new DocumentListener() {

                @Override
                public void changedUpdate(DocumentEvent e)
                {
                    updateButtonState();
                }

                @Override
                public void insertUpdate(DocumentEvent e)
                {
                    updateButtonState();
                }

                @Override
                public void removeUpdate(DocumentEvent e)
                {
                    updateButtonState();
                }
            } );
            description.setLineWrap( true );
            description.setWrapStyleWord( true );

            panel.setLayout( new GridBagLayout() );

            panel.add( new JLabel( "Title" ), constraints( 0, 0 ).useRelativeWidth()
                    .end() );
            panel.add( title, constraints( 1, 0 ).end() );

            panel.add( new JLabel( "Description" ), constraints( 0, 1 )
                    .useRelativeWidth().end() );
            panel.add( new JScrollPane( description ) , constraints( 1, 1 ).end() );

            panel.add( saveButton, constraints( 1, 2 ).end() );
        }

        @Override
        public void setSelectedNode(ITreeNode node)
        {
            if ( ! accepts( node ) )
            {
                throw new IllegalArgumentException( "Unsupported node " + node );
            }

            super.setSelectedNode( node );

            title.setText( getSelectedValue().getTitle() );
            description.setText( getSelectedValue().getDescription() );

            updateButtonState();
        }

        @Override
        public boolean accepts(ITreeNode node)
        {
            return node.getValue() instanceof ShoppingList;
        }

        @Override
        public JPanel getPanel()
        {
            return panel;
        }

    }

    private void selectedNodeChanged(ITreeNode node)
    {

        if ( node != null && currentNodeEditor != null
                && currentNodeEditor.accepts( node ) )
        {
            currentNodeEditor.setSelectedNode( node );
            this.tableModel.selectedNodeChanged( node );
            return;
        }

        if ( this.currentNodeEditor != null )
        {
            this.currentNodeEditor.dispose();
        }

        this.currentNodeEditor = null;
        this.componentPanel.removeAll();

        if ( node != null && node.getValue() instanceof ShoppingList )
        {
            currentNodeEditor = new ShoppingListNodeEditor();
            currentNodeEditor.setSelectedNode( node );
            this.componentPanel.add( currentNodeEditor.getPanel() );
            componentPanel.revalidate();
        }
        else if ( node != null && node.getValue() instanceof ShoppingListEntry )
        {
            currentNodeEditor = new ShoppingListEntryNodeEditor();
            currentNodeEditor.setSelectedNode( node );
            this.componentPanel.add( currentNodeEditor.getPanel() );
            componentPanel.revalidate();
        }

        getPanel().revalidate();

        this.tableModel.selectedNodeChanged( node );
    }

    @Override
    protected void disposeHook()
    {
        manager.removeChangeListener( treeModel );
        manager.removeChangeListener( tableModel );
    }

    @Override
    protected void onDetachHook()
    {

        ToolTipManager.sharedInstance().unregisterComponent( tree );

        manager.removeChangeListener( treeModel );
        manager.removeChangeListener( tableModel );

        marketDataProvider.removeChangeListener( tableModel );
    }

    @Override
    protected void onAttachHook(IComponentCallback callback)
    {
        ToolTipManager.sharedInstance().registerComponent( tree );

        manager.addChangeListener( treeModel );
        manager.addChangeListener( tableModel );

        marketDataProvider.addChangeListener( tableModel );

        tableModel.refresh();
        treeModel.refresh();
    }

    private void putSelectedShoppingListOnClipboard()
    {

        final StringBuffer result = new StringBuffer();

        final ITreeNode selected = getSelectedTreeNode();
        if ( selected == null )
        {
            return;
        }

        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        ShoppingList list = null;
        if ( selected.getValue() instanceof ShoppingList )
        {
            list = (ShoppingList) selected.getValue();
        }
        else if ( selected.getValue() instanceof ShoppingListEntry )
        {
            final ShoppingListEntry entry = (ShoppingListEntry) selected.getValue();
            list = entry.getShoppingList();
        }

        if ( list == null )
        {
            return;
        }

        result.append( list.getTitle() ).append( Misc.newLine() );
        if ( ! StringUtils.isBlank( list.getDescription() ) )
        {
            result.append( Misc.newLine() ).append( list.getDescription() ).append(
                Misc.newLine().twice() );
        }

        final DecimalFormat quantityFormat = new DecimalFormat( "###,###,###,###,##0" );

        for (Iterator<ShoppingListEntry> it = list.iterator(); it.hasNext();)
        {
            final ShoppingListEntry entry = it.next();

            result.append( StringUtils.rightPad( entry.getType().getName(), 30 ) )
                    .append(
                        StringUtils.leftPad(
                            quantityFormat.format( entry.getQuantity() ), 20 ) );

            if ( it.hasNext() )
            {
                result.append( Misc.newLine() );
            }
        }

        clipboard.setContents( new PlainTextTransferable( result.toString() ), null );
    }

    @Override
    protected JPanel createPanel()
    {

        table.setFillsViewportHeight( true );
        table.setRowSorter( tableModel.getRowSorter() );
        table.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e)
            {

                final int[] selectedViewRows = table.getSelectedRows();
                final int[] selectedModelRows = new int[selectedViewRows.length];

                int i = 0;
                for (int viewRow : selectedViewRows)
                {
                    selectedModelRows[i++] = viewRow;
                }

                List<TableRow> selection = new ArrayList<TableRow>();
                for (int modelRow : selectedModelRows)
                {
                    selection.add( tableModel.getRow( modelRow ) );
                }
                totalValue.setItems( selection );
                totalVolume.setItems( selection );
            }
        } );

        FixedBooleanTableCellRenderer.attach( table );

        final JPanel panel = new JPanel();
        panel.setLayout( new GridBagLayout() );

        final JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout( new GridBagLayout() );
        controlsPanel.add( this.tableViewModePanel, constraints( 0, 0 ).useRemainingWidth().end() );

        final JPanel totalsPanel = new JPanel();
        totalsPanel.setLayout( new GridBagLayout() );
        
        totalsPanel.add( totalValue.getPanel() , constraints( 0 , 0 ).weightX(0.2).width(1).weightY(0).anchorWest().end() );
        totalsPanel.add( totalVolume.getPanel() , constraints( 1 , 0 ).weightX(0.2).width(1).weightY(0).anchorWest().end() );
		
        final JPanel rest = new JPanel();
        rest.setLayout( new GridBagLayout() );
        
        rest.add( componentPanel , constraints( 0 , 0 ).weightX(0.2).width(1).weightY(0.3).anchorWest().end() );
        rest.add( totalsPanel , constraints( 0 , 1 ).weightX(0.2).width(1).weightY(0).resizeHorizontally().anchorWest().end() );	
        rest.add( controlsPanel , constraints( 0 , 2 ).weightX(0.2).width(1).weightY(0).resizeHorizontally().anchorWest().end() );        
        rest.add(  new JScrollPane( table ) , constraints( 0 , 3 ).weightX(0.2).width(1)
        		.weightY(0.7).resizeBoth().anchorWest().end() );	        
        
//        new GridLayoutBuilder().add(
//            new VerticalGroup( new Cell( componentPanel ),
//                    new FixedCell( controlsPanel ),
//                    new FixedCell( totalsPanel ),
//                    new Cell( new JScrollPane( table ) ) ) ).addTo( rest );

        final ImprovedSplitPane splitPane =
                new ImprovedSplitPane( JSplitPane.HORIZONTAL_SPLIT,
                        new JScrollPane( tree ), rest );

        panel.add( splitPane, constraints( 0, 0 ).resizeBoth().end() );

        tree.setRootVisible( false );
        tree.setCellRenderer( cellRenderer );

        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION );

        tree.getSelectionModel().addTreeSelectionListener( new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e)
            {
                final TreePath selection = e.getPath();
                selectedNodeChanged( (ITreeNode) ( selection != null ? selection
                        .getLastPathComponent() : null ) );
            }
        } );

        final PopupMenuBuilder menuBuilder = new PopupMenuBuilder();

        menuBuilder.addItem( "New shopping list...", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                final ShoppingListEditorComponent comp =
                        new ShoppingListEditorComponent( "New list", "",
                                new ArrayList<ItemWithQuantity>() );

                comp.setModal( true );
                ComponentWrapper.wrapComponent( "Create new shopping list", comp )
                        .setVisible( true );
                if ( ! comp.wasCancelled() )
                {
                    manager.addShoppingList( comp.getShoppingList() );
                }
            }
        } );

        menuBuilder.addItem( "Edit list...", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e)
            {

                final ITreeNode node = getSelectedTreeNode();
                if ( node == null )
                {
                    return;
                }

                final ShoppingList list = (ShoppingList) node.getValue();
                final ShoppingListEditorComponent comp =
                        new ShoppingListEditorComponent( list );
                comp.setModal( true );
                ComponentWrapper.wrapComponent( list.getTitle(), comp ).setVisible( true );
                if ( ! comp.wasCancelled() && comp.isExistingEntryEdited() )
                {
                    manager.shoppingListChanged( comp.getShoppingList() );
                }
            }

            @Override
            public boolean isEnabled()
            {
                final ITreeNode node = getSelectedTreeNode();
                return node != null && node.getValue() instanceof ShoppingList;
            }
        } );

        menuBuilder.addItem( "Delete item", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e)
            {

                final ITreeNode node = getSelectedTreeNode();
                if ( node != null && node.getValue() instanceof ShoppingListEntry )
                {
                    final ShoppingListEntry entry = (ShoppingListEntry) node.getValue();
                    final ShoppingList list = entry.getShoppingList();

                    manager.removeEntry( list, entry );
                    selectedNodeChanged( null );
                }
            }

            @Override
            public boolean isEnabled()
            {
                final ITreeNode node = getSelectedTreeNode();
                return node != null && node.getValue() instanceof ShoppingListEntry;
            }
        } );

        menuBuilder.addItem( "Delete list", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                final ShoppingList list = getSelectedShoppingList();
                if ( list != null )
                {
                    manager.removeShoppingList( list );
                    selectedNodeChanged( null );
                }
            }

            @Override
            public boolean isEnabled()
            {
                final ITreeNode node = getSelectedTreeNode();
                return node.getValue() instanceof ShoppingList;
            }
        } );

        menuBuilder.addItem( "Copy this list to clipboard (text)", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                putSelectedShoppingListOnClipboard();
            }

            @Override
            public boolean isEnabled()
            {
                return getSelectedShoppingList() != null;
            }
        } );

        menuBuilder.attach( tree );
        return panel;
    }

    private ShoppingList getSelectedShoppingList()
    {
        final ITreeNode selected = getSelectedTreeNode();
        if ( selected == null )
        {
            return null;
        }

        ShoppingList list = null;
        if ( selected.getValue() instanceof ShoppingList )
        {
            list = (ShoppingList) selected.getValue();
        }
        else if ( selected.getValue() instanceof ShoppingListEntry )
        {
            final ShoppingListEntry entry = (ShoppingListEntry) selected.getValue();
            list = entry.getShoppingList();
        }
        return list;
    }

    private ITreeNode getSelectedTreeNode()
    {
        final TreePath path = tree.getSelectionPath();

        return path != null ? (ITreeNode) tree.getSelectionPath().getLastPathComponent()
                : null;
    }

    private static final class TableRow implements Comparable<TableRow>
    {

        private InventoryType type;
        private int totalQuantity;
        private int purchasedQuantity;

        public TableRow(ShoppingListEntry entry) {
            this.type = entry.getType();
            this.totalQuantity = entry.getQuantity();
            this.purchasedQuantity = entry.getPurchasedQuantity();
        }

        public void merge(ShoppingListEntry entry)
        {
            if ( ! entry.getType().equals( this.type ) )
            {
                throw new IllegalArgumentException( "Cannot merge different types" );
            }

            this.totalQuantity += entry.getQuantity();
            this.purchasedQuantity += entry.getPurchasedQuantity();
        }

        @Override
        public int compareTo(TableRow o)
        {
            return this.type.getName().compareTo( o.type.getName() );
        }
    }

    /**
     * 
     * @param type
     * @param price
     * @return <code>false</code> if the input was not a valid decimal string
     */
    protected boolean updateBuyPrice(InventoryType type, String price)
    {

        if ( StringUtils.isBlank( price ) )
        {
            return false;
        }

        final long newPrice;
        try {
        	newPrice = AmountHelper.parseISKAmount( price.trim() );
        } catch(Exception e) {
        	return false;
        }

        PriceInfo buyPrice = getBuyPrice( type );
        if ( buyPrice == null )
        {
            buyPrice = new PriceInfo( Type.BUY, type, Source.USER_PROVIDED );
            buyPrice.setRegion( getDefaultRegion() );
        }
        else
        {
            buyPrice.setSource( Source.USER_PROVIDED );
        }
        buyPrice.setAveragePrice( newPrice );
        buyPrice.setTimestamp( new EveDate( systemClock ) );
        marketDataProvider.store( buyPrice );
        return true;
    }

    private final class MyTableModel extends AbstractTableModel<TableRow> implements IShoppingListManagerListener, IPriceInfoChangeListener
    {
        public static final int ITEMTYPE_COL_IX = 0;
        public static final int TOTAL_QTY_COL_IX = 1;
        public static final int PURCHASED_QTY_COL_IX = 2;
        public static final int BUYPRICE_COL_IX = 3;
        public static final int PURCHASED_COL_IX = 4;

        private final List<TableRow> data = new ArrayList<TableRow>();

        private ShoppingList lastSelectedShoppingList;

        public MyTableModel() 
        {
            super( new TableColumnBuilder().add( "Item" ).add( "Total quantity",
                Integer.class ).add( "Purchased quantity", Integer.class ).add(
                "Avg. buy price" ).add( "Purchased ?", Boolean.class ) );
        }

        public void selectedNodeChanged(ITreeNode node)
        {
            if ( tableViewModePanel.getTableViewMode() == TableViewMode.CURRENTLY_SELECTED_SHOPPINGLIST )
            {
                ShoppingList current = getSelectedShoppingList();

                if ( current != lastSelectedShoppingList )
                {
                    refresh();
                }
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == PURCHASED_COL_IX || columnIndex == BUYPRICE_COL_IX;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex)
        {

            if ( columnIndex == BUYPRICE_COL_IX )
            {
                updateBuyPrice( getRow( rowIndex ).type, (String) value );
                return;
            }

            if ( columnIndex != PURCHASED_COL_IX )
            {
                return;
            }

            boolean isSelected = (Boolean) value;
            final TableRow row = getRow( rowIndex );

            final InventoryType itemType = row.type;
            switch ( tableViewModePanel.getTableViewMode() )
            {
                case ALL_SHOPPINGLISTS:
                    for (ShoppingList list : manager.getEntries())
                    {
                        if ( isSelected )
                        {
                            manager.setPurchased( list, itemType );
                        }
                        else
                        {
                            manager.unsetPurchased( list, itemType );
                        }
                    }
                    break;
                case CURRENTLY_SELECTED_SHOPPINGLIST:
                    if ( lastSelectedShoppingList != null )
                    {
                        if ( isSelected )
                        {
                            manager.setPurchased( lastSelectedShoppingList, itemType );
                        }
                        else
                        {
                            manager.unsetPurchased( lastSelectedShoppingList, itemType );
                        }
                    }
                    break;
                default:
            }
        }

        @Override
        protected Object getColumnValueAt(int modelRowIndex, int modelColumnIndex)
        {
            final TableRow row = getRow( modelRowIndex );

            switch ( modelColumnIndex )
            {
                case ITEMTYPE_COL_IX:
                    return row.type.getName();
                case TOTAL_QTY_COL_IX:
                    return row.totalQuantity;
                case PURCHASED_QTY_COL_IX:
                    return row.purchasedQuantity;
                case BUYPRICE_COL_IX:
                    final PriceInfo buyPrice = getBuyPrice( row.type );
                    if ( buyPrice != null )
                    {
                        return AmountHelper.formatISKAmount( buyPrice.getAveragePrice() );
                    }
                    return "< unavailable >";
                case PURCHASED_COL_IX:
                    return row.purchasedQuantity > 0
                            && row.purchasedQuantity == row.totalQuantity;
                default:
                    throw new IllegalArgumentException( "Invalid column "
                            + modelColumnIndex );
            }
        }

        @Override
        public TableRow getRow(int modelRow)
        {
            return data.get( modelRow );
        }

        @Override
        public int getRowCount()
        {
            return data.size();
        }

        public void refresh()
        {
            final Map<InventoryType, TableRow> newData =
                    new HashMap<InventoryType, TableRow>();

            List<ShoppingList> listsToMerge;
            if ( tableViewModePanel.getTableViewMode() == TableViewMode.CURRENTLY_SELECTED_SHOPPINGLIST )
            {
                final ShoppingList shoppingList = getSelectedShoppingList();
                if ( shoppingList != null )
                {
                    listsToMerge = Collections.singletonList( shoppingList );
                    lastSelectedShoppingList = shoppingList;
                }
                else
                {
                    listsToMerge = Collections.emptyList();
                    lastSelectedShoppingList = null;
                }
            }
            else
            {
                lastSelectedShoppingList = null;
                listsToMerge = manager.getEntries();
            }

            for (ShoppingList list : listsToMerge)
            {
                for (ShoppingListEntry entry : list.getEntries())
                {
                    TableRow row = newData.get( entry.getType() );
                    if ( row == null )
                    {
                        row = new TableRow( entry );
                        newData.put( entry.getType(), row );
                    }
                    else
                    {
                        row.merge( entry );
                    }
                }
            }

            final List<TableRow> sorted = new ArrayList<TableRow>( newData.values() );
            Collections.sort( sorted );
            this.data.clear();
            this.data.addAll( sorted );
            modelDataChanged();
        }

        @Override
        public void shoppingListAdded(ShoppingList list)
        {
            refresh();
        }

        @Override
        public void shoppingListChanged(ShoppingList list)
        {
            refresh();
        }

        @Override
        public void shoppingListRemoved(ShoppingList list)
        {
            refresh();
        }

        @Override
        public void listEntryAdded(ShoppingList list, ShoppingListEntry newEntry)
        {
            refresh();
        }

        @Override
        public void listEntryChanged(ShoppingList list, ShoppingListEntry changedEntry)
        {
            refresh();
        }

        @Override
        public void listEntryRemoved(ShoppingList list, ShoppingListEntry removedEntry)
        {
            refresh();
        }

        @Override
        public void priceChanged(IMarketDataProvider caller, Region region, Set<InventoryType> type)
        {
            refresh();
        }

    }

    private final class MyTreeModel extends DefaultTreeModel implements
            IShoppingListManagerListener
    {

        private ITreeNode root;

        private final Comparator<ITreeNode> comp = new Comparator<ITreeNode>() {

            @Override
            public int compare(ITreeNode o1, ITreeNode o2)
            {
                final Object val1 = o1.getValue();

                final Object val2 = o2.getValue();

                if ( val1 instanceof ShoppingList )
                {
                    return ( (ShoppingList) val1 ).getTitle().compareTo(
                        ( (ShoppingList) val2 ).getTitle() );
                }
                else if ( val1 instanceof ShoppingListEntry )
                {
                    return ( (ShoppingListEntry) val1 ).getType().getName().compareTo(
                        ( (ShoppingListEntry) val2 ).getType().getName() );
                }
                else
                {
                    throw new RuntimeException( "Cannot compare " + val1 + " <-> " + val2 );
                }
            }
        };

        public void refresh()
        {
            root = createTree();
            fireEvent( EventType.STRUCTURE_CHANGED, new TreeModelEvent( this, root
                    .getPathToRoot() ) );
        }

        @Override
        public ITreeNode getRoot()
        {
            if ( root == null )
            {
                root = createTree();
            }
            return root;
        }

        public ITreeNode createTree()
        {
            final ITreeNode result = new DefaultTreeNode();

            for (ShoppingList l : manager.getEntries())
            {
                final ITreeNode listNode = createNodeForShoppingList( l );
                result.addChild( listNode );
            }

            sortChildren( result, comp, true );
            return result;
        }

        private ITreeNode createNodeForShoppingListEntry(ShoppingListEntry entry)
        {
            return new DefaultTreeNode( entry );
        }

        protected ITreeNode addEntryNodes(ITreeNode parent, ShoppingList l)
        {
            for (ShoppingListEntry entry : l.getEntries())
            {
                parent.addChild( createNodeForShoppingListEntry( entry ) );
            }
            return parent;
        }

        private ITreeNode createNodeForShoppingList(ShoppingList l)
        {
            return addEntryNodes( new DefaultTreeNode( l ), l );
        }

        @Override
        public void shoppingListAdded(ShoppingList list)
        {
            if ( root == null )
            {
                refresh();
                return;
            }

            final ITreeNode child = createNodeForShoppingList( list );

            int idx = addChild( getRoot(), child );
            fireEvent( EventType.ADDED, new TreeModelEvent( this, getRoot()
                    .getPathToRoot(), new int[] { idx }, new Object[] { child } ) );
        }

        @Override
        public void shoppingListChanged(ShoppingList list)
        {
            if ( root == null )
            {
                refresh();
                return;
            }

            ITreeNode listNode = findNodeFor( list );
            if ( listNode != null )
            {
                listNode.removeChildren();
                addEntryNodes( listNode, list );
                structureChanged( listNode );
                nodeValueChanged( listNode );
            }
        }

        public ITreeNode findNodeFor(ShoppingList list)
        {
            for (ITreeNode child : getRoot().getChildren())
            {
                if ( child.getValue() == list )
                {
                    return child;
                }
            }
            return null;
        }

        public ITreeNode findNodeFor(ShoppingListEntry entry)
        {
            final ITreeNode listNode = findNodeFor( entry.getShoppingList() );

            if ( listNode != null )
            {
                for (ITreeNode child : listNode.getChildren())
                {
                    if ( child.getValue() == entry )
                    {
                        return child;
                    }
                }
            }
            return null;
        }

        @Override
        public void shoppingListRemoved(ShoppingList list)
        {
            if ( root == null )
            {
                refresh();
                return;
            }

            final ITreeNode node = findNodeFor( list );
            if ( node != null )
            {
                int idx = removeChild( (ITreeNode) node.getParent(), node );

                fireEvent( EventType.REMOVED, new TreeModelEvent( this, getRoot()
                        .getPathToRoot(), new int[] { idx }, new Object[] { node } ) );
            }
        }

        @Override
        public void listEntryAdded(ShoppingList list, ShoppingListEntry newEntry)
        {

            final ITreeNode listNode = findNodeFor( list );

            if ( listNode != null )
            {
                final ITreeNode newNode = createNodeForShoppingListEntry( newEntry );

                final int idx = addChild( listNode, newNode );

                fireEvent( EventType.ADDED, new TreeModelEvent( this, listNode
                        .getPathToRoot(), new int[] { idx }, new Object[] { newNode } ) );
            }

        }

        @Override
        public void listEntryChanged(ShoppingList list, ShoppingListEntry changedEntry)
        {
            final ITreeNode entryNode = findNodeFor( changedEntry );

            if ( entryNode != null )
            {
                super.nodeValueChanged( entryNode );
            }
        }

        @Override
        public void listEntryRemoved(ShoppingList list, ShoppingListEntry removedEntry)
        {

            final ITreeNode entryNode = findNodeFor( removedEntry );

            if ( entryNode == null )
            {
                return;
            }

            final ITreeNode listNode = (ITreeNode) entryNode.getParent();

            final int idx = removeChild( listNode, entryNode );

            if ( idx != - 1 )
            {
                fireEvent( EventType.REMOVED, new TreeModelEvent( this, listNode
                        .getPathToRoot(), new int[] { idx }, new Object[] { entryNode } ) );
            }
        }
    }

}
