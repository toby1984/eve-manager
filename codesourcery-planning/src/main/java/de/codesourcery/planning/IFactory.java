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
import java.util.Map;

/**
 * A factory.
 * 
 * <pre>
 * Each factory may have zero or more {@link IFactorySlot}s. Each
 * slot may have zero or more {@link IJob}s assigned.
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IFactory
{
	/**
	 * Adds a slot to this factory.
	 * 
	 * @param slot
	 */
	public void addSlot(IFactorySlot slot);
	
	/**
	 * Removes a slot from this factory.
	 * 
	 * @param slot
	 */
	public void removeSlot(IFactorySlot slot);

	/**
	 * Returns all production slots of this factory.
	 * 
	 * @return
	 */
	public List<IFactorySlot> getSlots();
	
	/**
	 * Returns a map of all production slots, grouped by their type.
	 * @return
	 */
	public Map<ISlotType,List<IFactorySlot>> getSlotsGroupedByType();
	
	/**
	 * Returns the total utilization (in percent) of
	 * this factory for a given date range.
	 * 
	 * <pre>
	 * The returned value is
	 * 
	 * duration( duration of all jobs ) / duration( dateRange ) 
	 * </pre>
	 * @param startDate
	 * @param duration
	 * @return utilization in percent (0.0 ... 1.0)
	 */
	public float getUtilization(Date startDate,Date endDate);
	
	/**
	 * Returns the factory manager of this factory.
	 * 
	 * @return
	 */
	public IFactoryManager getFactoryManager();

	/**
	 * Sets the manager for this factory.
	 * 
	 * @param simpleFactoryManager
	 */
	public void setFactoryManager(IFactoryManager simpleFactoryManager);
	
	/**
	 * Returns all jobs for a given job template.
	 *  
	 * This method scans all production slots
	 * for jobs that refer to the given
	 * job template.
	 * 
	 * @param t
	 * @return 
	 */
	public List<IJob> getJobsForTemplate(IJobTemplate t);
	
	/**
	 * Returns the production slot a given
	 * job runs in.
	 *  
	 * @param job
	 * @return production slot or <code>null</code> if the job
	 * does not run in any of this factories production slots.
	 */
	public IFactorySlot getSlotFor(IJob job);
	
}
