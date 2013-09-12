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
package de.codesourcery.eve.skills.db.datamodel;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.datamodel.CorporationId;
import de.codesourcery.eve.skills.datamodel.INamedEntity;

public class Corporation implements INamedEntity {

	private String name;
	
	private CorporationId id;
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setId(CorporationId id) {
		this.id = id;
	}
	public CorporationId getId() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Corporation) && 
			ObjectUtils.equals( ((Corporation) obj).getId() , this.id );
	}
	
	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}
	
	public Corporation cloneCorporation() {
		
		final Corporation result = new Corporation();
		result.id = id;
		result.name = name;
		
		return result;
	}
	
}
