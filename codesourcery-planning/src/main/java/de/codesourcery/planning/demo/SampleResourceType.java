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
package de.codesourcery.planning.demo;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.planning.IResourceType;

public class SampleResourceType implements IResourceType
{

	private final String name;
	
	public SampleResourceType(String name) {
		
		if ( StringUtils.isBlank(name) ) {
			throw new IllegalArgumentException("name cannot be blank.");
		}
		this.name = name;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return ( obj instanceof SampleResourceType ) && ((SampleResourceType) obj).name.equals( this.name );
	}
	
	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
	
	@Override
	public String toString()
	{
		return name;
	}

}
