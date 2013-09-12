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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.eve.skills.calendar.impl.PlaintextPayload;
import de.codesourcery.eve.skills.calendar.impl.PlaintextPayloadType;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.Cell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.HorizontalGroup;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.VerticalGroup;
import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.Duration.Type;

public class CalendarEntryEditorComponent extends AbstractEditorComponent
{
	private final Date startDate;

	private final JTextField startDateField =
		new JTextField();

	private final JTextField summaryTextField =
		new JTextField("");

	private final JTextArea notes =
		new JTextArea( 5 , 35 );

	/*
	 * Duration stuff
	 */
	private final JTextField durationTextField =
		new JTextField("1");

	private DefaultComboBoxModel<Duration.Type> typeModel =
		new DefaultComboBoxModel<Duration.Type>( Duration.Type.values() );

	private final JComboBox durationType =
		new JComboBox( typeModel );	

	/*
	 * Notification stuff
	 */
	private JCheckBox reminderEnabled = new JCheckBox("Reminder enabled?");

	private final JTextField reminderOffsetTextField =
		new JTextField("0");

	private DefaultComboBoxModel<Duration.Type> reminderOffsetTypeModel =
		new DefaultComboBoxModel<Duration.Type>( Duration.Type.values() );

	private final JComboBox reminderOffsetType =
		new JComboBox( reminderOffsetTypeModel );	
	
