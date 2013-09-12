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
package de.codesourcery.eve.apiclient;

import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyRole;
import de.codesourcery.eve.apiclient.exceptions.MissingAPIKeyException;

/**
 * Returns API credentials for use by the client.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public interface ICredentialsProvider {
	
	/**
	 * Returns an API key by role.
	 * 
	 * @param type
	 * @return
	 * @throws IllegalArgumentException on <code>null</code> / invalid key types.
	 * @throws MissingAPIKeyException
	 */
	public APIKey getKeyForRole(KeyRole role) throws MissingAPIKeyException;
	
	/**
	 * Returns the API user ID to use.
	 * @return
	 */
	public long getUserId();
}