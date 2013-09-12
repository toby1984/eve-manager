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
package de.codesourcery.eve.skills.ui.components;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSelectionProvider<T> implements ISelectionProvider<T> ,
ISelectionListener<T> {

	// guarded-by: listeners
	private final List<ISelectionListener<T>> listeners =
		new ArrayList<ISelectionListener<T>>();
	
	public AbstractSelectionProvider() {
	}
	
	public void addSelectionListener(ISelectionListener<T> l) {
		synchronized (listeners) {
			listeners.add( l );
		}
	}
	
	public void removeSelectionListener(ISelectionListener<T> l) {
		synchronized (listeners) {
			listeners.remove( l );
		}
	}
	
	@Override
	public void selectionChanged(T selected) {
		final List<ISelectionListener<T>> copy;
		synchronized (listeners) {
			copy =
				new ArrayList<ISelectionListener<T>>( listeners);
		}
		
		for ( ISelectionListener<T> l : copy ) {
			l.selectionChanged( selected );
		}
	}
}
