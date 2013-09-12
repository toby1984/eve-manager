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

import java.util.Date;
import java.util.List;

/**
 * A slot in a <code>IFactory</code>.
 * 
 * <pre>
 * {@link IJob}s can only be assigned to
 * slots that accept them (see {@link ISlotType#accepts(IJobTemplate)}.
 * At any given time, a slot may execute at most one
 * {@link IJob}.
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 */
public interface IFactorySlot
{
	/**
	 * Returns this slot's type.
	 * @return
	 */
	public ISlotType getType();
	
	/**
	 * Tests whether this slot
	 * has a given type.
	 * 
	 * @param type
	 * @return
	 */
	public boolean hasType(ISlotType type);
	
	/**
	 * Sets the factory this slot belongs 
	 * to.
	 * 
	 * @param factory
	 */
	public void setFactory(IFactory factory);
	
	/**
	 * Returns the factory
	 * this slot belongs to.
	 * @return
	 */
	public IFactory getFactory();
	
	/**
	 * Returns all jobs of
	 * this slot.
	 * 
	 * @return
	 */
	public List<IJob> getJobs();
	
	/**
	 * Checks whether a given
	 * job is assigned to this production slot.
	 * 
	 * @param job
	 * @return <code>true</code> if the job
	 * is assigned to this production slot.
	 */
	public boolean contains(IJob job);
	
	/**
	 * Queues a new job.
	 * 
	 * @param job
	 * @throws IllegalArgumentException if the job is null or
	 * overlaps with another job already added to this slot
	 * 
	 */
	public void add(IJob job);
	
	/**
	 * Removes a job from this production slot.
	 * 
	 * @param job
	 */
	public void remove(IJob job);
	
	/**
	 * Returns all jobs of this
	 * slot sorted ascending by their start times.
	 * @return
	 */
	public List<IJob> getJobsSortedAscendingByStartTime();
	
	/**
	 * Returns a percentage-based
	 * utilization for this slot within
	 * a given date range.
	 * 
	 * @param dateRange
	 * @return 0.0f - 1.0f
	 */
	public float getUtilization(DateRange dateRange);
	
	/**
	 * Returns all jobs that belong to
	 * a given {@link IJobTemplate}
	 * and are queued in this slot.
	 * 
	 * @param t
	 * @return
	 */
	public List<IJob> getJobsForTemplate(IJobTemplate t);
	
	/**
	 * Returns the job that might run / runs on a 
	 * given date.
	 * 
	 * This method might very well return
	 * a prospective job.
	 * 
	 * @param date
	 * @return job that is scheduled to run on the
	 * given date, <code>null</code> if there currently
	 * is no job scheduled at the given date.
	 * @see IJob#getStatus()
	 * @see IJob.JobStatus#PROSPECTIVE
	 */
	public IJob getJobOn(Date date);

	/**
	 * Returns all jobs that are running
	 * on a given day.
	 * 
	 * @param day day (time of day gets ignored)
	 * @return
	 */
	public List<IJob> getJobsOnDay(Date day);

	/**
	 * Returns whether this slot
	 * (potentially) has any jobs
	 * running on a given day.
	 * 
	 * @param day day (time of day gets ignored)
	 * @return <code>true</code> if at least one job
	 * is running / might run on this day
	 */	
	public boolean hasJobsOnDay(Date day);
	
	/**
	 * Returns the output location where materials
	 * produced by jobs in this slot will be located.
	 * 
	 * @return
	 */
	public IProductionLocation getOutputLocation();
	
	/**
	 * Returns the input location where materials
	 * produced by jobs in this slot will be located.
	 * 
	 * @return
	 */	
	public IProductionLocation getInputLocation();
}
