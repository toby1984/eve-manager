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

/**
 * Represents a user-editable selection.
 * 
 * {@link SelectionListener}s may be registered
 * to receive notification about selection changes.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface ISelectionProvider<T> {

	public void addSelectionListener(ISelectionListener<T> l);
	
	public void removeSelectionListener(ISelectionListener<T> l);
	
	public T getSelectedItem();
}
