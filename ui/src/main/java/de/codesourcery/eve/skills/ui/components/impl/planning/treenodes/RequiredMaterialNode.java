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
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;

/**
 * Some required material / component (but NOT blueprint).
 * 
 * See {@link BlueprintNode} for blueprints required for execution of a given job.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public final class RequiredMaterialNode extends ProductionPlanNode {

	private final Blueprint producedBy; // can be NULL if material cannot be produced (by players)
	
	public RequiredMaterialNode(RequiredMaterial material,Blueprint producedBy) 
	{
		super(material , material.getQuantity() );
		this.producedBy = producedBy;
	}

	public boolean canBeProduced() {
		return this.producedBy != null;
	}

	public Blueprint getBlueprintForProduction() {
		return producedBy;
	}

	public RequiredMaterial getRequiredMaterial() {
		return (RequiredMaterial) getValue();
	}

	@Override
	public void setQuantity(int quantity)
	{
		super.setQuantity(quantity);
	}
	
	@Override
	public boolean hasEditableQuantity()
	{
		return false;
	}

}