	private final ActionListener closeOnEnterListener =
		new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				if ( hasValidInput() ) {
					getComponentCallback().dispose( CalendarEntryEditorComponent.this );
				}				
			}
	};

	/**
	 * Create instance.
	 * 
	 * @param startDate
	 */
	public CalendarEntryEditorComponent(String title , Date startDate) {
		super( title );
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		this.startDate = startDate;
	}
	
	/**
	 * Sync existing entry with data from this component.
	 * 
	 * @param entry
	 * @return <code>true</code> if the entry was changed by this method
	 */
	public boolean saveDataToEntry(ICalendarEntry entry) 
	{
		
		if ( ! entry.getPayload().getType().equals( PlaintextPayloadType.INSTANCE ) ) {
			throw new RuntimeException("Internal error - can only edit " +
					"plaintext calendar entries, this entry is of type "+
					entry.getPayload().getType() );
		}
		
		if ( ! hasValidInput() ) {
			throw new IllegalStateException("Internal error - won't update calendar entry with invalid form data");
		}
		
		final PlaintextPayload payload = (PlaintextPayload) entry.getPayload();
		
		boolean entryUpdated = false;
		if ( ! StringUtils.equals( payload.getSummary() , this.summaryTextField.getText() ) ) {
			payload.setSummary( this.summaryTextField.getText() );
			entryUpdated = true;
		}
		
		if ( ! StringUtils.equals( payload.getNotes() , this.notes.getText() ) ) {
			payload.setNotes( this.notes.getText() );
			entryUpdated = true;
		}
		
		final Duration duration =
			toDuration( this.durationTextField , this.durationType );
		
		/*
		 * TODO: Make start date editable as well ?
		 */
		
		if ( ! ObjectUtils.equals( duration , entry.getDateRange().getDuration() ) ) {
			final DateRange newDateRange = new DateRange( entry.getStartDate() , duration );
			entry.setDateRange( newDateRange );
			entryUpdated = true;
		}
		
		if ( entry.isReminderEnabled() != this.reminderEnabled.isSelected() ) {
			entryUpdated = true;
			entry.setReminderEnabled( this.reminderEnabled.isSelected() );
		}
		
		final Duration reminderOffset =
			toDuration( this.reminderOffsetTextField , this.reminderOffsetType );
		
		if ( ! ObjectUtils.equals( reminderOffset ,  entry.getReminderOffset() ) ) 
		{
			entry.setReminderOffset( reminderOffset );
			entryUpdated = true;
		}
		
		return entryUpdated;
	}
	
	private Duration toDuration(JTextField textField , JComboBox box) 
	{
		final Duration.Type type = (Duration.Type) box.getSelectedItem();
		final long value = Long.parseLong( textField.getText() );
		return new Duration( type.toSeconds() * value );
	}
	
	public void populateFromEntry(ICalendarEntry entry) 
	{
		this.startDateField.setText( 
				DateFormat.getDateInstance(DateFormat.LONG).format( entry.getStartDate() ) 
		);
		
		if ( ! entry.getPayload().getType().equals( PlaintextPayloadType.INSTANCE ) ) {
			throw new RuntimeException("Internal error - can only edit " +
					"plaintext calendar entries, this entry is of type "+
					entry.getPayload().getType() );
		}
		
		final PlaintextPayload payload = (PlaintextPayload) entry.getPayload();
		
		this.summaryTextField.setText( payload.getSummary() );
		this.notes.setText( payload.getNotes() );
		
		final Duration.Type longestDurationType = entry.getDateRange().getDuration().getLargestMatchingType();
		this.durationType.setSelectedItem( longestDurationType );
		
		final long amount = Math.max( entry.getDateRange().getDuration().roundTo( longestDurationType ) , 1 );
		this.durationTextField.setText( ""+amount );
		
		this.reminderEnabled.setSelected( entry.isReminderEnabled() );
		
		if ( entry.isReminderEnabled() ) {
			final Type durationType = entry.getReminderOffset().getLargestMatchingType();
			this.reminderOffsetType.setSelectedItem( durationType );
			this.reminderOffsetTextField.setText( "" + entry.getReminderOffset().roundTo( durationType ) );
		} else {
			this.reminderOffsetType.setSelectedItem( Duration.Type.DAYS );
			this.reminderOffsetTextField.setText( "1" );
		}
	}
	
	public CalendarEntryEditorComponent(Date startDate) {
		super();
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		this.startDate = startDate;
	}

	private String format(Date date) {
		return DateFormat.getDateTimeInstance().format( date );
	}

	public Date getStartDate() {
		return startDate;
	}

	public String getNotes() {
		final String text = notes.getText();
		return text != null ? text : ""; 
	}

	public DateRange getDateRange() {
		return new DateRange( getStartDate() , getDuration() );
	}

	public static Duration toDuration(int value,Duration.Type type) {
		return new Duration( value * type.toSeconds() );
	}

	public Duration getDuration() {
		return toDuration(
				Integer.parseInt( durationTextField.getText() ) , 
				((Duration.Type) durationType.getSelectedItem() ) 
		);
	}
	
	@Override
	protected JPanel createPanelHook()
	{
		startDateField.setText( format( startDate ) );
		startDateField.setEditable( false );

		startDateField.setColumns( 13 );
		durationTextField.setColumns( 13 );

		notes.setLineWrap( true );
		notes.setWrapStyleWord( true );

		durationTextField.setColumns( 6 );
		
		summaryTextField.addActionListener( closeOnEnterListener );
		durationTextField.addActionListener( closeOnEnterListener );
		reminderOffsetTextField.addActionListener( closeOnEnterListener );

		final DefaultListCellRenderer durationTypeRenderer = new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				setText( ((Duration.Type) value).getDisplayName() );
				return this;
			}
		};
		durationType.setRenderer( durationTypeRenderer );
		reminderOffsetType.setRenderer( durationTypeRenderer );

		durationType.setSelectedItem( Duration.Type.DAYS );
		reminderOffsetType.setSelectedItem( Duration.Type.DAYS );

		reminderEnabled.setEnabled( true );
		reminderOffsetTextField.setEnabled( false );
		reminderOffsetType.setEnabled( false );

		linkComponentEnabledStates( reminderEnabled , reminderOffsetTextField , reminderOffsetType );

		// do layout
		final JPanel result =
			new JPanel();

		result.setLayout( new GridBagLayout() );

		new GridLayoutBuilder().add( 
				new VerticalGroup(
						new HorizontalGroup( new Cell( new JLabel("Start date") ) , new Cell( startDateField ) ),
						new HorizontalGroup( new Cell( new JLabel("Summary") ) , new Cell( summaryTextField ) ),
						new HorizontalGroup( new Cell( new JLabel("Duration") ) , new Cell( durationTextField ) , new Cell( durationType ) ),
						new HorizontalGroup( new Cell( reminderEnabled ) ),
						new HorizontalGroup( new Cell( new JLabel("Reminder offset") ) , new Cell( reminderOffsetTextField ) , new Cell( reminderOffsetType ) ),
						new HorizontalGroup( new Cell( new JScrollPane( notes ) ) )
				)
		).addTo( result );

		return result;
	}

	@Override
	protected JButton createCancelButton()
	{
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton()
	{
		return new JButton("Ok");
	}

	@Override
	protected boolean hasValidInput()
	{
		if ( StringUtils.isBlank( durationTextField.getText() ) ) {
			displayError("Duration cannot be empty" );
			return false;
		}
		
		if ( ! isAPositiveNumber( durationTextField.getText() ) ) {
			displayError("Duration must be a number >= 0" );
			return false;
		}
		
		if ( ! isAPositiveNumber( this.reminderOffsetTextField.getText() ) ) {
			displayError("Reminder offset must be a number >= 0" );
			return false;
		}
		
		if ( StringUtils.isBlank( summaryTextField.getText() ) ) {
			displayError("Summary cannot be empty.");
			return false;
		}
		
		return true;
	}
	
	protected boolean isAPositiveNumber(String s) {
		try {
			final int number = Integer.parseInt( s );
			return number >= 0;
		} catch(Exception e) {
			return false;
		}
	}

	public String getSummary()
	{
		return summaryTextField.getText();
	}

	public Duration getReminderOffset() {
		return toDuration( 
				Integer.parseInt( reminderOffsetTextField.getText() ) ,
				(Type) reminderOffsetType.getSelectedItem() 
		);
	}

	public boolean isUserNotificationEnabled()
	{
		return reminderEnabled.isSelected();
	}

}
