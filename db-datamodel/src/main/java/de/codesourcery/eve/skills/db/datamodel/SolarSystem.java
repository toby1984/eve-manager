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
package de.codesourcery.eve.skills.db.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import de.codesourcery.eve.skills.datamodel.ILocation;

@Entity
@Table(name = "mapSolarSystems")
// @org.hibernate.annotations.Proxy(lazy=false)
public class SolarSystem implements ILocation
{

    private static final long MIN_ID = 30000001L;

    private static final long MAX_ID = 31102504L;

    public static final boolean isSolarSystemId(long id)
    {
        return ( id >= MIN_ID && id <= MAX_ID );
    }

    public static final boolean isOutpostId(long id)
    {
        return ( id >= 60000000 ); // FIXME: 60000000 is just an estimated
        // guess....
    }

    /*
     * mysql> select min(solarSystemID),max(solarSystemID) from mapSolarSystems;
     * +--------------------+--------------------+ | min(solarSystemID) |
     * max(solarSystemID) | +--------------------+--------------------+ |
     * 30000001 | 31002504 | +--------------------+--------------------+ 1 row
     * in set (0.00 sec)
     * 
     * mysql> select min(stationID),max(stationID) from staStations;
     * +----------------+----------------+ | min(stationID) | max(stationID) |
     * +----------------+----------------+ | 60000004 | 60015147 |
     * +----------------+----------------+ 1 row in set (0.00 sec)
     */

    /*
     * mysql> desc mapSolarSystems;
     * +-----------------+--------------+------+-----+---------+-------+ | Field
     * | Type | Null | Key | Default | Extra |
     * +-----------------+--------------+------+-----+---------+-------+ |
     * regionID | int(11) | YES | MUL | NULL | | | constellationID | int(11) |
     * YES | MUL | NULL | | | solarSystemID | int(11) | NO | PRI | NULL | | |
     * solarSystemName | varchar(100) | YES | | NULL | | | x | double | YES | |
     * NULL | | | y | double | YES | | NULL | | | z | double | YES | | NULL | |
     * | xMin | double | YES | | NULL | | | xMax | double | YES | | NULL | | |
     * yMin | double | YES | | NULL | | | yMax | double | YES | | NULL | | |
     * zMin | double | YES | | NULL | | | zMax | double | YES | | NULL | | |
     * luminosity | double | YES | | NULL | | | border | tinyint(1) | YES | |
     * NULL | | | fringe | tinyint(1) | YES | | NULL | | | corridor | tinyint(1)
     * | YES | | NULL | | | hub | tinyint(1) | YES | | NULL | | | international
     * | tinyint(1) | YES | | NULL | | | regional | tinyint(1) | YES | | NULL |
     * | | constellation | tinyint(1) | YES | | NULL | | | security | double |
     * YES | MUL | NULL | | | factionID | int(11) | YES | MUL | NULL | | |
     * radius | double | YES | | NULL | | | sunTypeID | smallint(6) | YES | MUL
     * | NULL | | | securityClass | varchar(2) | YES | | NULL | |
     * +-----------------+--------------+------+-----+---------+-------+
     */

    @Id
    private Long solarSystemID;

    @Column
    private String solarSystemName;

    @Column
    private double security;

    public void setID(Long solarSystemID)
    {
        this.solarSystemID = solarSystemID;
    }

    @Override
    public String toString()
    {
        return "SolarSystem[ id=" + solarSystemID + " , name=" + solarSystemName + " ]";
    }

    public Long getID()
    {
        return solarSystemID;
    }

    public void setSolarSystemName(String solarSystemName)
    {
        this.solarSystemName = solarSystemName;
    }

    public String getSolarSystemName()
    {
        return solarSystemName;
    }

    public void setSecurity(double security)
    {
        this.security = security;
    }

    public double getTrueSecurity()
    {
        return security;
    }

    public double getRoundedSecurity()
    {

        if ( security < 0 )
        {
            return 0.0d;
        }
        else
        {
            return Math.floor( security * 10.0d ) / 10.0d;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        return ( obj instanceof SolarSystem )
                && ( (SolarSystem) obj ).getSolarSystemName().equals(
                    this.solarSystemName );
    }

    @Override
    public int hashCode()
    {
        return this.solarSystemName != null ? this.solarSystemName.hashCode() : 0;
    }

    // ========= ILocation =============

    @Override
    public final SolarSystem asSolarSystem()
    {
        return this;
    }

    @Override
    public final Station asStation()
    {
        throw new UnsupportedOperationException( "Cannot cast solar system " + this
                + " to station" );
    }

    @Override
    public final boolean isSolarSystem()
    {
        return true;
    }

    @Override
    public final boolean isStation()
    {
        return false;
    }

    @Override
    public String getDisplayName()
    {
        return getSolarSystemName();
    }

    @Override
    public final boolean isUnknown()
    {
        return false;
    }

    @Override
    public final boolean isAnyLocation()
    {
        return false;
    }

    @Override
    public boolean isOutpost()
    {
        return false;
    }
}
