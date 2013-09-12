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


public class ManufacturingJobRequest extends JobRequest {

	/**
	 * BPO ME level.
	 */
	private int materialEfficiency; 
	/**
	 * BPO PE level.
	 */
	private int productionEfficiency;
	
	private final Blueprint blueprint;
	private int runs=1;
	private SlotAttributes location = SlotAttributes.HIGHSEC_NPC_STATION;
	private ICharacter character;
	
	public ManufacturingJobRequest(Blueprint blueprint) {
		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}
		this.blueprint = blueprint;
	}
	
	@Override
	public final Type getType() {
		return Type.MANUFACTORING;
	}
	
	public void setCharacter(ICharacter character) {
		if ( character == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		this.character = character;
	}

	public ICharacter getCharacter() {
		return character;
	}

	/**
	 * Sets the quantity to produce.
	 * 
	 * @param quantity will be automatically rounded up to a multiple of {@link Blueprint#getPortionSize()}.
	 */
	public void setQuantity(int quantity) {
		if ( quantity <= 0 ) {
			throw new IllegalArgumentException("Quantity must be > 0");
		}
		setRuns( (int) Math.max( 1.0d , Math.ceil( quantity / blueprint.getPortionSize() ) ) ); 
	}

	public int getQuantity() {
		return runs * blueprint.getPortionSize();
	}

	public void setRuns(int runs) {
		if ( runs < 1 ) {
			throw new IllegalArgumentException("Number of runs cannot be < 1");
		}
		this.runs = runs;
	}
	
	public int getRuns() {
		return runs;
	}

	public Blueprint getBlueprint() {
		return blueprint;
	}

	public void setMaterialEfficiency(int materialEfficiency) {
		this.materialEfficiency = materialEfficiency;
	}

	public int getMaterialEfficiency() {
		return materialEfficiency;
	}

	public void setSlotAttributes(SlotAttributes location) {
		if (location == null) {
			throw new IllegalArgumentException("location cannot be NULL");
		}
		this.location = location;
	}
	
	public SlotAttributes getSlotAttributes() {
		return location;
	}

	public void setProductionEfficiency(int productionEfficiency)
	{
		this.productionEfficiency = productionEfficiency;
	}

	public int getProductionEfficiency()
	{
		return productionEfficiency;
	}


}
