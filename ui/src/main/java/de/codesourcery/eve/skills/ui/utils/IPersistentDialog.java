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


/**
 * A warning/info dialog that my be suppressed 
 * by the user.
 * 
 * <pre>
 * Depending on the dialog's persistence
 * type , a dialog may be suppressed for
 * 
 * - the current session (application runtime)
 * - 'forever' (until the user resets the
 * flag)
 * 
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 */
public interface IPersistentDialog {
	
	public enum PersistenceType {
		/**
		 * Remember settings only
		 * for current session.
		 */
		SESSION,
		/**
		 * Remember settings permanently.
		 */
		PERMANENT;
	}

	/**
	 * Returns an application-wide ID that uniquely
	 * identifies this dialog.
	 * 
	 * @return
	 */
	public String getId();
	
	/**
	 * Returns the desired persistence 
	 * setting for this dialog.
	 * 
	 * @return
	 */
	public PersistenceType getPersistenceType();
	
	/**
	 * Returns whether display of this dialog 
	 * has been disabled by the user.
	 * 
	 * @return
	 */
	public boolean isDisabledByUser();
}
