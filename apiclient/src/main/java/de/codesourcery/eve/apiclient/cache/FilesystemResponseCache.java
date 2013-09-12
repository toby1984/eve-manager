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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.apiclient.utils.ICipherProvider;
import de.codesourcery.eve.apiclient.utils.XMLStreamHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Cache provider that persists cache contents
 * to files in a directory.
 * 
 * Data gets persisted when {@link #shutdown()}
 * is executed or dirty cache entries get 
 * purged because of memory constraints.  
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class FilesystemResponseCache extends InMemoryResponseCache {

	public static final String FILE_ENCODING = "ISO-8859-1";

	private final ThreadLocal<DateFormat> DATE_FORMAT = 
		new ThreadLocal<DateFormat>();

	private static final Logger log = Logger
	.getLogger(FilesystemResponseCache.class);

	private File cacheDirectory;
	private final ICipherProvider cipherProvider;
	private final ISystemClock systemClock;
	
	/**
	 * Config option: Controls whether <code>put()</code> / <code>evict()</code> operations
	 * will always be persisted to disk immediately.
	 * 
	 * Possible values: true,false
	 * @see #setCacheOptions(Map)
	 */
	public static final String OPTION_WRITE_IMMEDIATELY = "filecache.write_immediately";

	/**
	 * Controls whether <code>put()</code> / <code>evict()</code> operations
	 * will always be persisted to disk immediately.
	 */
	private boolean immediatelyWriteToDisk = false; 
	
	public FilesystemResponseCache(String cacheDirectory,ISystemClock clock) throws IOException, XMLStreamException {
		this( new File(cacheDirectory ), clock );
	}

	public FilesystemResponseCache(File cacheFile,ISystemClock clock) throws IOException, XMLStreamException {
		this( cacheFile , null , clock );
	}

	/**
	 * Create instance.
	 * @param cacheFile The file where to store cached data
	 * @param provider <code>Cipher</code> provider to be used
	 * for encrypting/decrypting the stored data, may be <code>null</code> (=no
	 * encryption).
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public FilesystemResponseCache(File cacheFile, ICipherProvider provider,ISystemClock systemClock) throws IOException, XMLStreamException {

		super();

		if ( systemClock == null ) {
			throw new IllegalArgumentException("systemClock cannot be NULL");
		}
		
		if ( cacheFile == null) {
			throw new IllegalArgumentException("Cache file cannot be NULL");
		}

		this.systemClock = systemClock;
		this.cipherProvider = provider;
		this.cacheDirectory = cacheFile;

		if ( cacheFile.exists() && ! cacheFile.isDirectory() ) {
			throw new IllegalAccessError("Cache directory "+cacheFile+" is not a directory ?");
		}

		if ( ! getCacheDirectory().exists() ) {

			log.info("FilesystemResponseCache(): Creating cache directory "+
					this.cacheDirectory.getAbsolutePath());
			if ( ! getCacheDirectory().mkdir() ) {
				throw new IOException("Unable to create cache directory "+
						this.cacheDirectory.getAbsolutePath());
			}
		}

		log.info("FilesystemResponseCache(): Using cache directory "+
				getCacheDirectory().getAbsolutePath());
	}

	protected final File getCacheDirectory() {
		return cacheDirectory;
	}

	@Override
	protected InternalAPIResponse loadFromBackingStore(APIQuery query) {

		final File entryFile =
			getFilenameForEntry( query );

		if ( ! entryFile.exists() ) {
			if ( log.isTraceEnabled() ) {
				log.trace("loadFromBackingStore(): [ 2ND LEVEL CACHE MISS ] "+query.getHashString());
			}
			return null;
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("loadFromBackingStore(): [ 2ND LEVEL CACHE HIT ] "+query.getHashString());
		}

		CacheEntry entry;
		try {
			entry = loadCacheEntry( entryFile );
			if ( entry == null ) { // file might've been corrupted , loadCacheEntry() will delete the file and return NULL
				return null;
			}
			super.put( query , entry.response );
			return entry.response;
		} 
		catch (Exception e) {
			log.error("loadFromBackingStore(): Unable to load cache entry "+entryFile,e);
		}
		return null;
	}

	static final class CacheEntry {
		public APIQuery query;
		public InternalAPIResponse response;

		public CacheEntry(APIQuery query, InternalAPIResponse response) {
			this.query = query;
			this.response = response;
		}
	}

	protected void cacheEntryReplaced(APIQuery query) {

		final File cacheFile = getFilenameForEntry( query );
		
		if ( log.isTraceEnabled()) {
			log.trace("cacheEntryReplaced(): Removing cache entry from disk : "+
					cacheFile.getAbsolutePath());
		}

		// delete stale entry
		cacheFile.delete();
	}

	protected void cacheEntryEvicted(String queryHash , InternalAPIResponse entry) {
		try {
			if ( log.isDebugEnabled() ) {
				log.trace("cacheEntryEvicted(): hash="+queryHash);
			}
			persistCacheEntry( queryHash , entry );
		} 
		catch (XMLStreamException e) {
			log.error("cacheEntryEvicted(): Unable to store cache entry",e);
			throw new RuntimeException("Unable to store cache entry",e);
		}
	}

	File getFilenameForEntry(APIQuery query) {
		return getFilenameForEntry( query.getHashString() );
	}

	File getFilenameForEntry(String queryHash) {
		return new File( getCacheDirectory() , queryHash+".xml" );
	}
	
	protected CacheEntry loadCacheEntry(File inputFile) throws IOException, XMLStreamException {
		try {
			return internalLoadCacheEntry(inputFile);
		}
		catch (IOException e) {
			throw e;
		}
		catch(XMLStreamException e) 
		{
			if ( inputFile.delete() ) {
				log.error("loadCacheEntry(): Deleted corrupted cache file "+inputFile.getAbsolutePath());
			} else {
				log.error("loadCacheEntry(): Unable to delete corrupted cache file "+inputFile.getAbsolutePath());
			}
			throw e;
		}
	}

	protected CacheEntry internalLoadCacheEntry(File inputFile) throws IOException, XMLStreamException {

		log.trace("loadCacheEntries(): Loading cache entry "+
				inputFile.getAbsolutePath() );

		final XMLInputFactory inputFactory=
			XMLInputFactory.newInstance();

		final InputStream input;
		if ( this.cipherProvider == null ) {
			input=new FileInputStream( inputFile );
		} else {
			input=
				new CipherInputStream( new FileInputStream(inputFile) , 
						this.cipherProvider.createCipher( true ) );
		}

		final XMLStreamReader reader = inputFactory.createXMLStreamReader(input);
		final XMLStreamHelper helper = new XMLStreamHelper( reader );

		try {

			// skip to first start element
			if ( ! helper.readEmptyStartElement( "cacheEntry") ) {
				log.error("loadCacheEntry(): Corrupted cache file: "+inputFile);
				throw new RuntimeException("Corrupted cache file: "+inputFile);
			}

			/* <cacheEntry>
			 *   <query></query>
			 *   <serverTime></serverTime>
			 *   <cachedUntil></cachedUntil>
			 *   <timestamp></timestamp>
			 *   <payload></payload>
			 * </cacheEntry>
			 */

			helper.readStartElementWithValue( "query" );
			final APIQuery query = 
				new APIQuery( helper.tag.getContents() );

			helper.readStartElementWithValue( "serverTime" );
			final Date serverTime = parseDate( helper.tag.getContents() );
			helper.readEndElement( "serverTime" );

			helper.readStartElementWithValue( "cachedUntil" );
			final Date cachedUntil = parseDate( helper.tag.getContents() );
			helper.readEndElement( "cachedUntil" );				

			helper.readStartElementWithValue( "timestamp" );
			final Date timestamp = parseDate( helper.tag.getContents() );
			helper.readEndElement("timestamp" );

			helper.readStartElementWithValue( "payload" );
			final String payload = helper.tag.getContents();
			helper.readEndElement("payload" );

			final InternalAPIResponse entry =
				new InternalAPIResponse(payload , 
						timestamp,
						EveDate.fromServerTime( serverTime, systemClock ),
						EveDate.fromServerTime( cachedUntil , systemClock ) ); 

			final CacheEntry result = new CacheEntry( query , entry );
			helper.readEndElement("cacheEntry" );
			return result;

		} finally {
			reader.close();
		}
	}

	@Override
	protected void shutdownHook() {

		log.info("shutdownHook(): Persisting dirty cache entries.");
		long time1 = -System.currentTimeMillis();

		try {

			final int[] persistedCounter=new int[] {0};
			super.visitCache( new ICacheVisitor() {

				@Override
				public void visit(String apiQueryHashKey, InternalAPIResponse response) throws XMLStreamException {
					if ( persistCacheEntry(apiQueryHashKey, response) ) {
						persistedCounter[0]++;
					}
				}
			});

			time1+= System.currentTimeMillis();
			log.info("shutdownHook(): Persisted ("+persistedCounter[0]+
					" entries , "+time1+" milliseconds.");
		} 
		catch (Exception e) {
			log.error("shutdownHook(): Failed to persist cache",e);
		}

	}
	
	protected boolean isAlreadyOnDisk(File outputFile,String apiQueryHashKey, InternalAPIResponse response) 
	{
		if ( !outputFile.exists() ) 
		{
			return false;
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("persistCacheEntry(): Found existing on-disk cache entry for "+apiQueryHashKey);
		}

		try {

			final CacheEntry existingEntry = 
				loadCacheEntry( outputFile );

			// file might've been corrupted , loadCacheEntry() will delete the file and return NULL in this case
			if ( existingEntry != null &&
					existingEntry.response.getServerTime().compareTo( response.getServerTime() ) >= 0 ) 
			{
				if ( log.isTraceEnabled() ) {
					log.trace("isAlreadyOnDisk(): Found existing on-disk cache entry for "+apiQueryHashKey);
				}
				return true;
			}

			if ( log.isTraceEnabled() ) {
				log.trace("isAlreadyOnDisk(): Overwriting stale cache file "+outputFile.getAbsolutePath());
			}
		} 
		catch(Exception e) {
			log.error("isAlreadyOnDisk(): Overwriting corrupted cache file "+outputFile.getAbsolutePath() , e );
		}
		return false;
	}

	protected boolean persistCacheEntry(String apiQueryHashKey,InternalAPIResponse response) throws XMLStreamException {

		final File outputFile =
			getFilenameForEntry( apiQueryHashKey );

		if ( isAlreadyOnDisk( outputFile , apiQueryHashKey , response ) )
		{
			return false;
		}

		final XMLOutputFactory outputFactory=XMLOutputFactory.newInstance();

		try {

			if ( log.isTraceEnabled() ) {
				log.trace("visit(): Writing cache entry "+outputFile.getAbsolutePath());
			}

			final OutputStream out = createCacheFile( outputFile );

			XMLStreamWriter writer=null;
			try {
				writer= outputFactory.createXMLStreamWriter( out , FILE_ENCODING );
				writer.writeStartDocument(FILE_ENCODING ,"1.0");
				writeCacheEntry( writer , apiQueryHashKey , response );
			} finally {
				try {
					if ( writer != null ) {
						writer.flush();
						writer.close();
					}
				} finally {
					out.close();
				}
			}

		} catch (IOException e) {
			log.error("visit(): Failed to store cache entry "+outputFile,e);
		}

		return true;
	}

	protected DateFormat getDateFormat() {
		DateFormat result = DATE_FORMAT.get();
		if ( result == null ) {
			result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			DATE_FORMAT.set( result );
		}
		return result;
	}
	
	protected String toString(EveDate d) {
		if ( d == null ) {
			return "";
		}
		return getDateFormat().format( d.getServerTime() );
	}

	protected String toString(Date d) {
		if ( d == null ) {
			return "";
		}
		return getDateFormat().format( d );
	}

	protected Date parseDate(String s) {
		if ( s == null || s.trim().length() == 0 ) {
			return null;
		}
		try {
			return getDateFormat().parse( s );
		} catch (ParseException e) {
			throw new RuntimeException("Failed to parse date >"+s+"<",e);
		}
	}

	protected OutputStream createCacheFile(File outputFile) throws IOException, XMLStreamException {

		if ( cipherProvider != null ) {

			final Cipher algo =
				cipherProvider.createCipher( false );
			final OutputStream fileOut = new FileOutputStream( outputFile , false );
			return new BufferedOutputStream( new CipherOutputStream( fileOut , algo ) );
		} 

		return new BufferedOutputStream( new FileOutputStream( outputFile , false ) );
	}

	protected void writeCacheEntry(XMLStreamWriter writer , String queryHashCode, InternalAPIResponse response) throws IOException, XMLStreamException {

		writer.writeStartElement("cacheEntry");

		// element body
		writer.writeStartElement("query");
		writer.writeCharacters( queryHashCode );
		writer.writeEndElement();

		writer.writeStartElement("serverTime");
		writer.writeCharacters( toString( response.getServerTime() ) ); // server time
		writer.writeEndElement();

		writer.writeStartElement("cachedUntil");
		writer.writeCharacters( toString( response.getCachedUntilServerTime() ) ); // server time
		writer.writeEndElement();

		writer.writeStartElement("timestamp");
		writer.writeCharacters( toString( response.getTimestamp() ) ); // LOCAL time
		writer.writeEndElement();	

		writer.writeStartElement("payload");
		writer.writeCData( response.getPayload() );
		writer.writeEndElement();

		// end of element body

		writer.writeEndElement();
	}

	@Override
	public void setCacheOptions(Properties options)
	{
		super.setCacheOptions(options);
		if ( options.containsKey( OPTION_WRITE_IMMEDIATELY ) ) {
			final String value =
				options.getProperty(OPTION_WRITE_IMMEDIATELY,"false");
			if ( "true".equalsIgnoreCase( value.trim() ) ) {
				log.info("setCacheOptions(): Will immediately write cache entries to disk.");
				this.immediatelyWriteToDisk = true;
			} else if ( "false".equalsIgnoreCase( value.trim() ) ) {
				this.immediatelyWriteToDisk = false;
			}
		}
	}
	
	@Override
	public void put(APIQuery query, InternalAPIResponse response)
	{
		
		/*
		 * super.put() might call
		 * cacheEntryReplaced() which 
		 * deletes any existing cache file
		 * for this entry.
		 * 
		 * Make sure we call if BEFORE
		 * we write the new entry to disk
		 */
		super.put(query, response);
		
		if ( immediatelyWriteToDisk ) 
		{
			if ( log.isTraceEnabled() ) {
				log.trace("put(): Immediate-write mode enabled.");
			}
			try {
				persistCacheEntry( query.getHashString() , response );
			}
			catch (XMLStreamException e) {
				log.error("put(): Immediate write to disk failed",e);
			}
		}
	}
}
