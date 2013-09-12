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
package de.codesourcery.eve.skills.ui.utils;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.planning.DateRange;

public class CalendarWidget extends JPanel
{
	public final SelectionListenerHelper<Date> selectionListener = new SelectionListenerHelper<Date>();
	
	private final ICalendarRenderer renderer;
	private Calendar startDate;
	private final MyButton[][] buttons = new MyButton[4][7];
	
	private volatile Date selectedDate;
	
	public interface ICalendarRenderer 
	{
		
		public String getToolTip(Date date);
		
		public String getDateLabel(Date date);
		
		public String getText(Date date);
		
		/**
		 * 
		 * @param date
		 * @return text color to use or <code>null</code> to use
		 * default color
		 */
		public Color getTextColor(Date date);
	}
	
	public CalendarWidget(Date startDate , ICalendarRenderer renderer) 
	{
		super.setLayout( new GridBagLayout() );
		
		if ( renderer == null ) {
			throw new IllegalArgumentException("renderer cannot be NULL");
		}
		
		setStartDate(startDate ,false );
		
		this.startDate = calcAlignedStartDate( de.codesourcery.eve.skills.util.Misc.stripToDay( startDate ) );
		this.renderer = renderer; 
		setup();
	}
	
	public void setStartDate(Date date) {
		setStartDate( date , true );
	}
	
	public void addSelectionListener(ISelectionListener<Date> l) {
		this.selectionListener.addSelectionListener( l );
	}
	
	public void removeSelectionListener(ISelectionListener<Date> l) {
		this.selectionListener.removeSelectionListener( l );
	}
	
	public Date getSelectedDate()
	{
		return selectedDate;
	}
	
