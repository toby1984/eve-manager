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
package de.codesourcery.eve.apiclient.parsers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.xpath.XPathExpression;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIError;
import de.codesourcery.eve.apiclient.exceptions.APIErrorException;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.apiclient.utils.XMLParseHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Abstract base-class for implementing EVE Online(tm) API parsers.
 * 
 * <pre>
 * This class provides various helper methods for XML parsing
 * in general and Eve Online(tm) API responses.  
 * Note that subclasses will take part in a two-step parsing
 * process:
 * 
 * First: data common to all API responses is always parsed
 *        by the base class by {@link #parseCommonData(Document)}.
 *        If the server returned an API error, the subclass will
 *        <b>not</b> be invoked and a {@link APIErrorException}
 *        will be thrown instead. 
 *        
 * Second: Parsing is handed over to the subclass by invoking
 *         {link {@link #parseHook(Document)}} , so it's safe
 *         to call {@link #getAPIVersion()} etc. from there. 
 * 
 * Although this classes internally uses XPath for parsing
 * the request's common data, XPath tends to be 
 * reaaaally sloooow (because the whole <code>Document</code> is
 * evaluated each time) so I suggest
 * using something else (DOM API or StaX come to mind)
 * if the parser is used a lot.
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class AbstractResponseParser<T> extends XMLParseHelper implements IResponseParser<T> {

	private static final Logger log = Logger.getLogger(AbstractResponseParser.class);

	// not final because of bug with easymock,partial mocking and CGLib
	// (final fields are not initialized by CGlib, constructor is not run either) 
	private ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};

	// XPath expressions for retrieving data common to all requests
	protected static final XPathExpression API_VERSION = compileXPathExpression("/eveapi/@version");
	protected static final XPathExpression ROWSET = compileXPathExpression("/eveapi/result/rowset");
	protected static final XPathExpression CACHED_UNTIL = compileXPathExpression("/eveapi/cachedUntil");
	protected static final XPathExpression SERVER_TIME = compileXPathExpression("/eveapi/currentTime");
	protected static final XPathExpression RESULT_NODE = compileXPathExpression("/eveapi/result");
	private static final XPathExpression ERROR_MESSAGE = compileXPathExpression("/eveapi/error");
	private static final XPathExpression ERROR_CODE = compileXPathExpression("/eveapi/error/@code");

	private final ISystemClock aClock;
	
	// --------- data ----------------

	protected APIError error;
	protected EveDate cachedUntilServerTime;
	protected EveDate serverTime;
	protected int apiVersion;

	private boolean commonDataParsed = false;
	private boolean responseParsed = false;

	/**
	 * Encapsulates a result row.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */
	protected final class Row {

		private final RowSet rowSet;
		private final Map<String, String> data;

		/**
		 * Creates a row.
		 * 
		 * @param rowSet
		 * @param data
		 */
		public Row(RowSet rowSet, Map<String, String> data) {
			if (data == null) {
				throw new IllegalArgumentException("data cannot be NULL");
			}
			this.data = data;
			if (rowSet == null) {
				throw new IllegalArgumentException("rowSet cannot be NULL");
			}
			this.rowSet = rowSet;
		}

		/**
		 * Returns the column names of this row.
		 * 
		 * @return
		 * @see RowSet#getColumnNames()
		 */
		public final Set<String> getColumnNames() {
			return rowSet.getColumnNames();
		}

		/**
		 * Returns the value for a specific column.
		 * 
		 * @param column
		 * @return
		 */
		public final String get(String column) {
			return get(column, true);
		}
		
		public boolean hasColumn(String column) {
			return ! StringUtils.isBlank( data.get( column ) );
		}

		public final long getLong(String column) {
			return Long.parseLong( get(column) );
		}

		public final long getLong(String column,boolean isRequired) {
			final String s = get(column,isRequired);
			if ( s == null ) {
				return 0L;
			}
			return Long.parseLong( s );
		}	

		public final int getInt(String column) {
			return Integer.parseInt( get(column) );
		}

		public final int getInt(String column,boolean isRequired) {
			final String s = get(column,isRequired);
			if ( s == null ) {
				return 0;
			}
			return Integer.parseInt( s );
		}

		public final EveDate getDate(String column) {
			return getDate( column , true);
		}

		public final EveDate getDate(String column
				,boolean isRequired) 
		{
			final String s = get(column,isRequired);
			if ( s == null ) {
				return null;
			}

			try {
				return EveDate.fromServerTime( getDateFormat().parse( s ) , getSystemClock() );
			} catch (ParseException e) {
				throw new  UnparseableResponseException("Unparseable date from server: "+s,e);
			}			
		}		

		public final float getFloat(String column) {
			return Float.parseFloat( get(column) );
		}

		public final double getDouble(String column) {
			return Double.parseDouble( get(column) );
		}

		public final double getDouble(String column,boolean isRequired) {
			final String s = get(column,isRequired);
			if ( s == null ) {
				return 0.0f;
			}
			return Double.parseDouble( s );
		}	

		public final float getFloat(String column,boolean isRequired) {
			final String s = get(column,isRequired);
			if ( s == null ) {
				return 0.0f;
			}
			return Float.parseFloat( s );
		}	

		/**
		 * Parses an ISK amount and returns it as a 
		 * <code>long</code> value (by multiplying it with 100).
		 * 
		 * Most parts of the application use <code>long</code>
		 * values to represent ISK amounts to avoid
		 * rounding issues.		 
		 */
		public long getISKAmount(String column) {
			return (long) ( 100.0d* getDouble( column ) );
		}

		/**
		 * Returns the value for a specific column.
		 * 
		 * @param column
		 * @return
		 */		
		public final String get(String column, boolean isRequired) {
			final String result = data.get(column);
			if (result == null || result.trim().length() == 0) {
				if (isRequired) {
					throw new UnparseableResponseException(
							"Row in response lacks value for column '" + column);
				}
				return null;
			}
			return result;
		}

		/**
		 * Returns number of columns for this row.
		 * 
		 * @param column
		 * @return
		 */		
		public final int getColumnCount() {
			return rowSet.getColumnCount();
		}

	}

	/**
	 * Encapsulates a set of result rows.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */	
	protected final class RowSet implements Iterable<Row> {

		private final String name;
		private final String key;

		private final Set<String> columns;

		private final List<Row> rows = new ArrayList<Row>();

		public RowSet(String name, String key, Set<String> columns) {
			this.name = name;
			this.key = key;
			this.columns = columns;
		}

		public Collection<Row> getRows() {
			return rows;
		}

		/**
		 * Adds a row to this rowset.
		 * 
		 * @param data
		 * @return
		 */
		public Row addRow(Map<String, String> data) {
			final Row r = new Row(this, data);
			rows.add(r);
			return r;
		}

		/**
		 * Returns the column names for this rowset.
		 * 
		 * @return
		 */
		public Set<String> getColumnNames() {
			return columns;
		}

		/**
		 * Returns the number of columns
		 * for this rowset.
		 * @return
		 */
		public int getColumnCount() {
			return columns.size();
		}

		/**
		 * Returns an iterator over
		 * all rows.
		 */
		@Override
		public Iterator<Row> iterator() {
			return rows.iterator();
		}

		/**
		 * Returns whether this rowset
		 * contains any rows.
		 * @return
		 */
		public boolean isEmpty() {
			return rows.isEmpty();
		}

		/**
		 * Returns the number of 
		 * rows in this rowset.
		 * 
		 * @return
		 */
		public int size() {
			return rows.size();
		}

		/**
		 * Returns a row with a specific index.
		 * 
		 * @param index row index, first row = 0
		 * @return
		 */
		public Row getRow(int index) throws NoSuchElementException {
			if (index < 0 || index > rows.size()) {
				throw new NoSuchElementException("Invalid row index " + index
						+ " , only " + size() + " rows available.");
			}
			return rows.get(index);
		}

		/**
		 * Returns the rowset's name.
		 * 
		 * @return
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the rowset's key.
		 * @return
		 */
		public String getKey() {
			return key;
		}

	}

	/**
	 * Returns a thread-local <code>DateFormat</code>
	 * instance for parsing date strings returned
	 * by the API server.
	 * 
	 * @return
	 */
	protected DateFormat getDateFormat() {
		/*
		 * TODO: Hack to circumvent a 
		 * bug with EasyMock partial mocking, somehow
		 * final instance fields are not initialized by CGLib and
		 * since the constructor doesn't seem to be run as well...
		 */
		final SimpleDateFormat result;
		if ( this.DATE_FORMAT == null ) {
			result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			this.DATE_FORMAT = new ThreadLocal<SimpleDateFormat>();
			DATE_FORMAT.set( result);
		} else {
			result = this.DATE_FORMAT.get();
		}
		return result;
	}

	public AbstractResponseParser(ISystemClock clock) {
		if ( clock == null ) {
			throw new IllegalArgumentException("clock cannot be NULL");
		}
		this.aClock = clock;
	}
	
	protected ISystemClock getSystemClock() {
		return aClock;
	}

	/**
	 * Wrapper to get rid of the 
	 * URI constructor's checked exception.
	 * 
	 * @param s
	 * @return
	 */
	protected static URI toURI(String s) {
		try {
			return new URI( s );
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts a <code>Double</code> ISK amount to
	 * a <code>long</code> value by multiplying it with 100.
	 * 
	 * Most parts of the application use <code>long</code>
	 * values to represent ISK amounts to avoid
	 * rounding issues.
	 * 
	 * @param price
	 * @return
	 */
	protected long toISKAmount(double price) {
		return (long) ( 100.0d* price );
	}
	/**
	 * Get time the server caches this response.
	 * 
	 * Response is <b>only</b> valid after {@link #parse(String)} has been
	 * called}.
	 * 
	 * @return cachedUntil time
	 * @throws IllegalStateException if common data has not been parsed yet
	 * @see #assertCommonDataParsed()	 
	 */
	@Override
	public final EveDate getCachedUntilServerTime() {
		assertCommonDataParsed();		
		return cachedUntilServerTime;
	}

	/**
	 * Get server time of this reponse.
	 * 
	 * Response is <b>only</b> valid after {@link #parse(String)} has been
	 * called}.
	 * 
	 * @return server time
	 * @throws IllegalStateException if common data has not been parsed yet
	 * @see #assertCommonDataParsed()
	 */
	@Override
	public final EveDate getServerTime() {
		assertCommonDataParsed();
		return serverTime;
	}

	/**
	 * Assert that the request's common data has been parsed.
	 *  
	 * @throws IllegalStateException if common data has not been parsed yet
	 * @see #parseCommonData(Document)
	 */
	protected final void assertCommonDataParsed() {
		if (!commonDataParsed) {
			throw new IllegalStateException(
			"response (common data) not parsed yet");
		}
	}

	/**
	 * Assert that request has been successfully
	 * parsed by the subclass.
	 * 
	 * @see #parseHook(Document)
	 * @see #assertCommonDataParsed()
	 * @throws IllegalStateException if the subclass parser has not completed
	 * parsing successfully
	 * @see #parseCommonData(Document)	 
	 */
	protected final void assertResponseParsed() {
		if (! responseParsed) {
			throw new IllegalStateException(
			"response not parsed yet");
		}
	}	

	/**
	 * Returns the API version returned with the server response.
	 *  
	 * @throws IllegalStateException if common data has not been parsed yet
	 * @see #assertCommonDataParsed()
	 */
	public final int getAPIVersion() {
		assertCommonDataParsed();
		return apiVersion;
	}

	public final InternalAPIResponse parse(Date responseTimestamp , String xml) throws UnparseableResponseException,APIErrorException  {

		log.debug("parse(): Parsing response xml...");

		if (responseParsed) {
			throw new IllegalStateException("response is already responseParsed ?");
		}

		final Document doc =
			parseXML( xml );

		log.debug("parse(): Parsing common data");

		final APIError error = 
			parseCommonData(doc);

		if ( error != null ) {
			log.error("parse(): Server returned error "+error);
			throw new APIErrorException( error );
		}

		commonDataParsed = true;

		log.debug("parse(): Running subclassing hook");
		parseHook(doc);

		responseParsed = true;

		log.debug("parse(): Parsing finished.");

		return new InternalAPIResponse( xml , responseTimestamp , this );		
	}

	/**
	 * Returns the XML node that resembles the response's 
	 * {@literal <result/>} element.
	 * @param doc
	 * @return
	 */
	protected Element getResultElement(Document doc) {
		return (Element) selectNode( doc , RESULT_NODE );
	}

	/**
	 * Parses the APIs error message.
	 *  
	 * @param doc
	 * @return error message or <code>null</code> if the request
	 * did not contain an error message
	 */
	protected String parseErrorMessage(Document doc) {
		return selectNodeValue( doc , ERROR_MESSAGE , false );
	}

	/**
	 * Parses any API error contained in the
	 * server's response.
	 * 
	 * @param doc
	 * @return API error or <code>null</code> if the response
	 * indicates no error occured..
	 */
	protected APIError parseError(Document doc) {

		final int errorCode =
			parseErrorCode( doc );
		if ( errorCode == 0 ) {
			return null;
		}
		return new APIError( errorCode , parseErrorMessage( doc ) );
	}

	/**
	 * Parses the server's numeric error code from the response.
	 * 
	 * @param doc
	 * @return
	 */
	protected int parseErrorCode(Document doc) {
		return Integer.parseInt( selectAttributeValue( doc , ERROR_CODE , "0" ) );
	}

	/**
	 * Parses the API version returned in the
	 * server's response.
	 * 
	 * @param doc
	 * @return
	 */
	protected int parseAPIVersion(Document doc) {
		return Integer.parseInt(selectAttributeValue(doc, API_VERSION ) );
	}

	/**
	 * Parses a date string in the APIs date format ( YYYY-MM-DD HH:MM:SS ).
	 * @param s
	 * @return
	 */
	protected final EveDate parseDate(String s) {
		try {
			return EveDate.fromServerTime( getDateFormat().parse(s) , getSystemClock() );
		} catch (ParseException e) {
			throw new UnparseableResponseException(
					"XML contained unparseable date: " + s, e);
		}
	}

	/**
	 * Parses the server time returned with the response.
	 * @param doc
	 * @return
	 */
	protected EveDate parseServerTime(Document doc) {
		return parseDate( selectNodeValue(doc, SERVER_TIME) );
	}

	/**
	 * Parses the {@literal <cachedUntil/>} node 
	 * from the server's response.
	 * @param doc
	 * @return
	 */	
	protected EveDate parseCachedUntilTime(Document doc) {
		return parseDate( selectNodeValue(doc , CACHED_UNTIL)  );
	}

	/**
	 * Parses a rowset with a given name.
	 * @param name
	 * @param doc
	 * @return
	 * @throws UnparseableResponseException if the response
	 * contained no rowset by that name
	 */
	protected RowSet parseRowSet(String name,Document doc) {
		return parseRowSet( name , doc , true );
	}

	/**
	 * Parses a rowset with a given name.
	 * 
	 * @param name
	 * @param doc
	 * @param isRequired whether the rowset is mandatory
	 * @return rowset or <code>null</code> if <code>isRequired</code>
	 * was set to <code>false</code> and no rowset by that name was found 
	 * @throws UnparseableResponseException if the response
	 * contained no rowset by that name and <code>isRequired</code>
	 * was set to <code>true</code>  
	 */
	protected RowSet parseRowSet(String name,Document doc,boolean isRequired) {

		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException(
			"rowset name cannot be blank / NULL");
		}

		final List<Element> matches = selectElements(doc, ROWSET);

		Element match = null;
		for ( Element rowSet : matches ) {
			if ( getAttributeValue( rowSet , "name" ).equals( name ) ) {

				if ( match != null ) {
					throw new UnparseableResponseException("Response contains more than "+
							" one rowset with name '"+name+"' ?");
				}
				match = rowSet;
			}
		}

		if ( match == null ) {
			if ( isRequired ) {
				throw new UnparseableResponseException("Response lacks expected "+
						" rowset with name '"+name+"' ?");
			}
			return null;
		}

		return parseRowSet( name , match );
	}

	/**
	 * Returns the {@literal <rowset/>} node with a given name.
	 * 
	 * @param parent
	 * @param rowSetName
	 * @return
	 * @throws UnparseableResponseException if the response
	 * contained no rowset by that name	 
	 */
	protected Element getRowSetNode(Node parent, String rowSetName) {
		/*
          <row typeName="Repair Drone Operation" groupID="273" typeID="3439">
            <description>Allows operation of logistic drones. 5% increased repair amount per level.</description>
            <rank>3</rank>
            <rowset name="requiredSkills" key="typeID" columns="typeID,skillLevel">
              <row typeID="3436" skillLevel="5" />
              <row typeID="23618" skillLevel="1" />
            </rowset>
            <requiredAttributes>
              <primaryAttribute>memory</primaryAttribute>
              <secondaryAttribute>perception</secondaryAttribute>
            </requiredAttributes>
            <rowset name="skillBonusCollection" key="bonusType" columns="bonusType,bonusValue">
              <row bonusType="damageHP" bonusValue="5" />
            </rowset>
          </row>		 
		 */

		Element result = null;
		final NodeList children = parent.getChildNodes();
		for ( int i = 0 ; i < children.getLength() ; i++ ) {
			Node n = children.item(i);
			if ( ! "rowset".equals( n.getNodeName() ) ) {
				continue;
			}
			if ( ! rowSetName.equals( getAttributeValue((Element) n , "name", false ) ) ) {
				continue;
			}
			if ( result != null ) {
				throw new UnparseableResponseException("Found more than " +
						"one <rowset/> node with " +
						"name '"+rowSetName+"' below node "+parent+" ?");
			}
			result = (Element) n;
		}

		if ( result == null ) {
			throw new UnparseableResponseException("Found no " +
					"<rowset/> node with " +
					"name '"+rowSetName+"' below node "+parent+" ?");
		}
		return result;
	}

	/**
	 * Parse {@literal <rowset/>} XML.
	 * 
	 * @param name the rowset name the returned rowset should get
	 * @param rowSet the rowset node
	 * @return rowset with the given name
	 * @throws UnparseableResponseException if the rowset
	 * does not contain any columns or the XML has an unexpected structure.
	 * @see #getRowSetNode(Node, String)
	 */	
	protected RowSet parseRowSet(String name , Element rowSet) {

		// parse columns
		final Set<String> columns = new HashSet<String>();

		final String[] sColumns = getAttributeValue(rowSet, "columns").split(
		",");

		if (ArrayUtils.isEmpty(sColumns)) {
			throw new UnparseableResponseException(
			"Response XML contains rowset without any columns ?");
		}

		for (String col : sColumns) {
			columns.add(col);
		}

		// parse key
		final String key = getAttributeValue(rowSet, "key", false);

		final RowSet result = new RowSet(name, key, columns);

		// parse rows
		final NodeList children = rowSet.getChildNodes();
		for ( int i = 0 ; i < children.getLength() ; i++) {
			final Node n = children.item(i);
			if ( ! ( n instanceof Element ) ) {
				continue;
			}

			final Element element = (Element) n;
			if ( ! "row".equalsIgnoreCase( element.getNodeName() ) ) {
				throw new UnparseableResponseException("Invalid response XML ," +
						" found unexpected node <"+element.getNodeName()+"> in rowset");
			}

			final Map<String,String> data =
				new HashMap<String, String>();

			for ( String attribute : result.getColumnNames() ) {
				final String value = getAttributeValue( element , attribute , false );
				if ( value != null ) {
					data.put( attribute , value );
				}
			}
			result.addRow( data );
		}

		return result;
	}

	protected APIError parseCommonData(Document doc) {
		/*
		 * <?xml version='1.0' encoding='UTF-8'?> <eveapi version="1">
		 * 
		 * <currentTime>2007-12-12 11:48:50</currentTime> 
		 * <result> 
		 *   <rowset name="characters" 
		 *         key="characterID"
		 *         columns="name,characterID,corporationName,corporationID">
		 *     <row name="Mary" characterID="150267069" corporationName="Starbase Anchoring Corp" corporationID="150279367"/>
		 *     <row name="Marcus" characterID="150302299" corporationName="Marcus Corp" corporationID="150333466" />
		 *     <row name="Dieinafire" characterID="150340823" corporationName="Center for Advanced Studies" corporationID="1000169" />
		 *   </rowset>
		 * </result>
		 * <cachedUntil>2007-12-12 12:48:50</cachedUntil>
		 * </eveapi>
		 */

		this.error = parseError( doc );
		this.apiVersion = parseAPIVersion(doc);
		this.serverTime = parseServerTime(doc);
		this.cachedUntilServerTime = parseCachedUntilTime(doc);

		if ( log.isDebugEnabled() ) {
			log.debug("parseCommonData(): Received API version="+apiVersion+
					" , error = "+this.error+
					" , server time="+this.serverTime+
					" , cached_until="+this.cachedUntilServerTime);
		}

		return error;
	}
	/**
	 * Returns any API error the response may contain.
	 * 
	 * @throws IllegalStateException if common data has not been parsed yet
	 * @see #assertCommonDataParsed()	 
	 */
	public APIError getError() {
		assertCommonDataParsed();
		return error;
	}

	/**
	 * Subclassing hook.
	 * 
	 * This hook is invoked after common data has been parsed 
	 * so calling methods like {link {@link #getServerTime()}
	 * etc. is ok in here.
	 * 
	 * Note that the hook is <b>never</b> executed 
	 * if parsing the common data indicated a server error
	 * ( {@link #parseCommonData(Document)} will throw a
	 * {@link APIErrorException} in this case).
	 * 
	 * @param document
	 * @throws UnparseableResponseException
	 * @throws IOException
	 * 
	 * @see #assertCommonDataParsed()
	 */
	abstract void parseHook(Document document) throws UnparseableResponseException;

}
