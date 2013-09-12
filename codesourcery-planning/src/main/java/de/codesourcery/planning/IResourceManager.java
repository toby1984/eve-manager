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
package de.codesourcery.planning;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Manages a pool of production resources (items).
 * 
 * Implementations manages a virtual pool
 * of production resources. Because this 
 * resource pool is virtual, resources may very well
 * have a negative amount/quantity (indicating
 * the lack of a given resource at some location).
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IResourceManager
{
	/**
	 * Check whether a given resource type may be 
	 * used simultanously by multiple jobs.
	 * 
	 * Note that a shared resource cannot be 
	 * {@link #consume(IResourceHandle)}ed since
	 * otherwise it could not be shared  in the
	 * first place. 
	 * 
	 * @return <code>true</code> if this resource
	 * may be shared by multiple users 
	 * at the same time.
	 */
	public boolean isShareable(IResourceType resource);
	
	/**
	 * Takes a snapshot of the resource
	 * manager's current state.
	 * 
	 * This is an expensive operation
	 * since all managed {@link IResource} instances
	 * will be cloned.
	 * 
	 * @return independent copy of this resource manager.
	 */
	public IResourceManager snapshot();
	
	/**
	 * Consumes a specific amount of a given resource
	 * from the resource pool.
	 * 
	 * @param resource
	 * @param when
	 * @param owner
	 * @param amount
	 * @return
	 * @throws IllegalArgumentException if resource or location is <code>null</code> or the amount is negative
	 * @throws UnsupportedOperationException if trying to <code>consume()</code> a sharable resource
	 */
	public void consume(IResourceType resource , IProductionLocation location, double amount);
	
	/**
	 * Registers a new resources.
	 * 
	 * @param resource
	 */
	public void produce(IResourceType resource , IProductionLocation location, double amount);
	
	/**
	 * Returns all resources at a given location.
	 * 
	 * @param location location, may be {@link IProductionLocation#ANY_LOCATION}
	 * to request all available resources
	 * @return
	 */
	public List<IResource> getResourcesAt(IProductionLocation location);
	
	/**
	 * Calculates the available (or missing) quantities
	 * after one or more resource types have been consumed.
	 * 
	 * This method does NOT change the actual state of any of 
	 * the resources managed by this resource manager.
	 * 
	 * @param items Resource types with quantities that should be consumed
	 * @param location either a specific location or {@link IProductionLocation#ANY_LOCATION}.
	 * @return
	 */
	public Map<IResourceType, IResource> calculateProjectedResourceStatus(Collection<? extends IResourceQuantity> items , 
			IProductionLocation location);
	
	/**
	 * Returns all resources with a given type.
	 * 
	 * @param type
	 * @param location location where to look for resources 
	 * of the given type (may be {@link IProductionLocation#ANY_LOCATION} but 
	 * never <code>null</code>). 
	 * @return
	 * 
	 * @see ILocation#ANY_LOCATION
	 */
	public IResource getResource(IResourceType type , IProductionLocation location);
	
	/**
	 * Sets the resource factory implementation responsible for creating new {@link IResource}.
	 * 
	 * @param factory
	 */
	public void setResourceFactory(IResourceFactory factory);
	
	/**
	 * Returns the time when the resources and resource amounts
	 * managed by this instance have last been reconciled
	 * with the backing store (if any).
	 * 
	 * Resource managers that do not support/have a backing store
	 * will always return the current time here.
	 *  
	 * @return
	 */
	public Date getTimestamp();

}
