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
package de.codesourcery.eve.skills.production.entity;

import de.codesourcery.eve.skills.datamodel.BlueprintInstance;
import de.codesourcery.eve.skills.production.IEveOnlineJob;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.impl.SimpleJob;

public abstract class EveOnlineJob extends SimpleJob implements IEveOnlineJob
{

	private final JobType jobType;
	private final BlueprintInstance blueprint;
	
	public static IEveOnlineJob createCopyJob(BlueprintInstance blueprint,IJobTemplate template) {
		return new EveOnlineJob( blueprint , JobType.COPYING , Duration.UNKNOWN , template ) {};
	}
	
	public static IEveOnlineJob createInventionJob(BlueprintInstance blueprint,IJobTemplate template) {
		return new EveOnlineJob( blueprint , JobType.INVENTION , Duration.UNKNOWN , template ) {};
	}
	
	public static IEveOnlineJob createManufacturingJob(BlueprintInstance blueprint,IJobTemplate template) {
		return new EveOnlineJob( blueprint , JobType.MANUFACTURING , Duration.UNKNOWN , template ) {};
	}
	
	private EveOnlineJob(BlueprintInstance blueprint, JobType jobType , Duration duration, IJobTemplate jobTemplate) 
	{
		super( blueprint.getBlueprint().getName() , jobTemplate, duration);
		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}
		if ( jobType == null ) {
			throw new IllegalArgumentException("jobType cannot be NULL");
		}
		this.jobType = jobType;
		this.blueprint = blueprint;
	}
	
	public boolean isCopyJob() {
		return hasJobType( JobType.COPYING );
	}
	
	public BlueprintInstance getBlueprint() {
		return blueprint;
	}

	@Override
	public JobType getJobType()
	{
		return jobType;
	}

	@Override
	public boolean hasJobType(JobType type)
	{
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		return getJobType() == type;
	}

	@Override
	public boolean isInventionJob()
	{
		return hasJobType( JobType.INVENTION );
	}

	@Override
	public boolean isManufacturingJob()
	{
		return hasJobType( JobType.MANUFACTURING );
	}
	
	@Override
	public String toString()
	{
		return "EveOnlineJob[ type="+getJobType()+" , blueprint="+getBlueprint()+" , duration "+getDuration()+" ]";
	}

}
