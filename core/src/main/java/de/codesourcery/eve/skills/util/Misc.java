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
package de.codesourcery.eve.skills.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

/**
 * Miscellanous stuff.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class Misc {

	private Misc() {
	}
	
	public static Calendar stripToDay(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime( d );
		cal.set( Calendar.HOUR_OF_DAY , 0 );
		cal.set( Calendar.MINUTE , 0 );
		cal.set( Calendar.SECOND, 0 );
		cal.set( Calendar.MILLISECOND , 0 );
		return cal;
	}
	
	public static void runOnEventThread(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void runOnEventThreadLater(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}
	
	public static final String readFile(IInputStreamProvider in) throws IOException {
		
		StringBuilder result =
			new StringBuilder();
		String line = null;
		final InputStream stream = in.createInputStream();
		try {
			final BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
			while ( ( line = reader.readLine() ) != null ) {
				result.append( line );
			}
			return result.toString();
		} finally {
			stream.close();
		}
	}
	
	public static String wrap(String s,String lineDelimiter, int maxLength) {
		
		if ( s == null || s.length() < maxLength ) {
			return s;
		}
		
		final StringBuilder result = new StringBuilder();
		for ( Iterator<String> it = wrap( s , maxLength ).iterator() ; it.hasNext() ; ) {
			result.append( it.next() );
			if ( it.hasNext() ) {
				result.append( lineDelimiter );
			}
		}
		return result.toString();
	}
	
	public static Collection<String> wrap(String s,int maxLength) {
		
		if ( StringUtils.isBlank( s ) ) {
			return Collections.emptyList();
		}
		
		if ( s.length() <= maxLength ) {
			final Collection<String> res =
				new ArrayList<String>();
			res.add( s);
			return res;
		}
		
		final List<String> lines= new ArrayList<String>();
		
		int i0=0;
		int i1=0;
		int lastSplit =-1;
		for ( ; i1 < s.length() ; i1++ ) {
			final char c = s.charAt( i1 );
			
			if ( c == ' ' || c == '.' || c == ',' ) {
				lastSplit = i1;
			}
			
			final int count = i1-i0;
			if ( count > maxLength ) {
				if ( lastSplit != -1 ) {
					lines.add( s.substring( i0 , lastSplit ) );
					i0 = lastSplit+1;
				} else {
					lines.add( s.substring( i0 , i1 ) );
					i0 = i1;
				}
				i1=i0;
				lastSplit=-1;
			}
		}
		
		if ( i1-i0 > 0 && i0 < s.length() ) {
			lines.add( s.substring( i0 , i1 ) ); 
		}
		
		return lines;
	}
}
