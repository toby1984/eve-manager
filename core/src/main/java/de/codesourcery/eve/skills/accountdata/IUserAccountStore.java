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
import java.util.List;
import java.util.NoSuchElementException;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.exceptions.NoSuchCharacterException;
import de.codesourcery.eve.skills.exceptions.UserAccountStoreLockedException;

/**
 * Holds EVE Online(tm) user account data.
 *
 *<pre>
 * Implementations MUST be thread-safe.
 *
 * Implementations require 'unlocking' (see {@link #unlock()} ) before
 * they can be used. All methods are subject to
 * throwing a {@link UserAccountStoreLockedException} if failing to do so.
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IUserAccountStore {
	
	public void persist() throws IOException;
	
	public void removeChangeLister(IUserAccountChangeListener l);
	
	public void addChangeLister(IUserAccountChangeListener l);
	
	public void notifyCharacterAdded(final UserAccount account , final ICharacter c);
		
	public void notifyCharacterEdited(final UserAccount account , final ICharacter c);
	
	public void notifyCharacterAboutToBeRemoved(final UserAccount account , final ICharacter c);
		
	
	public void notifyAccountEdited(final UserAccount account );

	/**
	 * Returns whether this credentials store is currently locked.
	 * 
	 * A credentials store may require 'unlocking' by the current
	 * user before it can actually be used. All methods are subject to
	 * throwing a {@link UserAccountStoreLockedException} if failing to do so.
	 * 	 
	 * @return
	 */
	public boolean isLocked();

	/**
	 * Unlock this credentials store.
	 * 
	 * Invoking this method on an already
	 * unlocked credentials store does no harm.
	 * 
	 * @param password
	 * @see #isLocked()
	 * @see #lock()
	 */
	public void unlock();

	/**
	 * Lock this credentials store.
	 * @see #unlock()
	 * @see #isLocked()
	 */
	public void lock();
	
	/**
	 * Closes this user account store.
	 * 
	 * After this method returns this user account
	 * store is no longer in a usable state.
	 */
	public void close();
	
	/**
	 * Returns the accounts hold in this store.
	 * 
	 * @return
	 */
	public List<UserAccount> getAccounts();

	/**
	 * Store/update credentials for a given character.
	 * 
	 * @param character
	 * @param credentials
	 */
	public void storeAccount(UserAccount account);
	
	/**
	 * Deletes a user account.
	 * 
	 * @param account
	 * @return <code>true</code> if the account has been deleted.
	 */
	public void deleteAccount(UserAccount account);
	
	/**
	 * Returns a user account by display name.
	 * 
	 * @param displayName
	 * @return
	 * @throws NoSuchElementException if no account with the given
	 * display name exists
	 */
	public UserAccount getAccountByDisplayName(String displayName);
	
	/**
	 * Returns a user account by user ID.
	 * 
	 * @param displayName
	 * @return
	 * @throws NoSuchElementException if no account with the given
	 * display name exists
	 */
	public UserAccount getAccountByUserId(long userId);	
	
	/**
	 * Try to refresh data for this character 
	 * from backing storage (read: API)
	 * @param characterId
	 */
	public void reconcile(CharacterID characterId);
	
	public void setAPIClient(IAPIClient client);
	
	/**
	 * Returns the user account that has a character with a given ID.
	 *  
	 * @param id
	 * @return
	 * @throws NoSuchCharacterException if no account with such a character exists
	 */
	public UserAccount getAccountByCharacterID(CharacterID id) throws NoSuchCharacterException;
}
