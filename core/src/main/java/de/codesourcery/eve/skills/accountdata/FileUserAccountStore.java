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
package de.codesourcery.eve.skills.accountdata;

import static de.codesourcery.eve.apiclient.utils.XMLParseHelper.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NullCipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.Credentials;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyType;
import de.codesourcery.eve.apiclient.utils.ICipherProvider;
import de.codesourcery.eve.skills.datamodel.Character;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.exceptions.NoSuchCharacterException;
import de.codesourcery.eve.skills.util.IInOutStreamProvider;
import de.codesourcery.eve.skills.util.IInputStreamProvider;
import de.codesourcery.eve.skills.util.IOutputStreamProvider;
import de.codesourcery.eve.skills.util.Misc;

/**
 * Stores user account data in a password-encrypted XML file.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class FileUserAccountStore extends AbstractUserAccountStore {

	private static final Logger log = Logger.getLogger(FileUserAccountStore.class);
	
	// guarded-by: userAccounts
	private final List<UserAccount> userAccounts =
		new ArrayList<UserAccount>();

	private volatile boolean accountsFetched = false;
	private de.codesourcery.eve.skills.util.IInOutStreamProvider inputProvider;

	private volatile ICipherProvider cipherProvider = new ICipherProvider() {

		@Override
		public Cipher createCipher(boolean decrypt) {
			return new NullCipher();
		}};
	
	/**
	 * Sets the cipher provider to be used for encryption/decryption
	 * of account data.
	 * 
	 * @param password the password to be used for reading/writing encrypted data or 
	 * <code>null<code> if data should not be encrypted.
	 */
	public void setCipherProvider(ICipherProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("cipher provider cannot be NULL");
		}
		
		synchronized( userAccounts ) {
			this.cipherProvider = provider;
		}
	}
	
	@Override
	public void reconcile(CharacterID characterId) 
	{
		if (characterId == null) {
			throw new IllegalArgumentException("characterId cannot be NULL");
		}
		
		log.info("reconcile(): character = "+characterId);
		
		synchronized( userAccounts ) {
			for ( UserAccount account : userAccounts ) {
				if ( account.containsCharacter( characterId ) ) {
					
					final APIResponse<ICharacter> response = 
						getAPIClient().getCharacter( 
							characterId , 
							account , 
							RequestOptions.DEFAULT
					);
					
					account.getCharacterByID( characterId ).reconcile( response.getPayload() );
					break;
				}
			}
		}
	}
	
	@Override
	public UserAccount getAccountByUserId(long userId) {
		
		assertUnlocked();
		
		synchronized( userAccounts ) {
			
			if ( ! accountsFetched ) {
				loadFile();
			}
			
			for ( UserAccount acct : userAccounts ) {
				if ( acct.getUserId() == userId ) {
					return acct;
				}
			}
		}
		throw new NoSuchElementException("No user account with ID "+userId);
	}
	
	@Override
	public void unlock() {
		
		log.debug("unlock(): called.");
		
		if ( ! isLocked() ) {
			return;
		}
		
		synchronized( userAccounts ) {
			if ( ! accountsFetched ) {
				loadFile();
			}
		}
		
		super.unlock();
	}

	@Override
	public List<UserAccount> getAccounts() {
		assertUnlocked();
		synchronized( userAccounts ) {
			
			if ( ! accountsFetched ) {
				loadFile();
			}
			return new ArrayList<UserAccount>( userAccounts );
		}
	}

	@Override
	public void storeAccount(UserAccount account) {

		if (account == null) {
			throw new IllegalArgumentException("account cannot be NULL");
		}

		assertUnlocked();

		synchronized( userAccounts ) {
			
			if ( ! accountsFetched ) {
				loadFile();
			}

			for ( Iterator<UserAccount> it = userAccounts.iterator() ; it.hasNext() ; ) {
				final UserAccount existing = it.next();
				if ( existing.getUserId() == account.getUserId() ) 
				{
					if ( existing == account ) {
						notifyAccountEdited( existing );
						return;
					}
					it.remove();
					notifyAccountAboutToBeRemoved( existing );
				}
			}

			userAccounts.add( account );
			notifyAccountAdded( account );			
		}

	}

	protected Cipher createCipher( boolean decrypt) {
		return cipherProvider.createCipher( decrypt );
	}
	
	protected IOutputStreamProvider encrypt(final IOutputStreamProvider provider) {
		
		final Cipher cipher =
			createCipher( false );
		
		if ( cipher == null ) {
			return provider;
		}
		
		return new IOutputStreamProvider() {

			public OutputStream createOutputStream() throws IOException {
				return new CipherOutputStream( provider.createOutputStream() , cipher  );
			}
		};
	}
	
	protected IInputStreamProvider decrypt(final IInputStreamProvider provider) {
		
		final Cipher cipher =
			createCipher( true );
		
		if ( cipher == null || cipher instanceof NullCipher) {
			return provider;
		}
		
		return new IInputStreamProvider() {

			@Override
			public InputStream createInputStream() throws IOException {
				return new CipherInputStream( provider.createInputStream() , cipher  );
			}};
	}
	
	public void initializeNewStore() throws IOException {
		if ( ! isLocked() ) {
			throw new IllegalStateException("Account store needs to be locked for initialization");
		}
		persist();
	}

	/*
	 * <useraccounts fileVersion="1">
	 *   <useraccount displayName="test account">
	 *     <credentials>
	 *       <userId>12345</userId>
	 *       <apikeys>
	 *         <apikey type="limited">deadbeef</apikey>
	 *         <apikey type="full">deadbeef</apikey>
	 *       </apikeys>
	 *     </credentials>
	 *     <characters>
	 *       <character characterId="123456" name="twink37" />
	 *       <character characterId="1234567" name="mainchar" />
	 *     </characters>
	 *   </useraccount>
	 * </useraccounts>
	 * 
	 */
	private void loadFile()  {

		log.debug("loadFile(): called.");
		
		accountsFetched = false;

		final Document document;
		try {
			
			final String xml =
				Misc.readFile( decrypt( inputProvider ) );
			
			document =
				parseXML(xml );
		} 
		catch (IOException e) {
			throw new RuntimeException("Failed to load user account data from "+inputProvider,e);
		}

		// check file version
		final Element rootNode =
			getChild( document, "useraccounts");

		final int fileVersion =
			getIntAttributeValue( rootNode , "fileVersion");
		if ( fileVersion != 1 ) {
			throw new RuntimeException("Unsupported file version "+fileVersion);
		}

		final List<UserAccount> tmpAccounts =
			new ArrayList<UserAccount>();

		// parse user accounts
		final List<Element> accountNodes = getChildNodes( rootNode , "useraccount");
		for ( Element accountNode : accountNodes ) {
			tmpAccounts.add( parseAccount(accountNode ) );
		}

		// copy tmp data
		synchronized( userAccounts ) {
			this.userAccounts.clear();
			this.userAccounts.addAll( tmpAccounts );
			accountsFetched = true;
		}
	}

	/*
	 *   <useraccount displayName="test account">
	 *     <credentials>
	 *       <userId>12345</userId>
	 *       <apikeys>
	 *         <apikey type="limited">deadbeef</apikey>
	 *         <apikey type="full">deadbeef</apikey>
	 *       </apikeys>
	 *     </credentials>
	 *     <characters>
	 *       <character characterId="123456" name="twink37" />
	 *       <character characterId="1234567" name="mainchar" />
	 *     </characters>
	 *   </useraccount>	 
	 */
	private UserAccount parseAccount(Element accountNode) {
		
		
		// assemble UserAccount object
		final String name =
			getAttributeValue( accountNode , "displayName" );

		// parse credentials
		final Element credentialsNode =
			getChild( accountNode , "credentials" );

		final Credentials credentials =
			new Credentials();
		
		final UserAccount result =
			new UserAccount( name , credentials );		

		final Element userIdNode =
			getChild( credentialsNode , "userId" , false );

		if ( userIdNode != null ) {

			final long userId =
				Long.parseLong( getNodeValue( userIdNode ) ); 

			credentials.setUserId( userId );

			final Element apiKeysNode =
				getChild( credentialsNode , "apikeys" , false );

			if ( apiKeysNode != null ) {
				final List<Element> apiKeyNodes =
					getChildNodes( apiKeysNode ,"apikey" );

				for ( Element apiKeyNode : apiKeyNodes ) {
					final String keyType =
						getAttributeValue(apiKeyNode , "type" );

					final String keyValue =
						getNodeValue( apiKeyNode );

					if ( "full".equals( keyType ) ) {
						credentials.setKey( APIKey.createFullAccessKey( keyValue ) );
					} else if ("limited".equals( keyType ) ) {
						credentials.setKey( APIKey.createLimitedKey( keyValue ) );
					} else {
						throw new RuntimeException("File contained invalid API key type '"+keyType+"'");
					}
				}
			}
		}

		// parse characters
		final Node parent =
			getChild( accountNode , "characters" , false);

		if ( parent != null ) {
			final List<Element> characterNodes = 
				getChildNodes( parent , "character" );

			for ( Element characterNode : characterNodes ) {
				result.addCharacter( parseCharacter( characterNode ));
			}
		}

		return result;
	}

	/*
	 * 	<character characterId="123456" name="twink37" />
	 */
	private ICharacter parseCharacter(Element characterNode) {

		final Character result = new Character();
		result.setCharacterId( new CharacterID( getAttributeValue( characterNode , "characterId" ) ) );
		result.setName( getAttributeValue( characterNode , "name" ) );
		
		return result;
	}

	public void setInputProvider(IInOutStreamProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider cannot be NULL");
		}
		this.inputProvider = provider;
	}

	@Override
	public void closeHook() {

		synchronized( userAccounts ) {
			userAccounts.clear();
			accountsFetched = false;
		}
	}
	
	/*
	 * <useraccounts fileVersion="1">
	 *   <useraccount displayName="test account">
	 *     <credentials>
	 *       <userId>12345</userId>
	 *       <apikeys>
	 *         <apikey type="limited">deadbeef</apikey>
	 *         <apikey type="full">deadbeef</apikey>
	 *       </apikeys>
	 *     </credentials>
	 *     <characters>
	 *       <character characterId="123456" name="twink37" />
	 *       <character characterId="1234567" name="mainchar" />
	 *     </characters>
	 *   </useraccount>
	 *   ...
	 * </useraccounts>	 	 
	 */
	public void persist() throws IOException {

		if ( this.inputProvider == null ) {
			throw new IllegalArgumentException("output stream provider cannot be NULL");
		}
		
		synchronized( userAccounts ) {
			
			final Document doc =
				createDocument();
			
			final OutputStream out = encrypt( this.inputProvider ).createOutputStream();
			try {
				writeXML( doc , out);
			} 
			catch (TransformerException e) {
				throw new RuntimeException(e);
			} finally {
				out.close();
			}
			
		}
	}
	
	private Document createDocument() {

		final DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder;
		try {
			docBuilder = fac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		final Document document =
			docBuilder.newDocument();

		final Element rootNode =
			document.createElement("useraccounts");

		rootNode.setAttribute("fileVersion" , "1" );
		document.appendChild( rootNode );

		for ( UserAccount account : this.userAccounts ) {

			final Element accountNode =
				document.createElement("useraccount");

			accountNode.setAttribute( "displayName" , account.getName() );

			rootNode.appendChild( accountNode );
			
			// add credentials
			final Element credentialsNode =
				document.createElement("credentials");
			
			accountNode.appendChild( credentialsNode );
			
			final long userId =
				account.getCredentials().getUserId() ;
			
			if ( userId != 0 ) {
				final Element userIdNode =
					document.createElement("userId");
				userIdNode.setTextContent( Long.toString( userId ) );
				credentialsNode.appendChild( userIdNode );
			}

			final List<APIKey> keys =
				account.getCredentials().getKeys();

			if ( ! keys.isEmpty() ) {

				final Element keysNode =
					document.createElement( "apikeys" );

				credentialsNode.appendChild( keysNode );
				
				for ( APIKey key : keys ) {

					final Element keyNode =
						document.createElement( "apikey" );

					keysNode.appendChild( keyNode );

					if ( key.hasType( KeyType.LIMITED_KEY ) ) {
						keyNode.setAttribute( "type" , "limited" );
					} else if ( key.hasType( KeyType.FULL_ACCESS ) ) {
						keyNode.setAttribute( "type" , "full" );
					} else {
						throw new RuntimeException("Unhandled key type "+key);
					}

					keyNode.setTextContent( key.getValue() );
				}
			}
			
			// append characters
			final Element charactersNode =
				document.createElement("characters");
			
			accountNode.appendChild( charactersNode );
			for ( ICharacter c : account.getCharacters() ) {
				final Element characterNode =
					document.createElement("character");
				
				charactersNode.appendChild( characterNode );
				
				characterNode.setAttribute("characterId" , c.getCharacterId().getValue() );
				characterNode.setAttribute( "name" , c.getName() );
			}
		}
		return document;
	}

	@Override
	public void deleteAccount(UserAccount account) {

		assertUnlocked();
		
		if (account == null) {
			throw new IllegalArgumentException("account cannot be NULL");
		}
		
		synchronized ( userAccounts ) {
			for ( Iterator<UserAccount> it = this.userAccounts.iterator() ; it.hasNext() ; ) {
				final UserAccount acct = it.next();
				if ( acct.getUserId() == account.getUserId() ) {
					notifyAccountAboutToBeRemoved( acct );
					it.remove();
					return;
				}
			}
		}
	}

	@Override
	public UserAccount getAccountByCharacterID(CharacterID id)
			throws NoSuchCharacterException 
	{
		
		if ( id == null ) {
			throw new IllegalArgumentException("Character ID cannot be NULL");
		}
		
		assertUnlocked();
		
		synchronized( userAccounts ) {
			
			for ( UserAccount acct : userAccounts ) {
				if ( acct.containsCharacter( id ) ) {
					return acct;
				} 
			}
		}
		
		throw new NoSuchCharacterException("Found no user account that has a character with ID "+id,id);
	}

}
