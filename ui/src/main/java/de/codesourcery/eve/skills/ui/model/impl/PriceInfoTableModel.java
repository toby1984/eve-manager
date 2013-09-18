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
package de.codesourcery.eve.skills.ui.model.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Source;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.market.IPriceInfoStore;
import de.codesourcery.eve.skills.market.PriceInfoQueryResult;
import de.codesourcery.eve.skills.ui.model.AbstractTableViewModel;
import de.codesourcery.eve.skills.ui.model.IViewDataModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class PriceInfoTableModel extends
        AbstractTableViewModel<PriceInfoTableModel.TableEntry>
{

    private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd" );

    public static final String EMPTY_STRING = "";

    public static final int ITEM_NAME_IDX = 0;

    public static final int AVG_BUY_PRICE_IDX = 1;
    public static final int BUY_PRICE_SOURCE_IDX = 2;
    public static final int BUY_PRICE_TIMESTAMP_IDX = 3;

    public static final int AVG_SELL_PRICE_IDX = 4;
    public static final int SELL_PRICE_SOURCE_IDX = 5;
    public static final int SELL_PRICE_TIMESTAMP_IDX = 6;

    private final IMarketDataProvider priceInfoStore;

    private static final Comparator<String> PRICE_COL_COMPARATOR =
            new Comparator<String>() {

                @Override
                public int compare(String o1, String o2)
                {

                    if ( EMPTY_STRING == o1 )
                    {
                        return 1;
                    }
                    else if ( EMPTY_STRING == o2 )
                    {
                        return - 1;
                    }

                    final long val1 = AmountHelper.parseISKAmount( o1 );
                    final long val2 = AmountHelper.parseISKAmount( o2 );
                    if ( val1 < val2 )
                    {
                        return - 1;
                    }
                    else if ( val1 > val2 )
                    {
                        return 1;
                    }
                    return 0;
                }
            };

    private final ISystemClock clock;
    private final Region defaultRegion;

    public static final class TableEntry
    {

        private final InventoryType type;
        private PriceInfo buyPrice;
        private PriceInfo sellPrice;

        public TableEntry(InventoryType type) {
            this( type, null, null );
        }

        public TableEntry(InventoryType type, PriceInfo buyPrice, PriceInfo sellPrice) {
            if ( type == null )
            {
                throw new IllegalArgumentException( "type cannot be NULL" );
            }
            this.type = type;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }

        public TableEntry(PriceInfoQueryResult result) {
            this( result.getItem(), result.hasBuyPrice() ? result.buyPrice() : null,
                    result.hasSellPrice() ? result.sellPrice() : null );
        }

        public void setBuyPrice(PriceInfo buyPrice)
        {
            this.buyPrice = buyPrice;
        }

        public void setSellPrice(PriceInfo sellPrice)
        {
            this.sellPrice = sellPrice;
        }

        public String getItemName()
        {
            return getItem().getName();
        }

        public InventoryType getItem()
        {
            return type;
        }

        public PriceInfo getBuyPrice()
        {
            return buyPrice;
        }

        public PriceInfo getSellPrice()
        {
            return sellPrice;
        }

        private long parsePrice(String price)
        {
            try
            {
                return Math.round( Float.parseFloat( price.trim() ) * 100.0f );
            }
            catch (NumberFormatException e)
            {
                return AmountHelper.parseISKAmount( price.trim() );
            }
        }

        public boolean hasBuyPrice()
        {
            return buyPrice != null;
        }

        public boolean hasSellPrice()
        {
            return sellPrice != null;
        }

    }

    public PriceInfoTableModel(IMarketDataProvider priceInfoStore, Region defaultRegion,
            IViewDataModel<TableEntry> model, ISystemClock clock) {
        super( new TableColumnBuilder().add( "Name" ) // 0
                .add( "Avg. buy price", String.class, PRICE_COL_COMPARATOR ) // 1
                .add( "Source" ) // 2
                .add( "Last update" ) // 3
                .add( "Avg. sell price", String.class, PRICE_COL_COMPARATOR ) // 4
                .add( "Source" ) // 5
                .add( "Last update" ), model ); // 6

        if ( defaultRegion == null )
        {
            throw new IllegalArgumentException( "Default region cannot be NULL" );
        }

        if ( model == null )
        {
            throw new IllegalArgumentException( "data model cannot be NULL" );
        }

        if ( clock == null )
        {
            throw new IllegalArgumentException( "clock cannot be NULL" );
        }

        if ( priceInfoStore == null )
        {
            throw new IllegalArgumentException( "priceInfoStore cannot be NULL" );
        }

        this.priceInfoStore = priceInfoStore;
        this.clock = clock;
        this.defaultRegion = defaultRegion;
    }

    public boolean isBuyPriceColumn(int modelColumn)
    {
        switch ( modelColumn )
        {
            case AVG_BUY_PRICE_IDX:
            case BUY_PRICE_SOURCE_IDX:
            case BUY_PRICE_TIMESTAMP_IDX:
                return true;
            default:
                return false;
        }
    }

    public boolean isSellPriceColumn(int modelColumn)
    {
        switch ( modelColumn )
        {
            case AVG_SELL_PRICE_IDX:
            case SELL_PRICE_SOURCE_IDX:
            case SELL_PRICE_TIMESTAMP_IDX:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected Object getColumnValueAt(int modelRowIndex, int modelColumnIndex)
    {

        final TableEntry row = getRow( modelRowIndex );

        final PriceInfo buyPrice = row.getBuyPrice();
        final PriceInfo sellPrice = row.getSellPrice();

        switch ( modelColumnIndex )
        {
            case ITEM_NAME_IDX: // name
                return row.getItem().getName();

            case AVG_BUY_PRICE_IDX:
                return row.hasBuyPrice() ? AmountHelper.formatISKAmount( buyPrice
                        .getAveragePrice() ) : EMPTY_STRING;
            case BUY_PRICE_SOURCE_IDX:

                if ( ! row.hasBuyPrice() )
                {
                    return EMPTY_STRING;
                }

                return row.getBuyPrice().getSource().getDisplayName();
            case BUY_PRICE_TIMESTAMP_IDX:
                return row.hasBuyPrice() ? DATE_FORMAT.format( buyPrice.getTimestamp()
                        .getLocalTime() ) : EMPTY_STRING;

            case AVG_SELL_PRICE_IDX:
                return row.hasSellPrice() ? AmountHelper.formatISKAmount( sellPrice
                        .getAveragePrice() ) : EMPTY_STRING;
            case SELL_PRICE_SOURCE_IDX:
                if ( ! row.hasSellPrice() )
                {
                    return EMPTY_STRING;
                }
                return row.getSellPrice().getSource().getDisplayName();
            case SELL_PRICE_TIMESTAMP_IDX:
                return row.hasSellPrice() ? DATE_FORMAT.format( sellPrice.getTimestamp()
                        .getLocalTime() ) : EMPTY_STRING;

            default:
                throw new IllegalArgumentException( "Invalid model column "
                        + modelColumnIndex );
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {

        switch ( columnIndex )
        {
            case AVG_BUY_PRICE_IDX:
            case AVG_SELL_PRICE_IDX:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex)
    {

        final TableEntry row = getRow( rowIndex );
        switch ( columnIndex )
        {
            case AVG_BUY_PRICE_IDX:
                updateAverageBuyPrice( row, (String) value );
                break;
            case AVG_SELL_PRICE_IDX:
                updateAverageSellPrice( row, (String) value );
                break;
            default:
                throw new UnsupportedOperationException( "Cannot edit value at column "
                        + columnIndex );
        }

    }

    protected void updateAverageBuyPrice(TableEntry row, String sPrice)
    {

        log.debug( "updateAverageBuyPrice(): User sets new avg. buy price " + sPrice
                + " for " + row.getBuyPrice() );
        final long price = row.parsePrice( sPrice );
        if ( price < 0 )
        {
            return;
        }

        log.debug( "updateAverageBuyPrice(): parsed price = " + price );

        PriceInfo info;
        if ( row.hasBuyPrice() )
        {
            info = row.buyPrice;
            info.setMaxPrice( 0 );
            info.setMinPrice( 0 );
        }
        else
        {
            row.buyPrice =
                    info = new PriceInfo( Type.BUY, row.type, Source.USER_PROVIDED );
            info.setRegion( getDefaultRegion() );
        }

        info.setTimestamp( new EveDate( clock ) );
        info.setSource( Source.USER_PROVIDED );
        info.setAveragePrice( price );

        getModel().valueChanged( row );

        priceInfoStore.store( info );
    }

    protected void updateAverageSellPrice(TableEntry row, String sPrice)
    {

        log.debug( "updateAverageSellPrice(): User sets new avg. sell price " + sPrice
                + " for " + row.getSellPrice() );

        final long price = row.parsePrice( sPrice );
        if ( price < 0 )
        {
            return;
        }

        log.debug( "updateAverageSellPrice(): parsed price = " + price );

        PriceInfo info;
        if ( row.hasSellPrice() )
        {
            info = row.sellPrice;
            info.setMaxPrice( 0 );
            info.setMinPrice( 0 );
        }
        else
        {
            row.sellPrice = info = new PriceInfo( Type.SELL, row.type, Source.USER_PROVIDED );
            info.setRegion( getDefaultRegion() );
        }

        info.setTimestamp( new EveDate( clock ) );
        info.setSource( Source.USER_PROVIDED );
        info.setAveragePrice( price );

        getModel().valueChanged( row );

        priceInfoStore.store( info );
    }

    protected Region getDefaultRegion()
    {
        return defaultRegion;
    }
}