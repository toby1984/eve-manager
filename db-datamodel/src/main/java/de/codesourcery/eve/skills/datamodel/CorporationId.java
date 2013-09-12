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


/**
 * Primary key that uniquely identifies a corporation.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class CorporationId implements IClientId {

	private final long id;
	
	public  CorporationId(long id) {
		this.id = id;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof CorporationId) && this.id == ((CorporationId) obj).getValue();
	}
	
	@Override
	public int hashCode() {
		return (int) ( (this.id & 0xff00 ) >> 16 ^( this.id & 0xff) ); 
	}

	public long getValue() {
		return this.id;
	}

	@Override
	public CharacterID asCharacterId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CorporationId asCorporationId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCharacterId()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCorporationId()
	{
		// TODO Auto-generated method stub
		return false;
	}
}