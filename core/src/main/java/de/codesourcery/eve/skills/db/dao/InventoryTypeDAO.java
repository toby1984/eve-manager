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
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;

public class InventoryTypeDAO extends HibernateDAO<InventoryType, Long>
		implements IInventoryTypeDAO {

	public InventoryTypeDAO() {
		super(InventoryType.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public InventoryType getTypeByName(final String name)
			throws EmptyResultDataAccessException {

		return execute(new HibernateCallback<InventoryType>() {

			@Override
			public InventoryType doInSession(Session session) {
				final Query query = session.createQuery(
						"from InventoryType where name = :itemName");
				query.setParameter("itemName", name);

				return getExactlyOneResult((List<InventoryType>) query.list());
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<InventoryType> searchTypesByName(final String name,
			final boolean marketOnly) {

		final String tmpName = "%" + name + "%";

		return execute(new HibernateCallback<List<InventoryType>>() {

			@Override
			public List<InventoryType> doInSession(Session session) {
				final Query query;

				if (!marketOnly) {
					query = session.createQuery(
							"from InventoryType where name like :itemName");
				} else {
					query = session.createQuery(
									"from InventoryType "
											+ "where name like :itemName and marketGroup is not null");
				}
				query.setParameter("itemName", tmpName);
				return query.list();
			}
		});
	}

	@Override
	public List<InventoryType> getInventoryTypes(final InventoryGroup group) {
		return execute(new HibernateCallback<List<InventoryType>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<InventoryType> doInSession(Session session) {
				final Query query = session.createQuery(
						"from InventoryType where groupId = :group");
				query.setParameter("group", group);
				return query.list();
			}
		});
	}

	private final Map<String, InventoryType> typeByName =
		new HashMap<String, InventoryType>();

	@Override
	public InventoryType getInventoryTypeByName(final String name) {

		synchronized (typeByName) {

			InventoryType result = typeByName.get(name);

			if (result != null) {
				return result;
			}

			result = execute(new HibernateCallback<InventoryType>() {

				@SuppressWarnings("unchecked")
				@Override
				public InventoryType doInSession(Session session) {
					final Query query = session.createQuery(
							"from InventoryType where name  = :itemName");
					query.setParameter("itemName", name);
					final List<InventoryType> result = query.list();

					if (result.isEmpty() || result.size() > 1) {
						throw new IncorrectResultSizeDataAccessException(""
								+ "Unexpected results for invType '" + name
								+ "', found " + result.size() + " ?", 1, result
								.size());
					}
					return result.get(0);
				}
			});
			typeByName.put(name, result);
			return result;
		}
	}

	@Override
	public List<InventoryType> getInventoryTypes(final MarketGroup group) {
		
		return execute(new HibernateCallback<List<InventoryType>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<InventoryType> doInSession(Session session) {
				final Query query = session.createQuery( "from InventoryType where marketGroup  = :marketGroup");
				query.setParameter("marketGroup", group);
				return query.list();
			}
		});
		
	}
}
