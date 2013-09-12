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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class RefiningResults
{

	private final InventoryType refinedItem;
	private final int unrefinedQuantity;
	private ISKAmount refinedValue;
	
	private List<RefiningResult> results=
		new ArrayList<RefiningResult>();
	
	public RefiningResults(InventoryType refinedItem,
			int unrefinedQuantity) 
	{
		
		if ( refinedItem == null ) {
			throw new IllegalArgumentException("refinedItem cannot be NULL");
		}
		if ( unrefinedQuantity < 0 ) {
			throw new IllegalArgumentException("unrefinedQuantity cannot be < 0 ");
		}
		this.refinedItem = refinedItem;
		this.unrefinedQuantity = unrefinedQuantity;
	}
	
	public void addResult(RefiningResult r) {
		if ( r == null ) {
			throw new IllegalArgumentException(
					"refining result cannot be NULL");
		}
		this.results.add( r );
	}

	public int getUnrefinedQuantity()
	{
		return unrefinedQuantity;
	}

	public InventoryType getRefinedItem()
	{
		return refinedItem;
	}
	
	public List<RefiningResult> getResults() {
		return Collections.unmodifiableList( results );
	}

	public void setRefinedValue(ISKAmount refinedValue)
	{
		this.refinedValue = refinedValue;
	}

	/**
	 * 
	 * @return value in ISK (might be NULL if 
	 * not calculated or material price(s) are unavailable)
	 */
	public ISKAmount getRefinedValue()
	{
		return refinedValue;
	}
	
}
