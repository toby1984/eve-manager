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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.exceptions.NoSuchCharacterException;

/**
 * A user account store that holds all data in memory.
 * 
 * This is a thread-safe class but otherwise 
 * not really useful unless you're unit-testing stuff.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class MemoryUserAccountStore extends AbstractUserAccountStore {

	public static final Logger log = Logger
			.getLogger(MemoryUserAccountStore.class);
	
	private final List<UserAccount> accounts =
		new ArrayList<UserAccount>();
	
	public List<UserAccount> getAccounts() {
		
		assertUnlocked();
		
		synchronized( accounts ) {
			return new ArrayList<UserAccount>( accounts );
		}
	}
	
	@Override
	public UserAccount getAccountByCharacterID(CharacterID id) throws NoSuchCharacterException {
		if ( id == null ) {
			throw new IllegalArgumentException("Character ID cannot be NULL");
		}
		
		assertUnlocked();
		
		synchronized( accounts ) {
			
			for ( UserAccount acct : accounts ) {
				if ( acct.containsCharacter(id ) ) {
					return acct;
				} 
			}
		}
		
		throw new NoSuchCharacterException("Found no user account that has a character with ID "+id,id);
	}
	
	@Override
	public UserAccount getAccountByUserId(long userId) {
		
		assertUnlocked();
		
		synchronized( accounts ) {
			for ( UserAccount acct : accounts ) {
				if ( acct.getUserId() == userId ) {
					return acct;
				}
			}
		}
		throw new NoSuchElementException("No user account with ID "+userId);
	}
	
	@Override
	public void reconcile(CharacterID characterId) {
		
		if (characterId == null) {
			throw new IllegalArgumentException("characterId cannot be NULL");
		}
		
		log.info("reconcile(): character = "+characterId);
		
		UserAccount owningAccount = null;
		synchronized( accounts ) {
			for ( UserAccount account : accounts ) {
				if ( account.containsCharacter( characterId ) ) {
					owningAccount = account;
					break;
				}
			}
		}
		
		if ( owningAccount == null ) {
			throw new IllegalArgumentException("Unable to find user account with character "+characterId);
		}
		
		final APIResponse<ICharacter> response = 
				getAPIClient().getCharacter( 
					characterId , 
					owningAccount , 
					RequestOptions.DEFAULT
			);
				
		final ICharacter character = owningAccount.getCharacterByID( characterId );
		character.reconcile( response.getPayload() );
		notifyCharacterEdited( owningAccount , character );
	}
	
	@Override
	public void storeAccount(UserAccount account) {
		
		if ( account == null ) {
			throw new IllegalArgumentException("account cannot be NULL");
		}
		
		assertUnlocked();

		UserAccount found = null;
		synchronized( accounts ) {
			for ( Iterator<UserAccount> it = accounts.iterator() ; it.hasNext() ; ) {
				final UserAccount existing = it.next();
				if ( existing.getUserId() == account.getUserId() ) 
				{
					found = existing;
					break;
				}
			}
			
		}
	
		if ( found != null ) 
		{
			
			if (  found == account ) {
				notifyAccountEdited( found );
				return;
			}
			
			notifyAccountAboutToBeRemoved( found );
			
			boolean reallyRemoved = false;
			for ( Iterator<UserAccount> it = accounts.iterator() ; it.hasNext() ; ) {
				if ( it.next() == found ) {
					it.remove();
					reallyRemoved = true;
					break;
				}
			}
			
			if ( reallyRemoved ) {
				notifyAccountRemoved( found );
			}
		}
		
		synchronized( accounts ) {
			accounts.add( account );
		}
		
		notifyAccountAdded( account );
	}

	@Override
	public void closeHook() {
		synchronized( accounts ) {
			accounts.clear();
		}
	}

	@Override
	public void deleteAccount(UserAccount account) {
		
		assertUnlocked();
		
		if (account == null) {
			throw new IllegalArgumentException("account cannot be NULL");
		}
		
		UserAccount found = null;
		synchronized (accounts) {
			for ( Iterator<UserAccount> it = this.accounts.iterator() ; it.hasNext() ; ) {
				final UserAccount acct = it.next();
				if ( acct.getUserId() == account.getUserId() ) {
					found = acct;
					break;
				}
			}
		}

		if ( found != null ) 
		{
			
			boolean reallyRemoved = false;
			
			notifyAccountAboutToBeRemoved( found );
			
			synchronized( accounts ) {
				for ( Iterator<UserAccount> it = this.accounts.iterator() ; it.hasNext() ; ) {
					if ( it.next().getUserId() == found.getUserId() ) {
						it.remove();
						reallyRemoved = true;
					}
				}
			}
			
			if ( reallyRemoved ) {
				notifyAccountRemoved( found );
			}
		}
	}

	@Override
	public void persist() throws IOException {
	}
}
