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
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.eve.skills.calendar.ICalendar;
import de.codesourcery.eve.skills.calendar.ICalendarChangeListener;
import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayloadTypeFactory;
import de.codesourcery.eve.skills.calendar.ICalendarManager;
import de.codesourcery.eve.skills.calendar.impl.DefaultCalendarEntryPayloadTypeFactory;
import de.codesourcery.eve.skills.calendar.impl.PlaintextPayload;
import de.codesourcery.eve.skills.calendar.impl.PlaintextPayloadType;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.utils.CalendarWidget;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.CalendarWidget.ICalendarRenderer;
import de.codesourcery.planning.Duration;

public class CalendarComponent extends AbstractComponent
{
	@Resource(name="calendar-manager")
	private ICalendarManager calendarManager;
	
	private final JButton addEntryButton = 
		new JButton("Add new entry...");
	
	private final MyTableModel tableModel =
		new MyTableModel();
	
	@Resource(name="payloadtype-factory")
	private ICalendarEntryPayloadTypeFactory payloadTypeFactory;
	
	private final JTable table = new JTable( tableModel );
	private final CalendarWidget calendarWidget;
	
	private final ICalendarRenderer renderer = new ICalendarRenderer() {

		private final DateFormat DF =
			DateFormat.getDateInstance( DateFormat.SHORT );
		
		@Override
		public String getDateLabel(Date date)
		{
			return DF.format( date );
		}

		@Override
		public String getText(Date date)
		{
			final StringBuffer result = new StringBuffer();
			
			for ( Iterator<ICalendarEntry> it = getCalendar().getEntriesForDay( date ).iterator(); it.hasNext() ; ) 
			{
				final ICalendarEntry entry = it.next();
				final String text;
				if ( entry.getPayload().getType().equals( PlaintextPayloadType.INSTANCE ) ) {
					text = ((PlaintextPayload) entry.getPayload() ).getSummary();
				} else {
					text = entry.getPayload().toString();
				}
				
				result.append( text);
				
				if ( it.hasNext() ) {
					result.append( "\n" );
				}
			}
			return result.toString();
		}

		@Override
		public String getToolTip(Date date)
		{
			return null;
		}

		@Override
		public Color getTextColor(Date date)
		{
			return null;
		}
	};
	
	private final ICalendarChangeListener calendarListener =
		new ICalendarChangeListener() {

			@Override
			public void entryAdded(ICalendar cal, ICalendarEntry newEntry)
			{
				calendarWidget.repaint( newEntry.getDateRange() );
				tableModel.refresh();
			}

			@Override
			public void entryChanged(ICalendar cal, ICalendarEntry changedEntry)
			{
				// need to repaint whole view area
				// since the entry's start date might've changed to
				// the entry's duration got shorter
				calendarWidget.repaintAll();
				tableModel.refresh();
			}

			@Override
			public void entryRemoved(ICalendar cal, ICalendarEntry deletedEntry)
			{
				calendarWidget.repaint( deletedEntry.getDateRange() );
				tableModel.refresh();
			}
	};
	
	public CalendarComponent() {
		super();
		calendarWidget =
			new CalendarWidget( new Date() , renderer );
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		calendarManager.addCalendarChangeListener( calendarListener );
		tableModel.refresh();
	}
	
	@Override
	protected void disposeHook()
	{
		calendarManager.removeCalendarChangeListener( calendarListener );
	}
	
	protected ICalendar getCalendar() 
	{
		return calendarManager.getCalendar();
	}
	
	protected DefaultCalendarEntryPayloadTypeFactory getPayloadTypeFactory() {
		return ( DefaultCalendarEntryPayloadTypeFactory ) this.payloadTypeFactory;
	}
	
