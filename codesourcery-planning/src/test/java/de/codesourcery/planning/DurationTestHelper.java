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
package de.codesourcery.planning;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import de.codesourcery.planning.impl.SimpleJob;

public class DurationTestHelper extends TestCase
{

	private static final DateFormat DF1 =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static final DateFormat DF2 =
		new SimpleDateFormat("yyyy-MM-dd");	
	
	protected Date toDate(String s ) {
		try {
			return DF1.parse( s );
		}
		catch (ParseException e) {
			try {
				return DF2.parse( s );
			}
			catch (ParseException e1) {
				throw new RuntimeException(e);
			}
		}
	}
	
	protected SimpleJob createSimpleJob(String name,Duration duration,int runs) {
		final IJobTemplate template = niceMock( IJobTemplate.class );
		EasyMock.expect( template.getDependencies() ).andReturn( Collections.<IJobTemplate>emptyList() ).anyTimes();
		EasyMock.replay( template );
		return new SimpleJob( name , template , duration , runs);	
	}
	
	protected <T> T mock(Class<T> clasz) {
		return EasyMock.createMock( clasz );
	}
	
	protected <T> T niceMock(Class<T> clasz) {
		return EasyMock.createNiceMock( clasz );
	}
	
	protected <T> T strictMock(Class<T> clasz) {
		return EasyMock.createStrictMock( clasz );
	}
}
