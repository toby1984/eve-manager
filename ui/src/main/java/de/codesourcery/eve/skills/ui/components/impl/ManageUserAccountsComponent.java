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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;

import javax.annotation.Resource;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.Credentials;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.IEditorComponent;
import de.codesourcery.eve.skills.ui.components.IEditorListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.components.ListEditingComponent;
import de.codesourcery.eve.skills.ui.model.impl.CharacterModel;
import de.codesourcery.eve.skills.ui.model.impl.UserAccountModel;

public class ManageUserAccountsComponent extends AbstractComponent {

	private final IUserAccountStore accountStore;

	private final UserAccountComponent accountEditor;
	private final CharacterComponent characterEditor;
	
	private final JButton closeButton = new JButton("Close");
	
	@Resource(name="api-client")
	private IAPIClient apiClient;

	public ManageUserAccountsComponent(IUserAccountStore store) {

		if (store == null) {
			throw new IllegalArgumentException("store cannot be NULL");
		}
		this.accountStore = store;

		accountEditor = new UserAccountComponent(
				new UserAccountModel( store )
		);

		characterEditor = new CharacterComponent( accountEditor );
		
		/*
		 * Make sure child components are automatically
		 * attached / detached / disposed with this
		 * component.
		 */
		registerChildren( accountEditor );
		registerChildren( characterEditor );
	}
	
	@Override
	public String getTitle() {
		return "User accounts";
	}

	@Override
	public boolean isModal() {
		return true;
	}

	@Override
	protected JPanel createPanel() {

		final JPanel result =
			new JPanel();
		
		result.setLayout( new GridBagLayout() );
		
		result.add( accountEditor.getPanel() ,
				constraints(0,0).end() );
		
		result.add( characterEditor.getPanel() ,
				constraints(1,0).end() );
		
		closeButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				getComponentCallback().dispose( ManageUserAccountsComponent.this );
			}} );

		return result;
	}

	private final class UserAccountComponent extends ListEditingComponent<UserAccount> {

		public UserAccountComponent(ListModel model) {
			super(model);
		}

		@Override
		public String getTitle() {
			return "User accounts";
		}

		@Override
		protected void addItem() {

			final CreateUserAccountComponent editor = 
				new CreateUserAccountComponent( accountStore );

			editor.addEditorListener( new IEditorListener() {

				@Override
				public void editingCancelled(IEditorComponent comp) { }

				@Override
				public void editingFinished(IEditorComponent comp) {

					Credentials credentials =
						new Credentials();

					if ( editor.hasLimitedApiKey() ) {
						credentials.setKey( editor.getLimitedApiKey() );
					}

					if ( editor.hasFullAccessApiKey() ) {
						credentials.setKey( editor.getFullApiKey() );
					}

					credentials.setUserId( editor.getUserId() );

					final UserAccount newAccount =
						new UserAccount( editor.getAccountName() , credentials );

					syncWithServer(newAccount,credentials);

					accountStore.storeAccount( newAccount );
					characterEditor.refresh();
					try {
						accountStore.persist();
					} catch (IOException e) {
						displayError("Failed to store account details: "+e.getMessage() ,e);
					}					
				}
			}
			);

			final Window dialog =
				ComponentWrapper.wrapComponent(editor );

			dialog.setVisible( true );
		}

		@Override
		protected void editItem(final UserAccount selected) {

			final CreateUserAccountComponent editor = 
				new CreateUserAccountComponent( accountStore , selected );

			editor.addEditorListener( new IEditorListener() {

				@Override
				public void editingCancelled(IEditorComponent comp) { }

				@Override
				public void editingFinished(IEditorComponent comp) {

					final Credentials creds =
						new Credentials( editor.getUserId() , editor.getKeys() );

					syncWithServer(selected, creds );

					selected.setName( editor.getAccountName() );
					selected.getCredentials().setUserId( editor.getUserId() );
					selected.getCredentials().setKeys( editor.getKeys() );

					accountStore.notifyAccountEdited( selected );

					try {
						accountStore.persist();
					} catch (IOException e) {
						displayError("Failed to store account details: "+e.getMessage(),e );
					}
				}
			}
			);

			final Window dialog =
				ComponentWrapper.wrapComponent(editor );

			dialog.setVisible( true );			
		}

		@Override
		protected void removeItem(UserAccount selected) {
			System.out.println("TODO: Remove account...");
		}

		@Override
		protected ListCellRenderer createListRenderer() {
			return new DefaultListCellRenderer() {

				@Override
				public Component getListCellRendererComponent(JList arg0,
						Object item, int arg2, boolean arg3, boolean arg4) 
				{
					super.getListCellRendererComponent(arg0, item, arg2, arg3, arg4);

					if ( item != null ) {
						setText( ((UserAccount) item).getName() );
					} else {
						setText(null);
					}
					return this;
				}
			};
		}

	}

	protected void syncWithServer(UserAccount account,Credentials creds) {

		final StatusBarComponent statusBar = new StatusBarComponent() ;

		final Window statusWindow = 
			ComponentWrapper.wrapComponent( statusBar );

		try {

			statusWindow.setVisible(true);
			final Collection<IBaseCharacter> charactersFromServer;
			try {

				statusBar.addMessage( "Synchronizing with server..." );

				final APIResponse<Collection<IBaseCharacter>> response = 
					apiClient.getAvailableCharacters(
							creds , RequestOptions.DEFAULT 
					);

				charactersFromServer = response.getPayload();

			} 
			catch (Exception e) {
				throw new RuntimeException(e);
			}					

			statusBar.addMessage( "Updating characters...");

			account.syncWithServer( accountStore , charactersFromServer );
		}
		finally {
			statusWindow.dispose();
		}
	}

	private final class CharacterComponent extends ListEditingComponent<ICharacter> {

		public CharacterComponent(ISelectionProvider<UserAccount> provider) {
			super(new CharacterModel( provider ));
		}
		
		public void refresh() {
			final CharacterModel model =
				(CharacterModel) getModel();
			final ISelectionProvider<UserAccount> provider = model.getSelectionProvider();
			
			if ( provider.getSelectedItem() == null ) {
				accountEditor.setSelectedIndex( 0 );
			} else {
				model.selectionChanged( provider.getSelectedItem() );
			}
			
		}
		
		protected ISelectionProvider<UserAccount> getSelectionProvider() {
			return ((CharacterModel) getModel()).getSelectionProvider();
		}

		@Override
		protected void addItem() {
			System.out.println("Add character...");
		}

		@Override
		protected void editItem(ICharacter selected) {
			System.out.println("Edit character...");
		}

		@Override
		protected void removeItem(ICharacter selected) {
			System.out.println("Remove character...");			
		}

		@Override
		public String getTitle() {
			return "Characters";
		}

		@Override
		protected void populateButtonPanel(JPanel buttonPanel) {
			buttonPanel.add( closeButton );
		}

		@Override
		protected ListCellRenderer createListRenderer() {
			return new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) 
				{
					super.getListCellRendererComponent(list, value, index, isSelected,
							cellHasFocus);

					if ( value != null ) {
						setText( ((ICharacter) value).getName() );
					} else {
						setText(null);
					}
					return this;
				}
			};
		}

	}


}
