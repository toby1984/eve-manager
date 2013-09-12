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
package de.codesourcery.eve.skills.market.impl;

import static java.util.Calendar.getInstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.MockSystemClock;

public class TestHelper extends TestCase {

	public static final InventoryType ITEM1 =
		createItem(1 , "Item #1" );

	public static final InventoryType ITEM2 =
		createItem(2 , "Item #2" );

	public static final InventoryType ITEM3 =
		createItem(3 , "Item #3" );

	public static final Region REGION1=
		createRegion(1 , "Region #1" );

	public static final Region REGION2=
		createRegion(2 , "Region #2" );
	
	private MockSystemClock systemClock = new MockSystemClock();

	protected static InventoryType createItem(long id,String name) {
		InventoryType result =
			new InventoryType();

		result.setTypeId( id );
		result.setName( name );
		return result;
	}

	protected EveDate eveDateFromLocalTime(Date date) {
		return EveDate.fromLocalTime( date , systemClock );
	}
	
	protected MockSystemClock systemClock() {
		return systemClock;
	}
	
	protected EveDate currentDateWithoutMillis() {
		return currentDateWithoutMillis(0);
	}
	
	protected EveDate currentDateWithoutMillis(int offsetInSeconds) {
		final Calendar cal = getInstance();
		cal.set( Calendar.MILLISECOND , 0 );
		cal.add( Calendar.SECOND , offsetInSeconds );
		return EveDate.fromLocalTime( cal.getTime() , systemClock );
	}
	
	protected void deleteFile(File f) {
		System.out.println("Deleting "+f);

		if ( ! f.delete() ) {
			throw new RuntimeException("Failed to delete "+f);
		}
	}

	private void deleteDir(File tmpDir2) {

		for ( File f : tmpDir2.listFiles() ) {
			if ( f.isDirectory() ) {
				deleteDir( f );
			} else if ( f.isFile() ) {
				deleteFile( f );
			} else {
				System.out.println("Ignoring unknown file "+f);
			}
		}

		if ( ! tmpDir2.delete() ) {
			throw new RuntimeException("Failed to delete directory "+tmpDir2);
		}
	}

	protected List<String> readFile(File f) throws IOException 
	{
		final BufferedReader reader = 
			new BufferedReader( new FileReader(f) );
		
		try {
			String line;
			final ArrayList<String> result = new ArrayList<String>();
			while (( line = reader.readLine() ) != null ) {
				result.add( line );
			}
			return result;
		} 
		finally {
			reader.close();
		}
	}

	protected File createTempDir() throws IOException {
		File tmpDir =
			File.createTempFile("some" , "tmpFile" );

		tmpDir.delete();

		if ( tmpDir.exists() ) {
			// make sure it's empty
			deleteDir( tmpDir );
		} else if ( ! tmpDir.mkdir() ) {
			throw new RuntimeException("Failed to create tmp dir "+tmpDir);
		}

		tmpDir.deleteOnExit();
		return tmpDir;
	}

	protected EveDate createDate(int offsetInDaysFromNow) {
		return eveDateFromLocalTime( new Date( System.currentTimeMillis() + (offsetInDaysFromNow*24*60*60*1000) ) );
	}

	protected static Region createRegion(long id,String name) {

		Region result =
			new Region();

		result.setID( id );
		result.setName( name );
		return result;
	}

	protected PriceInfo createPriceInfo(Type type, InventoryType item, Region region) {

		PriceInfo info = new PriceInfo( type , item , Source.EVE_CENTRAL );
		info.setRegion( region );
		info.setTimestamp( new EveDate( systemClock() ) );
		return info;
	}

	protected static <T> Collection<T> asList(T... data) {
		Collection<T> result = new ArrayList<T>();
		if ( data != null ) {
			for  ( T obj : data ) {
				result.add( obj );
			}
		}
		return result;
	}

}
