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
package de.codesourcery.eve.skills.assets;

import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.utils.EveDate;

public interface IAssetManager
{

	public interface IAssetChangeListener {
	
		public void assetsChanged(ICharacter character);
		
	}
	
	/**
	 * Returns the timestamp when assets where 
	 * last reconciled with the server for a given character.
	 * @return
	 */
	public EveDate getTimestamp(ICharacter character);
	
	public void addAssetChangeListener(IAssetChangeListener listener);
	
	public void removeAssetChangeListener(IAssetChangeListener listener);
	
	public AssetList getAssets(ICharacter character);
	
	public AssetList getAssets(ICharacter character,ILocation location);
	
	public AssetList getAssets(ICharacter character,InventoryType item);
	
	
}
