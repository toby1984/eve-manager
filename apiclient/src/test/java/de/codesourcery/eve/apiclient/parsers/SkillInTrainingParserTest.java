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

import org.easymock.classextension.EasyMock;

import de.codesourcery.eve.apiclient.datamodel.SkillInTraining;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.SkillGroup;

public class SkillInTrainingParserTest extends AbstractParserTest {
	
	private static final String XML_IN_TRAINING = "<eveapi version=\"2\">\n" + 
			"  <currentTime>2008-08-17 06:43:00</currentTime>\n" + 
			"  <result>\n" + 
			"    <currentTQTime offset=\"0\">2008-08-17 06:43:00</currentTQTime>\n" + 
			"    <trainingEndTime>2008-08-17 15:29:44</trainingEndTime>\n" + 
			"    <trainingStartTime>2008-08-15 04:01:16</trainingStartTime>\n" + 
			"    <trainingTypeID>3305</trainingTypeID>\n" + 
			"    <trainingStartSP>24000</trainingStartSP>\n" + 
			"    <trainingDestinationSP>135765</trainingDestinationSP>\n" + 
			"    <trainingToLevel>4</trainingToLevel>\n" + 
			"    <skillInTraining>1</skillInTraining>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2008-08-17 06:58:00</cachedUntil>\n" + 
			"</eveapi>\n"; 

	public static final String XML_NO_TRAINING = "<eveapi version=\"1\">\n" + 
			"  <currentTime>2007-06-21 10:57:10</currentTime>\n" + 
			"  <result>\n" + 
			"    <skillInTraining>0</skillInTraining>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2007-06-21 10:57:20</cachedUntil>\n" + 
			"</eveapi>\n";
	
	private static IStaticDataModel createDAOProvider() {
		final SkillTree skillTree = new SkillTree();
		
		final SkillGroup skillGroup = skillTree.getOrCreateSkillGroup(1);
		
		Skill s = skillTree.getOrCreateSkill( 3305 , true );
		s.setRank(1);
		
		skillGroup.addSkill( s );
		
		skillTree.validate();
		
		final IStaticDataModel provider =
			EasyMock.createMock( IStaticDataModel.class );
		
		EasyMock.expect( provider.getSkillTree() ).andReturn( skillTree ).atLeastOnce();
		
		EasyMock.replay( provider );
		return provider;
	}
	
	public void testSkillInTraining() throws Exception {
		
		final SkillInTrainingParser  parser =
			new SkillInTrainingParser( createDAOProvider() , systemClock());
		
		parser.parse( new Date() , XML_IN_TRAINING );
		
		final SkillInTraining result = parser.getResult();
		
		assertNotNull( result );
		assertEquals( createDate( "2008-08-17 06:43:00") , result.getCurrentTQTime() );
		assertEquals( createDate( "2008-08-17 15:29:44<") , result.getTrainingEndTime() );
		assertEquals( createDate( "2008-08-15 04:01:16") , result.getTrainingStartTime() );
		assertEquals( 24000 , result.getTrainingStartSP() );
		assertEquals( 135765 , result.getTrainingDestinationSP() );
		assertTrue( result.getSkill() instanceof Skill );
		assertEquals( 4 , result.getPlannedLevel() );
		assertTrue( result.isSkillInTraining() );
	}
	
	public void testNoSkillInTraining() throws Exception {
		
		final SkillInTrainingParser  parser =
			new SkillInTrainingParser( createDAOProvider() , systemClock() );
		
		parser.parse( new Date() , XML_NO_TRAINING );
		
		final SkillInTraining result = parser.getResult();
		
		assertNotNull( result );
		assertFalse( result.isSkillInTraining() );
	}
}
