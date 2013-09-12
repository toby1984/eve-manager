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
package de.codesourcery.eve.skills.ui.frames.impl;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.ServerStatus;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions.DataRetrievalStrategy;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.accountdata.UserAccountChangeListenerAdapter;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.ui.IMain;
import de.codesourcery.eve.skills.ui.components.AbstractSelectionProvider;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.IComponent;
import de.codesourcery.eve.skills.ui.components.impl.APIRequestStatusComponent;
import de.codesourcery.eve.skills.ui.components.impl.AppPreferencesComponent;
import de.codesourcery.eve.skills.ui.components.impl.AssetListComponent;
import de.codesourcery.eve.skills.ui.components.impl.BlueprintBrowserComponent;
import de.codesourcery.eve.skills.ui.components.impl.BlueprintLibraryComponent;
import de.codesourcery.eve.skills.ui.components.impl.CharacterSheetComponent;
import de.codesourcery.eve.skills.ui.components.impl.CharacterStandingsComponent;
import de.codesourcery.eve.skills.ui.components.impl.CorpFactionStandingsComponent;
import de.codesourcery.eve.skills.ui.components.impl.IndustryJobStatusComponent;
import de.codesourcery.eve.skills.ui.components.impl.ItemBrowserComponent;
import de.codesourcery.eve.skills.ui.components.impl.ManageUserAccountsComponent;
import de.codesourcery.eve.skills.ui.components.impl.MarketOrderComponent;
import de.codesourcery.eve.skills.ui.components.impl.MarketPriceEditorComponent;
import de.codesourcery.eve.skills.ui.components.impl.MarketTransactionsComponent;
import de.codesourcery.eve.skills.ui.components.impl.OreChartComponent;
import de.codesourcery.eve.skills.ui.components.impl.SkillInTrainingComponent;
import de.codesourcery.eve.skills.ui.components.impl.SkillTreeComponent;
import de.codesourcery.eve.skills.ui.components.impl.SkillTreeView;
import de.codesourcery.eve.skills.ui.components.impl.StatusBarComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.CalendarComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.ShoppingListComponent;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.frames.CustomFrame;
import de.codesourcery.eve.skills.ui.frames.WindowManager;
import de.codesourcery.eve.skills.ui.frames.WindowManager.IWindowProvider;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.model.impl.CharacterModel;
import de.codesourcery.eve.skills.ui.model.impl.UserAccountModel;
import de.codesourcery.eve.skills.ui.renderer.CharacterComboBoxRenderer;
import de.codesourcery.eve.skills.ui.renderer.UserAccountComboBoxRenderer;
import de.codesourcery.eve.skills.ui.tabbedpanes.TabbedComponentPane;
import de.codesourcery.eve.skills.ui.utils.ICommand;
import de.codesourcery.eve.skills.ui.utils.Misc;
import de.codesourcery.eve.skills.ui.utils.SmartMenuBar;
import de.codesourcery.eve.skills.ui.utils.SmartMenuItem;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.util.IStatusCallback;

