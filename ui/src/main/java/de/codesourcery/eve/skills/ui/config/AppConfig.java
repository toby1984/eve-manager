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
package de.codesourcery.eve.skills.ui.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.ILocation;

import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.ui.exceptions.ConfigurationException;
import de.codesourcery.eve.skills.ui.utils.IPersistentDialog;

/**
 * Application config.
 * 
 * Note that clients must call {@link IAppConfigProvider#appConfigChanged()}
 * after they're done with their modifications.
 * @author tobias.gierke@code-sourcery.de
 */
public class AppConfig extends HashMap<String,String> {
	
	private static final Logger log = Logger.getLogger(AppConfig.class);

	// user-editable properties
	public static final String  PROP_CLIENT_RETRIEVAL_STRATEGY = "client.retrievalstrategy";
	public static final String  PROP_USERACCOUNTSTORE_PASSWORD = "useraccountstore.password";
	public static final String  PROP_DEFAULT_REGION = "default.region";
	public static final String  PROP_EVE_CENTRAL_ENABLED = "eve_central.enabled";
	
	// full name of propery: <PREFIX>.<Character ID>
	public static final String  PROP_CHARACTER_DEFAULT_REFINING_STATION_PREFIX = "refining.station";
	
	// properties managed by the application itself 
	public static final String  PROP_USERACCOUNTS_FILE = "useraccounts.file";
	public static final String  PROP_CACHE_DIR = "cache.dir";
	public static final String  PROP_LAST_USED_CHARACTER= "last.character";
	public static final String  PROP_LAST_MARKETLOG_IMPORT_DIRECTORY = "last.marketlog.importdir";
	
	/** 
	 * Dummy property for use in change events, this is NOT
	 * the actual property used for storing the information !!
	 * 
	 * Properties are currently named
	 * <code>PROP_ALL_DIALOGS_REENABLED+{@link IPersistentDialog#getId()}</code>.
	 */
	public static final String PROP_ALL_DIALOGS_REENABLED = "dialogs_reenabled";
	
	private final IAppConfigProvider provider;
	private File file;
	
	public AppConfig(IAppConfigProvider provider , File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException("file cannot be NULL");
		}
		
		if ( provider == null ) {
			throw new IllegalArgumentException("provider cannot be NULL");
		}
		
		this.provider = provider;
		this.file = file;
		
		setDefaults();
		
