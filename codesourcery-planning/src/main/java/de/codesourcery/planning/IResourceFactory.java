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

import java.util.Date;

/**
 * A resource factory is responsible for creating new {@link IResource} instances
 * and keeping track of actual resource amounts.
 * 
 * For various reasons, a resource factory may not know the actual
 * resource amount for the current date but only for a past date. This
 * should be reflected by {@link #getTimestamp()}.
 * 
 * @author tobias.gierke@code-sourcery.de
 * @see IResourceManager
 */
public interface IResourceFactory
{
	
	/**
	 * Create a new resource instance.
	 * 
	 * The returned {@link IResource} should be populated
	 * with the resource amount at the given location. The amount
	 * relates to the date returned by {@link #getTimestamp()}
	 * and may not necessarily be the actual amount at the current time.
	 * 
	 * @param type
	 * @param location
	 * @return
	 * @see #getTimestamp()
	 */
	public IResource createResource(IResourceType type,IProductionLocation location);
	
	/**
	 * Clones a resource.
	 * 
	 * <b>Must</b> <code>synchronize</code> on the resource 
	 * instance while cloning.
	 * 
	 * @param resource
	 * @return
	 */
	public IResource cloneResource(IResource resource);
	
	/**
	 * Returns the date on which resource amounts are based.
	 * 
	 * A resource factory may not have access to current 
	 * data and therefore rely on the last known state. The
	 * date returned is the 'last-known-good' date.
	 * 
	 * 
	 * @return
	 */
	public Date getTimestamp();
}
