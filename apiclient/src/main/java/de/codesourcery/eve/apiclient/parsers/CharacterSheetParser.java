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

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.AttributeEnhancer;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.CorporationId;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.datamodel.CharacterDetails.Gender;
import de.codesourcery.eve.skills.datamodel.CharacterDetails.Race;
import de.codesourcery.eve.skills.db.datamodel.AttributeType;
import de.codesourcery.eve.skills.db.datamodel.Corporation;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class CharacterSheetParser extends AbstractResponseParser<ICharacter> {

	private static final Logger log = Logger
			.getLogger(CharacterSheetParser.class);
	
	public static final URI URI = toURI("/char/CharacterSheet.xml.aspx" );
	
	private de.codesourcery.eve.skills.datamodel.Character character = 
		new de.codesourcery.eve.skills.datamodel.Character();
	
	private IStaticDataModel staticDataModel;
	
	public CharacterSheetParser(IStaticDataModel daoProvider,ISystemClock clock) {
		super(clock);
		this.staticDataModel = daoProvider;
	}
	
	/*
<?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2007-06-18 22:49:01</currentTime>
  <result>
    <characterID>150337897</characterID>
    <name>corpslave</name>
    <race>Minmatar</race>
    <bloodLine>Brutor</bloodLine>
    <gender>Female</gender>
    <corporationName>corpexport Corp</corporationName>
    <corporationID>150337746</corporationID>
    <cloneName>Clone Grade Pi</cloneName>
    <cloneSkillPoints>54600000</cloneSkillPoints>
    <balance>190210393.87</balance>
    <attributeEnhancers>
      <intelligenceBonus>
        <augmentatorName>Snake Delta</augmentatorName>
        <augmentatorValue>3</augmentatorValue>
      </intelligenceBonus>
      <memoryBonus>
        <augmentatorName>Halo Beta</augmentatorName>
        <augmentatorValue>3</augmentatorValue>
      </memoryBonus>
    </attributeEnhancers>
    <attributes>
      <intelligence>6</intelligence>
      <memory>4</memory>
      <charisma>7</charisma>
      <perception>12</perception>
      <willpower>10</willpower>
    </attributes>
    <rowset name="skills" key="typeID">
      <row typeID="3431" skillpoints="8000" level="3"/>
      <row typeID="3413" skillpoints="8000" level="3"/>
      <row typeID="21059" skillpoints="500" level="1"/>
      <row typeID="3416" skillpoints="8000" level="3"/>
      <row typeID="3445" skillpoints="277578" unpublished="1"/>
    </rowset>
    
    <!-- Boring stuff (corp roles , certificates,...) here --->
     
  </result>
  <cachedUntil>2007-06-18 23:49:01</cachedUntil>
</eveapi>
	 
	 */
	@Override
	void parseHook(Document document) throws UnparseableResponseException {
		
		final Element resultNode =
			getResultElement( document );
		
		character.setCharacterId( new CharacterID( getChildValue( resultNode , "characterID" ) ) );
		character.setName( getChildValue( resultNode , "name" ) );
		character.getCharacterDetails().setRace( Race.fromTypeId( getChildValue( resultNode , "race" )  ) );
		character.getCharacterDetails().setBloodLine( getChildValue( resultNode , "bloodLine" ) );
		character.getCharacterDetails().setGender( Gender.fromTypeId( getChildValue( resultNode , "gender" )));
		
		// resolve corp by ID
		final Corporation corp = 
			new Corporation();
			
		corp.setName( getChildValue( resultNode , "corporationName"));
		corp.setId( new CorporationId(Long.parseLong( getChildValue( resultNode , "corporationID") ) ) );
		
		character.getCharacterDetails().setCorporation( corp );
		character.getCharacterDetails().setCloneName( getChildValue( resultNode , "cloneName") );
		character.getCharacterDetails().setCloneSkillPoints(
				Integer.parseInt( getChildValue( resultNode , "cloneSkillPoints") ) );
		
		final long balance =
			toISKAmount( Double.valueOf( getChildValue( resultNode , "balance") ) );
		
		character.getCharacterDetails().setBalance( balance );
		
		// parse attribute enhancers
		
		final Element attributeEnhancers =
			getChild( resultNode , "attributeEnhancers" );
		
		for ( Node n : nodeIterator( attributeEnhancers.getChildNodes() ) ) {
			
			if ( ! isElementNode( n ) ) {
				continue;
			}
			
			final Element e = (Element) n;
			
			AttributeType match = null;
			for ( AttributeType t : AttributeType.values() ) {
				final String name = t.getTypeId().toLowerCase()+"Bonus";
				if ( e.getNodeName().equals( name ) ) {
					match = t;
					break;
				}
			}
			
			if ( match == null ) {
				throw new UnparseableResponseException("Unknown attribute enhancer with attribute type '"+e.getNodeName()+"'");
			}
			final int attrBonus = 
				Integer.parseInt( getChildValue( e , "augmentatorValue" ) );
			
			character.getImplantSet().setImplant( new AttributeEnhancer( match , attrBonus) );
		}
		
		// parse character base attribute values
		final Element attrNode = getChild( resultNode , "attributes" );
		for ( Node n : nodeIterator( attrNode.getChildNodes() ) ) {
			if ( ! isElementNode( n ) ) {
				continue;
			}
			final Element e = (Element) n;
			AttributeType match=null;
			for ( AttributeType t : AttributeType.values() ) {
				if ( t.getTypeId().equalsIgnoreCase( e.getNodeName() ) ) {
					match = t;
					break;
				}
			}
			if ( match == null ) {
				throw new UnparseableResponseException("Unknown attribute type '"+e.getNodeName()+"'");
			}
			character.getAttributes().setBaseValue( match  , 
					Integer.parseInt( getNodeValue( e ) ) );
		}

		// parse trained skills
		final Element rowSet =
			getRowSetNode( resultNode , "skills" );
		
		final SkillTree skillTree;
		if ( this.staticDataModel != null ) {
			skillTree = staticDataModel.getSkillTree();
		} else {
			skillTree = null;
		}
		for ( Node n : nodeIterator( rowSet.getChildNodes() ) ) {
			if ( ! isElementNode( n ) ) {
				continue;
			}
			
			final Element e = (Element) n;
			
			if ( ! e.getNodeName().equals("row" ) ) {
				throw new UnparseableResponseException("Unexpected child node "+n.getNodeName()+" in rowset ?");
			}
			
			final int typeId =
				Integer.parseInt( getAttributeValue(e , "typeID" ) );			
			
			if ( getAttributeValue( e , "unpublished" , false ) != null ) {
				log.info("parse(): Ignoring unpublished skill "+typeId);
				continue;
			}
			
			final int skillPoints = Integer.parseInt( getAttributeValue( e , "skillpoints" ) );
//			log.debug("parse(): Got skill "+typeId+" , skillpoints = "+skillPoints);

			if ( skillTree != null ) {
				final Skill s = skillTree.getSkill( typeId );
				character.setTrainedSkill( s , skillPoints );
			}
		}		
		
	}

	@Override
	public URI getRelativeURI() {
		return URI;
	}

	@Override
	public ICharacter getResult() throws IllegalStateException {
		assertResponseParsed();
		return character;
	}

	@Override
	public void reset() {
		character = new de.codesourcery.eve.skills.datamodel.Character();
	}

}
