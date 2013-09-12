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
package de.codesourcery.eve.apiclient.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import de.codesourcery.eve.apiclient.IResponseParserInvoker;
import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.apiclient.parsers.IResponseParser;

/**
 * Special <code>IResponseParserInvoker</code> that makes
 * sure {@link IResponseParser#parse(Date, String)} only 
 * gets executed on the Swing Event Dispatch Thread.
 * @author tobias.gierke@code-sourcery.de
 * @see IResponseParserInvoker
 */
public class SwingEDTParserInvoker implements IResponseParserInvoker
{

	@Override
	public <T> InternalAPIResponse runParser(final IResponseParser<T> parser,
			final Date responseTimestamp, final String xml) throws UnparseableResponseException
	{
		
		if ( SwingUtilities.isEventDispatchThread() ) {
			return parser.parse(responseTimestamp, xml);
		}
		
		final AtomicReference<InternalAPIResponse> result =
			new AtomicReference<InternalAPIResponse>( null );
		try 
		{
			SwingUtilities.invokeAndWait( new Runnable() {

				@Override
				public void run()
				{
					result.set(
						parser.parse( responseTimestamp , xml)
					);
				}} );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (InvocationTargetException e) {
			final Throwable targetException = e.getTargetException();
			if ( targetException instanceof RuntimeException ) {
				throw (RuntimeException) targetException;
			}
			throw new RuntimeException("Failed to invoke parser "+parser,e);
		}
		
		return result.get();
	}

}
