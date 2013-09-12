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

import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.AssemblyLine;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;

public class AssemblyLineDAO extends HibernateDAO<AssemblyLine,Long> implements IAssemblyLineDAO
{

	public AssemblyLineDAO() {
		super(AssemblyLine.class);
	}

	@Override
	public List<AssemblyLine> getAssemblyLines(final Region region, final Activity activity)
	{
		
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<AssemblyLine>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<AssemblyLine> doInSession(Session session) {
				final Query query = 
					session.createQuery("from AssemblyLine r" +
					"where  r.station.region= :region and r.activity = :activity");

				query.setParameter("region" , region);
				query.setParameter("activity" , 
						activity ,
						Hibernate.custom( ActivityUserType.class ) );
				return (List<AssemblyLine>) query.list();					
			}} );
	}

	@Override
	public List<AssemblyLine> getAssemblyLines(final SolarSystem system,
			final Activity activity)
	{
		if ( system == null ) {
			throw new IllegalArgumentException("system cannot be NULL");
		}
		
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<AssemblyLine>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<AssemblyLine> doInSession(Session session) {
				final Query query = 
					session
					.createQuery("from AssemblyLine r" +
					"where  r.station.solarSystem= :system and r.activity = :activity");

				query.setParameter("system" , system);
				query.setParameter("activity" , 
						activity ,
						Hibernate.custom( ActivityUserType.class ) );
				return (List<AssemblyLine>) query.list();					
			}} );		
	}

	@Override
	public List<SolarSystem> getSolarSystemsFor(final Region region, final Activity activity)
	{
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<SolarSystem>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<SolarSystem> doInSession(Session session) 
			{
				final Query query = 
					session
					.createQuery("select distinct s from SolarSystem s , AssemblyLine line, Station st " +
					"where st.stationID = line.station.stationID and " +
					"st.region= :region and line.activity = :activity and "+
					"s = st.solarSystem" );

				query.setParameter("region" , region);
				query.setParameter("activity" , 
						activity ,
						Hibernate.custom( ActivityUserType.class ) );
				return (List<SolarSystem>) query.list();					
			}} );		
	}

	@Override
	public List<Station> getStationsFor(final Region region, final SolarSystem solarSystem, final Activity activity)
	{
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( solarSystem == null ) {
			throw new IllegalArgumentException("solarSystem cannot be NULL");
		}
		
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<Station>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<Station> doInSession(Session session) 
			{
				final Query query = 
					session
					.createQuery("select distinct st from Station st , AssemblyLine line" +
					" where st.stationID = line.station and st.region= :region and st.solarSystem = :system " +
					" and line.activity = :activity");

				query.setParameter("region" , region);
				query.setParameter("system" , solarSystem);
				query.setParameter("activity" , activity ,Hibernate.custom( ActivityUserType.class ) );
				return (List<Station>) query.list();					
			}} );			
	}
	
	public List<Station> getAllStations(final Region region) {
		
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<Station>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<Station> doInSession(Session session) 
			{
				final Query query = 
					session.createQuery("from Station st where st.region= :region");

				query.setParameter("region" , region);
				return (List<Station>) query.list();					
			}} );	
	}
	

	@Override
	public List<Station> getStationsFor(final Region region, final Activity activity)
	{
		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		
		if ( Activity.REFINING == activity ) 
		{
			/*
			 * TODO: This code needs fixing ... use station
			 * services to check whether refining is possible.
			 */
			System.out.println("TODO: Use station services table to " +
					"check whether a station has refining facilities");
			
			final List<Station>  result =
				getAllStations( region );
			
			for ( Iterator<Station> it = result.iterator() ;it.hasNext() ; ) {
				if ( it.next().getReprocessingEfficiency() == 0.0d ) {
					it.remove();
				}
			}
			return result;
		}
		
		return execute( new HibernateCallback<List<Station>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<Station> doInSession(Session session) 
			{
				final Query query = 
					session.createQuery("select distinct st from Station st , AssemblyLine line" +
					" where st.stationID = line.station and st.region= :region " +
					" and line.activity = :activity");

				query.setParameter("region" , region);
				query.setParameter("activity" , activity ,Hibernate.custom( ActivityUserType.class ) );
				return (List<Station>) query.list();					
			}} );			
	}

	@Override
	public List<AssemblyLine> getAssemblyLines(final Station station, final Activity activity)
	{
		if ( station == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		
		return execute( new HibernateCallback<List<AssemblyLine>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<AssemblyLine> doInSession(Session session) 
			{
				final Query query = 
					session.createQuery("from AssemblyLine line " +
					" where line.station = :station and "+
					" line.activity = :activity");

				query.setParameter("station" , station);
				query.setParameter("activity" , activity ,Hibernate.custom( ActivityUserType.class ) );
				return (List<AssemblyLine>) query.list();					
			}} );		
	}

}
