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

import java.util.Date;
import java.util.List;

import de.codesourcery.eve.apiclient.InternalAPIResponse;
import de.codesourcery.eve.apiclient.datamodel.SkillQueueEntry;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.SkillGroup;

public class SkillQueueParserTest extends AbstractParserTest
{
	
	private static final String XML = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
			"<eveapi version=\"2\">\n" + 
			"  <currentTime>2009-03-18 13:19:43</currentTime>\n" + 
			"  <result>\n" + 
			"    <rowset name=\"skillqueue\" key=\"queuePosition\" columns=\"queuePosition,typeID,level,startSP,endSP,startTime,endTime\">\n" + 
			"      <row queuePosition=\"1\" typeID=\"11441\" level=\"3\" startSP=\"7072\" endSP=\"40000\" startTime=\"2009-03-18 02:01:06\" endTime=\"2009-03-18 15:19:21\" />\n" + 
			"      <row queuePosition=\"2\" typeID=\"20533\" level=\"4\" startSP=\"112000\" endSP=\"633542\" startTime=\"2009-03-18 15:19:21\" endTime=\"2009-03-30 03:16:14\" />\n" + 
			"    </rowset>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2009-03-18 13:34:43</cachedUntil>\n" + 
			"</eveapi>";
	
	public void testParsing() {
		
		final SkillTree skillTree = new SkillTree();
		
		final SkillGroup skillGroup = skillTree.getOrCreateSkillGroup(1);
		
		final Skill s1 = skillTree.getOrCreateSkill( 11441 , true );
		s1.setName( "Skill #11441" );
		s1.setRank(1);
		
		skillGroup.addSkill( s1 );
		
		final Skill s2 = skillTree.getOrCreateSkill( 20533 , true );
		s2.setName( "Skill #20533" );
		s2.setRank(1);
		
		skillGroup.addSkill( s2 );
		
		skillTree.validate();
		
		final SkillQueueParser parser = 
			new SkillQueueParser( systemClock() , skillTree );
		
		final InternalAPIResponse response = parser.parse( new Date() , XML );
		
		assertNotNull( response );
		List<SkillQueueEntry> skillQueue = parser.getResult();
		assertNotNull( skillQueue );
		assertEquals( 2 , skillQueue.size() );
		
		SkillQueueEntry e1 = skillQueue.get(0);
		assertEquals( 11441 , e1.getSkill().getTypeId() );
		assertEquals( 3 , e1.getPlannedToLevel() );
		assertEquals( 7072 , e1.getStartSkillpoints() );
		assertEquals( 40000 , e1.getEndSkillpoints() );
		assertEquals( createDate("2009-03-18 02:01:06") , e1.getStartTime() );
		assertEquals( createDate("2009-03-18 15:19:21") , e1.getEndTime() );
		assertEquals( 1 , e1.getPosition() );
		
		SkillQueueEntry e2 = skillQueue.get(1);
		assertEquals( 20533 , e2.getSkill().getTypeId() );
		assertEquals( 4 , e2.getPlannedToLevel() );
		assertEquals( 112000 , e2.getStartSkillpoints() );
		assertEquals( 633542 , e2.getEndSkillpoints() );
		assertEquals( createDate("2009-03-18 15:19:21") , e2.getStartTime() );
		assertEquals( createDate("2009-03-30 03:16:14") , e2.getEndTime() );
		assertEquals( 2 , e2.getPosition() );		
		
	}
}
