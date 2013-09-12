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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.FutureTask;

import javax.annotation.Resource;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.ui.utils.ApplicationThreadManager;
import de.codesourcery.eve.skills.ui.utils.ConstraintsBuilder;
import de.codesourcery.eve.skills.ui.utils.Misc;
import de.codesourcery.eve.skills.ui.utils.ParallelUITasksRunner;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.ui.utils.ApplicationThreadManager.ITaskWithoutResult;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.util.IStatusCallback;
import de.codesourcery.eve.skills.util.SpringBeanInjector;
import de.codesourcery.eve.skills.util.IStatusCallback.MessageType;
import de.codesourcery.eve.skills.utils.ISKAmount;

/**
 * Base class for all {@link IComponent} implementations.
 * 
 * <pre>
 * Implements and enforces proper lifecycle management 
 * for this component and any registered child components.
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class AbstractComponent implements IComponent {

	public static final Logger log = Logger.getLogger(AbstractComponent.class);
	
	protected static final String NO_TITLE = "<no component title>";
	
	private volatile JPanel panel;
	private IComponentCallback callback;
	
	private String title="";
	
	private boolean isModal = false;
	
	private volatile ComponentState componentState =
		ComponentState.NEW;
	
	/**
	 * Child components that share
	 * the same life-cycle as this component.
	 */
	private final List<IComponent> children = 
		new ArrayList<IComponent>();
	
	public interface ILabelProvider<X> {
		public String getLabel(X obj);
	}
	
	protected static <T> List<T> sortUsingLabelProvider(Collection<T> data,final ILabelProvider<T> provider) {
	
		final Comparator<T> comparator = new Comparator<T>() {

			@Override
			public int compare(T o1, T o2)
			{
				return provider.getLabel( o1 ).compareTo( provider.getLabel( o2 ) );
			}
		};
		
		if ( data instanceof List ) {
			Collections.sort( (List<T>) data , comparator );
			return (List<T>) data;
		} 

		final List<T> target = new ArrayList<T>( data );
		Collections.sort( target , comparator );
		return target;
	}
	
	protected void assertDetached() {
		if ( ! hasState(ComponentState.NEW) && 
			 ! hasState(ComponentState.DETACHED ) ) 
		{
			throw new IllegalStateException("Component "+this.getClass().getName()+" is neither NEW nor DETACHED");
		}
	}
	
	/**
	 * Registers a child component.
	 * 
	 * <pre>
	 * Child components share the same life-cycle 
	 * as this component and will automatically be
	 * attached / detached / disposed
	 * as their parent component changes state.
	 *  
	 * Children are always attached / detached / disposed after their 
	 * respective parent.
	 * </pre>
	 * @param child
	 */
	protected void registerChildren(IComponent... children) {
		if ( children == null ) {
			throw new IllegalArgumentException("child cannot be NULL");
		}
		for ( IComponent child : children ) {
			if ( ! this.children.contains( child ) ) {
				this.children.add( child );
			} else {
				log.warn("registerChild(): Trying to register child "+child+" more than once on component "+getClass().getName() );
			}
		}
	}
	
	protected ComponentState getState() {
		return this.componentState;
	}
	
	public boolean hasState(ComponentState state) {
		return this.componentState == state;
	}
	
	protected void setState(ComponentState state) {
		this.componentState.assertValidStateTransition( state );
		if ( log.isTraceEnabled()) {
			log.trace("setState(): [ "+this.getClass().getName()+" ] State transition "+this.componentState+" -> "+state);
		}
		this.componentState = state;
	}
	
	@Resource(name="thread-manager")
	private ApplicationThreadManager threadManager;
	
	public AbstractComponent() {
		SpringBeanInjector.getInstance().injectDependencies( this );
	}
	
	public AbstractComponent(String title) {
		this.title = title;
		SpringBeanInjector.getInstance().injectDependencies( this );
	}
	
	@Override
	public boolean isModal() {
		return isModal;
	}
	
	public void setModal(boolean yesNo) {
		this.isModal = yesNo;
	}
	
	@Override
	public final JPanel getPanel() {
		
		if ( this.componentState != ComponentState.ATTACHED ) {
			throw new IllegalStateException("internal error , need to attach " +
					"component "+this.getClass().getName()+" first before calling getPanel()");
		}
		
		if ( panel == null ) {
			panel = createPanel();
			if ( panel == null ) {
				throw new RuntimeException("Internal error - component "+getClass().getName()+" returned a NULL panel ?");
			}
		}
		return panel;
	}
	
	protected ApplicationThreadManager getThreadManager() {
		return threadManager;
	}
	
	protected boolean submitParallelTasks(final UITask parent,UITask... children) 
	{
		return ParallelUITasksRunner.submitParallelTasks( threadManager , parent , children );
	}
	
	protected boolean submitTask(String id , ITaskWithoutResult task) {
		
		if ( log.isDebugEnabled() ) {
			log.debug("submitTask(): Submitting task "+id);
		}
		
		final boolean result =
			threadManager.submitTask( id , task ) != null;
		if ( ! result ) {
			log.warn("submitTask(): Failed to submit task "+id);
		}
		return result;
	}
	
	protected FutureTask<?> submitFutureTask(UITask task,boolean cancelExisting) 
	{
		if ( log.isDebugEnabled() ) {
			log.debug("submitTask(): Submitting task "+task.getId());
		}
		
		final FutureTask<?> result =  threadManager.submitTask( task );
		
		if ( result == null ) {
			log.warn("submitTask(): Failed to submit task "+task.getId());
		}
		return result;
	}
	
	protected boolean submitTask(UITask task,boolean cancelExisting) {
		return submitFutureTask(task, cancelExisting) != null;
	}
	
	protected boolean submitTask(UITask task) {
		return submitTask( task , false );
	}
	
	protected boolean isInitialized() {
		return panel != null;
	}
	
	protected void displayError(String message) {
		displayError(message,null);
	}
	
	protected void displayError(String message,Throwable t) {
		Component parent = null;
		if ( getComponentCallback() instanceof Component ) {
			parent = (Component) getComponentCallback();
		}
		Misc.displayError( parent , message , t );
	}
	
	protected void displayInfo(String message) {
		Component parent = null;
		if ( getComponentCallback() instanceof Component ) {
			parent = (Component) getComponentCallback();
		}
		JOptionPane.showMessageDialog( parent ,
					message,
					"Info",
					JOptionPane.INFORMATION_MESSAGE );
	}
	
	@Override
	public String getTitle() {
		return StringUtils.isNotBlank( title ) ? title : NO_TITLE;
	}
	
	protected abstract JPanel createPanel();
	
	/**
	 * Empty subclassing hook.
	 * 
	 * @see #dispose()
	 */
	protected void disposeHook() {
		
	}
	
	@Override
	public final void dispose() {
		
		log.debug("dispose(): this = "+this.getClass().getName() );
		
		if ( ! mayDispose() ) {
			throw new IllegalStateException("Component "+this+" may not be disposed just yet");
		}
		
		this.panel = null;
		setState( ComponentState.DISPOSED );
		disposeHook();
		
		for ( IComponent child : children ) {
			try {
				child.dispose();
			} 
			catch(Exception e) {
				log.error("dispose(): Failed to dispose child "+child,e);
				displayError("Failed to dispose child component: "+e.getMessage() ,e);
				if ( e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}		
	}
	
	/**
	 * Empty subclassing hook.
	 * 
	 * @param callback
	 * @see #onAttach(de.codesourcery.eve.skills.ui.components.IComponent.IComponentCallback)
	 */
	protected void onAttachHook(IComponentCallback callback) {
		
	}
	
	protected static String toString(ISKAmount amount) {
		return AmountHelper.formatISKAmount( amount )+" ISK";
	}
	
	@Override
	public final void onAttach(IComponentCallback callback) {
		log.debug("onAttach(): this = "+this.getClass().getName() );
		this.callback = callback;
		setState( ComponentState.ATTACHED );
		onAttachHook(callback);
		
		for ( IComponent child : children ) {
			try {
				child.onAttach( callback );
			} 
			catch(Exception e) {
				log.error("onAttach(): Failed to attach child "+child,e);
				displayError("Failed to attach child component: "+e.getMessage() ,e);
				if ( e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}
	}
	
	protected void displayStatus(String message) {
		
		if ( log.isTraceEnabled() ) {
			log.trace("displayStatus(): message="+message);
		}
			
		if ( hasState( ComponentState.ATTACHED ) ) {
			final IStatusCallback display = this.callback.getStatusCallback();
			if ( display != null ) {
				display.displayMessage( MessageType.INFO , message );
			} else if ( log.isTraceEnabled() ) {
				log.trace("displayStatus(): Callback did not provide a status display.");
			}
		} else {
			log.warn("displayStatus(): Called although component is detached.");
		}
	}
	
	protected IComponentCallback getComponentCallback() {
		return callback;
	}
	
	/**
	 * Subclassing hook.
	 * 
	 * @see #onDetach()
	 */
	protected void onDetachHook() {
	}
	
	@Override
	public final void onDetach() {
		log.debug("onDetach(): this = "+this.getClass().getName() );
		setState( ComponentState.DETACHED );
		onDetachHook();
		
		for ( IComponent child : children ) {
			try {
				child.onDetach();
			} 
			catch(Exception e) {
				log.error("onDetach(): Failed to attach child "+child,e);
				displayError("Failed to attach child component: "+e.getMessage() ,e);
				if ( e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}
	}
	
	@Override
	public boolean mayDispose() {
		return true;
	}
	
	protected static ConstraintsBuilder constraints() {
		return new ConstraintsBuilder();
	}
	
	protected static ConstraintsBuilder constraints(int x,int y) {
		return new ConstraintsBuilder().x(x).y(y);
	}
	
	protected static void setMonospacedFont(Component comp) {

		final int size;
		if ( comp.getFont() != null ) {
			size = comp.getFont().getSize();
		} else {
			size = 12;
		}
		Font font = new Font("Monospaced", Font.PLAIN, size );

		comp.setFont( font );
	}

	protected void runOnEventThread(Runnable r) {
		Misc.runOnEventThread( r );
	}
	
	protected void linkComponentEnabledStates(final JToggleButton parent,final Component... children) {
		
		final boolean selected = 
			parent.isSelected();
		
		for ( Component child : children ) {
			child.setEnabled( selected );
		}
		
		parent.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				
				final boolean selected = 
					e.getStateChange() == ItemEvent.SELECTED;
				
				for ( Component child : children ) {
					child.setEnabled( selected );
				}
			}} );
		
	}
	
	protected Border lineBorder( Color c) {
		return BorderFactory.createLineBorder( c );
	}

}
