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
package de.codesourcery.eve.skills.ui.model;

/**
 * Strategy that determines the visibility of some view's items.
 * 
 * A filter may itself be linked with other filters
 * in a delegation chain. A filter only declares
 * an item as being visible if the filter itself
 * and all it's delegates agree.
 *   
 * @author tobias.gierke@code-sourcery.de
 * 
 * @see AbstractTableModel#setViewFilter(IViewFilter)
 */
public interface IViewFilter<T> {

	/**
	 * Check whether an item should be hidden.
	 * 
	 * @param value data
	 * @return <code>true</code> if the object matches this filter, otherwise false.
	 */
	public boolean isHidden(T value);
	
	public <X extends IViewFilter<T>> X getFilter(Class<X> clasz);
	
	public IViewFilter<T> getDelegate();
	
	public void addFilter(IViewFilter<T> filter);
	
	public boolean removeFilter(IViewFilter<T> filter);

}
