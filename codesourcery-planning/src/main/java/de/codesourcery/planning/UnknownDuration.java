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

import java.util.Date;

/**
 * An unknown duration (immutable singleton).
 * 
 * <pre>
 * Note that the obvious rules apply (like
 * adding something to an unknown duration
 * yields an unknown duration again etc.).
 * 
 * Method invocations that make no sense on
 * an unknown duration will throw a 
 * {@link UnsupportedOperationException}. 
 * </pre>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public final class UnknownDuration extends Duration
{

	public static final Duration INSTANCE = new UnknownDuration();
	
	private UnknownDuration() {
		super(0);
	}

	@Override
	public Duration add(Duration other)
	{
		return this;
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public Date addTo(Date now)
	{
		throw new UnsupportedOperationException("Cannot add an unknown duration to some date");
	}

	/**
	 * Compares this duration to another
	 * 
	 * @param o
	 * @return 0 if the input duration is unknown as well, otherwise throws
	 * an {@link UnsupportedOperationException}.
	 * @throws UnsupportedOperationException
	 */
	@Override
	public int compareTo(Duration o)
	{
		if ( o.isUnknown() ) {
			return 0;
		}
		throw new UnsupportedOperationException("Cannot compare an unknown duration to some date");
	}

	@Override
	public boolean isUnknown()
	{
		return true;
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public boolean longerThan(Duration other)
	{
		throw new UnsupportedOperationException("Cannot compare with an unknown duration");
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public boolean shorterThan(Duration other)
	{
		throw new UnsupportedOperationException("Cannot compare with an unknown duration");		
	}

	@Override
	public Duration subtract(Duration other)
	{
		return this;
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public double toDays()
	{
		throw new UnsupportedOperationException("Duration is unknown.");
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public double toHours()
	{
		throw new UnsupportedOperationException("Duration is unknown.");		
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public double toMinutes()
	{
		throw new UnsupportedOperationException("Duration is unknown.");
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public double toMonths()
	{
		throw new UnsupportedOperationException("Duration is unknown.");
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public long toSeconds()
	{
		throw new UnsupportedOperationException("Duration is unknown.");
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public String toString()
	{
		return "unknown";
	}

	/**
	 * Always throws an {@link UnsupportedOperationException}.
	 */	
	@Override
	public double toWeeks()
	{
		throw new UnsupportedOperationException("Duration is unknown.");
	}

	@Override
	public boolean equals(Object obj)
	{
		if ( obj == INSTANCE ) {
			return true;
		}
		return false;
	}

}
