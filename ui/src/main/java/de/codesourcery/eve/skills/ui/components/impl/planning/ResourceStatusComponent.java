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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.production.ResourceManagerFactory;
import de.codesourcery.eve.skills.production.ShoppingListManager;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.renderer.FixedBooleanTableCellRenderer;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.IResource;
import de.codesourcery.planning.IResourceManager;
import de.codesourcery.planning.IResourceType;

public class ResourceStatusComponent extends AbstractComponent 
{
	private final MyTableModel model = new MyTableModel();
	private final JTable table = new JTable( model );
	
	private ICharacter selectedCharacter;
		
	@Resource(name="resource-manager-factory")
	private ResourceManagerFactory resourceManagerFactory;
	
	@Resource(name="shoppinglist-manager")
	private ShoppingListManager shoppingListManager;
	
	private final JButton addToShoppingListButton =
		new JButton("Add selected items to shopping list...");
	
	private final List<ItemWithQuantity> data = new ArrayList<ItemWithQuantity>();
	
	private String title;
	
	public ResourceStatusComponent(String title) {
		
		if ( StringUtils.isBlank( title ) ) {
			throw new IllegalArgumentException("title cannot be blank.");
		}
		
		this.title = title;
	}
	
	@Override
	protected JPanel createPanel()
	{
		final JPanel result =
			new JPanel();
		
		table.setFillsViewportHeight( true );
		
		table.setRowSorter( model.getRowSorter() );
		
		FixedBooleanTableCellRenderer.attach( table );
		
		table.setDefaultRenderer( Integer.class , new DefaultTableCellRenderer() 
		{
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
						row, column);
		
				if ( !( value instanceof Integer )  ) {
					return this;
				}
				
				if ( column == 0 ) {
					setHorizontalAlignment( SwingConstants.LEFT );
				} else {
					setHorizontalAlignment( SwingConstants.RIGHT );
				}
				
				Integer amount = (Integer) value;
				if ( amount.intValue() < 0 ) 
				{
					if ( ! isSelected ) {
						setBackground( Color.RED );
					} else {
						setBackground( table.getSelectionBackground() );
					}
				} else {
					setBackground( table.getBackground() );
				}
				return this;
			}
		} );
		
		addToShoppingListButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				final List<ItemWithQuantity> items =
					model.getSelectedItems();
				
				if ( items.isEmpty() ) {
					return;
				}
				
				final ShoppingListEditorComponent comp =
					new ShoppingListEditorComponent( title , "" , items );
				
				comp.setModal( true );
				ComponentWrapper.wrapComponent( comp ).setVisible( true );
				if ( ! comp.wasCancelled() && ! comp.getShoppingList().isEmpty() ) {
					shoppingListManager.addShoppingList( comp.getShoppingList() );
					getComponentCallback().dispose( ResourceStatusComponent.this );
				}
				
			}} );
		
		new GridLayoutBuilder().add(
				new GridLayoutBuilder.VerticalGroup(
						new GridLayoutBuilder.Cell( new JScrollPane( table ) ),
						new GridLayoutBuilder.FixedCell( addToShoppingListButton )
				)
		).addTo( result );
		return result;
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		model.setData( data );
	}

	private static final class ResourceStatus 
	{
		private final ItemWithQuantity item;
		private final IResource resource;
		private boolean isSelected;
		
		public ResourceStatus(ItemWithQuantity item, IResource resource) 
		{
			if ( item == null ) {
				throw new IllegalArgumentException("item cannot be NULL");
			}
			if ( resource == null ) {
				throw new IllegalArgumentException(
						"resource cannot be NULL");
			}
			this.item = item;
			this.resource = resource;
		}
		
		public boolean isSelected()
		{
			return isSelected;
		}
		
		public void setSelected(boolean isSelected)
		{
			this.isSelected = isSelected;
		}
	}
	
	public void setData(ICharacter character , List<ItemWithQuantity> quantities) 
	{
		if ( character == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		
		this.selectedCharacter = character;
		this.data.clear();
		this.data.addAll( quantities );
	}
	
	private final class MyTableModel extends AbstractTableModel<ResourceStatus> {

		private final List<ResourceStatus> resources =
			new ArrayList<ResourceStatus>();
		
		public MyTableModel() {
			super( new TableColumnBuilder()
			    .add("Add to shopping list",Boolean.class)
			    .add("Item")
				.add("Requested quantity",Integer.class)
				.add("Quantity remaining", Integer.class)
			);
		}
		
		public List<ItemWithQuantity> getSelectedItems() {
			
			final List<ItemWithQuantity> result =
				new ArrayList<ItemWithQuantity>();
			
			for ( ResourceStatus r : resources ) 
			{
				/*
				 * TODO: Currently I'm always adding the full required amount
				 * TODO: to the shopping list ... fix layout so one can choose
				 * TODO: to add the missing amount (delta) instead !!
				 */
				if ( r.isSelected() ) 
				{
					final int amount;
					if ( r.resource.isDepleted() ) 
					{
						// resource depleted , put delta on shopping list
						amount = (int) -r.resource.getAmount();
					} 
					else 
					{
						// resource not depleted , add total required amount
						amount = r.item.getQuantity();
					}
					
					result.add( new ItemWithQuantity( r.item.getType() , amount ) );
				}
			}
			
			return result;
		}
		
		public void setData(List<ItemWithQuantity> quantities) {
			
			if ( selectedCharacter == null ) {
				return;
			}
			
			// merge quantities by type
			final Map<InventoryType,ItemWithQuantity> items =
				ItemWithQuantity.mergeByTypeMap( quantities );
			
			final IResourceManager manager = 
				resourceManagerFactory.getResourceManager( selectedCharacter );
			
			final Map<IResourceType, IResource> resourceStatus = 
				manager.calculateProjectedResourceStatus( items.values() , IProductionLocation.ANY_LOCATION );
			
			final List<ResourceStatus> tmp =
				new ArrayList<ResourceStatus>();
			
			boolean materialsAreMissing = false;
			for ( Map.Entry<IResourceType,IResource> entry : resourceStatus.entrySet() ) 
			{
				final ResourceStatus newStatus = 
					new ResourceStatus( items.get( entry.getKey() ) , entry.getValue() );
				
				if ( entry.getValue().getAmount() < 0.0d ) {
					materialsAreMissing = true;
					newStatus.setSelected( true );
				}
				
				tmp.add( newStatus );
			}
			this.resources.clear();
			this.resources.addAll( tmp );
			modelDataChanged();
			
			addToShoppingListButton.setEnabled( materialsAreMissing );
		} 
		
		public boolean hasSelectedItems() {
			for ( ResourceStatus r : this.resources ) {
				if ( r.isSelected() ) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 0;
		}
		
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex)
		{
			if ( columnIndex != 0 ) {
				throw new RuntimeException("Internal error,invalid column "+columnIndex);
			}
			
			boolean selected = (Boolean) value;
			getRow( rowIndex ).setSelected( selected );
			
			if ( selected ) {
				addToShoppingListButton.setEnabled( true );
			} else {
				addToShoppingListButton.setEnabled( hasSelectedItems() );
			}
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final ResourceStatus status =
				getRow( modelRowIndex );

			switch( modelColumnIndex ) {
				case 0:
					return status.isSelected();
				case 1:
					return status.item.getType().getName();
				case 2: // requested
					return (int) status.item.getQuantity();
				case 3: // available
					return (int) status.resource.getAmount();
				default:
					throw new IllegalArgumentException("Invalid column "+modelColumnIndex);
			}
		}

		@Override
		public ResourceStatus getRow(int modelRow)
		{
			return resources.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return resources.size();
		}
		
	}
}
