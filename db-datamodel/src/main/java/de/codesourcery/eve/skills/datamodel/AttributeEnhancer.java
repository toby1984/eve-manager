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

import de.codesourcery.eve.skills.db.datamodel.AttributeType;




public final class AttributeEnhancer implements Implant {

	private final AttributeType attribute;
	private final int slot;
	private final int modifier;
	
	public AttributeEnhancer(AttributeType attribute,int modifier,int slot) {
		if ( attribute == null ) {
			throw new IllegalArgumentException("attribute cannot be NULL");
		}
		this.attribute = attribute;
		this.modifier = modifier;
		this.slot = slot;
	}
	
	public AttributeEnhancer(AttributeType attribute,int modifier) {
		if ( attribute == null ) {
			throw new IllegalArgumentException("attribute cannot be NULL");
		}
		this.attribute = attribute;
		
		switch( attribute ) {
			case MEMORY:
				this.slot = 2;
				break;
			case INTELLIGENCE:
				this.slot = 4;
				break;
			case WILLPOWER:
				this.slot = 3;
				break;
			case PERCEPTION:
				this.slot = 1;
				break;
			case CHARISMA:
				this.slot = 5;
				break;
			default:
				throw new RuntimeException("Unhandled attribute type "+attribute);
		}
		this.modifier = modifier;
	}

	@Override
	public boolean equals(Object obj) {
		return ( obj instanceof AttributeEnhancer) &&
		((AttributeEnhancer) obj).attribute.equals( this.attribute );
	}
	
	public int getSlot() {
		return slot;
	}
	
	@Override
	public int hashCode() {
		return attribute.hashCode();
	}
	
	public int getModifier() {
		return modifier;
	}

	public AttributeType getAttribute() {
		return attribute;
	}

	@Override
	public Implant cloneImplant() {
		return this;
	}
	
}
