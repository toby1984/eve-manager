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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactoryManager;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.ISlotType;

public class SimpleFactory implements IFactory 
{

	private IFactoryManager manager;
	private final String name;
	private final List<IFactorySlot> slots = 
		new ArrayList<IFactorySlot>();
	
	public SimpleFactory(String name) {
		
		if ( StringUtils.isBlank(name) ) {
			throw new IllegalArgumentException("name cannot be blank.");
		}

		this.name = name;
	}
	
	public void setFactoryManager(IFactoryManager manager)
	{
		if ( manager == null ) {
			throw new IllegalArgumentException("manager cannot be NULL");
		}
		
		this.manager = manager;
	}
	
	@Override
	public void addSlot(IFactorySlot slot)
	{
		 if ( slot == null ) {
			throw new IllegalArgumentException("slot cannot be NULL");
		}
		 slot.setFactory( this );
		 slots.add( slot );
	}

	public String getName()
	{
		return name;
	}

	@Override
	public List<IFactorySlot> getSlots()
	{
		return slots;
	}

	@Override
	public float getUtilization(Date startDate, Date endDate)
	{
		
		if ( slots.isEmpty() ) {
			return 0.0f;
		}
		
		float result = 0.0f;
		for ( IFactorySlot s : slots ) {
			result += s.getUtilization( new DateRange( startDate , endDate ) );
		}
		return result / (float) slots.size();
	}

	@Override
	public void removeSlot(IFactorySlot slot)
	{
		slots.remove( slot );
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public IFactoryManager getFactoryManager()
	{
		return manager;
	}

	@Override
	public List<IJob> getJobsForTemplate(IJobTemplate t)
	{
		
		final List<IJob> result = new ArrayList<IJob>();
		
		for ( IFactorySlot s : getSlots() ) {
			result.addAll( s.getJobsForTemplate( t ) );
		}
		return result;
	}

	@Override
	public Map<ISlotType, List<IFactorySlot>> getSlotsGroupedByType()
	{
		final Map<ISlotType, List<IFactorySlot>> slotsByType = 
			new HashMap<ISlotType, List<IFactorySlot>>();
		
		for ( IFactorySlot s : getSlots() ) {
			List<IFactorySlot> list = slotsByType.get( s.getType() );
			if ( list == null ) {
				list = new ArrayList<IFactorySlot>();
				slotsByType.put( s.getType() , list );
			}
			list.add( s );
		}
		return slotsByType;
	}

	@Override
	public IFactorySlot getSlotFor(IJob job)
	{
		
		for ( IFactorySlot slot : getSlots() ) 
		{
			if ( slot.contains( job ) ) {
				return slot;
			}
		}
		return null;
	}

}