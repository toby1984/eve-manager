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
package de.codesourcery.eve.skills.datamodel;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class MarketTransaction
{
	/*
	 * <eveapi version="1"> <currentTime>2007-06-18 22:38:52</currentTime>
	 * <result> <rowset name="transactions"> <row
	 * transactionDateTime="2008-06-15 09:27:00" transactionID="661583821"
	 * quantity="1" typeName="Medium Hull Repairer I" typeID="3653"
	 * price="100000.00" clientID="1113473668" clientName="Spencer Noffke"
	 * stationID="60011749"
	 * stationName="Luminaire VII - Moon 6 - Federation Navy Assembly Plant"
	 * transactionType="sell" transactionFor="personal" /> <row
	 * transactionDateTime="2008-06-15 08:57:00" transactionID="661564124"
	 * quantity="1" typeName="Photon Scattering Field I" typeID="2293"
	 * price="100000.00" clientID="1542532375" clientName="Cpt Luna"
	 * stationID="60011749"
	 * stationName="Luminaire VII - Moon 6 - Federation Navy Assembly Plant"
	 * transactionType="sell" transactionFor="personal" /> <row
	 * transactionDateTime="2008-05-26 19:21:00" transactionID="645542110"
	 * quantity="1" typeName="Gallente Shuttle" typeID="11129" price="25000.00"
	 * clientID="811272028" clientName="Quark Kunnin" stationID="60009367"
	 * stationName="Inghenges VII - Federal Freight Storage"
	 * transactionType="buy" transactionFor="personal" /> <row
	 * transactionDateTime="2007-12-30 14:46:00" transactionID="514892332"
	 * quantity="486" typeName="Gallente Federation Starbase Charter"
	 * typeID="24594" price="801.02" clientID="802387143" clientName="Wnsnte"
	 * stationID="60011737"
	 * stationName="Oursulaert VII - Moon 1 - Federation Navy Testing Facilities"
	 * transactionType="buy" transactionFor="corporation" /> </rowset> </result>
	 * <cachedUntil>2007-06-18 22:36:09</cachedUntil> </eveapi>
	 */

	/*
	 * transactionDateTime date string Date & time of the transaction
	 * transactionID int Transaction ID. Guaranteed to be unique with this page
	 * call; subject to renumbering periodically. Use the last listed
	 * transactionID with the beforeTransID argument to walk the list. See
	 * Journal Walking. quantity int Number of items bought/sold. typeName
	 * string Name of item bought/sold. typeID int ID of item. See invTypes
	 * table. price decimal The amount per unit paid. clientID int Character or
	 * corporation ID of the other party. If buying from an NPC corporation, see
	 * crpNPCCorporations and eveNames. clientName string Name of other party.
	 * stationID int Station in which the transaction took place. See
	 * staStations. stationName string Name of the station in which the
	 * transaction took place. transactionType string "buy" or "sell"
	 * transactionFor string "personal" or "corporation"
	 */

	private EveDate transactionDate;
	private long transactionId;
	private int quantity;
	private InventoryType itemType;
	private ISKAmount price;
	private IClientId clientId;
	private String clientName;
	private Station station;
	private PriceInfo.Type orderType;
	private boolean corporateTransaction;

	public EveDate getTransactionDate()
	{
		return transactionDate;
	}

	public void setTransactionDate(EveDate transactionDate)
	{
		this.transactionDate = transactionDate;
	}

	public long getTransactionId()
	{
		return transactionId;
	}

	public void setTransactionId(long transactionId)
	{
		this.transactionId = transactionId;
	}

	public int getQuantity()
	{
		return quantity;
	}

	public void setQuantity(int quantity)
	{
		this.quantity = quantity;
	}

	public InventoryType getItemType()
	{
		return itemType;
	}

	public void setItemType(InventoryType itemType)
	{
		this.itemType = itemType;
	}

	public ISKAmount getPrice()
	{
		return price;
	}

	public void setPrice(ISKAmount price)
	{
		this.price = price;
	}

	public IClientId getClientId()
	{
		return clientId;
	}

	public void setClientId(IClientId clientId)
	{
		this.clientId = clientId;
	}

	public String getClientName()
	{
		return clientName;
	}

	public void setClientName(String clientName)
	{
		this.clientName = clientName;
	}

	public Station getStation()
	{
		return station;
	}

	public void setStation(Station station)
	{
		this.station = station;
	}

	public PriceInfo.Type getOrderType()
	{
		return orderType;
	}

	public void setOrderType(PriceInfo.Type orderType)
	{
		this.orderType = orderType;
	}

	public boolean isCorporateTransaction()
	{
		return corporateTransaction;
	}
	
	public boolean isPersonalTransaction() {
		return ! corporateTransaction;
	}

	public void setCorporateTransaction(boolean corporateTransaction)
	{
		this.corporateTransaction = corporateTransaction;
	}

}
