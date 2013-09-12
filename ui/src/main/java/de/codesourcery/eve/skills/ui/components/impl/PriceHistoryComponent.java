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
package de.codesourcery.eve.skills.ui.components.impl;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.DateRange;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.market.IPriceInfoStore;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.utils.DateHelper;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class PriceHistoryComponent extends AbstractComponent
{

	private enum DisplayRange {
		WEEK(7) {
			@Override
			protected String getDisplayName()
			{
				return "One week";
			}
		},
		MONTH(4*7) {
			@Override
			protected String getDisplayName()
			{
				return "One month";
			}
		},
		THREE_MONTHS(3*4*7) {
			@Override
			protected String getDisplayName()
			{
				return "Three months";
			}
		},
		FULL(-1) {
			@Override
			protected String getDisplayName()
			{
				return "All available data";
			}
		};

		private final int rangeInDays;

		private DisplayRange(int rangeInDays) {
			this.rangeInDays = rangeInDays;
		}

		protected abstract String getDisplayName();

		@Override
		public String toString()
		{
			return getDisplayName();
		}

		public int getRangeInDays() {
			return rangeInDays;
		}
	}

	@Resource(name="priceinfo-store")
	private IPriceInfoStore priceInfoStore;

	@Resource(name="system-clock")
	private ISystemClock systemClock;

	// other stuff
	private final InventoryType item;
	private final PriceInfo.Type priceType;
	private final Region region;

	private final DataSets currentDataSets = new DataSets();
	private Date earliestDate;
	private Date latestDate;

	// Swing
	private final JComboBox dateRangeChooser =
		new JComboBox();

	private final JComboBox movingAverageChooser =
		new JComboBox();


	// JFreeChart
	private JFreeChart chart;

	private final ActionListener actionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e)
		{
			updateGraph();
		}
	};

	private enum MovingAverage {
		NONE(0) {
			@Override
			public String toString()
			{
				return "No moving average shown";
			}
		} ,
		TRHEE_DAYS(3),
		FIVE_DAYS(5),
		TEN_DAYS(10);

		private final int days;
		private MovingAverage(int days) {
			this.days = days;
		}

		public int getDays() { return days; }
		@Override
		public String toString() { return days+" days moving average"; }
	}

	public PriceHistoryComponent(InventoryType item,Region region , PriceInfo.Type priceType) {

		if ( item == null ) {
			throw new IllegalArgumentException("item cannot be NULL");
		}

		if ( priceType == null ) {
			throw new IllegalArgumentException("priceType cannot be NULL");
		}

		if ( region == null ) {
			throw new IllegalArgumentException("region cannot be NULL");
		}

		this.region = region;
		this.item = item;
		this.priceType = priceType;
	}

	@Override
	protected void onAttachHook(IComponentCallback callback)
	{
	}

	@Override
	protected JPanel createPanel()
	{

		dateRangeChooser.setModel( 
				new DefaultComboBoxModel<DisplayRange>( Arrays.asList( DisplayRange.values() ) )
		);

		dateRangeChooser.setSelectedItem( DisplayRange.WEEK );

		final JPanel controlPanel = new JPanel();
		controlPanel.setBackground(Color.WHITE);
		//		controlPanel.setLayout( new GridBagLayout() );
		//		controlPanel.add( dateRangeChooser , constraints().noResizing().end() );
		controlPanel.add( dateRangeChooser);

		dateRangeChooser.addItemListener( new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e)
			{
				changeDateRange( (DisplayRange) dateRangeChooser.getSelectedItem() );
			}} );

		// moving avg. chooser
		this.movingAverageChooser.setModel( 
				new DefaultComboBoxModel<MovingAverage>(
						Arrays.asList( MovingAverage.values() 
						)
				));

		this.movingAverageChooser.setSelectedItem( MovingAverage.FIVE_DAYS );
		this.movingAverageChooser.addActionListener( actionListener );
		controlPanel.add( movingAverageChooser );

		final JPanel result = new JPanel();
		result.setBackground(Color.WHITE);
		result.setLayout( new GridBagLayout() );

		final JPanel freeChartPanel = createFreeChartPanel(); // attention: chart must always be
		// populated AFTER date range chooser is set up because
		// this calculates earliest/latest dates from dataset

		//		panel.setPreferredSize( new Dimension(500,200 ) );

		result.add( controlPanel , constraints(0,0).resizeBoth().weightX(0.2).weightY(0.2).end() );
		result.add( freeChartPanel , constraints(0,1).resizeBoth().end() );

		return result;
	}

	protected void changeDateRange(DisplayRange range) {

		if ( latestDate == null ) {
			return;
		}

		final CombinedDomainXYPlot plot = (CombinedDomainXYPlot) chart.getPlot();
		DateAxis axis = (DateAxis) plot.getDomainAxis();

		final Calendar cal = Calendar.getInstance();
		if ( range.getRangeInDays() == -1 ) {
			cal.setTime( earliestDate );
		} else {
			cal.setTime( latestDate );
			cal.add( Calendar.DAY_OF_MONTH , -range.getRangeInDays() );
		}
		axis.setRange(  new DateRange( cal.getTime() , latestDate) );
	}

	private static final Comparator<PriceInfo> DATE_SORTER = new Comparator<PriceInfo> () {

		@Override
		public int compare(PriceInfo o1, PriceInfo o2)
		{
			return o1.getTimestamp().compareTo( o2.getTimestamp() );
		}
	};

	private final class DataSets {

		protected TimeSeries avgBuy;
		protected TimeSeries avgSell;

		protected TimeSeries buyPrice;
		protected final TimeSeries sellPrice;
		
		protected final TimeSeriesCollection prices;
		
		protected XYPlot pricePlot;
		protected XYPlot volumePlot;
		
		protected final TimeSeries buyVolume;
		protected final TimeSeries sellVolume;
		
		
		public DataSets() {
			prices = new TimeSeriesCollection();
			buyVolume = new TimeSeries("Buy volume");
			sellVolume = new TimeSeries("Sell volume");
			sellPrice = new TimeSeries("Sell price");
			buyPrice = new TimeSeries("Buy price");
		}
	}

	protected MovingAverage getMovingAverageSetting() {
		return (MovingAverage) this.movingAverageChooser.getSelectedItem();
	}

	protected void rememberDate(Date d) {
		if ( this.earliestDate == null || d.before( this.earliestDate ) ) {
			earliestDate = d;
		}
		if ( latestDate == null || d.after( latestDate ) ) {
			latestDate = d;
		}
	}


	private TimeSeries calcMovingAverage(String name,List<PriceInfo> data,MovingAverage dateRange) {

		// group price infos by days
		final Map<Date, List<PriceInfo>> pricesByDay = 
			new HashMap<Date,List<PriceInfo>>();

		for ( PriceInfo info : data ) {
			if ( info.getAveragePrice() == 0 ) {
				continue;
			}

			final Date stripped = DateHelper.stripTime( info.getTimestamp().getLocalTime() );
			List<PriceInfo> existing = pricesByDay.get(stripped);
			if ( existing == null) {
				existing = new ArrayList<PriceInfo>();
				pricesByDay.put( stripped , existing );
			}
			existing.add( info );
		}

		// calculate daily averages
		final Map<Date,Double> averages =
			new HashMap<Date, Double>();

		for ( Map.Entry<Date,List<PriceInfo>> entry : pricesByDay.entrySet() ) {

			double amount = 0;
			for ( PriceInfo info : entry.getValue() ) {
				amount+= info.getAveragePrice();
			}
			double average = amount / entry.getValue().size();
			averages.put( entry.getKey() , average );
		}

		// sort averages ascending by dates 
		final List<Date> dates = new ArrayList<Date>();
		dates.addAll( averages.keySet() );
		Collections.sort( dates );

		if ( dates.size() <= 2 ) {
			return null;
		}

		/*
		 * For each date between
		 * 'earliest' and 'latest':
		 * 
		 *  Calculate the average from this 
		 *  day and the previous 4 days.
		 */
		final Date latest= dates.get( dates.size() -1 );
		final Date earliest = dates.get(0);

		final Calendar current = Calendar.getInstance();
		current.setTime( latest );

		final Map<Date,Double> movingAverage =
			new HashMap<Date,Double>();

		// TODO: Not a very efficient implementation....
		final int days = dateRange.getDays();
		do {
			int count = 0;
			double sum=0;

			Calendar loop = Calendar.getInstance();
			loop.setTime( current.getTime() );
			for ( int i = 0 ; i < days ; i++ ) {
				Double avg = averages.get( loop.getTime() );
				if ( avg != null ) {
					count++;
					sum += avg;
				}
				loop.add( Calendar.DAY_OF_MONTH , -1 );
			}

			if ( count > 0 ) {
				final double average = ( sum / 100.0d) / (double) count;
				movingAverage.put( current.getTime() , average);
			}

			current.add( Calendar.DAY_OF_MONTH , -1 );
		} while ( current.getTime().compareTo( earliest ) >= 0 );

		final TimeSeries result = new TimeSeries(name);
		for ( Map.Entry<Date,Double> entry : movingAverage.entrySet()) {
			result.add( new Day( entry.getKey() ) , entry.getValue() );
		}
		return result;
	}

	protected void updateGraph() {
		setupDataSets();
	}

	protected void setupDataSets() {

		currentDataSets.prices.removeAllSeries();
		
		if ( this.priceType.matches( PriceInfo.Type.BUY ) ) 
		{

			final List<PriceInfo> buyPrices = 
				priceInfoStore.getPriceHistory( region , PriceInfo.Type.BUY, item );

			Collections.sort( buyPrices , DATE_SORTER );

			currentDataSets.buyPrice.clear();
			currentDataSets.buyVolume.clear();
			
			for ( PriceInfo info : buyPrices ) {
				final Day day = new Day( info.getTimestamp().getLocalTime() );
				rememberDate( info.getTimestamp().getLocalTime() );
				currentDataSets.buyPrice.add( day , info.getAveragePrice() / 100.0d );
				final long volume = info.getRemainingVolume(); 
				if ( volume > 0 ) {
					currentDataSets.buyVolume.add( day , volume );
				}
			}

			currentDataSets.prices.addSeries( currentDataSets.buyPrice );

			if ( getMovingAverageSetting() != MovingAverage.NONE ) 
			{
				final MovingAverage desiredRange =
					getMovingAverageSetting();
				
				final TimeSeries movingAverage =
					calcMovingAverage( desiredRange.getDays()+"-days moving avg. buy price" ,
							buyPrices, desiredRange );

				if ( movingAverage != null ) 
				{
					currentDataSets.prices.addSeries( movingAverage );
					currentDataSets.avgBuy = movingAverage;
				}
				else 
				{
					currentDataSets.avgBuy = null;
				}
				
			} else {
				currentDataSets.avgBuy = null;
			}

		}

		if ( this.priceType.matches( PriceInfo.Type.SELL ) ) 
		{
			final List<PriceInfo> sellPrices = 
				priceInfoStore.getPriceHistory( region , PriceInfo.Type.SELL , item );

			Collections.sort( sellPrices , DATE_SORTER );

			currentDataSets.sellPrice.clear();
			currentDataSets.sellVolume.clear();

			for ( PriceInfo info : sellPrices ) {
				final Day day = new Day( info.getTimestamp().getLocalTime() );

				rememberDate( info.getTimestamp().getLocalTime() );
				currentDataSets.sellPrice.add( day , info.getAveragePrice() / 100.0d );

				final long volume = info.getRemainingVolume();
				if ( info.getVolume() > 0 ) {
					currentDataSets.sellVolume.add( day , volume  );
				}
			}

			currentDataSets.prices.addSeries( currentDataSets.sellPrice );
			
			if ( getMovingAverageSetting() != MovingAverage.NONE ) 
			{
				final MovingAverage desiredRange =
					getMovingAverageSetting();
				
				final TimeSeries movingAverage =
					calcMovingAverage( desiredRange.getDays()+"-days moving avg. sell price" ,
							sellPrices, desiredRange );

				if ( movingAverage != null ) 
				{
					currentDataSets.prices.addSeries( movingAverage );
					currentDataSets.avgSell = movingAverage;
				}
				else 
				{
					currentDataSets.avgSell = null;
				}
				
			} else {
				currentDataSets.avgSell = null;
			}
		}

	}

	private JPanel createFreeChartPanel() {
		setupDataSets();
		return createFreeChartPanel2();
	}

	private JPanel createFreeChartPanel2() {

		/*
		 * Price plot.
		 */
		final StandardXYItemRenderer renderer1 = new StandardXYItemRenderer();

		renderer1.setDrawSeriesLineAsPath( false );
		final ValueAxis priceAxis = new NumberAxis("ISK");

		currentDataSets.pricePlot = new XYPlot( 
				currentDataSets.prices,
				null , 
				priceAxis , 
				renderer1 );

		currentDataSets.pricePlot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);


		/*
		 * Buy/sell volume plot.
		 */

		final StandardXYItemRenderer renderer2 = // XYBarRenderer
			new StandardXYItemRenderer();

		final ValueAxis volumeAxis = 
			new NumberAxis("Units");

		TimeSeriesCollection volumes =
			new TimeSeriesCollection();

		volumes.addSeries( currentDataSets.buyVolume );
		volumes.addSeries( currentDataSets.sellVolume );

		currentDataSets.volumePlot = new XYPlot( 
				volumes,
				null , 
				volumeAxis , 
				renderer2 );

		currentDataSets.volumePlot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

		/*
		 * Combined plot.
		 */

		final ValueAxis dateAxis = new DateAxis("Date");

		final CombinedDomainXYPlot plot = new CombinedDomainXYPlot( dateAxis );
		plot.setGap(10.0);
		plot.add( currentDataSets.pricePlot , 2 );
		plot.add( currentDataSets.volumePlot , 1 );

		plot.setOrientation(PlotOrientation.VERTICAL);

		/*
		 * Create chart.
		 */


		chart =
			new JFreeChart(
					item.getName(),
					JFreeChart.DEFAULT_TITLE_FONT, 
					plot, 
					true
			);

		chart.setBackgroundPaint(Color.white);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

		final XYItemRenderer r = plot.getRenderer();
		if ( r instanceof XYLineAndShapeRenderer ) {
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setBaseShapesVisible(true);
			renderer.setBaseShapesFilled(true);
		}

		final DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

		changeDateRange( (DisplayRange) this.dateRangeChooser.getSelectedItem() );

		// display chart
		ChartPanel chartPanel = new ChartPanel(chart);
		//		chartPanel.setMouseZoomable(true, false);
		return chartPanel;
	}
}
