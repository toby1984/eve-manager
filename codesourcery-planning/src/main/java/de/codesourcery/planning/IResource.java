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

/**
 * A production resource.
 * 
 * Note that implementations need to be thread-safe
 * with regards to {@link #incrementAmount(double)}
 * , {@link #decrementAmount(double)} and {@link #getAmount()}.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IResource {
	
	/**
	 * NOT PART OF PUBLIC API.
	 * 
	 * May only be called from within {@link IResourceManager} implementations.
	 * 
	 * ALWAYS <code>synchronize</code> on this resource instance when
	 * invoking this method.
	 * 
	 * @param amount
	 */
	public void incrementAmount(double amount);
	
	/**
	 * NOT PART OF PUBLIC API.
	 * 
	 * May only be called from within {@link IResourceManager} implementations.
	 * 
	 * ALWAYS <code>synchronize</code> on this resource instance when
	 * invoking this method.
	 * 
	 * @param amount
	 */	
	public void decrementAmount(double amount);
	
	public double getAmount();
	
	public IResourceType getType();
	
	public IProductionLocation getLocation();

	/**
	 * Returns whether this resource is
	 * depleted.
	 * 
	 * @return <code>true</code> if {@link #getAmount()}
	 * will return a negative value for this resource.
	 */
	public boolean isDepleted();
}
