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
package de.codesourcery.eve.skills.production;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.Decryptor;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;
import de.codesourcery.eve.skills.datamodel.Requirements;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Skill;

public class InventionChanceCalculator {

	public static final Logger log = Logger
			.getLogger(InventionChanceCalculator.class);
	
	private final IStaticDataModel model;
	public InventionChanceCalculator(IStaticDataModel dataModel) {
		if (dataModel == null) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.model = dataModel;
	}
	
	protected static void assertTech1(Blueprint bp) {
		if ( bp.getTechLevel() != 1 ) {
			throw new IllegalArgumentException("Cannot perform invention with a Tech "+bp.getTechLevel()+" blueprint");
		}
	}
	
	public boolean canBeUsedForInvention(Blueprint bp) {
		return bp.getTechLevel() == 1 && ! model.getTech2Variations( bp ).isEmpty();
	}
	
	public int calcMaximumNumberOfRunsPossible(Blueprint tech1Blueprint,Decryptor decryptor) {
		
		assertTech1(tech1Blueprint);
		
		if ( tech1Blueprint.getType().getBlueprintType().isShip() ) {
			return 1 + decryptor.getRunModifier();
		}
		return 10 + decryptor.getRunModifier();
	}
	
	/**
	 * Calculates the invention chance for a given
	 * T1 blueprint,character, meta-item level and decryptor.
	 * 
	 * @param bp
	 * @param character
	 * @param addItemMetaLevel
	 * @param decryptor
	 * @return invention chance ( 0 - 1.0 )
	 */
	public float calculateInventionChance(Blueprint bp , 
			ICharacter character, 
			int addItemMetaLevel , Decryptor decryptor) 
	{
		
		assertTech1( bp );
		
		/*
* Base chance is 20% for battlecruisers, battleships, Hulk
** Base chance is 25% for cruisers, industrials, Mackinaw
*** Base chance is 30% for frigates, destroyers, Skiff, freighters
**** Base chance is 40% for all other inventables		 
		 */
		
		/*
Base_Chance * (1 + (0.01 * Encryption_Skill_Level)) * 
(1 + ((Datacore_1_Skill_Level + Datacore_2_Skill_Level) * (0.1 / (5 - Meta_Level) ) ) )
 * Decryptor_Modifier		 
		 */
		
		final Skill racialSkill =
			getRacialSkill( bp );
		
		if ( ! character.hasSkill( racialSkill ) ) {
			return 0.0f;
		}
		
		final List<Skill> dcSkills =
			getInventionDatacoreSkills( bp );
		
		final int racialSkillLvl =
			character.getSkillLevel( racialSkill ).getLevel();
		
		final int skill1Lvl =
			character.getSkillLevel( dcSkills.get(0) ).getLevel();
		
		final int skill2Lvl =
			character.getSkillLevel( dcSkills.get(1) ).getLevel();

		return bp.getBaseInventionChance() * (1.0f+(0.01f * racialSkillLvl ) )*
				( 1+ ( ( skill1Lvl + skill2Lvl ) *( 0.1f / ( 5.0f - addItemMetaLevel ) ) ) )*
				decryptor.getChanceModifier();
	}	
	
	public Skill getRacialSkill(Blueprint bp) {
		assertTech1(bp);
		
		InventoryType dataInterface=null;
		for ( RequiredMaterial mat : bp.getRequirementsFor( Activity.INVENTION ).getRequiredMaterials() ) {
			
			final String typeName =
				mat.getType().getName();
			
			if ( ! typeName.startsWith("Datacore - " ) ) {
				dataInterface = mat.getType();
				break;
			}
		}
		
		if ( dataInterface == null ) {
			throw new RuntimeException("(Could not detect " +
					"required data interface) " +
					"Internal error, unable to determine racial skill for "+bp); 
		}
		
		final Blueprint dataInterfaceBlueprint = model.getBlueprintByProduct( dataInterface );
		
		Requirements requirements = dataInterfaceBlueprint.getRequirementsFor(Activity.MANUFACTURING );
		
		final List<Prerequisite> requiredSkills = 
			requirements.getRequiredSkills();
		
		Prerequisite encryptionMethod = null;
		for ( Prerequisite r : requiredSkills ) {
			final String name = r.getSkill().getName().toLowerCase();
			if ( name.contains( "encryption" ) && name.contains("methods" ) ) {
				encryptionMethod = r;
				break;
			}
		}
		
		if ( encryptionMethod == null ) {
			throw new RuntimeException("( Unable to determine skill for data interface from { "+requiredSkills+" }"+
					dataInterfaceBlueprint.getName()+" ) " +
					"Internal error, unable to determine racial skill for "+dataInterfaceBlueprint); 
		}
		
		return encryptionMethod.getSkill();
	}
	
	public static RequiredMaterial getRequiredDataInterface( Blueprint tech1Blueprint) 
	{
		assertTech1(tech1Blueprint);
		
		final List<RequiredMaterial> inventionMaterials = 
			tech1Blueprint.getRequirementsFor( Activity.INVENTION ).getRequiredMaterials();
		
		log.debug("getRequiredDatainterface(): Found "+inventionMaterials.size()+" "
				+" materials required for invention of "+
				tech1Blueprint.getName()+" ("+tech1Blueprint.getType().getBlueprintType().getTypeId());
		
		
		final List<RequiredMaterial> others = 
			new ArrayList<RequiredMaterial>();
		
		for ( RequiredMaterial mat : inventionMaterials ) {
			
			final String typeName =
				mat.getType().getName();

			log.debug("getInventionDatacoreSkills(): " +
					"Got material '"+typeName+"'");
					
			if ( ! typeName.startsWith("Datacore - " ) ) {
				others.add( mat );
				continue;
			}
			
		}
		
		if ( others.size() == 1 ) {
			return others.get(0);
		}
		
		log.error("getRequiredDatainterface(): Unable to determine data interface" +
				", found "+others.size()+" non datacore-skill materials: "+others);
		throw new RuntimeException("Internal error - unable to " +
				"determine data interface used to invent "+tech1Blueprint.getName() );
	}

	public List<Skill> getInventionDatacoreSkills( Blueprint bp ) {

		assertTech1(bp);
		
		final Set<String> skillNames = new HashSet<String>();
		final List<RequiredMaterial> inventionMaterials = 
			bp.getRequirementsFor( Activity.INVENTION ).getRequiredMaterials();
		
		log.debug("getInventionDatacoreSkills(): Found "+inventionMaterials.size()+" "
				+" materials required for invention of "+bp.getName()+" ("+bp.getType().getBlueprintType().getTypeId());
		
		
		final List<RequiredMaterial> others = new ArrayList<RequiredMaterial>();
		
		for ( RequiredMaterial mat : inventionMaterials ) {
			
			final String typeName =
				mat.getType().getName();

			log.debug("getInventionDatacoreSkills(): Got material '"+
					typeName+"'");
					
			if ( ! typeName.startsWith("Datacore - " ) ) {
				others.add( mat );
				continue;
			}
			
			skillNames.add( mat.getType().getName().substring( 11 ) );
		}
		
		List<Skill> result = new ArrayList<Skill>();
		for ( String skillName : skillNames ) {
			result.add(
				this.model.getSkillTree().getSkillByName( skillName )
			);
		}
		
		if ( result.size() != 2 ) {
			final String msg = 
				"Internal error, " +
				"unable to determine datacore skills for "+bp.getName()+" (ID "+
				bp.getType().getBlueprintType().getId()+") , found: "+result;
			log.error("getInventionDatacoreSkills(): "+msg);
			throw new RuntimeException( msg );
		}
		return result;
	}
}
