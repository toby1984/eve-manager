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
package de.codesourcery.eve.skills.ui.components.impl;

import java.awt.Desktop;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.Credentials;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyType;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;

public class CreateUserAccountComponent extends AbstractEditorComponent {

	private static final Logger log = Logger
			.getLogger(CreateUserAccountComponent.class);
	
	public static final String APIKEY_URL_HINT= "http://myeve.eve-online.com/api/default.asp";
	
	private final JTextField userId =
		new JTextField();

	private final JTextField limitedApiKey =
		new JTextField();

	private final JTextField fullApiKey =
		new JTextField();

	private final JTextField accountName =
		new JTextField();

	private final IUserAccountStore store;

	@Resource(name="api-client")
	private IAPIClient apiClient;
	
	private final UserAccount existingAccount;
	
	public CreateUserAccountComponent(IUserAccountStore store) {
		this.store = store;
		existingAccount = null;
	}
	
	public CreateUserAccountComponent(IUserAccountStore store,UserAccount existing) {
		
		this.store = store;
		this.existingAccount = existing;
		
		userId.setText( ""+existing.getUserId() );
		accountName.setText( existing.getName() );
		
		final Credentials cred =
			existing.getCredentials();
		
		if ( cred.hasKey( KeyType.FULL_ACCESS ) ) {
			fullApiKey.setText( existing.getCredentials().getKeyWithType( KeyType.FULL_ACCESS ).getValue() );
		}
		
		if ( cred.hasKey( KeyType.LIMITED_KEY ) ) {
			limitedApiKey.setText( existing.getCredentials().getKeyWithType( KeyType.LIMITED_KEY ).getValue() );
		}
	}	

	@Override
	protected JPanel createPanelHook() {

		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );

		userId.setColumns( 25 );
		limitedApiKey.setColumns( 25 );
		fullApiKey.setColumns( 25 );
		accountName.setColumns( 25 );

		final String hint = "<HTML><BODY>Hint: Visit <A HREF=\""+APIKEY_URL_HINT+"\">"+
		APIKEY_URL_HINT+"</A> to get your API key.";
//		final String hint = "Hint: Visit "+APIKEY_URL_HINT+" to get your API key.";
		
		JEditorPane pane =
			new JEditorPane();
		pane.setContentType("text/html");
		pane.setEditable( false );
		pane.setText( hint );
		pane.addHyperlinkListener( new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				
				if ( e.getEventType() != EventType.ACTIVATED ) {
					return;
				}
				
				if ( Desktop.isDesktopSupported() ) {
					try {
						Desktop.getDesktop().browse( e.getURL().toURI() );
					} catch (Exception e1) {
						log.error("caught",e1);
					}
				}
			}
		});

		int y = 0;
		result.add ( pane , constraints(0,y++).width(2).resizeHorizontally().end() );

		result.add( new JLabel("Account display name") , constraints(0,y).end() );
		result.add( accountName , constraints( 1 , y++ ).end() );
		
		result.add( new JLabel("User ID") , constraints(0,y).end() );
		result.add( userId , constraints(1,y++).end() );
		
		result.add( new JLabel("Limited API key") , constraints(0,y).end() );
		result.add( limitedApiKey , constraints(1,y++).end() );		
		
		result.add( new JLabel("Full API key") , constraints(0,y).end() );
		result.add( fullApiKey , constraints(1,y++).end() );			
		
		return result;
	}

	@Override
	public boolean isModal() {
		return true;
	}

	@Override
	public String getTitle() {
		return "Add user account";
	}
	
	@Override
	protected boolean hasValidInput()
	{
		if ( isBlank( accountName ) ) {
			displayError("You need to enter an account name");
			return false;
		}
		
		UserAccount existing;
		try {
			existing =
				store.getAccountByDisplayName( accountName.getText() );
		} catch(NoSuchElementException e) {
			existing = null;
		}
		
		if (  existing != null && 
			  existing != this.existingAccount ) 
		{
			displayError("This account name is already in use");
			return false;
		}
		
		if ( isBlank( userId ) ) {
			displayError("You need to enter a user ID");
			return false;
		}
		
		try {
			Long.parseLong( userId.getText() );
		} 
		catch(Exception e) {
			displayError("user ID needs to be a number");
			return false;
		}
		
		if ( isBlank( limitedApiKey ) && isBlank( fullApiKey ) ) {
			displayError("You need to enter at least one API key");
			return false;
		}
		
		return true;
	}

	@Override
	protected void okButtonClickedHook() {

		// verify against API
		final Credentials creds = new Credentials( getUserId() );
		creds.setKeys( getKeys() );
		
		try {
			apiClient.getAvailableCharacters( creds , RequestOptions.DEFAULT );
			displayInfo("Credentials are valid.");
		} catch (Exception e) {
			displayError( "Validating credentials failed: "+e.getMessage() );
			return;
		}
				
	}

	public String getAccountName() {
		return this.accountName.getText();
	}
	
	public APIKey[] getKeys() {
		
		final List<APIKey> keys =
			new ArrayList<APIKey>();
		
		if ( hasLimitedApiKey() ) {
			keys.add( getLimitedApiKey() );
		}
		if ( hasFullAccessApiKey() ) {
			keys.add( getFullApiKey() );
		}
		
		return keys.toArray(new APIKey[ keys.size() ] );
	}
	
	public long getUserId() {
		return Long.parseLong( userId.getText() );
	}
	
	public boolean hasLimitedApiKey() {
		return ! isBlank( limitedApiKey );
	}
	
	public boolean hasFullAccessApiKey() {
		return ! isBlank( fullApiKey );
	}
	
	public APIKey getLimitedApiKey() {
		return APIKey.createLimitedKey( limitedApiKey.getText() );
	}
	
	public APIKey getFullApiKey() {
		return APIKey.createFullAccessKey( fullApiKey.getText() );
	}

	@Override
	protected JButton createCancelButton() {
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton() {
		return new JButton("Ok");
	}

}
