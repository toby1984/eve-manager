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

import de.codesourcery.eve.apiclient.datamodel.APIError;

/**
 * Thrown when a response from the Eve Online(tm) API 
 * indicated an error.
 *
 * @author tobias.gierke@code-sourcery.de
 * @see APIError
 */
public class APIErrorException extends APIException {

	private final transient APIError error;

	public APIErrorException(APIError error) {
		super("API call failed with error: "+error);
		this.error = error;
	}
	
	public APIError getError() {
		return error;
	}
	
}
