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
package de.codesourcery.eve.skills.db.dao;

import java.util.List;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.db.datamodel.BlueprintType;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

public interface IBlueprintTypeDAO extends IReadOnlyDAO<BlueprintType, Long> { 

	public Blueprint getBlueprintByProduct(InventoryType product);

	public List<Blueprint> getBlueprintsByProductName(String name);

	public Blueprint getBlueprint(InventoryType blueprint);

	public List<Blueprint> getBlueprintsByProductGroup(InventoryGroup group);

	public Blueprint getBlueprintByName(String name);
	
	/**
	 * 
	 * @param tech1Blueprint
	 * @param techLevel the tech level the returned blueprints must have
	 * @return
	 */
	public List<Blueprint> getTech2Variations(Blueprint tech1Blueprint);
	
	public Blueprint getTech1Variation(final Blueprint tech2Blueprint);
	
	/**
	 * 
	 * TODO: Maybe the wrong interface for this method.
	 * @param item
	 * @return returns the refining outcome for this item (assuming perfect skills)
	 */
	public List<ItemWithQuantity> getRefiningOutcome(InventoryType item); 
}
