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

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.utils.EveDate;

public class MarketOrder
{
	/*
	 Output Rowset Columns
Name 	Type 	Description
orderID 	int 	Unique order ID for this order. Note that these are not guaranteed to be unique forever, they can recycle. But they are unique for the purpose of one data pull.
charID 	int 	ID of the character that physically placed this order.
stationID 	int 	ID of the station the order was placed in.
volEntered 	int 	Quantity of items required/offered to begin with.
volRemaining 	int 	Quantity of items still for sale or still desired.
minVolume 	int 	For bids (buy orders), the minimum quantity that must be sold in one sale in order to be accepted by this order.
orderState 	byte 	Valid states: 0 = open/active, 1 = closed, 
                    2 = expired (or fulfilled), 3 = cancelled, 4 = pending, 5 = character deleted.
typeID 	short 	ID of the type (references the invTypes table) of the items this order is buying/selling.
range 	short 	The range this order is good for. For sell orders, this is always 32767. For buy orders, allowed values are: -1 = station, 0 = solar system, 1 = 1 jump, 2 = 2 jumps, ..., 32767 = region.
accountKey 	short 	Which division this order is using as its account. Always 1000 for characters, but in the range 1000 to 1006 for corporations.
duration 	short 	How many days this order is good for. Expiration is issued + duration in days.
escrow 	decimal 	How much ISK is in escrow. Valid for buy orders only (I believe).
price 	decimal 	The cost per unit for this order.
bid 	bool 	If true, this order is a bid (buy order). Else, sell order.
issued 	datetime 	When this order was issued. 
	 */
	public enum OrderState
	{
		OPEN(0, "Open"), 
		CLOSED(1, "Closed"), 
		EXPIRED_FULFILLED(2,"Expired/Fulfilled"), 
		CANCELLED(3, "Cancelled"), 
		PENDING(4,"Pending"), 
		CHARACTER_DELETED(5, "Character deleted");

		private final int typeId;
		private final String displayName;

		private OrderState(int typeId, String displayName) {
			this.typeId = typeId;
			this.displayName = displayName;
		}

		public int getTypeId()
		{
			return typeId;
		}

		public String getDisplayName()
		{
			return displayName;
		}
		
		public static OrderState fromTypeId(int typeId) {
			for ( OrderState s : values() ) {
				if ( s.typeId == typeId ) {
					return s;
				}
			}
			throw new IllegalArgumentException("Unknown order state "+typeId);
		}
	}

	private long orderID;
	private CharacterID characterID;
	private Station station;
	private long volumeEntered;
	private long volumeRemaining;
	private long minVolume;
	private OrderState state;
	private InventoryType itemType;
	private int range;
	private int accountKey;
	private int durationInDays;
	private long moneyInEscrow;
	private long price;
	private PriceInfo.Type type;
	private EveDate issueDate;

	public long getOrderID()
	{
		return orderID;
	}

	public void setOrderID(long orderID)
	{
		this.orderID = orderID;
	}

	public CharacterID getCharacterID()
	{
		return characterID;
	}

	public void setCharacterID(CharacterID characterID)
	{
		this.characterID = characterID;
	}

	public Station getStation()
	{
		return station;
	}

	public void setStation(Station station)
	{
		this.station = station;
	}

	public long getVolumeEntered()
	{
		return volumeEntered;
	}

	public void setVolumeEntered(long volumeEntered)
	{
		this.volumeEntered = volumeEntered;
	}

	public long getVolumeRemaining()
	{
		return volumeRemaining;
	}

	public void setVolumeRemaining(long volumeRemaining)
	{
		this.volumeRemaining = volumeRemaining;
	}

	public long getMinVolume()
	{
		return minVolume;
	}

	public void setMinVolume(long minVolume)
	{
		this.minVolume = minVolume;
	}

	public OrderState getState()
	{
		return state;
	}

	public void setState(OrderState orderState)
	{
		this.state = orderState;
	}

	public InventoryType getItemType()
	{
		return itemType;
	}

	public void setItemType(InventoryType itemType)
	{
		this.itemType = itemType;
	}

	public int getRange()
	{
		return range;
	}

	public void setRange(int range)
	{
		this.range = range;
	}

	public int getAccountKey()
	{
		return accountKey;
	}

	public void setAccountKey(int accountKey)
	{
		this.accountKey = accountKey;
	}

	public int getDurationInDays()
	{
		return durationInDays;
	}

	public void setDurationInDays(int durationInDays)
	{
		this.durationInDays = durationInDays;
	}

	public long getMoneyInEscrow()
	{
		return moneyInEscrow;
	}

	public void setMoneyInEscrow(long moneyInEscrow)
	{
		this.moneyInEscrow = moneyInEscrow;
	}

	public long getPrice()
	{
		return price;
	}

	public void setPrice(long price)
	{
		this.price = price;
	}

	public PriceInfo.Type getType()
	{
		return type;
	}
	
	public boolean hasType(PriceInfo.Type type) {
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		type.assertNotAny();
		return getType() == type;
	}
	
	public boolean isSellOrder() {
		return hasType(PriceInfo.Type.SELL);
	}
	
	public boolean isBuyOrder() {
		return hasType(PriceInfo.Type.BUY);
	}

	public void setType(PriceInfo.Type type)
	{
		this.type = type;
	}

	/**
	 * Returns the date when this order was issued.
	 * @return
	 */
	public EveDate getIssueDate()
	{
		return issueDate;
	}

	/**
	 * Sets the date when this order was issued.
	 * @return
	 */
	public void setIssueDate(EveDate issueDate)
	{
		this.issueDate = issueDate;
	}

	public boolean hasState(OrderState... states)
	{
		if ( ArrayUtils.isEmpty( states ) ) {
			throw new IllegalArgumentException(
					"states array cannot be NULL/empty");
		}
		
		for ( OrderState s : states ) {
			if ( getState() == s ) {
				return true;
			}
		}
		return false;
	}

}
