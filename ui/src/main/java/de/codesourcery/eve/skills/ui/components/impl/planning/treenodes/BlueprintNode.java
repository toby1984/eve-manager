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

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.ui.model.ITreeNode;

/**
 * (required material) The blueprint required for production of a specific item.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public final class BlueprintNode extends ProductionPlanNode {

	private final Blueprint.Kind requiredBPKind;
	
	public BlueprintNode(Blueprint blueprint, int quantity, Blueprint.Kind requiredBPKind) 
	{
		super(blueprint,quantity);
		this.requiredBPKind = requiredBPKind; 
	}
	
	public ManufacturingJobRequest getParentJobRequest() {
		final ITreeNode parent = getParent();
		if ( parent instanceof ManufacturingJobNode ) {
			return ((ManufacturingJobNode) parent).getManufacturingJobRequest();
		}
		return null;
	}
	
	public Blueprint getBlueprint() {
		return (Blueprint) getValue();
	}

	public  Blueprint.Kind getBlueprintKind() {
		return this.requiredBPKind;
	}
	
	/**
	 * Whether this node represents a 
	 * requirement for a blueprint original.
	 * 
	 * @return
	 */
	public boolean isRequiresOriginal()
	{
		return requiredBPKind == Blueprint.Kind.ORIGINAL;
	}
	
	/**
	 * Whether this node represents a 
	 * requirement for a blueprint copy.
	 * 
	 * @return
	 */
	public boolean isRequiresCopy()
	{
		return requiredBPKind == Blueprint.Kind.COPY;
	}	
	
	public boolean isRequiresAny() {
		return requiredBPKind == Blueprint.Kind.ANY; 
	}

	@Override
	public boolean hasEditableQuantity()
	{
		return false;
	}

	public void setMeLevel(int meLevel)
	{
		final ManufacturingJobRequest job =
			getParentJobRequest();
		
		if ( job != null ) {
			job.setMaterialEfficiency( meLevel );
		}
	}

	public int getMeLevel()
	{
		final ManufacturingJobRequest job =
			getParentJobRequest();
		
		return job != null ? job.getMaterialEfficiency() :0;		
	}

	public void setPeLevel(int peLevel)
	{
		final ManufacturingJobRequest job =
			getParentJobRequest();
		
		if ( job != null ) {
			job.setProductionEfficiency( peLevel );
		}
	}

	public int getPeLevel()
	{
		final ManufacturingJobRequest job =
			getParentJobRequest();
		
		return job != null ? job.getProductionEfficiency() :0;
	}

}