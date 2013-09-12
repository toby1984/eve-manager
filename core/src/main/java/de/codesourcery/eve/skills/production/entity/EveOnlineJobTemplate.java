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

import org.apache.commons.lang.ArrayUtils;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.impl.SimpleJobTemplate;

public abstract class EveOnlineJobTemplate extends SimpleJobTemplate 
{

	public enum Type {
		MANUFACTURING_JOB,
		INVENTION_JOB,
		COPY_JOB;
	}
	
	private final Blueprint blueprint;
	private boolean runInParallel;
	
	public EveOnlineJobTemplate(Blueprint blueprint , int runs) {
		super(runs, JobMode.MANUAL );
		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}
		this.blueprint = blueprint;
	}

	@Override
	public final JobMode getJobMode()
	{
		return JobMode.MANUAL;
	}
	
	public final Blueprint getBlueprint()
	{
		return blueprint;
	}
	
	protected abstract Type getType();
	
	public final boolean hasType(Type... types) {
		
		if ( ArrayUtils.isEmpty(types) ) {
			throw new IllegalArgumentException("types cannot be NULL/empty");
		}
		for ( Type expected : types ) {
			if ( getType() == expected ) {
				return true;
			}
		}
		return false;
	}
	
	public abstract long calcCosts(IFactorySlot slot, int runs);

	public abstract Duration calcDuration(IFactorySlot slot, int runs);
	
	public boolean isRunInParallel() {
		return supportsParallelExecution() ? this.runInParallel : false;
	}
	
	public void setRunInParallel(boolean yesNo) {
		this.runInParallel = yesNo;
	}
	
}
