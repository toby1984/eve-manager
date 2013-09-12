package de.codesourcery.planning.impl;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

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

/**
 * Abstract template class for implementing job scheduling
 * strategies. 
 * 
 * <pre>
 * You will need to implement the following methods: 
 * 
 * - {@link #createProductionPlan(IFactoryManager, IProductionPlanTemplate)}
 * - {@link #createJob(IFactoryManager, IFactorySlot, IJobTemplate)}
 * - {@link #chooseProductionSlot(IJobTemplate, Date, List)}
 * 
 * You MAY want to override the following method:
 * 
 * - {@link #findStartDate(IFactorySlot, Duration, Date)}
 * 
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class AbstractJobSchedulingStrategy implements IJobSchedulingStrategy
{

	protected static final class MyEdge {

		public MyEdge() {
		}
	}

	protected abstract IJob createJob(IFactoryManager factoryManager , IFactorySlot slot , IJobTemplate t);

	/**
	 * 
	 */
	@Override
	public final IProductionPlan calculateProductionPlan(IFactoryManager manager,IProductionPlanTemplate template)
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

		// discover & add all vertices by recursively walking
		// the dependency tree
		for ( IJobTemplate t : template.getJobTemplates() ) {
			graph.addVertex( addToGraph(t, nodes , graph ) );
		}

		// set graph edges
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
		final CycleDetector<GraphNode , MyEdge> detector = new CycleDetector<GraphNode , MyEdge>( graph );
		if ( detector.detectCycles() ) {
			throw new IllegalArgumentException("Detected cyclic dependency between jobs: "+detector.findCycles());
		}

		/*
		 * Sort in topological order.
		 * 
		 * (Jobs that do not depend on other jobs
		 * will come last)
		 */
		final TopologicalOrderIterator<GraphNode,MyEdge> iterator = 
			new TopologicalOrderIterator<GraphNode , MyEdge>( graph );

		final List<GraphNode> sorted = new ArrayList<GraphNode>();
		for ( ; iterator.hasNext() ; ) {
			final GraphNode g = iterator.next();
			sorted.add(g );
		}

		// reverse order so that jobs without 
		// any dependencies come first
		Collections.reverse( sorted );

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
		
		/*
		 * Create production plan.
		 */
		final IProductionPlan result = 
			createProductionPlan(manager, template);
		
		for ( GraphNode node : sorted ) 
		{
			final IJobTemplate jobTemplate = node.value;
			
			final IFactorySlot targetSlot = 
				findProductionSlotFor( jobTemplate , now ,slots );

			if ( targetSlot == null ) {
				throw new RuntimeException("Unable to allocate slot for job template "+jobTemplate);
			}

			final IJob newJob = createJob( manager , targetSlot , jobTemplate );
			newJob.setStartDate( findStartDate( targetSlot , newJob.getDuration() , now ) );
			
			// jobs have prospective status until their production plan is
			// either submitted or disposed.
			newJob.setStatus(JobStatus.PROSPECTIVE);
			targetSlot.add( newJob );
			result.addJob( newJob );
		}

		return result;
	}
	
	protected abstract IProductionPlan createProductionPlan(IFactoryManager manager, IProductionPlanTemplate template);

	protected final IFactorySlot findProductionSlotFor(IJobTemplate template,
			Date desiredStartDate,
			Map<ISlotType, List<IFactorySlot>>  availableSlots) 
	{

		// find candidates
		final List<IFactorySlot> candidates =
			new ArrayList<IFactorySlot>();

		for ( ISlotType slotType : availableSlots.keySet() ) 
		{
			if ( slotType.accepts( template ) ) 
			{
				candidates.addAll( availableSlots.get( slotType ) );
			}
		}

		final IFactorySlot targetSlot = 
			chooseProductionSlot( template , desiredStartDate ,  candidates );

		if ( targetSlot == null ) {
			throw new RuntimeException("Unable to find usable/free  slot to process "+template);
		}
		return targetSlot;
	}

	protected abstract IFactorySlot chooseProductionSlot(IJobTemplate template , Date desiredStartDate,List<IFactorySlot> candidates); 

	/**
	 * Determine suitable start date for a given production slot and job duration.
	 * 
	 * @param slot the slot where to find a suitable job start date in
	 * @param jobDuration the duration of the job to be scheduled
	 * @param desiredStartDate the desired start date. This date will be ignored if
	 * it clashes with jobs that are already scheduled 
	 * @return start date that can be used to schedule this job for the given slot
	 * @see IFactorySlot#add(IJob)
	 */
	protected Date findStartDate(IFactorySlot slot, Duration jobDuration , Date desiredStartDate) {

		final List<IJob> jobs = slot.getJobsSortedAscendingByStartTime();
		if ( jobs.isEmpty() ) {
			return desiredStartDate;
		}

		if ( jobs.size() == 1 ) 
		{
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


	protected final GraphNode addToGraph(IJobTemplate t, IdentityHashMap<IJobTemplate, 
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

	protected final class GraphNode {

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
