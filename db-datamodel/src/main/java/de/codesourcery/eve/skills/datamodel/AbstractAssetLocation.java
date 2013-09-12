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
package de.codesourcery.eve.skills.datamodel;

import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;

public class AbstractAssetLocation implements ILocation
{

    private Object subType;

    public AbstractAssetLocation() {
    }

    protected AbstractAssetLocation(Object subType) {
        this.subType = subType;
    }

    protected void setSubType(Object subType)
    {
        this.subType = subType;
    }

    protected Object getSubType()
    {
        return subType;
    }

    @Override
    public SolarSystem asSolarSystem()
    {
        if ( subType instanceof SolarSystem )
        {
            return (SolarSystem) subType;
        }
        throw new UnsupportedOperationException( "Location " + this
                + " is not a solar-system" );
    }

    @Override
    public Station asStation()
    {
        if ( subType instanceof Station )
        {
            return (Station) subType;
        }
        throw new UnsupportedOperationException( "Location " + this + " is not a station" );
    }

    @Override
    public boolean isSolarSystem()
    {
        return subType instanceof Station;
    }

    @Override
    public boolean isStation()
    {
        return subType instanceof Station;
    }

    @Override
    public String getDisplayName()
    {

        if ( subType != null )
        {
            if ( isSolarSystem() )
            {
                return asSolarSystem().getDisplayName();
            }
            else if ( isStation() )
            {
                return asStation().getDisplayName();
            }
            return subType.toString();
        }
        return "<null>";
    }

    @Override
    public boolean isUnknown()
    {
        return false;
    }

    @Override
    public boolean isAnyLocation()
    {
        return false;
    }

    @Override
    public boolean isOutpost()
    {
        return false;
    }

}
