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
package de.codesourcery.eve.skills.ui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import de.codesourcery.eve.skills.ui.utils.Misc;

public final class DefaultComboBoxModel<T> implements ComboBoxModel, ListDataListener  {

	// guarded-by: listeners
	private final List<ListDataListener> listeners =
		new ArrayList<ListDataListener>();
	
	private ListModel model;
	private T selected;
	
	public DefaultComboBoxModel() {
		this( new ArrayList<T>() );
	}
	
	public DefaultComboBoxModel(T... data) {
		this( createListModel( Arrays.asList( data ) ) );
	}
	
	
	public DefaultComboBoxModel(Collection<T> data) {
		this( createListModel( new ArrayList<T>( data ) ) );
	}
	
	public DefaultComboBoxModel(List<T> data) {
		this( createListModel( Collections.unmodifiableList(data ) ) );
	}
	
	protected static <T> ListModel createListModel(final List<T> data) {
		return new ListModel() {

			@Override
			public void addListDataListener(ListDataListener l) {
			}

			@Override
			public Object getElementAt(int index) {
				return data.get( index );
			}

			@Override
			public int getSize() {
				return data.size();
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
			}};
	}
	
	public DefaultComboBoxModel(ListModel model) {
		this.model = model;
		model.addListDataListener( this );
	}
	
	@Override
	public T getSelectedItem() {
		return selected;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setSelectedItem(Object anItem) {
		this.selected = (T) anItem;
	}
	
	public void setData(List<T> data) {
		if ( this.model != null ) {
			this.model.removeListDataListener( this );
			this.model = null;
		}
		
		this.model = createListModel( data );
		contentsChanged( new ListDataEvent(this,ListDataEvent.CONTENTS_CHANGED, 0 , model.getSize() ) );
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (listeners) {
			this.listeners.add(l);
		}
	}

	@Override
	public Object getElementAt(int index) {
		return model.getElementAt( index );
	}

	@Override
	public int getSize() {
		return model.getSize();
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (listeners) {
			this.listeners.remove(l);
		}		
	}

	@Override
	public void contentsChanged(final ListDataEvent arg0) {
		
		final List<ListDataListener> copy;
		synchronized( listeners ) {
			copy =
				new ArrayList<ListDataListener>( this.listeners );
		}
		
		Misc.runOnEventThread( new Runnable() {

			@Override
			public void run()
			{
				for ( ListDataListener l : copy ) {
					l.contentsChanged( arg0 );
				}				
			}} );
	}

	@Override
	public void intervalAdded(ListDataEvent arg0) {
		final List<ListDataListener> copy;
		synchronized( listeners ) {
			copy =
				new ArrayList<ListDataListener>( this.listeners );
		}
		
		for ( ListDataListener l : copy ) {
			l.intervalAdded( arg0 );
		}		
	}

	@Override
	public void intervalRemoved(ListDataEvent arg0) {
		final List<ListDataListener> copy;
		synchronized( listeners ) {
			copy =
				new ArrayList<ListDataListener>( this.listeners );
		}
		
		for ( ListDataListener l : copy ) {
			l.intervalRemoved( arg0 );
		}		
	}


}
