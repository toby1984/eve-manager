package de.codesourcery.eve.apiclient.cache;

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

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import junit.framework.TestCase;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;
import de.codesourcery.eve.skills.utils.MockSystemClock;

public class AbstractCacheTest extends TestCase {

	protected static final DateFormat DATE_FORMAT =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	protected static final URI baseUri = toURI( "http://localhost" );

	protected final ISystemClock systemClock = new MockSystemClock();
	
	protected static final URI toURI(String s) {
		try {
			return new URI( s );
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	protected final Date createDate(String s) throws ParseException {
		return DATE_FORMAT.parse( s );
	}
	
	protected final EveDate createEveDate(String s) throws ParseException {
		return EveDate.fromServerTime( DATE_FORMAT.parse( s ) , systemClock );
	}

	protected final void assertSameDate(String expected,Date actual) throws ParseException {
		assertNotNull( actual );
		final Date expectedDate =
			createDate( expected );

		assertEquals( expectedDate ,actual );
	}
	
	protected final void assertSameDate(String expected,EveDate actual) throws ParseException {
		assertNotNull( actual );
		final EveDate expectedDate =
			createEveDate( expected );

		assertEquals( expectedDate ,actual );
	}

	protected final APIQuery createQuery(String relativePath,Map<String,Object> params) {
		return new APIQuery( baseUri , relativePath , params );
	}


}
