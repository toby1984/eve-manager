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
package de.codesourcery.eve.skills.calendar.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.codesourcery.eve.skills.calendar.ICalendar;
import de.codesourcery.eve.skills.calendar.ICalendarChangeListener;
import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayload;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayloadTypeFactory;
import de.codesourcery.eve.skills.calendar.ICalendarManager;
import de.codesourcery.eve.skills.util.Misc;
import de.codesourcery.eve.skills.utils.ISystemClock;
import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.utils.xml.XmlHelper;

public class DefaultCalendarManager implements InitializingBean, ICalendarManager , DisposableBean
{

	private static final Logger log = Logger.getLogger(DefaultCalendarManager.class);

	// guarded-by: "this"
	private MyCalendar calendar;

	private File inputFile;
	private ICalendarEntryPayloadTypeFactory payloadTypeFactory;

	private final List<ICalendarChangeListener> changeListener =
		new ArrayList<ICalendarChangeListener>();

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.calendar.ICalendarManager#getCalendar()
	 */
	public synchronized ICalendar getCalendar() {
		if ( calendar == null ) {
			calendar = new MyCalendar( inputFile );
		}
		return calendar;
	}

	protected enum NotificationType {
		ADDED,
		REMOVED,
		CHANGED;
	}

	public ICalendarEntryPayloadTypeFactory getPayloadTypeFactory()
	{
		return payloadTypeFactory;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.calendar.ICalendarManager#addCalendarChangeListener(de.codesourcery.eve.skills.calendar.ICalendarChangeListener)
	 */
	public void addCalendarChangeListener(ICalendarChangeListener l) {
		if ( l == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (changeListener) {
			changeListener.add( l );
		}
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.calendar.ICalendarManager#removeCalendarChangeListener(de.codesourcery.eve.skills.calendar.ICalendarChangeListener)
	 */
	public void removeCalendarChangeListener(ICalendarChangeListener l) 
	{
		if ( l == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}

		synchronized (changeListener) {
			changeListener.remove( l );
		}
	}
	
	public static final int toInteger(String s) {
		return Integer.parseInt( s );
	}
	
	public static final boolean toBoolean(String s) {
		return StringUtils.isBlank( s ) ? false : Boolean.valueOf( s );
	}

	protected void notifyListeners(ICalendar calendar, ICalendarEntry entry , NotificationType type) {

		final List<ICalendarChangeListener> copy;
		synchronized ( changeListener ) {
			copy =
				new ArrayList<ICalendarChangeListener>( this.changeListener );
		}

		for ( ICalendarChangeListener l : copy ) 
		{
			switch( type ) {
				case ADDED:
					l.entryAdded( calendar , entry );
					break;
				case REMOVED:
					l.entryRemoved( calendar , entry );
					break;
				case CHANGED:
					l.entryChanged( calendar , entry );
					break;
				default:
					throw new RuntimeException("Unhandled type "+type);
			}
		}
	}

	// =======================================

	private final class MyCalendar implements ICalendar {

		private final File inputFile;

		// guarded-by: this
		private final List<ICalendarEntry> entriesByStartDate =
			new ArrayList<ICalendarEntry> ();


		public MyCalendar(File inputFile) 
		{
			this.inputFile = inputFile;
			if ( inputFile.exists() && inputFile.length() > 0 ) {
				try {
					long time1 = -System.currentTimeMillis();
					loadData();
					time1 += System.currentTimeMillis();
					log.info("init(): Loaded "+entriesByStartDate.size()+" entries in "+time1+" milliseconds");
				}
				catch (Exception e) {
					log.error("init(): Failed to load calendar from file "+inputFile.getAbsolutePath() ,e);
					throw new RuntimeException("Failed to parse calendar file "+inputFile.getAbsolutePath() ,e);
				}
			} else {
				log.warn("init(): Calendar file "+inputFile.getAbsolutePath()+" not found/empty.");
			}
		}

		/*
		 * <calendar>
		 *   <entry start="dd-MM-yyyy HH:mm:ss" duration="seconds" type="payload type id">
		 *     <!-- contents depending on payload type id -->
		 *   </entry>
		 * </calendar>
		 */

		private final DateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		protected void loadData() throws Exception {

			log.info("loadData(): Loading calendar from "+inputFile.getAbsolutePath());

			final Document doc = XmlHelper.parseFile( inputFile );
			final Element element = XmlHelper.getElement( doc , "calendar" , true );

			final NodeList list = element.getChildNodes();
			for ( int i = 0 ; i < list.getLength() ; i++) 
			{
				final Element entry = (Element) list.item( i );
				
				final Date startDate =
					DF.parse( entry.getAttribute("start" ) );

				final Duration duration =
					Duration.seconds( Long.parseLong( entry.getAttribute("duration" ) ) );

				final int payloadType =
					Integer.parseInt( entry.getAttribute( "type" ) );

				final Element child=
					XmlHelper.getElement( entry , "reminder" , true );
				
				final boolean notified =
					toBoolean( child.getAttribute("userNotified") );
					
				final boolean notificationEnabled =
					toBoolean( child.getAttribute("notificationEnabled") );	
				
				final Duration offset;
				if ( notificationEnabled ) 
				{
					final Duration.Type type =
						Duration.Type.fromTypeId( toInteger( child.getAttribute( "offsetType" ) ) );
					final long value  =
						Long.parseLong( child.getAttribute( "offset" ) );
					
					offset = new Duration( value * type.toSeconds() );
				} else {
					offset = Duration.ZERO;
				}
				
				final ICalendarEntryPayload payload = 
					payloadTypeFactory.getPayloadType( payloadType ).parsePayload( entry );

				final ICalendarEntry newEntry = new SimpleCalendarEntry( 
						new DateRange( startDate , duration ) , payload );

				newEntry.setUserReminded( notified );
				newEntry.setReminderEnabled( notificationEnabled );
				newEntry.setReminderOffset( offset );
				
				addEntry( newEntry , false );
			}
		}

		public void store() throws Exception 
		{

			final TransformerFactory tFactory =
				TransformerFactory.newInstance();

			final Transformer copyTransformer = tFactory.newTransformer();

			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder = factory.newDocumentBuilder();

			final Document document = builder.newDocument();

			final Element rootNode = document.createElement("calendar" );
			document.appendChild( rootNode );
			synchronized( this.entriesByStartDate ) 
			{
				log.info("store(): Writing "+entriesByStartDate.size()+" calendar entries to "+inputFile.getAbsolutePath());

				for ( ICalendarEntry entry : this.entriesByStartDate ) 
				{
					final Element entryNode =
						document.createElement( "entry" );
					
					rootNode.appendChild( entryNode );
					
					entryNode.setAttribute("start" , DF.format( entry.getStartDate() ) );
					entryNode.setAttribute("duration" , ""+entry.getDateRange().getDuration().toSeconds() );
					entryNode.setAttribute("type" , ""+entry.getPayload().getType().getTypeId() );
					
					final Element node = document.createElement("reminder");
					entryNode.appendChild( node );
					
					if ( entry.isUserReminded() ) {
						node.setAttribute( "userNotified" , "true" );
					}
					
					if ( entry.isReminderEnabled() ) {
						node.setAttribute("notificationEnabled" , "true" );
					}
					
					final Duration offset = entry.getReminderOffset();
					node.setAttribute( "offsetType" , ""+Duration.Type.SECONDS.getTypeId() );
					node.setAttribute( "offset" , ""+offset.toSeconds() );

					entry.getPayload().getType().storePayload( document , entryNode , entry );
				}

				final DOMSource source = new DOMSource(document);
				final FileOutputStream outStream = new FileOutputStream( inputFile );
				try {
					final StreamResult result = new StreamResult( outStream );
					copyTransformer.transform(source, result);
				}
				finally 
				{
					try {
						outStream.close();
					} catch(Exception e) { /* ok */ }
				}


			}
		}
		
		private boolean isRemindUserOnDate(ICalendarEntry entry , ISystemClock clock)
		{
			if ( ! entry.isReminderEnabled() || entry.isUserReminded() ) {
				return false;
			}
			
			final Date today = new Date( clock.getCurrentTimeMillis() );
			
			final Date reminderDate = 
				entry.getDueDate();
			
			return DateUtils.isSameDay( reminderDate , today ) || reminderDate.compareTo( today ) <= 0;  
		}
		
		@Override
		public List<ICalendarEntry> getUnacknowledgedEntries(ISystemClock clock)
		{
			final List<ICalendarEntry> result =
				new ArrayList<ICalendarEntry>();
			
			synchronized (entriesByStartDate ) 
			{
				for ( ICalendarEntry e : entriesByStartDate ) 
				{
					final boolean isDue = isRemindUserOnDate( e , clock );
					
					if ( log.isTraceEnabled() ) {
						log.trace("getUnacknowledgedEntries(): "+e+" is due: "+isDue );
					}
					
					if ( isDue ) {
						result.add( e );
					}
				}
			}
			return result;
		}

		@Override
		public List<ICalendarEntry> getEntriesForDay(Date day)
		{
			List<ICalendarEntry> result =
				new ArrayList<ICalendarEntry>();

			final DateRange range =
				new DateRange( Misc.stripToDay( day ).getTime() , Duration.hours( 24 ) );

			synchronized( entriesByStartDate ) {
				for ( ICalendarEntry e : entriesByStartDate ) {
					if ( e.getDateRange().intersects( range ) ) {
						result.add( e );
					}
				}
			}
			return result;
		}

		@Override
		public ICalendarEntry addEntry(DateRange date, boolean reminderEnabled,
				Duration reminderOffset, ICalendarEntryPayload payload)
		{
			final SimpleCalendarEntry entry = new SimpleCalendarEntry( date , payload );
			entry.setReminderEnabled( reminderEnabled );
			entry.setReminderOffset( reminderOffset );
			return addEntry( entry , true );
		}
		
		protected ICalendarEntry addEntry(ICalendarEntry newEntry,boolean notifyListeners)
		{
			boolean added = false;
			synchronized (entriesByStartDate) 
			{
				int i = 0;
				for ( Iterator<ICalendarEntry> it = entriesByStartDate.iterator() ; it.hasNext() ; i++)
				{
					final ICalendarEntry existing = it.next();
					if ( newEntry.getStartDate().compareTo( existing.getStartDate() ) < 0 )
					{
						entriesByStartDate.add(i , newEntry );
						added = true;
						break;
					}
				}

				if ( ! added ) {
					entriesByStartDate.add( newEntry );
				}
			}

			if ( notifyListeners ) {
				notifyListeners( MyCalendar.this  , newEntry , NotificationType.ADDED );
			}
			return newEntry;
		}

		@Override
		public void deleteEntry(ICalendarEntry entry)
		{
			boolean removed = false;
			synchronized (entriesByStartDate) 
			{
				for ( Iterator<ICalendarEntry> it = entriesByStartDate.iterator() ; it.hasNext() ; )
				{
					if ( it.next() == entry ) {
						it.remove();
						removed = true;
						break;
					}
				}
			}

			if ( removed ) {
				notifyListeners( MyCalendar.this  , entry , NotificationType.REMOVED );
			}
		}

		@Override
		public void persist()
		{
			try {
				store();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void entryChanged(ICalendarEntry entry)
		{
			notifyListeners(this , entry , NotificationType.CHANGED );
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if ( inputFile == null ) {
			throw new BeanCreationException("inputFile cannot be NULL");
		}

		if ( payloadTypeFactory == null ) {
			throw new BeanCreationException("payloadTypeFactory cannot be NULL");
		}
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.calendar.ICalendarManager#setPayloadTypeFactory(de.codesourcery.eve.skills.calendar.ICalendarEntryPayloadTypeFactory)
	 */
	public void setPayloadTypeFactory(ICalendarEntryPayloadTypeFactory payloadTypeFactory)
	{
		if ( payloadTypeFactory == null ) {
			throw new IllegalArgumentException(
			"payloadTypeFactory cannot be NULL");
		}
		this.payloadTypeFactory = payloadTypeFactory;
	}

	public void setInputFile(File inputFile)
	{
		if ( inputFile == null ) {
			throw new IllegalArgumentException("inputFile cannot be NULL");
		}
		this.inputFile = inputFile;
	}

	public synchronized void persist() throws Exception 
	{
		if ( calendar != null ) {
			calendar.store();
		}
	}

	@Override
	public void destroy() throws Exception
	{
		persist();
	}

}
