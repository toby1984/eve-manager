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
package de.codesourcery.planning;


public class DurationTest extends DurationTestHelper
{

	public void testZero() {
		assertEquals( 0 , Duration.ZERO.toSeconds() );
	}
	
	public void testNotUnknown() {
		assertFalse( Duration.seconds( 1 ).isUnknown() );
	}
	
	public void testAddUnknown() {
		assertEquals( Duration.UNKNOWN , Duration.seconds( 5 ).add( Duration.UNKNOWN ) );
		assertEquals( Duration.UNKNOWN , Duration.UNKNOWN.add( Duration.seconds( 5 ) ) );
	}
	
	public void testAddYearsToDate() {
		assertEquals( toDate("2010-09-01 15:00:00" ) , Duration.add( 
				toDate("2009-09-01 15:00:00") , Duration.years( 1 ) ) );
	}		
	
	public void testRoundToDays() {
		
		assertEquals( Duration.days( 1 ) , Duration.days( 1 ).roundToDuration( Duration.Type.DAYS ) );
		
		assertEquals( Duration.days( 1 ) , 
				Duration.days( 1 )
					.add( Duration.hours( 11 ) )
					.add( Duration.minutes( 59 )
				).roundToDuration( Duration.Type.DAYS ) 
		);
		
		assertEquals( Duration.days( 2 ) , Duration.days( 1 ).add( Duration.hours( 12 ) ).roundToDuration( Duration.Type.DAYS ) );
		
		assertEquals( Duration.days( 2 ) , 
				Duration.days( 1 )
					.add( Duration.hours( 12 ) )
					.add( Duration.seconds( 1 ) 
				).roundToDuration( Duration.Type.DAYS )
		);
	}
	
	public void testTruncateToDays() {
		
		assertEquals( Duration.days( 1 ) , Duration.days( 1 ).truncateTo( Duration.Type.DAYS ) );
		
		assertEquals( Duration.days( 1 ) , 
				Duration.days( 1 )
					.add( Duration.hours( 11 ) )
					.add( Duration.minutes( 59 )
				).truncateTo( Duration.Type.DAYS ) 
		);
		
		assertEquals( Duration.days( 1 ) , Duration.days( 1 ).add( Duration.hours( 12 ) ).truncateTo( Duration.Type.DAYS ) );
		
		assertEquals( Duration.days( 1 ) , 
				Duration.days( 1 )
					.add( Duration.hours( 12 ) )
					.add( Duration.seconds( 1 ) 
				).truncateTo( Duration.Type.DAYS )
		);
	}
	
	public void testAddMonthsToDate() {
		assertEquals( toDate("2009-09-29 15:00:00" ) , Duration.add( 
				toDate("2009-09-01 15:00:00") , Duration.months( 1 ) ) ); 
	}	
	
	public void testAddWeeksToDate() {
		assertEquals( toDate("2009-09-08 15:00:00" ) , Duration.add( 
				toDate("2009-09-01 15:00:00") , Duration.weeks( 1 ) ) ); 
	}	
	
	public void testAddDaysToDate() {
		assertEquals( toDate("2009-09-28 15:00:00" ) , Duration.add(
				toDate("2009-09-26 15:00:00") , Duration.days( 2 ) ) ); 
	}
	
	public void testAddHoursToDate() {
		assertEquals( toDate("2009-09-28 15:00:00" ) , Duration.add( 
				toDate("2009-09-28 00:00:00") , Duration.hours( 15 ) ) ); 
	}
	
	public void testAddMinutesToDate() {
		assertEquals( toDate("2009-09-28 00:15:00" ) , Duration.add( 
				toDate("2009-09-28 00:00:00") , Duration.minutes( 15 ) ) ); 
	}
	
	public void testAddSecondsToDate() {
		assertEquals( toDate("2009-09-28 00:00:15" ) , Duration.add( 
				toDate("2009-09-28 00:00:00") , Duration.seconds( 15 ) ) ); 
	}	
	
	public void testFromDateRange1() {
		assertEquals( 10 , new Duration( toDate("2009-09-28 00:00:15" ) , toDate("2009-09-28 00:00:25" ) ).toSeconds() );
	}
	
	public void testFromDateRange2() {
		assertEquals( 
				1.0d , 
				new Duration( toDate("2009-09-28 00:00:15" ) , toDate("2009-09-29 00:00:15" ) )
				.toDays() 
		);
	}
	
	public void testNegativeDateRangeNotPermitted() {
		try {
			new Duration( toDate("2009-09-30 00:00:15" ) , toDate("2009-09-29 00:00:15" ) );
			fail("Should have failed");
		} catch(IllegalArgumentException e ) {
			// ok
		}
	}
	
	public void testAdd() {
		assertEquals( 30 , Duration.seconds( 10 ).add( Duration.seconds( 20 ) ).toSeconds() );
		
		assertEquals( Duration.UNKNOWN , Duration.seconds( 10 ).add( Duration.UNKNOWN ) );
		assertEquals( Duration.UNKNOWN , Duration.UNKNOWN.add( Duration.seconds( 10 ) ) );
	}
	
	public void testSubtract() {
		assertEquals( 5 , Duration.seconds( 10 ).subtract( Duration.seconds( 5 ) ).toSeconds() );
		
		assertEquals( Duration.UNKNOWN , Duration.seconds( 10 ).subtract( Duration.UNKNOWN ) );
		assertEquals( Duration.UNKNOWN , Duration.UNKNOWN.subtract( Duration.seconds( 10 ) ) );
	}
	
	public void testSubtractFailsIfResultNegative() {
		try {
			Duration.seconds( 10 ).subtract( Duration.seconds( 20 ) );
			fail("Should have failed");
		} 
		catch(IllegalArgumentException e) {
			// ok
		}
	}
	
	public void testLongerThan() {
		assertTrue( Duration.minutes( 1 ).longerThan( Duration.seconds( 1 ) ) );
		assertFalse( Duration.minutes( 1 ).longerThan( Duration.minutes( 2 ) ) );
		
		try {
			Duration.UNKNOWN.longerThan( Duration.seconds( 10 ) );
		} catch(UnsupportedOperationException e) { /* ok */ }
		
		try {
			Duration.seconds( 10 ).longerThan( Duration.UNKNOWN);
		} catch(UnsupportedOperationException e) { /* ok */ }
	}
	
	public void testShorterThan() {
		assertFalse( Duration.minutes( 1 ).shorterThan( Duration.seconds( 1 ) ) );
		assertTrue( Duration.minutes( 1 ).shorterThan( Duration.minutes( 2 ) ) );
		
		try {
			Duration.UNKNOWN.shorterThan( Duration.seconds( 10 ) );
		} catch(UnsupportedOperationException e) { /* ok */ }
		
		try {
			Duration.seconds( 10 ).shorterThan( Duration.UNKNOWN);
		} catch(UnsupportedOperationException e) { /* ok */ }
	}
	
	public void testToString() {
		assertEquals( "1 month , 2 weeks , 3 days , 4 hours , 5 minutes , 1 second",
				Duration.months(1)
				.add( Duration.weeks( 2 ) )
				.add( Duration.days( 3 ) )
				.add( Duration.hours( 4) )
				.add( Duration.minutes( 5 ) )
				.add( Duration.seconds( 1 ) )
				.toString() );
		
		assertEquals("unknown" , Duration.UNKNOWN.toString() );
	}
	
	public void testZeroDurationToString() {
		assertEquals( "0 seconds" , Duration.ZERO.toString() );
	}
	
}
