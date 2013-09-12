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
package de.codesourcery.eve.skills.ui.components.impl.planning;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.NPCCorpStandings;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.AssemblyLine;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.production.RefiningCalculator;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.renderer.ActivityComboBoxRenderer;
import de.codesourcery.eve.skills.ui.renderer.RegionComboBoxRenderer;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.Cell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.HorizontalGroup;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.VerticalGroup;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class AssemblyLineChooser extends AbstractEditorComponent
{

	private static final Logger log = Logger
	.getLogger(AssemblyLineChooser.class);

	private static final DecimalFormat STANDING_FORMAT =
		new DecimalFormat("#0.00");
	
	private static final DecimalFormat PERCENT_FORMAT =
		new DecimalFormat("##0.00");
	
	private static final DecimalFormat PERCENT_FORMAT2 =
		new DecimalFormat("##0.##");
	
	private final NPCCorpStandings characterStandings;

	private final JComboBox activity = new JComboBox(new DefaultComboBoxModel<Activity>( getFilteredActivities() ) );
	private final JComboBox region = new JComboBox();

	private final JTable stations = new JTable();
	private final TableModel stationModel = new TableModel();

	private final JTextArea details = new JTextArea();

	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;
	
	private Activity fixedActivity;
	
	private final ItemListener listener = new ItemListener() {

		@Override
		public void itemStateChanged(ItemEvent e)
		{
			if ( e.getSource() == activity ) {
				activityChanged( (Activity) e.getItem() );
			} else if ( e.getSource() == region ) {
				regionChanged( (Region) e.getItem() );
			} 
		}
	};

	private final class StandingsAwareRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);

			if ( column == 0 ) {
				setHorizontalAlignment( JLabel.LEADING );
			} else {
				setHorizontalAlignment( JLabel.TRAILING );
			}
			
			if ( isSelected ) { 
				setBackground( table.getSelectionBackground() );
				return this;
			}
			
			setBackground( table.getBackground() );
			
			final int modelRow =
				table.convertRowIndexToModel( row );
			
			final StationWithStanding stationWithStanding =
				stationModel.getRow( modelRow );

			if ( stationWithStanding.standing == null ) {
				return this;
			}
			
			final float standing = stationWithStanding.standing.getValue();
			
			if ( standing > 0.0 ) {
				setBackground( Color.GREEN );
			} else if ( standing < 0.0 ) {
				setBackground( Color.RED );
			} 
			return this;
		}
	}
	
	private static final class StationWithStanding 
	{
		public final Station station;
		public final Standing<NPCCorporation> standing;
		
		public StationWithStanding(Station station, Standing<NPCCorporation> standing) {
			this.standing = standing;
			this.station = station;
		}
		
		public String getStandingAsString() {
			if ( standing == null ) {
				return "";
			}
			return STANDING_FORMAT.format( standing.getValue() );
		}
	}

	private final class TableModel extends AbstractTableModel<StationWithStanding> {

		private List<StationWithStanding> stations =
			new ArrayList<StationWithStanding>();

		public TableModel() {
			super(
					new TableColumnBuilder()
					.add("Name")
					.add("Security level")
					.add("Standing")
					.add("Refining efficiency")
					.add("Owner")
			);
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex)
		{
			final StationWithStanding s = getRow(modelRowIndex);

			if ( modelColumnIndex == 0 ) {
				return s.station.getDisplayName();
			} else if ( modelColumnIndex == 1 ) {
				return Double.toString( s.station.getSolarSystem().getRoundedSecurity() );
			} else if ( modelColumnIndex == 2 ) {
				return s.getStandingAsString();
			} else if ( modelColumnIndex == 3 ) {
				return PERCENT_FORMAT2.format( s.station.getReprocessingEfficiency()*100.0d)+" %";
			} else if ( modelColumnIndex == 4 ) {
				return s.station.getOwner() != null ? s.station.getOwner().getName() : "<no owner?>";
			} else {
				throw new IllegalArgumentException("Invalid column index "+modelColumnIndex);
			}
		}

		@Override
		public StationWithStanding getRow(int modelRow)
		{
			if ( modelRow < 0 || modelRow >= stations.size() ) {
				throw new IllegalArgumentException("Invalid model row "+modelRow);
			}
			return stations.get( modelRow );
		}

		@Override
		public int getRowCount()
		{
			return stations.size();
		}

		protected void refresh()
		{
			if ( getSelectedActivity() != null && getSelectedRegion() != null ) {
				
				final List<StationWithStanding> tmp =
					new ArrayList<StationWithStanding>();
				
				final List<Station> stations = 
					dataModel.getStationsFor(getSelectedRegion(), getSelectedActivity() );
				
				System.out.println("Go "+stations.size()+" stations for activity "+getSelectedActivity() );
				
				for ( Station s  : stations ) {
					tmp.add( new StationWithStanding( s , characterStandings.getNPCCorpStanding( s.getOwner() ) 
							)
					);
				}
				this.stations = tmp;
			} else {
				this.stations.clear();
			}
			modelDataChanged();
		}

		public void clear()
		{
			stations.clear();
			modelDataChanged();			
		}

	}

	public AssemblyLineChooser(NPCCorpStandings standings) {
		if ( standings == null ) {
			throw new IllegalArgumentException("character standings cannot be NULL");
		}
		this.characterStandings = standings;
	}

	protected void solarSystemChanged(SolarSystem item)
	{
		stationModel.refresh();
	}

	protected void activityChanged(Activity item)
	{
		stationModel.refresh();
	}

	protected void regionChanged(Region item)
	{
		stationModel.refresh();
	}

	@Override
	protected JButton createCancelButton()
	{
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton()
	{
		return new JButton("OK");
	}

	private static List<Activity> getFilteredActivities() {
		final List<Activity> result =
			new ArrayList<Activity>();

		for ( Activity a : Activity.values() ) {
			if ( ( ! a.isPublished() && a != Activity.REFINING ) || a == Activity.NONE 
					|| a == Activity.REVERSE_ENGINEERING ) {
				continue;
			}
			result.add( a );
		}
		return result;
	}

	@Override
	protected JPanel createPanelHook()
	{
		
		details.setEditable( false );
		
		// activity combobox
		activity.setRenderer( new ActivityComboBoxRenderer() );
		activity.setPreferredSize( new Dimension( 150 , 20 ) );
		if ( fixedActivity != null ) {
			activity.setSelectedItem( fixedActivity );
		}
		
		// region combobox
		final List<Region> allRegions =
			dataModel.getAllRegions();
		
		Collections.sort( allRegions , Region.BY_NAME_COMPARATOR );
		
		log.debug("createPanel(): Got "+allRegions.size()+" regions.");

		region.setRenderer( new RegionComboBoxRenderer() );
		region.setPreferredSize( new Dimension( 150 , 20 ) );
		region.setModel( new DefaultComboBoxModel<Region>( allRegions ) );
		region.setSelectedItem( allRegions.get(0) );

		// stations
		stations.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		stations.setModel( stationModel );
		stations.setFillsViewportHeight( true );
		stations.setDefaultRenderer( String.class , new StandingsAwareRenderer() );
		stations.setRowSorter(  stationModel.getRowSorter() );
		stations.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if ( ! e.getValueIsAdjusting() ) {
					final int row = stations.convertRowIndexToModel( stations.getSelectedRow() );
					if ( row >= 0 && row < stationModel.getRowCount() ) {
						stationSelectionChanged( stationModel.getRow( row ) );
					} 
				}
			}
		} );

		// register listeners
		activity.addItemListener( listener );
		region.addItemListener( listener );

		// details
