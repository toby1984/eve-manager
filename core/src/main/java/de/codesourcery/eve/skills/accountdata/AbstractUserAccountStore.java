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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.exceptions.UserAccountStoreLockedException;

/**
 * Abstract base-class for implementing user account stores.
 * 
 * This is a thread-safe class but otherwise 
 * not really useful unless you're unit-testing stuff.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class AbstractUserAccountStore implements IUserAccountStore {

	private static final Logger log = Logger
			.getLogger(AbstractUserAccountStore.class);
	
	// guarded-by: listeners
	private final List<IUserAccountChangeListener> listeners =
		new ArrayList<IUserAccountChangeListener>();
	
	private volatile boolean isUnlocked;
	private IAPIClient apiClient;
	
	protected final void assertUnlocked() {
		if ( isLocked() ) {
			throw new UserAccountStoreLockedException();
		}
	}
	
	@Override
	public void setAPIClient(IAPIClient client) {
		if (client == null) {
			throw new IllegalArgumentException("client cannot be NULL");
		}
		this.apiClient = client;
	} 
	
	protected IAPIClient getAPIClient() {
		return apiClient;
	}
	
	@Override
	public void addChangeLister(IUserAccountChangeListener l) {
		if ( l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized( listeners ) {
			listeners.add( l );
		}
	}
	
	protected interface INotifier {
		public void notify(IUserAccountChangeListener l);
	}
	
	public void notifyChangeListeners(INotifier n) {
		final List<IUserAccountChangeListener> copy;
		synchronized( listeners ) {
			copy = 
				new ArrayList<IUserAccountChangeListener>( this.listeners );
		}
		for ( IUserAccountChangeListener l : copy ) {
			try {
				n.notify( l );
			} catch(Exception e) {
				log.error("notifyChangeListener(): Change listener "+l+" failed",e);
			}
		}
	}
	
	@Override
	public void removeChangeLister(IUserAccountChangeListener l) {
		synchronized (listeners) {
			listeners.remove( l );
		}
	}
	
	public boolean isLocked() {
		return ! isUnlocked;
	}
	
	public void unlock() {
		this.isUnlocked = true;
	}
	
	public void lock() {
		this.isUnlocked = false;
	}
	
	@Override
	public final void close() {
		try {
			closeHook();
		} finally {
			lock();
		}
	}
	
	public void notifyAccountAdded(final UserAccount account ) {
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.userAccountAdded( account );
			}
		} );
	}
	
	public void notifyAccountAboutToBeRemoved(final UserAccount account ) {
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.userAccountAboutToBeRemoved( account );
			}
		} );
	}	
	
	public void notifyAccountRemoved(final UserAccount account ) {
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.userAccountRemoved( account );
			}
		} );
	}	
	
	public void notifyCharacterRemoved(final UserAccount account , final ICharacter character ) {
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.characterRemoved( account  ,character );
			}
		} );
	}	
	
	public void notifyAccountEdited(final UserAccount account ) {
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.userAccountEdited( account );
			}
		} );
	}	
	
	public void notifyCharacterAdded(final UserAccount account , final ICharacter c) {
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.characterAdded( account , c );
			}
		} );
	}	
	
	public void notifyCharacterAboutToBeRemoved(final UserAccount account , final ICharacter c) {
		
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.characterAboutToRemoved( account , c );
			}
		} );
	}	
	
	public void notifyCharacterEdited(final UserAccount account , final ICharacter c) {
		notifyChangeListeners( new INotifier() {

			@Override
			public void notify(IUserAccountChangeListener l) {
				l.characterEdited( account , c );
			}
		} );
	}		
	
	protected abstract void closeHook();

	public UserAccount getAccountByDisplayName(String displayName) {
		
		assertUnlocked();
		
		for ( UserAccount acct : getAccounts() ) {
			if ( acct.getName().equals( displayName ) ) {
				return acct;
			}
		}
		throw new NoSuchElementException("Found no user account with display name '"+displayName+"'");
	}
}
