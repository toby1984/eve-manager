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
package de.codesourcery.eve.apiclient.exceptions;

import de.codesourcery.eve.apiclient.IAPIClient.EntityType;

/**
 * Thrown when resolving the name for a entity ID (character ID, corporation ID, etc.)
 * failed (ID is probably wrong / unknown ).
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class UnresolvableIDException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private final String id;
	private final EntityType idType;
	
	public UnresolvableIDException(String id, EntityType entityType) {
		super("Unable to resolve name for "+entityType+" ID '"+id+"'");
		this.id = id;
		this.idType = entityType;
	}

	public String getId() {
		return id;
	}

	public EntityType getEntityType() {
		return idType;
	}
	
}
