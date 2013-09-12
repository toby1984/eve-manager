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

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.codesourcery.eve.apiclient.cache.InMemoryResponseCache;

/**
 * Unique hashCode for an API request. 
 *
 *<pre>
 * This class is used by the {@link InMemoryResponseCache} to
 * implement caching support. Proper <code>equals()</code>
 * / <code>hashCode()</code> implementations are <b>mandatory</b>.
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 */
public class APIQuery {
	
	private static final char[] HEX = { '0' , '1' , '2', '3' , '4','5','6','7','8','9',
		'a' , 'b' , 'c' , 'd' , 'e' , 'f' };

	private final String hashCode;
	
	private static final String toString(byte[] data) {
		final StringBuilder result =
			new StringBuilder();
		
		for ( byte b : data ) {
			result.append( HEX[ ( b & 0xf0 ) >> 4 ] );
			result.append( HEX[ b & 0xf ] );
		}
		return result.toString();
	}
	
	@Override
	public String toString() {
		return "APIQuery[ hashCode="+hashCode+" ]";
	}
	
	/**
	 * Internal use only.
	 * 
	 * @param hashCode
	 */
	public APIQuery(String hashCode) {
		this.hashCode = hashCode;
	}
	
	/**
	 * Create instance.
	 * 
	 * @param baseURI the query's base URI (protocol, server, port, base path)
	 * @param relativeURI the query's relative URL (aka 'invoked method name').
	 * @param params ALL query parameters
	 */
	public APIQuery(URI baseURI , String relativeURI ,Map<String,Object> params) {
		
		if ( baseURI == null ) {
			throw new IllegalArgumentException("baseURI cannot be NULL");
		}
		
		if ( relativeURI == null ) {
			throw new IllegalArgumentException("relativeURI cannot be NULL");
		}
		
		if ( params == null ) {
			throw new IllegalArgumentException("params cannot be NULL");
		}
		
		final MessageDigest digest;
		try {
			digest =
				MessageDigest.getInstance("SHA1");
		} 
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
		digest.update( baseURI.resolve( relativeURI ).toString().getBytes() );
		
		// need to sort key since java.lang.Map has no fixed iteration order
		final List<String> sortedKeys = new ArrayList<String>( params.keySet() );
		Collections.sort( sortedKeys );
		
		
		for ( String key : sortedKeys ) {
			digest.update( key.getBytes() );
			digest.update( params.get( key ).toString().getBytes() );
		}
		
		this.hashCode = toString( digest.digest() );
	}
	
	public String getHashString() {
		return hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof APIQuery ) &&
				this.hashCode.equals( ((APIQuery) obj).hashCode );
	}
	
	@Override
	public int hashCode() {
		return hashCode.hashCode();
	}
	
}
