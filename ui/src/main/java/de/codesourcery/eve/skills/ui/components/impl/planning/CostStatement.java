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
package de.codesourcery.eve.skills.ui.components.impl.planning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind;
import de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Type;

public class CostStatement implements Iterable<CostPosition>
{
	
	private final List<CostPosition> positions =
		new ArrayList<CostPosition>();
	
	public CostStatement() {
	}
	
	public void add(CostPosition pos)
	{
		if ( pos == null ) {
			throw new IllegalArgumentException("pos cannot be NULL");
		}
		
		positions.add( pos );
	}
	
	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for ( int i = 0 ; i <positions.size() ; i++) {
			result.append( positions.get(i).toString() );
			if ( (i+1) < positions.size() ) {
				result.append("\n");
			}
		}
		return result.toString();
	}
	
	public Collection<CostPosition> getCostPositions() {
		return Collections.unmodifiableList( positions );
	}
	
	public interface IFilter {
		public boolean accepts(CostPosition p);
	}

	public void remove(CostPosition pos) {
		
		final Iterator<CostPosition> it = positions.iterator();
		while ( it.hasNext() ) {
			if ( it.next() == pos ) {
				it.remove();
				return;
			}
		}
		throw new RuntimeException("Internal error, failed to remove "+pos);
	}
	
	public static final class KindTypeFilter implements IFilter {

		private final CostPosition.Kind kind;
		private final CostPosition.Type type;
		
		private KindTypeFilter(Type type) {
			this( null , type );
		}
		
		private KindTypeFilter(Kind kind) {
			this( kind , null );
		}
		
		private KindTypeFilter(Kind kind, Type type) {
			super();
			this.kind = kind;
			this.type = type;
		}

		@Override
		public boolean accepts(CostPosition p)
		{
			if ( kind != null && p.getKind() != kind ) {
				return false;
			}
			if ( type != null && p.getType() != type ) {
				return false;
			}
			return true;
		}
		
	}
	
	public List<CostPosition> filter(IFilter f) 
	{
		final List<CostPosition> result = new ArrayList<CostPosition>();
		for ( CostPosition p : positions ) {
			if ( f.accepts( p ) ) {
				result.add( p );
			}
		}
		return Collections.unmodifiableList( result );
	}
	
	public Iterator<CostPosition> iterator( IFilter filter)
	{
		return filter( filter ).iterator();
	}
	
	@Override
	public Iterator<CostPosition> iterator()
	{
		return Collections.unmodifiableList( this.positions ).iterator();
	}
	
}
