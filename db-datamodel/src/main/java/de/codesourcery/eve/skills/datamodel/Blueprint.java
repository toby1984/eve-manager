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

import java.util.Map;

import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.BlueprintType;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.utils.ISKAmount;


public abstract class Blueprint {

	public enum Kind {
		ORIGINAL,
		COPY,
		ANY
	}
	
	private final BlueprintType type;

	private final Object LOCK = new Object();
	// guarded-by: LOCK
	private Map<Activity,Requirements> requirements = null;

	public Blueprint(BlueprintType type) {
		if (type == null) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		this.type = type;
	}

	public String getName() {
		return type.getBlueprintType().getName();
	}

	@Override
	public boolean equals(Object obj)
	{
		if ( obj instanceof Blueprint) {
			return this.getType().getBlueprintType().equals( ((Blueprint) obj).getType().getBlueprintType() );
		}
		return false;
	}
	
	public BlueprintType getType() {
		return type;
	}
	
	public BlueprintType getParentBlueprint() {
		return type.getParentBlueprint();
	}
	
	public ISKAmount getBasePrice() {
		return type.getBlueprintType().getBasePrice();
	}
	
	/**
	 * Returns the number of items produced
	 * per run.
	 * 
	 * @return
	 */
	public int getPortionSize() {
		return getProductType().getPortionSize();
	}

	public InventoryType getProductType() {
		return type.getProductType();
	}

	public int getProductionTime() {
		return type.getProductionTime();
	}

	public int getTechLevel() {
		return type.getTechLevel();
	}

	public int getResearchProductivityTime() {
		return type.getResearchProductivityTime();
	}

	public int getResearchMaterialTime() {
		return type.getResearchMaterialTime();
	}

	public int getResearchCopyTime() {
		return type.getResearchCopyTime();
	}

	public int getResearchTechTime() {
		return type.getResearchTechTime();
	}

	public int getProductivityModifier() {
		return type.getProductivityModifier();
	}

	public int getMaterialModifier() {
		return type.getMaterialModifier();
	}

	public int getWasteFactor() {
		return type.getWasteFactor();
	}

	public int getMaxProductionLimit() {
		return type.getMaxProductionLimit();
	}

	protected abstract Map<Activity,Requirements> fetchRequirements();
	
	public Requirements getRequirementsFor(Activity activity) {
		if (activity == null) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}

