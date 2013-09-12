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

import java.awt.Window;
import java.util.Arrays;

import javax.crypto.Cipher;

import de.codesourcery.eve.apiclient.utils.ICipherProvider;
import de.codesourcery.eve.apiclient.utils.IPasswordProvider;
import de.codesourcery.eve.apiclient.utils.PasswordCipherProvider;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.impl.PasswordComponent;
import de.codesourcery.eve.skills.ui.config.AppConfig;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;

public class DefaultCipherProvider implements ICipherProvider , IPasswordProvider {

	private final PasswordComponent.Mode mode;
	private final IAppConfigProvider appConfigProvider;
	
	private boolean passwordQueried = false;
	private final PasswordCipherProvider cipherProvider =
		new PasswordCipherProvider( this );
	
	public DefaultCipherProvider(PasswordComponent.Mode mode , IAppConfigProvider appConfigProvider) {
		if (appConfigProvider == null) {
			throw new IllegalArgumentException(
					"appConfigProvider cannot be NULL");
		}
		this.mode=mode;
		this.appConfigProvider = appConfigProvider;
	}
	
	private char[] enteredPassword;
	
	@Override
	public synchronized char[] getPassword() {
		
		if ( enteredPassword != null || passwordQueried ) {
			return enteredPassword;
		}
		// try to get password from app config first
		if ( appConfigProvider.getAppConfig().hasUserAccountStorePassword() ) {
			enteredPassword = appConfigProvider.getAppConfig().getUserAccountStorePassword();
			return enteredPassword;
		}
		
		// query user
		final String title;
		final String message;
		if ( mode ==  PasswordComponent.Mode.QUERY_PASSWORD || passwordQueried ) {
			title = "Password required";
			message = "Please enter the password for accessing your stored API keys";
		} else if ( mode == PasswordComponent.Mode.SET_PASSWORD ) {
			title = "Set password";
			message = "Please choose a password for protecting your API keys";
		} else {
			throw new RuntimeException("Internal error, unhandled mode "+mode);
		}
		
		final PasswordComponent comp =
			new PasswordComponent( title, message,mode);
		
		final Window window =
			ComponentWrapper.wrapComponent( comp );
	
		window.setVisible(true);
		
		passwordQueried = true;
		if ( ! comp.wasCancelled() ) {
			
			final char[] oldPassword =
				appConfigProvider.getAppConfig().getUserAccountStorePassword();
			
			enteredPassword = comp.getPassword1();

			final boolean passwordChanged =
				! Arrays.equals( oldPassword , enteredPassword );
			
			final char[] newPassword;
			if ( comp.isSavePassword() && passwordChanged ) {
				newPassword = enteredPassword;
			} else {
				newPassword = null;
			}
			appConfigProvider.getAppConfig().setUserAccountStorePassword( newPassword );
			if ( passwordChanged ) {
				appConfigProvider.appConfigChanged( AppConfig.PROP_USERACCOUNTSTORE_PASSWORD );
			}
		}
		return enteredPassword;
	}

	@Override
	public Cipher createCipher(boolean decrypt) {
		return cipherProvider.createCipher( decrypt );
	}

}
