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

import java.util.Date;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.DurationTestHelper;
import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.ISlotType;

public class SimpleProductionSlotTest extends DurationTestHelper
{
	
	private static final IProductionLocation LOCATION1 = new IProductionLocation() {
		@Override
		public String toString()
		{
			return "Location #1";
		}
	};
	
	private static final ISlotType SLOT_TYPE = new ISlotType() {

		@Override
		public boolean accepts(IJobTemplate t)
		{
			return true;
		}
	};

	private SimpleProductionSlot slot;
	private IFactory factory;
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		slot = new SimpleProductionSlot( "Some slot" , SLOT_TYPE  , LOCATION1);
		factory = new SimpleFactory("dummy factory");
		slot.setFactory( factory );
	}
	
	public void testAddJob() {
		
		final SimpleJob job1 = createSimpleJob("some job" , Duration.oneDay() , 1 );
		final Date startDate = toDate("2009-01-01" );
		job1.setStartDate( startDate );
		slot.add( job1 );
		
		assertEquals( startDate , job1.getStartDate() );
		
		assertFalse( slot.getJobs().isEmpty() );
		assertSame( job1 , slot.getJobs().get(0) );
		
		final SimpleJob job2 = createSimpleJob("some job" , Duration.oneDay() , 1 );
		job2.setStartDate( startDate );
		try {
			slot.add( job2 );
			fail("Should have failed because of same start date");
		} catch(IllegalArgumentException e) {
			// ok
		}
	}
	
	public void testGetJobOnDate() {
	
		final SimpleJob job1 = createSimpleJob("some job" , Duration.oneDay() , 1 );
		final Date startDate = toDate("2009-01-02 11:12:13" );
		job1.setStartDate( startDate );
		slot.add( job1 );

		assertNull( slot.getJobOn( toDate("2009-01-01" ) ) );
		assertFalse( slot.hasJobsOnDay( toDate("2009-01-01" ) ) );
		
		assertNull( slot.getJobOn( toDate("2009-01-02" ) ) );
		assertTrue( slot.hasJobsOnDay( toDate("2009-01-02" ) ) );
		
		assertNull( slot.getJobOn( toDate("2009-01-02 11:12:12" ) ) );
		assertTrue( slot.hasJobsOnDay( toDate("2009-01-02 11:12:12" ) ) );
		
		assertSame( job1 , slot.getJobOn( toDate("2009-01-02 11:12:13" ) ) );
		assertSame( job1 , slot.getJobOn( toDate("2009-01-03 01:12:13" ) ) );
		assertTrue( slot.hasJobsOnDay( toDate("2009-01-03" ) ) );
		
		assertSame( job1 , slot.getJobOn( toDate("2009-01-03 11:12:13" ) ) );
		assertNull( slot.getJobOn( toDate("2009-01-03 11:12:14" ) ) );
		
		assertFalse( slot.hasJobsOnDay( toDate("2009-01-04" ) ) );
	}
	
	public void testGetJobOnDay2() {
		final Date diagramStartDate = toDate("2009-01-01 00:00:00" );

		// add jobs
		final SimpleJob job1 = createSimpleJob( "First job in Slot 1",
				Duration.days( 3 ) , 1);
		job1.setStartDate( diagramStartDate );
		
		assertTrue( job1.runsAt( toDate("2009-01-01 00:00:00 " ) ) );
		assertTrue( job1.runsAt( toDate("2009-01-02 00:00:00 " ) ) );
		assertTrue( job1.runsAt( toDate("2009-01-03 00:00:00 " ) ) );
		assertTrue( job1.runsAt( toDate("2009-01-04 00:00:00 " ) ) );
		assertFalse( job1.runsAt( toDate("2009-01-04 00:00:01 " ) ) );
	}
	
	public void testGetUtilization() {
		
		final SimpleJob job1 = createSimpleJob("some job" , Duration.oneDay() , 1 );
		final Date startDate = toDate("2009-01-02 12:00:00" );
		job1.setStartDate( startDate );
		slot.add( job1 );
		
		assertEquals ( 0.5f , slot.getUtilization( new DateRange( toDate("2009-01-02 00:00:00") ,toDate("2009-01-03 00:00:00") ) ) );
		assertEquals ( 1.0f , slot.getUtilization( new DateRange( toDate("2009-01-02 18:00:00") ,toDate("2009-01-03 00:00:00") ) ) );
	}
}
