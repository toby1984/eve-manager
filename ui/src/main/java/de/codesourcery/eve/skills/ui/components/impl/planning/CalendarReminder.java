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
package de.codesourcery.eve.skills.ui.components.impl.planning;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.eve.skills.calendar.ICalendarManager;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class CalendarReminder implements DisposableBean
{

	private static final Logger log = Logger.getLogger(CalendarReminder.class);

	private int checkDelayInSeconds = 60;

	private final ICalendarManager calendarManager;

	private final ISystemClock systemClock;

	// guarded-by: WatchdogThread.this
	private WatchdogThread watchdog;

	public CalendarReminder(ICalendarManager calendarManager,ISystemClock systemClock) {
		this.calendarManager = calendarManager;
		this.systemClock = systemClock;
	}

	public synchronized void startWatchdogThread() 
	{
		if ( watchdog == null || ! watchdog.isAlive() ) {
			watchdog = new WatchdogThread();
			watchdog.start();
		}
	}

	public synchronized void stopWatchdogThread() 
	{
		if ( watchdog != null && watchdog.isAlive() ) {
			watchdog.terminate();
		}
	}

	private final class WatchdogThread extends Thread {

		private volatile boolean terminate = false;

		@Override
		public void run()
		{
			log.info("WatchdogThread(): Calendar watchdog started.");

			try {

				while ( ! terminate ) {

					try {
						if ( log.isDebugEnabled() ) {
							log.debug("run(): Checking calendar...");
						}
						checkCalendar();
					} 
					catch(Exception e) {
						log.error("run(): Caught ",e);
					}

					try {
						if ( ! terminate ) {
							java.lang.Thread.sleep( checkDelayInSeconds * 1000 );
						}
					} 
					catch(Exception e) {
						// ok
					}
				}
			} finally {
				log.info("WatchdogThread(): Watchdog thread terminated.");
			}
		}

		public void terminate()
		{
			this.terminate = true;
			this.interrupt();
		}

		protected void checkCalendar() throws InterruptedException, InvocationTargetException 
		{
			final List<ICalendarEntry> unacknowledgedEntries = 
				calendarManager.getCalendar().getUnacknowledgedEntries( systemClock );

			if ( log.isDebugEnabled() ) {
				log.debug("run(): "+unacknowledgedEntries.size()+" outstanding entries");
			}

			if ( unacknowledgedEntries.isEmpty() ) {
				return;
			}	

			SwingUtilities.invokeAndWait( new Runnable() {

				@Override
				public void run()
				{
					try {
						displayDialog(unacknowledgedEntries);
					} 
					catch(Exception e) {
						log.error("checkCalendar(): Caught ",e);
					}
				}

				private void displayDialog(
						final List<ICalendarEntry> unacknowledgedEntries)
				{
					final CalendarReminderComponent comp =
						new CalendarReminderComponent( unacknowledgedEntries );
					comp.setModal( true );
					ComponentWrapper.wrapComponent( "Reminder" , comp ).setVisible( true );
				}
			});
		}
	}

	@Override
	public void destroy() throws Exception
	{
		stopWatchdogThread();
	}
}
