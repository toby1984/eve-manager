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
package de.codesourcery.eve.skills.util;

import org.hibernate.Criteria;
import org.hibernate.ReplicationMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import de.codesourcery.eve.skills.db.datamodel.AssemblyLine;
import de.codesourcery.eve.skills.db.datamodel.AssemblyLineType;
import de.codesourcery.eve.skills.db.datamodel.BlueprintType;
import de.codesourcery.eve.skills.db.datamodel.Constellation;
import de.codesourcery.eve.skills.db.datamodel.EveName;
import de.codesourcery.eve.skills.db.datamodel.Faction;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryMetaType;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.ItemAttributeType;
import de.codesourcery.eve.skills.db.datamodel.ItemAttributeTypeMapping;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.db.datamodel.Race;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.db.datamodel.TypeActivityMaterials;
import de.codesourcery.eve.skills.db.datamodel.TypeMaterial;

/**
 * Helper class that exports
 * imports data from a MySQL database
 * into HSQL. 
 * @author tobias.gierke@code-sourcery.de
 */
public class DBConverter {

	private final ApplicationContext context;
	private SessionFactory mysql;
	private SessionFactory hsql;

	public DBConverter() {

		System.out.println("Creating context...");
		context = new ClassPathXmlApplicationContext("/db-conversion-spring.xml");

		System.out.println("Creating MySQL session factory ...");
		mysql = (SessionFactory) context.getBean("mysql-sessionFactory");

		System.out.println("Creating HSQL session factory ...");
		hsql =(SessionFactory) context.getBean("hsql-sessionFactory");
	}

	public static void main(String[] args) {

		new DBConverter().run();
	}

	public void run() {

		export( MarketGroup.class );
		
		// export races
		export( Race.class );
		
		export( ItemAttributeType.class );
		
		export( ItemAttributeTypeMapping.class );

		export( Faction.class );

		export( Region.class ); // -> Faction 

		export( Constellation.class ); // -> Faction

		export( SolarSystem.class ); // -> Constellation , Faction , Region
		
		export( EveName.class );
		
		export( NPCCorporation.class );

		export( Station.class ); 

		export( InventoryGroup.class ); // -> InvetoryCategory

		export( InventoryType.class ); // -> InventoryGroup
		
		export( TypeMaterial.class ); // Link table ( product inventory type , production/reprocessing result)

		export ( BlueprintType.class ); // Blueprint types
		
		export ( TypeActivityMaterials.class ); // Link table ( type , activity , materials )
		
		export( InventoryMetaType.class ); // Link type ( inventory type , parent inventory type , meta group )

		export( AssemblyLineType.class );
		
		export( AssemblyLine.class );
		
		/*
		 * Close session factories,
		 * issue a SHUTDOWN command
		 * to HSQL so data
		 * gets persisted.
		 */
		mysql.close();

//		Session hsqlSession =
//			hsql.openSession();
//
//		Query query = hsqlSession.createSQLQuery("SHUTDOWN");
//		query.executeUpdate();
//
//		hsqlSession.close();

		hsql.close();
		
		System.out.println("\n=== Export finished. ===");
	}
	
	protected void export(Class<?> entity) {

		System.out.println("\n============\nExporting "+entity.getName()+"\n============");

		// load data
		System.out.print("Opening MySQL session ...");
		final Session mysqlSession = mysql.openSession();
		System.out.print("created.");
		
//		mysqlSession.setFlushMode( FlushMode.MANUAL );
		
		Transaction mysqlTransaction = mysqlSession.beginTransaction();

		final Criteria criteria = mysqlSession.createCriteria( entity );

		// replicate data
		System.out.print("Opening HSQL session ...");
		final Session hsqlSession = hsql.openSession();
		System.out.println("created.");
//		mysqlSession.setFlushMode( FlushMode.MANUAL );

		final Transaction hsqlTransaction = 
			hsqlSession.beginTransaction();
		
		final ScrollableResults data = criteria.scroll();
		int count = 0;
		int dotCount = 0;
		try {
			while ( data.next() ) {
				Object loaded = data.get(0);
//				if ( entity == MarketGroup.class ) {
//					MarketGroup group = (MarketGroup) loaded;
//					System.out.println( group.getId() +" -> "+group.getParent() );
//				}
				hsqlSession.replicate( loaded , ReplicationMode.IGNORE );
				if ( ( ++count % 1000 ) == 0 ) { // make sure to adjust <prop key="hibernate.jdbc.batch_size">1000</prop> in config !!
					hsqlSession.flush();
					hsqlSession.clear();
					mysqlSession.flush();
					mysqlSession.clear();	
					System.out.print(".");
					dotCount++;
					if ( dotCount == 60 ) {
						System.out.println();
						dotCount = 0;
					}
				}
			}
		}
		finally { 
			data.close();
			System.out.println("\nExported "+count+" entries");
		}

		if ( mysqlTransaction.isActive() ) {
			mysqlTransaction.commit();
		}
		
		if ( hsqlTransaction.isActive() ) {
			hsqlTransaction.commit();
		}
		
		hsqlSession.flush();
		mysqlSession.flush();
		
		mysqlSession.close();
		hsqlSession.close();
	}
}
