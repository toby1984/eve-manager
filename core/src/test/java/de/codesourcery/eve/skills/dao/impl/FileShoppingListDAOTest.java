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
package de.codesourcery.eve.skills.dao.impl;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ShoppingList;
import de.codesourcery.eve.skills.datamodel.ShoppingList.ShoppingListEntry;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;

public class FileShoppingListDAOTest extends TestCase 
{

	public static final InventoryType ITEM1;
	public static final InventoryType ITEM2;
	public static final InventoryType ITEM3;
	
	private static final IStaticDataModel DATAMODEL;
	
	static {
		ITEM1 = new InventoryType();
		ITEM1.setName("Item #1" );
		ITEM1.setTypeId( 1L );
		
		ITEM2 = new InventoryType();
		ITEM2.setName("Item #2" );
		ITEM2.setTypeId( 2L );
		
		ITEM3 = new InventoryType();
		ITEM3.setName("Item #3" );
		ITEM3.setTypeId( 3L );
		
		DATAMODEL = createMock( IStaticDataModel.class );
		
		expect( DATAMODEL.getInventoryType( 1L ) ).andReturn( ITEM1 ).anyTimes();
		expect( DATAMODEL.getInventoryType( 2L ) ).andReturn( ITEM2 ).anyTimes();
		expect( DATAMODEL.getInventoryType( 3L ) ).andReturn( ITEM3 ).anyTimes();
		
		replay( DATAMODEL );
	}
	
	private File file;
	
	private FileShoppingListDAO dao;
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		dao = createDAO();
	}
	
	private static FileShoppingListDAO createDAO() {
		final FileShoppingListDAO dao = new FileShoppingListDAO();
		dao.setDataModel( DATAMODEL );
		return dao;
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		
		if ( file != null ) {
			file.delete();
			file = null;
		}
	}
	
	public void testWriteLoad() throws IOException {

		file = File.createTempFile("blubb" , "blah" );
		dao.setDataFile( file );
		
		final ShoppingList list1 =
			new ShoppingList("title1");
		list1.setDescription( "description1" );
		
		list1.addEntry( ITEM1 , 10 );
		
		final ShoppingList list2 =
			new ShoppingList("title2");
		list2.setDescription( "description2" );
		
		list2.addEntry( ITEM1 , 15 ).setPurchasedQuantity( 5 );
		list2.addEntry( ITEM2 , 30 );
		
		dao.store( list1 );
		dao.store( list2 );
		
		assertTrue( file.exists() );
		
		dao = createDAO();
		dao.setDataFile( file );

		final List<ShoppingList> lists = dao.getAll();
		assertNotNull( lists );
		assertEquals( 2 , lists.size() );
		
		boolean foundList1 = false;
		boolean foundList2 = false;
		for ( ShoppingList list : lists ) 
		{
			if ( "title1".equals( list.getTitle() ) ) 
			{
				assertFalse("Found List #1 more than once?",foundList1);
				foundList1 = true;
				assertEquals( "description1" , list.getDescription() );
				assertEquals( 1 , list.size() );
				final ShoppingListEntry entry = list.getEntries().get(0);
				assertSame( ITEM1 , entry.getType() );
				assertEquals( 10 , entry.getQuantity() );
				assertEquals( 0 , entry.getPurchasedQuantity() );
			}
			else if ( "title2".equals( list.getTitle() ) ) 
			{
				assertFalse("Found List #2 more than once?",foundList2);
				foundList2 = true;
				assertEquals( "description2" , list.getDescription() );
				assertEquals( 2 , list.size() );
				final ShoppingListEntry entry = list.getEntries().get(0);
				
				boolean foundEntry1 = false;
				boolean foundEntry2 = false;
				for ( ShoppingListEntry e : list.getEntries() ) 
				{
					if ( e.getType() == ITEM1 ) {
						assertFalse( "Found entry #1 more than once",foundEntry1);
						foundEntry1 = true;
						assertEquals( 15 , e.getQuantity() );
						assertEquals( 5 , e.getPurchasedQuantity() );						
					} 
					else if ( e.getType() == ITEM2 ) 
					{
						assertFalse( "Found entry #2 more than once",foundEntry2);
						foundEntry2 = true;
						assertEquals( 30 , e.getQuantity() );
						assertEquals( 0 , e.getPurchasedQuantity() );	
					}
					else 
					{
						fail("Found unexpected entry "+e);
					}
				}
				assertTrue( "Entry #1 not found?" , foundEntry1 );
				assertTrue( "Entry #2 not found?" , foundEntry2 );
			}
			else 
			{
				fail("Unexpected element "+list);
			}
		}

		assertTrue( "List #1 not found ?" , foundList1 );
		assertTrue( "List #2 not found ?" , foundList2 );
	}
	
	public void testWriteDelete() throws IOException {

		file = File.createTempFile("blubb" , "blah" );
		dao.setDataFile( file );
		
		final ShoppingList list1 =
			new ShoppingList("title1");
		list1.setDescription( "description1" );
		
		list1.addEntry( ITEM1 , 10 );
		
		final ShoppingList list2 =
			new ShoppingList("title2");
		list2.setDescription( "description2" );
		
		list2.addEntry( ITEM1 , 15 ).setPurchasedQuantity( 5 );
		list2.addEntry( ITEM2 , 30 );
		
		dao.store( list1 );
		dao.store( list2 );

		assertTrue( dao.delete( list1 ) );
		
		dao = createDAO();
		dao.setDataFile( file );

		final List<ShoppingList> lists = dao.getAll();
		assertNotNull( lists );
		assertEquals( 1 , lists.size() );
		
		ShoppingList list = lists.get(0);
		assertEquals( "title2" , list.getTitle() );
		assertEquals( "description2" , list.getDescription() );
		assertEquals( 2 , list.size() );
				
		boolean foundEntry1 = false;
		boolean foundEntry2 = false;
		for ( ShoppingListEntry e : list.getEntries() ) 
		{
			if ( e.getType() == ITEM1 ) {
				assertFalse( "Found entry #1 more than once",foundEntry1);
				foundEntry1 = true;
				assertEquals( 15 , e.getQuantity() );
				assertEquals( 5 , e.getPurchasedQuantity() );						
			} 
			else if ( e.getType() == ITEM2 ) 
			{
				assertFalse( "Found entry #2 more than once",foundEntry2);
				foundEntry2 = true;
				assertEquals( 30 , e.getQuantity() );
				assertEquals( 0 , e.getPurchasedQuantity() );	
			}
			else 
			{
				fail("Found unexpected entry "+e);
			}
		}
		assertTrue( "Entry #1 not found?" , foundEntry1 );
		assertTrue( "Entry #2 not found?" , foundEntry2 );
	}
}
