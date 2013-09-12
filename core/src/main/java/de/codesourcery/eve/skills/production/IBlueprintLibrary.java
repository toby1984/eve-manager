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

import java.util.List;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

/**
 * Keeps track of all blueprints a character owns , including
 * their ME / PE levels.
 * 
 * The EVE Online(tm) API currently does not support querying
 * blueprint ME / PE levels , so these need to be provided
 * by the user and managed separately.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IBlueprintLibrary
{

	public interface IBlueprintLibraryChangeListener {
		
		public void blueprintsChanged(
				List<? extends BlueprintWithAttributes> addedBlueprints,
				List<? extends BlueprintWithAttributes> changedBlueprints,
				List<? extends BlueprintWithAttributes> removedBlueprints
		);
		
	}
	
	public void addChangeListener(IBlueprintLibraryChangeListener listener);
	
	public void removeChangeListener(IBlueprintLibraryChangeListener listener);
	
	public List<? extends BlueprintWithAttributes> getBlueprints();
	
	public List<? extends BlueprintWithAttributes> getBlueprints(ICharacter owningCharacter);
	
	public List<? extends BlueprintWithAttributes> getBlueprints(ICharacter owningCharacter , Blueprint... blueprints);
	
	public List<? extends BlueprintWithAttributes> getBlueprints(Blueprint blueprint);
	
	public List<? extends BlueprintWithAttributes> getBlueprints(InventoryType blueprintType);
	
	public boolean containsBlueprint( InventoryType blueprintType );
	
	public boolean containsBlueprint( Blueprint blueprint );
	
	public boolean ownsBlueprint( ICharacter owningCharacter , Blueprint blueprint );
	
	public boolean ownsBlueprint( ICharacter owningCharacter , InventoryType blueprintType );
	
	public void addBlueprint( ICharacter owningCharacter , Blueprint blueprint, int meLevel , int peLevel); 
	
	public void removeBlueprint(BlueprintWithAttributes blueprint);
	
	public void updateOwningCharacter(BlueprintWithAttributes blueprint,ICharacter newOwner);
	
	public void update(BlueprintWithAttributes blueprint, int meLevel, int peLevel ); 
}
