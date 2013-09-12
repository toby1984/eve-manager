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
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;

import de.codesourcery.eve.apiclient.datamodel.SkillQueueEntry;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class SkillQueueParser extends AbstractResponseParser<List<SkillQueueEntry>>
{

	public static final URI URI = toURI( "/char/SkillQueue.xml.aspx");
	
	private List<SkillQueueEntry> result = new ArrayList<SkillQueueEntry>();
	private final SkillTree skillTree;
	
	/*
<?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2009-03-18 13:19:43</currentTime>
  <result>
    <rowset name="skillqueue" key="queuePosition" columns="queuePosition,typeID,level,startSP,endSP,startTime,endTime">
      <row queuePosition="1" typeID="11441" level="3" startSP="7072" endSP="40000" startTime="2009-03-18 02:01:06" endTime="2009-03-18 15:19:21" />
      <row queuePosition="2" typeID="20533" level="4" startSP="112000" endSP="633542" startTime="2009-03-18 15:19:21" endTime="2009-03-30 03:16:14" />
    </rowset>
  </result>
  <cachedUntil>2009-03-18 13:34:43</cachedUntil>
</eveapi>
	 
	 */
	public SkillQueueParser(ISystemClock clock , SkillTree skillTree) {
		
		super( clock );
		
		if ( skillTree == null ) {
			throw new IllegalArgumentException(
					"skillTree cannot be NULL");
		}
		this.skillTree = skillTree;
	}
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException
	{
		
		for ( Row row : parseRowSet( "skillqueue" , document ) ) {
			final SkillQueueEntry entry =
				new SkillQueueEntry();
			
			entry.setSkill( skillTree.getSkill( row.getInt( "typeID" ) ) );
			
			entry.setStartSkillpoints( row.getInt("startSP" ) );
			entry.setEndSkillpoints( row.getInt( "endSP" ) );
			
			entry.setStartTime( row.getDate("startTime" ) );
			entry.setEndTime( row.getDate( "endTime" ) );
			
			entry.setPlannedToLevel( row.getInt( "level" ) );
			entry.setPosition( row.getInt("queuePosition" ) );
			
			result.add( entry );
		}
		
		Collections.sort( result );
	}

	@Override
	public URI getRelativeURI()
	{
		return URI;
	}

	@Override
	public List<SkillQueueEntry> getResult() throws IllegalStateException
	{
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset()
	{
		result = new ArrayList<SkillQueueEntry>();
	}

}
