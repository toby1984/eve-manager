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

import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;

public class NPCCorpStandings extends AbstractStandings
{
	private final ICharacter toCorporation;
	
	private Map<Long,Standing<NPCCorporation> > npcCorpStandings =
		new HashMap<Long,Standing<NPCCorporation> >();
	
	public NPCCorpStandings(ICharacter to) {
		if (to == null) {
			throw new IllegalArgumentException("to cannot be NULL");
		}
		this.toCorporation = to;
	}
	
	public ICharacter getCharacter() {
		return toCorporation;
	}

	public void addNPCCorporationStanding(Standing<NPCCorporation> corpStanding) {
		if (corpStanding == null) {
			throw new IllegalArgumentException("corpStandings cannot be NULL");
		}
		
		npcCorpStandings.put( corpStanding.getFrom().getID() , corpStanding );
	}
	
	public Collection<Standing<NPCCorporation>> getNPCCorpStandings() {
		return Collections.unmodifiableCollection( npcCorpStandings.values() );
	}
	
	/**
	 * Returns the standing of a NPC corporation towards
	 * this character.
	 *  
	 * @param corp
	 * @return standing or <code>null</code> if the character has no
	 * standing at all with this corporation.
	 */
	public Standing<NPCCorporation> getNPCCorpStanding(NPCCorporation corp) {
		if ( corp == null) {
			throw new IllegalArgumentException("NPC corp cannot be NULL");
		}
		
		return this.npcCorpStandings.get( corp.getID() );
	}
}
