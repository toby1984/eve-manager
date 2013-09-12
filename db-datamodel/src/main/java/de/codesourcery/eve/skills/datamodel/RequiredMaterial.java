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
	private boolean isSubjectToWaste;
	
	public RequiredMaterial(RequiredMaterial original,int newQuantity) {
		this( original.getType() , newQuantity );
		
		setDamagePerJob( original.getDamagePerJob() );
		setSupportsRecycling( original.supportsRecycling() );
		setSubjectToManufacturingWaste( original.isSubjectToManufacturingWaste() );
	}
	
	public RequiredMaterial(InventoryType type,int quantity) {
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
	
	public static RequiredMaterial createNew(RequiredMaterial m,int newQuantity) {
		final RequiredMaterial newMat = 
			new RequiredMaterial( m.getType() , newQuantity );
		newMat.setDamagePerJob( m.getDamagePerJob() );
		newMat.setSubjectToManufacturingWaste( m.isSubjectToManufacturingWaste() );
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
		final int peSkillLevel = 
			jobRequest.getCharacter().getSkillLevel( productionEfficiencySkill ).getLevel();
		
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
	private float calcRequiredMaterial(int runs, Blueprint bp,
			int bpoMaterialEfficiency, int productionEfficiencySkill,
			boolean honorDamagePerJob) {

		/*
		 * !!! See isSubjectToManufacturingWaste() method !!!
		 * 
		 * Some materials ("extra" materials) are NOT subject to manufacturing waste.
		 * 
		 * If bpMaterialLevel >= 0 Then wasteMod = ((wasteFactor/100) /
		 * (bpMaterialLevel + 1)) Else wasteMod = (-bpMaterialLevel + 1) / 10
		 * End If
		 * 
		 * SkillBonus = 1.25 - (0.05 * CharSkillLevel) StationBonus =
		 * StationMaterialModifier - 1
		 * 
		 * BPMLWaste = BaseMaterialReq * wasteMod SkillWaste = BaseMaterialReq *
		 * (SkillBonus - 1) StationWaste = BaseMaterialReq * (SkillBonus *
		 * StationBonus)
		 * 
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
		float stationBonus = 1.0f - 1.0f; // TODO: stationMaterialModifier -1

		final float realQuantity;
		if (honorDamagePerJob && isSubjectToManufacturingWaste() ) 
		{
			realQuantity = (float) (quantity * damagePerJob);
		} else {
			realQuantity = quantity;
		}

		float blueprintWaste = realQuantity * wasteFactor;
		float skillWaste = realQuantity * (skillBonus - 1.0f);
		float stationWaste = realQuantity * (skillBonus * stationBonus);

		float wasteQuantity = blueprintWaste + skillWaste + stationWaste;

		float adjustedCost;
		if ( isSubjectToManufacturingWaste() ) {
			adjustedCost = realQuantity + wasteQuantity;
		} else {
			adjustedCost = realQuantity;
		}

		float result =
			runs * (float) Math.round( adjustedCost);
		return result < 1.0f ? 1.0f : result;
	}

	/**
	 * Being subject to waste isn't an attribute of the material, it is a
	 * property of each entry on the blueprint. Each blueprint has 2 categories
	 * of materials, raw materials and extra materials. It shows you these two
	 * categories in the summary screen when you go to build something.
	 * 
	 * Anything in the raw materials category is subject to waste, anything in
	 * the extra materials category is not. It's that simple. As a general rule
	 * Tech II materials for modules are usually considered extra, while for
	 * ships they are considered raw. This came as quite a surprise to me as
	 * someone who invented and built T2 modules before I got around to ships. I
	 * was used to no T2 waste mats and thought it was the norm.
	 * 
	 */
	public void setSubjectToManufacturingWaste(boolean isSubjectToWaste) {
		this.isSubjectToWaste = isSubjectToWaste;
	}

	public boolean isSubjectToManufacturingWaste() {
		return isSubjectToWaste;
	}

}
