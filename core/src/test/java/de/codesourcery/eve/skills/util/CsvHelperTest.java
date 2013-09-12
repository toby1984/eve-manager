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
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;

public class CsvHelperTest extends TestCase {

	public void testWriteEmpty() throws IOException {

		StringWriter writer = new StringWriter();
		CsvHelper.writeCsvLine( writer , new String[0] );
		assertEquals("" , writer.toString() );
	}
	
	public void testWriteOneElement() throws IOException {

		StringWriter writer = new StringWriter();
		CsvHelper.writeCsvLine( writer , new String[] { "blubb" } );
		assertEquals("blubb"+CsvHelper.LINE_SEPARATOR , writer.toString() );
	}
	
	public void testWriteTwoElements() throws IOException {

		StringWriter writer = new StringWriter();
		CsvHelper.writeCsvLine( writer , new String[] { "blubb" , "blah" } );
		assertEquals("blubb"+CsvHelper.COLUMN_SEPARATOR+"blah"+CsvHelper.LINE_SEPARATOR , writer.toString() );
	}
	
	public void testWriteTwoLines() throws IOException {

		StringWriter writer = new StringWriter();
		CsvHelper.writeCsvLine( writer , new String[] { "blubb" , "blah" } );
		CsvHelper.writeCsvLine( writer , new String[] { "xyz" , "abc" } );
		assertEquals("blubb"+CsvHelper.COLUMN_SEPARATOR+"blah"+CsvHelper.LINE_SEPARATOR
				+"xyz"+CsvHelper.COLUMN_SEPARATOR+"abc"+CsvHelper.LINE_SEPARATOR , writer.toString() );
	}
	
	public void testReadEmpty() throws IOException {
		
		final StringReader reader = new StringReader("");
		
		final String[] result =
			CsvHelper.readCsvLine( new BufferedReader( reader ) );
		
		assertArrayEquals( result , new String[0] );
	}
	
	public void testReadOneElement() throws IOException {
		
		final StringReader reader = new StringReader("xyz");
		
		final String[] result =
			CsvHelper.readCsvLine( new BufferedReader( reader ) );
		
		assertArrayEquals( result , "xyz" );
	}
	
	public void testReadTwoElements() throws IOException {
		
		final StringReader reader = new StringReader("xyz"+CsvHelper.COLUMN_SEPARATOR+"abc");
		
		final String[] result =
			CsvHelper.readCsvLine( new BufferedReader( reader ) );
		
		assertArrayEquals( result , "xyz" , "abc");
	}
	
	public void testReadTwoLines() throws IOException {
		
		final StringReader reader = new StringReader("abc"+CsvHelper.COLUMN_SEPARATOR+"def"+CsvHelper.LINE_SEPARATOR+
				"ghi"+CsvHelper.COLUMN_SEPARATOR+"jkl");
		
		final BufferedReader bufferedReader =
			new BufferedReader( reader ) ;
		
		final String[] result1 =
			CsvHelper.readCsvLine( bufferedReader );
		
		final String[] result2 =
			CsvHelper.readCsvLine( bufferedReader );
		
		assertArrayEquals( result1 , "abc" , "def" );
		assertArrayEquals( result2 , "ghi" , "jkl" );
	}
	
	protected void assertArrayEquals(String[] actual,String... expected) {
		
		if ( ! ArrayUtils.isEquals( actual , expected ) ) {
			fail("Got: "+ArrayUtils.toString( actual )+" , expected: "+ArrayUtils.toString( expected ) );
		}
	}
}
