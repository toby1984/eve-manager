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

import static org.easymock.EasyMock.*;

import java.util.Date;
import java.util.List;

import junit.framework.AssertionFailedError;
import de.codesourcery.eve.skills.datamodel.CorporationId;
import de.codesourcery.eve.apiclient.parsers.ConquerableStationsParser.Outpost;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;

public class ConquerableStationsParserTest extends AbstractParserTest
{

    private static final String XML =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "    <eveapi version=\"1\">\n"
                    + "    <currentTime>2007-12-02 19:55:38</currentTime>\n"
                    + "    <result>\n"
                    + "    <rowset name=\"outposts\" key=\"stationID\" columns=\"stationID,stationName,stationTypeID,solarSystemID,corporationID,corporationName\">\n"
                    + "      <row stationID=\"61000001\" stationName=\"DB1R-4 II - duperTum Corp Minmatar Service Outpost\"\n"
                    + "           stationTypeID=\"21646\" solarSystemID=\"30004470\" corporationID=\"150020944\"\n"
                    + "           corporationName=\"duperTum Corp\" />\n"
                    + "      <row stationID=\"61000002\" stationName=\"ZS-2LT XI - duperTum Corp Minmatar Service Outpost\"\n"
                    + "           stationTypeID=\"21646\" solarSystemID=\"30004469\" corporationID=\"150020944\"\n"
                    + "           corporationName=\"duperTum Corp\" />\n"
                    + "    </rowset>\n" + "    </result>\n"
                    + "    <cachedUntil>2007-12-02 20:55:38</cachedUntil>\n"
                    + "    </eveapi>";

    public void testParser()
    {

        final IStaticDataModel dataModel = createMock( IStaticDataModel.class );

        final SolarSystem sol1 = new SolarSystem();
        final SolarSystem sol2 = new SolarSystem();

        expect( dataModel.getSolarSystem( 30004470L ) ).andReturn( sol1 ).atLeastOnce();
        expect( dataModel.getSolarSystem( 30004469L ) ).andReturn( sol2 ).atLeastOnce();

        replay( dataModel );

        ConquerableStationsParser parser =
                new ConquerableStationsParser( dataModel, systemClock() );

        parser.parse( new Date(), XML );

        final List<Outpost> result = parser.getResult();

        assertNotNull( result );
        assertEquals( 2, result.size() );

        boolean found1 = true;
        boolean found2 = true;
        for (Outpost op : result)
        {
            if ( op.getId() == 61000001L )
            {
                assertSame( sol1, op.getSolarSystem() );
                assertEquals( "DB1R-4 II - duperTum Corp Minmatar Service Outpost", op
                        .getName() );
                assertEquals( "duperTum Corp", op.getCorporationName() );
                assertEquals( new CorporationId( 150020944L ), op.getCorporationId() );
                found1 = true;
            }
            else if ( op.getId() == 61000002L )
            {
                assertSame( sol2, op.getSolarSystem() );
                assertEquals( "ZS-2LT XI - duperTum Corp Minmatar Service Outpost", op
                        .getName() );
                assertEquals( "duperTum Corp", op.getCorporationName() );
                assertEquals( new CorporationId( 150020944L ), op.getCorporationId() );
                found2 = true;
            }
            else
            {
                throw new AssertionFailedError( "Unexpected outpost " + op );
            }
        }

        assertTrue( found1 );
        assertTrue( found2 );
        verify( dataModel );
    }

}
