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
package de.codesourcery.planning.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactoryManager;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.ISlotType;

/**
 * A simple factory slot where input and output
 * locations are always the same.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class SimpleProductionSlot implements IFactorySlot
{
	private final IProductionLocation location;
	private final String name;
	private final ISlotType type; 
	private IFactory factory;
	private final List<IJob> jobs = new ArrayList<IJob>();

	public SimpleProductionSlot(String name , ISlotType type, IProductionLocation location) {
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		if ( location == null ) {
			throw new IllegalArgumentException("location cannot be NULL");
		}
		this.location = location;
		this.name = name != null ? name : "";
		this.type = type;
	}

	@Override
	public List<IJob> getJobsSortedAscendingByStartTime()
	{
		final List<IJob> result = new ArrayList<IJob>( getJobs() );
		Collections.sort( result , new Comparator<IJob>() {

			@Override
			public int compare(IJob o1, IJob o2)
			{
				return o1.getEarliestStartDate().compareTo( o2.getEarliestStartDate() );
			}} );
		return result;
	}

	@Override
	public float getUtilization(DateRange dateRange)
	{

		if ( dateRange == null ) {
			throw new IllegalArgumentException("dateRange cannot be NULL");
		}
		
		final Date startDate=dateRange.getStartDate();
		final Date endDate = dateRange.getEndDate();
		
		if ( jobs.isEmpty() ) {
			return 0.0f;
		}

		final Duration range = new Duration( startDate , endDate );
		
		Duration duration = null;
		for ( IJob job : jobs ) {
			Duration d = job.getDurationWithin( startDate , endDate );
			if ( duration == null || d.longerThan( duration ) ) {
				duration = d;
			} 
		}
		return (float) duration.toSeconds() / (float) range.toSeconds();
	}

	@Override
	public IFactory getFactory()
	{
		return factory;
	}

	@Override
	public void setFactory(IFactory factory)
	{
		if ( factory == null ) {
			throw new IllegalArgumentException("factory cannot be NULL");
		}
		this.factory = factory;
	}

	@Override
	public void add(IJob job) 
	{
		if ( job == null ) {
			throw new IllegalArgumentException("job cannot be NULL");
		}

		// link with other dependencies
		final IFactoryManager factoryManager = 
			this.getFactory().getFactoryManager();

		for ( IJobTemplate dependency : job.getTemplate().getDependencies() ) 
		{
			final List<IJob> dependencies = factoryManager.getJobsForTemplate( dependency );
			if ( dependencies.isEmpty() ) {
				throw new RuntimeException("Internal error - found no scheduled jobs for dependency "+dependency);

			}
			for ( IJob depJob : dependencies ) {
				job.addDependentJob( depJob );
			}
		}

		// sanity checks
		final Date startDate = job.getStartDate();
		
		if ( ! job.getDependentJobs().isEmpty() ) {
			for ( IJob j : job.getDependentJobs() ) {
				if ( j.getEarliestFinishingDate().after( startDate ) ) {
					throw new IllegalArgumentException("Cannot add job that starts at "+startDate+" " +
							"before the job "+j+" it depends the has finished");
				}
			}
		}
		
		for ( IJob j : getJobs() ) {
			if ( j.getDateRange().intersects( job.getDateRange() ) ) {
				throw new IllegalArgumentException("Cannot add job "+job+
						" that overlaps with job "+j+" [ "+job.getDateRange()+" <=> "+j.getDateRange()+" ]");
			}
		}

		jobs.add( job );
	}

	@Override
	public void remove(IJob job)
	{
		if ( job == null ) {
			throw new IllegalArgumentException("job cannot be NULL");
		}
		jobs.remove( job );
	}

	@Override
	public ISlotType getType()
	{
		return type;
	}

	@Override
	public boolean hasType(ISlotType type)
	{
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		return this.type.equals( type );
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public List<IJob> getJobs()
	{
		return jobs;
	}

	@Override
	public List<IJob> getJobsForTemplate(IJobTemplate t)
	{
		final List<IJob> result = new ArrayList<IJob>();

		for ( IJob j : getJobs() ) {
			if ( j.getTemplate().equals( t ) ) {
				result.add( j );
			}
		}
		return result;		
	}

	public String getName()
	{
		return name;
	}

	@Override
	public IJob getJobOn(Date date)
	{
		if ( date == null ) {
			throw new IllegalArgumentException("date cannot be NULL");
		}

		IJob result = null;
		for ( IJob job : getJobs() ) {
			if ( job.runsAt( date ) ) {
				if ( result != null ) {
					throw new RuntimeException("Internal error - more than 1 job running on "+date);
				}
				result = job;
			}
		}
		return result;
	}

	private static Date startOfDay(Date date) {
		final Calendar cal = Calendar.getInstance();
		cal.setTime( date );

		cal.set(Calendar.HOUR_OF_DAY , 0 );
		cal.set(Calendar.MINUTE, 0 );
		cal.set(Calendar.SECOND, 0 );
		cal.set(Calendar.MILLISECOND, 0 );
		return cal.getTime();
	}

	private static Date endOfDay(Date date) {

		final Calendar cal = Calendar.getInstance();
		cal.setTime( date );

		cal.set(Calendar.HOUR_OF_DAY , 23 );
		cal.set(Calendar.MINUTE, 59 );
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999 );
		return cal.getTime();
	}	

	@Override
	public List<IJob> getJobsOnDay(Date day)
	{
		final List<IJob> result = new ArrayList<IJob>();

		final Date startDate = startOfDay( day );
		final Date endDate = endOfDay( day );

		for ( IJob job : getJobs() ) {
			if ( job.getDurationWithin( startDate , endDate ).longerThan( Duration.ZERO ) ) 
			{
				result.add( job );
			}
		}
		return result;
	}

	@Override
	public boolean hasJobsOnDay(Date day)
	{

		final Date startDate = startOfDay( day );
		final Date endDate = endOfDay( day );

		for ( IJob job : getJobs() ) {
			if ( job.getDurationWithin( startDate , endDate ).longerThan( Duration.ZERO ) ) 
			{
				return true;
			}			
		}
		return false;
	}

	@Override
	public boolean contains(IJob job)
	{
		if ( job == null ) {
			throw new IllegalArgumentException("job cannot be NULL");
		}
		
		for ( IJob j : getJobs() ) {
			if ( j.equals( job ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IProductionLocation getInputLocation()
	{
		return this.location;
	}

	@Override
	public IProductionLocation getOutputLocation()
	{
		return this.location;
	}
	
}
