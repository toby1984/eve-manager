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

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import de.codesourcery.eve.skills.db.datamodel.InventoryCategory;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;

public class InventoryGroupDAO extends HibernateDAO<InventoryGroup, Long> implements IInventoryGroupDAO {

	public InventoryGroupDAO() {
		super(InventoryGroup.class);
	}

	@Override
	public List<InventoryGroup> getInventoryGroups(final InventoryCategory cat) {
		
		return (List<InventoryGroup>) execute( new HibernateCallback<List<InventoryGroup>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<InventoryGroup> doInSession(Session session) {
				
				Query query = session.createQuery("from InventoryGroup where categoryID = :cat");
				query.setParameter( "cat" , cat ,
						Hibernate.custom( CategoryUserType.class ) );
				return query.list();
			}} );
	}
	
	@Override
	public List<InventoryGroup> getBlueprintProductGroups() {
		
		/*
		 select distinct g.*from InventoryGroup g
		 inner join InventoryType types on g.groupID=types.groupID
		 inner join BlueprintType bpos on types.typeID=bpos.productTypeID  
		 where 
		   g.allowManufacture<>0;  
		 */
		
		return (List<InventoryGroup>) execute( new HibernateCallback<List<InventoryGroup>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<InventoryGroup> doInSession(Session session) {
				
				final SQLQuery query = session.createSQLQuery("select distinct g.* from invGroups g, " + 
						" invTypes types , invBlueprintTypes  bpos WHERE " +
						" g.groupID = types.groupID and types.typeID=bpos.productTypeID " + 
						" and g.allowManufacture<>0 and exists( select * from typeActivityMaterials mats where " +
						" mats.typeID=bpos.blueprintTypeID and mats.activityID=1 and mats.quantity >0 )");
				query.addEntity( InventoryGroup.class );
				return query.list();
			}} );		
	}
	
}
