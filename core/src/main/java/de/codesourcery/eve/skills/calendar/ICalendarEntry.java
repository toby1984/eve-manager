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

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;

/**
 * <code>ICalendarEntry</code> implementations need to have 
 * working <code>equals()</code> / <code>hashCode</code> implementations
 * as well !!!
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface ICalendarEntry
{
	public DateRange getDateRange();
	
	public void setDateRange(DateRange dateRange);
	
	public Date getStartDate();
	
	public ICalendarEntryPayload getPayload();
	
	public void setPayload(ICalendarEntryPayload payload);
	
	public void setUserReminded(boolean userNotified);
	
	public boolean isUserReminded();
	
	/**
	 * Sets how much time in advance the user wants to be reminded.
	 * 
	 * @param notificationOffset
	 * @see #isReminderEnabled()
	 * @see #setReminderEnabled(boolean)
	 */
	public void setReminderOffset(Duration notificationOffset);
	
	public Duration getReminderOffset();

	/**
	 * 
	 * @param notificationEnabled
	 */
	public void setReminderEnabled(boolean notificationEnabled);
	
	public boolean isReminderEnabled();
	
	public Date getDueDate();
	
}
