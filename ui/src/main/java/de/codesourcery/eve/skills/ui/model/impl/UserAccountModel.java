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
package de.codesourcery.eve.skills.ui.model.impl;

import javax.swing.AbstractListModel;

import de.codesourcery.eve.skills.accountdata.IUserAccountChangeListener;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;

/**
 * Wraps a <code>IUserAccountStore</code>
 * and returns <code>UserAccount</code> instances.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class UserAccountModel extends AbstractListModel implements IUserAccountChangeListener {

	private final IUserAccountStore store;
	
	public UserAccountModel(IUserAccountStore store) {
		if (store == null) {
			throw new IllegalArgumentException("Useraccount store cannot be NULL");
		}
		this.store = store;
		store.addChangeLister( this );
	}
	
	public void dispose() {
		store.removeChangeLister( this );
	}
	
	@Override
	public UserAccount getElementAt(int index) {
		return store.getAccounts().get(index);
	}

	@Override
	public int getSize() {
		return store.getAccounts().size();
	}
	
	private int getAccountIndex(UserAccount account) {
		
		int index = 0;
		for ( UserAccount acct : store.getAccounts() ) {
			if ( acct == account ) {
				return index;
			}
			index++;
		}
	
		throw new RuntimeException("Account "+account+" not in accounts store ?");
	}
	
	@Override
	public void characterAdded(UserAccount account, ICharacter c) {
	}

	@Override
	public void characterEdited(UserAccount account, ICharacter c) {
	}

	@Override
	public void characterAboutToRemoved(UserAccount account, ICharacter c) {
	}

	@Override
	public void userAccountAdded(UserAccount account) {
		final int index = getAccountIndex( account );
		fireIntervalAdded( this , index ,index );		
	}

	@Override
	public void userAccountEdited(UserAccount account) {
		final int index = getAccountIndex( account );
		fireContentsChanged( this , index ,index );				
	}

	@Override
	public void userAccountAboutToBeRemoved(UserAccount account) 
	{
		// TODO: But this in userAccountRemoved() , another listener may have vetoed the removal
		final int index = getAccountIndex( account );
		fireIntervalRemoved( this , index ,index );				
	}

	@Override
	public void characterRemoved(UserAccount account, ICharacter c)
	{
	}

	@Override
	public void userAccountRemoved(UserAccount account)
	{
	}

}
