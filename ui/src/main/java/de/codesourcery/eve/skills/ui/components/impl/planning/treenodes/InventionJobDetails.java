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
package de.codesourcery.eve.skills.ui.components.impl.planning.treenodes;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.Decryptor;
import de.codesourcery.eve.skills.datamodel.SlotAttributes;

public class InventionJobDetails
{
	private final Blueprint tech1Blueprint;
	private final Blueprint tech2Blueprint;

	private SlotAttributes slotAttributes=SlotAttributes.HIGHSEC_NPC_STATION;
	private int metaLevel = 0;
	private Decryptor decryptor= Decryptor.NONE;
	private int numberOfRuns;

	public InventionJobDetails(Blueprint tech1Blueprint,
			Blueprint tech2Blueprint,
			int numberOfRuns) 
	{
		if ( tech1Blueprint == null ) {
			throw new IllegalArgumentException("tech1Blueprint cannot be NULL");
		}
		if ( tech2Blueprint  == null ) {
			throw new IllegalArgumentException("tech2Blueprint  cannot be NULL");
		}
		
		setNumberOfRuns( numberOfRuns );
		
		this.tech1Blueprint = tech1Blueprint;
		this.tech2Blueprint = tech2Blueprint;
	}

	public void setNumberOfRuns(int numberOfRuns)
	{
		if ( numberOfRuns < 1 ) {
			throw new IllegalArgumentException("runs must be >= 1 ");
		}
		this.numberOfRuns = numberOfRuns;
	}

	public int getNumberOfRuns()
	{
		return numberOfRuns;
	}

	public void setMetaLevel(int metaLevel)
	{
		if ( metaLevel < 0 || metaLevel > 4) {
			throw new IllegalArgumentException("meta-level must be 0 <= x <= 4 ");
		}
		this.metaLevel = metaLevel;
	}

	public int getMetaLevel()
	{
		return metaLevel;
	}

	public void setSlotAttributes(SlotAttributes slotAttributes)
	{
		if ( slotAttributes == null ) {
			throw new IllegalArgumentException("slotAttributes cannot be NULL");
		}
		this.slotAttributes = slotAttributes;
	}

	public SlotAttributes getSlotAttributes()
	{
		return slotAttributes;
	}

	public void setDecryptor(Decryptor decryptor)
	{
		if ( decryptor == null ) {
			throw new IllegalArgumentException("decryptor cannot be NULL");
		}
		this.decryptor = decryptor;
	}

	public Decryptor getDecryptor()
	{
		return decryptor;
	}

	public Blueprint getTech1Blueprint()
	{
		return tech1Blueprint;
	}

	public Blueprint getTech2Blueprint()
	{
		return tech2Blueprint;
	}

}
