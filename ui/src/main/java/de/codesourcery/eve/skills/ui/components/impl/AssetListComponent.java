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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.assets.IAssetManager;
import de.codesourcery.eve.skills.datamodel.Asset;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.ILocation;
import de.codesourcery.eve.skills.db.datamodel.InventoryCategory;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.components.impl.TotalItemVolumeComponent.IDataProvider;
import de.codesourcery.eve.skills.ui.model.AbstractViewFilter;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;
import de.codesourcery.eve.skills.ui.model.IViewFilter;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.ui.model.impl.AssetListTableModel;
import de.codesourcery.eve.skills.ui.renderer.FixedBooleanTableCellRenderer;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.Misc;
import de.codesourcery.eve.skills.ui.utils.PlainTextTransferable;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.UITask;

public class AssetListComponent extends AbstractComponent implements ICharacterSelectionProviderAware
{
	private static final Logger log = 
		Logger.getLogger(AssetListComponent.class);
	
	private static final Collection<Asset> NO_ASSETS =
		Collections.emptyList();

	private static final TypeWrapper ANY_TYPE = new TypeWrapper() {
		@Override
		public String toString()
		{
			return "ANY TYPE";
		}
	};

	private final PopupMenuBuilder popupMenuBuilder =
		new PopupMenuBuilder();

	private static class TypeWrapper {

		private InventoryType type;

		public TypeWrapper() {
		}

		public TypeWrapper(InventoryType type) {
			if ( type == null ) {
				throw new IllegalArgumentException("type cannot be NULL");
			}
			this.type = type;
		}

		@Override
		public String toString()
		{
			return type.getName();
		}
	}

	// swing
	private JTable table;

	private JCheckBox ignorePackaging = new JCheckBox();
	private JCheckBox ignoreLocations = new JCheckBox();
	private JCheckBox mergeAssetsByType = new JCheckBox();

	// filtering by name
	private final JTextField nameFilter = new JTextField();

	// location filtering
	private final JCheckBox filterByLocation = new JCheckBox("Filter by location?");
	private final JComboBox locationComboBox = new JComboBox();

	// category filter
	private final JCheckBox filterByCategory = new JCheckBox("Filter by category?");
	private final JComboBox categoryComboBox = new JComboBox();

	private final DefaultComboBoxModel<InventoryCategory> categoryModel = 
		new DefaultComboBoxModel<InventoryCategory>();	

	private final TotalItemVolumeComponent<Asset> selectedVolume =
		new TotalItemVolumeComponent<Asset>( new IDataProvider<Asset>() {

			@Override
			public int getQuantity(Asset obj)
			{
				return obj.getQuantity();
			}

			@Override
			public double getVolumePerUnit(Asset obj)
			{
				return obj.getType().getVolume();
			}} );
	
	// group filter
	private final JCheckBox filterByGroup = new JCheckBox("Filter by group?");
	private final JComboBox groupComboBox = new JComboBox();	
	private final DefaultComboBoxModel<InventoryGroup> groupModel = 
		new DefaultComboBoxModel<InventoryGroup>();


	private final DefaultComboBoxModel<ILocation> locationModel = 
		new DefaultComboBoxModel<ILocation>();

	private final JCheckBox filterByType = new JCheckBox("Filter by item type?");
	private final JComboBox typeComboBox = new JComboBox();
	private final DefaultComboBoxModel<TypeWrapper> typeModel = 
		new DefaultComboBoxModel<TypeWrapper>();

