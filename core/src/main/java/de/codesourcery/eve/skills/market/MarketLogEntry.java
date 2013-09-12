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


import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.utils.EveDate;


public class MarketLogEntry {

	private long orderId;
	private double remainingVolume;
	private double price;
	private PriceInfo.Type type;
	private double volume;
	private long minVolume;
	private EveDate issueDate;
	private int orderCount=1;

	public MarketLogEntry() {
	}
	
	@Override
	public String toString()
	{
		return ReflectionToStringBuilder.reflectionToString( this );
	}

	public void mergeWith(MarketLogEntry other) {
		if ( this.getType() != other.getType() ) {
			throw new IllegalArgumentException("Cannot join orders of different type");
		}
	
		if ( ! EveDate.isSameDay( this.getIssueDate(), other.getIssueDate() ) ) {
			throw new IllegalArgumentException("Cannot " +
					"join orders with " +
					"different timestamps, got "+this.getIssueDate().getLocalTime()+" <-> "+
					other.getIssueDate().getLocalTime() );
		}
		
		remainingVolume+= other.remainingVolume;
		price += other.price;
		volume += other.volume;
		if ( other.minVolume > this.minVolume ) {
			this.minVolume = other.minVolume;
		}
		
		// remember latest date when merging
		if ( other.issueDate.compareTo( this.issueDate ) > 0  ) {
			this.issueDate = other.issueDate;
		}
		orderCount+= other.orderCount;
	}
	
	public void mergingFinished() {
		this.price = ( this.price / (double) orderCount );
	}
	
	public boolean hasType(PriceInfo.Type type) {
		return getType() == type;
	}
	
	public PriceInfo.Type getType() {
		return type;
	}
	
	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public boolean isBuyOrder() {
		return type == PriceInfo.Type.BUY;
	}

	public boolean isSellOrder() {
		return ! isBuyOrder();
	}

	public void setBuyOrder(boolean isBuyOrder) {
		this.type = isBuyOrder ? Type.BUY : Type.SELL;
	}

	public double getVolume() {
		return volume;
	}

	public long getMinVolume() {
		return minVolume;
	}

	public void setMinVolume(long minVolume) {
		this.minVolume = minVolume;
	}

	/**
	 * Returns the date when this order was issued (LOCAL TIME).
	 * @return
	 */
	public EveDate getIssueDate() {
		return issueDate;
	}

	/**
	 * Sets the date when this order was issued.
	 * @return
	 */	
	public void setIssueDate(EveDate issueDate) {
		this.issueDate = issueDate;
	}

	public void setRemainingVolume(double remainingVolume) {
		this.remainingVolume = remainingVolume;
	}

	public double getRemainingVolume() {
		return remainingVolume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public int getOrderCount() {
		return orderCount;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public long getOrderId() {
		return orderId;
	}

}