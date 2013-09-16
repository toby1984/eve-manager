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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import de.codesourcery.eve.skills.db.datamodel.MarketGroup;

public class MarketGroupDAO extends HibernateDAO<MarketGroup,Long> implements IMarketGroupDAO {

	private final AtomicReference<List<MarketGroup>> allGroups = new AtomicReference<>();
	
	public MarketGroupDAO() {
		super( MarketGroup.class );
	}
	
	@Override
	public List<MarketGroup> fetchAll() 
	{
		final List<MarketGroup> result;
		if ( allGroups.get() == null ) {
			result = super.fetchAll();
			allGroups.compareAndSet(null , new ArrayList<>(result) );
		} else {
			result = new ArrayList<>( allGroups.get() );
		}
		return result;
	}

	@Override
	public List<MarketGroup> getLeafMarketGroups() 
	{
		// market groups that have no other market group
		// where MarketGroup#getParent() points to them are considered
		// leaf groups
		List<MarketGroup> all = fetchAll();
		List<MarketGroup> leafs = new ArrayList<>( all );
		for ( MarketGroup group : all ) 
		{
			if ( group.getParent() != null ) 
			{
				leafs.remove( group.getParent() );
			}
		}
		return leafs;
	}
}