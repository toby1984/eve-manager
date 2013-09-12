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
import java.util.Comparator;
import java.util.List;

import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;

public abstract class AbstractTableModel<T> implements TableModel  {

	public static final Logger log = Logger.getLogger(AbstractTableModel.class);
	
	private final List<TableModelListener> listeners =
		new ArrayList<TableModelListener>();
	
	private IViewFilter<T> viewFilter = null;
	
	protected final TableColumnBuilder columnBuilder;
	
	private TableRowSorter<AbstractTableModel<T>> rowSorter;
	
	// =============== custom rowsorter ==============

	public final class CustomRowSorter extends TableRowSorter<AbstractTableModel<T>> {

		public CustomRowSorter(AbstractTableModel<T> model) {
			super( model );
		}
		@Override
		public Comparator<?> getComparator(int column) {
			return columnBuilder.getColumn( column ).getComparator();
		}
		
	}
	
	public abstract T getRow(int modelRow);
	
	
	public final void dispose() {
		disposeHook();
	}
	
	protected void disposeHook() {
		
	}
	
	public void removeRows(int... rowIndices) {
		throw new UnsupportedOperationException("removeRows() not supported");
	}
	
	protected void notifyRowChanged(int row) {
		notifyRowsChanged(row,row);
	}
	
	protected void notifyRowsChanged(int firstRow,int lastRow) {
		
		if ( log.isTraceEnabled() ) {
			log.trace("notifyRowChanged(): firstRow = "+firstRow+" ,lastRow="+lastRow);
		}
		
		final TableModelEvent event =
			new TableModelEvent(this,firstRow,lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
		fireEvent( event );
	}
	
	protected void notifyRowRemoved(int index) {
		notifyRowsRemoved( index,index );
	}
	
	public int convertModelRowToView(int modelRow) {
		return getRowSorter().convertRowIndexToView(modelRow); 
	}
	
	public int convertViewRowToModel(int viewRow) {
		return getRowSorter().convertRowIndexToModel( viewRow );
	}

	protected void notifyRowsRemoved(int first,int last) {
		
		if ( log.isTraceEnabled() ) {
			log.trace("notifyRowsRemoved(): first="+first+",last="+last);
		}
		
		final TableModelEvent event =
			new TableModelEvent(this,first,last, TableModelEvent.ALL_COLUMNS , TableModelEvent.DELETE );
		
		fireEvent( event );
	}
	
	protected void notifyRowsInserted(int first,int last) {
		
		if ( log.isTraceEnabled() ) {
			log.trace("notifyRowsInserted(): first="+first+",last="+last);
		}
		
		final TableModelEvent event =
			new TableModelEvent(this,first,last, TableModelEvent.ALL_COLUMNS , TableModelEvent.INSERT);
		
		fireEvent( event );
	}
	
	protected void notifyRowInserted(int index) {
		notifyRowsInserted(index,index);
	}	
	
	// =============== END: custom rowsorter =========
	
	public AbstractTableModel(TableColumnBuilder columnBuilder) {
		if ( columnBuilder == null ) {
			throw new IllegalAccessError("NULL table column builder ?");
		}
		this.columnBuilder = columnBuilder;
	}
	
	public boolean hasViewFilter() {
		return this.viewFilter != null;
	}
	
	public IViewFilter<T> getViewFilter() {
		return viewFilter;
	}
	
	public TableRowSorter<AbstractTableModel<T>> getRowSorter() {
		if ( rowSorter == null ) {
			rowSorter = new CustomRowSorter( this );
		}
		return rowSorter;
	}
	
	@SuppressWarnings("unchecked")
	public <X extends IViewFilter<T>> X getViewFilter(Class<X> clasz) {
		if ( this.viewFilter == null ) {
			return null;
		}
		
		if ( this.viewFilter.getClass() == clasz ) {
			return (X) this.viewFilter;
		}
		
		return this.viewFilter.getFilter( clasz );
	}
	
	public void setViewFilter(IViewFilter<T> filter) {
		if ( this.viewFilter == filter ) {
			return;
		}
		this.viewFilter = filter;
		applyFilter();
	}
	
	public void viewFilterChanged() {
		applyFilter();
	}
	
	protected final void applyFilter() {
		
		if ( log.isDebugEnabled() ) {
			log.debug("applyFilter(): filter = "+this.viewFilter);
		}
		
		if ( this.viewFilter != null ) {
			
			final RowFilter<AbstractTableModel<T>, Integer > filter =
				new RowFilter<AbstractTableModel<T>, Integer >() {

				@Override
				public boolean include(
						javax.swing.RowFilter.Entry<? extends AbstractTableModel<T>, 
								? extends Integer> entry) 
				{
					if ( viewFilter == null ) {
						return true;
					}
					
					int modelRow = entry.getIdentifier();
					final T data = entry.getModel().getRow( modelRow );
					return ! viewFilter.isHidden( data );
				} };
				
			getRowSorter().setRowFilter( filter );

		} else {
			getRowSorter().setRowFilter( null );
		}
	}
	
	public void addViewFilter(IViewFilter<T> filter) {
		if ( this.viewFilter == null ) {
			this.viewFilter = filter;
		} else {
			this.viewFilter.addFilter( filter );
		}
		applyFilter();
	}
	
	public void removeViewFilter(IViewFilter<T> filter) {
		
		if ( filter == null ) {
			return;
		}
		
		final boolean removed;
		if ( this.viewFilter == filter ) {
			this.viewFilter = this.viewFilter.getDelegate();
			removed = true;
		} else {
			removed = this.viewFilter.removeFilter( filter );
		}
		
		if ( removed ) {
			applyFilter();
		}
	}	
	@Override
	public void addTableModelListener(TableModelListener l) {
		synchronized (listeners) {
			listeners.add( l );
		}
	}
	
	protected void fireEvent(final TableModelEvent event) {

		try {
			
			final Runnable runnable = new Runnable() {

				@Override
				public void run() {
					List<TableModelListener> copy;
					synchronized( listeners ) {
						copy = new ArrayList<TableModelListener>( listeners );
					}
					
					for ( TableModelListener l : copy ) {
						if ( log.isTraceEnabled() ) {
							log.trace("fireEvent(): Notifying "+l+" with "+event);
						}
						l.tableChanged( event );
					}				
				}
			};
			
			if ( SwingUtilities.isEventDispatchThread() ) {
				runnable.run();
			} else {
				// note: MUST be invokeAndWait() here, otherwise
				// note: expectations of callers regarding execution
				// note: order might be invalid
				SwingUtilities.invokeAndWait( runnable);
			}
		}
		catch (Exception e) {
			log.error("fireEvent(): failed to send event",e);
		}
		
	}
	
	public void modelDataChanged() {
		fireEvent(  new TableModelEvent(this) );
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return this.columnBuilder.getColumnClass( columnIndex );
	}

	@Override
	public int getColumnCount() {
		return this.columnBuilder.getColumnCount();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return this.columnBuilder.getColumnName( columnIndex );
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException("setValueAt()");
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}
	
	protected abstract Object getColumnValueAt(int modelRowIndex, int modelColumnIndex);
		
	@Override
	public final Object getValueAt(int modelRowIndex, int modelColumnIndex) {
		return getColumnValueAt( modelRowIndex , modelColumnIndex ); 
	}

}
