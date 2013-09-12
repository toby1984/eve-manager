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

import de.codesourcery.eve.skills.datamodel.CharacterID;

/**
 * Thrown when credentials for a given character ID could not be retrieved. 
 * from the underlying credentials storage.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class UnknownCredentialsException extends AuthenticationFailureException {

	private final transient CharacterID character;
	
	public UnknownCredentialsException(CharacterID character) {
		super("Found no credentials for character ID "+character);
		this.character = character;
	}

	public CharacterID getCharacterId() {
		return character;
	}

}
