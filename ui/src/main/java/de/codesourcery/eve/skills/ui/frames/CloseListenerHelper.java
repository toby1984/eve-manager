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
package de.codesourcery.eve.skills.ui.frames;

import java.util.ArrayList;
import java.util.List;

import javax.swing.RootPaneContainer;

import org.apache.log4j.Logger;

public class CloseListenerHelper implements ICloseListenerAware {

	public static final Logger log = Logger
			.getLogger(CloseListenerHelper.class);
	
	private final List<IWindowCloseListener> listeners =
		new ArrayList<IWindowCloseListener>();
	
	public CloseListenerHelper() {
	}
	
	public void addCloseListener(IWindowCloseListener l) {
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		
		if ( log.isDebugEnabled() ) {
			log.debug("addCloseListener(): Adding listener "+l);
		}
		
		synchronized (l) {
			for ( IWindowCloseListener existing : listeners ) {
				if ( existing == l ) {
					log.error("addCloseListener(): Won't add close listener "+l+" more than once.",new Exception() );
					return;
				}
			}
			listeners.add( l );
		}
	}

	public void removeCloseListener(IWindowCloseListener l) {
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized( listeners ) {
			listeners.remove( l );
		}
	}
	
	public void fireWindowClosed(RootPaneContainer window) {

		final List<IWindowCloseListener> copy;
		synchronized (listeners) 
		{
			if ( log.isDebugEnabled() ) 
			{
				log.debug("fireWindowClosed(): Window closed: "+window+
					", notifiying "+this.listeners.size()+" listeners ");
			}
			copy = new ArrayList<IWindowCloseListener>( this.listeners );
		}
		
		for ( IWindowCloseListener l : copy ) {
			l.windowClosed( window );
		}
	}
}
