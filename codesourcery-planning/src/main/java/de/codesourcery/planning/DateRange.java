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
package de.codesourcery.planning;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Immutable date range.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class DateRange
{
	
	private final Date startDate;
	private final Date endDate;
	private final Duration duration;
	
	public DateRange(Date startDate, Duration duration) {
		this( startDate , duration.addTo( startDate ) );
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof DateRange) {
			final DateRange other = (DateRange) obj; 
			final long sec1 = getSeconds( this.startDate);
			final long sec2 = getSeconds( other.startDate );
			
			return sec1 == sec2 &&
			  this.duration.equals( other.duration );
		}
		return false;
	}
	
	private long getSeconds(Date date) {
		return date.getTime() / 1000;
	}
	
	@Override
	public int hashCode()
	{
		return (int) ( 31*getSeconds( startDate )+31+getSeconds( endDate )*7 );
	}
	
	public DateRange(Date startDate, Date endDate) {
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		if ( endDate == null ) {
			throw new IllegalArgumentException("endDate cannot be NULL");
		}
		
		if ( startDate.compareTo( endDate ) > 0 ) {
			throw new IllegalArgumentException("start date must be <= end date");
		}
		this.startDate = startDate;
		this.endDate = endDate;
		this.duration = new Duration(startDate,endDate);
	}
	
	public boolean contains(Date d) {
		return startDate.compareTo( d ) <= 0 &&
		       d.compareTo( getEndDate() ) <= 0;
	}
	
	protected static boolean isBeforeOrOn(Date toCheck, Date other) {
		return toCheck.compareTo( other ) <= 0;
	}
	
	protected static boolean isAfterOrOn(Date toCheck, Date other) {
		return toCheck.compareTo( other ) >= 0;
	}
	
	public Date getStartDate()
	{
		return startDate;
	}
	
	public Date getEndDate()
	{
		return endDate;
	}
	
	public boolean intersects(DateRange range) {
		boolean result= this.contains( range.getStartDate() ) ||
			 this.contains( range.getEndDate() ) ||
			 range.contains( getStartDate() ) ||
			 range.contains( getEndDate() );
		
		if ( result ) {
			
			// special case: do not report intersection
			// on ajacent date ranges
			if ( this.getStartDate().equals( range.getEndDate() ) ||  
				 this.getEndDate().equals( range.getStartDate() ) ) 
			{
				return false;
			}
				 
		}
		return result;
	}
	
	public Duration getDuration() {
		return duration;
	}
	
	public static DateRange forDay(Date date ) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime( date );
		cal.set( Calendar.HOUR_OF_DAY , 0 );
		cal.set( Calendar.MINUTE , 0);
		cal.set( Calendar.SECOND , 0 );
		cal.set( Calendar.MILLISECOND , 0 );
		return new DateRange( cal.getTime() , Duration.oneDay().addTo( cal.getTime() ) );
	}
	
	@Override
	public String toString()
	{
		return this.startDate+" - "+this.endDate;
	}
	
	public String toString(DateFormat dateFormat)
	{
		return dateFormat.format( this.startDate )+" - "+dateFormat.format( this.endDate );
	}
}
