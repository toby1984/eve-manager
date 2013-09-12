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
package de.codesourcery.eve.skills.calendar;

import java.util.Date;
import java.util.List;

import de.codesourcery.eve.skills.utils.ISystemClock;
import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;

/**
 * Implementations MUST be thread-safe.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface ICalendar
{

	/**
	 * Returns all calendar entries for a given day.
	 * @param day
	 * @return entries sorted ascending by start date
	 */
	public List<ICalendarEntry> getEntriesForDay(Date day);
	
	/**
	 * Deletes an existing entry.
	 * @param entry
	 */
	public void deleteEntry(ICalendarEntry entry);
	
	/**
	 * Adds a new entry.
	 * 
	 * @param date
	 * @param payload
	 */
	public ICalendarEntry addEntry(DateRange date , boolean reminderEnabled , Duration reminderOffset , ICalendarEntryPayload payload);
	
	public void entryChanged( ICalendarEntry entry);
	
	public void persist();

	/**
	 * <code>
	 * Returns all calendar entries that
	 * 
	 * - have the reminder enabled
	 * - have a not-NULL reminder offset
	 * - are not already flagged as 'reminded'
	 * </code>
	 * @param clock
	 * @return
	 */
	public List<ICalendarEntry> getUnacknowledgedEntries(ISystemClock clock);
}
