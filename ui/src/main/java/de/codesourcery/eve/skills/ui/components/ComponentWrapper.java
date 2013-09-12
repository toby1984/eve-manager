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

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.ui.components.IComponent.ComponentState;
import de.codesourcery.eve.skills.ui.components.IComponent.IComponentCallback;
import de.codesourcery.eve.skills.ui.frames.CloseListenerHelper;
import de.codesourcery.eve.skills.ui.frames.ICloseListenerAware;
import de.codesourcery.eve.skills.ui.frames.IWindowCloseListener;
import de.codesourcery.eve.skills.ui.frames.IWindowManagerAware;
import de.codesourcery.eve.skills.util.IStatusCallback;


/**
 * Wraps a <code>IComponent</code> with a <code>JDialog</code>
 * or <code>JFrame</code> , depending on the component's
 * {@link IComponent#isModal()} method.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class ComponentWrapper {

	public static final Logger log = Logger.getLogger(WrapperFrame.class);

	private static final class WrapperFrame extends JFrame 
		implements IComponentContainer , ICloseListenerAware , IWindowManagerAware 
	{

		private final IComponent wrapped;
		
		private final CloseListenerHelper helper =
			new CloseListenerHelper();

		private volatile boolean managed = false;
		
		private final WindowAdapter windowListener = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				fireWindowClosed();
			}
		};
		
		public WrapperFrame(final IComponent wrapped , final String title) 
		{
			super( title );
			this.wrapped = wrapped;
			addWindowListener( windowListener);
		}
		
		public IComponent getWrappedComponent() {
			return wrapped;
		}

		@Override
		public IComponent getComponent() {
			return wrapped;
		}
		
		public void addCloseListener(IWindowCloseListener l) {
			helper.addCloseListener( l );
		}

		public void removeCloseListener(IWindowCloseListener l) {
			helper.removeCloseListener(l);
		}
		
		protected void fireWindowClosed() {
			helper.fireWindowClosed( this );
		}
		
		@Override
		public void setVisible(boolean visible )
		{
			visibilityChanged( wrapped , visible);
			super.setVisible( visible );
		}
		
		public boolean isManaged()
		{
			return managed;
		}

		@Override
		public void windowManaged()
		{
			if ( log.isTraceEnabled() ) {
				log.trace("windowManaged(): this="+this.getClass().getName());
			}

			removeWindowListener( windowListener );
			this.managed = true;
		}

		@Override
		public void windowReleased()
		{
			if ( log.isTraceEnabled() ) {
				log.trace("windowReleased(): this="+this.getClass().getName());
			}
			managed = false;
			// FIRST change the 'managed' flag and then invoke the listeners.
			// See wrapComponenten() method , the listener will call
			// isManaged() on this instance
			fireWindowClosed();			
		}

	}

	private static final class WrapperDialog extends JDialog implements IComponentContainer
		, ICloseListenerAware,IWindowManagerAware 
	{

		private final IComponent wrapped;
		
		private final CloseListenerHelper helper =
			new CloseListenerHelper();
		
		private volatile boolean managed = false;
		
		private final WindowListener windowListener = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				fireWindowClosed();
			}
		};

		public WrapperDialog(final IComponent wrapped , Window parent, String title,
				ModalityType application_modal) {
			super( parent,title,application_modal );
			this.wrapped = wrapped;
			addWindowListener( windowListener );			
		}

		public IComponent getWrappedComponent() {
			return wrapped;
		}
		
		@Override
		public IComponent getComponent() {
			return wrapped;
		}

		public void addCloseListener(IWindowCloseListener l) {
			helper.addCloseListener( l );
		}

		public void removeCloseListener(IWindowCloseListener l) {
			helper.removeCloseListener(l);
		}
		
		protected void fireWindowClosed() {
			helper.fireWindowClosed( this );
		}
		
		@Override
		public void windowManaged()
		{
			if ( log.isTraceEnabled() ) {
				log.trace("windowManaged(): this="+this.getClass().getName());
			}
			// remove listener, cleanup will be performed by 
			// windowReleased();
			removeWindowListener( windowListener );
			managed = true;
		}
		
		public boolean isManaged()
		{
			return managed;
		}
		
		@Override
		public void setVisible(boolean visible )
		{
			visibilityChanged( wrapped , visible);
			super.setVisible( visible );
		}

		@Override
		public void windowReleased()
		{
			if ( log.isTraceEnabled() ) {
				log.trace("windowReleased(): this="+this.getClass().getName());
			}
			managed = false;
			// FIRST change the 'managed' flag and then invoke the listeners.
			// See wrapComponenten() method , the listener will call
			// isManaged() on this instance
			fireWindowClosed();
		}
	}

	protected static void visibilityChanged(IComponent wrapped , boolean visible)
	{
		if ( visible ) 
		{
			if ( wrapped.hasState( ComponentState.DETACHED ) ) { // component becomes visible
				wrapped.onAttach( null );
			}
		}
		else if ( ! visible ) // component becomes hidden (window is closed) 
		{
			if ( wrapped.hasState( ComponentState.ATTACHED ) ) {
				wrapped.onDetach();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends IComponent> T unwrapComponent( Window window ) {

		if ( window instanceof IComponentContainer ) {
			return (T) ((IComponentContainer) window).getComponent();
		} 
		throw new IllegalArgumentException("Window "+window+" does not wrap a component ?");
	}

	/**
	 * Returns a window that displays a 
	 * given UI component.
	 * 
	 * @param parent parent window
	 * @param comp
	 * @return Window or <code>null</code> if the component
	 * refused to be displayed.
	 * 
	 * @see IComponentCallback#dispose(IComponent)
	 */
	public static Window wrapComponent(Window parent,final IComponent comp) {
		return wrapComponent( comp.getTitle() , parent , comp );
	}

	/**
	 * Returns a window that shows a 
	 * given UI component.
	 * 
	 * @param title window title to use
	 * @param parent parent window
	 * @param comp
	 * @return Window or <code>null</code> if the component
	 * refused to be displayed.
	 * 
	 * @see IComponentCallback#dispose(IComponent)
	 */	
	public static Window wrapComponent(String title, Window parent,final IComponent comp) {

		final Window result;

		Container container;
		if ( log.isDebugEnabled() ) {
			log.debug("wrapComponent(): Wrapping "+comp.getClass().getName()+" , modal="+comp.isModal());
		}
		
		if ( comp.isModal() ) {
			result = new WrapperDialog( comp , parent , title , ModalityType.APPLICATION_MODAL );
			container = result;
		} else {
			result = new WrapperFrame( comp , title );
			container = ((JFrame) result).getContentPane();
		}

		final AtomicBoolean disposedByComponent =
			new AtomicBoolean(false);
		// ======== ATTACH ===============
		comp.onAttach( new IComponentCallback() {

			@Override
			public void dispose(IComponent caller) {
				disposedByComponent.set( true );
				result.dispose(); // will trigger WINDOW_CLOSED event , listener detaches component
			}

			@Override
			public IStatusCallback getStatusCallback()
			{
				return null;
			}

		});
		
		if ( disposedByComponent.get() ) {
			return null;
		}
		
		container.setLayout( new GridBagLayout() );

		final GridBagConstraints cnstrs =
			new GridBagConstraints(0,0,1,1,0.5f,0.5f,GridBagConstraints.BASELINE,
					GridBagConstraints.BOTH , new Insets(2,2,2,2) , 0, 0 );

		final JPanel panel =
			comp.getPanel();
		
		if ( disposedByComponent.get() ) {
				return null;
		}
		
		if ( panel == null ) {
			log.error("wrap(): Internal error - component "+comp+ 
					" returned a NULL panel but didn't invoke IComponentCallback#dispose() ?");
			throw new RuntimeException("Internal error - component "+comp
					+" returned a NULL panel but didn't invoke IComponentCallback#dispose() ?");
		}
		
		container.add( panel , cnstrs );	

		// register close listener on window
		((ICloseListenerAware) result).addCloseListener( new IWindowCloseListener() {

			@Override
			public void windowClosed(RootPaneContainer window) {
				
				if ( log.isDebugEnabled() ) {
					log.debug("windowClosed(): Detaching component "+comp);
				}
				
				try {
					
					/*
					 * his code is somewhat tricky
					 * since a WrapperFrame / WrapperDialog
					 * might've been passed to
					 * WindowManager#registerWindow()
					 * and therefore the window manager
					 * keeps a reference to the
					 * window even after it's been closed.
					 * 
					 * Managed windows will invoke
					 * this method (windowClosed()) 
					 * AGAIN once they're released
					 * by the window manager.
					 * 
					 * See windowReleased() implementations
					 * in WrapperFrame / WrapperDialog for details. 
					 */
					if ( ! comp.hasState(ComponentState.DETACHED) ) {
						comp.onDetach();
					}
					
					final boolean isManaged =
						( window instanceof IWindowManagerAware ) ? 
								((IWindowManagerAware) window).isManaged() : false;
								
					if ( log.isTraceEnabled() )
					{
						log.trace("windowClosed(): [ is_managed="+isManaged+
								" ] window = "+window.getClass().getName());
					}
					
					if ( isManaged ) {
						log.debug("windowClosed(): Won't dispose component in managed window.");
						return;
					}
				} 
				finally {
				
					if ( log.isDebugEnabled() ) {
						log.debug("windowClosed(): Disposing component "+comp);
					}				
				
					comp.dispose();
				}
			}
		} );
				
		result.pack();
		result.setLocationRelativeTo( null );

		// windows with editor components get
		// destroyed once editing finishes
		if ( comp instanceof IEditorComponent ) {

			final IEditorListener l = new IEditorListener() {

				@Override
				public void editingCancelled(IEditorComponent comp) {
					comp.removeEditorListener( this );
					result.dispose();
				}

				@Override
				public void editingFinished(IEditorComponent comp) {
					comp.removeEditorListener( this );
					result.dispose();
				}
			};

			((IEditorComponent) comp).addEditorListener( l ); 
		}
		return result;
	}

	/**
	 * Returns a window that shows  a 
	 * given UI component.
	 * 
	 * @param title window title to use
	 * @param comp
	 * @return Window or <code>null</code> if the component
	 * refused to be displayed.
	 * 
	 * @see IComponentCallback#dispose(IComponent)
	 */	
	public static Window wrapComponent(String title , IComponent comp) {
		return wrapComponent(title ,null,comp);
	}

	/**
	 * Returns a window that shows a 
	 * given UI component.
	 * 
	 * @param comp
	 * @return Window or <code>null</code> if the component
	 * refused to be displayed.
	 * 
	 * @see IComponentCallback#dispose(IComponent)
	 */
	public static Window wrapComponent(IComponent comp) {
		return wrapComponent(comp.getTitle() , null,comp);
	}
}
