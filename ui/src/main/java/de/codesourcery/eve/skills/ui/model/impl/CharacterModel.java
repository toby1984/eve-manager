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

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;

/**
 * Combo box model that wraps a <code>UserAccount</code> 
 * and returns the account's <code>ICharacter</code> instances.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class CharacterModel extends AbstractListModel implements ISelectionListener<UserAccount>{

	private final ISelectionProvider<UserAccount> selectionProvider;
	
	public CharacterModel(ISelectionProvider<UserAccount> provider) {
		if (provider == null) {
			throw new IllegalArgumentException("selection provider cannot be NULL");
		}
		this.selectionProvider = provider;
		selectionProvider.addSelectionListener( this );
		selectionChanged( provider.getSelectedItem() );
	}
	
	public void dispose() {
		selectionProvider.removeSelectionListener( this );
	}
	
	@Override
	public ICharacter getElementAt(int index) {
		final UserAccount selected =
			selectionProvider.getSelectedItem();
		if ( selected == null ) {
			throw new IllegalStateException("No user account selected?");
		}
		return selected.getCharacters().get(index);
	}

	@Override
	public int getSize() {
		final UserAccount selected =
			selectionProvider.getSelectedItem();
		if ( selected == null ) {
			return 0;
		}		
		return selected.getCharacters().size();
	}

	@Override
	public void selectionChanged(UserAccount selected) {
		if ( selected == null ) {
			fireContentsChanged( this , 0 , 0 );
		} else {
			final int min = 0;
			final int max = selected.getCharacters().isEmpty() ? 0 : selected.getCharacters().size() - 1;
			fireContentsChanged( this , min , max );
		}
	}

	public ISelectionProvider<UserAccount> getSelectionProvider()
	{
		return selectionProvider;
	}
	
}
