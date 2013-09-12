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
import de.codesourcery.eve.skills.datamodel.CorporationId;
import de.codesourcery.eve.skills.datamodel.IClientId;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.MarketTransaction;
import de.codesourcery.eve.skills.datamodel.PriceInfo.Type;
import de.codesourcery.eve.skills.utils.ISKAmount;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class WalletTransactionsParser extends
		AbstractResponseParser<List<MarketTransaction>>
{

	public static final URI uri = toURI("/char/WalletTransactions.xml.aspx");

	private final IStaticDataModel dataModel;
	private List<MarketTransaction> result;
	
	
	public WalletTransactionsParser(IStaticDataModel dataModel , ISystemClock clock) {
		super(clock);
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.dataModel = dataModel;
	}

	/*
<eveapi version="1">
  <currentTime>2007-06-18 22:38:52</currentTime>
  <result>
    <rowset name="transactions">
      <row transactionDateTime="2008-06-15 09:27:00" transactionID="661583821" quantity="1"
           typeName="Medium Hull Repairer I" typeID="3653" price="100000.00" clientID="1113473668"
           clientName="Spencer Noffke" stationID="60011749"
           stationName="Luminaire VII - Moon 6 - Federation Navy Assembly Plant"
           transactionType="sell" transactionFor="personal" />
      <row transactionDateTime="2008-06-15 08:57:00" transactionID="661564124" quantity="1"
           typeName="Photon Scattering Field I" typeID="2293" price="100000.00" clientID="1542532375"
           clientName="Cpt Luna" stationID="60011749"
           stationName="Luminaire VII - Moon 6 - Federation Navy Assembly Plant"
           transactionType="sell" transactionFor="personal" />
      <row transactionDateTime="2008-05-26 19:21:00" transactionID="645542110" quantity="1"
           typeName="Gallente Shuttle" typeID="11129" price="25000.00" clientID="811272028"
           clientName="Quark Kunnin" stationID="60009367"
           stationName="Inghenges VII - Federal Freight Storage"
           transactionType="buy" transactionFor="personal" />
      <row transactionDateTime="2007-12-30 14:46:00" transactionID="514892332" quantity="486"
           typeName="Gallente Federation Starbase Charter" typeID="24594" price="801.02" clientID="802387143"
           clientName="Wnsnte" stationID="60011737"
           stationName="Oursulaert VII - Moon 1 - Federation Navy Testing Facilities"
           transactionType="buy" transactionFor="corporation" />
    </rowset>
  </result>
  <cachedUntil>2007-06-18 22:36:09</cachedUntil>
</eveapi>
	 
	 */
	@Override
	void parseHook(Document document) throws UnparseableResponseException
	{

		final List<MarketTransaction> tmpResult = new ArrayList<MarketTransaction>();
		for ( Row r : parseRowSet("transactions", document ) ) 
		{
			final MarketTransaction t = 
				new MarketTransaction();
			
			t.setTransactionDate( r.getDate("transactionDateTime" ) );
			t.setTransactionId( r.getLong("transactionID" ) );
			t.setQuantity( r.getInt("quantity" ) );
			if ( r.hasColumn("clientName" ) ) {
				t.setClientName( r.get("clientName" ) );
			} else {
				t.setClientName( "<unknown>" );
			}
			t.setItemType( dataModel.getInventoryType( r.getLong("typeID" ) ) );
			t.setPrice( new ISKAmount( r.getISKAmount("price" ) ) );
			
			final long clientId = r.getLong("clientID");
			final String clientType = 
				r.get("transactionFor");
			
			final IClientId id = new IClientId() {

				@Override
				public CharacterID asCharacterId()
				{
					return new CharacterID( Long.toString( clientId ) );
				}

				@Override
				public CorporationId asCorporationId()
				{
					return new CorporationId( clientId );
				}

				@Override
				public boolean isCharacterId()
				{
					return true;
				}

				@Override
				public boolean isCorporationId()
				{
					return true;
				}
			};
			t.setClientId( id );
			
			if ( "personal".equalsIgnoreCase( clientType ) ) {
				t.setCorporateTransaction(false);
			} 
			else if ( "corporation".equalsIgnoreCase( clientType ) ) {
				t.setCorporateTransaction(true);
			} else {
				throw new UnparseableResponseException("Unknown client type >"+clientType+"<");
			}
			t.setStation( dataModel.getStation( r.getLong("stationID" ) ) );
			
			final String transactionType=
				r.get("transactionType");
			
			if ( "buy".equalsIgnoreCase( transactionType ) ) {
				t.setOrderType(Type.BUY);
			} else if ( "sell".equalsIgnoreCase( transactionType ) ) {
				t.setOrderType(Type.SELL);
			} else {
				throw new UnparseableResponseException("Unknown transaction type >"+transactionType+"<");
			}
			
			tmpResult.add( t );
		}
		
		result = tmpResult;
	}

	@Override
	public URI getRelativeURI()
	{
		return uri;
	}

	@Override
	public List<MarketTransaction> getResult() throws IllegalStateException
	{
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset()
	{
		result = null;
	}

	/*
	 */
}
