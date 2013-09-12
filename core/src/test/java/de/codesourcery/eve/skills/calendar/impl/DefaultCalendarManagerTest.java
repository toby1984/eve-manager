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
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import de.codesourcery.eve.skills.calendar.ICalendar;
import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;

public class DefaultCalendarManagerTest extends TestCase
{
	
	private File tmpFile;
	
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		if ( tmpFile != null ) {
			tmpFile.delete();
			tmpFile = null;
		}
	}
	
	public void testStoreAndLoad() throws Exception {
		
		final DefaultCalendarEntryPayloadTypeFactory factory = new DefaultCalendarEntryPayloadTypeFactory();
		tmpFile = File.createTempFile("test123","tmp");
		
		DefaultCalendarManager manager =
			new DefaultCalendarManager();
		
		manager.setInputFile( tmpFile );
		manager.setPayloadTypeFactory( factory );
		
		final ICalendar cal = manager.getCalendar();
		
		final Date now = new Date();
		assertEquals( 0 , cal.getEntriesForDay( now ).size() );
		
		final PlaintextPayload payload =
			factory.createPlainTextPayload( "title", "notes");
		
		cal.addEntry( new DateRange( now , Duration.days( 1 ) ) , true , Duration.days(1) , payload );
		
		final List<ICalendarEntry> entries = cal.getEntriesForDay( now );
		assertNotNull( entries );
		assertEquals( 1 , entries.size() );
		
		final ICalendarEntry entry = entries.get(0);
		final PlaintextPayload payload2 =
			(PlaintextPayload) entry.getPayload();
		
		assertEquals( "title" , payload2.getSummary() );
		assertEquals( "notes" , payload2.getNotes() );
		assertTrue( entry.isReminderEnabled() );
		assertEquals( new DateRange( now , Duration.days(1) ) , entry.getDateRange() );
		assertEquals( Duration.days(1 ) , entry.getDateRange().getDuration() );
		
		manager.destroy();
		
		manager =
			new DefaultCalendarManager();
		
		manager.setInputFile( tmpFile );
		manager.setPayloadTypeFactory( factory );
		
		final List<ICalendarEntry> entries2 = manager.getCalendar().getEntriesForDay( now );
		assertNotNull( entries2 );
		assertEquals( 1 , entries2.size() );
		
		final ICalendarEntry entry2 = entries2.get(0);
		final PlaintextPayload payload3 =
			(PlaintextPayload) entry2.getPayload();
		
		assertEquals( "title" , payload3.getSummary() );
		assertEquals( "notes" , payload3.getNotes() );
		assertTrue( entry2.isReminderEnabled() );
		assertEquals( new DateRange( now , Duration.days(1) ) , entry2.getDateRange() );
		assertEquals( Duration.days(1 ) , entry2.getDateRange().getDuration() );		
	}
}
