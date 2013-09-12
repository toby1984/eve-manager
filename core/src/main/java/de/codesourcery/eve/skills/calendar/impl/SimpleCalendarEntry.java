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

import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayload;
import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;

public class SimpleCalendarEntry implements ICalendarEntry
{
	private Duration notificationOffset = Duration.ZERO;
	private boolean notificationEnabled;
	private boolean userNotified = false;
	private DateRange dateRange;
	private ICalendarEntryPayload payload;
	
	public SimpleCalendarEntry(DateRange dateRange,ICalendarEntryPayload payload) 
	{
		if ( dateRange == null ) {
			throw new IllegalArgumentException("dateRange cannot be NULL");
		}
		if ( payload == null ) {
			throw new IllegalArgumentException("payload cannot be NULL");
		}
		this.dateRange = dateRange;
		this.payload = payload;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof SimpleCalendarEntry ) {
			final SimpleCalendarEntry o = (SimpleCalendarEntry) obj;
			return new EqualsBuilder().append( this.dateRange , o.getDateRange() )
			.append( this.payload , o.getPayload() ).isEquals();
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder().append( this.dateRange ).append( this.payload ).toHashCode();
	}
	
	public SimpleCalendarEntry()
	{
		notificationOffset = Duration.ZERO;
		notificationEnabled = false;
	}
	
	public SimpleCalendarEntry(boolean notificationEnabled , Duration notifcationOffset) 
	{
		if ( notifcationOffset == null ) {
			throw new IllegalArgumentException(
					"notifcationOffset cannot be NULL");
		}
	
		this.notificationEnabled = notificationEnabled;
		this.notificationOffset = notifcationOffset;
	}

	public void setUserReminded(boolean userNotified)
	{
		this.userNotified = userNotified;
	}
	
	public boolean isUserReminded()
	{
		return userNotified;
	}
	
	public void setReminderOffset(Duration notificationOffset)
	{
		if ( notificationOffset == null ) {
			throw new IllegalArgumentException(
					"notificationOffset cannot be NULL");
		}
		this.notificationOffset = notificationOffset;
	}
	
	public Duration getReminderOffset() {
		return notificationOffset;
	}

	public void setReminderEnabled(boolean notificationEnabled)
	{
		this.notificationEnabled = notificationEnabled;
	}
	
	public boolean isReminderEnabled()
	{
		return notificationEnabled;
	}

	@Override
	public DateRange getDateRange()
	{
		return dateRange;
	}

	@Override
	public ICalendarEntryPayload getPayload()
	{
		return this.payload;
	}

	@Override
	public Date getStartDate()
	{
		return getDateRange().getStartDate();
	}

	@Override
	public void setDateRange(DateRange dateRange)
	{
		if ( dateRange == null ) {
			throw new IllegalArgumentException("dateRange cannot be NULL");
		}
		this.dateRange = dateRange;
	}

	@Override
	public void setPayload(ICalendarEntryPayload payload)
	{
		if ( payload == null ) {
			throw new IllegalArgumentException("payload cannot be NULL");
		}
		this.payload = payload;
	}

	@Override
	public Date getDueDate()
	{
		return getReminderOffset().subtractFrom( getStartDate() );
	}

}
