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

import org.apache.commons.lang.StringUtils;

/**
 * Menu item that wraps a {@link ICommand} instance
 * along with all data required for rendering.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class SmartMenuItem {

	private final String[] path;
	private final ICommand command;
	
	/**
	 * Create instance.
	 * 
	 * <pre>
	 * The menu path consists of
	 * at least two menu (item) names,separated
	 * by '/' characters.
	 * 
	 * Examples:
	 * 
	 * 'File/Edit' : Renders this menu item with the label 'Edit'
	 * in a Menu named 'File'
	 * 
	 * 'File/View/ViewOptions' : Renders this menu item
	 * with the label 'ViewOptions' in a submenu named
	 * 'View' within the 'File' top-level menu.
	 * 
	 * Top-level menus will be displayed sorted in alphabetical
	 * order within the menubar.
	 * </pre>
	 * 
	 * @param path The path that describes
	 * where in a menu bar this item should
	 * be rendered.  
	 * @param cmd The command that should get executed
	 * when this menu item is triggered
	 */
	public SmartMenuItem(String path,ICommand cmd) {
		
		if (StringUtils.isBlank(path)) {
			throw new IllegalArgumentException(
					"path cannot be blank / NULL");
		}
		
		if ( cmd == null ) {
			throw new IllegalArgumentException("cmd cannot be NULL");
		}
		
		this.path = path.split("/");
		this.command = cmd;
	}

	public ICommand getCommand() {
		return command;
	}

	public String[] getPath() {
		return path;
	}
	
}
