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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.Resource;
import javax.management.InvalidApplicationException;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.CharacterStandings;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.production.RefiningCalculator;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.impl.planning.AssemblyLineChooser;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;

public abstract class AbstractRefiningComponent extends AbstractComponent
{
	@Resource(name="api-client")
	private IAPIClient apiClient;
	
	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;
	
	@Resource(name="appconfig-provider")
	private IAppConfigProvider applicationConfigProvider;

	private JButton selectStationButton =
		new JButton("Select station");

	private JTextField selectedCharacterField =
		new JTextField(30);

	private JButton selectCharacterButton = 
		new JButton("Select character");
	
	private JTextField selectedStationField =
		new JTextField(30);
	
	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;
	
	private Station selectedStation;
	private ICharacter selectedCharacter;
	
	private final RefiningCalculator calculator;
	
	private StationWithStandings standings;
	
	@Resource(name="appconfig-provider")
	private IAppConfigProvider configProvider;
	
	protected static final class StationWithStandings 
	{
		private final Station station;
		private final float standings;
		
		protected StationWithStandings(Station station, Standing<NPCCorporation> standings ) {
			this.standings = standings != null ? standings.getValue() : 0.0f ;
			this.station = station;
		}
		
		public float getValue()
		{
			return standings;
		}
		
		public Station getStation()
		{
			return station;
		}
	}
	
	public AbstractRefiningComponent() {
		super();
		this.calculator = new RefiningCalculator( dataModel );
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
		super.onAttachHook(callback);
		
		if ( this.selectedCharacter != null ) {
			this.selectedCharacterField.setText( selectedCharacter.getName() );
		} else {
			this.selectedCharacterField.setText( null );
		}
		
		if ( selectedStation != null ) {
			selectedStationField.setText( selectedStation.getDisplayName() );
		} else {
			selectedStationField.setText( null );
		}
	}
	
	@Override
	protected void onDetachHook()
	{
		this.standings = null;
	}
	
	protected StationWithStandings getStandings() {
	
		final Station currentStation = getSelectedStation();
		final ICharacter currentCharacter =
			getSelectedCharacter();
		
		if ( currentStation == null ) {
			throw new IllegalArgumentException("currentStation cannot be NULL");
		}
		
		if ( currentCharacter == null ) {
			throw new IllegalArgumentException("currentCharacter cannot be NULL");
		}
		
		if ( standings == null || ! standings.station.getID().equals( currentStation.getID() ) ) 
		{
			final CharacterStandings charStandings=
				apiClient.getCharacterStandings(
						currentCharacter , 
						userAccountStore.getAccountByCharacterID( currentCharacter.getCharacterId() ),
						RequestOptions.DEFAULT ).getPayload();
			
			this.standings =
				new StationWithStandings( currentStation , 
					charStandings.getNPCCorpStanding( currentStation.getOwner() ) 
				);
			
		} 
		return standings;
	}
	
	protected RefiningCalculator getRefiningCalculator() {
		return calculator;
	}
	
	protected Station queryStation() {
		
		final CharacterStandings standings=
			apiClient.getCharacterStandings(
					selectedCharacter , 
					userAccountStore.getAccountByCharacterID( selectedCharacter.getCharacterId() ),
					RequestOptions.DEFAULT ).getPayload();

		// query user
		final AssemblyLineChooser comp =
			new AssemblyLineChooser( standings );

		comp.setFixedActivity( Activity.REFINING );
		
		comp.setModal( true );
		ComponentWrapper.wrapComponent( "Choose station where to refine at" , comp ).setVisible(true);

		if ( comp.wasCancelled() ) {
			return null;
		}
		return comp.getSelectedStation();
	}
	
	public void setSelectedCharacter(ICharacter character) {
		if ( character == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		this.selectedCharacterField.setText( character.getName() );
		this.selectedCharacter = character;
	}

	protected Station getSelectedStation()
	{
		if ( this.selectedStation == null ) 
		{
			final Long stationId = configProvider.getAppConfig().getDefaultRefiningStationId( getSelectedCharacter() );
			if ( stationId != null ) 
			{
				selectedStation = dataModel.getStation( stationId );
			} 
			else 
			{
				selectedStation = queryStation();
				if ( selectedStation != null ) {
					configProvider.getAppConfig().setDefaultRefiningStationId( getSelectedCharacter() ,
							selectedStation.getID() );
				}
			}
		}

		if ( selectedStation != null ) {
			selectedStationField.setText( selectedStation.getDisplayName() );
		} else {
			selectedStationField.setText( null );
		}
		return selectedStation;
	}
	
	public ICharacter getSelectedCharacter()
	{
		return selectedCharacter;
	}
	
	public void setSelectedStation(Station selectedStation)
	{
		this.selectedStation = selectedStation;
		if ( selectedStation != null ) 
		{
			if ( selectedCharacter != null ) {
			applicationConfigProvider.getAppConfig().setDefaultRefiningStationId(
					selectedCharacter , selectedStation.getID() );
			}
			selectedStationField.setText( selectedStation.getDisplayName() );
		} else {
			selectedStationField.setText( null );
		}
	}
	
	protected ICharacter queryCharacter() 
	{
		final CharacterChooserComponent comp = new CharacterChooserComponent();
		comp.setModal( true );
		comp.setSelectedCharacter( selectedCharacter );
		ComponentWrapper.wrapComponent("Choose a character" , comp ).setVisible(true);
		
		if ( ! comp.wasCancelled() ) {
			this.selectedCharacter = comp.getSelectedCharacter();
		}
		return this.selectedCharacter;
	}
	
	protected JPanel createSelectionPanel()
	{
		
		selectStationButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				setSelectedStation( queryStation() );
			}
		} );
		
		selectedStationField.setEditable( false );
		selectedCharacterField.setEditable( false );
		
		selectCharacterButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				setSelectedCharacter( queryCharacter() );
			}
		} );
		
		/*
		 * Create result panel.
		 */
		final JPanel controlsPanel =
			new JPanel();
		
		controlsPanel.setLayout( new GridBagLayout() );
		
		controlsPanel.add( selectedStationField , constraints(0,0).weightX(0.8).resizeHorizontally().end() );
		controlsPanel.add( selectStationButton, constraints(1,0).weightX(0.2).resizeHorizontally().end() );
		
		controlsPanel.add( selectedCharacterField, constraints(0,1).weightX(0.8).resizeHorizontally().end() );
		controlsPanel.add( selectCharacterButton, constraints(1,1).weightX(0.2).resizeHorizontally().end() );
		
		selectedStationField.setColumns( 35 );
		selectedCharacterField.setColumns( 35 );
		
		return controlsPanel;
			
	}
}
