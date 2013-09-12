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
package de.codesourcery.planning.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.IJobTemplate;
import de.codesourcery.planning.IProductionLocation;
import de.codesourcery.planning.ISlotType;
import de.codesourcery.planning.IJobTemplate.JobMode;
import de.codesourcery.planning.impl.SimpleFactory;
import de.codesourcery.planning.impl.SimpleJob;
import de.codesourcery.planning.impl.SimpleJobTemplate;
import de.codesourcery.planning.impl.SimpleProductionSlot;
import de.codesourcery.planning.swing.ILabelProvider;
import de.codesourcery.planning.swing.IRegionClickedListener;
import de.codesourcery.planning.swing.IRegionOfInterest;
import de.codesourcery.planning.swing.IToolTipProvider;
import de.codesourcery.planning.swing.PlanningCanvas;
import de.codesourcery.planning.swing.PointOnTimeline;

public class PlanningCanvasDemo
{

	private static final IProductionLocation LOCATION1 = new IProductionLocation() {
		@Override
		public String toString()
		{
			return "Location #1";
		}
	};
	
	private static final IProductionLocation LOCATION2 = new IProductionLocation() {
		@Override
		public String toString()
		{
			return "Location #2";
		}
	};	
	
	public static void main(String[] args) throws ParseException
	{

		// setup factory with dummy data

		final SimpleJobTemplate template = new SimpleJobTemplate( 1 , JobMode.AUTOMATIC ) {};

		final ISlotType type = new ISlotType() {

			@Override
			public boolean accepts(IJobTemplate t)
			{
				return t instanceof SimpleJobTemplate;
			}
		};

		final SimpleFactory factory1 = new SimpleFactory("Factory no. 1");
		final SimpleFactory factory2 = new SimpleFactory("Factory no. 2");

		final SimpleProductionSlot slot1 = new SimpleProductionSlot("Slot no. 1" , type , LOCATION1 );
		factory1.addSlot( slot1 );

		final SimpleProductionSlot slot2 = new SimpleProductionSlot("Slot no. 2 with a long name" , type , LOCATION1);
		factory1.addSlot( slot2 );
		
		final SimpleProductionSlot slot3 = new SimpleProductionSlot("Slot no. 3" , type , LOCATION2 );
		factory2.addSlot( slot3 );

		final DateFormat dateFormat =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		final Duration diagramRange = Duration.days( 7 );
		final Date diagramStartDate = dateFormat.parse("2009-01-01 00:00:00" );

		// add jobs
		final SimpleJob job1 = new SimpleJob("First job in Slot 1",template , Duration.days( 3 ) , 1);
		
		job1.setStartDate( diagramStartDate );
		slot1.add( job1);
		
		final SimpleJob job2 = new SimpleJob("First job in Slot 3",
				template  , Duration.days( 3 ).plus( Duration.hours( 12 )  ) , 1);
		
		job2.setStartDate( Duration.oneDay().addTo( diagramStartDate ) );
		slot3.add( job2 );

		final SimpleJob job3 = new SimpleJob("Second job in Slot 3",
				template, Duration.days( 3 ) , 1);
		
		job3.setStartDate( Duration.oneSecond().addTo( job2.getEndDate() ) );
		slot3.add( job3 );
		

		// setup label provider
		final ILabelProvider myLabelProvider = new ILabelProvider() {

			final DateFormat dateFormat =
				new SimpleDateFormat("dd.MM");

			@Override
			public String getLabel(ISlotType type)
			{
				return type.toString();
			}

			@Override
			public String getLabel(IFactory f)
			{
				return ((SimpleFactory) f).getName();
			}

			@Override
			public String getLabel(IFactorySlot slot)
			{
				return ((SimpleProductionSlot) slot).getName();
			}

			@Override
			public String getLabel(IJob job)
			{
				return job.toString();
			}

			@Override
			public String getTitle()
			{
				return "Slot utilization: "+dateFormat.format( diagramStartDate )+
				" - "+dateFormat.format( diagramRange.addTo( diagramStartDate ) );
			}

			@Override
			public Color getColorFor(IJob job)
			{
				final long start = job.getStartDate().getTime();
				final int r = 255; // (int) ( start & 255);
				final int g = (int) (( start & ( 255 << 8 ) ) >>8);
				final int b = (int) (( start & ( 255 << 16 ) ) >> 16);
				return new Color(r,g,b);
			}

			@Override
			public String getTimelineLabel(Date date)
			{
				return dateFormat.format( date );
			}
		};

		// setup canvas
		final PlanningCanvas canvas = new PlanningCanvas( myLabelProvider );
		canvas.addFactories( factory1 );
		canvas.addFactories( factory2 );
		canvas.setPreferredSize( new Dimension( 800 , 600 ) );
		canvas.setDateRange( Duration.days( 12 ) );
		canvas.setStartDate( diagramStartDate );

		
		canvas.setToolTipProvider( new IToolTipProvider() {

			final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			private String toString(DateRange d ) {
				return DF.format( d.getStartDate() )+" - "+DF.format( d.getEndDate() );
			}
			
			@Override
			public String getToolTipText(IRegionOfInterest region)
			{
				if ( region.getContents() instanceof PointOnTimeline ) {
					final PointOnTimeline point = (PointOnTimeline) region.getContents();
					
					final List<IJob> jobsOnDay = point.getProductionSlot().getJobsOnDay( point.getDate() );
					
					if ( jobsOnDay.isEmpty() ) {
						return null;
					}
					
					final StringBuilder result = new StringBuilder("<HTML><BODY>");
					for ( Iterator<IJob> it =jobsOnDay.iterator() ; it.hasNext() ;  ) 
					{
						final IJob job = it.next();
						result.append( job.getName()+" at "+toString( job.getDateRange() ) );
						if ( it.hasNext() ) {
							result.append("<BR>");
						}
					}
					result.append("</BODY></HTML>");
					return result.toString();
				} 
				return null;
			}} );
		
		// register mouselistener
		canvas.addRegionClickedListener( new IRegionClickedListener() {

			final DateFormat DF = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			
			private void showMessage(String msg) {
				JOptionPane.showMessageDialog(
						canvas,
						"You clicked: "+msg, "Clicked" , JOptionPane.INFORMATION_MESSAGE );	
			}
			@Override
			public void clicked(IFactorySlot slot,ClickType type)
			{
				if ( type == ClickType.DOUBLE_CLICK ) {
					showMessage("Production slot "+slot);
				}
			}
			
			private String toString(Date d) {
				return DF.format( d );
			}

			@Override
			public void clicked(PointOnTimeline pointOnTimeline,ClickType type)
			{
				
				if ( type != ClickType.DOUBLE_CLICK ) {
					return;
				}
				
				final StringBuilder text = new StringBuilder();
				final List<IJob> jobs = pointOnTimeline.getProductionSlot().getJobsOnDay( pointOnTimeline.getDate() );
				
				int i = 1;
				for ( IJob j : jobs ) {
					text.append("\n").append( "Job "+i+": "+j+" , "+toString( j.getStartDate() )+" - "+
							toString( j.getEndDate() ) );
					i++;
				}
				
				showMessage("Point on timeline: "+
						pointOnTimeline.getDate()+" / "+pointOnTimeline.getProductionSlot()+
						"\n"+text);
			}
		} );
		
		// create a frame
		final JFrame frame = new JFrame();
		canvas.setBorder(BorderFactory.createLineBorder(Color.black ) );
		canvas.setPreferredSize(new Dimension(600, 100 + ( 3 * 100 ) ) );
		JScrollPane pane = new JScrollPane( canvas );
		frame.getContentPane().add( pane );

		frame.pack();
		frame.setLocationRelativeTo( null );

		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible( true );
	}
}