		synchronized( LOCK ) {
			
			if ( requirements == null ) {
				requirements = fetchRequirements();
			}
			
			Requirements result = requirements.get( activity );
			if ( result == null ) {
				result = new Requirements( activity );
				requirements.put( activity, result );
			}
			return result;
		}
	}
	
	public long calculateMEResearchTime(int metallurgySkillLevel, SlotAttributes slot , float implantModifier ) 
	{
		/*
         * ME Research Time =  { Blueprint Base Research Time} * ( 1 - (0.05 * { Metallurgy Skill Level}) ) * {Research Slot Modifier} * {Implant Modifier}
		 */
		return Math.round( getResearchMaterialTime() * ( 1.0f - (0.05f * metallurgySkillLevel ) ) * slot.getResearchTimeModifier() * implantModifier );
	}
	

	public long calculatePEResearchTime(int researchSkillLevel, SlotAttributes slot , float implantModifier ) 
	{
		/*
         * ME Research Time =  { Blueprint Base Research Time} * ( 1 - (0.05 * { research Skill Level}) ) * {Research Slot Modifier} * {Implant Modifier}
		 */
		return Math.round( getResearchMaterialTime() * ( 1.0f - (0.05f * researchSkillLevel ) ) * slot.getResearchTimeModifier() * implantModifier );
	}
	
	/**
	 * Calculates the time it takes to create a copy with 
	 * 1 production run.
	 * @param scienceSkillLevel
	 * @param slot
	 * @param implantModifier
	 * @return
	 */
	public long calculateCopyTime(int scienceSkillLevel,SlotAttributes slot,float implantModifier) {
		/*
		 Copy Time ={Blueprint Base Copy Time} * ( 1 - (0.05 * {Science Skill Level} ) * {Copy Slot Modifier} * {Implant Modifier}

		 Alternative :
			Blueprint's Research Copy Time * 2 * Number of Licensed Production Runs * (1 - 0.05 * Science Skill Level)/ Maximum Number of Licensed Production Runs )
			
			TODO: Figure out which one is correct....
		 */
		return Math.round( getResearchCopyTime() * ( 1.0f - ( 0.05f * scienceSkillLevel ) ) * slot.getCopyTimeModifier() * implantModifier );
	}
	
	/**
	 * 
	 * @return production time in seconds
	 */
	public long calculateProductionTime(
			int PE,
			int industrySkill,
			float implantModifier, 
			SlotAttributes productionSlot) {
		
		final float BT = getProductionTime();
		final float PM = getProductivityModifier();
		
		final float base =
			( 1.0f - ( industrySkill * 0.04f ) )*implantModifier*productionSlot.getProductionTimeModifier();
		
		/*
Production Time (PE ≥ 0):

Production Time = {Base Production Time} * ( 1 - ( {Productivity Modifier } / {Base Production Time} ) ) * ( PE / (1+PE)) * PTM 

Production Time (PE < 0):

Production Time = {Base Production Time} * ( 1 - ( {Productivity Modifier } / {Base Production Time} ) ) * ( PE -1 ) * PTM
		 
		 */
		
		final double time;
		if ( PE >= 0 ) {
			time =
				BT * ( 1.0f - ( ( PM / BT ) * ( (float) PE / (1.0f+PE ) ) ) ) * base ;
		} else {
			time =
				BT * ( 1.0f - ( ( PM / BT ) * ( 1.0f+ (float) PE - 1.0f ) ) ) * base ;
		}
		
		return Math.round( time  );
		
		/*
		 * 
		 * Antimatter Reactor Unit Blueprint
typeID:17338 productionTime:300 productivityModifier:20 Productivity level:8

The equation
BT = productionTime (from dbo_invBlueprintTypes)
PM = productivityModifier (from dbo_invBlueprintTypes)
PL = Productivity Level
IS = Industry Skill

Blueprint Display:
------------------

Manufacturing Time = INT(BT * (1 - ((PM/BT) * (PL/(1+PL)))))
Manufacturing Time (YOU) = BT * (1 - ((PM/BT) * (PL/(1+PL)))) * (1 - (IS * 0.04))

Factory Quote:
--------------
Duration = ROUND(BT * (1 - ((PM/BT) * (PL/(1+PL)))) * (1 - (IS * 0.04)),0)

As you can see from the screen shots for the Antimatter Reactor Units

Manufacturing Time = 4 minutes, 42 seconds or 282 secs
-and-
Manufacturing Time (You) = 3 minutes, 45 seconds or 225 secs.

The calculations
Manufacturing time = int(300 * (1 – ((20/300) * (8/(1+8))))) = int(282.2222) = 282
		 * 
		 *  =========================================================
		 * 
 PTM  (Production Time Modifier ) = (1 - (0.04 * { Industry Skill} )) * {Implant Modifier} * {Production Slot Modifier}

Production Time (PE ≥ 0):

Production Time = {Base Production Time} * ( 1 - ( {Productivity Modifier } / {Base Production Time} ))* ( PE / (1+PE)) * PTM 

Production Time (PE < 0):

Production Time = {Base Production Time} * ( 1 - ( {Productivity Modifier } / {Base Production Time} )* ( PE -1 ) * PTM

}*/
		
	}
	
	public float getBaseInventionChance() {
		if ( getTechLevel() != 1 ) { // only Tech I can be invented
			return 0.0f;
		}
	/*
	 * Base chance is 20% for battlecruisers, battleships, Hulk
	 * Base chance is 25% for cruisers, industrials, Mackinaw
	 * Base chance is 30% for frigates, destroyers, Skiff, freighters
	 * Base chance is 40% for all other inventables		 
	 */
		
		final InventoryType producedItem =
			getProductType();
		
		if ( producedItem.isBattleCruiser() ||
			 producedItem.isBattleship() ||
			 producedItem.isHulk() ) 
		{
			return 0.2f;
			
		} else if ( producedItem.isCruiser() ||
				 producedItem.isIndustrial() ||
				 producedItem.isMackinaw() ) 
		{
			return 0.25f;
		} else if ( producedItem.isFrigate() ||
				 producedItem.isDestroyer() ||
				 producedItem.isSkiff() ||
				 producedItem.isFreighter() ) 
		{
			return 0.3f;
		}

		 return 0.4f;
	}
	
	@Override
	public String toString()
	{
		return "Blueprint [ "+type.getBlueprintType()+" ]"; 
	}
}