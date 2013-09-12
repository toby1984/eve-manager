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
package de.codesourcery.planning.demo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IFactoryManager;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.IProductionPlan;
import de.codesourcery.planning.IProductionPlanTemplate;
import de.codesourcery.planning.IResourceManager;
import de.codesourcery.planning.ISlotType;
import de.codesourcery.planning.impl.SimpleFactory;
import de.codesourcery.planning.impl.SimpleFactoryManager;
import de.codesourcery.planning.impl.SimpleJobTemplate;
import de.codesourcery.planning.impl.SimpleProductionSlot;
import de.codesourcery.planning.impl.SimpleResourceManager;

public class SampleJobSchedulingStrategyTest
{

	private static final SimpleDateFormat DATE_FORMAT = 
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static final IProductionLocation LOCATION1 = new IProductionLocation() {
		@Override
		public String toString()
		{
			return "Location #1";
		}
	};
	
	
	private static final ISlotType INVENTION_SLOT = new ISlotType() {
		@Override
		public boolean accepts(IJobTemplate t) {
			return ( t instanceof InventionJob);
		}
	};
	
	private static final ISlotType COPY_SLOT = new ISlotType() {
		@Override
		public boolean accepts(IJobTemplate t)
		{
			return ( t instanceof CopyJob);
		}
	};

	private static class CopySlot extends SimpleProductionSlot {
		public CopySlot(String name) { super( name , COPY_SLOT , LOCATION1); }
	}
	
	private static class InventionSlot extends SimpleProductionSlot {
		public InventionSlot(String name) { super( name , INVENTION_SLOT , LOCATION1 ); }
	}
	
	private static final class InventionJob extends SimpleJobTemplate implements SampleJobTemplate {

		private final String name; 
		@Override
		public String toString() { return name; }
		
		public InventionJob(String name , int runs) {  super(runs, JobMode.AUTOMATIC ); this.name=name; }

		@Override
		public long calcCosts(IFactorySlot slot,int runs) { return 0; }

		@Override
		public Duration calcDuration(IFactorySlot slot,int runs)
		{
			if ( ! slot.getType().accepts( this ) ) {
				throw new IllegalArgumentException("Invalid slot "+slot);
			}
			
			return Duration.minutes( runs );
		}
	}
	
	
	private static final class CopyJob extends SimpleJobTemplate implements SampleJobTemplate {

		private final String name;
		
		public CopyJob(String name , int runs) {  super( runs, JobMode.AUTOMATIC ); this.name = name; }

		@Override
		public String toString() { return name; }
		
		@Override
		public long calcCosts(IFactorySlot slot,int runs) { return 0; }

		@Override
		public Duration calcDuration(IFactorySlot slot,int runs)
		{
			if ( ! slot.getType().accepts( this ) ) {
				throw new IllegalArgumentException("Invalid slot "+slot);
			}
			
			return Duration.minutes( runs );
		}
	}
	
	private static final class MyResourceManager extends SimpleResourceManager {

		private Date now = new Date();
		
		@Override
		public IResourceManager snapshot()
		{
			MyResourceManager result = new MyResourceManager();
			result.now = new Date( this.now.getTime() );
			cloneInstance( result );
			return result;
		}

		@Override
		public Date getTimestamp()
		{
			return now;
		}
		
	}
	
	
	public static void main(String[] args)
	{
		
		/*
		 * Setup a factory with two slots.
		 */


		 final SimpleFactory f = new SimpleFactory("Factory no. 1");
		 f.addSlot( new CopySlot("Copy slot #1") );
		 f.addSlot( new InventionSlot("Invention slot #1") );
		 
		final IFactoryManager manager = new SimpleFactoryManager(new MyResourceManager() );
		 manager.addFactory( f );
		
		final List<IJobTemplate> jobs = 
			new ArrayList<IJobTemplate>();
		
		final CopyJob copyJob1 = new CopyJob( "Copy Hammerhead I blueprint #1" , 10 );
		final CopyJob copyJob2 = new CopyJob( "Copy Hammerhead I blueprint #2", 10 );
		final InventionJob inventionJob1 = new InventionJob( "Invent Hammerhead II from Copy Job #1" , 10 );
		final InventionJob inventionJob2 = new InventionJob( "Invent Hammerhead II from Copy Job #2" , 10 );
		
		inventionJob1.addDependency( copyJob1 );
		inventionJob2.addDependency( copyJob2 );
		
		jobs.add( inventionJob1 );
		jobs.add( inventionJob2 );
		
		final IProductionPlanTemplate template = new IProductionPlanTemplate() {

			@Override
			public List<IJobTemplate> getJobTemplates()
			{
				return jobs;
			}
		};
			
		final IProductionPlan productionPlan = 
			new SimpleJobSchedulingStrategy().calculateProductionPlan( manager , template );
		
		final Date now = new Date();
		
		for ( IFactorySlot s : f.getSlots() ) {
			System.out.println("--------- Slot: "+s+" ----------");
			final List<IJob> sorted = new ArrayList<IJob>( s.getJobs() );
			Collections.sort( sorted , IJob.START_DATE_COMPARATOR );
			for ( IJob j : sorted ) {
				System.out.println( toString( j , now ) );
			}
		}
	}
	
	private static String listDependencyNames(IJob j) {
		
		final StringBuilder result =
			new StringBuilder();
		
		result.append( j.toString() );
		
		if ( ! j.getDependentJobs().isEmpty() ) {
			result.append(" <- { ");
			for ( IJob dep : j.getDependentJobs() ) {
				result.append( "<- ").append( listDependencyNames( dep ) );
			}
			result.append(" } ");
		}
		
		return result.toString();
	}
	
	private static String toString(Date date) {
		return DATE_FORMAT.format( date );
	}
	
	private static String toString(IJob j,Date now) {
		
		final StringBuilder result =
			new StringBuilder( toString( j.getStartDate() ) + " until "+
					toString( j.getEarliestFinishingDate( ) )+
					" - "+j.getDuration()+" - ( "+j.getTotalDuration()+" total ) - "+j );
			
		if ( ! j.getDependentJobs().isEmpty() ) {
			result.append(" <- { ");
			for ( Iterator<IJob> it = j.getDependentJobs().iterator() ; it.hasNext() ; ) {
				result.append( listDependencyNames( it.next() ) );
				if ( it.hasNext() ) {
					result.append(" , ");
				}
			}
			result.append(" } ");
		}
		return result.toString();
	}
}
