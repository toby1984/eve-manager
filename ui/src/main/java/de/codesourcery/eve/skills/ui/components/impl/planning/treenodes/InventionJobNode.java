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
import de.codesourcery.eve.skills.datamodel.Decryptor;
import de.codesourcery.eve.skills.datamodel.SlotAttributes;

/**
 * T2 invention job.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class InventionJobNode extends ProductionPlanNode {

	public InventionJobNode(InventionJobDetails details,
			int requiredTech2Copies) 
	{
		super( details , requiredTech2Copies );
	}

	public InventionJobDetails getDetails() {
		return (InventionJobDetails) getValue();
	}

	public Blueprint getTech1Blueprint() {
		return  getDetails().getTech1Blueprint();
	}

	public Blueprint getTech2Blueprint()
	{
		return getDetails().getTech2Blueprint();
	}
	
	public int getRuns() {
		return getDetails().getNumberOfRuns();
	}
	
	public void setRuns(int runs) {
		getDetails().setNumberOfRuns( runs );
	}

	public int getRequiredTech1Copies() { return getDetails().getNumberOfRuns(); }

	public SlotAttributes getSlotAttributes()
	{
		return getDetails().getSlotAttributes();
	}

	public int getMetaLevel()
	{
		return getDetails().getMetaLevel();
	}

	public Decryptor getDecryptor()
	{
		return getDetails().getDecryptor();
	}

	@Override
	public boolean hasEditableQuantity()
	{
		return true;
	}

}