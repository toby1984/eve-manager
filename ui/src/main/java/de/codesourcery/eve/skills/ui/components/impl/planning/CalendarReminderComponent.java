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

import java.awt.GridBagLayout;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import de.codesourcery.eve.skills.calendar.ICalendar;
import de.codesourcery.eve.skills.calendar.ICalendarChangeListener;
import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.eve.skills.calendar.ICalendarManager;
import de.codesourcery.eve.skills.calendar.impl.PlaintextPayload;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;

public class CalendarReminderComponent extends AbstractEditorComponent
{
	private final MyModel tableModel;
	private final JTable table;
	
	@Resource(name="calendar-manager")
	private ICalendarManager calendarManager;
	
	private final ICalendarChangeListener changeListener = 
		new ICalendarChangeListener() {

			@Override
			public void entryAdded(ICalendar cal, ICalendarEntry newEntry)
			{
				// no-op
			}

			@Override
			public void entryChanged(ICalendar cal, ICalendarEntry changedEntry)
			{
				tableModel.entryChanged( changedEntry );
			}

			@Override
			public void entryRemoved(ICalendar cal, ICalendarEntry deletedEntry)
			{
				// no-op				
			}};
	
	public CalendarReminderComponent(List<ICalendarEntry> unacknowledgedEntries) 
	{
		tableModel = new MyModel( unacknowledgedEntries );
		table = new JTable( tableModel );
	}

	@Override
	protected JPanel createPanelHook()
	{
		table.setFillsViewportHeight( true );
		table.setRowSorter( tableModel.getRowSorter() );
		
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		
		result.add( new JScrollPane( table ) , constraints(0,0).resizeBoth().end() );
		return result;
	}
	
	@Override
	public final void onAttachHook(IComponentCallback callback)
	{
		calendarManager.addCalendarChangeListener( changeListener );
	}
	
	@Override
	protected void onDetachHook()
	{
		super.onDetachHook();
		calendarManager.removeCalendarChangeListener( changeListener );
	}

	private final class MyModel extends AbstractTableModel<ICalendarEntry> {

		private final List<ICalendarEntry> unacknowledgedEntries;
		
		public MyModel(List<ICalendarEntry> unacknowledgedEntries) 
		{
			super( new TableColumnBuilder()
					.add( "Acknowledged?", Boolean.class)
					.add( "Due date", Date.class )
					.add( "Start date", Date.class )
					.add("End date",Date.class)
					.add("Summary") 
			);
			
			if ( unacknowledgedEntries == null || unacknowledgedEntries.isEmpty() ) 
			{
				throw new IllegalArgumentException("Entries cannot be null/empty");
			}
			this.unacknowledgedEntries = unacknowledgedEntries;
		}
		
		public void entryChanged(ICalendarEntry changedEntry)
		{
			int index = 0;
			for ( ICalendarEntry existing : unacknowledgedEntries ) {
				if ( existing.equals( changedEntry ) ) {
					unacknowledgedEntries.remove( index );
					unacknowledgedEntries.add( index , changedEntry );
					notifyRowChanged( index );
					return;
				}
				index++;
			}
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
				throw new IllegalArgumentException("Invalid column "+columnIndex);
			}
			
			final ICalendarEntry row = getRow(rowIndex);
			final boolean selected = (Boolean) value;
			if ( selected != row.isUserReminded() ) {
				row.setUserReminded( selected );
				calendarManager.getCalendar().entryChanged( row );
			}
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final ICalendarEntry row = getRow( modelRowIndex );
			switch( modelColumnIndex ) {
				case 0:
					return row.isUserReminded();
				case 1:
					return row.getDueDate();
				case 2:
					return row.getStartDate();
				case 3:
					return row.getDateRange().getEndDate();
				case 4:
					if ( row.getPayload() instanceof PlaintextPayload ) {
						return ((PlaintextPayload) row.getPayload()).getSummary();
					}
					return row.getPayload().toString();
				default:
					throw new RuntimeException("Unhandled column "+modelColumnIndex);
			}
		}

		@Override
		public ICalendarEntry getRow(int modelRow)
		{
			return unacknowledgedEntries.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return unacknowledgedEntries.size();
		}
		
	}

	@Override
	protected JButton createCancelButton()
	{
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton()
	{
		return new JButton("OK");
	}

	@Override
	protected boolean hasValidInput()
	{
		return true;
	}
}
