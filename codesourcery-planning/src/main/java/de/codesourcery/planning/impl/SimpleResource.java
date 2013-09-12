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

import de.codesourcery.planning.IResource;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.IResourceType;

public class SimpleResource implements IResource
{
	// guarded-by: 'this'
	private double amount;
	
	private final IResourceType type;
	private final IProductionLocation location;
	
	public SimpleResource( IResourceType type , IProductionLocation location , double amount) {
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		if ( location == null ) {
			throw new IllegalArgumentException("location cannot be NULL");
		}
		this.type = type;
		this.amount = amount;
		this.location = location;
	}
	
	public SimpleResource(SimpleResource simpleResource) {
		if ( simpleResource == null ) {
			throw new IllegalArgumentException("simpleResource cannot be NULL");
		}
		this.amount = simpleResource.getAmount();
		this.location = simpleResource.getLocation();
		this.type = simpleResource.getType();
	}

	public boolean isDepleted() {
		return getAmount() <= 0;
	}
	
	@Override
	public synchronized double getAmount()
	{
		return amount;
	}
	
	public synchronized void incrementAmount(double amount) {
		if ( amount < 0 ) {
			throw new IllegalArgumentException("Won't increment by negative amount");
		}
		this.amount += amount;
	}
	
	public synchronized void decrementAmount(double amount) {
		if ( amount < 0 ) {
			throw new IllegalArgumentException("Won't decrement by negative amount");
		}
		this.amount -= amount;
	}

	@Override
	public IResourceType getType()
	{
		return type;
	}

	@Override
	public IProductionLocation getLocation()
	{
		return location;
	}

	@Override
	public String toString()
	{
		return "Resource[ type="+getType()+" , amount="+getAmount()+" , location="+getLocation()+" ]";
	}

}