	private ActionListener actionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e)
		{
			AssetListComponent.this.actionPerformed(e);
		}
	};

	// other stuff
	@Resource(name = "asset-manager")
	private IAssetManager assetManager;

	private ISelectionProvider<ICharacter> selectionProvider;
	private final AssetListModel model = new AssetListModel( new AssetList() );

	private final ISelectionListener<ICharacter> characterSelectionListener = new ISelectionListener<ICharacter>() {

		@Override
		public void selectionChanged(ICharacter selected)
		{
			updateAssetListModel();
		}
	};
	
	private IViewFilter<Asset> viewFilter = new AbstractViewFilter<Asset>() {

		@Override
		public boolean isHiddenUnfiltered(Asset asset)
		{

			if ( ! StringUtils.isBlank( nameFilter.getText() ) ) {
				if ( ! asset.getType().getName().toLowerCase().contains(( nameFilter.getText().toLowerCase() ) ) ) {
					return true;
				}
			}

			if ( filterByLocation.isSelected() ) {

				final ILocation selected = (ILocation) locationComboBox
				.getSelectedItem();

				if ( selected != null && ! matchesLocation(selected, asset ) ) {
					return true;
				}
			}

			if ( filterByType.isSelected() ) {

				final TypeWrapper selected = 
					(TypeWrapper) typeComboBox.getSelectedItem();

				if ( selected != null && selected != ANY_TYPE &&
						! matchesInventoryType( selected , asset ) ) 
				{
					return true;
				}
			}

			if ( filterByCategory.isSelected() ) 
			{
				final InventoryCategory selected =
					(InventoryCategory) categoryComboBox.getSelectedItem();
				if ( selected != null && ! matchesInventoryCategory( selected , asset) ) {
					return true;
				}
			}

			if ( filterByGroup.isSelected() ) {
				final InventoryGroup selected =
					(InventoryGroup) groupComboBox.getSelectedItem();
				if ( selected != null && ! matchesInventoryGroup( selected , asset) ) {
					return true;
				}
			}

			return false;
		}

		protected boolean matchesInventoryGroup(InventoryGroup group, Asset asset)
		{
			return asset.getType().getGroup().getId().equals( group.getId() );
		}	

		protected boolean matchesInventoryCategory(InventoryCategory cat, Asset asset)
		{
			return asset.getType().getGroup().getCategory().getId().equals( cat.getId() );
		}		

		protected boolean matchesInventoryType(TypeWrapper required, Asset asset)
		{
			if ( required == ANY_TYPE ) {
				return false;
			}

			return required.type.equals( asset.getType() );
		}

		protected boolean matchesLocation(ILocation required, Asset asset)
		{
			if ( required.isAnyLocation() ) {
				return true;
			}

			return asset.hasLocation( required );
		}
	};

	private void actionPerformed(ActionEvent e)
	{
		final Object src = e.getSource();
		if ( src == ignorePackaging || src == ignoreLocations || src == mergeAssetsByType) 
		{
			updateAssetListModel();
		}
		else if ( src == filterByLocation || src == locationComboBox ) 
		{
			model.viewFilterChanged();
		}
		else if ( src == filterByType || src == typeComboBox ) 
		{
			model.viewFilterChanged();
			if ( src == filterByType ) {
				updateComboBoxModels( model.getAssetList() );
			}
		}
		else if ( src == filterByCategory || src == categoryComboBox ) 
		{
			model.viewFilterChanged();
			if ( src == filterByCategory ) {
				updateComboBoxModels( model.getAssetList() );
			}
		}
		else if ( src == filterByGroup    || src == groupComboBox ) 
		{
			model.viewFilterChanged();
			if ( src == filterByGroup ) {
				updateComboBoxModels( model.getAssetList() );
			}
		}
	}
	
	public AssetListComponent() {
		super();
		registerChildren( this.selectedVolume );
	}

	private final class AssetListModel extends AssetListTableModel
	{

		public AssetListModel(AssetList assetList) {
			super(assetList, 
					new TableColumnBuilder()
			.add("Item type")
			.add("Category")
			.add("Group")
			.add("Location")
			.add("Quantity",Integer.class)
			.add("Packaged ?", Boolean.class)
			.add("Stored in", String.class));
		}
		
		@Override
		protected Object getColumnValue(Asset asset, int columnIndex)
		{
			switch ( columnIndex )
			{
				case 0:
					final InventoryType type = asset.getType();
					return type != null ? asset.getType().getName() : "<no type>";
				case 1:
					return asset.getType().getGroup().getCategory()
					.getCategoryName();
				case 2:
					return asset.getType().getGroup().getGroupName();
				case 3:
					return locationToString( asset );
				case 4:
					return asset.getQuantity();
				case 5:
					return asset.isPackaged();
				case 6:
					if ( asset.getContainer() != null ) {
						return asset.getContainer().getType().getName();
					}
					return "";
				default:
					throw new IllegalArgumentException(
							"Unhandled column index: " + columnIndex);
			}
		}

	}

	protected static final String locationToString(Asset asset)
	{

		if ( asset.hasMultipleLocations() ) {
			return "<multiple locations>";
		}

		final ILocation loc = asset.getLocation();
		return loc == null ? "<no location>" : loc.getDisplayName();
	}

	@Override
	protected void onAttachHook(IComponentCallback callback)
	{

		this.selectionProvider.addSelectionListener(characterSelectionListener);

		updateAssetListModel();
	}

	@Override
	protected void onDetachHook()
	{
		if ( this.selectionProvider != null ) {
			this.selectionProvider
			.removeSelectionListener(characterSelectionListener);
		}
	}

	@Override
	protected void disposeHook()
	{

		table = null;
		model.dispose();
		if ( this.selectionProvider != null ) {
			this.selectionProvider
			.removeSelectionListener(characterSelectionListener);
		}
	}

	@Override
	protected JPanel createPanel()
	{

		// Merge controls.
		final JPanel mergeControlsPanel = new JPanel();
		mergeControlsPanel.setLayout(new GridBagLayout());
		mergeControlsPanel.setBorder( BorderFactory.createTitledBorder("Merging" ) );

		int y = 0;

		// merge by type
		mergeAssetsByType.setSelected(true);
		mergeAssetsByType.addActionListener( actionListener );

		mergeControlsPanel.add(mergeAssetsByType, constraints(0,y).anchorWest().end());
		mergeControlsPanel.add(new JLabel("Merge assets by type",SwingConstants.LEFT), 
				constraints(1,y++).width(2).end());

		// "ignore different packaging"
		ignorePackaging.setSelected(true);
		ignorePackaging.addActionListener(actionListener);

		mergeControlsPanel.add(new JLabel(""), constraints(0,y).anchorWest().end());
		mergeControlsPanel.add(ignorePackaging, constraints(1,y).anchorWest().end());
		final JLabel label1 = new JLabel("Merge different packaging",SwingConstants.RIGHT);
		mergeControlsPanel.add(label1, 
				constraints(2,y++).end());

		// "ignore different locations"
		ignoreLocations.setSelected(true);
		ignoreLocations.addActionListener(actionListener);

		mergeControlsPanel.add(new JLabel(""), constraints(0,y).anchorWest().end());
		mergeControlsPanel.add(ignoreLocations, constraints(1,y).anchorWest().end());
		final JLabel label2 = new JLabel("Merge different locations",SwingConstants.RIGHT);
		mergeControlsPanel.add(label2,
				constraints(2,y++).end());

		linkComponentEnabledStates( mergeAssetsByType , 
				ignoreLocations , 
				ignorePackaging ,
				label1,label2);

		/*
		 * Filter controls.
		 */

		final JPanel filterControlsPanel = new JPanel();
		filterControlsPanel.setLayout(new GridBagLayout());
		filterControlsPanel.setBorder( BorderFactory.createTitledBorder("Filters" ) );

		y=0;
		// filter by location combo box
		filterByLocation.addActionListener( actionListener );
		locationComboBox.addActionListener( actionListener );

		filterByLocation.setSelected( false );
		linkComponentEnabledStates(filterByLocation,locationComboBox);

		locationComboBox.setRenderer( new LocationRenderer() );
		locationComboBox.setPreferredSize( new Dimension(150,20) );
		locationComboBox.setModel( locationModel );

		filterControlsPanel.add( filterByLocation, constraints(0,y).end() );
		filterControlsPanel.add( locationComboBox , constraints(1,y++).end() );

		// filter by type combo box
		filterByType.addActionListener( actionListener );
		typeComboBox.addActionListener( actionListener );

		filterByType.setSelected(false);

		linkComponentEnabledStates( filterByType , typeComboBox );

		typeComboBox.setPreferredSize( new Dimension(150,20) );
		typeComboBox.setModel( typeModel );

		filterControlsPanel.add( filterByType, constraints(0,y).end() );
		filterControlsPanel.add( typeComboBox , constraints(1,y++).end() );			

		// filter by item category combobox
		filterByCategory.addActionListener( actionListener );
		categoryComboBox.addActionListener( actionListener );
		categoryComboBox.setRenderer( new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				
				super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				
				setText( getDisplayName( (InventoryCategory) value) );
				setEnabled( categoryComboBox.isEnabled() );
				return this;
			}
		} );

		filterByCategory.setSelected(false);
		linkComponentEnabledStates( filterByCategory , categoryComboBox );

		categoryComboBox.setPreferredSize( new Dimension(150,20) );
		categoryComboBox.setModel( categoryModel );

		filterControlsPanel.add( filterByCategory, constraints(0,y).end() );
		filterControlsPanel.add( categoryComboBox , constraints(1,y++).end() );	

		// filter by item group combobox
		filterByGroup.addActionListener( actionListener );
		groupComboBox.addActionListener( actionListener );

		filterByGroup.setSelected(false);

		linkComponentEnabledStates( filterByGroup, groupComboBox );

		groupComboBox.setPreferredSize( new Dimension(150,20) );
		groupComboBox.setModel( groupModel );

		filterControlsPanel.add( filterByGroup , constraints(0,y).end() );
		filterControlsPanel.add( groupComboBox , constraints(1,y++).end() );			

		/*
		 * Table panel.
		 */

		table = new JTable() {

			@Override
			public TableCellRenderer getCellRenderer(int row, int column)
			{

				// subclassing hack is needed because table
				// returns different renderes depending on column type
				final TableCellRenderer result = super.getCellRenderer(row, column);

				return new TableCellRenderer() {

					@Override
					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column)
					{

						final Component comp = result.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

						final int modelRow = table.convertRowIndexToModel( row );
						final Asset asset = model.getRow( modelRow);

						final StringBuilder label = new StringBuilder("<HTML><BODY>");
						label.append( asset.getItemId() + " - flags: " + asset.getFlags()+"<BR>" );
						if ( asset.hasMultipleLocations() ) {
							label.append( "<BR>" );
							for ( ILocation loc : asset.getLocations() ) {
								label.append( loc.getDisplayName() ).append("<BR>");
							}
						}

						label.append("</BODY></HTML>");
						((JComponent) comp).setToolTipText( label.toString() );

						return comp;
					}};
			}
		};


		model.setViewFilter( this.viewFilter );

		table.getSelectionModel().addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				updateSelectedVolume();
			}
		} );
		
		FixedBooleanTableCellRenderer.attach( table );
		table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		table.setModel(model);
		table.setBorder( BorderFactory.createLineBorder(Color.BLACK ) );
		table.setRowSorter(model.getRowSorter());
		
		popupMenuBuilder.addItem("Refine..." , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				final List<Asset> assets = getSelectedAssets();
				if ( assets == null || assets.isEmpty() ) {
					return;
				}

				final ICharacter c = selectionProvider.getSelectedItem();
				final RefiningComponent comp = new RefiningComponent( c );
				comp.setItemsToRefine( assets );
				ComponentWrapper.wrapComponent( "Refining", comp ).setVisible( true );
			}

			@Override
			public boolean isEnabled()
			{
				return table.getSelectedRow() != -1;
			}
		});

		popupMenuBuilder.addItem("Copy selection to clipboard (text)" , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				final List<Asset> assets = getSelectedAssets();
				if ( assets == null || assets.isEmpty() ) {
					return;
				}

				new PlainTextTransferable( toPlainText( assets ) ).putOnClipboard();
			}

			@Override
			public boolean isEnabled()
			{
				return table.getSelectedRow() != -1;
			}
		});

		popupMenuBuilder.addItem("Copy selection to clipboard (CSV)" , new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				final List<Asset> assets = getSelectedAssets();
				if ( assets == null || assets.isEmpty() ) {
					return;
				}

				final Clipboard clipboard = 
					Toolkit.getDefaultToolkit().getSystemClipboard(); 

				clipboard.setContents( new PlainTextTransferable( toCsv( assets ) ) , null );
			}

			@Override
			public boolean isEnabled()
			{
				return table.getSelectedRow() != -1;
			}
		});		

		table.getSelectionModel().setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		this.popupMenuBuilder.attach( table );

		final JScrollPane scrollPane = new JScrollPane(table);

		/*
		 * Name filter
		 */

		final JPanel nameFilterPanel = 
			new JPanel();
		nameFilterPanel.setLayout( new GridBagLayout() );
		nameFilterPanel.setBorder( BorderFactory.createTitledBorder("Filter by name" ) );
		nameFilterPanel.setPreferredSize( new Dimension(150,70 ) );
		nameFilter.setColumns(10);
		nameFilter.getDocument().addDocumentListener( new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				model.viewFilterChanged();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				model.viewFilterChanged();				
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				model.viewFilterChanged();				
			}}
		);

		nameFilterPanel.add( nameFilter , constraints(0,0).resizeHorizontally().end() );
		final JButton clearButton = new JButton("Clear");
		clearButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				nameFilter.setText( null );
			}} );
		nameFilterPanel.add( clearButton , constraints(1,0).noResizing().end() );

		 // Selected volume
		final JPanel selectedVolumePanel =
			this.selectedVolume.getPanel();

		// add control panels to result panel
		final JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridBagLayout());

		topPanel.add(mergeControlsPanel, constraints(0,0).height(2).weightX(0).anchorWest().end());
		topPanel.add(filterControlsPanel, constraints(1,0).height(2).anchorWest().weightX(0).end());
		topPanel.add(nameFilterPanel, constraints(2,0).height(1).anchorWest().useRemainingWidth().end());
		topPanel.add(selectedVolumePanel, constraints(2,1).height(1).anchorWest().useRemainingWidth().end());

		final JSplitPane splitPane =
			new ImprovedSplitPane(JSplitPane.VERTICAL_SPLIT ,
					topPanel , 
					scrollPane );

		splitPane.setDividerLocation( 0.3d );

		final JPanel content = new JPanel();
		content.setLayout(new GridBagLayout());
		content.add( splitPane , constraints().resizeBoth().useRemainingSpace().end() );

		return content;
	}
	
	private void updateSelectedVolume() 
	{
		final int[] viewRows = table.getSelectedRows();
		if (  ArrayUtils.isEmpty(viewRows ) ) {
			selectedVolume.setItems( NO_ASSETS );
			return;
		}

		final List<Asset> selected = 
			new ArrayList<Asset>();
		
		for ( int viewRow : viewRows ) {
			final int modelRow =
				table.convertRowIndexToModel( viewRow );
			selected.add( model.getRow( modelRow ) );
		}
		selectedVolume.setItems( selected );
	}

	private List<Asset> getSelectedAssets()
	{
		final int[] selection = table.getSelectedRows();
		if ( ArrayUtils.isEmpty( selection ) ) {
			return null;
		}

		final List<Asset> assets = new ArrayList<Asset>();
		for ( int viewRow : selection ) {
			final int modelRow = model.convertViewRowToModel( viewRow );
			assets.add( model.getRow( modelRow ) ); 
		}
		return assets;
	}

	private String toCsv(List<Asset> assets)
	{
		// sort ascending by item name
		Collections.sort( assets , new Comparator<Asset>() {

			@Override
			public int compare(Asset arg0, Asset arg1)
			{
				return arg0.getType().getName().compareTo( arg1.getType().getName() );
			}
		} );

		final StringBuilder result = new StringBuilder();
		result.append( pad( "Item;Location;Packaged;Quantity" ) );
		for ( Asset a : assets ) {

			final String loc;
			if ( a.hasMultipleLocations() ) {
				loc = "multiple locations";
			} else {
				loc = a.getLocation().getDisplayName();
			}

			final String line =
				pad( a.getType().getName() , 45 )+";"+
				pad( loc , 20 )+";"+
				( a.isPackaged() ? "X" : " " ) +";"+
				pad( Integer.toString( a.getQuantity()) )+"\n";

			result.append( line );
		}

		return result.toString();		
	}

	private String pad(String s) {
		return StringUtils.rightPad( s , 12 );
	}

	private String pad(String s,int len) {
		return StringUtils.rightPad( s , len );
	}

	private String toPlainText(List<Asset> assets)
	{

		// sort ascending by item name
		Collections.sort( assets , new Comparator<Asset>() {

			@Override
			public int compare(Asset arg0, Asset arg1)
			{
				return arg0.getType().getName().compareTo( arg1.getType().getName() );
			}
		} );

		final StringBuilder result = new StringBuilder();
		result.append( createLine( "Item" ) );
		for ( Asset a : assets ) {
			result.append( createLine( a.getType().getName() ) );
		}

		return result.toString();
	}

	private String createLine(String col1 ) {
		return StringUtils.rightPad( col1 , 45 ) +"\n";
	}

	protected void updateAssetListModel() {

		final ICharacter character = this.selectionProvider.getSelectedItem();

		if ( character == null ) {
			log.debug("populateModel(): No character set");
			model.clear();
			return;
		}

		if ( assetManager == null ) {
			throw new IllegalStateException("No asset manager set ?");
		}

		log.info("createModel(): Fetching assets for character "
				+ character);


		final UITask updateTask = new UITask() {

			private AssetList assets;

			@Override
			public String getId()
			{
				return "fetch_assets_for_"+character.getName();
			}

			@Override
			public void run() throws Exception
			{

				displayStatus("Fetching assets for "+character.getName());

				/*
				 * TODO: THREADING BUG !!!! AssetManager  internally triggers lazy
				 * TODO: THREADING BUG !!!!  fetching and the current thread is NOT the EDT here....
				 */
				assets = assetManager.getAssets(character);

				log.info("createModel(): Got " + assets.size() + " assets.");

				if ( mergeAssetsByType.isSelected() ) {
					long delta = - System.currentTimeMillis();
					assets = assets.createMergedAssetListByType( 
							ignorePackaging.isSelected() ,
							ignoreLocations.isSelected() );
					delta += System.currentTimeMillis();
					log.info("createModel(): after merging = "+assets.size()+" [ "+delta+" millis");
				}
			}

			@Override
			public void successHook() throws Exception
			{
				updateAssetListModel( assets );
			}

			@Override
			public void failureHook(Throwable t) throws Exception
			{
				log.error("updateAssetListModel(): Caught ", t);
				displayError("Unable to retrieve asset list from server: "
						+ t.getMessage(), t);
				assets = new AssetList();
			}
		};
		
		// do stuff
		submitFutureTask(updateTask, true );
	}

	protected static <T> List<T> toList(Collection<T> data) {
		return new ArrayList<T>( data );
	}

	protected void updateAssetListModel(AssetList assets)
	{

		final int selectedRow = table.getSelectedRow();
		final Asset oldRowSelection = (selectedRow != -1 ) ?
				model.getRow( model.getRowSorter().convertRowIndexToModel( selectedRow ) ) : null;

				if ( log.isTraceEnabled() ) {
					log.trace("updateAssetListModel(): selected = "+oldRowSelection);
				}

				model.setAssetList(assets , ! this.mergeAssetsByType.isSelected() );

				updateComboBoxModels(assets);

				/*
				 * Try to restore old selection. 
				 */

				model.getRowSorter().setSortKeys( Arrays.asList( new SortKey(0,SortOrder.ASCENDING) ) );

				if ( oldRowSelection != null ) {
					// try to find same item
					int matchByItemId = -1;
					int matchByType = -1;
					for ( int i = 0 ; i < model.getRowCount() ; i++ ) {
						final Asset row = model.getRow( i);
						if ( row.getItemId() == oldRowSelection.getItemId() ) {
							matchByItemId = i;
							break;
						} 
						else if ( matchByType == -1l && row.getType().equals( oldRowSelection.getType() ) ) 
						{
							matchByType = i;
							break;
						}
					}

					int modelRow= matchByItemId;
					if ( modelRow == -1 ) {
						modelRow = matchByType;
					}
					if ( modelRow != -1 ) {
						int viewRow = model.convertModelRowToView( modelRow);
						if ( log.isTraceEnabled() ) {
							log.debug("updateAssetList(): Scrolling to view row "+viewRow);
						}

						Misc.scrollTableToRow( table , viewRow  );
						table.setRowSelectionInterval( viewRow , viewRow );
					} else {
						log.debug("updateAssetList(): Failed to restore selection");
					}
				}
	}

	private void updateComboBoxModels(AssetList assets)
	{
		// update location model

		final List<ILocation> locations = assets.getLocations();
		locations.add( 0 , ILocation.ANY_LOCATION );

		final ILocation selected =
			(ILocation) locationComboBox.getSelectedItem();

		this.locationModel.setData( locations );

		if ( selected != null && selected != ILocation.ANY_LOCATION ) {

			for ( ILocation newLoc : locations ) {
				if ( newLoc.getDisplayName().equals( selected.getDisplayName() ) ) {
					locationComboBox.setSelectedItem( newLoc );
				}
			}
		} 

		// update inventory type model
		final List<InventoryType> types = assets.getInventoryTypes();

		final List<TypeWrapper> wrappers =
			new ArrayList<TypeWrapper>();

		final Map<Long,InventoryCategory> categories =
			new HashMap<Long,InventoryCategory>();

		final Map<Long,InventoryGroup> groups=
			new HashMap<Long,InventoryGroup>();

		for ( InventoryType t : types ) {
			final InventoryGroup group = t.getGroup();
			final InventoryCategory cat = t.getGroup().getCategory();
			categories.put( cat.getId() , cat );
			if ( filterByCategory.isSelected() && 
					categoryModel.getSelectedItem() != null ) 
			{
				if ( group.getCategory() == categoryModel.getSelectedItem() ) {
					groups.put( group.getId() , group );
				}
			} else {
				groups.put( group.getId() , group );
			}
		}

		for ( InventoryType t : types ) {
			if ( filterByCategory.isSelected() &&
				 categoryModel.getSelectedItem() != null &&
				 t.getGroup().getCategory() != categoryModel.getSelectedItem() ) 
			{
				continue;
			}
			if ( filterByGroup.isSelected() &&
				 groupModel.getSelectedItem() != null &&
				 ! t.getGroup().getGroupID().equals( groupModel.getSelectedItem().getGroupID() ) ) 
			{
				continue;
			}
			wrappers.add( new TypeWrapper(t) );
		}
		wrappers.add( 0 , ANY_TYPE );

		// try restoring old selections
		final TypeWrapper oldSelection = 
			(TypeWrapper) this.typeComboBox.getSelectedItem();

		this.typeModel.setData( wrappers );

		if (oldSelection != null && oldSelection != ANY_TYPE ) {
			// try to restore selection 
			for ( TypeWrapper newWrapper : wrappers ) {
				if ( newWrapper.type.equals( oldSelection.type ) ) {
					this.typeComboBox.setSelectedItem( newWrapper );
					break;
				}
			}
		}

		// update inventory group model
		final InventoryGroup oldGroup =
			(InventoryGroup) groupComboBox.getSelectedItem();

		this.groupModel.setData( sortUsingLabelProvider(groups.values(), 
				new ILabelProvider<InventoryGroup>() {

			@Override
			public String getLabel(InventoryGroup obj)
			{
				return getDisplayName( obj );
			}} 
		));

		if ( oldGroup != null ) {
			// try to restore selection 
			for ( InventoryGroup newWrapper : groups.values() ) {
				if ( newWrapper.getGroupID().equals( oldGroup.getGroupID() ) ) {
					this.groupComboBox.setSelectedItem( newWrapper );
					break;
				}
			}
		}

		// update inventory category model
		final InventoryCategory oldCat =
			(InventoryCategory) categoryComboBox.getSelectedItem();

		this.categoryModel.setData( sortUsingLabelProvider( categories.values(), 
				new ILabelProvider<InventoryCategory>() {

			@Override
			public String getLabel(InventoryCategory obj)
			{
				return getDisplayName( obj );
			}
		})
		);

		if ( oldCat != null ) {
			// try to restore selection 
			for ( InventoryCategory newWrapper : categories.values() ) {
				if ( newWrapper.getId().equals( oldCat.getId() ) ) {
					this.categoryComboBox.setSelectedItem( newWrapper );
					break;
				}
			}
		}
	}

	protected static final String getDisplayName(InventoryCategory cat ) {
		if ( cat == null ) {
			return null;
		}
		return toPrettyName( cat.getCategoryName() );
	}

	protected static final String getDisplayName(InventoryGroup group) {
		if ( group == null ) {
			return null;
		}
		return toPrettyName( group.getGroupName() );
	}

	protected static final String toPrettyName(String name) {
		if ( StringUtils.isBlank( name ) ) {
			return name;
		}
		return name.substring(0,1).toUpperCase()+name.substring(1).toLowerCase().replaceAll("_"," ");
	}

	public void setSelectionProvider(
			ISelectionProvider<ICharacter> selectionProvider)
	{
		assertDetached();

		this.selectionProvider = selectionProvider;
	}

	private final class LocationRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);

			if ( value != null ) {
				setText( ((ILocation) value).getDisplayName() );
			}
			return this;
		}
	}

}
