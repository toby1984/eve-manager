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
package de.codesourcery.eve.skills.utils;

public class ISKAmount implements Comparable<ISKAmount>
{

	public static final ISKAmount ZERO_ISK = new ISKAmount(0) {
		
		public ISKAmount addTo(ISKAmount amount) {
			return amount;
		}
		
		@Override
		public ISKAmount multiplyBy(long arg0)
		{
			return this;
		}
		
	};
	
	private final long amount;
	
	public ISKAmount(long amount) {
		this.amount = amount;
	}
	
	public ISKAmount(double amount) {
		this.amount = (long) ( 100.0d*amount);
	}
	
	public ISKAmount addTo(ISKAmount amount) {
		if ( amount == ZERO_ISK ) {
			return this;
		}
		return new ISKAmount( this.amount + amount.amount );
	}
	
	public ISKAmount multiplyBy(long value) {
		return new ISKAmount( this.amount * value );
	}
	
	public ISKAmount multiplyBy(double value) {
		return new ISKAmount( (long) ( this.amount * value ) );
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof ISKAmount ) {
			return ( this.amount == ((ISKAmount) obj).amount);
		}
		return false;
	}
	
	public double toDouble() {
		return amount/100.d;
	}
	
	@Override
	public String toString()
	{
		return Long.toString( amount/100 ) +" ISK";
	}

	@Override
	public int compareTo(ISKAmount o)
	{
		if ( this.amount > o.amount ) {
			return 1;
		} else if ( this.amount < o.amount ) {
			return -1;
		}
		return 0;
	}

	public ISKAmount divideBy(int quantity)
	{
		final double value = toDouble() / quantity;
		return new ISKAmount( value );
	}

	public long toLong()
	{
		return this.amount;
	}
}
