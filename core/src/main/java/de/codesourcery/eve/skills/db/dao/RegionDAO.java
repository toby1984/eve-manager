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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import de.codesourcery.eve.skills.db.datamodel.Region;

public class RegionDAO extends HibernateDAO<Region,Long> implements IRegionDAO {

	public RegionDAO() {
		super(Region.class);
	}
	
	@SuppressWarnings("unchecked")
	public List<Region> fetchAll() {
		
		return (List<Region>) execute( new HibernateCallback<List<Region>>() {

			@Override
			public List<Region> doInSession(Session session) {
				final Criteria c = session.createCriteria( Region.class )
					.add( Restrictions.ne( "name", "Unknown" ) );
				return c.list();
			}} );
		
	}

}
