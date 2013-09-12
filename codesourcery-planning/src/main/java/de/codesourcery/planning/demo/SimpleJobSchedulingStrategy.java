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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactoryManager;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobSchedulingStrategy;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionPlan;
import de.codesourcery.planning.IProductionPlanTemplate;
import de.codesourcery.planning.ISlotType;
import de.codesourcery.planning.IJob.JobStatus;
import de.codesourcery.planning.impl.SimpleJob;

public class SimpleJobSchedulingStrategy implements IJobSchedulingStrategy
{

	protected float getCostFactor(IFactory factory,IFactorySlot slot) {
		return 1.0f;
	}

	public static final class MyEdge {

		public MyEdge() {
		}
	}

	protected IJob createJob(IFactoryManager factoryManager , IFactorySlot slot , SampleJobTemplate t) {

		final int runs = t.getRuns();

		final Duration duration = t.calcDuration( slot , runs );

		IJob result;
		if ( t.getDependencies().isEmpty() ) {
			result = new SimpleJob( t.toString() , t , duration , runs );
		} else {

			result = new SimpleJob( t.toString() , t , t.calcDuration( slot , runs ) );

			// link with other dependencies
			for ( IJobTemplate dependency : t.getDependencies() ) 
			{
				final List<IJob> dependencies = factoryManager.getJobsForTemplate( dependency );
				if ( dependencies.isEmpty() ) {
					throw new RuntimeException("Internal error - no jobs that "+result+" depend on has been queued yet ?");
				}

				for ( IJob depJob : dependencies ) {
					result.addDependentJob( depJob );
				}
			}
		}
		result.setStatus( JobStatus.PROSPECTIVE );
		return result;
	}

	@Override
	public IProductionPlan calculateProductionPlan(IFactoryManager manager,IProductionPlanTemplate template)
	{

		if ( manager == null ) {
			throw new IllegalArgumentException("manager cannot be NULL");
		}

		if ( template == null ) {
			throw new IllegalArgumentException("template cannot be NULL");
		}

		if ( template.getJobTemplates().isEmpty() ) {
			throw new IllegalArgumentException("Template has no jobs ?");
		}

		final Date now = new Date();

		/*
		 * Create a dependency graph.
		 */
		final DefaultDirectedGraph< GraphNode , MyEdge > graph =
			new DefaultDirectedGraph<GraphNode , MyEdge>( MyEdge.class );

		final IdentityHashMap<IJobTemplate, GraphNode> nodes =
			new IdentityHashMap<IJobTemplate, GraphNode>();

		for ( IJobTemplate t : template.getJobTemplates() ) {
			graph.addVertex( addToGraph(t, nodes , graph ) );
		}

		for ( GraphNode g : nodes.values() ) {

			for ( IJobTemplate child : g.value.getDependencies() ) {
				final GraphNode childNode = nodes.get( child );
				if ( childNode == null ) {
					throw new RuntimeException("Internal error, unresolved template "+child);
				}
				graph.addEdge( g , childNode );
			}
		}

		/*
		 * Check graph for cycles , we require an acyclic dependency graph.
		 */
		if ( new CycleDetector<GraphNode , MyEdge>( graph ).detectCycles() ) {
			throw new IllegalArgumentException("Input graph contains cycles !!");
		}

		/*
		 * Sort in topological order.
		 */
		final TopologicalOrderIterator<GraphNode,MyEdge> iterator = 
			new TopologicalOrderIterator<GraphNode , MyEdge>( graph );

		final List<GraphNode> sorted = new ArrayList<GraphNode>();
		for ( ; iterator.hasNext() ; ) {
			final GraphNode g = iterator.next();
			sorted.add(g );
		}

		Collections.reverse( sorted );
		//		System.out.println(" topological order = "+sorted);

		/*
		 * Assign slots.
		 */

		// group slots by type (for faster look-up)
		final IdentityHashMap<ISlotType, List<IFactorySlot>> slots = 
			new IdentityHashMap<ISlotType, List<IFactorySlot>>();

		for ( IFactory f : manager.getFactories() ) {
			for ( IFactorySlot s : f.getSlots() ) {
				List<IFactorySlot> existing = slots.get( s.getType() );
				if ( existing == null ) {
					existing = new ArrayList<IFactorySlot>();
					slots.put( s.getType() , existing );
				}
				existing.add( s );
			}
		}

		final Duration duration = Duration.months( 1 );
		final Date endDate = duration.addTo( now );

		for ( GraphNode node : sorted ) {

			// find candidates
			final List<IFactorySlot> candidates =
				new ArrayList<IFactorySlot>();



			IFactorySlot targetSlot = null;
			float utilization = 0.0f;
			for ( ISlotType slotType : slots.keySet() ) 
			{
				if ( ! slotType.accepts( node.value ) ) 
				{
					continue;
				}

				for ( IFactorySlot s : slots.get( slotType ) ) 
				{
					candidates.add(s);

					// use slot with lowest utilization
					final float u = s.getUtilization( new DateRange( now , endDate ) ); 
					if ( targetSlot == null || u < utilization ) {
						targetSlot = s;
						utilization = u;
					}
				}
			}

			if ( targetSlot == null ) {
				throw new RuntimeException("Found no slot to process "+node.value);
			}

			final IJob newJob = createJob( manager , targetSlot , (SampleJobTemplate) node.value );
			newJob.setStartDate( findStartDate( targetSlot , newJob.getDuration() , now ) );
			targetSlot.add( newJob );
		}

		return null;
	}
	
	protected Date findStartDate(IFactorySlot slot, Duration jobDuration , Date desiredStartDate) {
		
		final List<IJob> jobs = slot.getJobsSortedAscendingByStartTime();
		if ( jobs.isEmpty() ) {
			return desiredStartDate;
		}
		
		if ( jobs.size() == 1 ) {
			final IJob job = jobs.get(0);
			
			if ( job.runsAt( desiredStartDate ) ) {
				return Duration.seconds(1).addTo( job.getEndDate() );
			}
		}
		
		for ( int i = 0 ; i < ( jobs.size() -1 ) ; i++) {
			
			final Date gapStart = jobs.get(i).getEndDate();
			if ( gapStart.before( desiredStartDate ) ) {
				continue;
			}
			final Date gapEnd = jobs.get(i+1).getStartDate();
			final Duration gapLength = new Duration( gapStart , gapEnd );
			if ( jobDuration.compareTo( gapLength ) <= 0 ) {
				return gapStart;
			}
		}

		return jobs.get( jobs.size() -1 ).getEndDate();
	}


	protected GraphNode addToGraph(IJobTemplate t, IdentityHashMap<IJobTemplate, 
			GraphNode> graph,
			DefaultDirectedGraph< GraphNode , MyEdge > realGraph) 
	{

		GraphNode newNode = graph.get( t );
		if ( newNode != null ) {
			return newNode;
		}

		newNode = new GraphNode( t );
		graph.put( t , newNode );
		realGraph.addVertex( newNode );

		for ( IJobTemplate child : t.getDependencies() ) {
			addToGraph( child, graph , realGraph );
		}
		return newNode;
	}

	private class GraphNode {

		public IJobTemplate value;

		private GraphNode(IJobTemplate value) {
			this.value = value;
		}

		@Override
		public String toString()
		{
			return value == null ? "<NULL>" : value.toString();
		}


	}



}
