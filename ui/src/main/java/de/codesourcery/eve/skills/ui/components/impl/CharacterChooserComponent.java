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

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Resource;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.components.AbstractSelectionProvider;
import de.codesourcery.eve.skills.ui.model.impl.CharacterModel;
import de.codesourcery.eve.skills.ui.model.impl.UserAccountModel;

public class CharacterChooserComponent extends AbstractEditorComponent
{
	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;
	
	private final UserAccountModel userAccountModel;
	private final CharacterModel characterModel;
	
	private final JList userAccountList = new JList();
	private final JList characterList= new JList();
	
	private final ListCellRenderer userAccountRenderer = new DefaultListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			setText( ((UserAccount) value).getName() );
			return this;
		}
	};
	
	private final ListCellRenderer characterRenderer = new DefaultListCellRenderer() {
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			setText( ((ICharacter) value).getName() );
			return this;
		}
	};
	
	private final UserAccountSelectionListener userAccountSelectionListener = new UserAccountSelectionListener(); 
	
	
	private final class UserAccountSelectionListener extends AbstractSelectionProvider<UserAccount> 
		implements ListSelectionListener 
	{
		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			if ( ! e.getValueIsAdjusting() ) {
				final int idx = e.getFirstIndex();
				if ( idx >= 0 ) {
					selectionChanged( (UserAccount) userAccountList.getSelectedValue() );
					
					if ( characterModel.getSize() > 0 ) {
						characterList.setSelectedIndex(0);
					}
					
				} else {
					selectionChanged( null );
				}
			}
		}

		@Override
		public UserAccount getSelectedItem()
		{
			return (UserAccount) userAccountList.getSelectedValue();
		}
		
	};
	
	public CharacterChooserComponent() 
	{
		userAccountModel = new UserAccountModel( userAccountStore );
		this.userAccountList.setModel( userAccountModel );
		userAccountList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		userAccountList.setCellRenderer( userAccountRenderer );
		userAccountList.getSelectionModel().addListSelectionListener( userAccountSelectionListener );
		
		characterModel = new CharacterModel( userAccountSelectionListener );
		characterList.setModel( characterModel );
		characterList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		characterList.setCellRenderer( characterRenderer );
		
		characterList.addMouseListener( new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if ( e.getClickCount() != 2 || e.isPopupTrigger() ) {
					return;
				}
				okButtonClicked();
			}
		} );
	}
	
	public ICharacter getSelectedCharacter() {
		return (ICharacter) characterList.getSelectedValue();
	}

	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		if ( userAccountList.getSelectedValue() == null ) {
			if ( userAccountModel.getSize() > 0 ) {
				userAccountList.setSelectedIndex(0);
			}
		}
	}
	
	@Override
	protected JButton createCancelButton()
	{
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton()
	{
		return new JButton("Ok");
	}
	
	@Override
	protected void disposeHook()
	{
		characterModel.dispose();
		userAccountModel.dispose();
	}

	@Override
	protected JPanel createPanelHook()
	{
		final JPanel result = 
			new JPanel();
		result.setLayout( new GridBagLayout() );
		
		final JScrollPane accountsPane = new JScrollPane( userAccountList );
		accountsPane.setBorder( BorderFactory.createTitledBorder("User accounts" ) );
		
		final JScrollPane characterPane = new JScrollPane( characterList );
		characterPane.setBorder( BorderFactory.createTitledBorder("Characters" ) );
		
		result.add( accountsPane , constraints(0,0).useRelativeWidth().end() );
		result.add( characterPane , constraints(1,0).useRemainingWidth().end() );
		
		return result;
	}

	@Override
	protected boolean hasValidInput()
	{
		return true;
	}
	
	public void setSelectedCharacter(final ICharacter c) 
	{
		if ( c == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		
		final UserAccount userAccount = 
			userAccountStore.getAccountByCharacterID( c.getCharacterId() );
		
		final int accountIdx = findIndexOf( userAccountModel , new IMatcher<UserAccount>() {

			@Override
			public boolean matches(UserAccount obj)
			{
				return userAccount.getUserId() == obj.getUserId();
			}}
		);
		
		if ( accountIdx != -1 ) 
		{
			userAccountList.setSelectedIndex( accountIdx );
			final int charIdx = findIndexOf( characterModel , new IMatcher<ICharacter>() {

				@Override
				public boolean matches(ICharacter obj)
				{
					return c.getCharacterId().equals( obj.getCharacterId() );
				}} );
			if ( charIdx != -1 ) {
				characterList.setSelectedIndex( charIdx );
			}
		} else {
			log.error("setSelectedCharacter(): Character "+c+" not found in list ?");
		}
	}
	
	private interface IMatcher<T> {
		public boolean matches(T obj);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> int findIndexOf(ListModel list,IMatcher<T> matcher) {
		for ( int i = 0 ; i < list.getSize() ; i++ ) {
			if ( matcher.matches( (T) list.getElementAt( i ) ) ) {
				return i;
			}
		}
		return -1;
	}

}
