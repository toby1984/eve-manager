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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.FactionStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.Faction;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class FactionStandingParser extends AbstractResponseParser<FactionStandings> {
	
	public static final Logger log = Logger.getLogger(FactionStandingParser.class);

	public static final URI URI = toURI("/char/Standings.xml.aspx");

	private FactionStandings facStandings;

	/* NEW
	 * 
	 * <?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2013-09-12 14:52:13</currentTime>
  <result>
    <characterNPCStandings>
      <rowset name="agents" key="fromID" columns="fromID,fromName,standing">
        <row fromID="3008735" fromName="Avenid Voohah" standing="1.20" />
        <row fromID="3008738" fromName="Alaz Chaktaren" standing="5.85" />
        <row fromID="3008739" fromName="Orda Sand" standing="4.98" />
        <row fromID="3008859" fromName="Ohtar Defsunun" standing="1.39" />
        <row fromID="3008863" fromName="Farela Laresh" standing="5.18" />
        <row fromID="3008869" fromName="Siktinu Verk" standing="0.77" />
        <row fromID="3008872" fromName="Opoumouh Bemarah" standing="-0.07" />
        <row fromID="3008913" fromName="Urotak Jibila" standing="7.58" />
        <row fromID="3008916" fromName="Almananeg Erafeke" standing="0.71" />
        <row fromID="3008917" fromName="Monoth Khianasha" standing="3.28" />
        <row fromID="3008918" fromName="Narshandyn Pehibiz" standing="5.59" />
        <row fromID="3008919" fromName="Angohmateh Jibila" standing="0.13" />
        <row fromID="3008932" fromName="Amis Arvadan" standing="6.85" />
        <row fromID="3009977" fromName="Blartulla Eksmon" standing="1.82" />
        <row fromID="3009994" fromName="Orward Eolmulf" standing="0.46" />
        <row fromID="3010112" fromName="Lulmuna Eilirmuald" standing="0.02" />
        <row fromID="3010118" fromName="Beomilar Brettirdur" standing="0.30" />
        <row fromID="3012113" fromName="Osala Iskio" standing="6.87" />
        <row fromID="3012114" fromName="Koskanaiken Vilima" standing="1.28" />
        <row fromID="3013816" fromName="Ershol Arajah" standing="2.59" />
        <row fromID="3013947" fromName="Sirkild Teriner" standing="9.88" />
        <row fromID="3013948" fromName="Grie Baun" standing="0.28" />
        <row fromID="3013965" fromName="Unuter Hureirik" standing="8.45" />
        <row fromID="3014480" fromName="Nambouk Memi" standing="0.41" />
        <row fromID="3014643" fromName="Atmiel Hakarvin" standing="0.17" />
        <row fromID="3014772" fromName="Erafeke Ivah" standing="1.03" />
        <row fromID="3014881" fromName="Shetis Akabantan" standing="2.26" />
        <row fromID="3014900" fromName="Fasri Horshandas" standing="3.92" />
        <row fromID="3014903" fromName="Azih Joelrie" standing="0.56" />
        <row fromID="3014905" fromName="Hekhem Moranour" standing="0.35" />
        <row fromID="3015008" fromName="Sharenisali Sirjariad" standing="2.18" />
        <row fromID="3015013" fromName="Ashek Ohtouh" standing="0.56" />
        <row fromID="3015058" fromName="Samshek Taronzac" standing="1.88" />
        <row fromID="3015063" fromName="Padam Yanabas" standing="4.15" />
        <row fromID="3015066" fromName="Ourarot Oshiye" standing="5.96" />
        <row fromID="3015073" fromName="Zashada Gebase" standing="8.03" />
        <row fromID="3015074" fromName="Hanek Sabirahr" standing="-0.10" />
        <row fromID="3015689" fromName="Fitsyoo Valou" standing="2.41" />
        <row fromID="3016551" fromName="Outoras Ajanen" standing="-0.05" />
        <row fromID="3016886" fromName="Vasanen Vahoras" standing="2.52" />
        <row fromID="3016899" fromName="Kovula Ylatera" standing="5.42" />
        <row fromID="3016901" fromName="Saijimo Ukkuken" standing="5.13" />
        <row fromID="3016908" fromName="Harkuma Uesimaanen" standing="0.27" />
        <row fromID="3016909" fromName="Heikama Toikiainen" standing="8.87" />
        <row fromID="3016910" fromName="Yanakka Ijinen" standing="5.67" />
        <row fromID="3017149" fromName="Tohanala Pukegainen" standing="0.93" />
        <row fromID="3017550" fromName="Eitorkur Ormin" standing="0.02" />
        <row fromID="3017582" fromName="Molea Sigaza" standing="0.55" />
        <row fromID="3017615" fromName="Krigasand Eran" standing="0.30" />
        <row fromID="3017670" fromName="Musinner Hotalda" standing="2.20" />
        <row fromID="3018682" fromName="Nagamac Lava" standing="10.00" />
        <row fromID="3018690" fromName="Ezia Minaru" standing="0.55" />
        <row fromID="3018810" fromName="Futa Rura" standing="0.20" />
        <row fromID="3018924" fromName="Zidah Arvo" standing="1.93" />
      </rowset>
      <rowset name="NPCCorporations" key="fromID" columns="fromID,fromName,standing">
        <row fromID="1000020" fromName="Lai Dai Corporation" standing="0.00" />
        <row fromID="1000039" fromName="Home Guard" standing="8.26" />
        <row fromID="1000041" fromName="Spacelane Patrol" standing="6.08" />
        <row fromID="1000063" fromName="Amarr Constructions" standing="0.57" />
        <row fromID="1000067" fromName="Zoar and Sons" standing="0.02" />
        <row fromID="1000074" fromName="Joint Harvesting" standing="0.01" />
        <row fromID="1000077" fromName="Royal Amarr Institute" standing="0.25" />
        <row fromID="1000080" fromName="Ministry of War" standing="0.04" />
        <row fromID="1000082" fromName="Ministry of Internal Order" standing="0.21" />
        <row fromID="1000084" fromName="Amarr Navy" standing="5.04" />
        <row fromID="1000085" fromName="Court Chamberlain" standing="5.44" />
        <row fromID="1000088" fromName="Sarum Family" standing="1.44" />
        <row fromID="1000091" fromName="Tash-Murkon Family" standing="2.25" />
        <row fromID="1000092" fromName="Civic Court" standing="0.00" />
        <row fromID="1000093" fromName="Theology Council" standing="9.05" />
        <row fromID="1000123" fromName="Ammatar Fleet" standing="8.66" />
        <row fromID="1000124" fromName="Archangels" standing="-0.09" />
        <row fromID="1000126" fromName="Ammatar Consulate" standing="9.24" />
        <row fromID="1000127" fromName="Guristas" standing="-0.05" />
        <row fromID="1000162" fromName="True Power" standing="-0.16" />
      </rowset>
      <rowset name="factions" key="fromID" columns="fromID,fromName,standing">
        <row fromID="500001" fromName="Caldari State" standing="4.05" />
        <row fromID="500002" fromName="Minmatar Republic" standing="-5.98" />
        <row fromID="500003" fromName="Amarr Empire" standing="5.14" />
        <row fromID="500004" fromName="Gallente Federation" standing="-7.03" />
        <row fromID="500005" fromName="Jove Empire" standing="0.04" />
        <row fromID="500007" fromName="Ammatar Mandate" standing="6.06" />
        <row fromID="500008" fromName="Khanid Kingdom" standing="1.84" />
        <row fromID="500009" fromName="The Syndicate" standing="-1.13" />
        <row fromID="500010" fromName="Guristas Pirates" standing="-9.20" />
        <row fromID="500011" fromName="Angel Cartel" standing="-4.29" />
        <row fromID="500012" fromName="Blood Raider Covenant" standing="-9.80" />
        <row fromID="500013" fromName="The InterBus" standing="0.75" />
        <row fromID="500014" fromName="ORE" standing="-2.07" />
        <row fromID="500015" fromName="Thukker Tribe" standing="-7.00" />
        <row fromID="500016" fromName="Servant Sisters of EVE" standing="-0.48" />
        <row fromID="500017" fromName="The Society of Conscious Thought" standing="0.07" />
        <row fromID="500018" fromName="Mordu's Legion Command" standing="4.48" />
        <row fromID="500019" fromName="Sansha's Nation" standing="-9.96" />
        <row fromID="500020" fromName="Serpentis" standing="-0.69" />
      </rowset>
    </characterNPCStandings>
  </result>
  <cachedUntil>2013-09-12 17:49:13</cachedUntil>
</eveapi>
	 */
	private final ICharacter character; 
	private final IStaticDataModel staticDataModel;
	
	public FactionStandingParser(ICharacter character , IStaticDataModel staticDataModel,ISystemClock clock) 
	{
		super( clock );
		if (staticDataModel == null) {
			throw new IllegalArgumentException("Static datamodel cannot be NULL");
		}
		if ( character == null ) {
			throw new IllegalArgumentException("character must not be null");
		}
		this.character = character;
		this.staticDataModel = staticDataModel;
	}
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException {
		
		facStandings = new FactionStandings(this.character);

		final Element parent = getChild( getResultElement( document ) , "characterNPCStandings" );
		final Element rowSetNode = getRowSetNode( parent , "factions" );
		
		for ( Row r : parseRowSet( "factions", rowSetNode ) ) 
		{
			long factionId = r.getLong( "fromID" );
			final float standing = r.getFloat( "standing" );
			
			final Standing<Faction> s =
				new Standing<Faction>( staticDataModel.getFaction( factionId ) );
			
			s.setValue( standing );
			
			facStandings.addFactionStanding( s );
		}		
	}

	@Override
	public URI getRelativeURI() {
		return URI;
	}

	@Override
	public FactionStandings getResult() throws IllegalStateException {
		assertResponseParsed();
		return facStandings;
	}

	@Override
	public void reset() {
		facStandings = null;
	}
}