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
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class CsvHelper {

	public static final char COLUMN_SEPARATOR =  ';' ;
	
	public static final char LINE_SEPARATOR =  '\n' ;
	
	private static final String[] EMPTY_ARRAY = new String[0];
	
	public static void writeCsvLine(Writer writer , String[] data) throws IOException {
		final int len = data.length;
		for ( int i = 0 ; i < len ; i++ ) {
			writer.write( data[i] );
			if ( (i+1) < len ) {
				writer.write( COLUMN_SEPARATOR );
			}
		}
		if ( len > 0 ) {
			writer.write( LINE_SEPARATOR );
		}
	}
	
	public static String[] readCsvLine(String line) throws IOException {
		return readCsvLine( line , COLUMN_SEPARATOR , LINE_SEPARATOR );
	}
	
	public static String[] readCsvLine(BufferedReader reader) throws IOException {
		return readCsvLine( reader , COLUMN_SEPARATOR , LINE_SEPARATOR );
	}
	
	public static String[] readCsvLine(BufferedReader reader, char columnSeparator) throws IOException {
		return readCsvLine( reader ,columnSeparator, LINE_SEPARATOR );
	}
	
	public static String[] readCsvLine(BufferedReader reader, char columnSeparator, char lineSeparator) throws IOException {
		return readCsvLine( reader.readLine() , columnSeparator , lineSeparator );
	}
	
	public static String[] readCsvLine(String line, char columnSeparator, char lineSeparator) throws IOException {
		
		if ( line == null ) {
			return EMPTY_ARRAY;
		}
		
		final int len = line.length();
		if ( len == 0 ) {
			return EMPTY_ARRAY;
		}
		
		final List<String> result = 
			new ArrayList<String>();

		int lastMark = 0;
		
		for ( int i = 0 ; i < len ; i++ ) {
			final char c = line.charAt( i );
			if ( c == columnSeparator ) {
				result.add( line.substring( lastMark  , i ) );
				lastMark=i+1;
			} else if ( c == lineSeparator ) {
				result.add( line.substring( lastMark  , i ) );
				lastMark=len;
				break;
			} 
		}
		
		if ( lastMark < len ) {
			result.add( line.substring( lastMark  , len ) );
		}
		
		return result.toArray( new String[ result.size() ] );
	}
}
