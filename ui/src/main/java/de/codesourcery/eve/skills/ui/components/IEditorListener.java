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
 * Callback that receives notifications from successful/cancelled
 * editing components.
 *  
 * @author tobias.gierke@code-sourcery.de
 * @see IEditorComponent
 */
public interface IEditorListener {

	/**
	 * Called after the user successfully edited
	 * the component.
	 * 
	 * @param comp
	 */
	public void editingFinished(IEditorComponent comp);
	
	/**
	 * Called when the user cancelled editing
	 * the component.
	 * 
	 * @param comp
	 */
	public void editingCancelled(IEditorComponent comp);
}
