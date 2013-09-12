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
package de.codesourcery.simulation;

import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;

import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJob.JobStatus;
import de.codesourcery.planning.IJobTemplate.JobMode;

/**
 * Simulates the execution of {@link IJob}s over time.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class ProductionSimulator
{
	
	private IdentityHashMap<IJob,JobProxy> jobProxies = 
		new IdentityHashMap<IJob, JobProxy>();
	
	private SimulationClock clock;
	private final List<IFactory> factories;
	private ISimulationListener callback;
	
	public ProductionSimulator(List<IFactory> factories) {
		if ( factories == null ) {
			throw new IllegalArgumentException("factories cannot be NULL");
		}
		this.factories = factories;
	}
	
	private Action createFinishingAction(final JobProxy proxy) {
		
		final IJob thisJob = proxy.getJob();
		
		return new Action() {
			
			@Override
			public String toString()
			{
				return "End of job "+thisJob.getName();
			}
			
			@Override
			public void run(SimulationClock clock)
			{
				
				callback.beforeJobEnd( proxy.getSlot() , thisJob , clock );
				proxy.setStatus( IJob.JobStatus.FINISHED );
				callback.afterJobEnd( proxy.getSlot() , thisJob , clock );
				
				// schedule all jobs 
				// that depend on completion
				// of this job
				for ( IFactory f : factories ) {
					for ( IFactorySlot slot : f.getSlots() ) {
						for ( IJob realJob : slot.getJobs() ) {
							if ( realJob.dependsOn( thisJob ) ) {
								
								Action action;
								Date date;
								
								if ( ! mayStart( realJob , slot ) ) {
									continue;
								}
								
								if ( realJob.getTemplate().hasJobMode( JobMode.AUTOMATIC ) ) 
								{
									action = createStartAction( getSimulationProxy( realJob , slot ) );
									date = realJob.getStartDate();
								} 
								else if ( realJob.getTemplate().hasJobMode( JobMode.MANUAL ) ) 
								{
									if ( callback.hasManualJobBeenStarted( realJob, clock.getTime() ) ) {
										action = createStartAction( getSimulationProxy( realJob , slot ) );
										date = clock.getTime();
									} else {
										action = createStateCheckAction( getSimulationProxy( realJob , slot ) );
										date = new Date( clock.getTime().getTime() + ( clock.getResolutionInSeconds() * 1000 ) );
									}
								} else {
									throw new RuntimeException("Don't know how to dependent job "+realJob);
								}
								
								clock.schedule( action , date );
							}
						}
					}
				}
				
			}
		};
	}
	
	private Action createStateCheckAction(final JobProxy jobProxy) {
		return new Action() {

			@Override
			public void run(SimulationClock clock)
			{
				if ( callback.hasManualJobBeenStarted( jobProxy.getJob()  , clock.getTime() ) ) {
					clock.schedule( createStartAction( jobProxy ) , clock.getTime() );
				}
			}
			
			@Override
			public String toString()
			{
				return "Check state of manual job "+jobProxy.getName();
			}
		};
	}
	
	private Action createStartAction( final JobProxy jobProxy ) {
		return new Action() 
		{
			@Override
			public void run(SimulationClock clock)
			{
				callback.beforeJobStart( jobProxy.getSlot() , jobProxy.getJob() , clock );
				jobProxy.setStatus( IJob.JobStatus.PENDING );
				callback.afterJobStart( jobProxy.getSlot() , jobProxy.getJob() , clock );
				clock.schedule( createFinishingAction( jobProxy ) , jobProxy.getJob().getEndDate() );
			}
			
			@Override
			public String toString()
			{
				return "Start of job "+jobProxy.getName();
			}
		};
	}
	
	private static final class JobProxy {

		private final IFactorySlot slot;
		private final IJob job;
		private IJob.JobStatus jobStatus;
		
		public JobProxy(IJob job, IFactorySlot slot) {
			
			if ( job == null ) {
				throw new IllegalArgumentException("job cannot be NULL");
			}
			if ( slot == null ) {
				throw new IllegalArgumentException("slot cannot be NULL");
			}
			
			this.job = job;
			if ( job.getStatus() == null ) {
				throw new IllegalArgumentException("Job has no status set ?");
			}
			
			this.jobStatus = job.getStatus();
			this.slot = slot;
		}
		
		public IFactorySlot getSlot() {
			return slot;
		}
		
		public String getName()
		{
			return job.getName();
		}

		public IJob getJob() {
			return job;
		}
		
		public void setStatus(IJob.JobStatus jobStatus)
		{
			if ( ! this.jobStatus.mayTransitionTo( jobStatus ) ) {
				throw new IllegalStateException("Job "+job+" may not transition from "+this.jobStatus+" -> "+jobStatus);
			}
			this.jobStatus = jobStatus;
		}
		
		public IJob.JobStatus getStatus()
		{
			return jobStatus;
		}
		
		@Override
		public String toString()
		{
			return job.getName()+" , status ="+jobStatus;
		}
	}
	
	private JobProxy getSimulationProxy(final IJob job , final IFactorySlot slot) 
	{
		
		JobProxy result = jobProxies.get( job );
		if ( result == null ) {
			result = new JobProxy( job , slot );
			jobProxies.put( job, result );
		}
		
		return result;
	}
	
	protected boolean mayStart(IJob job,IFactorySlot slot) {
		
		for ( IJob dep : job.getDependentJobs() ) 
		{
			final JobProxy proxy = getSimulationProxy( dep , slot ); 
			if ( proxy.getStatus() != JobStatus.FINISHED )
			{
				return false;
			}
		}
		return true;
	}
	
	public void runSimulation(Date startDate , ISimulationListener callback) {

		if ( this.clock != null ) {
			throw new IllegalStateException("You must not invoke runSimulation() on " +
					"the same simulator instance more than once");
		}
		
		if ( callback == null ) {
			throw new IllegalArgumentException("callback cannot be NULL");
		}
		
		if ( startDate == null ) {
			throw new IllegalArgumentException("Simulation start date cannot be NULL");
		}

		this.callback = callback;
		jobProxies.clear();
		this.callback = callback;
		clock = new SimulationClock( startDate );
		
		// starting with the start date (pun intended)
		// , queue all jobs that do not depend on other jobs
	
		for ( IFactory factory : factories ) {
			for ( IFactorySlot slot : factory.getSlots() ) {
				for ( IJob job : slot.getJobsSortedAscendingByStartTime() ) {
					if ( job.getStartDate().before( startDate ) ) {
						continue;
					}
					if ( job.getDependentJobs().isEmpty() ) {
						clock.schedule( createStartAction( getSimulationProxy( job , slot ) ) , job.getStartDate() );
					}
				}
			}
		}

		clock.start( callback );
	}

	public Date getSimulationEndTime() {
		if ( this.clock == null || ! this.clock.wasStarted() ) {
			throw new IllegalStateException("Simulation has not been started yet");
		}
		if (  this.clock.isStopped() ) {
			return this.clock.getTime();
		}
		throw new IllegalStateException("Simulation is still running.");
	}
}
