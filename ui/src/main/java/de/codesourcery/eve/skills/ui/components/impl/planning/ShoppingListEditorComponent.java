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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.ShoppingList;
import de.codesourcery.eve.skills.datamodel.ShoppingList.ShoppingListEntry;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.impl.ItemChooserComponent;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;

public class ShoppingListEditorComponent extends AbstractEditorComponent
{

    private final JTextField title = new JTextField();
    private final JTextArea description = new JTextArea( 10, 25 );

    private final ShoppingListModel tableModel;
    private final JTable table;

    private final JButton addItemsButton = new JButton( "Add items..." );

    private ShoppingList existing;

    private boolean existingEntryChanged = false;

    public ShoppingListEditorComponent(ShoppingList shoppingList) {
        this( new ShoppingList( shoppingList ), shoppingList );
    }

    protected ShoppingListEditorComponent(ShoppingList shoppingList, ShoppingList existing) {
        tableModel = new ShoppingListModel( shoppingList );
        table = new JTable( tableModel );
        table.setRowSorter( tableModel.getRowSorter() );

        title.setText( shoppingList.getTitle() );
        description.setText( shoppingList.getDescription() );
        this.existing = existing;
    }

    public ShoppingListEditorComponent(String title, String notes,
            List<ItemWithQuantity> items) {
        this( createShoppingList( title, notes, items ), null );
    }

    public boolean isExistingEntryEdited()
    {
        return existingEntryChanged;
    }

    private static final ShoppingList createShoppingList(String title, String notes,
            List<ItemWithQuantity> items)
    {

        final ShoppingList shoppingList = new ShoppingList( title );
        shoppingList.setDescription( notes );

        for (ItemWithQuantity item : items)
        {
            shoppingList.addEntry( item.getType(), item.getQuantity() );
        }
        return shoppingList;
    }

    public ShoppingList getShoppingList()
    {
        if ( wasCancelled() )
        {
            throw new IllegalStateException(
                    "You must not retrieve results from a cancelled editor" );
        }

        if ( this.existing == null )
        {
            return tableModel.shoppingList;
        }
        return existing;

    }

    protected boolean mergeViewModelWithExistingList()
    {

        boolean existingEntryChanged = false;

        if ( ! StringUtils.equals( existing.getTitle(), title.getText() ) )
        {
            existing.setTitle( title.getText() );
            existingEntryChanged = true;
        }

        if ( ! StringUtils.equals( existing.getDescription(), description.getText() ) )
        {
            existing.setDescription( existing.getDescription() );
            existingEntryChanged = true;
        }

        // remove all entries no longer in the view model

        // create copy so we don't get a ConcurrentModificationException
        // when removing entries while iterating
        final List<ShoppingListEntry> existingEntries =
                new ArrayList<ShoppingListEntry>( existing.getEntries() );

        for (ShoppingListEntry entry : existingEntries)
        {
            boolean found = false;
            for (ShoppingListEntry viewEntry : tableModel.shoppingList.getEntries())
            {
                if ( entry.hasSameTypeAs( viewEntry ) )
                {
                    found = true;
                    break;
                }
            }
            if ( ! found )
            {
                existing.removeEntry( entry );
                existingEntryChanged = true;
            }
        }

        for (ShoppingListEntry fromView : tableModel.shoppingList.getEntries())
        {
            // try to update existing entry
            boolean found = false;
            for (ShoppingListEntry fromExisting : existing.getEntries())
            {
                if ( fromView.hasSameTypeAs( fromExisting ) )
                {
                    found = true;
                    if ( ! fromExisting.matches( fromView ) )
                    {
                        fromExisting.updateWith( fromView );
                        existingEntryChanged = true;
                    }
                }
            }

            // add new entry
            if ( ! found )
            {
                existingEntryChanged = true;
                existing.addEntry( fromView );
            }
        }

        return existingEntryChanged;
    }

    @Override
    protected JButton createCancelButton()
    {
        return new JButton( "Cancel" );
    }

    @Override
    protected JButton createOkButton()
    {
        return new JButton( "Save shopping list" );
    }

    @Override
    protected void okButtonClickedHook()
    {
        tableModel.shoppingList.setTitle( title.getText() );
        tableModel.shoppingList.setDescription( description.getText() );

        if ( this.existing != null )
        {
            this.existingEntryChanged = mergeViewModelWithExistingList();
        }
    }

