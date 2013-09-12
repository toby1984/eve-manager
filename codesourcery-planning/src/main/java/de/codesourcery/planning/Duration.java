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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Handy immutable (!) class for dealing with durations.
 * 
 * Be careful when using {@link #add(Duration)} and the like,
 * these methods all return a new <code>Duraton</code> instance
 * holding the result of the operation.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class Duration implements Comparable<Duration>
{
	private final long seconds;
	
	/**
	 * A duration of length 0.
	 */
	public static final Duration ZERO = new Duration(0);
	
	public static final Duration UNKNOWN = UnknownDuration.INSTANCE;
	
	public enum Type {
		SECONDS(1,"second",1, "Seconds"),
		MINUTES(2,"minute",60, "Minutes"),
		HOURS(3,"hour",60 * 60, "Hours"),
		DAYS(4,"day",24 * 60*60, "Days" ),
		WEEKS(5,"week",7 * 24 *60*60, "Weeks"),
		MONTHS(6,"month",4 * 7 * 24 * 60*60, "Months"),
		YEARS(7,"year",365 * 24 * 60 * 60, "Years");
		
		private final int typeId;
		private final String displayName;
		private final long inSeconds;
		private final String name;
		
		private Type(int typeId , String name , long inSeconds,String displayName) {
			this.typeId = typeId;
			this.inSeconds = inSeconds;
			this.name = name;
			this.displayName = displayName;
		}
		
		public static Type fromTypeId(int typeId) {
			for ( Type t : values() ) {
				if ( t.getTypeId() == typeId ) {
					return t;
				}
			}
			throw new IllegalArgumentException("Unknown duration type ID "+typeId);
		}
		
		public int getTypeId()
		{
			return typeId;
		}
		
		public long toSeconds() {
			return inSeconds;
		}
		
		protected String getName(int count) {
			return count <= 1 ? name : name+"s";
		}
		
		public String getDisplayName() {
			return displayName;
		}
		
		@Override
		public String toString() { return name; }
	}
	
	private static final Type[] TYPES_SORTED_DESCENDING =  
		{ Type.YEARS,Type.MONTHS,Type.WEEKS,Type.DAYS,Type.HOURS,Type.MINUTES,Type.SECONDS }; 
	
	public Duration(long seconds) {
		if ( seconds < 0 ) {
			throw new IllegalArgumentException("Duration must not be negative");
		}
		this.seconds = seconds;
	}
	
	public long roundTo(Duration.Type type) {
		return (long) Math.floor( this.seconds / type.toSeconds() );
	}
	
	/**
	 * Returns the Duration type that best fits
	 * this duration.
	 * 
	 * <pre>
	 * The 'best fit' is longest duration type
	 * that still matches this duration.
	 * 
	 * A duration of '1 day,12 hours,10 seconds'
	 * will return {@link Type#DAYS} where while 
	 * a duration of '4 months , 19 days , 10 minutes'
	 * will return {@link Type#MONTHS} etc. 
	 * </pre>
	 * @return
	 */
	public Duration.Type getLargestMatchingType() 
	{
		Duration.Type result = Duration.Type.SECONDS;
		for ( Duration.Type type : Duration.Type.values() ) 
		{
			if ( type.toSeconds() > this.seconds ) {
				break;
			}
			result = type;
		}
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof Duration ) {
			final Duration d= (Duration) obj;
			if ( d.isUnknown() ) {
				return false;
			}
			return toSeconds() == d.toSeconds();
		}
		return false;
	}
	
	public static Duration oneSecond() {
		return seconds(1);
	}
	
	public static Duration oneDay() {
		return days(1);
	}
	
	public Duration roundToDuration(Type t) {
		final double remainder = this.seconds / (double) t.toSeconds();
		return new Duration( Math.round( remainder ) * t.toSeconds() );
	}
	
	public Duration truncateTo(Type t) {
		final double remainder = this.seconds / (double) t.toSeconds();
		return new Duration( (long) Math.floor( remainder ) * t.toSeconds() );
	}
	
	public static Duration oneWeek() {
		return weeks(1);
	}
	
	public static Duration oneMonth() {
		return months(1);
	}
	
	public static Duration oneYear() {
		return years(1);
	}
	
	/**
	 * Add a duration to a given date.
	 * 
	 * @param startDate
	 * @param duration
	 * @return
	 */
	public static Date add(Date startDate, Duration duration) {
		
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		if ( duration == null ) {
			throw new IllegalArgumentException("duration cannot be NULL");
		}
		
		if ( duration.isUnknown() ) {
			throw new IllegalArgumentException("Cannot add an unknown duration to some date");
		}
		return new Date( startDate.getTime() + ( duration.seconds * 1000 ) );
	}
	
	public boolean isUnknown() {
		return false;
	}
	
	/**
	 * Creates a duration from a <code>Date</code> range.
	 * @param start
	 * @param end
	 */
	public Duration(Date start,Date end) {
		if ( start == null ) {
			throw new IllegalArgumentException("start cannot be NULL");
		}
		if ( end == null ) {
			throw new IllegalArgumentException("end cannot be NULL");
		}
		if ( start.after( end ) ) {
			throw new IllegalArgumentException("Start date cannot be after end date");
		}
		this.seconds = (long) ( ( end.getTime() - start.getTime() ) / 1000.0d );
	}
	
	/**
	 * Creates a duration in seconds.
	 * 
	 * @param seconds
	 * @return
	 */
	public static Duration seconds(long seconds) {
		return new Duration( seconds );
	}
	
	/**
	 * Creates a duration in minutes.
	 * 
	 * @param minutes
	 * @return
	 */
	public static Duration minutes(long minutes) {
		return seconds( 60 * minutes );
	}
	
	/**
	 * Creates a duration in hours.
	 * 
	 * @param hours
	 * @return
	 */
	public static Duration hours(long hours) {
		return minutes( 60 * hours );
	}
	
	/**
	 * Creates a duration in days.
	 * 
	 * @param days
	 * @return
	 */	
	public static Duration days(long days) {
		return hours( 24 * days );
	}
	
	/**
	 * Creates a duration in weeks.
	 * 
	 * @param weeks
	 * @return
	 */	
	public static Duration weeks(long weeks) {
		return days( 7 * weeks );
	}
	
	/**
	 * Creates a duration in months.
	 * @param months
	 * @return
	 */
	public static Duration months(long months) {
		return weeks( 4 * months );
	}
	
	/**
	 * Creates a duration in years.
	 * 
	 * @param years
	 * @return
	 */	
	public static Duration years(long years) {
		return days( 365 * years );
	}
	
	/**
	 * Adds another duration to this.
	 * 
	 * @param other
	 * @return The result of the addition
	 */
	public Duration add(Duration other) {
		if ( other == null ) {
			throw new IllegalArgumentException("date cannot be NULL");
		}
		if ( other.isUnknown() ) {
			return other;
		}
		return new Duration( this.seconds + other.seconds );
	}
	
	/**
	 * Adds another duration to this.
	 * 
	 * @param other
	 * @return The result of the addition
	 */
	public Duration plus(Duration other) {
		return add( other );
	}
	
	/**
	 * Substracts a duration from this duration.
	 * 
	 * @param other
	 * @return
	 * @throws IllegalArgumentException if the returned duration would be negative.
	 * @see #shorterThan(Duration)
	 */
	public Duration subtract(Duration other) {
		
		if ( other == null ) {
			throw new IllegalArgumentException("date cannot be NULL");
		}
		
		if ( other.isUnknown() ) {
			return other;
		}
		
		if ( this.shorterThan( other ) ) {
			throw new IllegalArgumentException("Cannot subtract longer duration from shorter ");
		}
		
		return new Duration( this.seconds - other.seconds );
	}
	
	/**
	 * Checks whether this duration is
	 * longer than another.
	 * 
	 * @param other
	 * @return
	 */
	public boolean longerThan(Duration other) {
		
		if ( other == null ) {
			throw new IllegalArgumentException("other duration cannot be NULL");
		}
		
		if ( other.isUnknown() ) {
			throw new UnsupportedOperationException("Cannot compare to unknown duration");
		}
		return this.seconds > other.seconds;
	}
	
	/**
	 * Checks whether this duration
	 * is shorther than another.
	 * 
	 * @param other
	 * @return
	 */
	public boolean shorterThan(Duration other) {
		if ( other == null ) {
			throw new IllegalArgumentException("other duration cannot be NULL");
		}
		if ( other.isUnknown() ) {
			throw new UnsupportedOperationException("Cannot compare to unknown duration");
		}
		return this.seconds < other.seconds;
	}
	
	/**
	 * Converts this duration to seconds.
	 * 
	 * @return
	 */
	public long toSeconds() {
		return seconds;
	}
	
	/**
	 * Converts this duration to minutes.
	 * 
	 * @return
	 */	
	public double toMinutes() {
		return seconds / 60.0d;
	}
	
	/**
	 * Converts this duration to hours.
	 * 
	 * @return
	 */	
	public double toHours() {
		return seconds / ( 60.0d * 60.0d );
	}
	
	/**
	 * Converts this duration to days.
	 * 
	 * @return
	 */	
	public double toDays() {
		return seconds / ( 24.0d * 60.0d * 60.0d );
	}
	
	/**
	 * Converts this duration to weeks.
	 * 
	 * @return
	 */	
	public double toWeeks() {
		return seconds / ( 7.0d * 24.0d * 60.0d * 60.0d );
	}

	/**
	 * Converts this duration to months.
	 * 
	 * @return
	 */	
	public double toMonths() {
		return seconds / ( 4.0d * 7.0d * 24.0d * 60.0d * 60.0d );
	}
	
	/**
	 * Returns a human-readable
	 * representation of this duration.
	 * 
	 * @return
	 */	
	public String toString() {

		if ( seconds == 0 ) {
			return "0 seconds";
		}
		
		long val = seconds;
		final StringBuilder result = new StringBuilder();
		for ( Type t : TYPES_SORTED_DESCENDING ) {
			final double factor = t.toSeconds();
			if ( val >= factor ) {
				final int num = (int) Math.floor( (double) val / factor );
				val -= ( num * factor );
				
				if ( result.length() > 0 ) {
					result.append(" , ");
				}
				result.append( num ).append(" ").append( t.getName( num ) );
			}
		}
		return result.toString();
	}

	@Override
	public int compareTo(Duration o)
	{
		
		if ( o.isUnknown() ) {
			throw new IllegalArgumentException("Cannot compare with unknown duration");
		}
		
		if ( this.seconds < o.seconds ) {
			return -1;
		} else if ( this.seconds == o.seconds ) {
			return 0;
		}
		return 1;
	}

	/**
	 * Adds this duration to a given date.
	 * 
	 * @param now
	 * @return the result of the addition
	 */
	public Date addTo(Calendar cal) {
		return addTo( cal.getTime() );
	}
	
	/**
	 * Adds this duration to a given date.
	 * 
	 * @param now
	 * @return the result of the addition
	 */
	public Date addTo(Date now)
	{
		if ( now == null ) {
			throw new IllegalArgumentException("date cannot be NULL");
		}
		return new Date( now.getTime() + ( this.seconds * 1000 ) );
	}

	public Date subtractFrom(Date date)
	{
		final Calendar cal = Calendar.getInstance();
		cal.setTime( date );
		cal.set(Calendar.MILLISECOND , 0 );
		cal.add( Calendar.SECOND , (int) -seconds );
		return cal.getTime();
	}
	
	
}
