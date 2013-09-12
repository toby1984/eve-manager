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
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.SkillInTraining;
import de.codesourcery.eve.apiclient.datamodel.SkillQueueEntry;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.db.dao.IStaticDataModelProvider;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.utils.Misc;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.utils.DateHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class SkillInTrainingComponent extends AbstractComponent implements ICharacterSelectionProviderAware {

	public static final Logger log = Logger
	.getLogger(SkillInTrainingComponent.class);

	@Resource(name="api-client")
	private IAPIClient apiClient;

	@Resource(name="system-clock")
	private ISystemClock systemClock;

	@Resource(name="useraccount-store")
	private IUserAccountStore accountStore;
	
	@Resource(name="datamodel-provider")
	private IStaticDataModelProvider dataModelProvider;

	private ICharacter currentCharacter;
	private ISelectionProvider<ICharacter> selectionProvider;

	private final ISelectionListener<ICharacter> listener = new ISelectionListener<ICharacter>() {

		@Override
		public void selectionChanged(ICharacter selected) {
			currentCharacter = selected;
			scheduleRefresh();
		}
	};
	// GUI
	private JTextArea textArea = new JTextArea(7,35);

	@Override
	protected JPanel createPanel() {

		JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );

		textArea.setEditable( false );
		scheduleRefresh();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		final JScrollPane pane = new JScrollPane( textArea );
		result.add( pane , constraints(0,0).resizeVertically().end() );

		log.trace("createPanel(): Panel created.");
		return result;
	}

	public SkillInTrainingComponent() {
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback) {
		
		this.selectionProvider.addSelectionListener( listener );
		if ( this.selectionProvider.getSelectedItem() != null ) {
			scheduleRefresh();
		}
	}

	@Override
	protected void onDetachHook() {
		if ( this.selectionProvider != null ) {
			this.selectionProvider.removeSelectionListener( listener );
		}
	}

	@Override
	protected void disposeHook() {
		if ( this.selectionProvider != null ) {
			this.selectionProvider.removeSelectionListener( listener );
		}		
	}

	protected void scheduleRefresh() {

		final ICharacter character =
			this.selectionProvider.getSelectedItem();

		if ( character == null ) {
			log.debug("scheduleRefresh(): No character selected.");
			setLabel("Please select a character.");
			return;
		}

		log.debug("scheduleRefresh(): Starting task...");

		submitTask( new UITask() {

			private volatile APIResponse<SkillInTraining>  response;
			private volatile APIResponse<List<SkillQueueEntry>>  response2;

			@Override
			public String getId() {
				return "skills_in_training_"+character.getName();
			}

			@Override
			public void beforeExecution() {
				setLabel("Fetching data from server...");
			}

			@Override
			public void failureHook(Throwable t) throws Exception {
				Misc.displayError( t );
				setLabel("Failed to query API server: "+t.getMessage() );
			}

			@Override
			public void successHook() throws Exception {
				updateTextArea( response , response2 , character );
			}

			@Override
			public void run() throws Exception {

				log.debug("getSkillInTraining(): Querying API for character "+
						character.getCharacterId() );

				final UserAccount acct =
					accountStore.getAccountByCharacterID( character.getCharacterId() );

				response = apiClient.getSkillInTraining( character.getCharacterId() ,
						acct , RequestOptions.DEFAULT );
				
				response2 = apiClient.getSkillQueue( character , acct , RequestOptions.DEFAULT );
			}} );
	}
	
	protected void updateTextArea(APIResponse<SkillInTraining> response,
			APIResponse<List<SkillQueueEntry>> skillQueue , ICharacter currentChar) {

		String outdated;
		if ( ! response.isUpToDate( systemClock ) ) {
			outdated = "== OUTDATED ==";
		} else {
			outdated = "";
		}

		outdated = toLocalFormat( response.getCachedUntil() )+outdated;

		final SkillInTraining skillInTraining =
			response.getPayload();

		if ( ! skillInTraining.isSkillInTraining() ) {
			setLabel( "NO SKILL IN TRAINING "+outdated+" !" );
			return;
		} 

		final StringBuilder label =
			new StringBuilder();

		label.append( currentChar.getName() );
		label.append( " is currently training " ).append( skillInTraining.getSkill().getName() );
		label.append( " to level ").append( skillInTraining.getPlannedLevel() ).append(".");

		label.append("\n\n");

		label.append("Current skillpoints: "+Skill.skillPointsToString( currentChar.getSkillpoints() ) );
		label.append("\n\n");

		label.append("Progress: \n\n");

		final SkillTree skillTree =
			dataModelProvider.getStaticDataModel().getSkillTree();
		
		/*
		 * Training speed.
		 */
		final int spDelta =
			skillInTraining.getTrainingDestinationSP() - skillInTraining.getTrainingStartSP();
		
		final long elapsedTimeMillis =
			skillInTraining.getTrainingEndTime().getDifferenceInMilliseconds( skillInTraining.getTrainingStartTime() );
		
		final float elapsedHours = ( elapsedTimeMillis / ( 1000 * 60 * 60 ) );
		
		final float trainingSpeed =
			spDelta / elapsedHours;
		
		label.append("Training speed (real)       : "+trainingSpeed+" SP/h\n\n");
		label.append("Training speed (calculated) : "+
				currentChar.getAttributes().calcTrainingSpeed( skillTree , skillInTraining.getSkill() )+
				" SP/h\n\n");
		
		/*
		 * End time.
		 */
		final Date endTime = 
			skillInTraining.getTrainingEndTime().getLocalTime();
		
		final long deltaMillis =
			endTime.getTime() - systemClock.getCurrentTimeMillis();
		
		label.append("Training end   time: "+toLocalFormat( skillInTraining.getTrainingEndTime() ) ).append(" ( "+
				DateHelper.durationToString( deltaMillis ) ).append(" )");

		label.append("\n\n=== Skill queue ===\n\n");
		
		boolean gotSkillsInQueue = false;
		for ( Iterator<SkillQueueEntry> it = skillQueue.getPayload().iterator() ; it.hasNext() ;  ) {
			final SkillQueueEntry entry = it.next();
			if ( entry.getPosition() == 0 ) {
				continue;
			}
			label.append(entry.getPosition()+". "+entry.getSkill().getName()+
					" to Level "+entry.getPlannedToLevel()+
					" ( end: "+toLocalFormat( entry.getEndTime() )+" )");
			
			gotSkillsInQueue = true;
			if ( it.hasNext() ) {
				label.append("\n");
			}
		}
		
		if ( ! gotSkillsInQueue ) {
			label.append(">> No skills in queue <<" );
		}
		setLabel( label.toString() );
	}

	protected String toLocalFormat(EveDate date) {

		if ( date == null ) {
			return "<No date?>";
		}
		
		final DateFormat localFormat =
			DateFormat.getDateTimeInstance();

		return localFormat.format( date.getLocalTime() );
	}


	protected String toLocalTime(EveDate serverTime) {

		if ( serverTime == null ) {
			return "<No date?>";
		}
		
		return toLocalFormat( serverTime );
	}

	protected void setLabel(final String msg) {
		runOnEventThread( new Runnable() {

			@Override
			public void run() {
				textArea.setText( msg );
			}} );
	}
	@Override
	public String getTitle() {
		if ( this.currentCharacter != null ) {
			return "Status for "+this.currentCharacter.getName();
		}
		return super.getTitle();
	}

	protected ICharacter getCurrentCharacter() {
		return currentCharacter;
	}

	public void setSelectionProvider( ISelectionProvider<ICharacter> selectionProvider) {

		assertDetached();

		if ( this.selectionProvider != null ) {
			this.selectionProvider.removeSelectionListener( listener );
		}
		this.selectionProvider = selectionProvider;
	}

}
