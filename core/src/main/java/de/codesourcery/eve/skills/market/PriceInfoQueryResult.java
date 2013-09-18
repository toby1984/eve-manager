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
package de.codesourcery.eve.skills.market;

import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.utils.ISystemClock;

public final class PriceInfoQueryResult
{
    private static final Logger log = Logger.getLogger( PriceInfoQueryResult.class );

    /**
     * Age of a <code>PriceInfo</code> in milliseconds that must be exceeded in
     * order for the IMarketDataProvider to fetch updated data.
     */
    public static final long REFRESH_AGE = 2 * 24 * 60 * 60 * 1000;

    private final InventoryType item;
    private final PriceInfo buyPrice;
    private final PriceInfo sellPrice;

    public PriceInfoQueryResult(InventoryType item) {
        this.item = item;
        this.buyPrice = this.sellPrice = null;
    }

    public PriceInfoQueryResult(PriceInfo buyPrice, PriceInfo sellPrice) {
        this( null, buyPrice, sellPrice );
    }

    public boolean containsMatchingPrice(MarketFilter filter)
    {

        switch ( filter.getOrderType() )
        {
            case BUY:
                return hasBuyPrice() && matches( filter, buyPrice );
            case SELL:
                return hasSellPrice() && matches( filter, sellPrice );
            case ANY:
                return ( hasBuyPrice() && matches( filter, buyPrice ) )
                        && ( hasSellPrice() && matches( filter, sellPrice ) );
            default:
                throw new RuntimeException( "Unhandled switch/case: "
                        + filter.getOrderType() );
        }
    }

    protected boolean matches(MarketFilter filter, PriceInfo info)
    {

        if ( info == null )
        {
            return false;
        }

        if ( filter.getOrderType() != Type.ANY )
        {
            if ( ! filter.hasOrderType( info.getPriceType() ) )
            {
                return false;
            }
        }

        if ( Region.isSameRegion( filter.getRegion(), info.getRegion() ) )
        {
            return false;
        }
        return true;
    }

    public PriceInfoQueryResult(List<PriceInfo> infos) {
        if ( infos.isEmpty() )
        {
            throw new IllegalArgumentException(
                    "You need to provide an item with the ( InventoryType,List )"
                            + " constructor when passing no price infos." );
        }

        if ( infos.size() > 2 )
        { // can be at most one buy and one sell price
            throw new IllegalArgumentException( "More than 2 prices ?" );
        }

        buyPrice = PriceInfo.findBuyPrice( infos );
        sellPrice = PriceInfo.findSellPrice( infos );

        if ( buyPrice == null && sellPrice == null )
        {
            throw new RuntimeException( "Unreachable code reached?" );
        }

        item = buyPrice != null ? buyPrice.getItemType() : sellPrice.getItemType();
    }

    public PriceInfoQueryResult merge(PriceInfo.Type mergeType, PriceInfo infoFromServer)
    {

        if ( log.isTraceEnabled() )
        {
            log.trace( "merge(): merge type = " + mergeType );
        }

        if ( mergeType != PriceInfo.Type.ANY && ! infoFromServer.hasType( mergeType ) )
        {
            if ( log.isTraceEnabled() )
            {
                log.trace( "merge(): returning 'this' : " + this );
            }
            return this;
        }

        if ( infoFromServer.hasType( Type.BUY ) )
        {
            if ( log.isTraceEnabled() )
            {
                log.trace( "merge(): merging buy price" );
            }
            return new PriceInfoQueryResult( this.item, infoFromServer, this.sellPrice );
        }
        else if ( infoFromServer.hasType( Type.SELL ) )
        {
            if ( log.isTraceEnabled() )
            {
                log.trace( "merge(): merging sell price." );
            }
            return new PriceInfoQueryResult( this.item, this.buyPrice, infoFromServer );
        }
        else
        {
            throw new RuntimeException( "Unhandled type " + infoFromServer.getPriceType() );
        }
    }

    public PriceInfoQueryResult(InventoryType type, List<PriceInfo> infos) {

        if ( type == null )
        {
            throw new IllegalArgumentException( "item type cannot be NULL" );
        }

        buyPrice = PriceInfo.findBuyPrice( infos );
        sellPrice = PriceInfo.findSellPrice( infos );
        this.item = type;
    }

    public PriceInfoQueryResult(InventoryType item, PriceInfo buyPrice,
            PriceInfo sellPrice) {

        InventoryType foundItem = item;
        if ( item == null )
        {

            if ( sellPrice != null )
            {
                foundItem = sellPrice.getItemType();
            }
            else if ( buyPrice != null )
            {
                foundItem = buyPrice.getItemType();
            }
            else
            {
                throw new IllegalArgumentException( "item cannot be NULL" );
            }
        }

        this.item = foundItem;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public PriceInfoQueryResult(PriceInfoQueryResult cached) {
        this.buyPrice = new PriceInfo( cached.buyPrice );
        this.sellPrice = new PriceInfo( cached.sellPrice );
        this.item = cached.item;
    }

    public boolean hasBuyPrice()
    {
        return buyPrice != null;
    }

    public InventoryType getItem()
    {
        return item;
    }

    public boolean hasNoBuyPrice()
    {
        return ! hasBuyPrice();
    }

    public boolean isEmpty()
    {
        return hasNoBuyPrice() && hasNoSellPrice();
    }

    public boolean hasSellPrice()
    {
        return sellPrice != null;
    }

    public boolean hasNoSellPrice()
    {
        return ! hasSellPrice();
    }

    public PriceInfo buyPrice()
    {
        if ( buyPrice == null )
        {
            throw new IllegalStateException( "No buy price available for " + this.item );
        }
        return buyPrice;
    }

    public PriceInfo sellPrice()
    {
        if ( sellPrice == null )
        {
            throw new IllegalStateException( "No sell price available for " + this.item );
        }
        return sellPrice;
    }

    public boolean isOutdated(ISystemClock clock)
    {
        return hasOutdatedBuyPrice( clock ) || hasOutdatedSellPrice( clock );
    }

    public boolean hasOutdatedBuyPrice(ISystemClock clock)
    {
        return hasBuyPrice() && isOutdated( buyPrice(), clock );
    }

    public boolean hasOutdatedSellPrice(ISystemClock clock)
    {
        return hasSellPrice() && isOutdated( sellPrice(), clock );
    }

    public static boolean isOutdated(PriceInfo info, ISystemClock clock)
    {
        return info.getTimestamp().getAgeInMillis( clock ) > REFRESH_AGE;
    }

    @Override
    public String toString()
    {
        return "PriceInfoQueryResult[ buyPrice = " + buyPrice + " , sellPrice = "
                + sellPrice + "]";
    }
}
