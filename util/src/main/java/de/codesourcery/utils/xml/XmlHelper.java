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
package de.codesourcery.utils.xml;

/*
 * Copyright 2007 Tobias Gierke (Tobias.Gierke@code-sourcery.de)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on 
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language governing 
 * permissions and limitations under the License. 
 */

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlHelper {

	private static final Log log = LogFactoryImpl.getLog(XmlHelper.class
			.getName());

	public static Document parseFile(File inputFile) throws SAXException, IOException, ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse( inputFile );
	}
	
	public static String getDirectChildValue(Element parent,String childTag,boolean isRequired) throws ParseException {
		
		NodeList list = 
			parent.getChildNodes();
		
		Node matchingNode = null;
		for ( int i = 0 ; i < list.getLength() ; i++ ) {
			Node n = 
				list.item( i );
			
			if ( n.getNodeName().equals( childTag ) ) {
				if ( matchingNode == null ) {
					matchingNode = n;
				} else {
					throw new ParseException("Node "+parent.getNodeName()+" contains more than one child <"+childTag+"> ?!",-1);
				}
			}
		}
		
		if ( matchingNode == null ) {
			if ( isRequired ) {
				throw new ParseException("Node "+parent.getNodeName()+
						" contains no child node <"+childTag+"> ?!",-1);				
			}
			return null;
		}
		
		return getNodeValue( matchingNode, null , isRequired );
	}

	public static Element getElement(Document doc , String tagName, boolean isRequired)
			throws ParseException {

		NodeList list = 
			doc.getElementsByTagName(tagName);

		if (list.getLength() == 0 && isRequired) {
			log.error("getElement(): XML is lacking required tag <" + tagName
					+ ">");
			throw new ParseException("XML is lacking required tag <" + tagName
					+ ">", -1);
		}

		if (list.getLength() > 1) {
			log.error("getElement(): XML is contains multiple <" + tagName
					+ "> tags, expected only one");
			throw new ParseException("XML is contains multiple <" + tagName
					+ "> tags, expected only one", -1);
		}

		Node n = list.item(0);
		if (!(n instanceof Element)) {
			log
					.error("getElement(): Internal error, expected Element but got Node");
			throw new ParseException(
					"Internal error, expected Element but got Node", -1);
		}
		return (Element) n;
	}
	
	public static String getElementValue(Element parent , String tagName, boolean isRequired)
	throws ParseException {
		Element element =
			getElement( parent , tagName, isRequired );
		
		String result = null;
		if ( element != null ) {
			result = getNodeValue( element , null, isRequired );
		}
		
		if ( StringUtils.isBlank( result ) && isRequired ) {
			final String msg = "Required child tag <"+tagName+" of parent <"+parent.getNodeName()+
					" to have non-blank value";
			log.error("getElement(): "+msg);
			throw new ParseException( msg , -1 );
		}
		return result;
	}
	
	
	public static Element getElement(Element parent , String tagName, boolean isRequired)
		throws ParseException 
		{

		NodeList list = 
			parent.getElementsByTagName( tagName );

		if (list.getLength() == 0 ) {
			if ( isRequired ) {
				log.error("getElement(): XML is lacking required tag <" + tagName
						+ ">");
				throw new ParseException("XML is lacking required tag <" + tagName
						+ ">", -1);
			}
			return null;
		}

		if (list.getLength() > 1) {
			log.error("getElement(): XML is contains multiple <" + tagName
					+ "> tags, expected only one");
			throw new ParseException("XML is contains multiple <" + tagName
					+ "> tags, expected only one", -1);
		}

		Node n = list.item(0);
		if (!(n instanceof Element)) {
			log
			.error("getElement(): Internal error, expected Element but got Node");
			throw new ParseException(
					"Internal error, expected Element but got Node", -1);
		}
		return (Element) n;
	}

	public static List<Element> getNodeChildren(Document doc , String parentTag, String childTag,
			boolean isRequired) throws ParseException {

		final Element parent = getElement(doc , parentTag, isRequired);
		return getNodeChildren( parent , childTag , isRequired );
	}
	
	public static List<Element> getNodeChildren(Element parent, String childTag,
			boolean isRequired) throws ParseException {
		
		return getNodeChildren( parent, childTag , isRequired ? 1 : -1 , -1 );
	}

	public static List<Element> getNodeChildren(Element parent, String childTag,
			int minCount,int maxCount) throws ParseException {

		final List<Element> result = new LinkedList<Element>();

		if (parent != null) {

			NodeList list = parent.getChildNodes();

			for (int i = 0; i < list.getLength(); i++) {
				Node n = list.item(i);
				if ( n.getNodeName().equals( childTag ) ) {
					if (n instanceof Element) {
						result.add((Element) n);
					} else {
						log.trace("getNodeChildren(): Ignoring non-Element "
								+ n.getNodeName() + " , type " + n.getNodeType());
					}
				}
			}
		}

		if (result.isEmpty() && minCount > 0 ) {
			final String msg = "XML is lacking required child tag <" + childTag
					+ "> for parent tag <" + parent.getTagName() + ">";
			log.error("getElement(): " + msg);
			throw new ParseException(msg, -1);
		}

		if ( maxCount > 0 && result.size() > maxCount ) {
			final String msg = "XML may contain at most "+maxCount+" child tags <" + childTag
			+ "> for parent tag <" + parent.getTagName() + ">";
			log.error("getElement(): " + msg);
			throw new ParseException(msg, -1);
		}
		return result;
	}
	public static String getNodeValue(Node n, String defaultValue, boolean isRequired)
			throws ParseException {

		if (n == null) {

			if (isRequired) {
				final String msg = "Unable to determine node value";
				log.error("getNodeValue(): " + msg);
				throw new ParseException(msg, -1);
			}
			return defaultValue;
		}

		String result = null;
		if (n.getNodeType() == Node.TEXT_NODE) {
			result = n.getNodeValue();
		} else {
			NodeList children = n.getChildNodes();

			if (children.getLength() == 0) {
				log.trace("getNodeValue(): Node " + n.getLocalName()
						+ " is no TEXT_NODE and has no children");
			} else if (children.getLength() == 1) {
				if (children.item(0).getNodeType() == Node.TEXT_NODE) {
					result = children.item(0).getNodeValue();
				} else {
					log.trace("getNodeValue(): Node " + n.getLocalName()
							+ " doesn't have TEXT_NODE children");
				}
			} else {
				log.error("getNodeValue(): Node " + n.getLocalName()
						+ " is no TEXT_NODE and has multiple children");
				throw new ParseException("Node " + n.getLocalName()
						+ " is no TEXT_NODE and has multiple children", -1);
			}

		}

		if (StringUtils.isBlank(result)) {
			if (isRequired) {
				final String msg = "Node " + n.getLocalName()
						+ " requires a non-blank value";
				log.error("getNodeValue(): " + msg);
				throw new ParseException(msg, -1);
			} else {
				return defaultValue;
			}
		}

		return result;
	}

	public static int getIntAttribute(Element root, String attrName, boolean isRequired) throws ParseException {
		
		String sValue = 
			root.getAttribute(attrName);
		
		if ( ! StringUtils.isBlank( sValue ) ) {
			try {
				return Integer.parseInt( sValue );
			}
			catch(NumberFormatException ex) {
				final String msg = "Invalid attribute value (not a number) for tag <"+root.getNodeName()+">"+
				" , attribute "+attrName+" , value '"+sValue+"'";
				log.error("getIntAttribute(): "+msg);
				throw new ParseException( msg , - 1 );
			}
		} else {
			
			if ( isRequired ) {
				final String msg = 
					"Tag <"+root.getNodeName()+"> is lacking required integer attribute "+
					attrName;
				log.error("getIntAttribute(): "+msg);
				throw new ParseException( msg , - 1 );
			}
			
			return 0;
		}
	}
	
	public boolean getBooleanAttribute(Element root, String attrName, boolean isRequired) throws ParseException {
		
		String sValue = 
			root.getAttribute(attrName);
		
		if ( ! StringUtils.isBlank( sValue ) ) {
				if ( "1".equalsIgnoreCase( sValue ) ||
				     "true".equalsIgnoreCase( sValue ) ||
				     "yes".equalsIgnoreCase( sValue ) ) {
					return true;
				}
				return false;
		} else {
			
			if ( isRequired ) {
				final String msg = 
					"Tag <"+root.getNodeName()+"> is lacking required boolean attribute "+
					attrName;
				log.error("getBooleanAttribute(): "+msg);
				throw new ParseException( msg , - 1 );
			}
			
			return false;
		}
	}
	
	public static String getAttribute(Element root, String attrName, boolean isRequired) throws ParseException {
		
		String sValue = 
			root.getAttribute(attrName);
		
		if ( ! StringUtils.isBlank( sValue ) ) {
				return sValue;
		} else {
			
			if ( isRequired ) {
				final String msg = 
					"Tag <"+root.getNodeName()+"> is lacking required attribute "+
					attrName;
				log.error("getAttribute(): "+msg);
				throw new ParseException( msg , - 1 );
			}
			
			return null;
		}
	}
}