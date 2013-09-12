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
package de.codesourcery.planning.impl;

import java.util.Date;
import java.util.List;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IFactoryManager;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionPlan;
import de.codesourcery.planning.IProductionPlanTemplate;

/**
 * Simple scheduling strategy that tries to balance
 * utilization across slots.
 * 
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class SimpleJobSchedulingStrategy extends AbstractJobSchedulingStrategy 
{

	protected IProductionPlan createProductionPlan(IFactoryManager manager, IProductionPlanTemplate template) {
		return new SimpleProductionPlan(manager,template);
	}
	
	protected IFactorySlot chooseProductionSlot(IJobTemplate template , 
			Date desiredStartDate,List<IFactorySlot> candidates) 
	{
		IFactorySlot targetSlot = null;
		float utilization = 0.0f;

		final DateRange utiRange = new DateRange( desiredStartDate , Duration.oneMonth() );

		for ( IFactorySlot s : candidates ) {
			{
				// use slot with lowest utilization
				final float u = s.getUtilization( utiRange ); 
				if ( targetSlot == null || u < utilization ) {
					targetSlot = s;
					utilization = u;
				}
			}
		}
		
		if ( targetSlot == null ) {
			throw new RuntimeException("Unable to find usable/free  slot to process "+template);
		}
		return targetSlot;
	}

}
