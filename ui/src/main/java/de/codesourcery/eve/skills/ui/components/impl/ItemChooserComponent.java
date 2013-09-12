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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.db.datamodel.InventoryCategory;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.DefaultTreeNode;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.model.LazyTreeNode;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.model.impl.ItemTreeBuilder;
import de.codesourcery.eve.skills.util.Misc;

public class ItemChooserComponent extends AbstractEditorComponent
{

    @Resource(name = "static-datamodel")
    private IStaticDataModel dataModel;

    private final JTree tree = new JTree();

    private final JTable itemTable = new JTable();

    private final ItemTreeBuilder treeBuilder;

    public enum SelectionMode
    {
        SINGLE_SELECTION, MULTIPLE_SELECTION;
    }

    private final SelectionMode selectionMode;

    private final InventoryTypeModel selectedItemsModel = new InventoryTypeModel();

    private TreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row,
                hasFocus );

            final ITreeNode node = (ITreeNode) value;
            String toolTip = null;
            final Object nodeValue = node.getValue();
            if ( nodeValue instanceof InventoryCategory )
            {
                setText( ( (InventoryCategory) nodeValue ).getCategoryName() );
            }
            else if ( nodeValue instanceof InventoryGroup )
            {
                setText( ( (InventoryGroup) nodeValue ).getGroupName() );
            }
            else if ( nodeValue instanceof InventoryType )
            {
                setText( ( (InventoryType) nodeValue ).getName() );
                toolTip = getItemDescription( (InventoryType) nodeValue );
            }

            setToolTipText( toolTip );
            return this;
        }
    };

    public ItemChooserComponent() {
        this( SelectionMode.SINGLE_SELECTION );
    }

    public ItemChooserComponent(SelectionMode mode) {
        super();

        if ( mode == null )
        {
            throw new IllegalArgumentException( "mode cannot be NULL" );
        }
        this.treeBuilder = new ItemTreeBuilder( dataModel );
        this.selectionMode = mode;
    }

    @Override
    protected JButton createCancelButton()
    {
        return new JButton( "Cancel" );
    }

    @Override
    protected JButton createOkButton()
    {
        return new JButton( "Ok" );
    }

    @Override
    protected void onAttachHook(IComponentCallback callback)
    {
        tree.setModel( treeBuilder.getTreeModel() );
        ToolTipManager.sharedInstance().registerComponent( tree );
    }

    @Override
    protected void onDetachHook()
    {
        ToolTipManager.sharedInstance().unregisterComponent( tree );
    }

    private void handleTreeExpansion(TreeExpansionEvent event)
    {
        final ITreeNode n = (ITreeNode) event.getPath().getLastPathComponent();

        if ( ! ( n instanceof LazyTreeNode ) )
        {
            return;
        }

        final LazyTreeNode node = (LazyTreeNode) n;

        log.debug( "handleTreeExpansion(): children fetched = " + node.childrenFetched() );

        if ( node.childrenFetched() )
        {
            return;
        }

        // fetch children
        final InventoryGroup group = (InventoryGroup) node.getValue();

        final List<InventoryType> items = dataModel.getInventoryTypes( group );

        log.debug( "handleTreeExpansion(): got " + items.size()
                + " inventory types for group" + group.getGroupName() );

        Collections.sort( items, InventoryType.BY_NAME_COMPARATOR );

        final List<ITreeNode> nodes = new ArrayList<ITreeNode>();
        for (InventoryType t : items)
        {
            if ( t.isPublished() )
            {
                nodes.add( new DefaultTreeNode( t ) );
            }
        }

        node.setChildrenFetched();

        treeBuilder.getTreeModel().addChildren( node, nodes );
    }

    @Override
    protected JPanel createPanelHook()
    {

        tree.setRootVisible( false );
        tree.setCellRenderer( treeRenderer );

        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION );

        tree.addMouseListener( new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if ( e.getClickCount() != 2 || e.isPopupTrigger() )
                {
                    return;
                }

                final TreePath path = tree.getClosestPathForLocation( e.getX(), e.getY() );
                final ITreeNode node = (ITreeNode) path.getLastPathComponent();

                if ( node.getValue() instanceof InventoryType )
                {
                    final InventoryType selectedItem = (InventoryType) node.getValue();
                    selectedItemsModel.addItem( selectedItem );
                    if ( selectionMode == SelectionMode.SINGLE_SELECTION )
                    {
                        okButtonClicked();
                    }
                    else if ( selectionMode == SelectionMode.MULTIPLE_SELECTION )
                    {
                        // ok
                    }
                    else
                    {
                        throw new RuntimeException( "Unhandled mode " + selectionMode );
                    }
                }
            }
        } );

        tree.addTreeWillExpandListener( new TreeWillExpandListener() {

            @Override
            public void treeWillCollapse(TreeExpansionEvent event)
                    throws ExpandVetoException
            {
            }

            @Override
            public void treeWillExpand(TreeExpansionEvent event)
                    throws ExpandVetoException
            {
                handleTreeExpansion( event );
            }
        } );

        final JScrollPane pane = new JScrollPane( tree );
        pane.setPreferredSize( new Dimension( 400, 400 ) );

        final JPanel result = new JPanel();
        result.setLayout( new GridBagLayout() );

        switch ( selectionMode )
        {
            case SINGLE_SELECTION:
                result.add( pane, constraints().useRemainingSpace().resizeBoth().end() );
                break;
            case MULTIPLE_SELECTION:
                result.add( pane, constraints( 0, 0 ).useRelativeWidth().resizeBoth()
                        .end() );
                result.add( createListViewPanel(), constraints( 1, 0 )
                        .useRemainingWidth().resizeBoth().end() );
                break;
            default:
                throw new RuntimeException( "Unhandled selection mode" + selectionMode );
        }

        return result;
    }

    private void addButtonClicked()
    {
        final TreePath selectionPath = tree.getSelectionPath();
        if ( selectionPath == null || selectionPath.getPathCount() < 3 )
        {
            return;
        }

        final ITreeNode selected = (ITreeNode) selectionPath.getLastPathComponent();
        if ( selected.getValue() instanceof InventoryType )
        {
            selectedItemsModel.addItem( (InventoryType) selected.getValue() );
        }
    }

    public void removeButtonClicked()
    {
        final List<ItemWithQuantity> selection = getSelectedValues();
        for (ItemWithQuantity o : selection)
        {
            selectedItemsModel.removeItem( o );
        }
    }

    protected List<ItemWithQuantity> getSelectedValues()
    {
        final List<ItemWithQuantity> result = new ArrayList<ItemWithQuantity>();

        final int[] selectedRows = itemTable.getSelectedRows();
        if ( ! ArrayUtils.isEmpty( selectedRows ) )
        {
            for (int viewRow : selectedRows)
            {
                final int modelRow = itemTable.convertRowIndexToModel( viewRow );
                result.add( selectedItemsModel.getRow( modelRow ) );
            }
        }
        return result;
    }

    private JPanel createListViewPanel()
    {

        final JPanel result = new JPanel();
        result.setLayout( new GridBagLayout() );

        // create button panel

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new GridBagLayout() );

        final JButton addButton = new JButton( "====>" );
        final JButton removeButton = new JButton( "<====" );

        final ActionListener buttonListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                if ( e.getSource() == addButton )
                {
                    addButtonClicked();
                }
                else if ( e.getSource() == removeButton )
                {
                    removeButtonClicked();
                }
                else
                {
                    throw new RuntimeException( "Unhandled event source " + e.getSource() );
                }
            }

        };

        addButton.addActionListener( buttonListener );
        removeButton.addActionListener( buttonListener );

        buttonPanel.add( addButton, constraints( 0, 0 ).noResizing().end() );
        buttonPanel.add( removeButton, constraints( 0, 1 ).noResizing().end() );

        // create list panel

        itemTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        itemTable.setModel( selectedItemsModel );
        itemTable.setRowSorter( selectedItemsModel.getRowSorter() );

        result.add( buttonPanel, constraints( 0, 0 ).useRelativeWidth().resizeBoth()
                .end() );
        result.add( new JScrollPane( itemTable ), constraints( 1, 0 ).useRemainingWidth()
                .resizeBoth().end() );

        return result;
    }

    @Override
    protected boolean hasValidInput()
    {
        return true;
    }

    protected String getItemDescription(InventoryType type)
    {
        return "<HTML><BODY>" + Misc.wrap( type.getDescription(), "<BR>", 40 )
                + "</BODY></HTML>";
    }

    public List<InventoryType> getSelectedItems()
    {
        final List<InventoryType> result = new ArrayList<InventoryType>();
        for (ItemWithQuantity item : selectedItemsModel.getSelectedItems())
        {
            result.add( item.getType() );
        }
        return result;
    }

    public List<ItemWithQuantity> getSelectedItemsWithQuantity()
    {
        return selectedItemsModel.getSelectedItems();
    }

    protected final class InventoryTypeModel extends AbstractTableModel<ItemWithQuantity>
    {

        private final List<ItemWithQuantity> selectedItems =
                new ArrayList<ItemWithQuantity>();

        public static final int TYPE_COL = 0;
        public static final int QUANTITY_COL = 1;

        public InventoryTypeModel() {
            super( new TableColumnBuilder().add( "Type" ).add( "Quantity", Integer.class ) );
        }

        public void addItem(InventoryType newItem)
        {

            if ( newItem == null )
            {
                throw new IllegalArgumentException( "item cannot be NULL" );
            }

            int idx2 = 0;
            for (ItemWithQuantity existing : selectedItems)
            {
                if ( existing.getType().getTypeId().equals( newItem.getTypeId() ) )
                {
                    // duplicate , just increment quantity by 1
                    existing.incQuantity( 1 );
                    notifyRowChanged( idx2 );
                    return;
                }
                idx2++;
            }

            int idx = 0;
            for (ItemWithQuantity existing : selectedItems)
            {
                if ( existing.getType().getName().compareTo( newItem.getName() ) > 0 )
                {
                    selectedItems.add( idx, new ItemWithQuantity( newItem, 1 ) );
                    notifyRowInserted( idx );
                    return;
                }
                idx++;
            }

            idx = selectedItems.size();
            selectedItems.add( new ItemWithQuantity( newItem, 1 ) );
            notifyRowInserted( idx );
        }

        public void removeItem(ItemWithQuantity item)
        {
            if ( item == null )
            {
                throw new IllegalArgumentException( "item cannot be NULL" );
            }
            int idx = 0;
            for (ItemWithQuantity existing : selectedItems)
            {
                if ( existing.getType().getTypeId().equals( item.getType().getTypeId() ) )
                {
                    selectedItems.remove( idx );
                    notifyRowRemoved( idx );
                    return;
                }
                idx++;
            }
        }

        public List<ItemWithQuantity> getSelectedItems()
        {
            return selectedItems;
        }

        // ============== new

        @Override
        protected Object getColumnValueAt(int modelRowIndex, int modelColumnIndex)
        {
            final ItemWithQuantity item = getRow( modelRowIndex );

            switch ( modelColumnIndex )
            {
                case TYPE_COL:
                    return item.getType().getName();
                case QUANTITY_COL:
                    return item.getQuantity();
                default:
                    throw new IllegalArgumentException( "Invalid column "
                            + modelColumnIndex );
            }
        }

        @Override
        public ItemWithQuantity getRow(int modelRow)
        {
            return selectedItems.get( modelRow );
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == QUANTITY_COL;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex)
        {
            final ItemWithQuantity row = getRow( rowIndex );
            final int newQuantity = (Integer) value;
            if ( newQuantity > 0 && newQuantity != row.getQuantity() )
            {
                row.setQuantity( newQuantity );
                notifyRowsChanged( rowIndex, rowIndex );
            }
        }

        @Override
        public int getRowCount()
        {
            return selectedItems.size();
        }

    }
}
