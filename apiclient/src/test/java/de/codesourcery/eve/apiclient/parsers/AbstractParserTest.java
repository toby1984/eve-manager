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
package de.codesourcery.eve.apiclient.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import junit.framework.TestCase;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Abstract base-class for parser tests. 
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class AbstractParserTest extends TestCase {
	
	private final ISystemClock systemClock = new ISystemClock() {

		@Override
		public long getCurrentTimeMillis()
		{
			return System.currentTimeMillis();
		}

		@Override
		public TimeZone getLocalTimezone()
		{
			return TimeZone.getDefault();
		}};
	
	protected static String loadXML(InputStream in) throws UnparseableResponseException, IOException  {
		
		final StringBuilder xml = new StringBuilder();
		final BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
		try {
			String line=null;
			while ( ( line = reader.readLine() ) != null ) {
				xml.append( line );
			}
			return xml.toString(); 
		} finally {
			reader.close();
		}
	}
	
	protected ISystemClock systemClock() { return systemClock; }
	
	protected EveDate createDate(String date) {
		try {
			return EveDate.fromServerTime( new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse( date ) , systemClock() );
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

}
