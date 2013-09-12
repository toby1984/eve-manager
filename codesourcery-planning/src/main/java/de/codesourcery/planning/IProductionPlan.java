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

import de.codesourcery.planning.IJob.JobStatus;

/**
 * Holds all information / state necessary for the
 * actual execution of a {@link IProductionPlanTemplate}.
 *
 * Note that creating a production plan pre-allocates 
 * production slots using status {@link JobStatus#PROSPECTIVE}
 * and you <b>MUST</b> call either {@link #submit()}
 * or {@link #dispose()} on any <code>IProductionPlan</code>
 * instance you created.
 * 
 * @author tobias.gierke@code-sourcery.de
 * 
 * @see IFactoryManager#cancelProductionPlan(IProductionPlan)
 * @see IFactoryManager#submitProductionPlan(IProductionPlan)
 */
public interface IProductionPlan
{

	public enum Status {
		/**
		 * Indicates that all required resources
		 * for this production plan have been
		 * been allocated but the plan 
		 * has not been submitted for actual execution yet.
		 * 
		 * @see IFactoryManager#submitProductionPlan(IProductionPlan)
		 */
		PROSPECTIVE,
		NOT_STARTED,
		PENDING,
		FINISHED;
	}
	
	public void addJob(IJob job);
	
	/**
	 * Returns the jobs that are part of this production
	 * plan.
	 * 
	 * @return
	 */
	public List<IJob> getJobs();
	
	public Date getStartDate();
	
	/**
	 * Returns the total duration of this production plan.
	 * 
	 * @return
	 */
	public Duration getDuration();
	
	public IProductionPlanTemplate getTemplate();

	public Status getStatus();
	
	public boolean hasStatus(Status s);
	
	/**
	 * Disposes this production plan.
	 * 
	 * @throws IllegalStateException if invoked on
	 * a production plan that has already been submitted using
	 * {@link #submit()}.
	 * 
	 * @see Status#PROSPECTIVE
	 */
	public void dispose();

	/**
	 * Submits this production plan for execution.
	 */
	public void submit(); 
}
