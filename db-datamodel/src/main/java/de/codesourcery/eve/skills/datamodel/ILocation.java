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

/**
 * A location.
 * 
 * Implementors MUST provide proper hashCode() / equals() methods.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface ILocation
{

    public static final ILocation ANY_LOCATION = new ILocation() {

        @Override
        public SolarSystem asSolarSystem()
        {
            throw new UnsupportedOperationException(
                    "cannot convert 'any' location to solar system" );
        }

        @Override
        public Station asStation()
        {
            throw new UnsupportedOperationException(
                    "cannot convert 'any' location to station" );
        }

        @Override
        public String getDisplayName()
        {
            return "ANY LOCATION";
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
            return true;
        }

        @Override
        public boolean isAnyLocation()
        {
            return true;
        }

        @Override
        public boolean isOutpost()
        {
            return false;
        }
    };

    public static final ILocation UNKNOWN_LOCATION = new ILocation() {

        @Override
        public SolarSystem asSolarSystem()
        {
            throw new UnsupportedOperationException(
                    "cannot convert unknown location to solar system" );
        }

        @Override
        public Station asStation()
        {
            throw new UnsupportedOperationException(
                    "cannot convert unknown location to station" );
        }

        @Override
        public String getDisplayName()
        {
            return "<unknown location>";
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
            return true;
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
    };

    public boolean isStation();

    public boolean isSolarSystem();

    public boolean isAnyLocation();

    public boolean isOutpost();

    public boolean isUnknown();

    public SolarSystem asSolarSystem();

    public Station asStation();

    public String getDisplayName();

}
