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

import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.ui.components.IComponent;
import de.codesourcery.eve.skills.ui.components.IComponentContainer;
import de.codesourcery.eve.skills.ui.components.IComponent.IComponentCallback;
import de.codesourcery.eve.skills.util.IStatusCallback;

public class CustomInternalFrame extends JInternalFrame implements ICloseListenerAware ,
IComponentContainer {

	public static final Logger log = Logger
	.getLogger(CustomInternalFrame.class);

	private final List<IWindowCloseListener> listeners =
		new ArrayList<IWindowCloseListener>();

	private final IComponent component;

	protected CustomInternalFrame(String title,IComponent component) {
		super( title );
		if ( component == null ) {
			throw new IllegalArgumentException("component cannot be NULL");
		}

		this.component = component;

		component.onAttach( new IComponentCallback() {

			@Override
			public void dispose(IComponent caller) {
				CustomInternalFrame.this.dispose();
			}

			@Override
			public IStatusCallback getStatusCallback()
			{
				return null;
			}
		} );

		final JPanel panel =
			component.getPanel();
		add( panel );
		pack();
	}

	@Override
	public void dispose() {
		super.dispose();
		this.component.onDetach();
		fireWindowClosed();
	}

	public void addCloseListener(IWindowCloseListener l) {
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (l) {
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


	protected void fireWindowClosed() {

		if ( log.isDebugEnabled() ) {
			log.debug("fireWindowClosed(): Window closed: "+this);
		}

		final List<IWindowCloseListener> copy;
		synchronized (listeners) {
			copy = new ArrayList<IWindowCloseListener>( this.listeners );
		}
		for ( IWindowCloseListener l : copy ) {
			l.windowClosed( this );
		}
	}

	@Override
	public IComponent getComponent() {
		return this.component;
	}
}
