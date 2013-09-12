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

import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.eve.skills.datamodel.SlotAttributes;
import de.codesourcery.eve.skills.production.entity.EveOnlineJobTemplate.Type;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.ISlotType;
import de.codesourcery.planning.impl.SimpleProductionSlot;

public class EveOnlineFactorySlot extends SimpleProductionSlot
{

	public ISlotType MANUFACTURING_SLOT = new ISlotType() {

		@Override
		public boolean accepts(IJobTemplate t)
		{
			return ((EveOnlineJobTemplate) t).hasType( Type.MANUFACTURING_JOB );
		}
	};
	
	public ISlotType COPYING_SLOT = new ISlotType() {

		@Override
		public boolean accepts(IJobTemplate t)
		{
			return ((EveOnlineJobTemplate) t).hasType( Type.COPY_JOB );
		}
	};
	
	public ISlotType INVENTION_SLOT = new ISlotType() {

		@Override
		public boolean accepts(IJobTemplate t)
		{
			return ((EveOnlineJobTemplate) t).hasType( Type.INVENTION_JOB );
		}
	};
	
	private ILocation location;
	private SlotAttributes attributes;
	
	public EveOnlineFactorySlot(String name, ISlotType type,IProductionLocation location) {
		super(name, type, location );
	}

	public void setAttributes(SlotAttributes location)
	{
		this.attributes = location;
	}

	public SlotAttributes setAttributes()
	{
		return attributes;
	}

	public void setLocation(ILocation location)
	{
		this.location = location;
	}

	public ILocation getLocation()
	{
		return location;
	}

}
