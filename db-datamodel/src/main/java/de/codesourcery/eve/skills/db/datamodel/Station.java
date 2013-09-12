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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.planning.IProductionLocation;

@Entity
@Table(name = "staStations")
// @org.hibernate.annotations.Proxy(lazy=false)
public class Station implements ILocation, IProductionLocation
{

    private static final long MIN_ID = 60000004L;
    private static final long MAX_ID = 60115147L;

    public static final boolean isStationId(long id)
    {
        return ( id >= MIN_ID && id <= MAX_ID );
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
     * mysql> desc staStations;
     * +--------------------------+---------------------
     * +------+-----+---------+-------+ | Field | Type | Null | Key | Default |
     * Extra |
     * +--------------------------+---------------------+------+-----+----
     * -----+-------+ | stationID | int(11) | NO | PRI | NULL | | | security |
     * smallint(6) | YES | | NULL | | | dockingCostPerVolume | double | YES | |
     * NULL | | | maxShipVolumeDockable | double | YES | | NULL | | |
     * officeRentalCost | int(11) | YES | | NULL | | | operationID | tinyint(3)
     * unsigned | YES | MUL | NULL | | | stationTypeID | smallint(6) | YES | MUL
     * | NULL | | | corporationID | int(11) | YES | MUL | NULL | | |
     * solarSystemID | int(11) | YES | MUL | NULL | | | constellationID |
     * int(11) | YES | MUL | NULL | | | regionID | int(11) | YES | MUL | NULL |
     * | | stationName | varchar(100) | YES | | NULL | | | x | double | YES | |
     * NULL | | | y | double | YES | | NULL | | | z | double | YES | | NULL | |
     * | reprocessingEfficiency | double | YES | | NULL | | |
     * reprocessingStationsTake | double | YES | | NULL | | |
     * reprocessingHangarFlag | tinyint(4) | YES | | NULL | |
     * +------------------
     * --------+---------------------+------+-----+---------+-------+
     */

    @Id
    private Long stationID;

    @Column
    private String stationName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "regionID", nullable = false, updatable = false)
    private Region region;

    @OneToOne
    @JoinColumn(name = "solarSystemID", nullable = true, updatable = false)
    private SolarSystem solarSystem;

    // TODO: currently broken
    // @OneToOne(optional=false)
    // @JoinColumn(name="stationTypeID",nullable=false,updatable=false)
    // private StationType stationType;

    @OneToOne
    @JoinColumn(name = "constellationID", nullable = true, updatable = false)
    private Constellation constellation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "corporationID")
    private NPCCorporation owner;

    @Column(name = "reprocessingEfficiency")
    private double reprocessingEfficiency;

    public void setName(String stationName)
    {
        this.stationName = stationName;
    }

    public String getName()
    {
        return stationName;
    }

    public void setID(Long stationID)
    {
        this.stationID = stationID;
    }

    public Long getID()
    {
        return stationID;
    }

    @Override
    public String toString()
    {
        return "Station[ id=" + stationID + " , name=" + stationName + " ]";
    }

    // ============ ILocation interface =================

    @Override
    public boolean equals(Object obj)
    {
        if ( obj instanceof Station )
        {
            return ObjectUtils.equals( this.stationID, ( (Station) obj ).stationID );
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return this.stationID != null ? this.stationID.hashCode() : 0;
    }

    @Override
    public final SolarSystem asSolarSystem()
    {
        throw new UnsupportedOperationException( "Cannot cast station " + this
                + " to solar system" );
    }

    @Override
    public final Station asStation()
    {
        return this;
    }

    @Override
    public final boolean isSolarSystem()
    {
        return false;
    }

    @Override
    public final boolean isStation()
    {
        return true;
    }

    public Region getRegion()
    {
        return region;
    }

    public void setConstellation(Constellation constellation)
    {
        this.constellation = constellation;
    }

    public Constellation getConstellation()
    {
        return constellation;
    }

    public void setSolarSystem(SolarSystem solarSystem)
    {
        this.solarSystem = solarSystem;
    }

    public SolarSystem getSolarSystem()
    {
        return solarSystem;
    }

    @Override
    public String getDisplayName()
    {
        return getName();
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

    public NPCCorporation getOwner()
    {
        return owner;
    }

    /**
     * Reprocessing efficiency in percent (0...1).
     * 
     * @return
     */
    public double getReprocessingEfficiency()
    {
        return reprocessingEfficiency;
    }

    @Override
    public boolean isOutpost()
    {
        return false;
    }

}
