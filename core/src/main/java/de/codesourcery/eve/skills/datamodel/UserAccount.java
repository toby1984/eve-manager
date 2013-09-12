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
package de.codesourcery.eve.skills.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.ICredentialsProvider;
import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.Credentials;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyRole;
import de.codesourcery.eve.apiclient.exceptions.MissingAPIKeyException;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.exceptions.NoSuchCharacterException;

/**
 * An EVE Online(tm) user account.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class UserAccount implements ICredentialsProvider {
	
	private static final Logger log = Logger.getLogger(UserAccount.class);

	private Map<CharacterID,ICharacter> characters =
		new HashMap<CharacterID, ICharacter>();
	
	private String displayName;
	private final Credentials credentials;
	
	@Override
	public String toString() {
		return "UserAccount[ display_name="+displayName+" , user ID= "+credentials.getUserId()+" ]";
	}
	
	/**
	 * Create instance.
	 * 
	 * @param credentials Credentials to use for this account
	 * @throws IllegalArgumentException on <code>null</code> <code>credentials</code>
	 */
	public UserAccount(String name , Credentials credentials) {
		if (credentials == null) {
			throw new IllegalArgumentException("credentials cannot be NULL");
		}
		this.credentials = credentials;
		setName( name );
	}
	
	public boolean containsCharacter(IBaseCharacter character) {
		
		if (character == null) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		
		for ( ICharacter c : characters.values() ) {
			if ( c.getCharacterId().equals( character.getCharacterId() ) ) {
				return true;
			}
		}
		return false;
	}
	
	public void syncWithServer(IUserAccountStore accountStore, Collection<IBaseCharacter> fromServer) {
		
		if ( log.isDebugEnabled() ) {
			log.debug("syncWithServer(): Syncing characters of account "+this+" with server");
		}
		
		final List<ICharacter> existing = new ArrayList<ICharacter>( getCharacters() );
		
		for ( ICharacter c : existing ) {
			if ( ! containsCharacter( fromServer , c ) ) {
				if ( log.isDebugEnabled() ) {
					log.debug("syncWithServer(): Removing character "+c);
				}
				accountStore.notifyCharacterAboutToBeRemoved( this , c );
				removeCharacter( c.getCharacterId() );
			}
		}
		
		for ( IBaseCharacter c : fromServer ) {
			if ( containsCharacter( c ) ) {
				continue;
			}
			
			final Character newChar = new Character( c );
			if ( log.isDebugEnabled() ) {
				log.debug("syncWithServer(): Adding character "+newChar);
			}
			addCharacter( newChar );
			accountStore.notifyCharacterAdded( this , newChar );
		}
		
		if ( log.isDebugEnabled() ) {
			log.debug("syncWithServer(): Finished syncing : "+this);
		}
	}
	
	private boolean containsCharacter(Collection<IBaseCharacter> chars , 
			IBaseCharacter character) {
		
		if (character == null) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		
		for ( IBaseCharacter c : chars  ) {
			if ( c.getCharacterId().equals( character.getCharacterId() ) ) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Add a character to this account.
	 * 
	 * @param c
	 * @throws RuntimeException if a character with the same character ID was already added
	 */
	public void addCharacter(ICharacter c) {
		
		if (c == null) {
			throw new IllegalArgumentException(
					"character cannot be NULL");
		}
		
		if (c.getCharacterId() == null) {
			throw new IllegalArgumentException("character ID cannot be NULL");
		}
		
		if ( characters.get( c.getCharacterId() ) != null ) {
			throw new RuntimeException("Duplicate character with ID "+c.getCharacterId() );
		}
		
		this.characters.put( c.getCharacterId() , c );
	}
	
	public boolean containsCharacter(CharacterID id) {
		
		if (id == null) {
			throw new IllegalArgumentException("character id cannot be NULL");
		}
		
		return characters.get( id ) != null;
	}
	
	/**
	 * Get character by character ID.
	 * 
	 * @param id
	 * @return
	 * @throws NoSuchCharacterException
	 */
	public ICharacter getCharacterByID(CharacterID id) {
		if ( id == null ) {
			throw new IllegalArgumentException(" character ID cannot be NULL");
		}
		
		final ICharacter result = characters.get( id );
		if ( result != null ) {
			return result;
		}
		throw new NoSuchCharacterException( id );
	}
	/**
	 * Returns the characters for this account.
	 * 
	 * @return
	 */
	public List<ICharacter> getCharacters() {
		return new ArrayList<ICharacter>( characters.values() );
	}

	/**
	 * Returns the credentials for this account.
	 * 
	 */
	public Credentials getCredentials() {
		return credentials;
	}
	
	public void removeCharacter(CharacterID id) {
		if (id == null) {
			throw new IllegalArgumentException("character ID cannot be NULL");
		}
		this.characters.remove( id );
	}

	public void setName(String name) {
		
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("account displayName cannot be blank / NULL");
		}
		this.displayName = name;
	}

	public String getName() {
		return displayName;
	}

	@Override
	public APIKey getKeyForRole(KeyRole role) throws MissingAPIKeyException {
		return credentials.getKeyForRole( role );
	}

	@Override
	public long getUserId() {
		return credentials.getUserId();
	}

	public boolean hasCharacter(CharacterID charID) {
		if ( charID == null) {
			throw new IllegalArgumentException("character ID cannot be NULL");
		}
		return this.characters.containsKey( charID );
	}

}
