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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.codesourcery.eve.skills.db.datamodel.AttributeType;

public class ImplantSet implements Iterable<Implant>{

	private final Object LOCK = new Object();

	// guarded-by: LOCK
	private final Map<Integer,Implant> implants =
		new HashMap<Integer,Implant>();


	/**
	 * Reconciles this implant set
	 * with the implants from a character.
	 * 
	 * @param payload
	 */
	public void reconcile(ICharacter payload) {

		final ImplantSet other =
			payload.getImplantSet();

		synchronized( LOCK ) {
			
			final Map<Integer,Implant> tmp=
				new HashMap<Integer,Implant>();

			for( Implant imp : other ) {
				tmp.put( imp.getSlot(), imp );
			}
			this.implants.clear();
			this.implants.putAll( tmp );
		}

	}

	public Iterator<Implant> iterator() {
		synchronized( LOCK ) {
			return new ArrayList<Implant>( this.implants.values() ).iterator();
		}
	}

	public int getAttributeModifier(AttributeType attribute) {

		synchronized( LOCK ) {
			for ( Implant implant : implants.values() ) {
				if ( implant instanceof AttributeEnhancer) {
					final AttributeEnhancer enh = (AttributeEnhancer) implant;
					if ( enh.getAttribute().equals( attribute ) ) {
						return enh.getModifier();
					}
				}
			}
			return 0;
		}
	}

	public Implant getImplantFromSlot(int slot) {
		if ( slot < Implant.FIRST_SLOT || slot > Implant.LAST_SLOT ) {
			throw new IllegalArgumentException("Invalid slot "+slot);
		}
		synchronized( LOCK ) {
			return implants.get(slot );
		}
	}

	public void removeAll() {
		synchronized( LOCK ) {
			implants.clear();
		}
	}

	public void removeImplant(Implant implant) {
		if (implant== null) {
			throw new IllegalArgumentException("implant cannot be NULL");
		}
		synchronized( LOCK ) {
			this.implants.remove( implant.getSlot() );
		}
	}

	public void setImplant(Implant implant) {

		if (implant== null) {
			throw new IllegalArgumentException("implant cannot be NULL");
		}

		synchronized( LOCK ) {
			implants.remove( implant );
			implants.put( implant.getSlot() , implant);
		}
	}

	/**
	 * Returns the attribute enhancer of a given type.
	 * @param a
	 * @return attribute enhancer of <code>null</code> if character
	 * has no attribute enhancer of this type.
	 */
	public AttributeEnhancer getAttributeEnhancer(AttributeType a) {
		synchronized( LOCK ) {
			for ( Implant implant : implants.values() ) {
				if ( implant instanceof AttributeEnhancer) {
					AttributeEnhancer enh = (AttributeEnhancer) implant;
					if ( enh.getAttribute().equals( a ) ) {
						return enh;
					}
				}
			}
			return null;
		}
	}

	public ImplantSet cloneImplants() {
		final ImplantSet result = new ImplantSet();
		synchronized( LOCK ) {
			result.implants.putAll( new HashMap<Integer,Implant>( this.implants ) );
		}
		return result;
	}

}
