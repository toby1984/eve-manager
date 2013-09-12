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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;


/**
 * Abstract base-class for implementing XML parsers.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class XMLParseHelper {

	public static XPathExpression compileXPathExpression(String xPathExpression) {

		final XPathFactory factory = XPathFactory.newInstance();
		final XPath xpath = factory.newXPath();
		try {
			return xpath.compile( xPathExpression );
		} catch (XPathExpressionException e) {
			throw new RuntimeException(
					"Internal error, invalid XPATH expression " + xPathExpression, e);
		}
	}
	
	public static final void writeXML(Document doc , OutputStream out) throws TransformerException {
		final TransformerFactory tf = TransformerFactory.newInstance();
		final Transformer serializer = tf.newTransformer();
		serializer.transform(new DOMSource( doc ), new StreamResult(out) ); 
	}
	
	public static String selectNodeValue(Document doc, XPathExpression expr) {
		return selectNodeValue(doc, expr, true );
	}
	
	public static String selectNodeValue(Document doc, XPathExpression expr,boolean isRequired) {
		final Node n = selectNode(doc, expr, isRequired );
		
		if ( n == null ) {
			return null;
		}
		
		final String result = n.getTextContent();
		if ( StringUtils.isBlank( result ) ) {
			if ( isRequired ) {
				throw new UnparseableResponseException("Selected node '"+n.getNodeName()+"' does not contain a value");
			}
			return null;
		}
		return result;
	}
	
	protected static final NodeIterator nodeIterator(NodeList n) {
		return new NodeIterator(n);
	}
	
	protected static final ElementIterator elementIterator(NodeList n) {
		return new ElementIterator(n);
	}
	
	protected static final class NodeIterator implements Iterator<Node>,Iterable<Node> {
		
		private int index = 0;
		private final NodeList list;
		
		public NodeIterator(NodeList n) {
			this.list = n;
		}

		@Override
		public boolean hasNext() {
			return index < list.getLength();
		}

		@Override
		public Node next() {
			if ( index >= list.getLength() ) {
				throw new NoSuchElementException();
			}			
			return list.item( index++ );
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove()");
		}

		@Override
		public Iterator<Node> iterator() {
			return this;
		}
		
	}
	
	protected static final class ElementIterator implements Iterator<Element>,Iterable<Element> {
		
		private int index = 0;
		private final NodeList list;
		
		public ElementIterator(NodeList n) {
			this.list = n;
		}

		@Override
		public boolean hasNext() {
			return index < list.getLength();
		}

		@Override
		public Element next() {
			if ( index >= list.getLength() ) {
				throw new NoSuchElementException();
			}
			
			Node n = null;
			do {
				n = list.item( index ++ );
			} while ( n.getNodeType() != Node.ELEMENT_NODE && index < list.getLength() );
			
			if ( n.getNodeType() == Node.ELEMENT_NODE ) {
				return (Element) n;
			}
			throw new NoSuchElementException("Unable to find any element node in list");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove()");
		}

		@Override
		public Iterator<Element> iterator() {
			return this;
		}
		
	}
	

	public static List<Element> getChildNodes(Node parent,String childName) {
	
		final List<Element> result = 
			new ArrayList<Element>();
		
		final NodeList nodes = parent.getChildNodes();
		
		final int len = 
			nodes.getLength() ;
		
		for ( int i = 0 ; i < len ; i++ ) {
			final Node n = nodes.item(i);
			if ( ! isElementNode( n ) ) {
				continue;
			}
			if ( n.getNodeName().equals( childName ) ) {
				result.add( (Element) n);
			}
		}
		return result;
	}
	
	public static Node selectNode(Document doc, XPathExpression expr) {
		return selectNode(doc, expr, true);
	}

	public static String getChildValue(Element node,String childName) {
		
		final NodeList l = node.getChildNodes();
		Element result = null;
		for ( int i = 0; i < l.getLength() ; i++) {
			final Node n = l.item( i );
			if ( n instanceof Element &&
					n.getNodeName().equals( childName ) ) 
			{
				if ( result != null ) {
					throw new UnparseableResponseException("Found more than one child element "+
							" named '"+childName+"' " +
							"below node '"+node.getNodeName()+
							"' , expected exactly one");
				}
				result = (Element) n;
			}
		}
		if ( result == null ) {
			throw new UnparseableResponseException("Expected child element "+
					" '"+childName+"' " +
					" not found below node '"+node.getNodeName());
		}
		
		final String value = result.getTextContent();
		if ( StringUtils.isBlank( value ) ) {
			throw new UnparseableResponseException("Child element "+
					" '"+childName+"' " +
					" below node '"+node.getNodeName()+" has no/blank value ?");
		}
		return value;
	}
	
	public static final String getNodeValue(Node n) {
		final String value = n.getTextContent();
		if ( StringUtils.isBlank( value ) ) {
			throw new UnparseableResponseException("Child element "+
					" '"+n.getNodeName()+"' " +
					" has no/blank value ?");
		}
		return value;
	}
	
	public static String selectAttributeValue(Document doc, 
			XPathExpression expr) 
	{
		return selectAttributeValue(doc, expr,true);	
	}
	
	public static String selectAttributeValue(Document doc, 
			XPathExpression expr,
			String defaultValue) 
	{
		final String result = selectAttributeValue( doc , expr , false );
		return result != null ? result : defaultValue;
	}
	
	public static String selectAttributeValue(Document doc, 
			XPathExpression expr,
			boolean isRequired) 
	{

		final String result;
		try {
			result = (String) expr.evaluate(doc, XPathConstants.STRING);
		} 
		catch (XPathExpressionException e) {
			throw new RuntimeException(
					"Internal error while evaluating XPath expression " + expr,
					e);
		}

		if ( StringUtils.isBlank( result ) ) {
			if (isRequired) {
				throw new UnparseableResponseException(
						"Unexpected response XML,"
						+ " XPath expression returned nothing: " + expr);
			}
			return null;
		}

		return result;
	}
	
	public static Node selectNode(Document doc, XPathExpression expr,
			boolean isRequired) {

		final Node result;
		try {
			result = (Node) expr.evaluate(doc, XPathConstants.NODE);
		} 
		catch (XPathExpressionException e) {
			throw new RuntimeException(
					"Internal error while evaluating XPath expression " + expr,
					e);
		}

		if (result == null) {
			if (isRequired) {
				throw new UnparseableResponseException(
						"Unexpected response XML,"
						+ " XPath expression returned nothing: " + expr);
			}
			return null;
		}

		return result;
	}

	public static List<Element> selectElements(Document doc,
			XPathExpression expr) {
		return selectElements(doc, expr, true);
	}

	public static List<Element> selectElements(Document doc,
			XPathExpression expr, boolean isRequired) {
		final NodeList result;
		try {
			result = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(
					"Internal error while evaluating XPath expression " + expr,
					e);
		}

		if (result.getLength() == 0) {
			if (isRequired) {
				throw new UnparseableResponseException(
						"Unexpected response XML,"
						+ " XPath expression returned nothing: " + expr);
			}
			return Collections.emptyList();
		}

		return new AbstractList<Element>() {

			@Override
			public Element get(int index) {
				return (Element) result.item(index);
			}

			@Override
			public int size() {
				return result.getLength();
			}
		};
	}

	public static Document parseXML(String xml) throws UnparseableResponseException  {

		final DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder;
		try {
			docBuilder = fac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(
					"Error while creating XML document builder factory: "
					+ e.getMessage(), e);
		}

		try {
			return docBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
		} catch (SAXException e) {
			throw new UnparseableResponseException(
					"Received invalid XML from server", e);
		}
		catch (IOException e) {
			// should never happen, we're reading a byte array....
			throw new RuntimeException(e);
		}		

	}

	public static String getAttributeValue(Element element, String attr) {
		return getAttributeValue(element, attr, true);
	}

	public static String getAttributeValue(Element element, String attr,
			boolean isRequired) 
	{
		final String value = element.getAttribute(attr);
		if (StringUtils.isBlank(value)) {
			if (isRequired ) {
				throw new UnparseableResponseException(
						"Response XML rowset lacks value for attribute '" + attr
						+ "'");
			}
			return null;
		}
		return value;
	}

	/**
	 * Check whether a given XML <code>Node</code> is
	 * an element node.
	 * 
	 * Sun should've implemented <code>Node.hasType(int)</code>....
	 * 
	 * @param n
	 * @return
	 */
	public static final boolean isElementNode(Node n) {
		return n != null && n.getNodeType() == Node.ELEMENT_NODE;
	}

	/**
	 * Get XML attribute value as an integer.
	 * 
	 * @param node
	 * @param attribute
	 * @return attribute value as an integer
	 * @throws UnparseableResponseException If the attribute is missing, has a blank
	 * value or did not contain a parseable integer string.
	 */
	public static int getIntAttributeValue(Element node, String attribute) {
		return getIntAttributeValue(node, attribute, true );
	}

	/**
	 * Get XML attribute value as an integer.
	 * 
	 * @param node
	 * @param attribute
	 * @return attribute value or <code>0</code> if the attribute was missing/blank and
	 * <code>isRequired</code> was set to <code>false</code>
	 * @throws UnparseableResponseException If the attribute did not contain a parseable integer string.
	 */
	public static int getIntAttributeValue(Element node, String attribute, boolean isRequired) {
		final String value = getAttributeValue(node , attribute,isRequired);
		if ( value == null ) {
			return 0;
		}
		return Integer.parseInt( value );
	}
	
	/**
	 * Get XML attribute value as a Long.
	 * 
	 * @param node
	 * @param attribute
	 * @return attribute value as an integer
	 * @throws UnparseableResponseException If the attribute is missing, has a blank
	 * value or did not contain a parseable Long string.
	 */
	public static long getLongAttributeValue(Element node, String attribute) {
		return getLongAttributeValue(node, attribute, true );
	}

	/**
	 * Get XML attribute value as an Long.
	 * 
	 * @param node
	 * @param attribute
	 * @return attribute value or <code>0</code> if the attribute was missing/blank and
	 * <code>isRequired</code> was set to <code>false</code>
	 * @throws UnparseableResponseException If the attribute did not contain a parseable Long string.
	 */
	public static long getLongAttributeValue(Element node, String attribute, boolean isRequired) {
		final String value = getAttributeValue(node , attribute,isRequired);
		if ( value == null ) {
			return 0;
		}
		return Long.parseLong( value );
	}	
	
	/**
	 * Looks-up a child node by name.
	 * 
	 * @param parent
	 * @param name
	 * @return
	 * @throws UnparseableResponseException if the parent node had no such child 
	 * element
	 */
	public static Element getChild(Node parent, String name) {
		return getChild( parent , name , true );
	}

	/**
	 * Looks-up a child node by name.
	 * 
	 * @param parent
	 * @param name
	 * @return child node or <code>null</code> if the parent had no such node
	 * and <code>isRequired</code> was set to <code>false</code>
	 */
	public static Element getChild(Node parent, String name, boolean isRequired) {
	
		Node result = null;
		final NodeList children = parent.getChildNodes();
		for ( int i = 0 ; i < children.getLength() ; i++ ) {
			final Node n = children.item(i);
			if ( n.getNodeName().equals( name ) ) {
	
				if ( result != null ) {
					throw new UnparseableResponseException("Found more than one " +
							"child node named '"+name+"' below "+parent+"?");
				}
				result = n;
			}
		}
	
		if ( result == null ) {
			if ( isRequired ) {
				throw new UnparseableResponseException("Found no " +
						"child node named '"+name+"' below "+parent+"?");
			}
			return null;
		}
		return (Element) result;
	}

}
