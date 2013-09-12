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
package de.codesourcery.eve.skills.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;

/**
 * Abstract base-class for simple list-editing components. 
 *
 * This component renders a <code>JList</code> with the
 * model data and three buttons for adding, editing
 * and removing list items. Subclasses may override
 * {@link #populateButtonPanel(JPanel)} to customize 
 * the button panel. 
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class ListEditingComponent<T> extends AbstractComponent implements 
ListSelectionListener ,
ISelectionProvider<T>,
ActionListener {

	private final JList list =
		new JList();
	
	private final ListModel model;
	
	protected final JButton addButton =
		new JButton("Add...");
	
	protected final JButton removeButton =
		new JButton("Remove");
	
	protected final JButton editButton =
		new JButton("Edit...");	
	
	// guarded-by: listeners
	private final List<ISelectionListener<T>> listeners =
		new ArrayList<ISelectionListener<T>>();
	
	public void addSelectionListener(ISelectionListener<T> l) {
		synchronized (listeners) {
			listeners.add( l );
		}
	}
	
	public void setSelectedIndex(int index) {
		this.list.setSelectedIndex( index );
	}
	
	public void removeSelectionListener(ISelectionListener<T> l) {
		synchronized (listeners) {
			listeners.remove( l );
		}
	}
	
	@SuppressWarnings("unchecked")
	public T getSelectedItem() {
		return (T) list.getSelectedValue();
	}
	
	public ListEditingComponent(ListModel model) 
	{
		if (model == null) {
			throw new IllegalArgumentException("model cannot be NULL");
		}
	
		this.model = model;
		this.list.setCellRenderer( createListRenderer() );
		this.list.setModel( model );
		if ( model.getSize() > 0 ) {
			this.list.setSelectedIndex( 0 );
		}
		this.list.getSelectionModel().addListSelectionListener( this );
		
		addButton.addActionListener( this );
		removeButton.addActionListener( this );
		editButton.addActionListener( this );
	}
	
	protected abstract ListCellRenderer createListRenderer();

	/**
	 * Hook method invoked from {@link #getPanel()}
	 * after the generic part of the panel has been populated.
	 * 
	 * @param panel The panel that will be returned by
	 * {@link #getPanel()}.
	 */
	protected void createPanelHook(JPanel panel) {
		
	}
	
	@Override
	protected final JPanel createPanel() {
		
		final JPanel result = new JPanel();
		
		result.setLayout( new BorderLayout() );
		
		final JScrollPane scrollPane =
			new JScrollPane( list );

		if ( ! StringUtils.isBlank( getTitle() ) )  {
			scrollPane.setBorder( BorderFactory.createTitledBorder( getTitle() ) );
		} 
		
		scrollPane.setPreferredSize( new Dimension(150,400) );
		result.add( scrollPane , BorderLayout.NORTH );
		
		final JPanel buttonPanel =
			new JPanel();
		
		populateButtonPanel(buttonPanel);
		
		result.add( buttonPanel, BorderLayout.SOUTH );
		
		createPanelHook(result);
		
		return result;
	}
	
	protected void populateButtonPanel(JPanel buttonPanel) {
		buttonPanel.add( addButton );
		buttonPanel.add( editButton );
		buttonPanel.add( removeButton );
	}

	protected ListModel getModel() {
		return model;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void valueChanged(ListSelectionEvent event) {
		final T item = (T) model.getElementAt( event.getFirstIndex() );
		selectionChanged( item );
	}

	protected void selectionChanged(T selected) {
		
		final List<ISelectionListener<T>> copy;
		synchronized (listeners) {
			copy = new ArrayList<ISelectionListener<T>>( this.listeners );
		}
		
		for ( ISelectionListener<T> l : copy ) {
			l.selectionChanged( selected );
		}
	}
	
	protected abstract void addItem();
	
	protected abstract void removeItem(T selected);
	
	protected abstract void editItem(T selected);
	
	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent event) {
		
		final Object src = event.getSource();
		
		if ( src == editButton || src == removeButton  ) {
			final int index = list.getSelectionModel().getMinSelectionIndex();
			if ( index != -1 ) {
				final T item = (T) model.getElementAt( index );
				if ( src == editButton ) {
					editItem( item );
				} else if ( src == removeButton ) {
					removeItem( item );
				} else {
					throw new RuntimeException("Internal error");
				}
			}
		} else if ( src == addButton ) {
			addItem();
		}
	}
}
