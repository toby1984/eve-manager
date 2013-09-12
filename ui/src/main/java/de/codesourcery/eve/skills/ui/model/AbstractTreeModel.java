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
package de.codesourcery.eve.skills.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.apache.log4j.Logger;

public abstract class AbstractTreeModel implements ITreeModel
{
	
	private static final Logger log = Logger.getLogger(AbstractTreeModel.class);
	
	private volatile boolean listenerNotificationEnabled = 
		true;
	
	private final List<TreeModelListener> listeners =
		new ArrayList<TreeModelListener>();
	
	protected static enum EventType {
		ADDED,
		REMOVED,
		CHANGED,
		STRUCTURE_CHANGED;
	}
	
	@Override
	public final void removeTreeModelListener(TreeModelListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (listeners) {
			listeners.remove( l );
		}
	}
	
	@Override
	public final void addTreeModelListener(TreeModelListener l) {

		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		
		synchronized (listeners) {
			listeners.add( l );
		}
	}
	
	protected final void fireEvent(EventType type , TreeModelEvent event) {
		
		if ( ! listenerNotificationEnabled ) {
			return;
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("fireEvent: type="+type+", event = "+event+" , this = "+this);
		}
		
		final List<TreeModelListener> copy;
		synchronized( listeners ) {
			copy = new ArrayList<TreeModelListener>( this.listeners );
		}
		
		for ( TreeModelListener l : copy ) {
			if ( log.isTraceEnabled() ) {
				log.trace("fireEvent(): listener = "+l);
			}
			switch( type ) {
			case ADDED:
				l.treeNodesInserted( event );
				break;
			case CHANGED:
				l.treeNodesChanged( event );
				break;
			case REMOVED:
				l.treeNodesRemoved( event );
				break;
			case STRUCTURE_CHANGED:
				l.treeStructureChanged( event );
				break;
				default:
					throw new RuntimeException("Internal error, unhandled event type "+type);
			}
		}
	}
	
	@Override
	public final void setListenerNotificationEnabled(boolean yesNo)
	{
		this.listenerNotificationEnabled = yesNo;
	}
}
