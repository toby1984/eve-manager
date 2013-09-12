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

import org.easymock.EasyMock;

import de.codesourcery.planning.Duration;
import de.codesourcery.planning.DurationTestHelper;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IJob.JobStatus;

public class ComplexJobTest extends DurationTestHelper 
{

	private SimpleJob job;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		job = createComplexJob( "name" , Duration.seconds( 10 ) );
	}

	private SimpleJob createJob(IJobTemplate template)
	{
		return new SimpleJob( "name" , template , Duration.seconds( 10 ) );
	}

	protected SimpleJob createComplexJob(String name , Duration d)
	{
		final IJobTemplate template = niceMock( IJobTemplate.class );
		return new SimpleJob( name , template , d );
	}

	public void testCreation() {

		final IJobTemplate template = EasyMock.createNiceMock( IJobTemplate.class );
		final SimpleJob job = createJob(template);

		assertEquals( "name" , job.getName() );
		assertEquals( 10 , job.getDuration().toSeconds() );
		assertSame( template ,job.getTemplate() );
		assertEquals( 1 , job.getRuns() );
	}



	public void testDependentJobs() {
		assertTrue( job.getDependentJobs().isEmpty() );
	}

	public void testAddDependencyWorks() {

		assertTrue( job.getDependentJobs().isEmpty() );
		IJob mockJob = niceMock( IJob.class );
		job.addDependentJob( mockJob );
		assertEquals( 1 , job.getDependentJobs().size() );
		assertSame( mockJob , job.getDependentJobs().get(0) );
	}

	public void testSetRuns() {
		assertEquals( 1 , job.getRuns() );
		job.setRuns( 2 );
		assertEquals( 2 , job.getRuns() );
	}

	public void testGetStartDateWithoutChildren() {
		
		Date date = toDate( "2008-11-12 13:14:15" );
		job.setStartDate( date );
		assertEquals( date , job.getStartDate() );

		Date date2 = toDate( "2009-11-12 13:14:15" );
		SimpleJob simpleJob = createSimpleJob("test", Duration.weeks( 1 ) , 1 );
		simpleJob.setStartDate( date2 );
		
		job.addDependentJob( simpleJob );
		assertEquals( Duration.weeks( 1 ).addTo( date2 ) , job.getStartDate() );
	}

	public void testNewJobIsProspective() {
		assertEquals(JobStatus.PROSPECTIVE , job.getStatus() );
		assertTrue( job.hasStatus( JobStatus.PROSPECTIVE ) );
		assertFalse( job.hasStatus( JobStatus.NOT_STARTED ) );
	}

	public void testDurationFrom() {
		final IJobTemplate template = EasyMock.createNiceMock( IJobTemplate.class );
		final SimpleJob job = new SimpleJob("name" , template , Duration.seconds( 10 ) , 5 );

		job.setStartDate( toDate("2009-09-01 00:00:00" ) );
		assertEquals( Duration.seconds( 10 ) , job.getDurationFrom( toDate("2009-09-01 00:00:00" ) ) );
		assertEquals( Duration.seconds( 0 ) , job.getDurationFrom( toDate("2009-09-02 00:00:00" ) ) );
		assertEquals( Duration.seconds( 10 ) , job.getDurationFrom( toDate("2009-08-02 00:00:00" ) ) );
		assertEquals( Duration.seconds( 5 ) , job.getDurationFrom( toDate("2009-09-01 00:00:05" ) ) );
	}

	public void testGetEarliestFinishingDateFailsWIthoutStartDate() {
		try {
			job.getEarliestFinishingDate();
			fail("Should've failed");
		} catch(IllegalStateException e) {
			// ok
		}
	}

	/*
	 * And now for the interesting stuff...
	 */
	public void testOneSimpleDependentJob() {

		final IJobTemplate template = mock( IJobTemplate.class );

		final SimpleJob simpleJob = new SimpleJob("Simple" , template , Duration.seconds( 20 ) , 5 );
		final Date startDate = toDate("2009-09-01 00:00:00" );
		simpleJob.setStartDate( startDate );

		job.addDependentJob( simpleJob );

		assertEquals( toDate("2009-09-01 00:00:20" ) , job.getStartDate() );
		assertEquals( toDate("2009-09-01 00:00:20" ) , job.getEarliestStartDate() );
		assertEquals( Duration.seconds( 10 ) , job.getDuration() );
		assertEquals( Duration.seconds( 30 ) , job.getTotalDuration() );

		assertEquals( toDate("2009-09-01 00:00:30") , job.getEarliestFinishingDate() );
	}

	public void testTwoSimpleDependentJobs() {

		final IJobTemplate template = mock( IJobTemplate.class );

		final SimpleJob simpleJob = new SimpleJob("Simple" , template , Duration.seconds( 20 ) , 5 );
		final Date startDate = toDate("2009-09-01 00:00:00" );
		simpleJob.setStartDate( startDate );

		final SimpleJob simpleJob2 = new SimpleJob("Simple" , template , Duration.seconds( 20 ) , 5 );
		final Date startDate2 = toDate("2009-09-2 00:00:00" );
		simpleJob2.setStartDate( startDate2 );

		job.addDependentJob( simpleJob );
		job.addDependentJob( simpleJob2 );

		assertEquals( toDate("2009-09-02 00:00:20" ) , job.getStartDate() );
		assertEquals( toDate("2009-09-02 00:00:20" ) , job.getEarliestStartDate() );
		assertEquals( Duration.seconds( 10 ) , job.getDuration() );
		assertEquals( Duration.seconds( 30 ).add( Duration.oneDay() ) , job.getTotalDuration() );

		assertEquals( toDate("2009-09-02 00:00:30") , job.getEarliestFinishingDate() );
	}
	
	public void testRunsOn() {
		
		final Date date1 = toDate( "2009-01-01 00:00:00" );
		final Date date2 = toDate( "2009-01-01 00:00:01" );
		
		final Date date3 = toDate( "2009-01-02 00:00:00" );
		final Date date4 = toDate( "2009-01-02 00:00:01" );
		final Date date5 = toDate( "2009-01-03 00:00:00" );
		
		final SimpleJob complexJob =
			createComplexJob( "complex job", Duration.oneDay() );
		
		final SimpleJob job = createSimpleJob( "Job 1", Duration.days( 1 ) , 5 );
		job.setStartDate( toDate( "2009-01-01 00:00:01 " ) );
		
		complexJob.addDependentJob( job );
		
		assertFalse( complexJob.runsAt( date1 ) );
		
		assertFalse( complexJob.runsAt( date2 ) );
		
		assertFalse( complexJob.runsAt( date3 ) );
		assertTrue( complexJob.runsAt( date4 ) );
		
		assertTrue( complexJob.runsAt( date5 ) );
	}

}
