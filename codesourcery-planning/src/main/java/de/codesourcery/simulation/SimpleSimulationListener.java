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
import de.codesourcery.planning.IProductionJob;
import de.codesourcery.planning.IResourceManager;

/**
 * {@link ISimulationListener} implementation that knows about
 * how to handle {@link IProductionJob}s
 * properly.
 * 
 * This implementation calls {@link IResourceManager#consume(de.codesourcery.planning.IResourceType, de.codesourcery.planning.IResourceLocation, double)}
 * and {@link IResourceManager#produce(de.codesourcery.planning.IResourceType, de.codesourcery.planning.IResourceLocation, double)}
 * right before a job gets started / after a job has finished executing. 
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class SimpleSimulationListener implements ISimulationListener
{
	private Date simulationTime;
	private final IResourceManager resourceManager;
	
	public SimpleSimulationListener(Date simulationStartDate , IResourceManager resourceManager) {
		if ( resourceManager == null ) {
			throw new IllegalArgumentException("resourceManager cannot be NULL");
		}
		if ( simulationStartDate == null ) {
			throw new IllegalArgumentException("simulationStartDate cannot be NULL");
		}
		this.simulationTime = simulationStartDate;
		this.resourceManager = resourceManager.snapshot();
	}
	
	@Override
	public void afterJobEnd(IFactorySlot slot , IJob job, SimulationClock clock)
	{
		if ( job instanceof IProductionJob ) {
			((IProductionJob) job).addProducedResources( slot, resourceManager );
		}
	}

	@Override
	public void afterJobStart(IFactorySlot slot , IJob job, SimulationClock clock)
	{
	}

	@Override
	public void beforeJobEnd(IFactorySlot slot , IJob job, SimulationClock clock)
	{
	}

	@Override
	public void beforeJobStart(IFactorySlot slot , IJob job, SimulationClock clock)
	{
		if ( job instanceof IProductionJob ) {
			((IProductionJob) job).consumeRequiredResources( slot , resourceManager );
		}		
	}

	@Override
	public void clockAdvanced(SimulationClock clock)
	{
		if ( clock == null ) {
			throw new IllegalArgumentException("clock cannot be NULL");
		}
		this.simulationTime = clock.getTime();
	}
	
	/**
	 * Returns the available resources at
	 * the current simulation time.
	 *  
	 * available/missing 
	 * @return
	 * @see #getSimulationTime()
	 */
	public IResourceManager getResourceManagerSnapshot() {
		return resourceManager;
	}
	
	/**
	 * Returns the current simulation time.
	 * @return
	 */
	public Date getSimulationTime()
	{
		return simulationTime;
	}

	@Override
	public boolean hasManualJobBeenStarted(IJob job, Date currentDate)
	{
		return true;
	}

}
