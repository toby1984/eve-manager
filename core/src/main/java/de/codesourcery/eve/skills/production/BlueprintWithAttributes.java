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

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;

public abstract class BlueprintWithAttributes
{
	protected CharacterID owningCharacterId;
	private final Long blueprintTypeId;

	protected boolean foundInAssets;
	protected int meLevel;
	protected int peLevel;
	
	public BlueprintWithAttributes(CharacterID owningCharacterId , Long blueprintTypeId,boolean foundInAssets) {
		this( owningCharacterId , blueprintTypeId , 0, 0 , foundInAssets );
	}
	
	public BlueprintWithAttributes(CharacterID owningCharacterId , Long blueprintTypeId,int meLevel,int peLevel,boolean foundInAssets) 
	{
		if ( blueprintTypeId == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}
		if ( owningCharacterId == null ) {
			throw new IllegalArgumentException("characterId cannot be NULL");
		}
		this.foundInAssets = foundInAssets;
		this.owningCharacterId = owningCharacterId;
		this.blueprintTypeId =  blueprintTypeId;
		this.meLevel = meLevel;
		this.peLevel = peLevel;
	}
	
	/**
	 * Subclassing hook for lazy loading.
	 * @return
	 */
	public abstract Blueprint getBlueprint();

	public Long getBlueprintTypeId() {
		return blueprintTypeId;
	}
	
	public boolean isOwnedBy(ICharacter c) {
		if ( c == null ) {
			throw new IllegalArgumentException("character cannot be NULL");
		}
		return c.getCharacterId().equals( this.owningCharacterId );
	}
	
	public int getMeLevel()
	{
		return meLevel;
	}
	
	public int getPeLevel()
	{
		return peLevel;
	}

	public CharacterID getOwningCharacterId()
	{
		return owningCharacterId;
	}

	public boolean isFoundInAssets()
	{
		return foundInAssets;
	}
}