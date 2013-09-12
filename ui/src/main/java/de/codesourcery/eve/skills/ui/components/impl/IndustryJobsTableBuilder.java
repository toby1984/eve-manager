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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.IndustryJob;
import de.codesourcery.eve.skills.datamodel.IndustryJob.JobStatus;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.utils.DateHelper;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class IndustryJobsTableBuilder {
	
	public static final Logger log = Logger
			.getLogger(IndustryJobsTableBuilder.class);

	private final AbstractTableModel<JobWrapper> model;
	
	private volatile List<JobWrapper> jobs =
		new ArrayList<JobWrapper>();
	
	public AbstractTableModel<JobWrapper> getModel() {
		return model;
	}

	private final ISystemClock clock;
	
	public IndustryJobsTableBuilder(ISystemClock clock) {
		if ( clock == null ) {
			throw new IllegalArgumentException("clock cannot be NULL");
		}
		this.clock = clock;
		this.model = createModel();
	}
	
	public void setJobs(Collection<IndustryJob> newJobs) {
		
		final List<JobWrapper> tmp =
			new ArrayList<JobWrapper>();
		
		if ( newJobs != null ) {
			for ( IndustryJob j : newJobs ) {
				tmp.add( new JobWrapper( j ) );
			}
		}
		
		this.jobs = tmp;
		model.modelDataChanged();
		log.debug("setJobs(): new model row count = "+model.getRowCount()); 
	}
	
	private AbstractTableModel<JobWrapper> createModel() {
		
		final TableColumnBuilder builder = new TableColumnBuilder();
		builder.add("Activity")
		.add("Location")
		.add("Status")
		.add("Blueprint")
		.add("Runs",Integer.class)
		.add("Start date",EveDate.class)
		.add("End date", EveDate.class);
		
		return new AbstractTableModel<JobWrapper>( builder ) {

			@Override
			protected Object getColumnValueAt(int modelRowIndex,
					int modelColumnIndex) 
			{
				
				final JobWrapper wrapper =
					getRow( modelRowIndex );
				
				switch( modelColumnIndex ) {
					case 0:
						return wrapper.getActivity();
					case 1:
						return wrapper.getLocation();
					case 2:
						return wrapper.getStatus();
					case 3:
						return wrapper.getBlueprintName();
					case 4:
						return wrapper.getRuns();
					case 5:
						return wrapper.getStartTime();
					case 6:
						return wrapper.getEndTime();
						default:
							throw new RuntimeException("Internal error, invalid model column index "+modelColumnIndex);
				}
			}

			@Override
			public JobWrapper getRow(int modelRow) {
				JobWrapper result =
					jobs.get( modelRow );
				if ( result == null ) {
					throw new RuntimeException("Internal error , invalid model row index: "+modelRow);
				}
				return result;
			}

			@Override
			public int getRowCount() {
				return jobs.size();
			}
		};
	}
	
	public final class JobWrapper {
		
		private final IndustryJob job;
		
		protected JobWrapper(IndustryJob j) {
			if (j == null) {
				throw new IllegalArgumentException("job cannot be NULL");
			}
			this.job = j;
		}
		
		public IndustryJob getJob() {
			return job;
		}
		
		public String getStatus() {
			
			final JobStatus jobStatus =
				job.getJobStatus( clock );
			
			if ( job.isCompleted() ) {
				return job.getCompletedStatus().toString();
			} else if ( jobStatus == JobStatus.READY ) {
				return ">> READY <<";
			} else if ( jobStatus == JobStatus.PENDING ) {
				return "PENDING";
			} else if ( jobStatus == JobStatus.FAILED ) {
				return "FAILED";
			} 
			return "<unhandled: "+jobStatus+">";
		}
		
		public String getLocation() {
			return job.getLocation().getDisplayName();
		}
		
		public String getBlueprintName() {
			return job.getInstalledBlueprint().getName();
		}
		
		public int getRuns() {
			return job.getRuns();
		}
		
		public EveDate getStartTime() {
			return job.getBeginProductionTime();
		}
		
		public EveDate getEndTime() {
			return job.getEndProductionTime();
		}		
		
		public String getActivity() {
			return job.getActivity().getName();
		}
		
	}
	
	protected Date toLocalTime(Date serverTime) {
		return DateHelper.toLocalTime( serverTime , clock );
	}
}