//		details.setRows( 5 );
//		details.setColumns( 25 );

		final JPanel result = new JPanel();

		final JSplitPane splitPane =
			new ImprovedSplitPane(JSplitPane.VERTICAL_SPLIT , new JScrollPane( stations ) , new JScrollPane( details) );
		
		splitPane.setDividerLocation( 0.70d );
		
		new GridLayoutBuilder().add( 
				new VerticalGroup(
						new HorizontalGroup( new Cell( new JLabel("Region") ) , new Cell( new JLabel("Activity") ) ),
						new HorizontalGroup( new Cell( region ) , new Cell( activity )  ),
						new HorizontalGroup( new Cell( splitPane ) )
				)
		).addTo( result );

		return result;
	}

	public Station getSelectedStation() 
	{
		final AssemblyLine line = getSelectedAssemblyLine();
		if ( line != null ) {
			return line.getStation();
		}
		
		final int selectedRow = stations.getSelectedRow();
		if ( selectedRow == -1 ) {
			return null;
		}
		
		final int row = stations.convertRowIndexToModel( selectedRow );
		if ( row >= 0 && row < stationModel.getRowCount() ) {
			return stationModel.getRow( row ).station;
		}  
		
		return null;
	}
	
	public AssemblyLine getSelectedAssemblyLine() {
		
		final int selectedRow = stations.getSelectedRow();
		if ( selectedRow == -1 ) {
			return null;
		}
		
		final StationWithStanding sWithS;
		final int row = stations.convertRowIndexToModel( selectedRow );
		if ( row >= 0 && row < stationModel.getRowCount() ) {
			sWithS = stationModel.getRow( row );
		}  else {
			return null;
		}
		
		final List<AssemblyLine> lines = 
			dataModel.getAssemblyLines( sWithS.station , getSelectedActivity() );
		
		return lines.isEmpty() ? null : lines.get(0);
	}

	protected void stationSelectionChanged(StationWithStanding stationWithStanding)
	{

		if ( stationWithStanding == null ) {
			details.setText("Select a station.");
		} else 
		{
			final List<AssemblyLine> lines = 
				dataModel.getAssemblyLines( stationWithStanding.station , getSelectedActivity() );

			final StringBuffer buffer = new StringBuffer();

			if ( lines.isEmpty() ) {
				if ( getSelectedActivity() != Activity.REFINING ) { // does not require assembly lines
					buffer.append("Found no assembly lines for this station ?");
				} else {
					
					buffer.append("Selected station: "+stationWithStanding.station.getName() ).append("\n\n");
					
					final double percent;
					final Standing<NPCCorporation> standing = stationWithStanding.standing;
					if ( standing != null ) {
						percent = 100.0d* RefiningCalculator.calculateStationTax( standing.getValue() );
					} else {
						percent = 5.0d;
					}
					buffer.append("Refining tax: "+PERCENT_FORMAT.format( percent ) +" %");
				}
			} else {
				final AssemblyLine line = lines.get(0);
				
				final NPCCorporation owningCorp = line.getOwner();
				final Standing<NPCCorporation> standing =
					this.characterStandings.getNPCCorpStanding( owningCorp );
				
				final double minStanding = line.getMinimumStanding();
				
				if( standing != null && standing.getValue() < minStanding ) {
					// TODO: Need to apply social skills here ???
					buffer.append("\n*** You do not meet the standing requirements to use this station ***\n\n" );
				}

				buffer.append("Selected station: "+stationWithStanding.station.getName() ).append("\n\n");

				final double discountPercent = line.getDiscountPercent( standing );
				final double factor = 1 - ( discountPercent / 100.0d);
				
				buffer.append( "Standing discount percentage: "+
						PERCENT_FORMAT.format( discountPercent )+" %\n\n" );
				
				buffer.append( "Installation costs: ")
					.append( format( line.getInstallationCost() ) )
					.append( " ( you: "+toString( line.getInstallationCost().multiplyBy( factor ) ) )
					.append(" )\n");
				
				buffer.append( "Hourly costs: ")
				.append( format( line.getCostPerHour() ) )
				.append( " ( you: "+toString( line.getCostPerHour().multiplyBy( factor ) ) )
				.append(" )\n\n");				
				

				buffer.append( "Minimum required standing: "+STANDING_FORMAT.format( minStanding ) ).append("\n");
				
				if ( standing != null ) {
					buffer.append( "\nYour standing towards "+owningCorp.getName()+
							": "+STANDING_FORMAT.format( standing.getValue() ) );
				} else {
					buffer.append( "\nYou don't have any standings for "+owningCorp.getName());
				}

			}
			details.setText( buffer.toString() );
			details.setCaretPosition(0);
		}
	}
	
	protected static final String format(ISKAmount amount) {
		return AmountHelper.formatISKAmount( amount )+" ISK";
	}

	public Region getSelectedRegion() {
		return (Region) region.getSelectedItem();
	}

	public Activity getSelectedActivity() {
		if ( fixedActivity != null ) {
			return fixedActivity;
		}
		return (Activity) activity.getSelectedItem();
	}
	
	public void setFixedActivity( Activity activity ) {
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		this.fixedActivity = activity;
		this.activity.setEnabled( false );
		this.activity.setSelectedItem( activity );
	}

	@Override
	protected boolean hasValidInput()
	{
		return true;
	}

}
