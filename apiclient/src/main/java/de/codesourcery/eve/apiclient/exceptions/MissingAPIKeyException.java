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
package de.codesourcery.eve.apiclient.exceptions;

import java.util.Arrays;

import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyType;

/**
 * Thrown when an action required an API key of a specific
 * type but no suitable key could be found.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class MissingAPIKeyException extends AuthenticationFailureException {

	private final transient KeyType[] missingKeyTypes;
	
	public MissingAPIKeyException(String message , KeyType... missingKeyType) {
		super( message +" , required key type(s): "+Arrays.toString( missingKeyType) );
		this.missingKeyTypes = missingKeyType;
	}
	
	public MissingAPIKeyException(KeyType... missingKeyType) {
		super( "No suitable API key found, required key type(s): "+Arrays.toString( missingKeyType ) );		
		this.missingKeyTypes = missingKeyType;
	}

	/**
	 * Returns the missing API key.
	 * @return
	 */
	public KeyType[] getMissingKeyTypes() {
		return missingKeyTypes;
	}


}
