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

import java.util.Date;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.apiclient.parsers.IResponseParser;
import de.codesourcery.eve.apiclient.utils.SwingEDTParserInvoker;

/**
 * Executor used to actually invoke the {@link IResponseParser#parse(Date, String)} method when
 * processing a request. 
 * 
 * <pre>
 * Setting an executor explicitly is rarely necessary and 
 * was originally implemented to confine execution of the parsers
 * to a single thread to fix issues with Hibernate in a multi-threaded
 * swing application.
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 * @see IAPIClient#setResponseParserInvoker(IResponseParserExecutor)
 * @see SwingEDTParserInvoker
 */
public interface IResponseParserInvoker
{
	/**
	 * Invoke parser.
	 * 
	 * @param parser the parser to invoke
	 * @param responseTimestamp Time when the response XML was received (LOCAL time)
	 * @param xml the XML to parse
	 * @return the result returned from the parser
	 * @throws UnparseableResponseException
	 */
	public <T> InternalAPIResponse runParser(IResponseParser<T> parser,Date responseTimestamp, String xml) throws UnparseableResponseException;
}
