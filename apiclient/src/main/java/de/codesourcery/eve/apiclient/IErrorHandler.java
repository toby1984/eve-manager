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

import de.codesourcery.eve.apiclient.datamodel.APIQuery;

/**
 * Error handler invoked by the API client when
 * something goes wrong.
 * 
 * <pre>
 * Error handlers are invoked on
 * 
 * - IO error
 * - response XML parse errors
 * </pre>
 *
 * @author tobias.gierke@code-sourcery.de
 */
public interface IErrorHandler {
	
	/**
	 * Callback that may be used by an
	 * error handler to retry the failed operation.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */
	public interface IJoinPoint {
		
		/**
		 * Retries the failed operation.
		 *  
		 * @throws Exception
		 */
		public void retry() throws Exception;
		
	}
	
	/**
	 * Handle error.
	 * 
	 * @param jp callback to invoke to retry the failed operation 
	 * @param query the operation that failed
	 * @param ex the exception that occured 
	 * @throws Exception
	 */
	public void handleError( IAPIClient apiClient, IJoinPoint jp , APIQuery query, Exception ex) throws Exception;
}
