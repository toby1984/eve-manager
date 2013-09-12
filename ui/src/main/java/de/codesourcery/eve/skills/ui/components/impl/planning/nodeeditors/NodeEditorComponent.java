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
package de.codesourcery.eve.skills.ui.components.impl.planning.nodeeditors;

import java.lang.ref.WeakReference;

import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.INodeValueChangeListener;
import de.codesourcery.eve.skills.ui.model.ITreeNode;

public abstract class NodeEditorComponent extends AbstractComponent 
{

	private WeakReference<INodeValueChangeListener> listener;
	
	public NodeEditorComponent(INodeValueChangeListener listener) {
		if ( listener == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		this.listener = new WeakReference<INodeValueChangeListener>( listener );
	}
	
	protected void notifyListener(ITreeNode node) {
		if ( getState() == ComponentState.ATTACHED ) { 
			final INodeValueChangeListener l = listener.get();
			if ( l != null ) {
				l.nodeValueChanged( node );
			}
		}
	}
	
	@Override
	protected final void disposeHook()
	{
		listener = null;
	}
}
