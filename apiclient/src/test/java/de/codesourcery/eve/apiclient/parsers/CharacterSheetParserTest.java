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

import de.codesourcery.eve.skills.datamodel.AttributeEnhancer;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.datamodel.CharacterDetails.Gender;
import de.codesourcery.eve.skills.datamodel.CharacterDetails.Race;
import de.codesourcery.eve.skills.db.datamodel.AttributeType;
import de.codesourcery.eve.skills.db.datamodel.Corporation;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.SkillGroup;

public class CharacterSheetParserTest extends AbstractParserTest {

	private static final String XML = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
			"<eveapi version=\"2\">\n" + 
			"  <currentTime>2007-06-18 22:49:01</currentTime>\n" + 
			"  <result>\n" + 
			"    <characterID>150337897</characterID>\n" + 
			"    <name>corpslave</name>\n" + 
			"    <race>Minmatar</race>\n" + 
			"    <bloodLine>Brutor</bloodLine>\n" + 
			"    <gender>Female</gender>\n" + 
			"    <corporationName>corpexport Corp</corporationName>\n" + 
			"    <corporationID>150337746</corporationID>\n" + 
			"    <cloneName>Clone Grade Pi</cloneName>\n" + 
			"    <cloneSkillPoints>54600000</cloneSkillPoints>\n" + 
			"    <balance>190210393.87</balance>\n" + 
			"    <attributeEnhancers>\n" + 
			"      <intelligenceBonus>\n" + 
			"        <augmentatorName>Snake Delta</augmentatorName>\n" + 
			"        <augmentatorValue>3</augmentatorValue>\n" + 
			"      </intelligenceBonus>\n" + 
			"      <memoryBonus>\n" + 
			"        <augmentatorName>Halo Beta</augmentatorName>\n" + 
			"        <augmentatorValue>3</augmentatorValue>\n" + 
			"      </memoryBonus>\n" + 
			"    </attributeEnhancers>\n" + 
			"    <attributes>\n" + 
			"      <intelligence>6</intelligence>\n" + 
			"      <memory>4</memory>\n" + 
			"      <charisma>7</charisma>\n" + 
			"      <perception>12</perception>\n" + 
			"      <willpower>10</willpower>\n" + 
			"    </attributes>\n" + 
			"    <rowset name=\"skills\" key=\"typeID\" >\n" + 
			"      <row typeID=\"3431\" skillpoints=\"8000\" level=\"3\"/>\n" + 
			"      <row typeID=\"3416\" skillpoints=\"3000\" level=\"1\"/>\n" + 
			"      <row typeID=\"3445\" skillpoints=\"277578\" unpublished=\"1\"/>\n" + 
			"    </rowset>\n" + 
			"    <rowset name=\"certificates\" key=\"certificateID\" columns=\"certificateID\">\n" + 
			"      <row certificateID=\"1\"/>\n" + 
			"      <row certificateID=\"5\"/>\n" + 
			"      <row certificateID=\"19\"/>\n" + 
			"      <row certificateID=\"239\"/>\n" + 
			"      <row certificateID=\"282\"/>\n" + 
			"      <row certificateID=\"32\"/>\n" + 
			"      <row certificateID=\"258\"/>\n" + 
			"    </rowset>\n" + 
			"    <rowset name=\"corporationRoles\" key=\"roleID\" columns=\"roleID,roleName\">\n" + 
			"      <row roleID=\"1\" roleName=\"roleDirector\" />\n" + 
			"    </rowset>\n" + 
			"    <rowset name=\"corporationRolesAtHQ\" key=\"roleID\" columns=\"roleID,roleName\">\n" + 
			"      <row roleID=\"1\" roleName=\"roleDirector\" />\n" + 
			"    </rowset>\n" + 
			"    <rowset name=\"corporationRolesAtBase\" key=\"roleID\" columns=\"roleID,roleName\">\n" + 
			"      <row roleID=\"1\" roleName=\"roleDirector\" />\n" + 
			"    </rowset>\n" + 
			"    <rowset name=\"corporationRolesAtOther\" key=\"roleID\" columns=\"roleID,roleName\">\n" + 
			"      <row roleID=\"1\" roleName=\"roleDirector\" />\n" + 
			"    </rowset>\n" + 
			"    <rowset name=\"corporationTitles\" key=\"titleID\" columns=\"titleID,titleName\">\n" + 
			"      <row titleID=\"1\" titleName=\"Member\" />\n" + 
			"    </rowset>\n" + 
			"  </result>\n" + 
			"  <cachedUntil>2007-06-18 23:49:01</cachedUntil>\n" + 
			"</eveapi>\n" + 
			"";
	
