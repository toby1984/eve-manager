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
package de.codesourcery.eve.skills.ui.utils;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.dao.IRegionDAO;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.DefaultComboBoxModel;

public class RegionSelectionDialog extends JDialog {

	private final JComboBox regions = new JComboBox();
	
	private final JButton okButton = new JButton("OK");
	
	private static final class MyRenderer extends DefaultListCellRenderer {
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) 
		{
			
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			
			setText( ((Region) value).getName() );
			return this;
		}
	}
	
	private interface IRegionProvider 
	{
		public Region getRegion(long id);
		public List<Region> fetchAll();
	}
	
	public static IRegionQueryCallback createCallback(Frame frame , final IRegionDAO dao) {
		return createCallback( null , createRegionProvider( dao ) );
	}
	
	public static IRegionQueryCallback createCallback(final IRegionDAO dao) {
		return createCallback( null , createRegionProvider( dao ) );
	}
	
	public static IRegionQueryCallback createCallback(Frame parent , final IRegionProvider dao) 
	{
		return new IRegionQueryCallback() {

			@Override
			public Region getRegion(String message) {
				
				final RegionSelectionDialog dialog = new RegionSelectionDialog(message,dao);
				
				dialog.pack();
				dialog.setLocationRelativeTo( null );
				dialog.setVisible( true );
				return dialog.getSelectedRegion();
			}

			@Override
			public Region getRegionById(long id) {
				return dao.getRegion( id );
			}
		};
	}
	
	public static IRegionProvider createRegionProvider(final IRegionDAO model)
	{
		return new IRegionProvider() 
		{
			@Override
			public List<Region> fetchAll()
			{
				return model.fetchAll();
			}

			@Override
			public Region getRegion(long id)
			{
				return model.fetch(id);
			} };
	}
	
	private static IRegionProvider createRegionProvider(final IStaticDataModel model) 
	{
		return new IRegionProvider() {

			@Override
			public List<Region> fetchAll()
			{
				return model.getAllRegions();
			}

			@Override
			public Region getRegion(long id)
			{
				return model.getRegion( id );
			} };
	}
	
	public static IRegionQueryCallback createCallback(Frame parent , final IStaticDataModel dataModel) {
		return createCallback( parent , createRegionProvider( dataModel ) );
	}
	
	public RegionSelectionDialog(String message, final IRegionProvider regionDAO) {
		this( null , message , regionDAO );
	}
	
	public RegionSelectionDialog(Frame parent , String message, IRegionProvider regionDAO) {
	
		super( parent ,"Please select a region", true );
		
		if ( regionDAO == null ) {
			throw new IllegalArgumentException("regionDAO cannot be NULL");
		}
		
		final List<Region> allRegions =
			regionDAO.fetchAll();
		
		// sort regions ascending by name
		Collections.sort( allRegions , new Comparator<Region>() {

			@Override
			public int compare(Region o1, Region o2) {
				return o1.getName().compareTo( o2.getName() );
			}
		});
		
		final DefaultComboBoxModel<Region> model = 
			new DefaultComboBoxModel<Region>( allRegions );
		
		regions.setModel( model );
		if ( ! allRegions.isEmpty() ) {
			regions.setSelectedItem( allRegions.get(0) );
		}
		regions.setRenderer( new MyRenderer() );
		
		// setup UI
		
		
		JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );
		
		// add text area
		final JTextArea area = new JTextArea();
		area.setText( message );
		area.setWrapStyleWord(true);
		area.setLineWrap(true);
		area.setEditable( false );
		
		panel.add( area , constraints().x(0).y(0).width(2 ).end() );
		
		// add combobox + button
		
		panel.add( regions , constraints().x(0).y(1).width(1).resizeHorizontally().end() );
		
		okButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		} );
		
		panel.add( okButton , constraints().x(1).y(1).width(1).noResizing().end() );
		
		// add panel to content pane
		getContentPane().setLayout( new GridBagLayout() );
		getContentPane().add( panel , constraints().resizeBoth().useRemainingSpace().end() );
		
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosed(WindowEvent e) {
				dispose();
			}
		} );
		
		pack();
	}
	
	protected ConstraintsBuilder constraints() {
		return new ConstraintsBuilder();
	}
	
	public Region getSelectedRegion() {
		return (Region) regions.getSelectedItem();
	}
	
	public static void main(String[] args) {
		
		final Map<Long,Region> regions =
			new HashMap<Long,Region>();
		
		Region r1 = new Region();
		r1.setID( new Long(1) );
		r1.setName( "Region #1");
		regions.put( r1.getID() , r1 );
		
		Region r2 = new Region();
		r2.setID( new Long(2) );
		r2.setName( "Region #2");
		regions.put( r2.getID() , r2 );
		
		Region r3 = new Region();
		r3.setID( new Long(3) );
		r3.setName( "Region #3");
		regions.put( r3.getID() , r3 );
		
		final IRegionDAO dao = 
			new IRegionDAO() {

				@Override
				public Region fetch(Long id) {
					return regions.get( id );
				}

				@Override
				public List<Region> fetchAll() {
					return new ArrayList<Region>( regions.values() );
				}
		};
		
		final RegionSelectionDialog dialog = 
			new RegionSelectionDialog("Just a sample message" , createRegionProvider( dao ) );
		dialog.setVisible( true );
	}
}
