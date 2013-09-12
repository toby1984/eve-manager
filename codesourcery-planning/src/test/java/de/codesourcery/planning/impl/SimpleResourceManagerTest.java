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
package de.codesourcery.planning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.IResource;
import de.codesourcery.planning.IResourceFactory;
import de.codesourcery.planning.IResourceManager;
import de.codesourcery.planning.IResourceQuantity;
import de.codesourcery.planning.IResourceType;

public class SimpleResourceManagerTest extends TestCase
{

	private static final IResourceType RES1 = new MyResourceType("Tritanium");
	private static final IResourceType RES2 = new MyResourceType("Pyerite");
	private static final IResourceType RES3 = new MyResourceType("Isogen");

	private static final IProductionLocation LOC1 = new MyLocation("Station #1");
	private static final IProductionLocation LOC2 = new MyLocation("Station #2");
	private static final IProductionLocation LOC3 = new MyLocation("Station #3");
	
	private SimpleResourceManager manager;
	
	private static final class ResourceWithQuantity implements IResourceQuantity {

		private final IResourceType type;
		private final double quantity;
		
		public ResourceWithQuantity(IResourceType type,double quantity) {
			if ( type == null ) {
				throw new IllegalArgumentException("type cannot be NULL");
			}
			this.type = type;
			this.quantity = quantity;
		}
		
		@Override
		public double getResourceQuantity() { return quantity;}

		@Override
		public IResourceType getResourceType() { return type; }
		
		@Override
		public String toString()
		{
			return "ResourceWithQuantity[ "+type+" , quantity="+quantity+" ]";
		}
		
	}
	
	private static final class MyResourceManager extends SimpleResourceManager  {

		private Date timestamp = new Date();
		
		private final IResourceFactory resourceFactory =
			new IResourceFactory() {

			@Override
			public IResource createResource(IResourceType type,
					IProductionLocation location)
			{
				return new SimpleResource(type,location,0);
			}

			@Override
			public Date getTimestamp()
			{
				return timestamp;
			}

			@Override
			public IResource cloneResource(IResource resource)
			{
				return new SimpleResource((SimpleResource) resource);
			}
		};
		
		@Override
		public IResourceManager snapshot()
		{
			final MyResourceManager result = new MyResourceManager();
			cloneInstance( result );
			return result;
		}
		
		@Override
		protected void initHook()
		{
			final List<IResource> data =
				new ArrayList<IResource>();
			
			data.add( new SimpleResource(RES1,LOC1, 5000) );
			data.add( new SimpleResource(RES2,LOC1, 3000) );
			
			data.add( new SimpleResource(RES2,LOC2, 2000) );
			
			internalSetResources( data );
		}
		
		@Override
		protected IResourceFactory getResourceFactory()
		{
			return resourceFactory;
		}

		@Override
		public Date getTimestamp()
		{
			return timestamp;
		}
	};
	
	private static final class MyResourceType implements IResourceType 
	{
		private final String name;

		public MyResourceType(String name) 
		{
			if ( name == null ) {
				throw new IllegalArgumentException("name cannot be NULL");
			}
			this.name = name;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if ( obj instanceof MyResourceType) {
				return ((MyResourceType) obj).name.equals( this.name );
			}
			return false; 
		}
		
