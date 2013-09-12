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
package de.codesourcery.eve.apiclient.datamodel;

import org.apache.commons.lang.StringUtils;

/**
 * EVE Online(tm) API key.
 * 
 * Instances <b>MUST</b> be immutable (design assumption).
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class APIKey {
	
	/**
	 * API key types.
	 *
	 * API client code should not request keys with
	 * a specific type but rather with a specific
	 * {@link KeyRole}. This prevents code breakage 
	 * when new key types are introduced
	 * or permissions of existing key types change. 
	 *   
	 * Limited API keys currently are the key type with the
	 * fewest access permissions. All other key types are expected  
	 * to permit at least the same operations as a limited API key.
	 * 	
	 * @author tobias.gierke@code-sourcery.de
	 * @see Credentials#getKeyForRole(KeyRole)
	 * @see Credentials#getKeyWithType(KeyType)
	 */
	public enum KeyType {
		LIMITED_KEY,
		FULL_ACCESS;
	};
	
	/**
	 * API key roles.
	 *
	 * API client code should not request keys with
	 * a specific type but rather with a specific
	 * {@link KeyRole}. This prevents code breakage 
	 * when new key types are introduced
	 * or permissions of existing key types change. 
	 * 	
	 * @author tobias.gierke@code-sourcery.de
	 * @see Credentials#getKeyForRole(KeyRole)
	 * @see Credentials#getKeyWithType(KeyType)	 
	 */
	public enum KeyRole {
		NONE_REQUIRED,
		LIMITED_ACCESS,
		FULL_ACCESS;
	}

	private final String value;
	private final KeyType type;
	
	/**
	 * Creates a limited API key.
	 * 
	 * This key currently only provides access 
	 * to a subset of the API's functions.
	 * 
	 * @param value
	 * @return
	 */
	public static APIKey createLimitedKey(String value) {
		return new APIKey( value , KeyType.LIMITED_KEY );
	}
	
	/**
	 * Creates a full API key.
	 * 
	 * This key currently provides access
	 * to all available API functions.
	 * 
	 * @param value
	 * @return
	 */
	public static APIKey createFullAccessKey(String value) {
		return new APIKey( value , KeyType.FULL_ACCESS );
	}
	
	@Override
	public String toString() {
		return "APIKey[ "+type+" , has_value="+StringUtils.isNotBlank( value )+" ]";
	}

	/**
	 * Create instance.
	 * 
	 * You might want to use the static factory methods
	 * instead of calling this constructor....
	 * 
	 * @param value The actual <code>apiKey</code> value to be transmitted to the EVE Online(tm) API.
	 * @param type the key type
	 * @throws IllegalArgumentException On NULL/invalid method arguments
	 * @see #createFullAccessKey(String)
	 * @see #createLimitedKey(String)
	 */
	public APIKey(String value,KeyType type) {
		
		if ( value == null ) {
			throw new IllegalArgumentException("key cannot be NULL");
		}
		
		if ( type == null ) {
			throw new IllegalArgumentException("key type cannot be NULL");
		}
		
		this.value=value;
		this.type = type;
	}
	
	/**
	 * Returns the actual <code>apiKey</code> value to be 
	 * transmitted to the EVE Online(tm) API.
	 * @return
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Checks whether this key is of a specific type.
	 * @param type
	 * @return
	 */
	public boolean hasType(KeyType type) {
		if (type == null) {
			throw new IllegalArgumentException("key type cannot be NULL");
		}
		return type.equals( this.type );
	}

	/**
	 * Returns this API key's type.
	 * 
	 * @return
	 */
	public KeyType getType() {
		return type;
	}
}