	@Override
	protected JPanel createPanel()
	{
		
		addEntryButton.setEnabled( false );
		addEntryButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				addNewCalendarEntry();
			}
			
		} );
		
		calendarWidget.addSelectionListener( new ISelectionListener<Date>() {

			@Override
			public void selectionChanged(Date selected)
			{
				addEntryButton.setEnabled( selected != null );
				tableModel.refresh();
			}
		} );
		
		table.addMouseListener( new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if ( e.getClickCount() != 2 || e.isPopupTrigger() ) {
					return;
				}
				
				final int viewRow = table.rowAtPoint( e.getPoint() );
				if ( viewRow != -1 ) {
					editCalendarEntry( tableModel.getRow( table.convertRowIndexToModel( viewRow ) ) );
				}
			}
		} );
		
		table.setFillsViewportHeight( true );
		table.setRowSorter( tableModel.getRowSorter() );
		table.getSelectionModel().setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		
		final PopupMenuBuilder menuBuilder =
			new PopupMenuBuilder();
		
		menuBuilder.addItem( "Remove" , new AbstractAction() {

			@Override
			public boolean isEnabled()
			{
				return ! ArrayUtils.isEmpty( table.getSelectedRows() );
			}
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final int[] viewRows = table.getSelectedRows();
				if ( ArrayUtils.isEmpty(viewRows ) ) {
					return;
				}
				
				final int[] modelRows = new int[ viewRows.length ];
				int i = 0;
				for ( int viewRow : viewRows ) {
					modelRows[i++] = table.convertRowIndexToModel( viewRow );
				}
				
				final List<ICalendarEntry> removedEntries =
					new ArrayList<ICalendarEntry> ();
				
				for ( int modelRow : modelRows ) {
					removedEntries.add( tableModel.getRow( modelRow ) );
				}
				
				for ( ICalendarEntry toBeRemoved : removedEntries ) {
					calendarManager.getCalendar().deleteEntry( toBeRemoved );
				}
			}
		} );
		
		menuBuilder.addItem ("Edit..." , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				final int modelRow = table.convertRowIndexToModel( table.getSelectedRows()[0] );
				editCalendarEntry( tableModel.getRow( modelRow ) );
			}
			
			@Override
			public boolean isEnabled()
			{
				return ! ArrayUtils.isEmpty( table.getSelectedRows() );
			}
		} );
		
		menuBuilder.attach( table );
		
		// controls panel
		final JPanel controlsPanel = 
			new JPanel();
		
		controlsPanel.setLayout( new GridBagLayout() );
		controlsPanel.add( addEntryButton , constraints(0,0).noResizing().end() );
		
		// combine panels
		final JPanel subPanel = 
			new JPanel();
		
		subPanel.setLayout( new GridBagLayout() );
		
		subPanel.add( controlsPanel , constraints(0,0).useRelativeWidth().weightY(0).end() );
		subPanel.add( new JScrollPane( table ) , constraints(0,1).useRelativeWidth().useRemainingHeight().end() );
		
		final ImprovedSplitPane splitPane =
			new ImprovedSplitPane(JSplitPane.HORIZONTAL_SPLIT ,
					calendarWidget , subPanel );

		splitPane.setDividerLocation( 0.7 );
		
		final JPanel result = 
			new JPanel();
		result.setLayout( new GridBagLayout()  );
		
		result.add( splitPane , constraints(0,0).resizeBoth().end() );

		return result;
	}
	
	protected void addNewCalendarEntry()
	{
		final CalendarEntryEditorComponent comp = 
			new CalendarEntryEditorComponent("Create new calendar entry" ,
					calendarWidget.getSelectedDate() );
		
		comp.setModal( true );
		ComponentWrapper.wrapComponent( comp ).setVisible( true );
		
		if ( ! comp.wasCancelled() ) 
		{
			final boolean userNotificationEnabled = comp.isUserNotificationEnabled();
			final Duration notificationOffset =
				userNotificationEnabled ? comp.getReminderOffset() : Duration.ZERO;
			
				final PlaintextPayload payload = getPayloadTypeFactory().createPlainTextPayload( 
						comp.getSummary(),  
						comp.getNotes() );
						
			calendarManager.getCalendar().addEntry( comp.getDateRange(), userNotificationEnabled , notificationOffset , payload );
		}		
	}
	
	protected void editCalendarEntry(ICalendarEntry entry)
	{
		final CalendarEntryEditorComponent comp = 
			new CalendarEntryEditorComponent("Edit calendar entry" ,
					calendarWidget.getSelectedDate() );
		
		comp.setModal( true );
		comp.populateFromEntry( entry );
		
		ComponentWrapper.wrapComponent( comp ).setVisible( true );
		
		if ( ! comp.wasCancelled() && comp.saveDataToEntry( entry ) ) 
		{
			calendarManager.getCalendar().entryChanged( entry );
		}		
	}

	protected String getNotesFor(ICalendarEntry entry) {
		if ( entry.getPayload() instanceof PlaintextPayload ) {
			return ((PlaintextPayload ) entry.getPayload() ).getSummary();
		}
		return entry.getPayload().toString();
	}

	private final class MyTableModel extends AbstractTableModel<ICalendarEntry> {

		private List<ICalendarEntry> data =
			new ArrayList<ICalendarEntry>();
		
		public MyTableModel() {
			super(
				new TableColumnBuilder()
				.add("Start" , Date.class )
				.add("End" , Date.class )
				.add("Summary" , String.class ) 
			);
		}
		
		public void refresh() {
			
			final Date selected =
				calendarWidget.getSelectedDate();
			
			if ( selected == null ) {
				data.clear();
			} else {
				data = calendarManager.getCalendar().getEntriesForDay( selected );
			}
			modelDataChanged();
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final ICalendarEntry row = getRow(modelRowIndex);
			
			switch( modelColumnIndex ) {
				case 0:
					return row.getStartDate();
				case 1:
					return row.getDateRange().getEndDate();
				case 2:
					return getNotesFor( row );
				default:
						throw new IllegalArgumentException("Invalid column "+modelColumnIndex);
			}
		}

		@Override
		public ICalendarEntry getRow(int modelRow)
		{
			return data.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}
		
	}
}
