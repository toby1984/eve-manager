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
package de.codesourcery.eve.skills.calendar;

import java.text.ParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Type of a calendar entry's payload.
 * 
 * The payload type knows how to
 * serialize/deserialize the payload to XML.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface ICalendarEntryPayloadType
{
	
	/**
	 * Returns the unique ID of this payload type.
	 * @return
	 */
	public int getTypeId();
	
	/**
	 * Deserialize payload from XML.
	 * @param element
	 * @return
	 * @throws ParseException 
	 */
	public ICalendarEntryPayload parsePayload( Element element ) throws ParseException ;
	
	/**
	 * Serialize payload to XML.
	 * 
	 * @param parent
	 * @param entry
	 */
	public void storePayload( Document document , Element parent , ICalendarEntry entry );
	
}
