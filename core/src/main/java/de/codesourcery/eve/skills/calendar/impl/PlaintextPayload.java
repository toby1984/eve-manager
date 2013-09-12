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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import de.codesourcery.eve.skills.calendar.ICalendarEntryPayload;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayloadType;

public class PlaintextPayload implements ICalendarEntryPayload
{
	private String summary;
	private String notes;
	
	public PlaintextPayload() {
	}
	
	public PlaintextPayload(String summary) {
		setSummary( summary );
	}
	
	public PlaintextPayload(String summary,String notes) {
		setSummary( summary );
		setNotes( notes );
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof PlaintextPayload) {
			final PlaintextPayload o = (PlaintextPayload) obj;
			return new EqualsBuilder().append( this.summary , o.getSummary() ).append( this.notes, o.getNotes() ).isEquals();
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder().append( this.summary ).append( this.notes ).toHashCode();
	}
	
	public void setNotes(String notes)
	{
		this.notes = notes;
	}
	
	public String getNotes()
	{
		return notes;
	}
	
	public void setSummary(String summary)
	{
		
		if ( StringUtils.isBlank(summary) ) {
			throw new IllegalArgumentException("summary cannot be blank.");
		}
		
		this.summary = summary;
	}
	public String getSummary()
	{
		return summary;
	}

	@Override
	public ICalendarEntryPayloadType getType()
	{
		return PlaintextPayloadType.INSTANCE;
	}
}
