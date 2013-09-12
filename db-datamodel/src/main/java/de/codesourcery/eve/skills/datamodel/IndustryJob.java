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
package de.codesourcery.eve.skills.datamodel;

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.utils.EveDate;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class IndustryJob {
	
	/*
      <row jobID="14229974" assemblyLineID="105036" containerID="60013150"
           installedItemID="934765261" installedItemLocationID="983232683"
           installedItemQuantity="1" installedItemProductivityLevel="0"
           installedItemMaterialLevel="0" installedItemLicensedProductionRunsRemaining="1"
           outputLocationID="60013150" installerID="834605870" runs="1"
           licensedProductionRuns="0" installedInSolarSystemID="30000190"
           containerLocationID="30000190" materialMultiplier="1" charMaterialMultiplier="1"
           timeMultiplier="1" charTimeMultiplier="0.8" installedItemTypeID="17721"
           outputTypeID="17720" containerTypeID="3865" installedItemCopy="1" completed="1"
           completedSuccessfully="0" installedItemFlag="4" outputFlag="4" activityID="1"
           completedStatus="1" installTime="2007-11-06 05:18:00"
           beginProductionTime="2007-11-06 05:18:00" endProductionTime="2007-11-06 07:58:00"
           pauseProductionTime="0001-01-01 00:00:00" />
	 */
	
	private long jobId;
	
	private long assemblyLineId;
	
	private ILocation location;
	
	private long containerId;
	
	private Blueprint installedItemId;
	private int installedItemMaterialLevel;
	private int installedItemProductivityLevel;
	
	private int runs;
	private int licensedProductionRuns;
	
	private Activity activity;
	private CompletedStatus completedStatus;
	
	private boolean completed;
	private EveDate beginProductionTime;
	private EveDate endProductionTime;
	
	public static enum CompletedStatus {
		/*
1 = delivered, 2 = aborted, 3 = GM aborted, 4 = inflight unanchored, 5 = destroyed, 0 = failed		 
		 */
		DELIVERED(1),
		ABORTED(2),
		GM_ABORTED(3),
		INFLIGHT_UNANCHORED(4),
		DESTROYED(5),
		FAILED(0);
		
		private final int typeId;
		
		private CompletedStatus(final int typeId) {
			this.typeId =  typeId;
		}
		
		public static CompletedStatus fromTypeId(int id) {
			for ( CompletedStatus s : values() ) {
				if ( s.typeId == id ) {
					return s;
				}
			}
			throw new IllegalArgumentException("Unknown completed_status "+id);
		}
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public long getAssemblyLineId() {
		return assemblyLineId;
	}

	public void setAssemblyLineId(long assemblyLineId) {
		this.assemblyLineId = assemblyLineId;
	}

	public long getContainerId() {
		return containerId;
	}

	public void setContainerId(long containerId) {
		this.containerId = containerId;
	}

	public Blueprint getInstalledBlueprint() {
		return installedItemId;
	}

	public void setInstalledBlueprint(Blueprint installedItemId) {
		this.installedItemId = installedItemId;
	}

	public int getInstalledItemMaterialLevel() {
		return installedItemMaterialLevel;
	}

	public void setInstalledItemMaterialLevel(int installedItemMaterialLevel) {
		this.installedItemMaterialLevel = installedItemMaterialLevel;
	}

	public int getInstalledItemProductivityLevel() {
		return installedItemProductivityLevel;
	}

	public void setInstalledItemProductivityLevel(int installedItemProductivityLevel) {
		this.installedItemProductivityLevel = installedItemProductivityLevel;
	}

	public int getRuns() {
		return runs;
	}

	public void setRuns(int runs) {
		this.runs = runs;
	}

	public int getLicensedProductionRuns() {
		return licensedProductionRuns;
	}

	public void setLicensedProductionRuns(int licensedProductionRuns) {
		this.licensedProductionRuns = licensedProductionRuns;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public CompletedStatus getCompletedStatus() {
		return completedStatus;
	}

	public void setCompletedStatus(CompletedStatus completedStatus) {
		this.completedStatus = completedStatus;
	}

	public boolean hasJobStatus(ISystemClock clock, JobStatus... states) 
	{
		if ( ArrayUtils.isEmpty( states ) ) {
			throw new IllegalArgumentException("NULL/empty states array ?");
		}
		
		for ( JobStatus s: states ) {
			if ( getJobStatus( clock ) == s ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public EveDate getBeginProductionTime() {
		return beginProductionTime;
	}

	public void setBeginProductionTime(EveDate beginProductionTime) {
		this.beginProductionTime = beginProductionTime;
	}

	public EveDate getEndProductionTime() {
		return endProductionTime;
	}

	public void setEndProductionTime(EveDate endProductionTime) {
		this.endProductionTime = endProductionTime;
	}

	public void setLocation(ILocation location) {
		this.location = location;
	}

	public ILocation getLocation() {
		return location;
	}
	
	public static enum JobStatus {
		READY,
		PENDING,
		DELIVERED,
		ABORTED,
		FAILED;
	}
	
	public JobStatus getJobStatus(ISystemClock clock) {
		if ( isCompleted() ) {
			switch( this.completedStatus ) {
				case DELIVERED:
					return JobStatus.DELIVERED;
				case ABORTED:
					return JobStatus.ABORTED;
				default:
					return JobStatus.FAILED;
			}
		} else if ( getEndProductionTime() != null && isExpired( clock ) ) {
			return JobStatus.READY;
		} else  {
			return JobStatus.PENDING;
		}
	}
	
	public boolean isExpired(ISystemClock clock) {
		
		final EveDate now = new EveDate( clock );
		final EveDate endDate = 
			getEndProductionTime();
		
		return endDate != null ? now.after( endDate ) : false;
	}
}
