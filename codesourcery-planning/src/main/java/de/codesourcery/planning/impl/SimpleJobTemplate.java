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
import java.util.List;

import de.codesourcery.planning.IJobTemplate;

public class SimpleJobTemplate implements IJobTemplate
{
	private final JobMode mode;
	private final List<IJobTemplate> dependencies = new ArrayList<IJobTemplate>();
	private final int runs;
	
	public SimpleJobTemplate(int runs , JobMode type) {
		if ( type == null ) {
			throw new IllegalArgumentException("job mode cannot be NULL");
		}
		if ( runs <= 0 ) {
			throw new IllegalArgumentException("runs cannot be <= 0");
		}
		this.runs = runs;
		this.mode = type;
	}
	
	public void addDependency(IJobTemplate other) {
		if ( other == null ) {
			throw new IllegalArgumentException("dependency cannot be NULL");
		}
		this.dependencies.add( other );
	}
	
	@Override
	public List<IJobTemplate> getDependencies()
	{
		return dependencies;
	}
	
	@Override
	public JobMode getJobMode()
	{
		return mode;
	}

	@Override
	public boolean hasJobMode(JobMode type)
	{
		return this.mode == type;
	}
	
	@Override
	public int getRuns()
	{
		return runs;
	}

	@Override
	public boolean supportsParallelExecution()
	{
		return false;
	}


}
