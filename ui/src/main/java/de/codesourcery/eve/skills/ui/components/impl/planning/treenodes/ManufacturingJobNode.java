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
package de.codesourcery.eve.skills.ui.components.impl.planning.treenodes;

import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

public class ManufacturingJobNode extends ProductionPlanNode 
{

	public ManufacturingJobNode(ManufacturingJobRequest request) {
		super(request , request.getQuantity());
		if ( request.getBlueprint() == null ) {
			throw new IllegalArgumentException("Blueprint on "+request+" cannot be NULL");
		}
		if ( request.getBlueprint().getProductType() == null ) {
			throw new IllegalArgumentException("Blueprint "+request.getBlueprint()+" has NULL product type ?");
		}
	}

	public ManufacturingJobRequest getManufacturingJobRequest()
	{
		return (ManufacturingJobRequest) getValue();
	}
	
	@Override
	public int getQuantity()
	{
		return getManufacturingJobRequest().getQuantity();
	}
	
	public InventoryType getProduct() {
		return getManufacturingJobRequest().getBlueprint().getProductType();
	}
	
	@Override
	public void setQuantity(int quantity)
	{
		getManufacturingJobRequest().setQuantity( quantity );
	}

	@Override
	public boolean hasEditableQuantity()
	{
		return getParent() == null;
	}
	
	public boolean isTech1Job() {
		return getManufacturingJobRequest().getBlueprint().getTechLevel() == 1;
	}
	
	public boolean isTech2Job() {
		return getManufacturingJobRequest().getBlueprint().getTechLevel() == 2;
	}

}
