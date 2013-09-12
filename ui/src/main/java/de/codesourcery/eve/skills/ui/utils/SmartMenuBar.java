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

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * Menu bar that supports adding {@link SmartMenuItem}s to it.
 * 
 * 'Smart' menu items hold a path that describes
 * where this menu item should be rendered.
 * 
 * The menu path consists of at least two components,
 * a menu name and a menu item name,separated
 * by a '/' character. The path may contain
 * more than two components to create sub-menu items.
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
 * Top-level menus are rendered in alphabetical order,
 * starting at the left side of the menu window.  
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class SmartMenuBar extends JMenuBar {

	protected static JMenu findSubMenu(JMenu m,String title) {
		for ( int i = 0 ; i < m.getMenuComponentCount() ; i++ ) {
			final Component c = m.getMenuComponent(i);
			if ( ! ( c instanceof JMenu ) ) {
				continue;
			}
			if ( ((JMenu) c).getText().equals( title ) ) {
				return (JMenu) c;
			}
		}
		return null;
	}

	private static final class MyAction extends AbstractAction {

		private final SmartMenuItem item;

		public MyAction(SmartMenuItem item) {
			super( item.getPath()[ item.getPath().length -1 ] );
			this.item = item;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			item.getCommand().execute( null );
		}
	}

	public void add(final SmartMenuItem item) {

		final String[] path = item.getPath();

		JMenu last = null;
		for ( int i = 0 ; i < getMenuCount() ; i++ ) {
			final JMenu menu = getMenu(i);
			if ( menu.getText().equals( path[0] ) ) {
				last = menu;
				break;
			}
		}

		if ( last == null ) {
			last = new JMenu( path[0] );
			add( last );
		}

		int i = 1;
		for (  ; i < path.length-1 ; i++ ) {
			JMenu next = findSubMenu( last  , path[i] );
			if ( next == null ) {
				next = new JMenu( path[i] );
				last.add( next );
			}
			last = next;
		}

		final JMenuItem newItem =
			new JMenuItem( path[ path.length-1 ] );

		newItem.setAction( new MyAction(item) );
		last.add( newItem );
	}
}
