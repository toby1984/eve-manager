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
package de.codesourcery.eve.apiclient.parsers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.IndustryJob;
import de.codesourcery.eve.skills.datamodel.IndustryJob.CompletedStatus;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class CharacterIndustryJobsParser extends AbstractResponseParser<Collection<IndustryJob>> {

	public static final Logger log = Logger
			.getLogger(CharacterIndustryJobsParser.class);
	
	private static final URI uri = URI.create( "/char/IndustryJobs.xml.aspx" );
	
	private final Collection<IndustryJob> result = new ArrayList<IndustryJob>();
	private final IStaticDataModel dataModel;
	private final AssetList assets;
	
	public CharacterIndustryJobsParser(AssetList assets, IStaticDataModel model,ISystemClock clock) {
		super( clock );
		this.dataModel = model;
		this.assets = assets;
	}
	
	/*
<?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2008-05-25 20:23:34</currentTime>
  <result>
    <rowset name="jobs" key="jobID" columns="jobID,assemblyLineID,containerID,
            installedItemID,installedItemLocationID,installedItemQuantity,
            installedItemProductivityLevel,installedItemMaterialLevel,
            installedItemLicensedProductionRunsRemaining,outputLocationID,
            installerID,runs,licensedProductionRuns,installedInSolarSystemID,
            containerLocationID,materialMultiplier,charMaterialMultiplier,
            timeMultiplier,charTimeMultiplier,installedItemTypeID,outputTypeID,
            containerTypeID,installedItemCopy,completed,completedSuccessfully,
            installedItemFlag,outputFlag,activityID,completedStatus,installTime,
            beginProductionTime,endProductionTime,pauseProductionTime">
      <row jobID="23264063" assemblyLineID="100518790" containerID="1386493620"
           installedItemID="1002502594" installedItemLocationID="199583646"
           installedItemQuantity="1" installedItemProductivityLevel="12"
           installedItemMaterialLevel="40" installedItemLicensedProductionRunsRemaining="-1"
           outputLocationID="1386493620" installerID="674831735" runs="6"
           licensedProductionRuns="15" installedInSolarSystemID="30005005"
           containerLocationID="30005005" materialMultiplier="1" charMaterialMultiplier="1"
           timeMultiplier="0.65" charTimeMultiplier="1.5" installedItemTypeID="971"
           outputTypeID="971" containerTypeID="28351" installedItemCopy="0" completed="0"
           completedSuccessfully="0" installedItemFlag="121" outputFlag="120" activityID="5"
           completedStatus="0" installTime="2008-05-23 00:38:00"
           beginProductionTime="2008-05-23 00:38:00" endProductionTime="2008-06-08 16:47:00"
           pauseProductionTime="0001-01-01 00:00:00" />
	 
	 */
	@Override
	void parseHook(Document document) throws UnparseableResponseException {
		
		final RowSet rowSet = parseRowSet( "jobs" , document );
		
		for ( Row row : rowSet ) {
	
			final IndustryJob job = new IndustryJob();
			
			job.setActivity( Activity.fromTypeId( row.getInt( "activityID" )  ) );
			job.setAssemblyLineId( row.getLong("assemblyLineID" ) );
			job.setBeginProductionTime( row.getDate( "beginProductionTime" ) );
			job.setCompletedStatus( CompletedStatus.fromTypeId( row.getInt( "completedStatus"  ) ) );
			job.setCompleted( row.getInt( "completed" ) != 0 );
			job.setContainerId( row.getLong("containerID" ) );
			final long blueprintId = row.getLong( "installedItemTypeID" );
			
			try {
				job.setInstalledBlueprint( this.dataModel.getBlueprint( dataModel.getInventoryType( blueprintId ) ) );
			} 
			catch(Exception e) {
				log.error("parseHook(): Discarding job with unknown blueprint type ID "+blueprintId);
				continue;
			}
			
			/*
If the container is a station (see containerTypeID, below), 
this is the stationID in the staStations table. 
For a POS module, this is its itemID (see also the Corporation Asset List API page). 			 
			 */
			final long locationId =
				row.getLong( "containerID" );
			
			try {
				final Station station = dataModel.getStation( locationId );
				job.setLocation( station );
			} 
			catch(Exception e) {
				log.error("parseHook(): Failed to find location for container ID "+locationId);
				job.setLocation( ILocation.UNKNOWN_LOCATION );
			}
			
			job.setInstalledItemMaterialLevel( row.getInt( "installedItemMaterialLevel" ) );
			job.setInstalledItemProductivityLevel( row.getInt( "installedItemProductivityLevel" ) );
			job.setJobId( row.getLong("jobID" ) );
			job.setLicensedProductionRuns( row.getInt("licensedProductionRuns" ) );
			job.setRuns( row.getInt("runs" ) );
			job.setEndProductionTime( row.getDate( "endProductionTime" ) );
			
			// add parsed job to result
			result.add( job );
		}
	}

	@Override
	public URI getRelativeURI() {
		return uri;
	}

	@Override
	public Collection<IndustryJob> getResult() throws IllegalStateException {
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset() {
		result.clear();
	}

}
