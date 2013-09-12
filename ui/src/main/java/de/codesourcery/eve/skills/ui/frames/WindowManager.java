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

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
 * Keeps track of all open application windows.
 * 
 * <pre>
 * Windows may be associated with (String) keys and 
 * retrieved from the window manager. This is useful
 * whenever only one window of a specific type
 * should be present.
 *
 * Windows may implement the {@link IWindowManagerAware}
 * interface to be notified when they become
 * managed/un-managed.
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class WindowManager {

	private static final Logger log = Logger.getLogger(WindowManager.class);

	// guarded-by: windows
	private final Map<String,Window> windows =
		new ConcurrentHashMap<String,Window>();

	private static final WindowManager INSTANCE = new WindowManager();

	/**
	 * Factory callback for creating
	 * new Windows.
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
	public interface IWindowProvider {

		public Window createWindow(); 
	}

	protected WindowManager() {
	}

	/**
	 * Get window manager instance (singleton).
	 * 
	 * @return
	 */
	public static WindowManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Shutdown this window manager.
	 * 
	 * All registered windows will be closed
	 * by this method.
	 *  
	 * @param force
	 */
	public void shutdown(boolean force) {

		synchronized( windows ) {
			Collection<Window> frames = new ArrayList<Window>( windows.values() );
			for ( Window f : frames ) {
				f.dispose();
			}
		}
	}

	/**
	 * Tries to look up a window by key, creates and registers
	 * a new window if not already registered.
	 * 
	 * @param key
	 * @param provider the window provider to be used
	 * to create a new window if no window by that key is already registered.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Window>  T getWindow(String key,IWindowProvider provider) {
		if ( provider == null ) {
			throw new IllegalArgumentException("NULL window provider ?");
		}

		Window result;
		synchronized( windows ) {
			result =
				windows.get( key );
			if ( result == null ) {
				try {
					if ( log.isTraceEnabled() ) {
						log.trace("getWindow(): "+key+" => window not found , creating new.");
					}					
					result = provider.createWindow();
					registerWindow( key , result );
				} catch(Exception e) {
					log.error("getWindow(): Window provider "+provider+" failed",e);
					throw new RuntimeException(e);
				}
			} else if ( log.isTraceEnabled() ) {
				log.trace("getWindow(): "+key+" => found");
			}
		}
		return (T) result;
	}

	/**
	 * Looks up a window by key.
	 * 
	 * @param key
	 * @return window with that key or <code>null</code>
	 */
	public Window getWindow(String key) {
		Window result;
		synchronized( windows ) {
			result= windows.get( key );
		}

		if ( log.isTraceEnabled() ) {
			log.trace("getWindow(): "+key+" => "+( result != null ? "found" : "not found" ) );
		}
		return result;
	}

	/**
	 * Registers an new window.
	 * 
	 * @param key
	 * @param frame
	 */
	public void registerWindow(final String key ,final  Window frame) {

		if ( frame == null ) {
			throw new IllegalArgumentException("frame cannot be NULL");
		}

		if (StringUtils.isBlank(key)) {
			throw new IllegalArgumentException(
			"key cannot be blank / NULL");
		}

		synchronized( windows ) {

			final Window oldFrame = windows.get( key );
			if ( oldFrame != null ) {
				log.warn("registerWindow(): REPLACING existing window with key '"+key+"'");
				unregisterWindow( key , oldFrame );
			}

			log.debug("registerWindow(): Registering window with key = "+key);

			windows.put( key , frame );

			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent e)
				{
					unregisterWindow( key , frame );
				}
			}
			);

			if ( frame instanceof IWindowManagerAware) {
				((IWindowManagerAware) frame).windowManaged();
			}			
		}
	}

	protected void unregisterWindow(String key , Window window) {

		if ( log.isDebugEnabled() ) {
			log.debug("unregisterWindow(): key = "+key+" , window = "+window);
		}

		synchronized( windows ) {

			for ( Iterator<Entry<String,Window> > frame = 
				this.windows.entrySet().iterator() ; frame.hasNext() ;  )
			{
				final Map.Entry<String,Window> entry = frame.next();
				if ( entry.getValue() == window ) {

					log.debug("unregisterWindow(): removing window with key = "+entry.getKey() );

					try {
						
						if ( window instanceof IWindowManagerAware) {
							( (IWindowManagerAware) window ).windowReleased();
						}

					} 
					finally {
						frame.remove();
					}
				}
			}
		}
	}

}