    @Override
    protected JPanel createPanelHook()
    {

        final JPanel result = new JPanel();

        addItemsButton.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                final ItemChooserComponent comp =
                        new ItemChooserComponent(
                                ItemChooserComponent.SelectionMode.MULTIPLE_SELECTION );

                comp.setModal( true );
                ComponentWrapper.wrapComponent( "Add items", comp ).setVisible( true );

                if ( ! comp.wasCancelled() )
                {
                    for (ItemWithQuantity item : comp.getSelectedItemsWithQuantity())
                    {
                        tableModel.addItem( item );
                    }
                }
            }
        } );

        final PopupMenuBuilder menuBuilder = new PopupMenuBuilder();

        menuBuilder.addItem( "Delete", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                deleteSelectedEntriesFromList();
            }

            @Override
            public boolean isEnabled()
            {
                return table.getSelectedRows().length > 0;
            }
        } );

        menuBuilder.attach( table );

        table.setFillsViewportHeight( true );

        final JPanel top = new JPanel();

        new GridLayoutBuilder()
                .add(
                    new GridLayoutBuilder.VerticalGroup(
                            new GridLayoutBuilder.HorizontalGroup(
                                    new GridLayoutBuilder.FixedCell( new JLabel( "Title" ) ),
                                    new GridLayoutBuilder.FixedCell( title ) ),
                            new GridLayoutBuilder.HorizontalGroup(
                                    new GridLayoutBuilder.FixedCell( new JLabel(
                                            "Description" ) ),
                                    new GridLayoutBuilder.FixedCell( description ) ),
                            new GridLayoutBuilder.HorizontalGroup(
                                    new GridLayoutBuilder.FixedCell( addItemsButton ) ) ) )
                .addTo( top );

        new GridLayoutBuilder().add(
            new GridLayoutBuilder.VerticalGroup( new GridLayoutBuilder.FixedCell( top ),
                    new GridLayoutBuilder.Cell( new JScrollPane( table ) ) ) ).addTo(
            result );

        this.title.setColumns( 25 );
        this.description.setLineWrap( true );
        this.description.setWrapStyleWord( true );

        return result;
    }

    protected void deleteSelectedEntriesFromList()
    {
        final List<ShoppingListEntry> toBeRemoved = new ArrayList<ShoppingListEntry>();

        final int[] viewRows = table.getSelectedRows();
        for (int viewRow : viewRows)
        {
            toBeRemoved
                    .add( tableModel.getRow( table.convertRowIndexToModel( viewRow ) ) );
        }

        for (ShoppingListEntry remove : toBeRemoved)
        {
            tableModel.removeEntry( remove );
        }
    }

    @Override
    protected boolean hasValidInput()
    {
        final boolean isValid = ! StringUtils.isBlank( title.getText() );

        if ( ! isValid )
        {
            displayError( "Shopping list title cannot be blank" );
        }
        return isValid;
    }

    private final class ShoppingListModel extends AbstractTableModel<ShoppingListEntry>
    {

        private final ShoppingList shoppingList;

        public ShoppingListModel(ShoppingList list) {
            super( new TableColumnBuilder().add( "Material" ).add( "Quantity",
                Integer.class ) );
            if ( list == null )
            {
                throw new IllegalArgumentException( "list cannot be NULL" );
            }
            this.shoppingList = list;
        }

        public void addItem(ItemWithQuantity item)
        {
            final ShoppingListEntry newEntry = shoppingList.addEntry( item );
            int idx = 0;
            for (ShoppingListEntry entry : shoppingList.getEntries())
            {
                if ( entry == newEntry )
                {
                    notifyRowInserted( idx );
                }
                idx++;
            }
        }

        public void removeEntry(ShoppingListEntry entry)
        {
            int i = 0;
            for (ShoppingListEntry e : shoppingList.getEntries())
            {
                if ( e == entry )
                {
                    shoppingList.removeEntry( e );
                    notifyRowRemoved( i );
                    return;
                }
                i++;
            }
        }

        @Override
        protected Object getColumnValueAt(int modelRowIndex, int modelColumnIndex)
        {
            final ShoppingListEntry row = getRow( modelRowIndex );
            switch ( modelColumnIndex )
            {
                case 0:
                    return row.getType().getName();
                case 1:
                    return row.getQuantity();
                default:
                    throw new IllegalArgumentException( "Invalid column "
                            + modelColumnIndex );
            }
        }

        @Override
        public ShoppingListEntry getRow(int modelRow)
        {
            return shoppingList.getEntries().get( modelRow );
        }

        @Override
        public int getRowCount()
        {
            return shoppingList.getEntries().size();
        }
    }

}
