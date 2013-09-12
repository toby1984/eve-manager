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

import de.codesourcery.simulation.ProductionSimulator;

/**
 * A job that consumes and/or produces something.
 * 
 * The methods in this interface are used
 * by the {@link ProductionSimulator} to
 * predict available / required resource amounts
 * at a given point in time.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IProductionJob extends IJob
{

	/**
	 * Consume all resources that are required
	 * for this job's execution.
	 * 
	 * @param slot the slot this job will be started in 
	 * @param manager
	 */
	public void consumeRequiredResources(IFactorySlot slot , IResourceManager manager);
	
	/**
	 * Register resources produced by this job.
	 * @param slot the slot this job has been running in
	 * @param manager
	 */
	public void addProducedResources(IFactorySlot slot , IResourceManager manager);
}
