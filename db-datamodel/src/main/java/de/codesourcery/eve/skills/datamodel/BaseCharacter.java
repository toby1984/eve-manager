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




public class BaseCharacter implements IBaseCharacter {

	/**
	 * EVE Online API character ID.
	 */
	private CharacterID characterId; 
	private String name;
	
	public BaseCharacter() {
	}
	
	public BaseCharacter(String name) {
		setName( name );
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.datamodel.ICharacter#cloneCharacter()
	 */
	protected final void cloneCharacter(IBaseCharacter c) {
		c.setName( this.name );
		c.setCharacterId( this.characterId );
	}
	
	public BaseCharacter(String name,CharacterID id) {
		setName( name );
		setCharacterId( id );
	}
	
	public void setCharacterId(CharacterID id) {
		this.characterId = id;		
	}

	@Override
	public CharacterID getCharacterId() {
		return characterId;
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}	

}
