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

import javax.swing.SwingUtilities;

import de.codesourcery.eve.skills.ui.utils.ApplicationThreadManager.ITaskWithoutResult;
import de.codesourcery.eve.skills.util.SpringBeanInjector;

public abstract class UITask implements ITaskWithoutResult {

	private final Component comp;

	public UITask() {
		comp = null;
		SpringBeanInjector.getInstance().injectDependencies( this );
	}
	
	public abstract String getId();
	
	/**
	 * 
	 * @param comp UI component to disable while this 
	 * task is pending 
	 */
	public UITask(Component comp) {
		this.comp = comp;
		SpringBeanInjector.getInstance().injectDependencies( this );
	}
	
	public void failureHook(Throwable t) throws Exception {
		Misc.displayError( null  , "An unexpected error occured" , t);
	}
	
	@Override
	public final void failure(Throwable t) throws Exception {
		try {
			setEnabled( true );
		} finally {
			failureHook( t );
		}
	}

	public void successHook() throws Exception {
	}
	
	@Override
	public final void success() throws Exception {
		try {
			setEnabled( true );
		} finally {
			successHook();
		}
	}
	
	public void cancellationHook() {
	}

	@Override
	public final void cancelled() {
		try {
			setEnabled( true );
		} finally {
			cancellationHook();
		}
	}
	
	public void setEnabledHook(boolean yesNo) {
		
	}
	
	public final void setEnabled(boolean yesNo) {
		if ( comp != null ) {
			comp.setEnabled( yesNo );
			setEnabledHook( yesNo );
		}
	}
	
	protected final void runOnEDT(Runnable r) {
		if ( SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( r );
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public final boolean isUITask() {
		return true;
	}
	
	@Override
	public void beforeExecution() {
		
	}
}
