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

public abstract class AbstractViewFilter<T> implements IViewFilter<T>{

	private IViewFilter<T> delegate;
	
	@Override
	public final boolean isHidden(T value) {
		
		boolean result = false;
		if ( delegate != null ) {
			result = delegate.isHidden( value );
			if ( result ) {
				return true;
			}
		}
		return isHiddenUnfiltered( value );
	}
	
	public abstract boolean isHiddenUnfiltered(T value);

	@Override
	public final void addFilter(IViewFilter<T> filter) {
		
		if ( filter == null ) {
			throw new IllegalArgumentException("Cannot add NULL filter");
		}
		
		if ( this.delegate != null ) {
			filter.addFilter( this.delegate );
		} 
		this.delegate = filter;
	}
	
	@SuppressWarnings("unchecked")
	public final <X extends IViewFilter<T>> X getFilter(Class<X> clasz) {
		
		if ( this.delegate == null ) {
			return null;
		}
		
		if ( this.delegate.getClass() == clasz ) {
			return (X) this.delegate;
		}
		return this.delegate.getFilter( clasz );
	}

	@Override
	public final boolean removeFilter(IViewFilter<T> filter) {
		
		if ( filter == null || this.delegate == null ) {
			return false;
		}
		
		if ( this.delegate == filter ) {
			this.delegate = this.delegate.getDelegate();
			return true;
		} 

		return this.delegate.removeFilter( filter );
	}

	@Override
	public final IViewFilter<T> getDelegate() {
		return this.delegate;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder( this.getClass().getName() );
		privateToString( result , this.delegate );
		return result.toString();
	}
	
	protected void privateToString(StringBuilder builder,IViewFilter<T> filter) {
		if ( filter == null ) {
			return;
		}
		builder.append(" -> ").append( filter.getClass().getName() );
		privateToString( builder , filter.getDelegate() );
	}
	

}
