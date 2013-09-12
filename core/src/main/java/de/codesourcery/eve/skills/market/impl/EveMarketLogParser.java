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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.MarketLogEntry;
import de.codesourcery.eve.skills.market.impl.MarketLogFile.IMarketLogFilter;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Parses CSV files generated by the Eve client
 * market export.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class EveMarketLogParser {

	private static final DateFormat DATE_FORMAT = 
		new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS");

	private final ISystemClock clock;
	private final IStaticDataModel dataModel;

	public EveMarketLogParser(IStaticDataModel dataModel,ISystemClock systemClock) {
		if (dataModel == null) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		if ( systemClock == null ) {
			throw new IllegalArgumentException("systemClock cannot be NULL");
		}
		this.clock = systemClock;
		this.dataModel = dataModel;
	}
	
	protected static final String formatLeft(Object s) {
		return StringUtils.leftPad( s.toString() , 20 )+" | ";
	}

	protected static final String formatRight(Object s) {
		return StringUtils.rightPad( s.toString() , 20 )+" | ";
	}

	protected String[] parseLine(BufferedReader reader ) throws IOException {

		final String line = reader.readLine();
		if ( line == null || line.trim().length() == 0 ) {
			return new String[0];
		}
		return line.split(",");
	}

	protected static IMarketLogFilter createFilter(final IMarketLogFilter filter1,final IMarketLogFilter filter2) {
		return new IMarketLogFilter() {

			@Override
			public boolean includeInResult(MarketLogEntry entry) {
				return filter1.includeInResult( entry ) && filter2.includeInResult( entry );
			}};
	}
	
	public MarketLogFile parseFile(File file) throws IOException, ParseException {

		InventoryType type=null;
		Region region =null;

		final BufferedReader reader =
			new BufferedReader(new FileReader(file) );

		final List<MarketLogEntry> result = 
			new ArrayList<MarketLogEntry>();

		try {
			/*
price,volRemaining,typeID,range,orderID   ,volEntered,minVolume ,  bid , issued                  ,duration      ,stationID,regionID,solarSystemID,jumps,
40.0 ,1621.0      ,37    ,32767,1235636507, 1621     , 1        ,False , 2009-08-09 16:57:34.000,  90,           60007579,10000001,30000037,6,	 
			 */

			String[] cols =
				parseLine( reader );

			if ( cols.length == 0 || ! "price".equalsIgnoreCase( cols[0] ) ) {
				throw new ParseException("No or unparseable header column in file "+file.getAbsolutePath(),0);
			}

			final Map<String, Integer> columns  = 
				new HashMap<String,Integer>();

			for ( int i = 0 ; i < cols.length ; i++ ) {
				columns.put( cols[i] , i );
			}

			while ( ( cols = parseLine( reader ) ).length > 0 ) {

				final MarketLogEntry entry = new MarketLogEntry();

				if ( type == null ) {
					final long typeId = 
						parseLong( readColumn( cols , "typeID", columns ) );
					type = dataModel.getInventoryType( typeId );
				}

				if ( region == null ) {
					final long regionId =
						parseLong( readColumn( cols , "regionID", columns ) );
					region = dataModel.getRegion( regionId );
				}

				entry.setRemainingVolume( parseDouble( readColumn( cols , "volRemaining" , columns ) ) );
				entry.setOrderId( parseLong( readColumn( cols , "orderID" ,columns ) ) );
				entry.setPrice( parseDouble( readColumn( cols , "price", columns ) ) );
				entry.setBuyOrder( parseBoolean( readColumn( cols , "bid", columns ) ) );
				entry.setVolume( parseDouble( readColumn( cols , "volEntered", columns ) ) );
				final Date issueDate = parseDate( readColumn( cols , "issued", columns ) );
				entry.setIssueDate( EveDate.fromServerTime( issueDate , clock ) );
				entry.setMinVolume( parseInt( readColumn( cols , "minVolume", columns ) ) );
				result.add( entry );
			}
		} finally {
			reader.close();
		}

		return new MarketLogFile(result , type , region );
	}

	protected static final String readColumn(String[] data, String column,Map<String, Integer> mappings ) throws ParseException {
		final Integer idx = mappings.get( column );
		if ( idx == null ) {
			throw new ParseException("CSV lacks column '"+column+"'",-1);
		}
		return data[ idx ];
	}

	protected static final boolean parseBoolean(String s) { 
		return Boolean.parseBoolean( s.trim().toLowerCase() );
	}

	protected static final int parseInt(String s) { return Integer.parseInt( s );	}
	protected static final long parseLong(String s) { return Long.parseLong( s );	}
	protected static final double parseDouble(String s ) { return Double.parseDouble( s.trim() ); }
	protected static final Date parseDate(String s ) throws ParseException { return DATE_FORMAT.parse( s ); }


}