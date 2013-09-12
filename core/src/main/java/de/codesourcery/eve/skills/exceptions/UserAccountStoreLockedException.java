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
package de.codesourcery.eve.skills.exceptions;

import de.codesourcery.eve.skills.accountdata.IUserAccountStore;

/**
 * Thrown when trying to access a locked credentials store. 
 *
 * @author tobias.gierke@code-sourcery.de
 * @see IUserAccountStore#isLocked()
 * @see IUserAccountStore
 */
public class UserAccountStoreLockedException extends RuntimeException {

	public UserAccountStoreLockedException() {
		super();
	}

	public UserAccountStoreLockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserAccountStoreLockedException(String message) {
		super(message);
	}

	public UserAccountStoreLockedException(Throwable cause) {
		super(cause);
	}

}
