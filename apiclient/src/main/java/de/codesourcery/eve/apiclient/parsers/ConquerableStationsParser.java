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
import de.codesourcery.eve.skills.datamodel.CorporationId;
import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class ConquerableStationsParser extends
        AbstractResponseParser<List<ConquerableStationsParser.Outpost>>
{

    public static final URI URI = toURI( "/eve/ConquerableStationList.xml.aspx" );

    private final IStaticDataModel dataModel;
    private List<Outpost> result = new ArrayList<Outpost>();

    public static final class Outpost implements ILocation
    {

        private final long id;
        private final CorporationId corporationId;
        private final String name;
        private final SolarSystem solarSystem;
        private final String corporationName;

        protected Outpost(long id, CorporationId corporationId, String name,
                SolarSystem solarSystem, String corporationName) {
            this.id = id;
            this.corporationId = corporationId;
            this.name = name;
            this.solarSystem = solarSystem;
            this.corporationName = corporationName;
        }

        @Override
        public String toString()
        {
            return "Outpost[ id=" + id + " , " + getDisplayName() + " ] ";
        }

        @Override
        public SolarSystem asSolarSystem()
        {
            return solarSystem;
        }

        public CorporationId getCorporationId()
        {
            return corporationId;
        }

        @Override
        public boolean equals(Object obj)
        {
            if ( obj instanceof Outpost )
            {
                return this.id == ( (Outpost) obj ).id;
            }
            return super.equals( obj );
        }

        @Override
        public int hashCode()
        {
            return (int) ( 31 + 31 * id );
        }

        @Override
        public Station asStation()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDisplayName()
        {
            return name + " - " + corporationName;
        }

        @Override
        public boolean isAnyLocation()
        {
            return false;
        }

        @Override
        public boolean isOutpost()
        {
            return true;
        }

        @Override
        public boolean isSolarSystem()
        {
            return false;
        }

        @Override
        public boolean isStation()
        {
            return false;
        }

        @Override
        public boolean isUnknown()
        {
            return false;
        }

        public long getId()
        {
            return id;
        }

        public Object getSolarSystem()
        {
            return solarSystem;
        }

        public String getName()
        {
            return this.name;
        }

        public String getCorporationName()
        {
            return corporationName;
        }

    }

    /*
    <?xml version='1.0' encoding='UTF-8'?>
    <eveapi version="1">
    <currentTime>2007-12-02 19:55:38</currentTime>
    <result>
    <rowset name="outposts" key="stationID" columns="stationID,stationName,stationTypeID,solarSystemID,corporationID,corporationName">
      <row stationID="60014862" stationName="0-G8NO VIII - Moon 1 - Manufacturing Outpost"
           stationTypeID="12242" solarSystemID="30000480" corporationID="1000135"
           corporationName="Serpentis Corporation" />
      <row stationID="60014863" stationName="4-EFLU VII - Moon 3 - Manufacturing Outpost"
           stationTypeID="12242" solarSystemID="30000576" corporationID="1000135"
           corporationName="Serpentis Corporation" />
      ...
      <row stationID="60014928" stationName="6T3I-L VII - Moon 5 - Cloning Outpost"
           stationTypeID="12295" solarSystemID="30004908" corporationID="1000135"
           corporationName="Serpentis Corporation" />
      <row stationID="61000001" stationName="DB1R-4 II - duperTum Corp Minmatar Service Outpost"
           stationTypeID="21646" solarSystemID="30004470" corporationID="150020944"
           corporationName="duperTum Corp" />
      <row stationID="61000002" stationName="ZS-2LT XI - duperTum Corp Minmatar Service Outpost"
           stationTypeID="21646" solarSystemID="30004469" corporationID="150020944"
           corporationName="duperTum Corp" />
    </rowset>
    </result>
    <cachedUntil>2007-12-02 20:55:38</cachedUntil>
    </eveapi>
     
     */

    public ConquerableStationsParser(IStaticDataModel dataModel, ISystemClock clock) {
        super( clock );
        if ( dataModel == null )
        {
            throw new IllegalArgumentException( "dataModel cannot be NULL" );
        }
        this.dataModel = dataModel;
    }

    @Override
    void parseHook(Document document) throws UnparseableResponseException
    {

        final RowSet rowSet = super.parseRowSet( "outposts", document );
        for (Row row : rowSet)
        {
            final long stationId = row.getLong( "stationID" );
            final CorporationId corpId =
                    new CorporationId( row.getLong( "corporationID" ) );
            final String stationName = row.get( "stationName" );
            final long solarSystemId = row.getLong( "solarSystemID" );
            final String corpName = row.get( "corporationName" );

            final SolarSystem ss = dataModel.getSolarSystem( solarSystemId );
            result.add( new Outpost( stationId, corpId, stationName, ss, corpName ) );
        }

    }

    @Override
    public URI getRelativeURI()
    {
        return URI;
    }

    @Override
    public List<Outpost> getResult() throws IllegalStateException
    {
        assertResponseParsed();
        return result;
    }

    @Override
    public void reset()
    {
        result = new ArrayList<Outpost>();
    }

}