/**
 * The application's main window.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class MainFrame extends CustomFrame implements ActionListener,ItemListener {

	// UI
	private final JPanel toolbar = new JPanel();
	
	private final JComboBox accountSelector = new JComboBox();
	private final JComboBox charSelector = new JComboBox();

	// buttons
	private final JButton quitButton = new JButton("Quit");

	// menu bar
	private final SmartMenuBar menuBar = new SmartMenuBar();
	
	private final StatusBarComponent statusBar = new StatusBarComponent();

	// ================================= data ============================

	// data

	@Resource(name="main")
	private IMain application;

	@Resource(name="api-client")
	private IAPIClient apiClient;
	
	@Resource(name="appconfig-provider")
	private IAppConfigProvider appConfigProvider;
	
	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;
	
	@Resource(name="marketdata-provider")
	private IMarketDataProvider marketDataProvider;
	
	private final SkillTreeComponent skillTreeComponent =
		new SkillTreeComponent();

	private ICharacter selectedCharacter;
	private UserAccount selectedAccount;

	private final AbstractSelectionProvider<ICharacter> characterHelper = 
		new AbstractSelectionProvider<ICharacter>() {

			@Override
			public ICharacter getSelectedItem() {
				return selectedCharacter;
			}
		};
	private final AbstractSelectionProvider<UserAccount> accountHelper =
		new AbstractSelectionProvider<UserAccount>() {

			@Override
			public UserAccount getSelectedItem() {
				return selectedAccount;
			}};

	public MainFrame() 
	{
		super("Eve Skills");
		userAccountStore.addChangeLister( new UserAccountChangeListenerAdapter() {

			@Override
			public void characterAboutToRemoved(UserAccount account,
					ICharacter c)
			{
				setupCharacterSelectionModels();
			}

			@Override
			public void characterAdded(UserAccount account, ICharacter c) { 
				setupCharacterSelectionModels();
			}

			@Override
			public void characterEdited(UserAccount account, ICharacter c) { 
				setupCharacterSelectionModels();
			}

			@Override
			public void userAccountAboutToBeRemoved(UserAccount account) { 
				setupCharacterSelectionModels();
			}

			@Override
			public void userAccountAdded(UserAccount account)
			{
				setupCharacterSelectionModels();				
			}

			@Override
			public void userAccountEdited(UserAccount account)  { 
				setupCharacterSelectionModels();
			}} );
	}
	
	protected void initialize() {
		
		final JPanel contentPanel = new JPanel();
		
		contentPanel.setLayout( new GridBagLayout() );

		/* +------------------------+
		 * |  toolbar               |
		 * +------------------------+
		 * |    Tabbed pane         |
		 * +-----------+------------+
		 * |      Status bar        |
		 * +-----------+------------+
		 */

		// add button toolbar
		quitButton.addActionListener( this );
		
		accountSelector.setPreferredSize( new Dimension( 200, 25 ) );
		charSelector.setPreferredSize( new Dimension( 200, 25 ) );
		toolbar.add( accountSelector );
		toolbar.add( charSelector );
		toolbar.add( quitButton );
		contentPanel.add( toolbar , constraints(0,0 ).resizeHorizontally().anchorWest().weightY(0.0d).end() );

		setupComboBoxes();
		
		// add tabbed pane
		contentPanel.add( createTabbedPane() , constraints( 0, 1 ).resizeBoth().end() );
		
		// add status bar
		statusBar.onAttach( null );
		marketDataProvider.addStatusCallback( new IStatusCallback() {

			@Override
			public void displayMessage(MessageType type, String message) {
				if ( type != MessageType.ERROR ) {
					statusBar.addMessage( message);
				} else {
					statusBar.addMessage("ERROR: "+message);
				}
			}
		} );
		JPanel p = statusBar.getPanel();
		p.setPreferredSize(new Dimension(600,20 ) );
		
		contentPanel.add( p , constraints( 0, 2 ).noResizing().end() );

		// setup menu
		setupMenu();
		
		this.setJMenuBar( menuBar );

		// add to frame's content pane
		getContentPane().add( contentPanel );
		
		accountSelector.addItemListener( this );
		charSelector.addItemListener( this );		

		pack();
	}
	
	protected TabbedComponentPane createTabbedPane() {
		
		final TabbedComponentPane result =
			new TabbedComponentPane( this.statusBar );
		
		final SkillTreeView view =
			new SkillTreeView();
		
		view.setSkillTreeComponent( skillTreeComponent );

		addTab( result , "Skill tree" , view );
		addTab( result , "Character status" , new SkillInTrainingComponent() );
		addTab( result , "Character sheet" , new CharacterSheetComponent() );		
		addTab( result ,"Asset list" , new AssetListComponent() );
		addTab( result ,"Blueprint library" , new BlueprintLibraryComponent() );
		addTab( result , "Item browser" , new ItemBrowserComponent());
		addTab( result , "Ore chart" , new OreChartComponent() );
		addTab( result , "NPC corp standings" , new CharacterStandingsComponent());	
		addTab( result , "Corp NPC faction standings" , new CorpFactionStandingsComponent());
		
		addTab( result , "Calendar" , new CalendarComponent() );
		addTab( result , "Shopping list" , new ShoppingListComponent() );
		
		// Add blueprint browser component
		addTab( result , "Blueprint browser" , new BlueprintBrowserComponent());		
		addTab( result , "Character industry jobs" , new IndustryJobStatusComponent( ));			
		addTab( result , "Market prices" , new MarketPriceEditorComponent( marketDataProvider ) );
		addTab( result,"Market orders", new MarketOrderComponent( characterHelper ) );
		addTab( result , "Market transactions" , new MarketTransactionsComponent( ));		
		
		return result;
	}
	
	private void addTab(TabbedComponentPane pane , String tabTitle , IComponent component ) 
	{
		if ( component instanceof ICharacterSelectionProviderAware) {
		((ICharacterSelectionProviderAware) component).setSelectionProvider( characterHelper );	
		}
		pane.add( tabTitle , component );
	}

	protected void setupToolbar() {
		quitButton.addActionListener( this );
	}

	protected void setupMenu() {

		menuBar.add( new SmartMenuItem("File/Manager user accounts..." , 
				new ICommand() {

			@Override
			public void execute(Object context) {
				final String key = "manage_useraccounts";
				WindowManager.getInstance().getWindow( key , new IWindowProvider() {

					@Override
					public Window createWindow() {
						return ComponentWrapper.wrapComponent( MainFrame.this,
								new ManageUserAccountsComponent( MainFrame.this.userAccountStore ) );
					}
					
				}).setVisible( true );
			}
		}) );
		

		menuBar.add( new SmartMenuItem("File/Edit preferences..." , 
				new ICommand() {
			@Override
			public void execute(Object context) {
				ComponentWrapper.wrapComponent( new AppPreferencesComponent() ).setVisible( true );
			}
		}));
		
		menuBar.add( new SmartMenuItem("File/Quit application" , 
				new ICommand() {
			@Override
			public void execute(Object context) {
				application.shutdown();
			}
		}));	
		
		menuBar.add( new SmartMenuItem("Help/Show server status" , 
				new ICommand() {
			@Override
			public void execute(Object context) {
				
				submitTask( new UITask() {

					private ServerStatus status;
					
					@Override
					public String getId() {
						return "server_status";
					}

					@Override
					public void successHook() throws Exception {
						final String msg;
						if ( status.isServerOpen() ) {
							msg = "Server open, "+status.getPlayerCount()+" players online.";
						} else {
							msg = "The Tranquility server is currently closed.";
						}
						displayInfo( msg );						
					}

					@Override
					public void run() throws Exception {
						status =
							apiClient.getServerStatus( new RequestOptions(DataRetrievalStrategy.FORCE_UPDATE) ).getPayload();
					}
				});
			}
		}));
		
		menuBar.add( new SmartMenuItem("Help/API request monitor" , 
				new ICommand() {
			@Override
			public void execute(Object context) {
				
				final String key = "api_request_monitor";
				WindowManager.getInstance().getWindow( key , new IWindowProvider() {

						@Override
						public Window createWindow() {
							return ComponentWrapper.wrapComponent( "API request monitor" ,
									new APIRequestStatusComponent() );
						}
						
					} ).setVisible( true );
			}
		}));

		menuBar.add( new SmartMenuItem("Help/About" , new ICommand() {

			@Override
			public void execute(Object context)
			{
				final String msg =  "Eve Skills 0.1 Alpha\n\n(C) 2009 tobias.gierke@code-sourcery.de";
				JOptionPane.showMessageDialog( null,
						msg, "About" , 
						JOptionPane.INFORMATION_MESSAGE);
			}} )  );
	}
	
	@Override
	public void dispose() {
		super.dispose();
		try {
			statusBar.onDetach();
		} 
		finally {
			statusBar.dispose();
		}
	}
	
	protected UserAccount getInitialAccount(List<UserAccount> accounts) {

		final CharacterID charID =
			appConfigProvider.getAppConfig().getLastUsedCharacter();

		if ( charID != null ) {
			for( UserAccount acct : accounts ) {
				if ( acct.hasCharacter( charID ) ) {
					return acct;
				}
			}
		}

		if ( ! accounts.isEmpty() ) {
			final List<ICharacter> chars = accounts.get(0).getCharacters();
			if ( ! chars.isEmpty() ) {
				return accounts.get(0);
			}
		}
		return null;
	}

	protected ICharacter getInitialCharacter(List<UserAccount> accounts) {

		if ( accounts.isEmpty() ) {
			return null;
		}
		
		final CharacterID charID =
			appConfigProvider.getAppConfig().getLastUsedCharacter();

		ICharacter result = null;
		if ( charID != null ) {
			for( UserAccount acct : accounts ) {
				if ( acct.hasCharacter( charID ) ) {
					result = acct.getCharacterByID( charID );
					break;
				}
			}
		}

		if ( result == null ) {
			final List<ICharacter> chars = accounts.get(0).getCharacters();

			if ( chars.isEmpty() ) {
				return null;
			}
			result = chars.get(0);
		}
		
		appConfigProvider.getAppConfig().setLastUsedCharacter( result );
		
		userAccountStore.reconcile( result.getCharacterId() );
		return result;
	}	
	
	protected void setupCharacterSelectionModels() {
		final List<UserAccount> accts = this.userAccountStore.getAccounts();
		
		/* 
		 * Setup account selector.
		 */
		accountSelector.setModel(
				new DefaultComboBoxModel<UserAccount>( 
						new UserAccountModel( this.userAccountStore ) 
				) 
		);

		this.selectedAccount = getInitialAccount( accts );
		if ( this.selectedAccount != null ) {
			accountSelector.setSelectedItem( this.selectedAccount );
		}
		
		/*
		 * Setup character selector.
		 */
		this.selectedCharacter = getInitialCharacter( accts );
		log.debug("setupComboBoxes(): initial character = "+selectedCharacter);
		
		if ( this.selectedCharacter != null ) {
			charSelector.setModel(
					new DefaultComboBoxModel<ICharacter>(
							new CharacterModel( accountHelper )
					) 
			);
			charSelector.setSelectedItem( this.selectedCharacter );
		} else {
			charSelector.setModel( new javax.swing.DefaultComboBoxModel() );
		}
	}

	protected void setupComboBoxes() {

		setupCharacterSelectionModels();
		accountSelector.setRenderer( new UserAccountComboBoxRenderer() );
		charSelector.setRenderer( new CharacterComboBoxRenderer() );
	}

	protected SkillTreeComponent getSkillTreeComponent() {
		return skillTreeComponent;
	}

	protected boolean assertCharacterSelected() {
		if ( this.selectedAccount == null || this.selectedCharacter == null ) {
			displayInfo("You need to select a user account and character first");
			return false;
		}
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		final Object source = event.getSource();

		if ( source == quitButton ) {
			dispose();
			application.shutdown();
		}
	}

	protected IUserAccountStore getUserAccountStore() {
		return userAccountStore;
	}

	@Override
	public void itemStateChanged(ItemEvent event) {

		if ( event.getStateChange() == ItemEvent.DESELECTED ) {
			return;
		}
		
		if ( event.getSource() == this.accountSelector ) {
			this.selectedAccount = (UserAccount) event.getItem(); 
			if ( accountHelper != null ) {
				accountHelper.selectionChanged( selectedAccount );
			}
			this.selectedCharacter = null;
			this.charSelector.setSelectedItem(null);
			
			if ( characterHelper != null ) {
				characterHelper.selectionChanged( null );						
			}
			
		} else if ( event.getSource() == this.charSelector ) {
			
			this.selectedCharacter = (ICharacter) event.getItem();
			
			final IBaseCharacter c = (IBaseCharacter) event.getItem();
			if ( c != null ) {
				appConfigProvider.getAppConfig().setLastUsedCharacter( selectedCharacter );
			}
			
			log.debug("character helper: "+this.characterHelper );
			
			if ( characterHelper != null ) {
				
				final ICharacter selected =
					this.selectedCharacter;
				
				log.debug("character selection changed: "+selected);
				submitTask( new UITask() {

					@Override
					public void beforeExecution() {
						if ( selected != null ) {
							statusBar.addMessage("Fetching character data for "+
									selected.getName() );
						}
					}
					
					@Override
					public void failureHook(Throwable t) throws Exception {
						if ( selected != null ) {
							statusBar.addMessage("Failed to fetch character data for "+
									selected.getName()+" : "+t.getMessage() );
						}
					}
					
					@Override
					public void successHook() throws Exception {
						statusBar.addMessage("Data retrieved.");
					}
					
					@Override
					public String getId() {
						return "reconcile_"+selectedCharacter.getCharacterId().getValue();
					}

					@Override
					public void run() throws Exception {
						
						if ( selected != null ) {
							userAccountStore.reconcile( selected.getCharacterId() );
						}
						
						Misc.runOnEventThread( new Runnable() {

							@Override
							public void run() {
								characterHelper.selectionChanged( selected );								
							}
						} );
												
					}} );
				
			}
		}
	}

	public StatusBarComponent getStatusBar() {
		return statusBar;
	}

}
