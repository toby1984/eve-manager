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
package de.codesourcery.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

public class SimulationClock {
	
	private boolean started;
	private boolean stopped;
	
	private Date currentTime;
	private final Stack<Action> runnable = new Stack<Action>();
	private final SortedMap<Long,List<Action> > scheduled = new TreeMap<Long,List<Action>>();
	
	public final void schedule(Action action , Date date) {
		if ( date.compareTo( currentTime ) == 0) {
			runnable.add( action );
		} else if ( date.compareTo( currentTime ) > 0 ) {
			List<Action> existing = scheduled.get( date.getTime() );
			if ( existing == null ) {
				existing = new ArrayList<Action>();
				scheduled.put( date.getTime() , existing );
			}
			existing.add( action );
		} else {
			throw new IllegalArgumentException("Job cannot be scheduled in the past");
		}
	}
	
	public Collection<Action> getRunnables() {
		return runnable;
	}
	
	public SortedMap<Long,List<Action> > getScheduled() {
		return scheduled;
	}
	
	public final Action getNextAction() {
		if ( runnable.isEmpty() ) {
			return null;
		}
		return runnable.pop();
	}
	
	public long getResolutionInSeconds() {
		return 1;
	}
	
	public final boolean advanceClock()
	{
		if ( ! runnable.isEmpty() ) {
			throw new IllegalStateException("Cannot advance clock while there are still runnable actions left");
		}
		
		if ( scheduled.isEmpty() ) {
			return false;
		}
		
		// advance to next scheduled action
		final Iterator<Long> scheduledDates = scheduled.keySet().iterator();
		for ( ; scheduledDates.hasNext() ; ) {
			final Long time = scheduledDates.next();
			final List<Action> eligibleToRun = scheduled.get( time );
			scheduledDates.remove();
			if ( ! eligibleToRun.isEmpty() ) {
				runnable.addAll( eligibleToRun );
				currentTime = new Date( time );
				return true;
			}
		}
		return false;
	}
	
	public SimulationClock(Date time) {
		if ( time == null ) {
			throw new IllegalArgumentException("date cannot be NULL");
		}
		this.currentTime = time;
	}
	
	public void start(ISimulationListener listener) {
		
		if ( started ) {
			throw new IllegalStateException("Already started");
		}
		
		this.started = true;
		Action a;
		while ( ! stopped ) 
		{
			while ( ! stopped && ( a = getNextAction() ) != null ) {
				a.run( this );
			}
			
			if ( ! advanceClock() ) {
				stopped = true;
			} else {
				listener.clockAdvanced( this );
			}
		}
	}
	
	public void stop() {
		stopped = true;
	}
	
	public boolean wasStarted() {
		return started;
	}
	
	public boolean isStopped() {
		return stopped;
	}
	
	public final Date getTime() {
		return currentTime;
	}
	
}