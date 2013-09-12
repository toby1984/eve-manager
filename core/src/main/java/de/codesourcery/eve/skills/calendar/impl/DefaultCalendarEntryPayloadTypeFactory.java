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
package de.codesourcery.eve.skills.calendar.impl;

import java.util.HashMap;
import java.util.Map;

import de.codesourcery.eve.skills.calendar.ICalendarEntryPayloadType;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayloadTypeFactory;


public class DefaultCalendarEntryPayloadTypeFactory implements ICalendarEntryPayloadTypeFactory
{

	private static final Map<Integer,ICalendarEntryPayloadType> payloadTypes=
		new HashMap<Integer,ICalendarEntryPayloadType>();
	
	static {
		registerType( PlaintextPayloadType.INSTANCE );
	}

	private static void registerType(ICalendarEntryPayloadType type)
	{
		payloadTypes.put( type.getTypeId() , type);
	}
	
	public PlaintextPayload createPlainTextPayload( String title , String notes )
	{
		return new PlaintextPayload( title , notes );
	}
	
	@Override
	public ICalendarEntryPayloadType getPayloadType(int typeId)
	{
		final ICalendarEntryPayloadType result = payloadTypes.get( typeId );
		if ( result == null ) {
			throw new IllegalArgumentException("Unknown payload type "+typeId);
		}
		return result;
	}

}
