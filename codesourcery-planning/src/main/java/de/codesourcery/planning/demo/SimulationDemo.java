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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.planning.IResource;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionJob;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.IResourceFactory;
import de.codesourcery.planning.IResourceManager;
import de.codesourcery.planning.IResourceType;
import de.codesourcery.planning.ISlotType;
import de.codesourcery.planning.IJobTemplate.JobMode;
import de.codesourcery.planning.impl.ResourceAmount;
import de.codesourcery.planning.impl.SimpleFactory;
import de.codesourcery.planning.impl.SimpleJob;
import de.codesourcery.planning.impl.SimpleJobTemplate;
import de.codesourcery.planning.impl.SimpleProductionSlot;
import de.codesourcery.planning.impl.SimpleResource;
import de.codesourcery.planning.impl.SimpleResourceManager;
import de.codesourcery.simulation.ProductionSimulator;
import de.codesourcery.simulation.SimpleSimulationListener;
import de.codesourcery.simulation.SimulationClock;

public class SimulationDemo
{
	
	private static final DateFormat dateFormat =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static final IProductionLocation LOCATION1 = new IProductionLocation() {
		@Override
		public String toString()
		{
			return "Location #1";
		}
	};
	
	private static final class MyJob extends SimpleJob implements IProductionJob {

		private final Collection<ResourceAmount> consumed;
		private final Collection<ResourceAmount> produced;
		
		public MyJob(String name, 
				IJobTemplate jobTemplate, 
				Duration duration,
				Collection<ResourceAmount> consumed, 
				Collection<ResourceAmount> produced) 
		{
			super(name, jobTemplate, duration);

			if ( produced == null ) {
				throw new IllegalArgumentException("produced cannot be NULL");
			}
			
			if ( consumed == null ) {
				throw new IllegalArgumentException("consumed cannot be NULL");
			}
			
			this.consumed = consumed;
			this.produced = produced;
			setStatus( JobStatus.NOT_STARTED );
		}

		@Override
		public void addProducedResources(IFactorySlot slot,
				IResourceManager manager)
		{
			
			for ( ResourceAmount resource : produced ) {
				final IResource res =
					manager.getResource( resource.getResourceType() , slot.getOutputLocation() );
				res.incrementAmount( resource.getAmount());
			}
			
		}

		@Override
		public void consumeRequiredResources(IFactorySlot slot,
				IResourceManager manager)
		{
			for ( ResourceAmount resource : consumed ) {
				final IResource res =
					manager.getResource( resource.getResourceType() , slot.getInputLocation() );
				res.decrementAmount( resource.getAmount() );
			}			
		}
		
	}
	
	private static <T> List<T> toList(T... data) {
		final List<T> result = new ArrayList<T> ();
		for ( T o : data ) {
			result.add( o );
		}
		return result;
	}

	private static List<IFactory> createFactories() throws ParseException {
		// setup factory with dummy data

		final List<IFactory> result = new ArrayList<IFactory>();
		
		final SimpleJobTemplate template = new SimpleJobTemplate( 1 , JobMode.AUTOMATIC ) {};

		final ISlotType type = new ISlotType() {

			@Override
			public boolean accepts(IJobTemplate t)
			{
				return t instanceof SimpleJobTemplate;
			}
		};

		final SimpleFactory factory1 = new SimpleFactory("Factory no. 1");
		final SimpleFactory factory2 = new SimpleFactory("Factory no. 2");
		
		result.add( factory1 );
		result.add( factory2 );
		
		final SimpleProductionSlot slot1 = new SimpleProductionSlot("Slot no. 1" , type , LOCATION1);
		final SimpleProductionSlot slot2 = new SimpleProductionSlot("Slot no. 2 with a long name" , type , LOCATION1);
		final SimpleProductionSlot slot3 = new SimpleProductionSlot("Slot no. 3" , type , LOCATION1 );
		
		factory1.addSlot( slot1 );
		factory1.addSlot( slot2 );
		factory2.addSlot( slot3 );

		final Date diagramStartDate = dateFormat.parse("2009-01-01 00:00:00" );
		
		final IResourceType type1 = new SampleResourceType("Tritanium");
		final IResourceType type2 = new SampleResourceType("Hammerhead I");
		final IResourceType type3 = new SampleResourceType("Hammerhead I BPC");
		final IResourceType type4 = new SampleResourceType("Hammerhead II BPC");
		final IResourceType type5 = new SampleResourceType("Hammerhead II");

		// add jobs
		final SimpleJob job1 = new MyJob(
				"Produce HammerHead I",
				template , 
				Duration.days( 3 ) , 
				toList( new ResourceAmount( type1 , 1 ) , new ResourceAmount( type3 , 0 ) ), 
				toList( new ResourceAmount( type2 , 1 ) )
		);
		
		job1.setStartDate( diagramStartDate );
		slot1.add( job1 );
		
		final SimpleJob job2 = new MyJob(
				"Produce Hammerhead II BPC",
				template  , 
				Duration.days( 3 ).plus( Duration.hours( 12 )  ), 
				toList( new ResourceAmount( type3 , 1 ) ), 
				toList( new ResourceAmount( type4 , 1 ) )				
		);
		
		job2.setStartDate( Duration.oneDay().addTo( diagramStartDate ) );
		slot2.add( job2 );

		final SimpleJob job3 = new MyJob("Produce Hammerhead II",
				template, Duration.days( 3 ) , 
				toList( new ResourceAmount( type4 , 1 ) ,  new ResourceAmount( type2 , 1 )), 
				toList( new ResourceAmount( type5 , 1 ) )					
				);
		
		job3.setStartDate( Duration.oneSecond().addTo( job2.getEndDate() ) );
		job3.addDependentJob( job1 );
		job3.addDependentJob( job2 );
		slot3.add( job3 );
		return result;
	}
	
