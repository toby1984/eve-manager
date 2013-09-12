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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.annotation.Resource;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.log4j.Logger;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IndustryJob;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.datamodel.IndustryJob.JobStatus;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.components.impl.IndustryJobsTableBuilder.JobWrapper;
import de.codesourcery.eve.skills.ui.model.AbstractViewFilter;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.model.IViewFilter;
import de.codesourcery.eve.skills.ui.renderer.EveDateTableRenderer;
import de.codesourcery.eve.skills.ui.utils.UITask;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class IndustryJobStatusComponent extends AbstractComponent implements ISelectionListener<ICharacter>,ICharacterSelectionProviderAware {

	public static final Logger log = Logger
	.getLogger(IndustryJobStatusComponent.class);

	private volatile ICharacter selectedCharacter;

	private volatile ISelectionProvider<ICharacter> charSelectionProvider = null;

	@Resource(name="api-client")
	private IAPIClient apiClient;

	@Resource(name="useraccount-store")
	private IUserAccountStore accountStore;
	
	@Resource(name="system-clock")
	private ISystemClock clock;	

	// needs to be volatile, accessed from EDT and other threads
	private volatile APIResponse<Collection<IndustryJob>> jobs=null;
	private final JTable table;
	private final IndustryJobsTableBuilder tableBuilder;
	
	private final JComboBox stateFilterCombobox = new JComboBox();
	
	protected enum StateFilter {
		ANY("Any state") {
			@Override
			protected boolean isHidden(ISystemClock clock , IndustryJob job) { return false; }
		},
		ONLY_ACTIVE("Any active state") {

			@Override
			protected boolean isHidden(ISystemClock clock , IndustryJob job)
			{
				return ! job.hasJobStatus( clock, 
						JobStatus.READY , JobStatus.PENDING );
			}
		} ,
		ONLY_NON_ACTIVE("Any non-active state") {

			@Override
			protected boolean isHidden(ISystemClock clock, IndustryJob job)
			{
				return ! ONLY_ACTIVE.isHidden( clock , job );
			}
		};
		
		private final String displayName;
		
		private StateFilter(String displayName) {
			this.displayName = displayName;
		}
		
		protected abstract boolean isHidden(ISystemClock clock , IndustryJob job);
		
		@Override
		public String toString() { return displayName; } 
		
	}
	private final IViewFilter<JobWrapper> viewFilter = new AbstractViewFilter<JobWrapper>() {

		@Override
		public boolean isHiddenUnfiltered(JobWrapper value)
		{
			return getIndustryJobStateFilter().isHidden( clock,value.getJob() );
		}
	};

	protected StateFilter getIndustryJobStateFilter() {
		return (StateFilter) this.stateFilterCombobox.getSelectedItem();
	}

	public IndustryJobStatusComponent() {
		tableBuilder = new IndustryJobsTableBuilder( clock ); // 'clock' field gets initialized by super constructor!
		table = new JTable( tableBuilder.getModel() );
		table.setRowSorter( tableBuilder.getModel().getRowSorter() );
		tableBuilder.getModel().setViewFilter( viewFilter );
		table.setDefaultRenderer( EveDate.class  , new EveDateTableRenderer() );
	}

	@Override
	protected JPanel createPanel() {

		final JPanel result =
			new JPanel();
		
		result.setLayout( new GridBagLayout() );
		
		stateFilterCombobox.setModel( 
			new DefaultComboBoxModel<StateFilter>( StateFilter.values() ) 
		);
		
		stateFilterCombobox.setSelectedItem( StateFilter.ONLY_ACTIVE );
		stateFilterCombobox.addItemListener( new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e)
			{
				if ( e.getStateChange() == ItemEvent.SELECTED ) 
				{
					tableBuilder.getModel().viewFilterChanged();
				}
			}
		});
		
		result.add( new JLabel("Displayed jobs:") , constraints(0,0).anchorWest().noResizing().end() );
		result.add( stateFilterCombobox, constraints(1,0).anchorWest().noResizing().end() );
		result.add( new JScrollPane( table ) , constraints(0,1).width(2).resizeBoth().end() );
		return result;
	}

	@Override
	protected void onAttachHook(IComponentCallback callback) {
		if ( this.charSelectionProvider != null ) {
			this.charSelectionProvider.addSelectionListener( this );
			final ICharacter selected= this.charSelectionProvider.getSelectedItem();
			selectionChanged( selected );
		}
	}

	@Override
	protected void onDetachHook() {
		if ( this.charSelectionProvider != null ) {
			this.charSelectionProvider.removeSelectionListener( this );
		}
	}

	public void setSelectionProvider(ISelectionProvider<ICharacter> prov) {

		assertDetached();
		this.charSelectionProvider = prov;
		this.jobs = null;
	}

	protected void update() {
		
		final Collection<IndustryJob> jobs = 
			this.jobs != null ? this.jobs.getPayload() : new LinkedList<IndustryJob>();

		log.debug("update(): Job count = "+jobs.size() );
		tableBuilder.setJobs( jobs );
	}

	protected static void writeTitle(StringBuilder builder,String title) {
		
		final int len = title.length();
		builder.append( repeat( '*' , len + 4 ) ).append("\n");
		builder.append("* ").append( title ).append(" *\n");
		builder.append( repeat( '*' , len + 4 ) ).append("\n");
	}

	protected static String repeat(char c , int times) {
		final StringBuilder result = new StringBuilder();
		for ( int i = 0 ; i < times ; i++ ) {
			result.append( c );
		}
		return result.toString();
	}
	
	@Override
	public void selectionChanged(final ICharacter selected) {

		log.debug("selectionChanged(): selected = "+selected);

		if ( selected == null ) {
			if ( this.selectedCharacter != null ) {

				this.selectedCharacter = null;
				this.jobs = null;

				update();
			} else {
				selectedCharacter = null;
			}
			return;
		}

		// a character has been selected...
		if ( this.selectedCharacter != null && 
				selectedCharacter.getCharacterId().equals( selected.getCharacterId() ) ) 
		{
			return; // same character
		}

		this.selectedCharacter = selected;
		this.jobs = null;

		submitTask( new UITask() {

			@Override
			public String getId() {
				return "query_industry_jobs_"+selected.getCharacterId().getValue();
			}

			@Override
			public void run() throws Exception {

				log.debug("run(): Querying industry jobs for "+selected);
				displayStatus("Querying industry jobs for "+selected.getName());
				
				final UserAccount userAccount = 
					accountStore.getAccountByCharacterID( selected.getCharacterId() );

				jobs =
					apiClient.getCharacterIndustryJobs( selected,
							userAccount , RequestOptions.DEFAULT );

				update();
			}

			@Override
			public void successHook() throws Exception {
			}

			@Override
			public void failureHook(Throwable t) throws Exception {
				displayError("Failed to query industry jobs from server: "+
						( t != null ? t.getMessage(): "" ) , t );
			}
		} , true );
	}

}
