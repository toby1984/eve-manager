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

import java.util.Date;

import org.easymock.EasyMock;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.skills.datamodel.Asset;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.EveFlags;

public class AssetListParserTest extends AbstractParserTest
{

    private static final String XML =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<eveapi version=\"1\">\n"
                    + "  <currentTime>2007-12-01 17:55:07</currentTime>\n"
                    + "  <result>\n"
                    + "    <rowset name=\"assets\" key=\"itemID\" columns=\"itemID,locationID,typeID,quantity,flag,singleton\">\n"
                    + "      <row itemID=\"150354641\" locationID=\"30000380\" typeID=\"11019\" quantity=\"1\" flag=\"0\" singleton=\"1\">\n"
                    + "        <rowset name=\"contents\" key=\"itemID\" columns=\"itemID,typeID,quantity,flag,singleton\">\n"
                    + "          <row itemID=\"150354709\" typeID=\"16275\" quantity=\"200000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354710\" typeID=\"16272\" quantity=\"150000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354711\" typeID=\"16273\" quantity=\"150000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354712\" typeID=\"24597\" quantity=\"1000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354713\" typeID=\"24596\" quantity=\"1000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354714\" typeID=\"24595\" quantity=\"1000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354715\" typeID=\"24594\" quantity=\"1000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354716\" typeID=\"24593\" quantity=\"1000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354717\" typeID=\"24592\" quantity=\"1000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354718\" typeID=\"16274\" quantity=\"450000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354719\" typeID=\"9848\" quantity=\"1000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354720\" typeID=\"9832\" quantity=\"8000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354721\" typeID=\"3689\" quantity=\"5000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354722\" typeID=\"3683\" quantity=\"25000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354723\" typeID=\"44\" quantity=\"4000\" flag=\"5\" singleton=\"0\" />\n"
                    + "        </rowset>\n"
                    + "      </row>\n"
                    + "      <row itemID=\"150354706\" locationID=\"30001984\" typeID=\"11019\" quantity=\"1\" flag=\"0\" singleton=\"1\">\n"
                    + "        <rowset name=\"contents\" key=\"itemID\" columns=\"itemID,typeID,quantity,flag,singleton\">\n"
                    + "          <row itemID=\"150354741\" typeID=\"24593\" quantity=\"400\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354742\" typeID=\"24592\" quantity=\"400\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354755\" typeID=\"16275\" quantity=\"199000\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354837\" typeID=\"24597\" quantity=\"400\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354838\" typeID=\"24596\" quantity=\"400\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354839\" typeID=\"24595\" quantity=\"400\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150354840\" typeID=\"24594\" quantity=\"400\" flag=\"5\" singleton=\"0\" />\n"
                    + "          <row itemID=\"150356329\" typeID=\"14343\" quantity=\"1\" flag=\"5\" singleton=\"0\" />\n"
                    + "        </rowset>\n"
                    + "      </row>\n"
                    + "      <row itemID=\"150212056\" locationID=\"60001078\" typeID=\"25851\" quantity=\"10\" flag=\"4\" singleton=\"0\" />\n"
                    + "      <row itemID=\"150212057\" locationID=\"60001078\" typeID=\"20424\" quantity=\"20\" flag=\"4\" singleton=\"0\" />\n"
                    + "      <row itemID=\"150212058\" locationID=\"60001078\" typeID=\"20421\" quantity=\"20\" flag=\"4\" singleton=\"0\" />\n"
                    + "      <row itemID=\"150357641\" locationID=\"30001984\" typeID=\"23\" quantity=\"1\" flag=\"0\" singleton=\"1\">\n"
                    + "        <rowset name=\"contents\" key=\"itemID\" columns=\"itemID,typeID,quantity,flag,singleton\">\n"
                    + "          <row itemID=\"150357740\" typeID=\"16275\" quantity=\"9166\" flag=\"0\" singleton=\"0\" />\n"
                    + "        </rowset>\n"
                    + "      </row>\n"
                    + "      <row itemID=\"150212062\" locationID=\"60001078\" typeID=\"944\" quantity=\"1\" flag=\"4\" singleton=\"1\" />\n"
                    + "      <row itemID=\"150212063\" locationID=\"60001078\" typeID=\"597\" quantity=\"1\" flag=\"4\" singleton=\"0\" />\n"
                    + "    </rowset>\n" + "  </result>\n"
                    + "  <cachedUntil>2007-12-02 16:55:07</cachedUntil>\n"
                    + "</eveapi>\n" + "";

    public void testParser()
    {

        final IAPIClient client = EasyMock.createStrictMock( IAPIClient.class );

        EasyMock.replay( client );

        AssetListParser parser =
                new AssetListParser( new CharacterID( "blibb" ), null, client,
                        systemClock() );

        parser.parse( new Date(), XML );

        final AssetList result = parser.getResult();

        assertNull( result.getOwner() );
        assertNotNull( result );
        assertEquals( 8, result.size() );

        Asset child = result.searchAsset( 150354838L );
        assertNotNull( child );
        assertNotNull( child.getContainer() );
        assertEquals( 150354706L, child.getContainer().getItemId() );

        assertEquals( 199000, result.searchAsset( 150354755L ).getQuantity() );

        assertEquals( EveFlags.CARGO, result.searchAsset( 150354755L ).getFlags() );

        assertEquals( EveFlags.HANGAR, result.searchAsset( 150212056L ).getFlags() );

        assertFalse( result.searchAsset( 150212062 ).isPackaged() );

        child = result.getAsset( 150354641L );
        assertNotNull( child );

        assertEquals( 15, child.getContents().size() );
        assertNotNull( child.getContents().getAsset( 150354712 ) );

        child = result.getAsset( 150354706 );
        assertNotNull( child );
        assertEquals( 8, child.getContents().size() );

        assertNotNull( result.getAsset( 150212056 ) );
        assertNotNull( result.getAsset( 150212057 ) );
        assertNotNull( result.getAsset( 150212058 ) );
        assertNotNull( result.getAsset( 150357641 ) );
        assertNotNull( result.getAsset( 150212062 ) );
        assertNotNull( result.getAsset( 150212063 ) );

        for (Asset a : result)
        {
            printAsset( a, 0 );
        }
    }

    private void printAsset(Asset a, int depth)
    {

        final StringBuilder indention = new StringBuilder();
        for (int i = 0; i < depth; i++)
        {
            indention.append( "     " );
        }

        System.out.println( indention + "------ ID: " + a.getItemId() + " / Quantity: "
                + a.getQuantity() + " / flags " + a.getFlags() + " / packaged: "
                + a.isPackaged() );
        for (Asset child : a.getContents())
        {
            printAsset( child, depth + 1 );
        }
    }
}
