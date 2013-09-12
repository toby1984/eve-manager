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
package de.codesourcery.eve.skills.db.dao;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;

public class StationDAO extends HibernateDAO<Station,Long> implements IStationDAO {

	public StationDAO() {
		super(Station.class);
	}

	@Override
	public List<Station> getStations(final Region region)
	{
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<Station>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<Station> doInSession(Session session) {
					final Query query = 
						session
						.createQuery("from Station " +
						"where region = :region");

					query.setParameter("region" , region );
					return (List<Station>) query.list();					
				}} );
		
	}

	@Override
	public List<Station> getStations(final Region region, final SolarSystem solarSystem)
	{
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		if ( solarSystem == null ) {
			throw new IllegalArgumentException("solarSystem cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<Station>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<Station> doInSession(Session session) {
					final Query query = 
						session
						.createQuery("from Station " +
						"where region = :region and solarSystem = :solarsystem");

					query.setParameter("region" , region );
					query.setParameter("solarsystem" , solarSystem);
					return (List<Station>) query.list();					
				}} );
		
	}

}