	public void testParser() {
		
		final SkillTree skillTree = new SkillTree();
		
		final SkillGroup skillGroup = skillTree.getOrCreateSkillGroup(1);
		
		Skill s = skillTree.getOrCreateSkill( 3431 , true );
		s.setRank(1);
		
		skillGroup.addSkill( s );
		
		s = skillTree.getOrCreateSkill( 3416 , true );
		s.setRank(1);
		
		skillGroup.addSkill( s );
		
		s = skillTree.getOrCreateSkill( 3445 , true );
		s.setRank(1);		
		skillGroup.addSkill( s );
		
		skillTree.validate();
		
		final IStaticDataModel provider =
			EasyMock.createMock( IStaticDataModel.class );
		
		EasyMock.expect( provider.getSkillTree() ).andReturn( skillTree ).atLeastOnce();
		
		EasyMock.replay( provider );
		
		final CharacterSheetParser parser =
			new CharacterSheetParser( provider , systemClock() );
		
		final Date now = new Date();
		parser.parse( now , XML );
		
		final ICharacter result = parser.getResult();
		assertNotNull( result );
		
		// validate misc data
		assertEquals( "150337897" , result.getCharacterId().getValue() );
		assertEquals("corpslave" , result.getName() );
		assertEquals( Race.MINMATAR , result.getCharacterDetails().getRace() );
		assertEquals( "Brutor" , result.getCharacterDetails().getBloodLine() );
		assertEquals( Gender.FEMALE , result.getCharacterDetails().getGender() );
		
		final Corporation corp = 
			result.getCharacterDetails().getCorporation();
		
		assertNotNull( corp );
		
		assertEquals( "corpexport Corp" , corp.getName() );
		assertNotNull( corp.getId() );
		assertEquals( new Long(150337746L) , (Long) corp.getId().getValue() );
		assertEquals( "Clone Grade Pi" , result.getCharacterDetails().getCloneName() );
		assertEquals( 54600000 , result.getCharacterDetails().getCloneSkillPoints() );
		assertEquals( 19021039387L , result.getCharacterDetails().getBalance() );
		
		// validate attribute enhancers
		AttributeEnhancer enhancer =
			result.getImplantSet().getAttributeEnhancer( AttributeType.INTELLIGENCE );

		assertNotNull( enhancer );
		assertEquals( 3 , enhancer.getModifier() );
		
		enhancer =
			result.getImplantSet().getAttributeEnhancer( AttributeType.MEMORY );

		assertNotNull( enhancer );
		assertEquals( 3 , enhancer.getModifier() );
		
		// validate attributes
		assertEquals( 6.0f , result.getAttributes().getBaseValue( AttributeType.INTELLIGENCE ) );
		assertEquals( 4.0f , result.getAttributes().getBaseValue( AttributeType.MEMORY ) );
		assertEquals( 7.0f , result.getAttributes().getBaseValue( AttributeType.CHARISMA) );
		assertEquals( 12.0f , result.getAttributes().getBaseValue( AttributeType.PERCEPTION) );
		assertEquals( 10.0f , result.getAttributes().getBaseValue( AttributeType.WILLPOWER) );
		
		// validate trained skills
		assertEquals( 8000 , result.getCurrentSkillPoints( skillTree.getSkill( 3431 ) ) );
		assertEquals( 3000 , result.getCurrentSkillPoints( skillTree.getSkill( 3416 ) ) );
		assertEquals( 0 , result.getCurrentSkillPoints( skillTree.getSkill( 3445 ) ) );
		
		// common data
		assertNull( parser.getError() );
		assertEquals( 2 , parser.getAPIVersion() );
		assertEquals( createDate( "2007-06-18 22:49:01" ) , parser.getServerTime() );
		assertEquals( createDate( "2007-06-18 23:49:01" ) , parser.getCachedUntilServerTime() );	
		
		EasyMock.verify( provider );
	}
}
