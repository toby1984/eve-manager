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

import de.codesourcery.planning.IFactoryManager;

/**
 * A production template that describes all the necessary
 * steps to create some product.
 * 
 * <pre>
 * This template can be considered a 'blueprint' for
 * how to build something, the actual execution
 * plan of a production template is captured
 * in a {@link IProductionPlan} created by
 * a {@link IJobSchedulingStrategy}.
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 * @see IJobSchedulingStrategy#calculateProductionPlan(IFactoryManager, IProductionTemplate)
 */
public interface IProductionPlanTemplate
{
	
	/**
	 * Returns the jobs that need to be run
	 * for this production template.
	 * 
	 * @return
	 */
	public List<IJobTemplate> getJobTemplates();
} 
