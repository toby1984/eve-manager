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
import de.codesourcery.planning.IFactoryManager;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IProductionPlan;
import de.codesourcery.planning.IProductionPlanTemplate;

public class SimpleProductionPlan implements IProductionPlan
{
	private final List<IJob> jobs =
		new ArrayList<IJob>();
	
	private final IFactoryManager manager;
	private final IProductionPlanTemplate template;
	private Status status = Status.NOT_STARTED;
	
	public SimpleProductionPlan(IFactoryManager manager , IProductionPlanTemplate template) {
		if ( template == null ) {
			throw new IllegalArgumentException("template cannot be NULL");
		}
		if ( manager == null ) {
			throw new IllegalArgumentException("manager cannot be NULL");
		}

		this.manager = manager;
		this.template = template;
	}
	
	@Override
	public void submit()
	{
		manager.submitProductionPlan( this );
	}
	
	@Override
	public void dispose()
	{
		manager.cancelProductionPlan( this );
	}

	@Override
	public Duration getDuration()
	{
		
		Duration result = Duration.seconds( 0 );
		for ( IJob j : jobs ) {
			result = result.add( j.getDuration() );
		}
		return result;
	}

	@Override
	public List<IJob> getJobs()
	{
		return jobs;
	}

	@Override
	public Date getStartDate()
	{
		Date result = new Date();
		for ( IJob j : jobs ) {
			final Date d = j.getStartDate();
			if ( d.before( result ) ) {
				result = d;
			}
		}
		return result;
	}

	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public IProductionPlanTemplate getTemplate()
	{
		return template;
	}

	@Override
	public boolean hasStatus(Status s)
	{
		if ( s == null ) {
			throw new IllegalArgumentException("s cannot be NULL");
		}
		return status == s ;
	}

	@Override
	public void addJob(IJob job)
	{
		if ( job == null ) {
			throw new IllegalArgumentException("job cannot be NULL");
		}
		this.jobs.add( job );
	}

}
