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
package de.codesourcery.eve.skills.production;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class RefiningResult 
{
	private final InventoryType type;
	private int yourQuantity;
	private int perfectQuantity;
	private final float stationTaxFactor;
	private ISKAmount sellValue;
	
	public RefiningResult(InventoryType type, int yourQuantity, int perfectQuantity,float stationTaxFactor) {
		
		if ( type == null ) 
		{
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		if ( perfectQuantity < yourQuantity ) {
			throw new IllegalArgumentException("perfectQuantity cannot be less than your quantity");
		}

		if ( stationTaxFactor < 0.0f || stationTaxFactor > 0.05f ) {
			throw new IllegalArgumentException("Invalid station tax "+stationTaxFactor*100.0f+" %");
		}
		this.stationTaxFactor = stationTaxFactor;
		this.type = type;
		setPerfectQuantity( perfectQuantity );
		setYourQuantity(yourQuantity);
	}
	
	public int getYourQuantityMinusStationTax() {
		final int stationTakes = (int) Math.round( getYourQuantity() * getStationTaxFactor() );
		return getYourQuantity() - stationTakes;
	}

	public float getStationTaxFactor()
	{
		return stationTaxFactor;
	}
	
	public InventoryType getType()
	{
		return type;
	}

	public void setPerfectQuantity(int perfectQuantity)
	{
		if ( perfectQuantity < 0 ) {
			throw new IllegalArgumentException("perfectQuantity cannot be < 0");
		}
		this.perfectQuantity = perfectQuantity;
	}
	
	public void setYourQuantity(int yourQuantity)
	{
		if ( yourQuantity < 0 ) {
			throw new IllegalArgumentException("yourQuantity cannot be < 0 ");
		}
		this.yourQuantity = yourQuantity;
	}
	
	public int getYourQuantity()
	{
		return yourQuantity;
	}

	public int getPerfectQuantity()
	{
		return perfectQuantity;
	}

	public void setSellValue(ISKAmount sellValue)
	{
		this.sellValue = sellValue;
	}

	/**
	 * This refining result's total sell value.
	 * 
	 * @return ISK amount or <code>null</code> if value
	 * is unknown or hasn't been determined
	 */
	public ISKAmount getSellValue()
	{
		return sellValue;
	}
}
