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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.IResource;
import de.codesourcery.planning.IResourceFactory;
import de.codesourcery.planning.IResourceManager;
import de.codesourcery.planning.IResourceQuantity;
import de.codesourcery.planning.IResourceType;

public abstract class SimpleResourceManager implements IResourceManager
{
	private IResourceFactory resourceFactory;

	// guarded-by: resourcesByLocation
	private Map<IProductionLocation, Map<IResourceType, IResource>> resourcesByLocation = 
		new HashMap<IProductionLocation, Map<IResourceType, IResource>>();

	private final Object INIT_LOCK = new Object();

	// guarded-by: INIT_LOCK
	private boolean initialized = false;

	/**
	 * Subclassing hook , needs to be overridden.
	 * 
	 * @param result
	 */
	protected final void cloneInstance(SimpleResourceManager result) 
	{
		synchronized ( INIT_LOCK) {
			result.resourceFactory = getResourceFactory();
			result.resourcesByLocation = cloneResources();
			result.initialized = initialized;
		}
	}

	@Override
	public Map<IResourceType, IResource> calculateProjectedResourceStatus(
			Collection<? extends IResourceQuantity> items,
			IProductionLocation location)
			{

		if ( items == null ) {
			throw new IllegalArgumentException("items cannot be NULL");
		}

		init();

		final Map<IResourceType, IResource> result =
			new HashMap<IResourceType, IResource>();

		synchronized(resourcesByLocation) 
		{
			for ( IResourceQuantity item : items ) 
			{

				final IResourceType resourceType = item.getResourceType();
				
				for ( IProductionLocation loc : resourcesByLocation.keySet() ) 
				{
					if ( location != IProductionLocation.ANY_LOCATION ) {
						if ( ! loc.equals( location ) ) {
							continue;
						}
					}

					final IResource existing = result.get( resourceType );

					// NEED to clone() the resource here since we're manipulating
					// the 'quantity' property
					final IResource found = 
						getResourceFactory().cloneResource( getResource( resourceType , loc ) );

					if ( existing == null ) {
						result.put( resourceType , found );
					} else {
						if ( found.getAmount() >= 0 ) {
							existing.incrementAmount( found.getAmount() );
						} else {
							existing.decrementAmount( - found.getAmount() );
						}
					}
				}
			}

			for ( IResourceQuantity item : items ) 
			{
				IResource r = result.get( item.getResourceType() );
				if ( r == null ) {
					r = getResourceFactory().createResource( item.getResourceType() , location );
					result.put( item.getResourceType() , r );
				}
				r.decrementAmount( item.getResourceQuantity() );
			}
		}
		return result;
			}

	protected IResourceFactory getResourceFactory() {
		return resourceFactory;
	}

	protected final Map<IProductionLocation, Map<IResourceType, IResource>> cloneResources()
	{

		synchronized( resourcesByLocation ) 
		{
			final Map<IProductionLocation, Map<IResourceType, IResource>> original =
				resourcesByLocation;

			final Map<IProductionLocation, Map<IResourceType, IResource>> result =
				new HashMap<IProductionLocation, Map<IResourceType, IResource>> ();

			for ( IProductionLocation loc : original.keySet() ) 
			{
				final Map<IResourceType, IResource> cloned =
					new HashMap<IResourceType, IResource>();

				for ( IResource r : original.get( loc ).values() ) 
				{
					cloned.put( r.getType() , getResourceFactory().cloneResource( r ) );
				}
				result.put( loc , cloned );
			}
			return result;
		}

	}

	protected final void removeAllResources() 
	{
		synchronized (resourcesByLocation) 
		{
			resourcesByLocation.clear();
		}
	}

	/**
	 * Subclassing hook that is called
	 * exactly once for the lifetime
	 * of this resource manager.
	 * 
	 * Non-open call (INIT_LOCK is held).
	 */
	protected void initHook() {
	}

	protected final void init() 
	{
		synchronized( INIT_LOCK ) {
			if ( ! initialized ) {
				initHook();
				initialized = true;
			}
		}
	}

	@Override
	public void consume(IResourceType type, IProductionLocation location,
			double amount)
	{

		if ( amount < 0.0d ) {
			throw new IllegalArgumentException("amount cannot be negative");
		}

		if ( isShareable( type ) ) {
			throw new UnsupportedOperationException("Cannot consume() shared resource "+type);
		}

		init();

		findResource(type, location).decrementAmount(amount);
	}

	private final IResource findResource(IResourceType type,
			IProductionLocation location)
	{

		if ( location == null || location == IProductionLocation.ANY_LOCATION ) {
			throw new IllegalArgumentException("location cannot be NULL / ANY");
		}

		if ( type == null ) {
			throw new IllegalArgumentException("resource type cannot be NULL");
		}

		synchronized( resourcesByLocation ) 
		{
			final Map<IResourceType, IResource> resources = 
				resourcesByLocation.get(location);

			if ( resources == null ) {
				resourcesByLocation.put( location , new HashMap<IResourceType,IResource>() );
			}

			IResource resource = resources.get(type);
			if ( resource == null ) {
				resource = getResourceFactory().createResource( type , location );
				resources.put( type , resource );
			}
			return resource;
		}
	}

	@Override
	public final IResource getResource(IResourceType type,IProductionLocation location)
	{
		init();

		return findResource( type , location );
	}

	@Override
	public final List<IResource> getResourcesAt(IProductionLocation location)
	{

		if ( location == null ) {
			throw new IllegalArgumentException("location cannot be NULL");
		}

		init();

		final List<IResource> result = new ArrayList<IResource>();
		synchronized( resourcesByLocation ) 
		{
			if ( location == IProductionLocation.ANY_LOCATION ) 
			{
				for ( IProductionLocation loc : resourcesByLocation.keySet() ) {
					Map<IResourceType, IResource> byType = resourcesByLocation.get( loc );
					if ( byType != null ) {
						result.addAll( byType.values() );
					}
				}
			} else {
				final Map<IResourceType, IResource> byType = resourcesByLocation.get( location );
				if ( byType != null ) {
					result.addAll( byType.values() );
				}
			}
		}
		return result;
	}

	@Override
	public boolean isShareable(IResourceType resource)
	{
		return false;
	}

	/**
	 * Safe to call from within {@link #initHook()}.
	 * 
	 * @param resource
	 */
	protected final void internalSetResources(List<IResource> allResources) {

		synchronized( resourcesByLocation ) 
		{

			this.resourcesByLocation.clear();

			for ( IResource resource : allResources ) {
				final IProductionLocation location = resource.getLocation();

				Map<IResourceType, IResource> resourcesByType = 
					resourcesByLocation.get(location);

				if ( resourcesByType == null ) {
					resourcesByType = new HashMap<IResourceType,IResource>();
					resourcesByLocation.put( location , resourcesByType );
				}

				final IResource existing = resourcesByType.get( resource.getType() );

				if ( existing != null ) {
					throw new IllegalStateException("Resource "+resource+" already registered ?");
				}
				resourcesByType.put( resource.getType() , resource );
			}
		}
	}

	@Override
	public final void produce(IResourceType type , IProductionLocation location, double amount)
	{

		if ( amount < 0.0d ) {
			throw new IllegalArgumentException("amount cannot be negative");
		}

		init();

		findResource( type ,location ).incrementAmount( amount );
	}

	@Override
	public void setResourceFactory(IResourceFactory factory)
	{
		if ( factory == null ) {
			throw new IllegalArgumentException("factory cannot be NULL");
		}
		this.resourceFactory = factory;
	}


}
