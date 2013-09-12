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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

public class ProductionPlan {

	private final ManufacturingJobRequest jobRequest;
	private BigDecimal materialCosts;
	
	/* 
	 * < parent item inventory type> , Map< required item inventory type , textual description> >
	 * 
	 * parent item inventory type is 0 (ZERO) when
	 * querying a top-level requirement (something
	 * the blueprint itself requires, not one of
	 * the sub-components)
	 */
	private final Map<Long,Map<Long,String>> calculations =
		new HashMap<Long, Map<Long,String>>();
	
	public ProductionPlan( ManufacturingJobRequest jobRequest ) {
		if (jobRequest == null) {
			throw new IllegalArgumentException("jobRequest cannot be NULL");
		}
		this.jobRequest = jobRequest;
	}
	
	public BigDecimal getTotalCosts() {
		return getMaterialCosts(); // TODO: Take installation cost + hourly fee into account !!!
	}

	public void setMaterialCosts(BigDecimal materialCosts) {
		this.materialCosts = materialCosts;
	}

	public BigDecimal getMaterialCosts() {
		return materialCosts;
	}

	public Blueprint getBlueprint() {
		return this.jobRequest.getBlueprint();
	}
	
	public void setCalculationFor(InventoryType parent , RequiredMaterial material,String calculation) {
		
		final Long parentId = parent != null ? parent.getId() : 0L;
		final Long itemId = material.getType().getId();
		Map<Long, String> calcsForParent = calculations.get( parentId );
		if ( calcsForParent == null ) {
			calcsForParent = new HashMap<Long, String>();
			calculations.put( parentId , calcsForParent);
		}
		calcsForParent.put( itemId , calculation );
	}
	
	public String getCalculationFor(InventoryType parent , RequiredMaterial material) {
		final Long parentId = parent != null ? parent.getId() : 0L;
		Map<Long, String> calcsForParent = calculations.get( parentId );
		if ( calcsForParent == null ) {
			return "< not specified >";
		}
		String result = calcsForParent.get( material.getType().getId() );
		if ( result == null ) {
			return "< not specified >";
		}
		return result;
	}

	public ManufacturingJobRequest getJobRequest() {
		return jobRequest;
	}

	public int getNumberOfProducedItems() {
		return jobRequest.getQuantity();
	}
}
