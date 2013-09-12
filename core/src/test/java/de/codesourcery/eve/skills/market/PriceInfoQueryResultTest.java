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

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.market.impl.TestHelper;

public class PriceInfoQueryResultTest extends TestHelper {

	public void testEmptyWhenNew() {
		assertTrue( new PriceInfoQueryResult( ITEM1 , null , null ).isEmpty() );
	}
	
	public void testMergeBuyPrice() {
		
		PriceInfo existing =
			createPriceInfo(Type.BUY , ITEM1 , REGION1 );
		
		PriceInfoQueryResult merged =
			new PriceInfoQueryResult( ITEM1 , null , null )
			.merge( Type.BUY , existing );
		
		assertFalse( merged.hasSellPrice() );
		
		assertTrue( merged.hasBuyPrice() );
		assertSame( existing , merged.buyPrice() );
	}
	
	public void testMergeSellPrice() {
		
		PriceInfo existing =
			createPriceInfo(Type.SELL , ITEM1 , REGION1 );
		
		PriceInfoQueryResult merged =
			new PriceInfoQueryResult( ITEM1 , null , null ).merge( Type.SELL , existing );
		
		assertFalse( merged.hasBuyPrice() );
		
		assertTrue( merged.hasSellPrice() );
		assertSame( existing , merged.sellPrice() );
	}
}
