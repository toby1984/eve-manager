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

import java.util.Collection;
import java.util.Date;

import org.easymock.EasyMock;

import de.codesourcery.planning.Duration;
import de.codesourcery.planning.DurationTestHelper;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IJob.JobStatus;

public class SimpleJobTest extends DurationTestHelper 
{
	private SimpleJob job;
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		job = createSimpleJob("name" , Duration.seconds(10) , 5 );
	}
	
	public void testCreation() {
		
		final IJobTemplate template = EasyMock.createNiceMock( IJobTemplate.class );
		final SimpleJob job = new SimpleJob("name" , template , Duration.seconds( 10 ) , 5 );
		
		assertEquals( "name" , job.getName() );
		assertEquals( 10 , job.getDuration().toSeconds() );
		assertSame( template ,job.getTemplate() );
		assertEquals( 5 , job.getRuns() );
	}
	
	public void testGetDurationWithin() {
		
		SimpleJob job = createSimpleJob("test", Duration.days( 1 ) , 1);
		job.setStartDate( toDate("2009-09-02 12:00:00 " ) );
	
		assertEquals( Duration.ZERO , 
				job.getDurationWithin( toDate("2009-09-02 00:00:00" ) , toDate("2009-09-02 01:00:00") ) ); 
		
		assertEquals( Duration.ZERO , 
				job.getDurationWithin( toDate("2009-09-03 12:00:01" ) , toDate("2009-09-03 13:00:00") ) ); 
		
		assertEquals( Duration.hours(1) , 
				job.getDurationWithin( toDate("2009-09-02 09:00:00" ) , toDate("2009-09-02 13:00:00") ) ); 
		
		assertEquals( Duration.hours(12) , 
				job.getDurationWithin( toDate("2009-09-03 00:00:00" ) , toDate("2009-09-03 23:00:00") ) ); 
		
		assertEquals( Duration.hours( 24 ) , 
				job.getDurationWithin( toDate("2009-09-02 12:00:00" ) , toDate("2009-09-03 12:00:00") ) ); 
		
		assertEquals( Duration.hours( 12 ) , 
				job.getDurationWithin( toDate("2009-09-02 12:00:00" ) , toDate("2009-09-03 00:00:00") ) ); 
	}
	
	public void testGetDurationWithin2() {
		
		final SimpleJob job1 = createSimpleJob("some job" , Duration.oneDay() , 1 );
		final Date startDate = toDate("2009-01-02 12:00:00" );
		job1.setStartDate( startDate );
		
		assertEquals ( Duration.hours( 6 ), 
				job1.getDurationWithin(toDate("2009-01-02 18:00:00") ,toDate("2009-01-03 00:00:00") ) );
	}
	
	public void testDependentJobs() {
		assertTrue( job.getDependentJobs().isEmpty() );
	}
	
	public void testSetRuns() {
		assertEquals( 5 , job.getRuns() );
		job.setRuns( 1 );
		assertEquals( 1 , job.getRuns() );
		
		try {
			job.setRuns( 0 );
			fail("SHould have failed");
		} catch(IllegalArgumentException e) {
			assertEquals( 1 , job.getRuns() );
		}
		
		try {
			job.setRuns( -10 );
			fail("SHould have failed");
		} catch(IllegalArgumentException e) {
			assertEquals( 1 , job.getRuns() );
		}
		
	}
	public void testNoStartDateSet() {
		assertNull ( job.getStartDate() );
	}
	
	public void testSetStartDate() {
		assertNull ( job.getStartDate() );
		final Date date = new Date();
		job.setStartDate( date );
		assertEquals( date ,job.getStartDate() );
	}
	
	public void testNewJobIsProspective() {
		assertEquals(JobStatus.PROSPECTIVE , job.getStatus() );
		assertTrue( job.hasStatus( JobStatus.PROSPECTIVE ) );
		assertFalse( job.hasStatus( JobStatus.NOT_STARTED ) );
	}
	
	public void testDurationFrom() {
		final IJobTemplate template = EasyMock.createNiceMock( IJobTemplate.class );
		final SimpleJob job = new SimpleJob("name" , template , Duration.seconds( 10 ) , 5 );
		
		job.setStartDate( toDate("2009-09-02 00:00:00" ) );
		assertEquals( Duration.seconds( 10 ) , job.getDurationFrom( toDate("2009-09-01 00:00:00" ) ) );
		assertEquals( Duration.seconds( 10 ) , job.getDurationFrom( toDate("2009-09-02 00:00:00" ) ) );
		assertEquals( Duration.seconds( 0 ) , job.getDurationFrom( toDate("2009-09-03 00:00:00" ) ) );
		assertEquals( Duration.seconds( 0 ) , job.getDurationFrom( toDate("2009-09-03 00:00:00" ) ) );
		assertEquals( Duration.seconds( 5 ) , job.getDurationFrom( toDate("2009-09-02 00:00:05" ) ) );
	}
	
	public void testGetEarliestStartDate() {
		final Date startDate = toDate("2009-09-03 01:02:03" );
		job.setStartDate( startDate );
		assertEquals( startDate , job.getEarliestStartDate() );
	}
	
	public void testGetEarliestStartDateWithoutStartDateSet() {
		try {
			job.getEarliestStartDate();
			fail("Should've failed");
		} catch(IllegalStateException e) {
			/* ok */
		}
	}
	
	public void testGetTotalDuration() {
		assertEquals( Duration.seconds( 10 ) , job.getTotalDuration() );
	}
	
	public void testGetEarliestFinishingDate() {
		job.setStartDate( toDate("2009-09-01 00:00:00 " ) );
		assertEquals( toDate("2009-09-01 00:00:10") , job.getEarliestFinishingDate() );
	}
	
	public void testGetEarliestFinishingDateFailsWIthoutStartDate() {
		try {
			job.getEarliestFinishingDate();
			fail("Should've failed");
		} catch(IllegalStateException e) {
			// ok
		}
	}

	public void testRunsOn() {
		
		final Date date1 = toDate( "2009-01-01 00:00:00 " );
		final Date date2 = toDate( "2009-01-01 00:00:01 " );
		
		final Date date3 = toDate( "2009-01-05 00:00:00 " );
		final Date date4 = toDate( "2009-01-11 00:00:01 " );
		
		final Date date5 = toDate( "2009-01-11 00:00:02 " );
		
		final SimpleJob job = createSimpleJob("Job 1", Duration.days( 10 ) , 5 );
		job.setStartDate( toDate( "2009-01-01 00:00:01 " ) );
		
		assertFalse( job.runsAt( date1 ) );
		
		assertTrue( job.runsAt( date2 ) );
		assertTrue( job.runsAt( date3 ) );
		assertTrue( job.runsAt( date4 ) );
		
		assertFalse( job.runsAt( date5 ) );
	}
	
	private <T> void assertContains(Collection<T> col , T... data ) {
	
outer:		
		for ( T expected : data ) {
			
			for ( T obj : col ) {
				if ( obj.equals( expected ) ) {
					continue outer;
				}
			}
			fail("Did not find "+expected+" in "+col);
		}
	}
	
}
