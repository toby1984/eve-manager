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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.lang.time.DateUtils;

import de.codesourcery.planning.swing.BoundingBox;
import de.codesourcery.planning.swing.DateAxis;
import de.codesourcery.planning.swing.DateAxis.ITimelineCallback;
import de.codesourcery.planning.swing.ILabelProvider;

public class DateAxisTestTool
{

	private static DateAxis axis = new DateAxis();
	
	public static void main(String[] args)
	{
		
		axis.setStartDate( DateUtils.round( new Date() , Calendar.DAY_OF_MONTH ) );
		axis.setRange( Duration.weeks( 1 ) );
		axis.setTickDuration( Duration.oneDay() );
		
		JFrame frame = new JFrame();
		frame.getContentPane().add( new DateCanvas() );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setLocationRelativeTo( null );
		frame.pack();
		frame.setVisible( true );
	}
	
	@SuppressWarnings("serial")
	private static final class DateCanvas extends JPanel {

		public DateCanvas() {
			super();
			setPreferredSize(new Dimension( 800 , 250 ) );
		}
		
		@Override
		public void paint(final Graphics g)
		{
			super.paint(g);
			
			final ILabelProvider labelProvider = new ILabelProvider() {

				@Override
				public Color getColorFor(IJob job)
				{
					return Color.BLACK;
				}

				@Override
				public String getLabel(IFactorySlot slot)
				{
					return null;
				}

				@Override
				public String getLabel(ISlotType type)
				{
					return null;
				}

				@Override
				public String getLabel(IFactory f)
				{
					return null;
				}

				@Override
				public String getLabel(IJob job)
				{
					return null;
				}

				private final DateFormat DF = new SimpleDateFormat("dd.MM.yy");
				
				@Override
				public String getTimelineLabel(Date date)
				{
					return DF.format( date ); 
				}

				@Override
				public String getTitle()
				{
					return null;
				}
			};
			
			final ITimelineCallback callback = new ITimelineCallback() {

				@Override
				public void drawString(Color color, int x, int y, String s)
				{
					final int ascend = g.getFontMetrics().getAscent();
					g.drawString(s , x , y + ascend+2 );
				}

				@Override
				public BoundingBox getBoundingBox()
				{
					return new BoundingBox( 50, 50, 500 , 200 );
				}

				@Override
				public ILabelProvider getLabelProvider()
				{
					return labelProvider;
				}

				@Override
				public Rectangle2D getStringBounds(String s)
				{
					return g.getFontMetrics().getStringBounds( s , g );
				}

				@Override
				public Graphics2D getGraphics()
				{
					return (Graphics2D) g;
				}
			};
			
			axis.render( callback , false );
		}
	}
}