		if ( file.exists() ) {
			load();
		} 
	}
	
	public boolean hasDefaultRegion() {
		return hasProperty( PROP_DEFAULT_REGION ); 
 	}
	
	/**
	 * Returns the region used to query price information.
	 * 
	 * @param callback invoked when no default region is set yet
	 * @return
	 */
	public Region getDefaultRegion(IRegionQueryCallback callback) {
		
		if ( ! hasDefaultRegion() ) {
			Region result =
				callback.getRegion("Please choose a default region for managing price information");
			if ( result == null ) {
				return null;
			}
			setDefaultRegion( result );
			return result;
		}
		
		final String id = getProperty( PROP_DEFAULT_REGION );
		return callback.getRegionById( Long.parseLong( id ) );
	}
	
	public void setDefaultRegion(Region region) {
		if (region == null) {
			throw new IllegalArgumentException("region cannot be NULL");
		}
		setProperty( PROP_DEFAULT_REGION, Long.toString( region.getID() ) );
	}
	
	public void setLastUsedCharacter(IBaseCharacter c) {
		if ( log.isTraceEnabled() ) {
			log.debug("setLastUsedCharacter(): "+c.getName()+" [ "+c.getCharacterId().getValue()+" ]");
		}
		setProperty( PROP_LAST_USED_CHARACTER , c.getCharacterId().getValue() );
	}
	
	public void setUserAccountStorePassword(char[] password) {
		if ( password == null ) {
			log.info("setUserAccountStorePassword(): Password cleared.");
			remove( PROP_USERACCOUNTSTORE_PASSWORD );
		} else {
			setProperty( PROP_USERACCOUNTSTORE_PASSWORD , new String( password ) );
			log.info("setUserAccountStorePassword(): Password stored.");
		}
	}
	
	public boolean hasUserAccountStorePassword() {
		return hasProperty( PROP_USERACCOUNTSTORE_PASSWORD );
	}
	
	public char[] getUserAccountStorePassword() {
		if ( ! hasUserAccountStorePassword() ) {
			return null;
		}
		return getProperty( PROP_USERACCOUNTSTORE_PASSWORD ).toCharArray();
	}
	
	public CharacterID getLastUsedCharacter() {
		
		CharacterID result = null;
		if ( hasProperty( PROP_LAST_USED_CHARACTER ) ) {
			result =
				new CharacterID( getProperty( PROP_LAST_USED_CHARACTER ) );
		}
		
		if ( log.isTraceEnabled() ) {
			log.debug("getLastUsedCharacter(): "+result);
		}		
		return result;
	}

	protected void setDefaults() {
		setProperty( PROP_CLIENT_RETRIEVAL_STRATEGY , 
				RequestOptions.DataRetrievalStrategy.DEFAULT.name() );
	}
	
	public RequestOptions.DataRetrievalStrategy getClientRetrievalStrategy() {
		final String value = getProperty( PROP_CLIENT_RETRIEVAL_STRATEGY );
		return RequestOptions.DataRetrievalStrategy.valueOf( value );
	}
	
	public void setClientRetrievalStrategy(RequestOptions.DataRetrievalStrategy strategy) {
		setProperty( PROP_CLIENT_RETRIEVAL_STRATEGY , strategy.name() );
	}
	
	public void setUserAccountsFile(File file) {
		setProperty(PROP_USERACCOUNTS_FILE , file.getAbsolutePath() );
	}
	
	public File getUserAccountsFile() {
		return new File( getProperty(PROP_USERACCOUNTS_FILE ) );
	}
	
	public void setCacheDir(File cacheDir) {
		setProperty( PROP_CACHE_DIR , cacheDir.getAbsolutePath());
	}

	public File getCacheDir() {
		return new File( getProperty(PROP_CACHE_DIR ) );
	}
	
	// ===============================================
	
	public void load() throws IOException {
		
		log.debug("load(): Loading app config from "+file);
		
		if ( file.exists() ) {
			final Properties props = 
				new Properties();
			final Reader instream = new FileReader( file );
			try {
				props.load( instream );
				clear();
				setDefaults();
				for ( String key : props.stringPropertyNames() ) {
					put( key , (String) props.get( key ) );
				}
			} finally {
				instream.close();
			}
		} 
	}
	
	public void save() throws IOException {
		
		log.debug("save(): Saving app config to "+file);
		
		final Properties props = 
			new Properties();
		
		for ( Map.Entry<String,String> e : this.entrySet() ) {
			props.setProperty( e.getKey() , e.getValue() );
		}
		
		final Writer out = new FileWriter( file , false );
		try {
			props.store( out , "Automatically generated, DO NOT EDIT.");
		} finally {
			out.close();
		} 
	}
	
	public boolean hasProperty(String name) {
		return containsKey( name );
	}
	
	public String getProperty(String name) {
		final String value =
			get( name );
		
		if ( value == null ) {
			throw new ConfigurationException("Missing config property '"+name+"'");
		}
		return value;
	}
	
	
	public void setDefaultRefiningStationId(IBaseCharacter character , Long defaultRefiningStationId)
	{
		final String key =
			PROP_CHARACTER_DEFAULT_REFINING_STATION_PREFIX+"."+character.getCharacterId().getValue();
		
		if ( defaultRefiningStationId != null ) {
			setProperty( key , Long.toString( defaultRefiningStationId ) );
		} else {
			remove( key );
		}
	}

	/**
	 * Returns this character's default station
	 * when it comes to refining.
	 * @return station ID or <code>null</code> if no default station has been set yet.
	 */
	public Long getDefaultRefiningStationId(IBaseCharacter character)
	{
		final String key =
			PROP_CHARACTER_DEFAULT_REFINING_STATION_PREFIX+"."+character.getCharacterId().getValue();
		
		if ( ! hasProperty( key ) ) {
			return null;
		}
		return Long.parseLong( getProperty( key ) );
	}
	
	private void setProperty(String name,long value) {
		setProperty( name , Long.toString( value ) );
	}	
	
	private void setProperty(String name,boolean value) {
		setProperty( name , value ? "true" : "false" );
	}
	
	private void setProperty(String name,String value) {
		String oldValue = get( name );
		put( name , value );
		if ( ! StringUtils.equals( oldValue , value ) ) {
			provider.appConfigChanged( name );
		}
	}

	public void setLastMarketLogImportDirectory(File lastMarketLogImportDirectory) {
		setProperty( PROP_LAST_MARKETLOG_IMPORT_DIRECTORY , 
				lastMarketLogImportDirectory.getAbsolutePath() );
	}
	
	public boolean hasLastMarketLogImportDirectory() {
		return hasProperty( PROP_LAST_MARKETLOG_IMPORT_DIRECTORY );
	}

	public File getLastMarketLogImportDirectory() {
		return new File( getProperty( PROP_LAST_MARKETLOG_IMPORT_DIRECTORY ) );
	}
	
	public static final String DIALOG_SETTING_PREFIX = "dialog.";

	protected long getLongProperty(String name) {
		final String value = getProperty( name );
		
		try {
			return  Long.parseLong( value );
		} catch(Exception e) {
			// ok
		}
		throw new ConfigurationException("App config contains value '"+value+
					"' for Long property '"+name+"'");
	}
	
	protected boolean getBooleanProperty(String name) {
		final String value = getProperty( name );
		
		if ( "true".equalsIgnoreCase( value ) ) {
			return true;
		} else if ( "false".equalsIgnoreCase( value ) ) {
			return false;
		} else {
			throw new ConfigurationException("App config contains invalid value '"+value+
					"' for boolean property '"+name+"'");
		}
	}
	
	public boolean isDialogDisabledByUser(String id) {
		
		
		if (StringUtils.isBlank(id))
		{
			throw new IllegalArgumentException("id cannot be blank.");
		}
		
		final String propName = DIALOG_SETTING_PREFIX+id;
		if ( hasProperty( propName ) ) {
			return getBooleanProperty( propName );
		}
		return false;
	}
	
	public boolean isEveCentralEnabled() {
		if ( hasProperty( PROP_EVE_CENTRAL_ENABLED ) ) {
			return getBooleanProperty( PROP_EVE_CENTRAL_ENABLED );
		}
		return true;
	}
	
	public void setEveCentralEnabled(boolean yesNo) {
		setProperty( PROP_EVE_CENTRAL_ENABLED , yesNo );
	}

	public void forgetDialogSettings(String id) {
		
		if (StringUtils.isBlank(id))
		{
			throw new IllegalArgumentException("id cannot be blank.");
		}
		
		remove( DIALOG_SETTING_PREFIX+id );
	}

	@Override
	public String remove(Object key)
	{
		final String oldValue =
			super.remove(key);
		
		if ( oldValue != null ) {
			provider.appConfigChanged( (String) key );
		}
		return oldValue;
	}
	
	public void rememberDialogSettings(String id, boolean disabledByUser) {
		
		
		if ( StringUtils.isBlank(id) ) {
			throw new IllegalArgumentException(
					"id cannot be blank.");
		}
		
		setProperty( DIALOG_SETTING_PREFIX+id , Boolean.toString( disabledByUser ) );
	}

	public void reenableAllDialogs() {

		final Set<String> names = 
			new HashSet<String>( keySet() );
		
		for ( String s : names ) {
			super.remove( s );
		}
		
		if ( ! names.isEmpty() ) {
			provider.appConfigChanged( PROP_ALL_DIALOGS_REENABLED );
		}
	}

}
