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

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Helper class that wraps a <code>XMLStreamReader</code>
 * with some convenience methods.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class XMLStreamHelper {

	private final XMLStreamReader reader;

	public final Tag tag = new Tag();
	
	public static final class Tag {
		protected String name;
		protected String contents;
		
		protected Tag() {
		}
		
		public String getName() { return name; }
		public String getContents() { return contents; }
	}
	
	public XMLStreamHelper( XMLStreamReader reader) {
		this.reader = reader;
	}
	
	public boolean readStartElementWithValue(String name) throws XMLStreamException {
		
		int type;
		while ( reader.hasNext() ) {
			type = reader.next();
			if ( type == XMLStreamReader.START_ELEMENT) {
				if ( reader.hasNext() && name.equals( reader.getName().getLocalPart() ) ) 
				{
					type = reader.next();
					if ( type == XMLStreamReader.CDATA || type == XMLStreamReader.CHARACTERS ) {
						tag.contents = reader.getText();
					} else {
						throw new XMLStreamException("Tag "+name+" has no value ?", reader.getLocation() );
					}
					return true;
				}
			}
		}
			
		return false;
	}
	
	public boolean readEmptyStartElement(String name) throws XMLStreamException {
		
		while ( reader.hasNext() ) {
			final int type = reader.next();
			if ( type == XMLStreamReader.START_ELEMENT) {
				if ( reader.hasNext() && name.equals( reader.getName().getLocalPart() ) ) 
				{
					return true;
				}
			}
		}
			
		return false;
	}
	
	public void readEndElement(String name) throws XMLStreamException {
		
		final Location loc = reader.getLocation();
		if ( reader.hasNext() ) {
			final int type = reader.next();
			if ( type == XMLStreamReader.END_ELEMENT) {
				if ( name.equals( reader.getName().getLocalPart() ) ) 
				{
					return;
				}
			}
		}
			
		throw new XMLStreamException("Found no end element </"+name+"> at location",loc );
	}	
	
	public Tag readElement(String name) throws XMLStreamException {
		
		final Location loc = reader.getLocation();
		while ( reader.hasNext() ) {
			final int type = reader.next();
			if ( type == XMLStreamReader.START_ELEMENT) {
				if ( reader.hasNext() && name.equals( reader.getName().getLocalPart() ) ) 
				{
					tag.name = name;
					tag.contents = reader.getElementText();
					reader.next(); // skip corresponding end element
					return tag;
				}
			}
		}
			
		throw new XMLStreamException("Found no start element <"+name+"> at location",loc );
	}
}
