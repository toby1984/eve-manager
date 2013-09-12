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
package de.codesourcery.eve.skills.db.datamodel;

import junit.framework.TestCase;
import de.codesourcery.eve.skills.datamodel.TrainedSkill;

public class TrainedSkillTest extends TestCase {

	public void testSkillAtLevel1() {
		
		Skill s = new Skill(1,true);
		s.setName("dummy rank1 skill");
		s.setRank(1);
		
		/*
		switch( lvl ) {
			case 1:
				result = 250;
				break;
			case 2: 
				result = 1414;
				break;
			case 3:
				result = 8000;
				break;
			case 4:
				result = 45255;
				break;
			case 5:
				result = 256000;
				break;
			default:
				throw new IllegalArgumentException("Invalid level "+lvl);
		}
		
		skillpoints = result*rank;		 
		 */
		
		final TrainedSkill trained = new TrainedSkill( s , 250 );
		assertEquals( 1 , trained.getLevel() );
		assertFalse( trained.isPartiallyTrained() );
		assertEquals( 100.0f , trained.getFractionOfLevelTrained(1) );
		assertEquals( 0.0f , trained.getFractionOfLevelTrained(2) );		
	}
	
	public void testSkillAtLevel5() {
		
		Skill s = new Skill(1,true);
		s.setName("dummy rank1 skill");
		s.setRank(1);
		
		/*
		switch( lvl ) {
			case 1:
				result = 250;
				break;
			case 2: 
				result = 1414;
				break;
			case 3:
				result = 8000;
				break;
			case 4:
				result = 45255;
				break;
			case 5:
				result = 256000;
				break;
			default:
				throw new IllegalArgumentException("Invalid level "+lvl);
		}
		
		skillpoints = result*rank;		 
		 */
		
		final TrainedSkill trained = new TrainedSkill( s , 256000 );
		assertEquals( 5 , trained.getLevel() );
		assertFalse( trained.isPartiallyTrained() );
		assertEquals( 100.0f , trained.getFractionOfLevelTrained(1) );
	}	
	
	public void testSkillPartiallyTrainedToLevel1() {
		
		Skill s = new Skill(1,true);
		s.setName("dummy rank1 skill");
		s.setRank(1);
		
		/*
		switch( lvl ) {
			case 1:
				result = 250;
				break;
			case 2: 
				result = 1414;
				break;
			case 3:
				result = 8000;
				break;
			case 4:
				result = 45255;
				break;
			case 5:
				result = 256000;
				break;
			default:
				throw new IllegalArgumentException("Invalid level "+lvl);
		}
		
		skillpoints = result*rank;		 
		 */
		
		final TrainedSkill trained = new TrainedSkill( s , 125 );
		assertEquals( 0 , trained.getLevel() );
		assertTrue( trained.isPartiallyTrained() );
		assertTrue( trained.isPartiallyTrained(1) );
		assertEquals( 50.0f , trained.getFractionOfLevelTrained(1) );
	}	
	
	public void testSkillPartiallyTrainedToLevel3() {
		
		Skill s = new Skill(1,true);
		s.setName("dummy rank1 skill");
		s.setRank(1);
		
		/*
		switch( lvl ) {
			case 1:
				result = 250;
				break;
			case 2: 
				result = 1414;
				break;
			case 3:
				result = 8000;
				break;
			case 4:
				result = 45255;
				break;
			case 5:
				result = 256000;
				break;
			default:
				throw new IllegalArgumentException("Invalid level "+lvl);
		}
		
		skillpoints = result*rank;		 
		 */
		
		final TrainedSkill trained = new TrainedSkill( s , 1414 + Math.round( (8000-1414) / 2.0f ) );
		assertEquals( 2 , trained.getLevel() );
		assertTrue( trained.isPartiallyTrained() );
		assertTrue( trained.isPartiallyTrained(3) );
		assertFalse( trained.isPartiallyTrained(2) );
		assertFalse( trained.isPartiallyTrained(4) );
		assertEquals( 50.0f , trained.getFractionOfLevelTrained(3) );
	}	
	
	public void testSkillPartiallyTrainedToLevel5() {
		
		Skill s = new Skill(1,true);
		s.setName("dummy rank1 skill");
		s.setRank(1);
		
		/*
		switch( lvl ) {
			case 1:
				result = 250;
				break;
			case 2: 
				result = 1414;
				break;
			case 3:
				result = 8000;
				break;
			case 4:
				result = 45255;
				break;
			case 5:
				result = 256000;
				break;
			default:
				throw new IllegalArgumentException("Invalid level "+lvl);
		}
		
		skillpoints = result*rank;		 
		 */
		
		final TrainedSkill trained = new TrainedSkill( s , 45255 + Math.round( (256000-45255) / 2.0f ) );
		assertEquals( 4 , trained.getLevel() );
		assertTrue( trained.isPartiallyTrained() );
		assertFalse( trained.isPartiallyTrained(3) );
		assertTrue( trained.isPartiallyTrained(5) );
		assertEquals( 50.0f , trained.getFractionOfLevelTrained(5) , 0.001);
	}	
}
