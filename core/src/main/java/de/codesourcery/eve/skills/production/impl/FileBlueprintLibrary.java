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
package de.codesourcery.eve.skills.production.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.assets.IAssetManager;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.util.CsvHelper;

public class FileBlueprintLibrary extends InMemoryBlueprintLibrary implements InitializingBean,DisposableBean
{

	private static final Logger log = Logger.getLogger(FileBlueprintLibrary.class);

	private File inputFile;
	
	public FileBlueprintLibrary(IStaticDataModel dataModel,
			IAssetManager assetManager, IUserAccountStore userAccountStore) 
	{
		super(dataModel, assetManager, userAccountStore);
	}
	
	@Override
	protected CacheImpl createCache()
	{
		final CacheImpl result =
			new CacheImpl();
	
		populateFromDisk( result );
		
		return result;
	}
	
	protected void populateFromDisk(CacheImpl cache) {
		
		log.info("populateFromDisk(): Trying to read from "+inputFile.getAbsolutePath());
		
		if ( ! inputFile.exists()  || inputFile.length() == 0 ) {
			log.info("populateFromDisk(): File "+inputFile.getAbsolutePath()+" does not exist");
			return;
		}
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader( new FileReader( inputFile ) );
			String[] data=null;
			while ( ( data = CsvHelper.readCsvLine( reader ) ).length > 0 ) {
				cache.addBlueprint( blueprintFromArray( data ) );
			}
		}
		catch (FileNotFoundException e) {
			log.error("populateFromDisk(): Should not happen",e); // we already checked above...
		} 
		catch(Exception e) {
			log.error("populateFromDisk(): Failed to read blueprints from "+inputFile.getAbsolutePath() ,e); // we already checked above...
//			inputFile.delete();
			throw new RuntimeException("Failed to read blueprints from "+inputFile.getAbsolutePath(),e);
		}
		finally {
			if ( reader != null ) {
				try { reader.close(); } catch(IOException e) {}
			}
		}
	}
	
	/*
	 * CSV:
	 * 
	 * testchar;42;1;2;false
	 */
	public static final int COL_CHARACTER_ID = 0;
	public static final int COL_TYPE_ID = 1;
	public static final int COL_ME = 2;
	public static final int COL_PE = 3;
	public static final int COL_FOUND_IN_ASSETS = 4;
	
	public static final int COL_COUNT = 5;
	
	private BlueprintWithAttributesImpl blueprintFromArray(String[] data)
	{
		
		final CharacterID charId =
			new CharacterID( data[ COL_CHARACTER_ID ] );
		
		final Long typeId =
			new Long( data[ COL_TYPE_ID ] );
		
		final int me = Integer.parseInt( data[COL_ME ] );
		final int pe = Integer.parseInt( data[COL_PE ] );
		final boolean foundInAssets = Boolean.parseBoolean( data[COL_FOUND_IN_ASSETS] );
		
		return new BlueprintWithAttributesImpl (charId,typeId , me , pe , foundInAssets );
	}
	
	private String[] blueprintToArray(BlueprintWithAttributesImpl impl) {
		final String[] data = new String[ COL_COUNT ];
		
		data[ COL_CHARACTER_ID ] = impl.getOwningCharacterId().getValue();
		data[ COL_TYPE_ID ] = Long.toString( impl.getBlueprintTypeId() );
		data[ COL_ME ] = Integer.toString( impl.getMeLevel() );
		data[ COL_PE ] = Integer.toString( impl.getPeLevel() );
		data[ COL_FOUND_IN_ASSETS ] = Boolean.toString( impl.isFoundInAssets() );
		
		return data;
	}

	public void persist() {
		
		// write log statement just in case we dead-lock when
		// trying to aquire the exclusive lock
		log.info("persist(): Called."); 
		
		getCache().doWithExclusiveAccess( new IDoWithExclusiveAccess() {

			@Override
			public void doWhileLocked(CacheImpl cache)
			{
				
				log.info("persist(): Writing blueprint data to "+inputFile.getAbsolutePath());
				
				BufferedWriter writer = null;
				try {
					if ( ! inputFile.exists() ) {
						File parent = inputFile.getParentFile();
						if ( parent != null && ! parent.exists() ) {
							parent.mkdirs();
							log.error("persist(): Failed to create dirs in path "+parent.getAbsolutePath()); 
						}
					}
					writer = new BufferedWriter( new FileWriter( inputFile ) );
					for ( BlueprintWithAttributesImpl bp : cache.getAllBlueprints() ) 
					{
						CsvHelper.writeCsvLine( writer , blueprintToArray( bp ) );
					}
				}
				catch (IOException e) {
					log.error("persist(): Failed to persist blueprint library to "+inputFile.getAbsolutePath(),e);
					throw new RuntimeException("Failed to persist blueprint library to "+inputFile.getAbsolutePath(),e);
				}
				finally 
				{
					if ( writer != null ) {
						try { writer.close(); } catch (IOException e) { }
					}
				}
			}} );
	}

	public void setInputFile(File inputFile)
	{
		if ( inputFile == null ) {
			throw new IllegalArgumentException("inputFile cannot be NULL");
		}
		this.inputFile = inputFile;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if ( inputFile == null ) {
			throw new BeanInitializationException("inputFile cannot be NULL");
		}
	}

	@Override
	public void destroy() throws Exception
	{
		persist();
	}
}