	private static final class MyResourceManager extends SimpleResourceManager {

		private Date now = new Date();
		
		@Override
		public IResourceManager snapshot()
		{
			MyResourceManager result = new MyResourceManager();
			result.now = new Date( now.getTime() );
			cloneInstance( result );
			return result;
		}

		@Override
		public Date getTimestamp()
		{
			return now;
		}
		
	}
	
	public static void main(String[] args) throws ParseException
	{

		final Date simulationStartDate=
			dateFormat.parse("2008-01-01 00:00:00" );
		
		final IResourceFactory resourceFactory = new IResourceFactory() {

			@Override
			public IResource createResource(IResourceType type,
					IProductionLocation location)
			{
				return new SimpleResource(type , location , 0.0d );
			}

			@Override
			public Date getTimestamp()
			{
				return simulationStartDate;
			}

			@Override
			public IResource cloneResource(IResource resource)
			{
				synchronized( resource ) {
					return new SimpleResource( (SimpleResource) resource);
				}
			}
		};
		
		final IResourceManager resourceManager = new MyResourceManager();
		resourceManager.setResourceFactory( resourceFactory );
		
		final List<IFactory> factories = createFactories();
		final ProductionSimulator simulator =
			new ProductionSimulator( factories  );

		final SimpleSimulationListener callback = 
			new SimpleSimulationListener( simulationStartDate , resourceManager ) 
		{
			@Override
			public void clockAdvanced(SimulationClock clock)
			{
				super.clockAdvanced(clock);
				
				System.out.println("\n===== Simulation time: "+clock.getTime()+" ====");
				printResources( factories , getResourceManagerSnapshot() );
			}
			
			@Override
			public void beforeJobStart(IFactorySlot slot, IJob job,
					SimulationClock clock)
			{
				System.out.println("# Starting job "+job+" at "+clock.getTime() );
				super.beforeJobStart(slot, job, clock);
			}
		};
		
		simulator.runSimulation(  
			simulationStartDate , 
			callback
		);
		
		System.out.println("\n\n=========================================");
		System.out.println("Simulation end: "+simulator.getSimulationEndTime() );
		printResources( factories , callback.getResourceManagerSnapshot() );
	}
	
	private static void printResources(List<IFactory> factories , IResourceManager manager) 
	{
		
		final Set<IProductionLocation> locations = new HashSet<IProductionLocation>();
		
		for ( IFactory f : factories ) {
			for ( IFactorySlot slot : f.getSlots() ) {
				locations.add( slot.getInputLocation() );
				locations.add( slot.getOutputLocation() );
			}
		}
		
		for ( IProductionLocation location : locations ) {
			final List<IResource> r = new ArrayList<IResource>( manager.getResourcesAt( location ) );
//			Collections.sort( r , new Comparator<IResource>() {
//
//				@Override
//				public int compare(IResource o1, IResource o2)
//				{
//					return o1.getType().compareTo( o2.getType() );
//				}
//			} );
			System.out.println("\nResources at "+location+":\n\n");
			for ( IResource resource : r) {
				System.out.println( StringUtils.rightPad( 
						resource.getType().toString() , 25 )+" "+StringUtils.leftPad( ""+resource.getAmount() , 15 ) );
			}
		}
	}
}
