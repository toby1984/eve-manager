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
package de.codesourcery.eve.skills.ui.components.impl;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.utils.Misc;
import de.codesourcery.eve.skills.util.IStatusCallback;

/**
 * Component that displays status messages for a short amount of
 * time.
 * 
 * Status messages may be queued
 * @author <a href="mailto:tobias.gierke@freenet-ag.de">Tobias Gierke</a>
 * @version $Revision$
 */
public class StatusBarComponent extends AbstractComponent implements IStatusCallback {
	
	public static final Logger log = Logger.getLogger(StatusBarComponent.class);

	private final JLabel label = new JLabel();
	
	private int delay = 1000;
	
	private final BlockingQueue<String> messages =
		new ArrayBlockingQueue<String>(50);

	// guarded-by: this
	private StatusUpdater displayThread;
	
	private volatile boolean shutdownCalled=false;
	
	public StatusBarComponent() {
	}
	
	@Override
	protected JPanel createPanel() {
		
		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );
		label.setPreferredSize( new Dimension(400, 30 ) );
		panel.add( label , constraints().resizeHorizontally().useRemainingWidth().end() );
		return panel;
	}
	
	@Override
	protected void disposeHook() {
		shutdownCalled=true;
		log.debug("disposeHook(): Called.");
		stopDisplayThread();
		messages.clear();
	}
	
	protected synchronized void startDisplayThread() {
		if ( displayThread == null || ! displayThread.isAlive() ) {
			log.debug("startDisplayThread(): Starting thread.");
			displayThread = new StatusUpdater();
			displayThread.start();
		}
	}
	
	protected synchronized void stopDisplayThread() {
		if ( displayThread != null && displayThread.isAlive() ) {
			displayThread.terminate();
			displayThread.interrupt();
			try {
				log.debug("stopDisplayThread(): Waiting for display thread to terminate...");
				displayThread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.debug("stopDisplayThread(): Display thread terminated.");
			displayThread = null;
		}
	}	
	
	public void addMessage(final String msg) {
		
		if ( shutdownCalled ) {
			throw new IllegalStateException("Shutdown running");
		}
		
		try {
			if ( messages.offer( msg , 1 , TimeUnit.SECONDS ) ) {
				startDisplayThread();	
			} else {
				log.error("addMessage(): Dropping status message >"+msg+"< , queue full.");
			}
		}
		catch (InterruptedException e) {
			log.error("addMessage(): Dropping status message >"+msg+"< , interrupted");
		}
		
	}
	
	private final class StatusUpdater extends Thread {

		private volatile boolean terminate = false;
		
		@Override
		public void run() {
			
			int pollsWithoutMessage = 0;
			while ( ! terminate && ! shutdownCalled && pollsWithoutMessage < 3 ) {
				try {
					if ( log.isTraceEnabled() ) {
						log.trace("StatusUpdater.run(): Waiting "+delay+" seconds for new messages to arrive.");
					}
					
					final String message =
						messages.poll(delay , TimeUnit.MILLISECONDS );
					
					if ( terminate ) {
						break;
					}
					
					if ( message == null ) {
						pollsWithoutMessage++;
					} else {
						pollsWithoutMessage=0;
					}
					setLabelText( message );
				} 
				catch(InterruptedException e) {
					// ok
				}
			}
			
			setLabelText( null );
			log.debug("StatusUpdater.run(): Thread terminated.");
		}
		
		private void setLabelText(final String msg) {
			
			log.debug("StatusUpdater.run(): displaying message = "+msg);
			
			Misc.runOnEventThread( new Runnable() {

				@Override
				public void run() {
					label.setText( msg );
					label.validate();
					label.getParent().validate();
				}} );
		}
		
		public void terminate() {
			this.terminate = true;
		}
		
	}

	@Override
	public void displayMessage(MessageType type, String message)
	{
		switch( type ) {
			case INFO:
				addMessage( message );
				break;
			case ERROR:
				addMessage("ERROR: "+message);
				break;
			default:
				addMessage( message );
		}
	}
}
