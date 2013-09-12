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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.codesourcery.eve.skills.db.datamodel.Corporation;
import de.codesourcery.eve.skills.db.datamodel.Faction;


public class CorpStandings extends AbstractStandings {
	
	private Corporation toCorporation;
	
	private Map<Long,Standing<Faction> > factionStandings =
		new HashMap<Long,Standing<Faction> >();
	
	public CorpStandings(Corporation to) {
		if (to == null) {
			throw new IllegalArgumentException("to cannot be NULL");
		}
		this.toCorporation = to;
	}
	
	public Corporation getCorporation() {
		return toCorporation;
	}

	public void addFactionStanding(Standing<Faction> factionStanding) {
		if (factionStanding == null) {
			throw new IllegalArgumentException("factionStanding cannot be NULL");
		}
		
		factionStandings.put( factionStanding.getFrom().getID() , factionStanding );
	}
	
	public Collection<Standing<Faction>> getFactionStandings() {
		return Collections.unmodifiableCollection( factionStandings.values() );
	}
	
	public Standing<Faction> getFactionStanding(Faction faction) {
		if (faction == null) {
			throw new IllegalArgumentException("faction cannot be NULL");
		}
		
		return this.factionStandings.get( faction.getID() );
	}
	
	
}
