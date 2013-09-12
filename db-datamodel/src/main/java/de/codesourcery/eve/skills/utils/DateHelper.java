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
package de.codesourcery.eve.skills.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Helper class for date conversions between EVE Online(tm) API
 * server time and local time.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class DateHelper {

	private static final long SECOND = 1;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = 60 * MINUTE;
	private static final long DAY = 24 * HOUR;
	private static final long MONTH = 4 * 7 * DAY;
	
	public static final TimeZone SERVER_TIMEZONE =
		TimeZone.getTimeZone("UTC");

	private static ThreadLocal<SimpleDateFormat> DATE_FORMAT = 
		new ThreadLocal<SimpleDateFormat>();

	private DateHelper() {
	}

	protected static DateFormat getDateFormat() {
		SimpleDateFormat result =
			DATE_FORMAT.get();
		if ( result == null ) {
			result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			DATE_FORMAT.set( result );
		} 
		return result;
	}	

	public static Date toLocalTime(Date serverTime,ISystemClock clock) {
		return toLocalTime( getDateFormat().format( serverTime ) , clock );
	}
	
	public static String format(Date date) {
		return date == null ? "" : getDateFormat().format( date );
	}
	
	/**
	 * Convert from server time to local time.
	 * 
	 * @param serverTime server time, may be <code>null</code>
	 * @param clock
	 * @return local time or <code>null</code> if input time
	 * was <code>null</code>
	 * @throws ParseException 
	 */
	public static Date toLocalTime(String date,ISystemClock clock) {
		try {
			return convert(date , SERVER_TIMEZONE , clock.getLocalTimezone() , clock );
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	protected static Date convert(String date, 
			TimeZone from,
			TimeZone to,
			ISystemClock systemClock) throws ParseException 
	{

		if ( date == null ) {
			return null;
		}

		DateFormat dateFormat = getDateFormat();
		dateFormat.setTimeZone( from );

		final Date fromDate = dateFormat.parse( date ); // to -> UTC
		dateFormat.setTimeZone( to );

		final String converted  = dateFormat.format( fromDate ); // UTC -> from
		dateFormat.setTimeZone( from );
		return dateFormat.parse( converted );
	}
	
	public static Date stripTime(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime( date );
		cal.set( Calendar.HOUR_OF_DAY ,0 );
		cal.set( Calendar.MINUTE,0 );
		cal.set( Calendar.SECOND,0 );
		cal.set( Calendar.MILLISECOND,0 );
		return cal.getTime();
	}

	/**
	 * Convert from server time to local time.
	 * 
	 * @param localTime local time, may be <code>null</code>
	 * @param clock
	 * @return server time or <code>null</code> if input time
	 * was <code>null</code>
	 */	
	public static Date toServerTime(Date localTime,ISystemClock clock) {
		try {
			return convert( getDateFormat().format( localTime ),
					clock.getLocalTimezone() , 
					SERVER_TIMEZONE, clock );
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String durationToString(long durationInMilliSeconds) {
		
		long duration = durationInMilliSeconds / 1000; // => seconds
		
		StringBuilder result =
			new StringBuilder();
		
		if ( duration >= MONTH ) {
			final long value  = duration / MONTH ;
			duration -= (value * MONTH );
			result.append( value ).append("month(s)");
		}
		
		if ( duration >= DAY ) {
			final long value  = duration / DAY;
			duration -= (value * DAY );
			if ( result.length() > 0 ) {
				result.append( ", " );
			}
			result.append( value ).append("d");
		}
		
		if ( duration >= HOUR ) {
			final long value = duration / HOUR;
			duration -= (value * HOUR );
			if ( result.length() > 0 ) {
				result.append( ", " );
			} 
			result.append( value ).append("h");
		}
		
		if ( duration >= MINUTE ) {
			final long value = duration / MINUTE;
			duration -= (value * MINUTE );
			if ( result.length() > 0 ) {
				result.append(", ");
			}
			result.append( value ).append("m");
		}
		
		if ( duration >= SECOND ) {
			final long value = duration / SECOND;
			duration -= (value * SECOND );
			if ( result.length() > 0 ) {
				result.append(", ");
			}
			result.append( value ).append("s");
		}

		return result.toString(); 
	}
}
