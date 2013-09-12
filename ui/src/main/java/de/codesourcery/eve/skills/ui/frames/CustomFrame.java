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

import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.annotation.Resource;
import javax.swing.JFrame;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.IComponent;
import de.codesourcery.eve.skills.ui.utils.ApplicationThreadManager;
import de.codesourcery.eve.skills.ui.utils.ConstraintsBuilder;
import de.codesourcery.eve.skills.ui.utils.Misc;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.util.SpringBeanInjector;

/**
 * Base class of all application windows.
 * 
 * <pre>
 * This classes constructor automatically invokes
 * {@link SpringBeanInjector#injectDependencies(Object)}
 * on the newly created instance so all subclasses
 * are free to use the {@link Resource} annotation
 * to get their dependencies injected from the
 * current Spring application context.
 * 
 * This class contains various convenience methods
 * for making Swing UI programming less painful.
 * 
 * Notable methods:
 *
 * - {@link #initialize()} : Invoked ONCE by this base class
 * before the window is made visible
 * - {@link #isInitialized()} : Checks whether this method's
 * <code>initialize()</code> method has been executed successfully.
 * - {@link #constraints(int, int)} , {@link #constraints()} : Makes
 * creating {@link GridBagConstraints} instances less painful
 * - {@link #displayError(String)} : Displays a modal
 * error dialog
 * - {@link #displayInfo(String)} : Displays a modal
 * info dialog 
 * 
 * Often one really only wants to display a single {@link IComponent}
 * within a window, there is a convenience class {@link ComponentWrapper}
 * that provides static methods to do just that.
 * 
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 * @see WindowManager
 * @see IComponent
 */
public abstract class CustomFrame extends JFrame implements ICloseListenerAware {

	public static final Logger log = Logger.getLogger(CustomFrame.class);
	
	private boolean initialized = false;
	
	private final CloseListenerHelper helper =
		new CloseListenerHelper();
	
	@Resource(name="thread-manager")
	private ApplicationThreadManager threadManager;

	public void addCloseListener(IWindowCloseListener l) {
		helper.addCloseListener( l );
	}

	public void removeCloseListener(IWindowCloseListener l) {
		helper.removeCloseListener(l);
	}
	
	protected void fireWindowClosed() {
		helper.fireWindowClosed( this );
	}

	protected ApplicationThreadManager getThreadManager() {
		return threadManager;
	}
	
	
	protected void submitTask(UITask task) {
		final boolean submitted =
			threadManager.submitTask( task ) != null;
		
		if ( ! submitted ) {
			displayError("Failed to start task '"+task.getId()+"'");
		}
	}
	
	
	public CustomFrame() throws HeadlessException {
		super();
		SpringBeanInjector.getInstance().injectDependencies( this );
		registerWindow();
	}

	public CustomFrame(GraphicsConfiguration arg0) {
		super(arg0);
		SpringBeanInjector.getInstance().injectDependencies( this );
		registerWindow();
	}

	public CustomFrame(String arg0, GraphicsConfiguration arg1) {
		super(arg0, arg1);
		SpringBeanInjector.getInstance().injectDependencies( this );
		registerWindow();
	}
	
	private void registerWindow() {
		
		// register frame so it will be automatically closed by WindowManager#shutdown(boolean)
		WindowManager.getInstance().registerWindow( getClass().getName() , this );
		
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				fireWindowClosed();
			}
		} );
	}

	public CustomFrame(String arg0) throws HeadlessException {
		super(arg0);
		SpringBeanInjector.getInstance().injectDependencies( this );
		registerWindow();
	}

	protected static ConstraintsBuilder constraints() {
		return new ConstraintsBuilder();
	}
	
	protected static ConstraintsBuilder constraints(int x,int y) {
		return new ConstraintsBuilder().x(x).y(y);
	}
	
	protected void displayError(String message) {
		Misc.displayError( this , message );
	}
	
	protected abstract void initialize();
	
	protected boolean isInitialized() {
		return initialized;
	}
	
	@Override
	public void setVisible(boolean b) {
		if ( ! initialized ) {
			initialize();
			initialized = true;
			pack();
			setLocationRelativeTo(null);
		}
		
		super.setVisible(b);
	}
	
	protected void displayInfo(String message) {
		Misc.displayInfo( message );
	}

	protected void runOnEventThread(Runnable r) {
		Misc.runOnEventThread( r );
	}
}
