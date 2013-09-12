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

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.lang.StringUtils;

public class PopupMenuBuilder extends MouseAdapter {
	
	private final JPopupMenu popupMenu = new JPopupMenu();
	
	private final List<Object> entries =
		new ArrayList<Object>();
	
	public void mousePressed(MouseEvent e) {
		maybeShowPopup(e);
	}

	public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	}

	protected static final Object SEPARATOR =new Object();
	
	/**
	 * A context menu-action that - instead of just
	 * not being rendered at all - shows a disabled
	 * label that acts as a hint on why this action
	 * is currently disabled. 
	 *  
	 * @author tobias.gierke@code-sourcery.de
	 */
	public interface IActionWithDisabledText extends Action {
		
		/**
		 * Returns the label to be rendered
		 * while this action is disabled.
		 * 
		 * @return label for when this action is disabled, <code>null</code>/blank
		 * to completely hide the corresponding menu item 
		 */
		public String getDisabledText();
	}
	
	protected static final class MyMenuItem {
		
		private final String label;
		private final Action action;
		
		public MyMenuItem(String label , Action action) {
			if ( label == null ) {
				throw new IllegalArgumentException("label cannot be NULL");
			}
			if ( action == null ) {
				throw new IllegalArgumentException("action cannot be NULL");
			}
			this.label = label;
			this.action = action;
		}
		
		public boolean isEnabled() {
			return action.isEnabled();
		}
		
		public String getLabel() {
			return label;
		}
		
		public Action getAction() {
			return action;
		}
	}
	
	public PopupMenuBuilder attach(JComponent comp) {
		comp.addMouseListener( this );
		return this;
	}
	
	public PopupMenuBuilder detach(JComponent comp) {
		comp.removeMouseListener( this );
		return this;
	}
	
	public PopupMenuBuilder addItem(String label,final Runnable r) {
		this.entries.add( new MyMenuItem( label , new AbstractAction() {

			@Override
			public boolean isEnabled() {
				return true;
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				r.run();
			}} ) );
		return this;
	}
	
	public PopupMenuBuilder addItem(String label,Action action) {
		this.entries.add( new MyMenuItem( label , action ) );
		return this;
	}
	
	public PopupMenuBuilder addSeparatorIfNotEmpty() {
		if ( ! entries.isEmpty() ) {
			this.entries.add( SEPARATOR );
		}
		return this;
	}
	
	public PopupMenuBuilder addSeparator() {
		this.entries.add( SEPARATOR );
		return this;
	}
	
	/**
	 * Checks whether a mouse-click event
	 * should trigger a context-sensitive popup menu
	 * and renders the menu if so.
	 * 
	 * @param e
	 */
	protected void maybeShowPopup(MouseEvent e) {

		if ( ! e.isPopupTrigger() ) {
			return;
		}
		
		popupMenu.removeAll();
		
		if ( populateMenu() > 0 ) {
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	protected int populateMenu() {
		
		int itemCount = 0;
		for ( Object obj : this.entries ) {
			if ( obj == SEPARATOR ) {
				popupMenu.addSeparator();
			} else {
				final MyMenuItem item = (MyMenuItem) obj;
				
				final Action action = item.getAction();
				
				final boolean isEnabled =
					action.isEnabled() ;
				
				if ( isEnabled || action instanceof IActionWithDisabledText ) 
				{
					final JMenuItem menuItem = new JMenuItem( action );
					
					final String text;
					if ( isEnabled ) {
						text = item.getLabel();
					} else {
						text = ((IActionWithDisabledText) action).getDisabledText();
						// do not render action that returns a blank/empty label
						if ( StringUtils.isBlank( text ) ) {
							continue; 
						}
						menuItem.setEnabled( false );
					}
					menuItem.setText( text);
					popupMenu.add( menuItem );
					itemCount++;
				}
			}
		}
		return itemCount;
	}

}
