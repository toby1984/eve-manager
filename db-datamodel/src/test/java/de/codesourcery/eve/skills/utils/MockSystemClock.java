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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MockSystemClock implements ISystemClock
{

	public static final SimpleDateFormat DF = 
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private final Date now;
	private TimeZone timezone;

	public MockSystemClock() {
		this( new Date() );
	}
	
	public MockSystemClock(String date) throws ParseException {
		this( DF.parse( date ) );
	}
	
	public MockSystemClock(long timeInMillis) {
		this( new Date(timeInMillis ) );
	}
	
	public MockSystemClock(Date now) {
		this.now = now;
		timezone = TimeZone.getDefault();
	}
	
	@Override
	public long getCurrentTimeMillis()
	{
		return now.getTime();
	}

	@Override
	public TimeZone getLocalTimezone()
	{
		return timezone;
	}

}
