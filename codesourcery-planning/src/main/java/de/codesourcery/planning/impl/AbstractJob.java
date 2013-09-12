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

import java.util.Date;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;

public abstract class AbstractJob implements IJob
{

	private final Duration duration;
	private JobStatus status = JobStatus.PROSPECTIVE;
	private int runs=1;
	private final String name;
	private final IJobTemplate jobTemplate;
	
	protected AbstractJob(String name , IJobTemplate jobTemplate,Duration duration) {
		this( name , jobTemplate , duration , 1 );
	}
	
	protected AbstractJob(String name , IJobTemplate jobTemplate,Duration duration , int runs) {
		if ( jobTemplate == null ) {
			throw new IllegalArgumentException("jobTemplate cannot be NULL");
		}
		if ( duration == null ) {
			throw new IllegalArgumentException("duration cannot be NULL");
		}
		
		if ( StringUtils.isBlank(name ) ) {
			throw new IllegalArgumentException(
					"name cannot be blank.");
		}
		this.name = name;
		this.duration = duration;
		this.jobTemplate = jobTemplate;
		setRuns( runs );
	}
	
	@Override
	public final String getName()
	{
		return name;
	}
	
	@Override
	public final Duration getDurationFrom(Date date)
	{
		final Date jobStartDate = getStartDate();
		if ( isAfterOrOn( jobStartDate , date ) ) {
			return getDuration();
		}
		
		final Date endDate = Duration.add( jobStartDate  , getDuration() );
		if ( endDate.before( date ) ) {
			return Duration.ZERO;
		}
			
		return new Duration( date , endDate );
	}
	
	@Override
	public Duration getDuration()
	{
		return this.duration;
	}
	
	protected static boolean isBeforeOrOn(Date toCheck, Date other) {
		return toCheck.compareTo( other ) <= 0;
	}
	
	protected static boolean isAfterOrOn(Date toCheck, Date other) {
		return toCheck.compareTo( other ) >= 0;
	}
	
	@Override
	public final IJobTemplate getTemplate()
	{
		return jobTemplate;
	}
	
	@Override
	public final JobStatus getStatus()
	{
		return status;
	}

	@Override
	public final boolean hasStatus(JobStatus... s)
	{
		if ( ArrayUtils.isEmpty( s ) ) {
			throw new IllegalArgumentException("s cannot be NULL");
		}
		for ( JobStatus expected : s ) {
			if ( status == expected ) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public final void setStatus(JobStatus status)
	{
		if ( status == null ) {
			throw new IllegalArgumentException("status cannot be NULL");
		}
		
		if ( ! this.status.mayTransitionTo( status ) ) {
			throw new IllegalStateException("Invalid job state transition "+this.status+" -> "+status);
		}
		this.status = status;
	}
	
	@Override
	public final void setRuns(int runs)
	{
		if ( runs <= 0 ) {
			throw new IllegalArgumentException("runs cannot be <= 0");
		}
		
		this.runs = runs;		
	}
	
	@Override
	public final int getRuns()
	{
		return runs;
	}
	
	protected static Date latest(Date d1,Date d2) {
		final int comp = d1.compareTo( d2 );
		if ( comp == -1 ) {
			return d2;
		} else if ( comp == 1 ) {
			return d1;
		}
		return d1;
	}
	
	protected static Date earliest(Date d1,Date d2) {
		final int comp = d1.compareTo( d2 );
		if ( comp == -1 ) {
			return d1;
		} else if ( comp == 1 ) {
			return d2;
		}
		return d1;
	}
	
	@Override
	public final DateRange getDateRange()
	{
		return new DateRange( getStartDate() , getEndDate() );
	}
	
	@Override
	public final Duration getDurationWithin(Date startDate, Date endDate)
	{
	
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		
		if ( endDate == null ) {
			throw new IllegalArgumentException("endDate cannot be NULL");
		}
		
		if ( ! isBeforeOrOn( startDate , endDate ) ) {
			throw new IllegalArgumentException("Start date needs to be <= end date");
		}
		
		final Date jobStartDate = getStartDate();
		final Date jobEndDate = getEndDate();
		
		if ( isBeforeOrOn( jobEndDate , startDate ) ) {
			return Duration.ZERO;
		}
		
		if ( isAfterOrOn( jobStartDate , endDate ) ) {
			return Duration.ZERO;
		}
		
		final Date start = latest( startDate ,getStartDate() );
		final Date end = earliest( endDate ,getEndDate() );

		return new Duration(  earliest( start, end ) , latest( start, end ) );
	}

	/**
	 * Returns the end date of this job.
	 * 
	 * @return getStartDate() + getDuration()
	 */
	public final Date getEndDate() {
		return getDuration().addTo( getStartDate() );
	}

	@Override
	public boolean runsAt(Date date)
	{
		if ( date == null ) {
			throw new IllegalArgumentException("date cannot be NULL");
		}
		
		return getDateRange().contains( date );
	}
}
