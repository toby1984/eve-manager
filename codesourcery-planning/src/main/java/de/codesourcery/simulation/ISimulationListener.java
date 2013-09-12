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

import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IJobTemplate.JobMode;

public interface ISimulationListener
{
	
	/**
	 * Invoked each time AFTER the simulation clock
	 * has been advanced.
	 * @param clock
	 */
	public void clockAdvanced(SimulationClock clock);
	
	/**
	 * Invoked right before a job's status gets switched
	 * to {@link IJob.JobStatus#PENDING}.
	 *
	 * @param slot The production slot the job will be run in
	 * @param job
	 * @param clock
	 */
	public void beforeJobStart(IFactorySlot slot , IJob job,SimulationClock clock);
	
	/**
	 * Invoked right after a job's status has been switched
	 * to {@link IJob.JobStatus#PENDING}.
	 * 
	 * @param job
	 * @param slot The production slot the job is running in
	 * @param clock
	 */	
	public void afterJobStart(IFactorySlot slot , IJob job,SimulationClock clock);
	
	/**
	 * Invoked right before a job's status is switched
	 * to {@link IJob.JobStatus#FINISHED}.
	 * 
	 * @param slot The production slot the job will be finishing in
	 * @param job
	 * @param clock
	 */	
	public void beforeJobEnd(IFactorySlot slot , IJob job,SimulationClock clock);
	
	/**
	 * Invoked right after a job's status has been switched
	 * to {@link IJob.JobStatus#FINISHED} but BEFORE
	 * any of the jobs depending on it are started.
	 * 
	 * @param slot The production slot the job has finished in
	 * @param job
	 * @param clock
	 */	
	public void afterJobEnd(IFactorySlot slot , IJob job,SimulationClock clock);
	
	/**
	 * Callback that gets invoked by the simulation
	 * to determine whether a manual job 
	 * (see {@link IJobTemplate#getJobMode() }) 
	 * has been started on a given date.
	 *
	 * @see JobMode#MANUAL
	 */
	public boolean hasManualJobBeenStarted(IJob job,Date currentDate);
}
