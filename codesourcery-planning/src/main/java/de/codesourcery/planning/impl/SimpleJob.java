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
import java.util.Date;
import java.util.List;

import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;

public class SimpleJob extends AbstractJob
{

	private Date startDate;
	private final List<IJob> dependsOn = new ArrayList<IJob>();

	public SimpleJob(String name , IJobTemplate jobTemplate , Duration duration) 
	{
		super( name , jobTemplate , duration , 1);
	}
	
	public SimpleJob(String name, IJobTemplate jobTemplate,
			Duration duration, int runs) {
		super(name, jobTemplate, duration, runs);
	}

	public void addDependentJob(IJob job) {
		if ( job == null ) {
			throw new IllegalArgumentException("job cannot be NULL");
		}
		this.dependsOn.add( job );
	}
	
	@Override
	public List<IJob> getDependentJobs()
	{
		return dependsOn;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public void setStartDate(Date startDate)
	{
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		
		this.startDate = startDate;
	}
	
	@Override
	public Date getStartDate()
	{
		if ( this.dependsOn.isEmpty() ) {
			return startDate;
		}
		return getEarliestStartDate();
	}
	
	@Override
	public Date getEarliestFinishingDate() {
		return getDuration().addTo( getEarliestStartDate() );
	}

	@Override
	public Date getEarliestStartDate()
	{
		
		if ( dependsOn.isEmpty() ) {
			if ( startDate == null ) {
				throw new IllegalStateException("Start date not set/available");
			}
			return startDate;
		}
		
		Date startDate = null;
		for ( IJob t : dependsOn ) {
			
			final Date sd =
				t.getEarliestFinishingDate();
			
			if ( startDate == null || sd.after( startDate ) ) {
				startDate = sd;
			}
		}
		return startDate;
	}
	
	@Override
	public Duration getTotalDuration()
	{
		if ( ! getDependentJobs().isEmpty() ) {
			Date startDate = null;
			for ( IJob dep : getDependentJobs() ) {
				if ( startDate == null || dep.getStartDate().before( startDate ) ) {
					startDate = dep.getStartDate();
				}
			}
			return new Duration( startDate , getEarliestFinishingDate() );
		}
		return getDuration(); // new Duration( getEarliestStartDate() , getEarliestFinishingDate() );
	}

	@Override
	public boolean dependsOn(IJob job)
	{
		if ( job == null ) {
			throw new IllegalArgumentException("job cannot be NULL");
		}
		return this.dependsOn.contains( job );
	}


}
