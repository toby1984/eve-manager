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
package de.codesourcery.eve.skills.ui.tabbedpanes;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.ui.components.IComponent;
import de.codesourcery.eve.skills.ui.components.IComponent.IComponentCallback;
import de.codesourcery.eve.skills.util.IStatusCallback;

public class TabbedComponentPane extends JTabbedPane {
	
	public static final Logger log = Logger
			.getLogger(TabbedComponentPane.class);

	// index = tab no.
	private final Map<Integer,TabEntry> entries =
		new HashMap<Integer,TabEntry>();
	
	private final IStatusCallback statusCallback;
	
	private final IComponentCallback callback = new IComponentCallback() {

		@Override
		public void dispose(IComponent caller) {
		}

		@Override
		public IStatusCallback getStatusCallback()
		{
			return statusCallback;
		}
	};
	
	private final class TabEntry 
	{
		private JPanel panel;
		private IComponent component;
		private final JLabel dummyLabel = new JLabel();

		public TabEntry(IComponent comp) {
			this.component = comp;
		}
		
		public JLabel getDummyLabel() {
			return dummyLabel;
		}

		public boolean isInitialized() {
			return panel != null;
		}

		public JPanel getPanel() {
			if ( panel != null ) {
				return panel;
			}
			component.onAttach( callback );
			panel = component.getPanel();
			return panel;
		}

		public void destroy() {
			if ( panel != null ) {
				component.onDetach();
				panel = null;
			}
			if ( isDisposeComponentOnDetach() ) {
				component.dispose();
			}
			component = null;
		}

		public void hide() {
			if ( panel != null ) {
				log.trace("hide(): Hiding "+component);
				panel = null;
				component.onDetach();
			}
		}
	}

	@Override
	public void setSelectedIndex(int index) {
		final int oldTab = getSelectedIndex();
		super.setSelectedIndex(index);
		selectedTabChanged(oldTab, index );
	}
	
	protected void selectedTabChanged(int oldTab, int newTab) {
		
		if ( oldTab == -1 ) {
			return;
		}
		
		final TabEntry entry = entries.get( oldTab );
		if ( entry == null ) {
			log.warn("selectedTabChanged(): Found no entry for tab index "+oldTab+" ?");
			return;
		}
		
		// detach hidden entry
		entry.hide();
	}

	protected boolean isDisposeComponentOnDetach() {
		return true;
	}
	
	public void dispose() {
		for ( Iterator<Map.Entry<Integer,TabEntry>> it = entries.entrySet().iterator() ; it.hasNext() ; ) 
		{
			final Map.Entry<Integer,TabEntry> entry = it.next();
			entry.getValue().destroy();
			it.remove();
		}
	}

	public TabbedComponentPane(IStatusCallback callback) 
	{
		this.statusCallback = callback;
		
		addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				activate();
			}
		});
	}
	
	public void add(String title, IComponent comp) {
		
		final TabEntry entry = 
			new TabEntry( comp );
		
		entries.put( getComponentCount() , entry );
		
		super.add(title, entry.getDummyLabel() );
		
		if ( getTabCount() == 1 ) {
			activate();
		}
	}

	private void activate() {
		final TabEntry entry = entries.get( getSelectedIndex() );

		if ( entry == null || entry.isInitialized() ) {
			return;
		}

		log.trace("activate(): Initializing component "+entry.component);
		final JPanel compPanel = entry.getPanel();
		compPanel.setPreferredSize( new Dimension(800,600) );
		setComponentAt( getSelectedIndex(), compPanel );
		invalidate();
		revalidate();
	}

	private TabEntry getTab(int index) {
		return entries.get( index );
	}

	public void removeTabAt(int removeIndex) {
		TabEntry existing = getTab( removeIndex );
		super.removeTabAt( removeIndex );
		if ( existing != null ) {
			existing.destroy();
		}
	}
}