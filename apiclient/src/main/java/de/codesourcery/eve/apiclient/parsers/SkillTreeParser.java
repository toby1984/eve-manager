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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.db.datamodel.AttributeType;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.SkillGroup;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class SkillTreeParser extends AbstractResponseParser<SkillTree> {

	private static final Logger LOG = Logger.getLogger(SkillTreeParser.class);
	
	public static final boolean DEBUG_PARSING = true;
	
	private static final URI URI = toURI("/eve/SkillTree.xml.aspx");
	
	private SkillTree skillTree =
		new SkillTree();
	
	public SkillTreeParser(ISystemClock clock) {
		super(clock);
	}
	
	/*
  <?xml version='1.0' encoding='utf-8'?>
  <eveapi version="2">
  <currentTime>2009-05-31 19:28:05</currentTime>
  <result>
	<rowset name="skillGroups" key="groupID" columns="groupName,groupID">
      <row groupName="Corporation Management" groupID="266">
        <rowset name="skills" key="typeID" columns="typeName,groupID,typeID">
          <row typeName="Anchoring" groupID="266" typeID="11584">
            <description>Skill at Anchoring Deployables. Can not be trained on Trial Accounts.</description>
            <rank>3</rank>
            <rowset name="requiredSkills" key="typeID" columns="typeID,skillLevel" />
            <requiredAttributes>
              <primaryAttribute>memory</primaryAttribute>
              <secondaryAttribute>charisma</secondaryAttribute>
            </requiredAttributes>
            <rowset name="skillBonusCollection" key="bonusType" columns="bonusType,bonusValue">
              <row bonusType="canNotBeTrainedOnTrial" bonusValue="1" />
            </rowset>
          </row>
          <row typeName="Repair Drone Operation" groupID="273" typeID="3439">
          <description>Allows operation of logistic drones. 5% increased repair amount per level.</description>
          <rank>3</rank>
          <rowset name="requiredSkills" key="typeID" columns="typeID,skillLevel">
            <row typeID="3436" skillLevel="5" />
            <row typeID="23618" skillLevel="1" />
          </rowset>
          <requiredAttributes>
            <primaryAttribute>memory</primaryAttribute>
            <secondaryAttribute>perception</secondaryAttribute>
          </requiredAttributes>
          <rowset name="skillBonusCollection" key="bonusType" columns="bonusType,bonusValue">
            <row bonusType="damageHP" bonusValue="5" />
          </rowset>
          </row>
        

	 */
	@Override
	protected void parseHook(Document document)
			throws UnparseableResponseException 
	{
		
		final Element result = getResultElement( document );
		
		/*
	<rowset name="skillGroups" key="groupID" columns="groupName,groupID">
      <row groupName="Corporation Management" groupID="266">
        <rowset name="skills" key="typeID" columns="typeName,groupID,typeID">
          <row typeName="Anchoring" groupID="266" typeID="11584">		 
		 */
		final NodeList categories = getChild( result , "rowset" ).getChildNodes();
		for ( int i = 0 ; i < categories.getLength() ; i++ ) {
			final Node n = categories.item(i);
			if ( ! isElementNode( n ) ) {
				continue;
			}
			
			final Element categoryNode = (Element) categories.item( i ); // <row groupName="Corporation Management" groupID="266">
			
			final int id = getIntAttributeValue( categoryNode , "groupID" );
			final SkillGroup cat = skillTree.getOrCreateSkillGroup( id );
			cat.setName( getAttributeValue(categoryNode , "groupName" ) );
			
			final Element rowSet = getRowSetNode( categoryNode , "skills" );
			final NodeList skills = rowSet.getChildNodes();
			for ( int j = 0 ; j < skills.getLength() ; j++ ) {
				final Node n2 = skills.item( j );
				if ( ! isElementNode(n2) ) {
					continue;
				}
				final Element skillNode = (Element) skills.item(j);
				parseSkill( skillNode , cat );
			}
		}
		
		skillTree.validate();
	}
	
	protected void parseSkill(Element skillNode,SkillGroup cat) {
		
		final int skillId = getIntAttributeValue( skillNode , "typeID"  );		
		final String skillName = getAttributeValue( skillNode , "typeName" );
		final String skillDescription = getChildValue( skillNode,"description" );
		final int skillRank = Integer.parseInt( getChildValue( skillNode , "rank"  ) );
		
		final boolean isPublished= !"0".equals( getAttributeValue( skillNode , "published" ) );
		
		final Skill skill = cat.getOrCreateSkill( skillId , isPublished );
		skill.setCategory( cat );
		skill.setName( skillName );
		skill.setDescription( skillDescription );
		skill.setRank( skillRank );

		if ( DEBUG_PARSING ) {
			LOG.info("parseSkill(): Parsing "+skill);
		}
		
		if ( ! isPublished ) {
			LOG.warn("parseSkill(): Not published: "+skill); 
		}
		
		// parse prerequisites
		final Element requiredSkillsNode  =
			getRowSetNode( skillNode , "requiredSkills" );
		
		final NodeList prereqs = requiredSkillsNode.getChildNodes();
		for ( int i = 0 ; i < prereqs.getLength() ; i++ ) {
			final Node n = prereqs.item( i );
			if ( ! isElementNode( n ) ) {
				continue;
			}
			final Element node = (Element) n;
			
			final Prerequisite p = new Prerequisite();
			
			p.setRequiredLevel( getIntAttributeValue( node , "skillLevel" ) );
			final int skillTypeID = getIntAttributeValue( node , "typeID" );
			if ( skillTypeID <= 0 ) {
				throw new UnparseableResponseException("Invalid skill type ID "+skillTypeID);
			}
			p.setSkill( skillTree.getOrCreateSkill( skillTypeID , null ) ); // isPublished flag unknown (may be a forward reference)
			skill.addPrerequisite( p );
		}
		
		// parse skill attributes
		final Element requiredAttributes =
			getChild( skillNode , "requiredAttributes" );
		
		if ( requiredAttributes.hasChildNodes() || skill.isPublished() ) {
			final String primary = getChildValue(requiredAttributes , "primaryAttribute" );
			final String secondary= getChildValue(requiredAttributes , "secondaryAttribute" );
			try {
				skill.setPrimaryAttribute( AttributeType.getByTypeId( primary ) );
				skill.setSecondaryAttribute( AttributeType.getByTypeId( secondary ) );
			} catch(IllegalArgumentException e) {
				throw new UnparseableResponseException( "Invalid primary and/or secondary attribute: "+e.getMessage(),e );
			}
		} else {
			LOG.error("parseSkill(): Skill has no required attributes ? "+skill+" , assigning perc + willpower");
			skill.setPrimaryAttribute( AttributeType.PERCEPTION );
			skill.setSecondaryAttribute( AttributeType.WILLPOWER );
		}
	}
	
	public SkillTree getResult() {
		assertResponseParsed();
		return skillTree;
	}

	@Override
	public void reset() {
		skillTree = new SkillTree();
	}

	@Override
	public URI getRelativeURI() {
		return URI;
	}
}
