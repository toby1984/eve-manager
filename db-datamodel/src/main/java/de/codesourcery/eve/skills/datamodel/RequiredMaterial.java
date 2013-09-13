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

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Skill;

/**
 * Material requirements of a given 
 * type for a single production run.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class RequiredMaterial {

	private final InventoryType type;
	private final int quantity;
	private double damagePerJob;
	private boolean recycle;

	/**
	 * Returns whether the regular 
	 * material waste factor 
	 * applies for this
	 * required material when manufacturing
	 * the corresponding blueprint's item.
	 */
	private boolean isSubjectToSkillWaste; // required material quantity affected by PE level of player
	private boolean isSubjectToBPMWaste; // required material quantity affected by ME level of blueprint 	
	private boolean isSubjectToStationWaste; // required material quantity affected by standings of player towards station
	// where the item is being manufactured
	
	public RequiredMaterial(RequiredMaterial original,int newQuantity) 
	{
		this( original.getType() , newQuantity );
		
		setDamagePerJob( original.getDamagePerJob() );
		setSupportsRecycling( original.supportsRecycling() );
		setSubjectToBPMWaste( original.isSubjectToBPMWaste() );
		setSubjectToSkillWaste( original.isSubjectToSkillWaste() );
		setSubjectToStationWaste( original.isSubjectToStationWaste() );		
	}
	
	public RequiredMaterial(InventoryType type,int quantity) 
	{
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		this.type = type;
		this.quantity = quantity;
	}
	
	public boolean hasSameTypeAs(RequiredMaterial mat) {
		return this.type.equals( mat.type );
	}

	public InventoryType getType() {
		return type;
	}

	public int getQuantity() {
		return quantity;
	}
	
	public static RequiredMaterial createNew(RequiredMaterial m,int newQuantity)
{
		final RequiredMaterial newMat = new RequiredMaterial( m.getType() , newQuantity );
		
		newMat.setDamagePerJob( m.getDamagePerJob() );
		
		newMat.setSubjectToBPMWaste( m.isSubjectToBPMWaste() );
		newMat.setSubjectToSkillWaste( m.isSubjectToSkillWaste() );
		newMat.setSubjectToStationWaste( m.isSubjectToStationWaste() );
		newMat.setSupportsRecycling( m.supportsRecycling() );
		return newMat;
	}

	/**
	 * Returns required quantity adjusted by damagePerJob factor.
	 * 
	 * @return
	 */
	public float getRealQuantity() {
		return (float) (quantity * damagePerJob);
	}

	public void setDamagePerJob(double damagePerJob) {
		this.damagePerJob = damagePerJob;
	}

	public double getDamagePerJob() {
		return damagePerJob;
	}

	public void setSupportsRecycling(boolean recycle) {
		this.recycle = recycle;
	}

	public boolean supportsRecycling() {
		return recycle;
	}
	
	/**
	 * 
	 * TODO: <code>honorDamagePerRun</code> is currently
	 * TODO: (more or less) set randomly from various call sites...
	 * TODO: According to the Eve Online UI, always the 
	 * TODO: full quantity (disregarding dmgPerRun)
	 * TODO: seems to be required ... VERIFY THIS !!!
	 * 
	 * @param jobRequest details of this manufacturing job
	 * @param productionEfficiencySkill Used to determine PE skill level of the
	 * character running the production job
	 * @param honorDamagePerRun whether to account for fractional damage (used
	 * in T2 production, e.g. R.A.M.s take fractional damage each production run)
	 * @return
	 */
	public float calcRequiredMaterial(ManufacturingJobRequest jobRequest,
			Skill productionEfficiencySkill,boolean honorDamagePerRun) 
	{
		final int peSkillLevel = jobRequest.getCharacter().getSkillLevel( productionEfficiencySkill ).getLevel();
		
		final int runs = jobRequest.getRuns();
		
		return calcRequiredMaterial( runs ,
				jobRequest.getBlueprint() ,
				jobRequest.getMaterialEfficiency(), 
				peSkillLevel , 
				honorDamagePerRun );
	}

	/**
	 * 
	 * @param runs
	 * @param bp
	 * @param bpoMaterialEfficiency
	 * @param productionEfficiencySkill
	 * @param honorDamagePerJob
	 * @return
	 */
	private float calcRequiredMaterial(int runs, Blueprint bp, int bpoMaterialEfficiency, int productionEfficiencySkill, boolean honorDamagePerJob) 
	{
		/* !!! See isSubjectToManufacturingWaste() method !!!
		 * 
		 * Some materials ("extra" materials) are NOT subject to manufacturing waste.
		 * 
		 * If bpMaterialLevel >= 0 Then 
		 *     wasteMod = ((wasteFactor/100) / (bpMaterialLevel + 1)) 
		 * Else 
		 *     wasteMod = (-bpMaterialLevel + 1) / 10
		 * End If
		 * 
		 * SkillBonus = 1.25 - (0.05 * CharSkillLevel)    ==> 1 for PE skill lvl 5
		 * 
		 * StationBonus = StationMaterialModifier - 1
		 * 
		 * BPMLWaste = BaseMaterialReq * wasteMod 
		 * SkillWaste = BaseMaterialReq * (SkillBonus - 1)   ===> 0 for PE skill lvl 5
		 * 
		 * StationWaste = BaseMaterialReq * (SkillBonus * * StationBonus)
		 * 
		 * Waste = Round(BPMLWaste + SkillWaste + StationWaste, 0)
		 */

		float baseWaste = bp.getType().getWasteFactor() / 100.0f;

		float wasteFactor;
		if (bpoMaterialEfficiency >= 0) {
			wasteFactor = baseWaste / (float) (1.0f + bpoMaterialEfficiency);
		} else {
			wasteFactor = (-bpoMaterialEfficiency + 1.0f) / 10.0f;
		}

		float skillBonus = 1.25f - (0.05f * (float) productionEfficiencySkill);
		float stationBonus = 1.0f - 1.0f; // TODO: stationMaterialModifier - 1

		final float realQuantity;
		if (honorDamagePerJob && isSubjectToDamagePerRunWaste() ) 
		{
			realQuantity = (float) (quantity * damagePerJob);
		} else {
			realQuantity = quantity;
		}

		float blueprintWaste = realQuantity * wasteFactor;
		float skillWaste = realQuantity * (skillBonus - 1.0f);
		float stationWaste = realQuantity * (skillBonus * stationBonus);

		float wasteQuantity=0;
		if ( isSubjectToBPMWaste() ) {
			wasteQuantity += blueprintWaste;
		}
		
		if ( isSubjectToSkillWaste() ) {
			wasteQuantity += skillWaste;
		}
		
		if ( isSubjectToStationWaste() ) {
			wasteQuantity += stationWaste;
		}

		float adjustedCost = realQuantity + wasteQuantity;

		float result = runs * (float) Math.round( adjustedCost);
		return result < 1.0f ? 1.0f : result;
	}
	
	private boolean isSubjectToDamagePerRunWaste() 
	{
		return isSubjectToBPMWaste || isSubjectToStationWaste;
	}
	
	public void setSubjectToBPMWaste(boolean isSubjectToBPMWaste) {
		this.isSubjectToBPMWaste = isSubjectToBPMWaste;
	}
	
	public void setSubjectToSkillWaste(boolean isSubjectToSkillWaste) {
		this.isSubjectToSkillWaste = isSubjectToSkillWaste;
	}
	
	public void setSubjectToStationWaste(boolean isSubjectToStationWaste) {
		this.isSubjectToStationWaste = isSubjectToStationWaste;
	}
	
	/**
	 * 
	 * @return <code>true</code> if this material is subject to either Blueprint material level waste , PE skill waste
	 * or station standing waste
	 */
	public boolean isSubjectToManufacturingWaste() 
	{
		return isSubjectToBPMWaste || isSubjectToSkillWaste || isSubjectToStationWaste;
	}

	/**
	 * Being subject to waste isn't an attribute of the material, it is a
	 * property of each entry on the blueprint. Each blueprint has 2 categories
	 * of materials, raw materials and extra materials. It shows you these two
	 * categories in the summary screen when you go to build something.
	 * 
	 * Anything in the raw materials category is subject to waste, anything in
	 * the extra materials category is not (UNLESS it's also present 
	 * in the invTypeMaterials table for the same blueprint , these are subject 
	 * to PE/skill waste ONLY. This was introduced with the Odyssee expansion).
	 * 
	 * @return <code>true</code> if the actual required quantity depends on the PE (production efficiency) 
	 * level of the character 
	 */	 
	public boolean isSubjectToSkillWaste() {
		return isSubjectToSkillWaste;
	}
	
	/**
	 * Being subject to waste isn't an attribute of the material, it is a
	 * property of each entry on the blueprint. Each blueprint has 2 categories
	 * of materials, raw materials and extra materials. It shows you these two
	 * categories in the summary screen when you go to build something.
	 * 
	 * Anything in the raw materials category is subject to waste, anything in
	 * the extra materials category is not (UNLESS it's also present 
	 * in the invTypeMaterials table for the same blueprint , these are subject 
	 * to PE/skill waste ONLY. This was introduced with the Odyssee expansion).
	 * 
	 * @return <code>true</code> if the actual required quantity depends on the ME (material efficiency)
	 * level of the BLUEPRINT
	 */
	public boolean isSubjectToBPMWaste() {
		return isSubjectToBPMWaste;
	}	
	
	public boolean isSubjectToStationWaste() {
		return isSubjectToStationWaste;
	}
}
