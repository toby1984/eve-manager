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

/**
 * Factory manager.
 * 
 * The factory manager keeps track of all
 * available factories is responsible
 * for scheduling / cancelling production 
 * plans.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IFactoryManager
{

	public List<IFactory> getFactories();
	
	public void addFactory(IFactory factory);
	
	public List<IProductionPlan> getProductionPlans();
	
	public void submitProductionPlan(IProductionPlan plan);
	
	public void cancelProductionPlan(IProductionPlan plan);
	
	public List<IJob> getJobsForTemplate(IJobTemplate t);
	
	public IResourceManager getResourceManager(IFactorySlot slot);
	
}
