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
package de.codesourcery.planning.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;

public class DateAxis
{

	public static final boolean DEBUG = false;
	
	private Date startDate;
	private Duration duration;
	private Duration tickDuration = Duration.oneDay();
	
	public interface ITimelineCallback {
		
		public BoundingBox getBoundingBox();
		
		public void drawString( Color color , int x , int y , String s);
		
		public ILabelProvider getLabelProvider();
		
		public Graphics2D getGraphics();
		
		public Rectangle2D getStringBounds( String s);
	}
	
	public DateAxis() {
		this( new Date() , Duration.weeks( 2 ) );
	}
	
	public DateAxis(Date startDate, Duration duration) 
	{
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
	
		if ( duration == null ) {
			throw new IllegalArgumentException("duration cannot be NULL");
		}
		
		if ( duration.isUnknown() ) {
			throw new IllegalArgumentException("duration cannot be unknown");
		}
		
		this.startDate = startDate;
		this.duration = duration;
	}

	public BoundingBox render(ITimelineCallback callback,boolean layoutOnly) 
	{
		final Calendar cal = Calendar.getInstance();
		cal.setTime( startDate );
		cal.set( Calendar.MILLISECOND, 0 );
		Date currentDate = cal.getTime();
		
		final Date endDate = 
			duration.addTo( startDate );
		
		BoundingBox lastLabel = null;
		
		final int labelSpacing = 10;
		
		final Graphics2D graphics = callback.getGraphics();
		final int fontHeight = graphics.getFontMetrics().getHeight();
		
		final double scalingFactor = getXScalingFactor( callback.getBoundingBox() );
		final BoundingBox box = callback.getBoundingBox();
		double x = callback.getBoundingBox().getX();
		final int tickToLabelSpacing = 2;
		final int tickLength = fontHeight;
		final int axisHeight = fontHeight+tickLength+tickToLabelSpacing;
		final double xIncrement = Math.floor( tickDuration.toSeconds() * scalingFactor );
		
		final Color oldColor = graphics.getColor();
//		
		while( currentDate.compareTo( endDate) <=0 ) 
		{
			final int currentX = (int) Math.floor(x);
			if( lastLabel == null || lastLabel.getMaxX() < x ) 
			{
				final String labelText = 
					callback.getLabelProvider().getTimelineLabel( currentDate );
				if ( ! StringUtils.isBlank( labelText ) ) 
				{
					final Rectangle2D stringBounds = 
						callback.getStringBounds( labelText );
					

					if ( ! layoutOnly ) {
						graphics.setColor( Color.BLACK);
						// draw tick
						final Stroke oldStroke = graphics.getStroke();
						graphics.setStroke( new BasicStroke(2.0f) );
						graphics.drawLine( currentX  , box.getY() + axisHeight , currentX , box.getY() + fontHeight + tickToLabelSpacing );
						graphics.setStroke( oldStroke );
						
						// draw label
						callback.drawString( Color.BLACK , currentX , box.getY() , labelText );
					}
					
					final BoundingBox labelBox = new BoundingBox( 
							currentX , 
							box.getY() , 
							currentX + (int) stringBounds.getWidth() + labelSpacing, 
							box.getY() + (int) stringBounds.getHeight() );
					
					if ( lastLabel == null ) 
					{
						lastLabel = labelBox;
					}
					else 
					{
						lastLabel.add( labelBox );
					}
					
				}
			} else {
				// draw short tick
				if ( ! layoutOnly ) {
					
					final int halfTickHeight =
						(int) Math.floor( tickLength / 2.0d);
					
					graphics.drawLine( 
							currentX  , 
							box.getY() + axisHeight , 
							currentX , 
							box.getY() + axisHeight - halfTickHeight  
					);
				}
			}
			
			// draw part of axis
			if ( ! layoutOnly ) {
				graphics.drawLine( 
						(int) x, 
						box.getY() + axisHeight,
						(int) ( x + xIncrement) , 
						box.getY() + axisHeight
				);	
			}
			x+= xIncrement;
			currentDate = tickDuration.addTo( currentDate );
		}
		
		callback.getGraphics().setColor( oldColor );
		final BoundingBox result = lastLabel != null ? lastLabel : new BoundingBox(0,0,0,0);
		result.incHeight( axisHeight );
		return result;
	}
	
	protected static void debugBoundingBox(BoundingBox box , Graphics2D g) {
		if ( DEBUG ) {
			Color old = g.getColor();
			g.setColor(Color.RED);
			g.drawRect(box.getX() , box.getY() , box.getWidth() , box.getHeight() );
			g.setColor( old );
		}
	}
	
	public BoundingBox getBoundingBoxFor(ITimelineCallback callback, Date start, Duration range ) {
		return getBoundingBoxFor( callback , new DateRange( start, range ) ); 
	}
	
	public BoundingBox getBoundingBoxFor(ITimelineCallback callback, Date start, Date end) {
		return getBoundingBoxFor( callback , new DateRange( start, end ) ); 
	}
	
	public BoundingBox getBoundingBoxFor(ITimelineCallback callback, DateRange range) {
		
		final Date start;
		if ( range.getStartDate().before( startDate ) ) {
			start = startDate;
		} else {
			start = range.getStartDate();
		}
		
		final Date end;
		final Date endDate = duration.addTo( startDate );
		if ( range.getEndDate().after( endDate) ) {
			end = endDate;
		} else {
			end = range.getEndDate();
		}

		final Duration realDuration = new Duration(start,end);
		
		final BoundingBox box =
			callback.getBoundingBox();
		
		final long startSeconds = (long) Math.floor( ( start.getTime() - this.startDate.getTime() ) / 1000.0f);
		
		final double scalingFactor = getXScalingFactor( box );
		
		final int startX =  (int) ( box.getX() + Math.floor( scalingFactor * startSeconds ) );
		final int endX =  (int) ( startX + Math.floor( scalingFactor * realDuration.toSeconds() ) );
		return new BoundingBox( startX , 0 , endX , 0 );
	}
	
	public Date getEndDate() {
		return getRange().addTo( getStartDate() );
	}
	
	private double getXScalingFactor(BoundingBox box) {
		return ( box.getWidth() - box.getX() ) / (double) duration.toSeconds();
	}
	
	public void setStartDate(Date startDate)
	{
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		this.startDate = startDate;
	}

	public Duration getTickDuration() {
		return this.tickDuration;
	}
	
	public void setTickDuration(Duration d) 
	{
		if ( d == null ) {
			throw new IllegalArgumentException("d cannot be NULL");
		}
		
		if ( d.isUnknown() ) {
			throw new IllegalArgumentException("duration cannot be unknown");
		}
		
		if ( d.shorterThan( Duration.oneSecond() ) ) {
			throw new IllegalArgumentException("duration cannot be less than 1 second");
		}
	
		this.tickDuration = d;
	}

	public Date getStartDate()
	{
		return startDate;
	}
	
	public double getRangeInDays() {
		return this.duration.toDays();
	}

	public void setRange(Duration range)
	{
		if ( range == null ) {
			throw new IllegalArgumentException("duration cannot be NULL");
		}
		
		if ( range.isUnknown() ) {
			throw new IllegalArgumentException("duration cannot be unknown");
		}
		
		this.duration= range;
	}


	public Duration getRange()
	{
		return duration;
	}
	
}
