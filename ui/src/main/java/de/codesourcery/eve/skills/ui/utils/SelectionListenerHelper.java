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
package de.codesourcery.eve.skills.ui.utils;

import java.util.ArrayList;
import java.util.List;

import de.codesourcery.eve.skills.ui.components.ISelectionListener;

public class SelectionListenerHelper<T> {

	private final List<ISelectionListener<T>> listeners =
		new ArrayList<ISelectionListener<T>>();
	
	public void addSelectionListener(ISelectionListener<T> l) {
		if (l == null) {
			throw new IllegalArgumentException(
					"listener cannot be NULL");
		}
		synchronized( listeners ) {
			listeners.add( l );
		}
	}
	
	public List<ISelectionListener<T>> getListeners() {
		synchronized( listeners ) {
			return new ArrayList<ISelectionListener<T>>( this.listeners );
		}
	}
	
	public void removeSelectionListener(ISelectionListener<T> l) {
		if (l == null) {
			throw new IllegalArgumentException(
					"listener cannot be NULL");
		}
		synchronized (listeners) {
			listeners.remove( l );
		}
	}
	
	public void selectionChanged(T selected) {
		
		final List<ISelectionListener<T>> copy;
		synchronized (listeners) {
			copy =
				new ArrayList<ISelectionListener<T>>( listeners );	
		}
		for ( ISelectionListener<T> l : copy ) {
			l.selectionChanged( selected );
		}
	}
}
