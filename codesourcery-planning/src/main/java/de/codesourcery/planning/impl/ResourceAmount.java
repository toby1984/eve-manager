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
package de.codesourcery.planning.impl;

import de.codesourcery.planning.IResourceType;

public class ResourceAmount 
{
	
	private final double amount;
	private final IResourceType type;
	
	public ResourceAmount(IResourceType type,double amount) {
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		if ( amount < 0 ) {
			throw new IllegalArgumentException("amount cannot be < 0 ");
		}
		this.type = type;
		this.amount = amount;
	}
	
	public double getAmount()
	{
		return amount;
	}
	
	public IResourceType getResourceType()
	{
		return type;
	}
}