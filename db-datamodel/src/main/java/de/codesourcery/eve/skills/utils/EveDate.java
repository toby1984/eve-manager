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

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

public final class EveDate implements Comparable<EveDate>
{

	public static final Comparator<EveDate> COMPARATOR =
		new Comparator<EveDate> () {

			@Override
			public int compare(EveDate o1, EveDate o2)
			{
				return o1.compareTo( o2 );
			}
	};
		
	private final Date serverTime;
	private final Date localTime;
	
	public EveDate(ISystemClock clock) {
		final Date now = new Date( clock.getCurrentTimeMillis() );
		this.serverTime = DateHelper.toServerTime( now  , clock );
		this.localTime = now;
	}
	
	private EveDate(long localTime,long serverTime) {
		this.localTime = new Date( localTime );
		this.serverTime = new Date( serverTime);
	}
	
	private EveDate(Date localTime,Date serverTime) {
		this.localTime = localTime;
		this.serverTime = serverTime;
	}
	
	public boolean isSameDay(EveDate other) {
		return EveDate.isSameDay( this , other );
	}
	
	public EveDate addMilliseconds(long millis) {
		return new EveDate( localTime.getTime() + millis , serverTime.getTime() + millis );
	}
	
	public long getDifferenceInMilliseconds(EveDate other) {
		
		final Calendar cal1 = Calendar.getInstance();
		cal1.setTime( this.localTime );
		
		final Calendar cal2 = Calendar.getInstance();
		cal2.setTime( other.localTime );
		
		return cal1.getTimeInMillis() - cal2.getTimeInMillis();
	}
	
	public long getAgeInMillis(ISystemClock clock) {
		// TODO: Needs to account for daylight saving
		return clock.getCurrentTimeMillis() - this.localTime.getTime();
	}
	
	public static boolean isSameDay(EveDate date1,EveDate date2 ) {
		
		if ( date1 == null ) {
			throw new IllegalArgumentException("date1 cannot be NULL");
		}
		
		if ( date2 == null ) {
			throw new IllegalArgumentException("date2 cannot be NULL");
		}
		
		final Calendar cal1 = Calendar.getInstance();
		cal1.setTime( date1.localTime );
		
		final Calendar cal2 = Calendar.getInstance();
		cal2.setTime( date2.localTime );
		
		return cal1.get( Calendar.DAY_OF_MONTH ) == cal2.get( Calendar.DAY_OF_MONTH) &&
		cal1.get( Calendar.MONTH ) == cal2.get( Calendar.MONTH) &&
		cal1.get( Calendar.YEAR ) == cal2.get( Calendar.YEAR);
	}
	
	public static EveDate fromServerTime(Date serverTime ,ISystemClock clock) {
		return new EveDate( DateHelper.toLocalTime( serverTime , clock ),
				serverTime );
	}
	
	public static EveDate fromLocalTime(long localTime ,ISystemClock clock) {
		return fromLocalTime( new Date(localTime) , clock );
	}
	
	public static EveDate fromLocalTime(Date localTime ,ISystemClock clock) {
		return new EveDate( localTime , DateHelper.toServerTime( localTime , clock ) );
	}
	
	public Date getServerTime() {
		return serverTime;
	}
	
	public Date getLocalTime() {
		return localTime;
	}
	
	protected static Date stripTime(Date d) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime( d );
		cal.set( Calendar.HOUR_OF_DAY ,0 );
		cal.set( Calendar.MINUTE,0 );
		cal.set( Calendar.SECOND,0 );
		cal.set( Calendar.MILLISECOND,0 );
		return cal.getTime();
	}
	
	public EveDate stripTime(ISystemClock clock) {
		return fromLocalTime( stripTime(localTime ) , clock );
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof EveDate ) {
			return this.serverTime.getTime() == ((EveDate) obj).serverTime.getTime();
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return serverTime.hashCode();
	}
	
	public boolean before(EveDate date) {
		return this.serverTime.before( date.getServerTime() );
	}
	
	public boolean after(EveDate date) {
		return this.serverTime.after( date.getServerTime() );
	}

	@Override
	public int compareTo(EveDate o)
	{
		return this.serverTime.compareTo( o.serverTime );
	}
	
	@Override
	public String toString()
	{
		return "local="+localTime+" / server="+serverTime;
	} 
	
}