		@Override
		public int hashCode()
		{
			return name.hashCode();
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private static final class MyLocation implements IProductionLocation {
		private final String name;

		public MyLocation(String name) 
		{
			if ( name == null ) {
				throw new IllegalArgumentException("name cannot be NULL");
			}
			this.name = name;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if ( obj instanceof MyLocation) {
				return ((MyLocation) obj).name.equals( this.name );
			}
			return false; 
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		@Override
		public int hashCode()
		{
			return name.hashCode();
		}
	}

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		this.manager = new MyResourceManager();
	}
	
	private static final class ResourceMatcher {
		
		private final IResource actual;
		
		public ResourceMatcher(IResource actual) {
			assertNotNull( actual );
			this.actual = actual;
		}
		
		public ResourceMatcher withLocation(IProductionLocation loc) {
			assertSame( loc , actual.getLocation() );
			return this;
		}
		
		public ResourceMatcher withAmount(int amount) 
		{
			assertEquals( amount, (int) actual.getAmount() );
			return this;
		}
		
		public ResourceMatcher withAmount(double amount) 
		{
			assertEquals( amount, actual.getAmount() );
			return this;
		}
		
		public ResourceMatcher withType(IResourceType type) {
			assertSame( type , actual.getType() );
			return this;
		}
		
	}
	
	// =================================================================
	
	/*
			data.add( new SimpleResource(RES1,LOC1, 5000) );
			data.add( new SimpleResource(RES2,LOC1, 3000) );
			
			data.add( new SimpleResource(RES2,LOC2, 2000) );	 
	 */
	
	public void testCalculateProjectedResourceStatusWithSpecificLocation() {
		
		final List<IResourceQuantity> items =
			new ArrayList<IResourceQuantity>();

		items.add( new ResourceWithQuantity( RES1 , 6000 ) );
		items.add( new ResourceWithQuantity( RES2 , 5000 ) );
		
		final Collection<IResource> result = 
			manager.calculateProjectedResourceStatus( items , LOC2 ).values();
		
		assertNotNull( result );
		assertEquals( 2 , result.size() );
		
		boolean found1 = false;
		boolean found2 = false;
		for ( IResource r : result ) 
		{
			if ( r.getType().equals( RES1 ) ) 
			{
				assertFalse( found1 );
				found1=true;
				assertEquals( -6000.0d , r.getAmount() );
			} 
			else if ( r.getType().equals( RES2 ) ) {
				assertFalse( found2 );
				found2=true;
				assertEquals( -3000.0d , r.getAmount() );
			} else {
				fail("Unexpected resource "+r);
			}
		}
		
		assertTrue( found1 & found2 );
	}
	
	public void testCalculateProjectedResourceStatusWithAnyLocation() {
		
		final List<IResourceQuantity> items =
			new ArrayList<IResourceQuantity>();

		items.add( new ResourceWithQuantity( RES1 , 6000 ) );
		items.add( new ResourceWithQuantity( RES2 , 5000 ) );
		
		final Collection<IResource> result = 
			manager.calculateProjectedResourceStatus( items , IProductionLocation.ANY_LOCATION ).values();
		
		assertNotNull( result );
		assertEquals( 2 , result.size() );
		
		boolean found1 = false;
		boolean found2 = false;
		for ( IResource r : result ) 
		{
			if ( r.getType().equals( RES1 ) ) 
			{
				assertFalse( found1 );
				found1=true;
				assertEquals( -1000.0d , r.getAmount() );
			} 
			else if ( r.getType().equals( RES2 ) ) {
				assertFalse( found2 );
				found2=true;
				assertEquals( 0.0d , r.getAmount() );
			} else {
				fail("Unexpected resource "+r);
			}
		}
		
		assertTrue( found1 & found2 );
	}
	
	public void testgetAllResources() {
		
		List<IResource> resources = manager.getResourcesAt( IProductionLocation.ANY_LOCATION );
		assertNotNull(resources);
		assertEquals( 3 , resources.size() );
		
		boolean matched1 = false;
		boolean matched2 = false;
		boolean matched3 = false;
		for ( IResource r : resources ) {
			if ( r.getType() == RES1 && r.getLocation() == LOC1) {
				assertFalse( matched1 );
				matched1=true;
			} else if ( r.getType() == RES2 && r.getLocation() == LOC1) {
				assertFalse( matched2 );
				matched2=true;
			} else if ( r.getType() == RES2 && r.getLocation() == LOC2) {
				assertFalse( matched3 );
				matched3=true;
			} else {
				fail("Unmatched resource : "+r);
			}
		}

		assertTrue( matched1 & matched2 & matched3 );
	}
	
	
	public void testGetResource() {
	
		IResource res = manager.getResource( RES1 , LOC1 );
		new ResourceMatcher( res ).withType( RES1 ).withAmount(5000).withLocation(LOC1);
		
		res = manager.getResource( RES2 , LOC2 );
		new ResourceMatcher( res ).withType( RES2 ).withAmount(2000).withLocation(LOC2);
		
		res = manager.getResource( RES3 , LOC2 );
		new ResourceMatcher( res ).withType( RES3 ).withAmount( 0).withLocation(LOC2);
	}
	
	public void testGetResourcesAt() {
		
		List<IResource> resources = manager.getResourcesAt( LOC1 );

		assertNotNull( resources );
		assertEquals( 2 , resources.size() );

		if ( resources.get(0).getType().equals( RES1 ) ) 
		{
			new ResourceMatcher( resources.get(0) ).withType( RES1 ).withAmount(5000).withLocation(LOC1);
			new ResourceMatcher( resources.get(1) ).withType( RES2 ).withAmount(3000).withLocation(LOC1);
		} 
		else if ( resources.get(0).getType().equals( RES2 ) ) 
		{
			new ResourceMatcher( resources.get(0) ).withType( RES2 ).withAmount(3000).withLocation(LOC1);
			new ResourceMatcher( resources.get(1) ).withType( RES1 ).withAmount(5000).withLocation(LOC1);
		} 
		else {
			fail("Unexpected resource in collection");
		}
		
		resources = manager.getResourcesAt( LOC3 );
		assertNotNull( resources );
		assertTrue( resources.isEmpty() );
	}
	
	public void testConsume() {
		
		manager.consume( RES1 , LOC1 , 1000 );
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount(4000).withLocation(LOC1);
		
		manager.consume( RES1 , LOC1 , 5000 );
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( -1000).withLocation(LOC1);
	}
	
	public void testProduce() {
		
		manager.consume( RES1 , LOC1 , 6000 );
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( -1000).withLocation(LOC1);
		
		manager.produce( RES1, LOC1 , 1000 );
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( 0).withLocation(LOC1);
		
		manager.produce( RES1, LOC1 , 1000 );
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( 1000 ).withLocation(LOC1);
	}
	
	public void testSnapshot() {
		
		manager.consume( RES1 , LOC1 , 1000 );
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( 4000).withLocation(LOC1);
		
		IResourceManager snapshot =
			manager.snapshot();
		
		snapshot.produce( RES1, LOC1 , 500 );
		
		new ResourceMatcher( snapshot.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( 4500).withLocation(LOC1);
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
			.withType( RES1 ).withAmount( 4000).withLocation(LOC1);
		
		manager.consume(RES1,LOC1 , 1000 );
		
		new ResourceMatcher( manager.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( 3000).withLocation(LOC1);		
		
		new ResourceMatcher( snapshot.getResource( RES1, LOC1 ) )
		.withType( RES1 ).withAmount( 4500).withLocation(LOC1);
	}
}
