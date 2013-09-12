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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.ui.model.ViewDataModelChangeEvent.Type;
import de.codesourcery.eve.skills.ui.utils.Misc;

public class ListViewModel<T> implements IViewDataModel<T> {
	
	private static final Logger log = Logger.getLogger(ListViewModel.class);

	private final List<T> entries =
		new ArrayList<T>();

	private final List<IViewDataModelChangeListener> listeners =
		new ArrayList<IViewDataModelChangeListener>();
	
	private IViewFilter<T> viewFilter=null;

	public ListViewModel(T... data) {
		
		if ( data == null ) {
			throw new IllegalArgumentException("data cannot be NULL");
		}
		
		final Collection<T> list =
			new ArrayList<T>();
		
		for ( T obj : data ) {
			list.add( obj );
		}
		setData( list );
	}
	
	public ListViewModel(Collection<T> data) {
		setData( data );
	}
	public ListViewModel() {
	}

	public void setViewFilter(IViewFilter<T> viewFilter)
	{
		IViewFilter<T> oldFilter = this.viewFilter;
		this.viewFilter = viewFilter;
		if ( oldFilter != viewFilter ) {
			fireEvent( new ViewDataModelChangeEvent(this)  );
		}
	}
	
	public void viewFilterChanged() {
		fireEvent( new ViewDataModelChangeEvent(this)  );
	}
	
	public void setData(Collection<T> data) {
		if ( data == null ) {
			throw new IllegalArgumentException("data cannot be NULL");
		}
		
		synchronized (entries) {
			entries.clear();
		}
		// note: Do *NOT* call addAll() while holding the 'entries' monitor
		// may cause dead-lock with EDT when this method
		// has not been invoked from the EDT itself
		addAll( data );
	}
	
	public void addAll(Collection<T> data) {

		if ( data == null ) {
			throw new IllegalArgumentException("data cannot be NULL");
		}
		
		if ( data.isEmpty() ) {
			return;
		}
		
		synchronized( entries ) 
		{
			entries.addAll( data );
		}
		
		fireEvent( new ViewDataModelChangeEvent(this) );
	}
	
	protected boolean isHidden(T obj) {
		return this.viewFilter != null && this.viewFilter.isHidden( obj );
	}

	public int add(T data) {

		synchronized( entries ) {
			entries.add( data );
		}

		if ( isHidden( data ) ) {
			return -1;
		}
		
		synchronized( entries ) {
			int index = 0;
			for ( T item : getFilteredData() ) {
				if ( item.equals( data ) ) {
					fireEvent( new ViewDataModelChangeEvent(data,this,index,Type.ITEM_ADDED) );
					return index;
				}
				index++;
			}
		}
		
		return -1;
	}

	public int remove(T obj) {
		
		synchronized (entries) 
		{
			int i = 0;
			for ( T item : getFilteredData() ) {
				if ( item.equals( obj ) ) {
					this.entries.remove( item );
					fireEvent( new ViewDataModelChangeEvent(item,this,i ,Type.ITEM_REMOVED) );
					return i;
				}
				i++;
			}
			// item is not visible
			this.entries.remove( obj );
		}
		
		return -1;
	}
	
	public T remove(int index) {
		
		synchronized (entries) 
		{
			int i = 0;
			for ( T item : getFilteredData() ) {
				if ( i == index ) {
					this.entries.remove( item );
					fireEvent( new ViewDataModelChangeEvent(item,this,i ,Type.ITEM_REMOVED) );
					return item;
				}
				i++;
			}
			return this.entries.remove( index );
		}
	}

	@Override
	public void addDataModelChangeListener(IViewDataModelChangeListener l)
	{
		if ( l == null ) {
			throw new IllegalArgumentException("l cannot be NULL");
		}
		synchronized( listeners ) {
			this.listeners.add( l );
		}
	}
	
	private List<T> getFilteredData() 
	{
		
		synchronized( entries ) {

			if ( viewFilter == null ) {
				return Collections.unmodifiableList( this.entries );
			} 

			final List<T> result =
				new ArrayList<T>();

			for ( T obj : this.entries ) {
				if ( ! isHidden( obj ) ) {
					result.add( obj );
				}
			}
			return Collections.unmodifiableList( result );
		}
	}

	@Override
	public int getIndexFor(T data)
	{
		int index=0;
		synchronized( entries ) {
			for ( T entry : getFilteredData() ) {
				if ( data == entry ) {
					return index;
				}
				index++;
			}
		}
		return -1;
	}

	@Override
	public int getSize()
	{
		synchronized (entries) {
			return getFilteredData().size();
		}
	}

	@Override
	public T getValueAt(int index)
	{
		synchronized ( entries ) {
			return getFilteredData().get( index );
		}
	}

	@Override
	public void removeDataModelChangeListener(IViewDataModelChangeListener l)
	{
		if ( l == null ) {
			throw new IllegalArgumentException("l cannot be NULL");
		}

		synchronized (listeners) {
			listeners.remove( l );
		}
	}
	
	@Override
	public void valueChanged(T value)
	{
		
		if ( log.isDebugEnabled() ) {
			log.debug("valueChanged(): entity = "+value);
		}
		
		int index = getIndexFor( value );
		if ( index != -1 ) {
			fireEvent( 
				new ViewDataModelChangeEvent( value , this,index ,ViewDataModelChangeEvent.Type.ITEM_CHANGED )
			);
		}

	}

	protected void fireEvent(final ViewDataModelChangeEvent event) {

		Misc.runOnEventThread( new Runnable() {

			@Override
			public void run()
			{
				if ( log.isDebugEnabled() ) {
					log.debug("fireEvent(): event = "+event);
				}
				
				List<IViewDataModelChangeListener> copy;
				synchronized( listeners ) {
					copy = new ArrayList<IViewDataModelChangeListener>( listeners );
				}

				for ( IViewDataModelChangeListener l : copy ) {
					l.dataModelChanged( event );
				}				
			}} );
	}

	@Override
	public Iterator<T> iterator()
	{
		return getFilteredData().iterator();
	}

}