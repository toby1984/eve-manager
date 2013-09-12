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
package de.codesourcery.eve.skills.db.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.db.datamodel.Faction;

public class FactionDAO extends HibernateDAO<Faction,Long> implements IFactionDAO {

	public static final Logger log = Logger.getLogger(FactionDAO.class);
	
	// thread-safe , immutable map
	private Map<Long,Faction> byIdCache;
	
	// thread-safe , immutable map
	private Map<String,Faction> byNameCache;
	
	private final Object LOCK = new Object();
	private volatile boolean initialized = false;
	
	public FactionDAO() {
		super(Faction.class);
	}
	
	protected void load() {
		
		synchronized ( LOCK ) {
			
			if ( initialized ) {
				return;
			}
		
			final Map<String,Faction> tmpByNameMap =
				new HashMap<String, Faction>();
			
			final Map<Long,Faction> tmpByIDMap =
				new HashMap<Long, Faction>();
			
			for ( Faction f : fetchAll() ) {
				tmpByNameMap.put( f.getName(), f );
				tmpByIDMap.put( f.getID() , f );
			}
			
			log.debug("load(): Loaded "+byNameCache.size()+" factions from database.");
			
			this.byNameCache = tmpByNameMap;
			this.byIdCache = tmpByIDMap;
			
			initialized = true;
		}
	}

	@Override
	public Faction getFactionByName(String factionName) throws NoSuchElementException {

		if (StringUtils.isBlank(factionName)) {
			throw new IllegalArgumentException("factionName cannot be blank.");
		}
		
		if ( ! initialized ) { // thread-safe because of volatile semantics
			load();
		}
		
		final Faction result = 
			this.byNameCache.get( factionName );
		
		if ( result == null ) {
			throw new NoSuchElementException("Found no faction with name '"+factionName+"'");
		}
		return result;
	}

}