	protected void setStartDate(Date date,boolean refresh) 
	{
		if ( date == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		
		this.startDate = calcAlignedStartDate( de.codesourcery.eve.skills.util.Misc.stripToDay( date ) );
		if ( refresh ) 
		{
			repaintButtons();
		}
	}

	private void repaintButtons()
	{
		for ( MyButton button : buttons() )
		{
			if ( button != null ) {
				button.redraw();
			}
		}
	}
	
	private Iterable<MyButton> buttons() {
		return new Iterable<MyButton>() {

			@Override
			public Iterator<MyButton> iterator()
			{
				return buttonIterator();
			}};
	}
	
	private Iterator<MyButton> buttonIterator() {
		
		return new Iterator<MyButton>() {

			private int x = 0;
			private int y = 0;
			
			@Override
			public boolean hasNext()
			{
				return y < 4;
			}

			@Override
			public MyButton next()
			{
				
				if ( ! hasNext() ) {
					throw new NoSuchElementException();
				}
				
				final MyButton result = buttons[y][x];
				
				x++;
				if ( x == 7 ) {
					x = 0 ;
					y++;
				}
				return result;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("remove()");
			}};
	}
	
	private Calendar calcAlignedStartDate(Calendar cal)
	{
		while ( cal.get( Calendar.DAY_OF_WEEK ) != Calendar.MONDAY ) {
			cal.add( Calendar.DAY_OF_MONTH , -1 );
		}
		return cal;
	}
	
	protected final class MyButton extends JToggleButton implements ActionListener {
		
		private final int x;
		private final int y;
		
		public MyButton(int x,int y) {
			this.x = x;
			this.y = y;
			addActionListener( this );
			redraw();
		}
		
		public Date getDate() {
			return getDateFor( x, y );
		}
		
		private String toHex(int i) {
			return StringUtils.leftPad( Integer.toHexString( i ) , 2 , '0' );
		}
		
		private String toHTMLColor(Color color) {
			return toHex( color.getRed() )+toHex( color.getGreen() )+toHex( color.getBlue() );
		}
		
		public void redraw() {
			
			final Date date = getDate();
			
			final StringBuffer text = new StringBuffer("<HTML><BODY>");
			
			text.append("<B>");
			text.append( renderer.getDateLabel( date ) );
			text.append("</B>");
			text.append( "<BR><BR>" );
			
			final String htmlLabel = renderer.getText( date ).replaceAll("\n" , "<BR>" );
			final Color textColor = renderer.getTextColor( date );
			if ( textColor != null ) {
				text.append("<font color=#"+toHTMLColor( textColor)+">");
				text.append( htmlLabel );
				text.append("</font>");
			} else {
				text.append( htmlLabel );
			}
			text.append("</BODY></HTML>");
			
			setText( text.toString() );
			
			setToolTipText( renderer.getToolTip( date ) );
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if ( ! isSelected() ) {
				setSelected(true);
				return;
			}
			final Date d = getDate();
			selectedDate = d;
			// deselect all other buttons
			for ( MyButton b : buttons() )
			{
				if ( b == this ) {
					continue;
				}
				if ( b.isSelected() ) {
					b.setSelected( false );
				}
			}
			selectionListener.selectionChanged( selectedDate );
		}
	}
	
	/**
	 * Returns the last day displayed by the widget.
	 * @return
	 */
	public Calendar getEndDate() 
	{
		final Calendar result =
			Calendar.getInstance();
		result.setTime( startDate.getTime() );
		result.add( Calendar.DAY_OF_MONTH , 28 );
		return result;
	}
	
	public void refreshDateLabel( Date date ) 
	{
		final Calendar stripped = de.codesourcery.eve.skills.util.Misc.stripToDay( date );
		if ( stripped.compareTo( startDate ) < 0 ) {
			throw new IllegalArgumentException("Date "+stripped+" is before start date "+startDate.getTime() );
		}
		if ( stripped.compareTo( getEndDate() ) > 0 ) {
			throw new IllegalArgumentException("Date "+stripped+" is after end date "+getEndDate() );
		}
		
		final long MILLIS_PER_DAY = 
			1000 * 60 * 60 * 24;
		
		final int deltaInDays = (int) ( ( stripped.getTimeInMillis() - startDate.getTimeInMillis() ) / MILLIS_PER_DAY );
		final int y = (int) ( deltaInDays / 7.0);
		final int x = deltaInDays - ( y * 7 );
		
		buttons[y][x].redraw();
	}
	
	protected Date getDateFor(int x,int y) {
		Calendar cal = Calendar.getInstance();
		cal.setTime( this.startDate.getTime() );
		cal.add( Calendar.DAY_OF_MONTH , ( y *7) + x );
		return cal.getTime();
	}
	
	public static void main(String[] args) throws Exception
	{
		
		final SimpleDateFormat DF = new SimpleDateFormat("dd.MM");
		
		final Calendar specialDate = Calendar.getInstance();
		specialDate.add( Calendar.DAY_OF_MONTH , 5 );
		
		final AtomicBoolean doStuff = new AtomicBoolean(false);
		
		final ICalendarRenderer renderer = new ICalendarRenderer() {

			@Override
			public String getDateLabel(Date date)
			{
				return DF.format( date );
			}

			@Override
			public String getText(Date date)
			{
				if ( DateUtils.isSameDay( date , specialDate.getTime() ) && doStuff.get() ) 
				{
					return "SPECIAL !!!";
				} 
				return "some\nmultiline\ntext";
			}

			@Override
			public String getToolTip(Date date)
			{
				return getText( date );
			}

			@Override
			public Color getTextColor(Date date)
			{
				return Color.RED;
			}
		};
		
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().setLayout( new GridBagLayout() );
		
		final CalendarWidget widget = new CalendarWidget( new Date() , renderer );

		widget.addSelectionListener( new ISelectionListener<Date>() {

			@Override
			public void selectionChanged(Date selected)
			{
				System.out.println("Selected date > "+selected);
			}
		});
		frame.getContentPane().add( 
				widget,
				new ConstraintsBuilder().end() );
		frame.pack();
		frame.setVisible( true );

		java.lang.Thread.sleep( 2 * 1000 );
		doStuff.set( true );
		widget.refreshDateLabel( specialDate.getTime() );
		
	} 
	
	private String getWeekDayName(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime( date );
		return calendar.getDisplayName(Calendar.DAY_OF_WEEK,
				Calendar.LONG , Locale.getDefault() );
	}
	
	protected void setup() {
		
		// draw heading
		for ( int x = 0 ; x < 7 ; x++ ) 
		{
			final String day = getWeekDayName( getDateFor( x, 0 ) );
			final JLabel label = new JLabel( day , JLabel.CENTER );
			label.setBorder( BorderFactory.createBevelBorder( BevelBorder.LOWERED) );
			label.setOpaque( true );
			add( label , new ConstraintsBuilder( x , 0 ).resizeHorizontally().weightY(0).end() );
		}

		// draw 4x7 buttons
		for ( int y = 0 ; y < 4 ; y++ ) 
		{
			for ( int x = 0 ; x < 7 ; x++ ) 
			{
				buttons[y][x] = new MyButton( x , y );
				add( buttons[y][x] , new ConstraintsBuilder( x , 1+y ).end() );
			}
		}
	}

	public void repaintAll() 
	{
		for ( MyButton button : buttons() ) 
		{
			button.redraw();
		}
	}
	
	public void repaint(DateRange dateRange)
	{
		for ( MyButton button : buttons() ) 
		{
			if ( dateRange.contains( button.getDate() ) ) 
			{
				button.redraw();
			}
		}
	}
}
