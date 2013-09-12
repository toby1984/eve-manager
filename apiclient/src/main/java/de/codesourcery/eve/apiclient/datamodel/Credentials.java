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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.eve.apiclient.ICredentialsProvider;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyRole;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyType;
import de.codesourcery.eve.apiclient.exceptions.MissingAPIKeyException;

/**
 * Credentials used for authentication against the
 * EVE Online(tm) API.
 *
 * Most of this classes methods may 
 * throw an {@link CredentialsInvalidatedException}
 * when invoked on an already invalidated instance.
 * 
 * @author tobias.gierke@code-sourcery.de
 * @see #invalidate()
 */
public final class Credentials implements ICredentialsProvider {

	private long userId;
	private APIKey limitedKey;
	private APIKey fullAccessKey;
	private boolean valid = true;
	
	public Credentials() {
	}
	
	public Credentials(long userId, APIKey... key) {
		setUserId( userId );
		setKeys( key );
	}
	
	/**
	 * Sets the API userid.
	 * 
	 * @param userId
	 */
	public void setUserId(long userId) {
		assertValid();
		this.userId = userId;
	}

	/**
	 * Returns the API userid.
	 * @return
	 */
	public long getUserId() {
		assertValid();
		return userId;
	}
	
	/**
	 * Check whether these credentials
	 * are still valid.
	 * 
	 * @throws CredentialsInvalidatedException if this instance
	 * is no longer valid
	 * @see #invalidate()
	 */
	protected void assertValid() {
		if ( ! valid ) {
			throw new IllegalStateException("Method invoked on invalidated credentials?");
		}
	}
	
	/**
	 * Check whether these credentials
	 * are still valid.
	 * 
	 * @return
	 * @see #invalidate()
	 */
	public boolean isValid() {
		return valid;
	}
	
	/**
	 * Invalidate credentials.
	 * @see #assertValid()
	 */
	public void invalidate() {
		valid = false;
		userId = 0;
		limitedKey = null;
		fullAccessKey = null;
	}

	/**
	 * Set API key.
	 * 
	 * @param key
	 * @throws IllegalArgumentException on <code>null</code> keys.
	 */
	public void setKey(APIKey key) {
		
		assertValid();
		
		if (key == null) {
			throw new IllegalArgumentException("key cannot be NULL");
		}
		
		if ( key.hasType( KeyType.LIMITED_KEY ) ) {
			this.limitedKey = key;
		} else if ( key.hasType( KeyType.FULL_ACCESS) ) {
			this.fullAccessKey = key;
		} else {
			throw new RuntimeException("Internal error, key with unhandled type "+key.getType() );
		}
	}

	/**
	 * Checks whether this credentials object
	 * has a key with a specific type.
	 * 
	 * @param type
	 * @return
	 */
	public boolean hasKey(KeyType type) {
		
		if ( type == null) {
			throw new IllegalArgumentException("key type cannot be NULL");
		}
		
		switch( type ) {
			case LIMITED_KEY:
				return this.limitedKey != null;
			case FULL_ACCESS:
				return this.fullAccessKey != null;
			default:
					throw new RuntimeException("internal error , unhandled key type "+type);
		}
	}
	
	public APIKey getKeyForRole(KeyRole role) throws MissingAPIKeyException {
		
		if (role == null) {
			throw new IllegalArgumentException("key role cannot be NULL");
		}
		
		final KeyType[] requiredTypes;
		
		switch( role ) {
			case LIMITED_ACCESS:
				requiredTypes= new KeyType[]{ KeyType.LIMITED_KEY , KeyType.FULL_ACCESS };
				break;
			case FULL_ACCESS:
				requiredTypes= new KeyType[]{ KeyType.FULL_ACCESS };
				break;
			default:
				throw new RuntimeException("Unhandled role: "+role);
		}
		
		for ( KeyType t : requiredTypes ) {
			if ( hasKey( t ) ) {
				return getKeyWithType( t );
			}
		}
		
		throw new MissingAPIKeyException( requiredTypes );
	}

	/**
	 * Returns an API key by type.
	 * 
	 * @param type
	 * @return
	 * @throws IllegalArgumentException on <code>null</code> / invalid key types.
	 * @throws MissingAPIKeyException
	 */
	public APIKey getKeyWithType(KeyType type) throws MissingAPIKeyException {
		
		assertValid();
		
		if ( ! hasKey( type ) ) {
			throw new MissingAPIKeyException( type );
		}
		
		if ( KeyType.LIMITED_KEY.equals( type ) ) {
			return this.limitedKey;
		} else if ( KeyType.FULL_ACCESS.equals( type ) ) {
			return this.fullAccessKey;
		}
		
		throw new IllegalArgumentException("Invalid key type: "+type);
	}

	public List<APIKey> getKeys() {
		
		final List<APIKey> result =
			new ArrayList<APIKey>();
		
		if ( hasKey( KeyType.FULL_ACCESS ) ) {
			result.add( getKeyWithType( KeyType.FULL_ACCESS ) );
		}
		
		if ( hasKey( KeyType.LIMITED_KEY ) ) {
			result.add( getKeyWithType( KeyType.LIMITED_KEY) );
		}
		return result;
	}

	public void setKeys(APIKey[] keys) {
		
		this.fullAccessKey = null;
		this.limitedKey = null;
		
		if ( ! ArrayUtils.isEmpty( keys ) ) {
			for ( APIKey k : keys ) {
				setKey( k );
			}
		}
	}


}
