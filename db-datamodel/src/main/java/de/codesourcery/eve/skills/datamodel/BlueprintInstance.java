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
package de.codesourcery.eve.skills.datamodel;

public class BlueprintInstance
{

	private final Blueprint blueprint;
	private final boolean isOriginal;
	
	private int me;
	private int pe;
	
	private int runsLeft;
	
	public BlueprintInstance(Blueprint blueprint,boolean isOriginal) {
		this.isOriginal = isOriginal;
		this.blueprint = blueprint;
	}

	public boolean isOriginal()
	{
		return isOriginal;
	}

	public boolean isCopy() {
		return ! isOriginal;
	}

	public Blueprint getBlueprint()
	{
		return blueprint;
	}
	
	public int getRunsLeft()
	{
		return runsLeft;
	}
	
	public void setMe(int me)
	{
		this.me = me;
	}
	
	public void setPe(int pe)
	{
		this.pe = pe;
	}
	
	public void setRunsLeft(int runsLeft)
	{
		if ( runsLeft < 0 ) {
			throw new IllegalArgumentException("runs-left value cannot be < 0");
		}
		this.runsLeft = runsLeft;
	}
	
	public int getME()
	{
		return me;
	}
	
	public int getPE()
	{
		return pe;
	}
}
