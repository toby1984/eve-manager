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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.util.CsvHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class PriceInfoFile
{
    private static final Logger log = Logger.getLogger( PriceInfoFile.class );

    private interface ICsvLineFormat
    {

        public Date getDate(String[] csv) throws ParseException;

        public PriceInfo.Type getOrderType(String[] csv);

        public long getMinPrice(String[] csv);

        public long getAvgPrice(String[] csv);

        public long getMaxPrice(String[] csv);

        public Source getSource(String[] csv);

        public int getOrderCount(String[] csv);

        public long getTotalVolumn(String[] csv);

        public long getRemainingVolume(String[] csv);

        public long getOrderId(String[] csv);

        public int getColumnCount();

        public PriceInfo read(String[] csv, ISystemClock clock) throws ParseException;

        public void write(PrintWriter writer, PriceInfo info) throws IOException;
    }

    protected final class DefaultLineFormat implements ICsvLineFormat
    {

        private final DateFormat DATE_FORMAT =
                new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

        private static final int ORDER_ID_IDX = 0;
        private static final int DATE_IDX = 1;
        private static final int TYPE_IDX = 2;
        private static final int SOURCE_IDX = 3;
        private static final int MIN_PRICE_IDX = 4;
        private static final int AVG_PRICE_IDX = 5;
        private static final int MAX_PRICE_IDX = 6;
        private static final int ORDER_COUNT_IDX = 7;
        private static final int ORDERS_REMAINING_VOLUME_IDX = 8;
        private static final int ORDERS_TOTAL_VOLUME_IDX = 9;

        private static final int CSV_COLUMNS = 10;

        @Override
        public long getAvgPrice(String[] csv)
        {
            return Long.parseLong( csv[AVG_PRICE_IDX] );
        }

        @Override
        public Date getDate(String[] csv) throws ParseException
        {
            return DATE_FORMAT.parse( csv[DATE_IDX] );
        }

        @Override
        public long getMaxPrice(String[] csv)
        {
            return Long.parseLong( csv[MAX_PRICE_IDX] );
        }

        @Override
        public long getMinPrice(String[] csv)
        {
            return Long.parseLong( csv[MIN_PRICE_IDX] );
        }

        @Override
        public Source getSource(String[] csv)
        {
            return Source.getSource( csv[SOURCE_IDX] );
        }

        @Override
        public Type getOrderType(String[] csv)
        {
            return PriceInfo.Type.fromTypeId( csv[TYPE_IDX] );
        }

        @Override
        public int getColumnCount()
        {
            return CSV_COLUMNS;
        }

        @Override
        public long getTotalVolumn(String[] csv)
        {
            return Long.parseLong( csv[ORDERS_TOTAL_VOLUME_IDX] );
        }

        @Override
        public int getOrderCount(String[] csv)
        {
            return Integer.parseInt( csv[ORDER_COUNT_IDX] );
        }

        @Override
        public long getOrderId(String[] csv)
        {
            return Long.parseLong( csv[ORDER_ID_IDX] );
        }

        @Override
        public long getRemainingVolume(String[] csv)
        {
            return Long.parseLong( csv[ORDERS_REMAINING_VOLUME_IDX] );
        }

        @Override
        public void write(PrintWriter writer, PriceInfo info) throws IOException
        {

            if ( log.isTraceEnabled() )
            {
                log.trace( "writePriceInfo(): Writing " + info );
            }

            String[] data = new String[CSV_COLUMNS];

            data[ORDER_ID_IDX] = Long.toString( info.getOrderId() );
            data[DATE_IDX] = DATE_FORMAT.format( info.getTimestamp().getLocalTime() );
            data[TYPE_IDX] = info.getPriceType().getId();
            data[SOURCE_IDX] = info.getSource().getTypeId();
            data[MIN_PRICE_IDX] = Long.toString( info.getMinPrice() );
            data[AVG_PRICE_IDX] = Long.toString( info.getAveragePrice() );
            data[MAX_PRICE_IDX] = Long.toString( info.getMaxPrice() );
            data[ORDER_COUNT_IDX] = Long.toString( info.getOrderCount() );
            data[ORDERS_TOTAL_VOLUME_IDX] = Long.toString( info.getVolume() );
            data[ORDERS_REMAINING_VOLUME_IDX] = Long.toString( info.getRemainingVolume() );

            CsvHelper.writeCsvLine( writer, data );
        }

        @Override
        public PriceInfo read(String[] csv, ISystemClock clock) throws ParseException
        {
            final PriceInfo result =
                    new PriceInfo( getOrderType( csv ), itemType, getSource( csv ) );

            result.setAveragePrice( getAvgPrice( csv ) );
            // result.setInventoryType( ); / already set
            result.setMaxPrice( getMaxPrice( csv ) );
            result.setMinPrice( getMinPrice( csv ) );
            result.setOrderCount( getOrderCount( csv ) );
            result.setOrderId( getOrderId( csv ) );
            result.setRegion( region );
            result.setRemainingVolume( getRemainingVolume( csv ) );
            // result.setSource( getSource( csv ) ); // already set
            result.setTimestamp( EveDate.fromLocalTime( getDate( csv ), clock ) );
            result.setVolume( getTotalVolumn( csv ) );

            return result;
        }

    }

    private final ICsvLineFormat fileFormat = new DefaultLineFormat();
    private final Region region;
    private final InventoryType itemType;
    private final File file;

    public interface IPriceInfoVisitor
    {

        public boolean visit(PriceInfo info);
    }

    public PriceInfoFile(long regionId, long invTypeId, File file,
            IStaticDataModel dataModel) {
        this( dataModel.getRegion( regionId ), dataModel.getInventoryType( invTypeId ),
                file );

    }

    public PriceInfoFile(Region region, InventoryType type, File file) {
        if ( region == null )
        {
            throw new IllegalArgumentException( "region cannot be NULL" );
        }
        if ( type == null )
        {
            throw new IllegalArgumentException( "type cannot be NULL" );
        }
        if ( file == null )
        {
            throw new IllegalArgumentException( "file cannot be NULL" );
        }

        this.file = file;
        this.itemType = type;
        this.region = region;
    }

    public boolean exists()
    {
        return file.exists();
    }

    public List<PriceInfo> load(ISystemClock clock) throws IOException, ParseException
    {

        final ArrayList<PriceInfo> result = new ArrayList<PriceInfo>();

        load( new IPriceInfoVisitor() {

            @Override
            public boolean visit(PriceInfo info)
            {
                result.add( info );
                return true;
            }
        }, clock );

        return result;
    }

    public File getFile()
    {
        return file;
    }

    protected FileWriter openFile() throws IOException
    {

        if ( ! file.exists() )
        {
            final File parent = file.getParentFile();
            if ( ! parent.exists() )
            {
                if ( ! parent.mkdirs() )
                {
                    log.error( "openFile(): Unable to create parent directories "
                            + parent.getAbsolutePath() );
                    throw new IOException( "Unable to create parent directories "
                            + parent.getAbsolutePath() );
                }
            }
        }
        return new FileWriter( file );
    }

    public void load(IPriceInfoVisitor visitor, ISystemClock clock) throws IOException,
            ParseException
    {

        if ( ! file.exists() )
        {
            return;
        }

        final BufferedReader reader = new BufferedReader( new FileReader( file ) );
        try
        {
            String line;
            while ( ( line = reader.readLine() ) != null )
            {
                if ( ! visitor.visit( readPriceInfo( line, clock ) ) )
                {
                    break;
                }
            }
        }
        finally
        {
            reader.close();
        }
    }

    public Region getRegion()
    {
        return region;
    }

    public InventoryType getInventoryType()
    {
        return itemType;
    }

    protected ICsvLineFormat getCsvLineFormat()
    {
        return fileFormat;
    }

    protected PriceInfo readPriceInfo(String line, ISystemClock clock)
            throws IOException, ParseException
    {

        String[] data = CsvHelper.readCsvLine( line );
        if ( data.length == 0 )
        {
            return null;
        }

        if ( data.length != getCsvLineFormat().getColumnCount() )
        {
            throw new IOException( "CSV file corrupted at line " + line + " , expected "
                    + getCsvLineFormat().getColumnCount() + " columns" );
        }

        return getCsvLineFormat().read( data, clock );
    }

    public void save(List<PriceInfo> entries) throws IOException
    {

        if ( log.isDebugEnabled() )
        {
            log.debug( "save(): Saving " + entries.size() + " prices for "
                    + itemType.getName() + " (" + itemType.getId() + ") to "
                    + file.getAbsolutePath() );
        }

        final List<PriceInfo> sorted = new ArrayList<PriceInfo>( entries );

        // sort so that latest entry comes at head of file
        Collections.sort( sorted, new Comparator<PriceInfo>() {

            @Override
            public int compare(PriceInfo o1, PriceInfo o2)
            {
                return o2.getTimestamp().compareTo( o1.getTimestamp() );
            }
        } );

        final PrintWriter writer = new PrintWriter( new BufferedWriter( openFile() ) );

        try
        {
            for (PriceInfo info : entries)
            {
                getCsvLineFormat().write( writer, info );
            }
            writer.flush();
        }
        finally
        {
            writer.close();
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "save(): Finished." );
        }
    }

    @Override
    public String toString()
    {
        return "PriceInfoFile[ file = "
                + file.getAbsolutePath()
                + " , region="
                + ( region != null ? region.getName() : "<NULL>" )
                + " , item="
                + ( this.itemType != null ? itemType.getName() + " ( "
                        + itemType.getTypeId() + ")" : "<NULL" );
    }
}
