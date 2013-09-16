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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.LongType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;

public class InventoryTypeDAO extends HibernateDAO<InventoryType, Long> implements IInventoryTypeDAO 
{
	private final ConcurrentHashMap<Long,InventoryType> typeByID = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long,List<InventoryType>> typeByMarketGroupID = new ConcurrentHashMap<>();	
	private final ConcurrentHashMap<String, InventoryType> typeByName = new ConcurrentHashMap<String, InventoryType>();
	
	public InventoryTypeDAO() {
		super(InventoryType.class);
	}

	@Override
	public InventoryType getTypeByName(final String name) throws EmptyResultDataAccessException 
	{
		InventoryType result = typeByName.get( name );
		if ( result != null ) {
			return result;
		}		
		return execute(new HibernateCallback<InventoryType>() {

			@Override
			public InventoryType doInSession(Session session) 
			{
				final Query query = session.createQuery( "from InventoryType where name = :itemName");
				query.setParameter("itemName", name);
				InventoryType result = getExactlyOneResult((List<InventoryType>) query.list());
				if ( result != null ) {
					typeByID.putIfAbsent( result.getId() , result );
					typeByName.putIfAbsent( result.getName() , result );
				}
				return result;
			}
		});
	}

	@Override
	public List<InventoryType> searchTypesByName(final String name, final boolean marketOnly) 
	{
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
	public List<InventoryType> getInventoryTypes(final InventoryGroup group) 
	{
		return execute(new HibernateCallback<List<InventoryType>>() {

			@Override
			public List<InventoryType> doInSession(Session session) 
			{
				final Query query = session.createQuery( "from InventoryType where groupId = :group");
				query.setParameter("group", group);
				return query.list();
			}
		});
	}

	@Override
	public InventoryType getInventoryTypeByName(final String name) {

		InventoryType result = typeByName.get(name);

		if (result != null) {
			return result;
		}

		result = execute(new HibernateCallback<InventoryType>() {

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
		typeByName.putIfAbsent(name, result);
		return result;
	}

	@Override
	public List<InventoryType> getInventoryTypes(final MarketGroup group) 
	{
		if (group == null) {
			throw new IllegalArgumentException("group must not be NULL");
		}
		
		final List<InventoryType> cached = typeByMarketGroupID.get( group.getId() );
		if ( cached != null ) {
			return new ArrayList<>(cached);
		}
		return execute(new HibernateCallback<List<InventoryType>>() {

			@Override
			public List<InventoryType> doInSession(Session session) 
			{
				final SQLQuery query = session.createSQLQuery( "SELECT typeID FROM invTypes WHERE marketGroupID = :marketGroupId");
				query.setParameter("marketGroupId", group.getId() );
				query.addScalar("typeID" , new LongType() );
				
				final List<InventoryType> result = getInventoryTypesByIDs( query.list() );
				typeByMarketGroupID.putIfAbsent( group.getId() , new ArrayList<>( result ) );
				return result;
			}
		});
	}
	
	private List<InventoryType> getInventoryTypesByIDs(final Collection<Long> invTypeIds) 
	{
		final Set<Long> ids= new HashSet<>( invTypeIds );

		final List<InventoryType> result = new ArrayList<>();
		for (Iterator<Long> it = ids.iterator(); it.hasNext();) 
		{
			final Long id = it.next();
			InventoryType type = typeByID.get( id );
			if ( type != null ) {
				result.add( type );
				it.remove();
			}
		}
		if ( ids.isEmpty() ) {
			return result;
		}
		return execute(new HibernateCallback<List<InventoryType>>() {

			@Override
			public List<InventoryType> doInSession(Session session) 
			{
				final String idsString = StringUtils.join( ids , "," );
				final SQLQuery query = session.createSQLQuery( "SELECT * FROM invTypes WHERE typeID IN ( "+idsString+")");
				query.addEntity( InventoryType.class );
				
				final List<InventoryType> notCached = query.list();
				result.addAll( notCached );
				for ( InventoryType type : notCached ) 
				{
					typeByName.putIfAbsent( type.getName() , type );
					typeByID.putIfAbsent( type.getId() , type );
				}
				return result;
			}
		});		
	}
	
	@Override
	public List<InventoryType> getInventoryTypes(final MarketGroup group,final String itemNamePattern) 
	{
		if ( group == null ) {
			throw new IllegalArgumentException("group must not be NULL");
		}
		if (StringUtils.isBlank(itemNamePattern)) 
		{
			throw new IllegalArgumentException("itemNamePattern must not be NULL or blank");
		}
		return filterByNameSubstring( getInventoryTypes( group )  , itemNamePattern ); 
	}

	@Override
	public List<InventoryType> getInventoryTypesWithBlueprints( MarketGroup group, String itemNamePattern) 
	{
		return filterByNameSubstring( getInventoryTypesWithBlueprints(group) , itemNamePattern );
	}	

	@Override
	public List<InventoryType> getInventoryTypesWithBlueprints(final MarketGroup group) 
	{
		return execute(new HibernateCallback<List<InventoryType>>() {

			@Override
			public List<InventoryType> doInSession(Session session) 
			{
				final SQLQuery query = session.createSQLQuery( "SELECT i.typeID FROM invTypes i , invBlueprintTypes bp WHERE "
						+ "i.marketGroupID = :marketGroupId AND bp.productTypeID = i.typeID");
				query.setParameter("marketGroupId", group.getId() );
				query.addScalar("typeID" , new LongType() );
				
				return getInventoryTypesByIDs( query.list() );
			}
		});
	}
	
	private List<InventoryType> filterByNameSubstring(List<InventoryType> toFilter , String itemNamePattern ) 
	{
		final String lowerPattern = itemNamePattern.toLowerCase();
		final List<InventoryType> result = new ArrayList<>(); 
		for ( InventoryType inventoryType : toFilter ) 
		{
			if ( inventoryType.getName().toLowerCase().contains( lowerPattern ) ) {
				result.add( inventoryType );
			}
		}
		return result;
	}
}