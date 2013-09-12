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

import javax.swing.RootPaneContainer;


/**
 * Convenience 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IWindowCloseListener {

	/**
	 * TODO: Using <code>RootPaneContainer</code> here is kind of hack
	 * TODO: that stems from the fact that close listeners should work 
	 * TODO: for frames,dialogs <b>and</b> internal frames.
	 *  
	 * TODO: Since internal frames do have
	 * TODO: nothing in common with regular frames or dialogs <b>except</b>
	 * TODO: the <code>RootPaneContainer</code> interface, I choose this
	 * TODO: interface as parameter type since it's the smallest common denominator.
	 * @param window
	 */
	public void windowClosed(RootPaneContainer window);
}
