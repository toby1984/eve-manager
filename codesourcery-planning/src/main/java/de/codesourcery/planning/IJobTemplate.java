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

import java.util.List;

public interface IJobTemplate
{

	public enum JobMode
	{
		/**
		 * Manual jobs require user confirmation before they are considered
		 * 'started'.
		 * 
		 * The date of this confirmation will be used to adjust the start date of all
		 * jobs that depend on this one.
		 */
		MANUAL,
		/**
		 * A job that starts automatically at the given start date (unless cancelled before).
		 */
		AUTOMATIC;
	}

	/**
	 * Returns templates for other jobs that must finish before this one can be
	 * started.
	 * 
	 * @return
	 */
	public List<IJobTemplate> getDependencies();

	/**
	 * Returns the mode of this job.
	 * 
	 * Jobs may either be 'automatic' (=start
	 * automagically when their start date is reached)
	 * or manual (require explicit starting, the
	 * job's actual start date is the date of this action).
	 *  
	 * @return
	 * @see JobMode#AUTOMATIC
	 * @see JobMode#MANUAL
	 */
	public JobMode getJobMode();

	public boolean hasJobMode(JobMode type);

	/**
	 * Returns the number of 'runs' this job template has.
	 * 
	 * <pre> Up to <code>runs</code> {@link IJob} instances may be created to
	 * execute this job template in parallel.
	 * 
	 * Jobs that have more than one run and a {@link #supportsParallelExecution()}
	 * method that yields <code>true</code> are eligible for parallel execution
	 * (up to <code>runs</code> instances of this job may be run in parallel).
	 * <pre<
	 * 
	 * @return
	 */
	public int getRuns();

	/**
	 * Scheduler hint that indicates whether this job may be sped-up by dividing
	 * it into sub-tasks and running those in parallel.
	 * 
	 * Up to {@link #getRuns()} parallel jobs may be started.
	 * 
	 * @return
	 */
	public boolean supportsParallelExecution();

}
