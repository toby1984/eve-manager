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
package de.codesourcery.eve.skills.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.skills.accountdata.FileUserAccountStore;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.ui.components.impl.PasswordComponent.Mode;
import de.codesourcery.eve.skills.ui.config.AppConfig;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.util.FileInputStreamProvider;
import de.codesourcery.eve.skills.util.FileOutputStreamProvider;
import de.codesourcery.eve.skills.util.IInOutStreamProvider;

public class UserAccountStoreFactory {
	
    private static final Logger log = Logger.getLogger( UserAccountStoreFactory.class );
    
	private final IAPIClient apiClient;
	private final IAppConfigProvider configProvider;
	
	private File inputFile;
	
	public void setInputFile(File inputFile)
	{
		this.inputFile = inputFile;
	}

	public UserAccountStoreFactory(IAppConfigProvider appConfigProvider,
			IAPIClient client) 
	{
		this.configProvider = appConfigProvider;
		this.apiClient = client;
	}

	public IUserAccountStore createInstance() throws IOException {
		
		// determine file location
		File userAccountsFile = inputFile;
		if ( userAccountsFile == null || ! userAccountsFile.exists() ) {
			if ( ! configProvider.getAppConfig().hasProperty( AppConfig.PROP_USERACCOUNTS_FILE ) ) {
				userAccountsFile = new File("data/useraccounts.xml" );
				configProvider.getAppConfig().setUserAccountsFile( userAccountsFile  );
				configProvider.save();
			} else {
				userAccountsFile = configProvider.getAppConfig().getUserAccountsFile();
			}
		}

		final FileUserAccountStore store =
			new FileUserAccountStore();
		
		store.setAPIClient( this.apiClient );
		
		final File anonDummyFile = userAccountsFile; // ...needs to be final so compiler doesn't complain...
		
		// make sure directory exists
		final File path = anonDummyFile.getParentFile();
		if ( ! path.exists() ) 
		{
			log.warn("createInstance(): Directory "+path.getAbsolutePath()+" does not exist, creating directory");
			if ( ! path.mkdirs() ) {
				throw new IOException("Failed to create directory "+path.getAbsolutePath());
			}
		}
		store.setInputProvider(
				new IInOutStreamProvider() {

					@Override
					public InputStream createInputStream() throws IOException {
						return new FileInputStreamProvider( anonDummyFile ).createInputStream();
					}

					@Override
					public OutputStream createOutputStream() throws IOException {
						return new FileOutputStreamProvider(anonDummyFile , true ).createOutputStream();
					}
				} 
		);

		Mode mode = Mode.QUERY_PASSWORD;
		
		final boolean fileExists = userAccountsFile.exists();
		if ( ! fileExists ) {
			mode = Mode.SET_PASSWORD;
		}

		store.setCipherProvider( new DefaultCipherProvider( mode , this.configProvider ) ); 

		if ( ! fileExists ) {
			System.out.println("WARNING: Initializing account store "+userAccountsFile.getAbsolutePath() );
			store.initializeNewStore();
		}

		System.out.println("Loading user account data from "+userAccountsFile.getAbsolutePath() );

		store.unlock();
		return store;
	}

	public IAPIClient getApiClient() {
		return apiClient;
	}
	
}
