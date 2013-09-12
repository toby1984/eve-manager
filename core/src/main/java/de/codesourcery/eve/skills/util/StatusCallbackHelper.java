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
package de.codesourcery.eve.skills.util;

import java.util.ArrayList;
import java.util.List;

import de.codesourcery.eve.skills.util.IStatusCallback.MessageType;

public class StatusCallbackHelper {

	// guarded-by: callbacks
	private final List<IStatusCallback> callbacks =
		new ArrayList<IStatusCallback>();
	
	public void addStatusCallback(IStatusCallback callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback cannot be NULL");
		}
		
		synchronized (callbacks) {
			callbacks.add( callback );
		}
	}

	public void removeStatusCallback(IStatusCallback callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback cannot be NULL");
		}
		synchronized (callbacks) {
			callbacks.remove( callback );
		}
	}
	
	public void notifyStatusCallbacks(MessageType type ,String message) {
		
		List<IStatusCallback> copy;
			
		synchronized(callbacks) {
			copy = new ArrayList<IStatusCallback>( callbacks );
		}
		
		for ( IStatusCallback cb : copy ) {
			cb.displayMessage( type , message ); 
		}
	}
	
}
