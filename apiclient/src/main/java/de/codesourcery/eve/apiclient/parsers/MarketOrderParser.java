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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.MarketOrder;
import de.codesourcery.eve.skills.datamodel.MarketOrder.OrderState;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class MarketOrderParser extends AbstractResponseParser<List<MarketOrder>>
{

	public static final URI uri = toURI("/char/MarketOrders.xml.aspx");
	
	private List<MarketOrder> result;
	private final IStaticDataModel dataModel;
	
	public MarketOrderParser(IStaticDataModel dataModel,ISystemClock clock) {
		super(clock);
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		if ( clock == null ) {
			throw new IllegalArgumentException("clock cannot be NULL");
		}
		this.dataModel = dataModel;
	}
	
	/*
<eveapi version="2">
<currentTime>2008-02-04 13:28:18</currentTime>
<result>
<rowset name="orders" key="orderID" columns="orderID,charID,stationID,
volEntered,volRemaining,minVolume,orderState,typeID,range,accountKey,duration,escrow,price,bid,issued">

<row orderID="639356913" charID="118406849" stationID="60008494" 
   volEntered="25" volRemaining="18" minVolume="1" orderState="0" typeID="26082" 
   range="32767" accountKey="1000" duration="3" escrow="0.00" 
    price="3398000.00" bid="0" issued="2008-02-03 13:54:11"/>

<row orderID="639477821" charID="118406849" stationID="60004357" volEntered="25" volRemaining="24" minVolume="1" orderState="0" typeID="26082" range="32767" accountKey="1000" duration="3" escrow="0.00" price="3200000.00" bid="0" issued="2008-02-02 16:39:25"/>
<row orderID="639587440" charID="118406849" stationID="60003760" volEntered="25" volRemaining="4" minVolume="1" orderState="0" typeID="26082" range="32767" accountKey="1000" duration="1" escrow="0.00" price="3399999.98" bid="0" issued="2008-02-03 22:35:54"/>
</rowset>
</result>
<cachedUntil>2008-02-04 14:28:18</cachedUntil>
</eveapi>
	 */
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException
	{
	
		final List<MarketOrder> tmpResult = 
			new ArrayList<MarketOrder>();
		
		for ( Row row : parseRowSet("orders" , document ) ) {
			final MarketOrder order = new MarketOrder();
			
			order.setOrderID( row.getLong("orderID" ) );
			order.setCharacterID( new CharacterID( row.get("charID" ) ) );
			order.setStation( dataModel.getStation( row.getLong("stationID") ) );
			order.setVolumeEntered( row.getLong("volEntered" ) );
			order.setVolumeRemaining( row.getLong("volRemaining" ) );
			order.setMinVolume( row.getLong("minVolume" ) );
			order.setState( OrderState.fromTypeId( row.getInt("orderState" ) ) );
			order.setItemType( dataModel.getInventoryType( row.getLong("typeID" ) ) );
			order.setRange( row.getInt("range" ) );
			order.setAccountKey( row.getInt("accountKey" ) );
			order.setDurationInDays( row.getInt("duration" ) );
			order.setMoneyInEscrow( row.getISKAmount( "escrow" ) );
			order.setPrice( row.getISKAmount( "price") );
			
			final int isBid = row.getInt( "bid" );
			order.setType( isBid == 0 ? Type.SELL : Type.BUY );
			order.setIssueDate( row.getDate("issued" ) );
			
			tmpResult.add( order );
		}
		this.result = tmpResult;
	}

	@Override
	public URI getRelativeURI()
	{
		return uri;
	}

	@Override
	public List<MarketOrder> getResult() throws IllegalStateException
	{
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset()
	{
		result = null;
	}

}
