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

/**
 * A strategy that utilizes a {@link IProductionPlanTemplate} and a {@link IFactoryManager}
 * to calculate an actual production plan that may be executed by one or more {@link IFactory} instances.
 * 
 * @author tobias.gierke@code-sourcery.de
 * @see IFactoryManager
 * @see IProductionPlanTemplate
 */
public interface IJobSchedulingStrategy
{

	/**
	 * Calculate production plan.
	 * 
	 * This method call already allocates all resources
	 * required for the actual execution of the returned
	 * production plan. It is therefore <b>mandator</b>
	 * to either {@link IProductionPlan#submit()} <b>OR</b>
	 * {@link IProductionPlan#dispose()} upon the 
	 * result of this method ; otherwise these
	 * resources will stay allocated forever.
	 * 
	 * @param manager used to obtain {@link IFactorySlot}s for the actual execution from 
	 * @param template the template for which a production plan should be created
	 * @return
	 * @see IProductionPlan#submit()
	 * @see IProductionPlan#dispose()
	 */
	public IProductionPlan calculateProductionPlan(IFactoryManager manager , IProductionPlanTemplate template);

}
