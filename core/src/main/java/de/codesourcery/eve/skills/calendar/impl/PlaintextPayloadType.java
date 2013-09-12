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

import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.codesourcery.eve.skills.calendar.ICalendarEntry;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayload;
import de.codesourcery.eve.skills.calendar.ICalendarEntryPayloadType;
import de.codesourcery.utils.xml.XmlHelper;

public class PlaintextPayloadType implements ICalendarEntryPayloadType
{

	public static final ICalendarEntryPayloadType INSTANCE = new PlaintextPayloadType();
	
	public static final int toInteger(String s) {
		return Integer.parseInt( s );
	}
	
	public static final boolean toBoolean(String s) {
		return StringUtils.isBlank( s ) ? false : Boolean.valueOf( s );
	}
	
	private PlaintextPayloadType() {
	}
	
	@Override
	public int getTypeId()
	{
		return 1;
	}
	
	@Override
	public ICalendarEntryPayload parsePayload(Element element) throws ParseException
	{
		
		final PlaintextPayload result = new PlaintextPayload();
		
		final Element child=
			XmlHelper.getElement( element , "plaintext" , true );
		
		final String notes =
			child.getTextContent();
		
		final String summary =
			child.getAttribute("summary");
		
		result.setSummary( summary );
		result.setNotes( notes );
		
		return result;
	}

	@Override
	public void storePayload(Document document , Element parent, ICalendarEntry entry)
	{
		final PlaintextPayload payload = 
			(PlaintextPayload) entry.getPayload();
		
		final Element node = document.createElement("plaintext");
		parent.appendChild( node );
		
		node.setAttribute("summary" , payload.getSummary() );
		node.appendChild( document.createTextNode( payload.getNotes() ) );
	}
}
