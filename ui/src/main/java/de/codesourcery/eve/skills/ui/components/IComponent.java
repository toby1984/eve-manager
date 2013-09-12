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

import java.awt.Panel;

import javax.swing.JPanel;

import de.codesourcery.eve.skills.util.IStatusCallback;

/**
 * A UI component.
 *
 * <pre>
 * UI components render themselves to
 * a {@link Panel} that will be displayed
 * by the client code. For each <code>IComponent</code>
 * that {@link #getPanel()} is at most one time
 * (a component that is only created but never 
 * actually displayed will see no call to 
 * it's {@link #getPanel()} method).
 * 
 * UI Components are notified before they become
 * visible ({@link #onAttach(IComponentCallback)},
 * after they are removed from their 
 * containing component ({@link #onDetach()})
 * or are no longer needed ({@link #dispose()}).
 * Components may choose to veto their disposal with
 * the {@link #mayDispose()} method but client code
 * is free to dispose the component anyway or not
 * call <code>mayDispose()</code> at all.
 * 
 * Components are always associated with a
 * {@link ComponentState}. 
 * 
 * Valid state transitions are:
 * 
 * {@literal}
 * 
 *  NEW -> ATTACHED <-> DETACHED -> DISPOSED
 * }
 * 
 * Invalid state transitions are
 * rejected with a <code>IllegalStateException</code>.
 * 
 * Invoking {@link #getPanel()} is only allowed
 * while the component is in attached state ; likewise
 * {@link #dispose()} may only be invoked on a detached
 * component.
 * 
 * Components may have associated child 
 * components that then share the same life-cycle
 * as their parent/owning component.
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IComponent {
	
	public static enum ComponentState {
		
		NEW {
			@Override
			protected boolean mayTransitionTo(ComponentState newState) {
				return ( newState == ATTACHED || newState== DISPOSED);
			}
		},
		ATTACHED {
			@Override
			protected boolean mayTransitionTo(ComponentState newState) {
				return ( newState==DETACHED );
			}
		},
		DETACHED {
			@Override
			protected boolean mayTransitionTo(ComponentState newState) {
				return ( newState== ATTACHED || newState==DISPOSED);
			}
		},
		DISPOSED {
			@Override
			protected boolean mayTransitionTo(ComponentState newState) {
				return false;
			}
		};
		
		protected abstract boolean mayTransitionTo( ComponentState newState);
		
		public void assertValidStateTransition(ComponentState newState) {
			if ( newState == null ) {
				throw new IllegalArgumentException("NULL target state ?");
			}
			
			if ( ! mayTransitionTo( newState ) ) {
				throw new IllegalStateException("Internal error - illegal component state transition "+
						" "+this+" -> "+newState+" for component "+this.getClass().getName());
			}
			
		}
	}

	/**
	 * Callback that may get invoked when
	 * the component decides to dispose 
	 * itself.
	 * 
	 * A component may invoke this 
	 * callback as long it is attached
	 * to the current owner ( so 
	 * even {@link IComponent#getPanel()}
	 * may invoke <code>dispose(IComponent)</code> ,
	 * be careful !!!)
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 * @see IComponent#dispose()
	 * @see IComponent#mayDispose()
	 */
	public interface IComponentCallback {
		
		public void dispose(IComponent caller);
		
		public IStatusCallback getStatusCallback();
	}
	
	/**
	 * Returns this component's UI {@link JPanel}.
	 * 
	 * This method may only be invoked
	 * on a component in attached state.
	 * 
	 * Implementors must NOT invoke
	 * {@link IComponentCallback#dispose(IComponent)}
	 * from inside this method.
	 * 
	 * @return A panel holding the component's UI
	 * @throws IllegalStateException If component
	 * is not in attached state. 
	 */
	public JPanel getPanel();
	
	/**
	 * Check whether this component may be disposed.
	 * 
	 * Client code is free to not invoke this method
	 * at all / ignore this method's result
	 * when disposing a component.
	 * @return
	 */
	public boolean mayDispose();
	
	/**
	 * Called when this component gets
	 * attached to a container.
	 * 
	 * This method must ALWAYS be called 
	 * before {@link #getPanel()} is invoked.
	 * 
	 * Components may hold on to the
	 * caller reference as long as they're
	 * attached to it.
	 * 
	 * @param callback
	 * @throws IllegalStateException If component
	 * is already in attached or disposed state. 	
	 * 
	 * @see #onDetach()
	 * @see #dispose()
	 */
	public void onAttach(IComponentCallback callback);
	
	/**
	 * Called when this component is removed 
	 * from its container and no longer visible.
	 * 
	 * This method MUST be called
	 * before disposing an attached component.
	 * 
	 * @see #dispose()
	 * @see #onAttach(IComponentCallback)
	 * @throws IllegalStateException If component
	 * is not in attached state
	 */
	public void onDetach();
	
	/**
	 * Called when this component should be destroyed.
	 * 
	 * If this component is currently attached,
	 * the caller must call {@link #onDetach()} first
	 * before invoking this method.
	 * 
	 * @see #onAttach(IComponentCallback)
	 * @see #onDetach()
	 */
	public void dispose();
	
	/**
	 * Returns whether this component
	 * has a given state.
	 * 
	 * Note that in order for a component 
	 * to be in detached state, it must've been in
	 * attached state before. New components
	 * (components where {@link #onAttach(IComponentCallback)}
	 * has not been invoked yet) are
	 * <b>not</b> considered to be 'detached' but
	 * are 'new' instead..
	 * @return
	 */
	public boolean hasState(ComponentState state);
	
	/**
	 * Returns whether this is a modal component.
	 * 
	 * At most one instance of a modal component
	 * can be visible at all times.
	 * @return
	 */
	public boolean isModal();
	
	/**
	 * (Optional method) Mark this component
	 * as (not) being a modal component.
	 * 
	 * Implementors are free to leave this
	 * method empty.
	 * @param yesNo
	 */
	public void setModal(boolean yesNo);
	
	/**
	 * Returns the component's title
	 * for display as a window/dialog/border title. 
	 */
	public String getTitle();
}
