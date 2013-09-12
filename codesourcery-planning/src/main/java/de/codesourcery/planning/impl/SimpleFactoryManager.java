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
import java.util.List;

import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactoryManager;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionPlan;
import de.codesourcery.planning.IResourceManager;
import de.codesourcery.planning.IJob.JobStatus;

public class SimpleFactoryManager implements IFactoryManager
{

	private final List<IFactory> factories =
		new ArrayList<IFactory>();
	
	private final List<IProductionPlan> plans =
		new ArrayList<IProductionPlan>();
	
	private final IResourceManager resourceManager;
	
	public SimpleFactoryManager(IResourceManager manager) {
		if ( manager == null ) {
			throw new IllegalArgumentException("manager cannot be NULL");
		}
		this.resourceManager = manager;
	}
	
	@Override
	public void cancelProductionPlan(IProductionPlan plan)
	{
		// cancel and remove all jobs
		for ( IJob job : plan.getJobs() ) 
		{
			final IFactorySlot slot = findSlotFor( job );
			if ( slot != null ) {
				slot.remove( job );
			}
			
			if ( ! job.hasStatus( JobStatus.CANCELLED , JobStatus.FINISHED ) ) {
				job.setStatus(JobStatus.CANCELLED );
			}
		}
	}
	
	private IFactorySlot findSlotFor(IJob job) {
		
		IFactorySlot result = null;
		for ( IFactory f : getFactories() ) {
			final IFactorySlot tmp = f.getSlotFor( job );
			if ( tmp != null ) {
				if ( result != null ) {
					throw new IllegalStateException("Internal error - job "+job+" runs in more than one production slot ?");
				}
				result = tmp;
			}
		}
		return result;
	}

	@Override
	public List<IFactory> getFactories()
	{
		return factories;
	}

	@Override
	public List<IProductionPlan> getProductionPlans()
	{
		return plans;
	}

	@Override
	public void submitProductionPlan(IProductionPlan plan)
	{
		for ( IJob job : plan.getJobs() ) {
			job.setStatus( JobStatus.NOT_STARTED );
		}
	}

	@Override
	public void addFactory(IFactory factory)
	{
		if ( factory == null ) {
			throw new IllegalArgumentException("factory cannot be NULL");
		}
		factory.setFactoryManager( this );
		this.factories.add( factory );
	}

	@Override
	public List<IJob> getJobsForTemplate(IJobTemplate t)
	{
		final List<IJob> result = new ArrayList<IJob>();
		
		for ( IFactory f : getFactories() ) {
			result.addAll( f.getJobsForTemplate( t ) );
		}
		return result;
	}

	@Override
	public IResourceManager getResourceManager(IFactorySlot slot)
	{
		return resourceManager;
	}

}
