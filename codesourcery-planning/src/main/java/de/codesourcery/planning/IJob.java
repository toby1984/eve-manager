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
package de.codesourcery.planning;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.codesourcery.planning.impl.AbstractJob;
import de.codesourcery.planning.impl.SimpleJob;

/**
 * A job that has been assigned to a specific {@link IFactorySlot}.
 * 
 * <pre>
 * Jobs are always linked to the {@link IJobTemplate} they
 * have been created from.
 * 
 * Jobs come in two flavors:
 * 
 * 1.) Simple jobs
 * 
 * Simple jobs are jobs that do not depend on 
 * the completion of other jobs before they
 * can be started.
 * 
 * 2.) Complex jobs
 * 
 * Complex jobs depend on the completion
 * of AT LEAST one other job before they
 * can be started. 
 *  
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 * @see IFactorySlot#add(IJob, Date)
 * @see SimpleJob
 * @see SimpleJob
 * @see AbstractJob
 */
public interface IJob
{

	/**
	 * Comparator that compares to jobs by 
	 * their start dates.
	 */
	public static Comparator<IJob> START_DATE_COMPARATOR = new Comparator<IJob>() {

		@Override
		public int compare(IJob o1, IJob o2)
		{
			return o1.getStartDate().compareTo( o2.getStartDate() );
		}
	};
	
	/**
	 * A jobs processing status.
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
	public enum JobStatus {
		/**
		 * A planned job that
		 * has not been finalized.
		 */
		PROSPECTIVE {
			@Override
			public boolean mayTransitionTo(JobStatus next)
			{
				return next == NOT_STARTED || next == CANCELLED; 
			}
		},
		/**
		 * A scheduled job
		 * that has not started yet.
		 */
		NOT_STARTED {

			@Override
			public boolean mayTransitionTo(JobStatus next)
			{
				return next == PENDING || next == CANCELLED;
			}
			
		},
		/**
		 * A scheduled job
		 * that is currently running.
		 */
		PENDING {

			@Override
			public boolean mayTransitionTo(JobStatus next)
			{
				return next == CANCELLED || next == FINISHED;
			}
		},
		CANCELLED {

			@Override
			public boolean mayTransitionTo(JobStatus next)
			{
				return false;
			}
			
		},
		/**
		 * A scheduled job
		 * that has finished execution.
		 */
		FINISHED {
			@Override
			public boolean mayTransitionTo(JobStatus next)
			{
				return false;
			}
		};
		
		public abstract boolean mayTransitionTo(JobStatus next);
	}
	
	public String getName();
	
	/**
	 * Returns the {@link IJobTemplate} this job
	 * was created from.
	 * 
	 * @return
	 */
	public IJobTemplate getTemplate();

	/**
	 * Returns the jobs that need 
	 * to be finished before this job can start.
	 * @return
	 */
	public List<IJob> getDependentJobs();
	
	/**
	 * Adds a job that needs to finish
	 * before this job may start.
	 *   
	 * @param job
	 */
	public void addDependentJob(IJob job);
	
	/**
	 * Check whether this job depends
	 * upon completion of a given job.
	 * @param job
	 * @return
	 */
	public boolean dependsOn(IJob job);
	
	/**
	 * Sets this job's status.
	 * 
	 * @param status
	 */
	public void setStatus(JobStatus status);
	
	/**
	 * Returns this job's status.
	 * 
	 * @return
	 */
	public JobStatus getStatus();
	
	/**
	 * Tests whether this job has a 
	 * given status.
	 * 
	 * @param s
	 * @return
	 */
	public boolean hasStatus(JobStatus... s);
	
	/**
	 * Sets the starting date
	 * for this job.
	 * 
	 * This method may <b>only</b>
	 * be invoked on simple jobs
	 * (since complex ones 
	 * have a derived starting date).
	 * 
	 * @param s
	 * @throws UnsupportedOperationException if this
	 * is a complex job.
	 */
	public void setStartDate(Date s);
	
	/**
	 * Returns the date range this 
	 * job covers.
	 * 
	 * @return daterange( getStartDate() , getEndDate() )
	 */
	public DateRange getDateRange();
	
	/**
	 * Returns the earliest starting date
	 * of this job.
	 * 
	 * For simple jobs, this is their
	 * assigned start date , for 
	 * complex jobs this is the latest
	 * earliest finishing date
	 * of all jobs this job depends on.
	 * 
	 * @return
	 */
	public Date getEarliestStartDate();
	
	/**
	 * Returns the earliest finishing date
	 * of this job.
	 * 
	 * For simple jobs, this is always 
	 * ( startDate + duration ) ,
	 * for complex jobs
	 * this is the latest 
	 * earliest finishing date
	 * of all jobs this job depends on
	 * plus the duration of THIS job.
	 * 	 
	 * @return
	 */
	public Date getEarliestFinishingDate();
	
	/**
	 * Returns the starting date for this job.
	 * 
	 * For simple jobs, this method
	 * returns the assigned starting date.
	 * For complex jobs, this method returns the 
	 * latest earliest finishing date
	 * of all jobs this job depends on.
	 * 
	 * @return 
	 * @see #getEarliestStartDate()
	 * @see #getEarliestFinishingDate()
	 */
	public Date getStartDate();
	
	/**
	 * Returns the duration of THIS job.
	 * 
	 * The returned value does <b>NOT</b>
	 * include the duration of
	 * any jobs this job depends on. Use
	 * {@link #getTotalDuration()} to
	 * get this.
	 * 
	 * @return this job's duration. Be careful,
	 * jobs <b>MAY</b> return an {@link Duration#UNKNOWN}
	 * as well !!
	 * 
	 * @see Duration#isUnknown()
	 */
	public Duration getDuration();
	
	/**
	 * Returns the end date of this job.
	 * 
	 * @return getStartDate() + getDuration()
	 */
	public Date getEndDate();
	
	/**
	 * Returns the total duration of this job.
	 * 
	 * The returned value does include duration
	 * of all jobs this job depends on.
	 * 
	 * @return this job's total duration. Be careful,
	 * jobs <b>MAY</b> return an {@link Duration#UNKNOWN}
	 * as well !!
	 * 
	 * @see Duration#isUnknown()
	 */	
	public Duration getTotalDuration();
	
	/**
	 * Returns the duration left for this
	 * job after a given date.
	 * 
	 * <pre>
	 * this.earliestFinishingDate < startDate: Duration.ZERO
	 * this.earliestStartDate > startDate: Duration.ZERO
	 * this.earliestStartDate <= startDate <= this.earliestFinishingDate: Duration( earliestFinishingDate - startDate )
	 * </pre>
	 * @param startDate
	 * @return
	 */
	public Duration getDurationFrom(Date startDate);
	
	/**
	 * Returns the amount of time this job
	 * will be running within a given date range.
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public Duration getDurationWithin(Date startDate,Date endDate);

	/**
	 * Sets the number of runs for this job.
	 * 
	 * @param runs Number of runs, must be >=1
	 */
	public void setRuns(int runs);
	
	/**
	 * Returns the number of runs for this job.
	 * @return
	 */
	public int getRuns();
	
	/**
	 * Check whether this job runs at a given date.
	 * @param date
	 * @return
	 */
	public boolean runsAt(Date date);
}
