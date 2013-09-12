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
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import de.codesourcery.eve.apiclient.IAPIClient;
import de.codesourcery.eve.apiclient.IAPIRequestObserver;
import de.codesourcery.eve.apiclient.datamodel.APIQuery;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;
import de.codesourcery.eve.skills.utils.EveDate;

public class APIRequestStatusComponent extends AbstractComponent {

	@Resource(name="api-client")
	private IAPIClient apiClient;

	private final JTable table = new JTable() {
		
		private final TableCellRenderer renderer =
			new CustomTableRenderer();
		
		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			return renderer;
		}
	};

	private final Observer observer = new Observer();
	
	private volatile int maxEntries = 10;

	private static final class APICallResult {
		
		private static final String STATUS_MSG_OK = "OK";
		private static final String STATUS_MSG_PENDING = "Pending";
		
		private long timestamp;
		private APIQuery query;
		private URI baseURI;
		private long duration;
		private String cachedUntil;
		private String status = STATUS_MSG_PENDING;
		
		private boolean isPending() {
			return STATUS_MSG_PENDING.equals( status );
		}
		
		public boolean isOK() {
			return STATUS_MSG_OK.equals( status );
		}
		
		public boolean isError() {
			return status != null && status.startsWith("ERROR");
		}
	}

	private final DataModel model =
		new DataModel();

	private final class IndexedData {
		// guarded-by: "this"
		private final Map<APIQuery, APICallResult> map =
			new HashMap<APIQuery , APICallResult>();
		
		private final List<APICallResult> list =
			new ArrayList<APICallResult>();
		
		public synchronized int addEntry(APICallResult result) {
			map.put( result.query, result );
			list.add( 0 , result );
			return 0;
		}
		
		public synchronized APICallResult get(APIQuery query) {
			return map.get( query ); 
		}
		
		public synchronized int getIndexFor(APICallResult result) {
			int i = 0;
			for ( APICallResult obj : list ) {
				if ( obj == result ) {
					return i;
				}
				i++;
			}
			return -1;
		}
		
		public synchronized APICallResult getRow(int row) {
			return list.get( row );
		}
		
		public synchronized int purge() {
			
			if ( list.size() >= maxEntries ) {
			
				final int removedIndex = list.size() -1;
				final APICallResult removed = 
					list.remove( list.size()-1 );
				
				map.values().remove( removed );
				return removedIndex;
			} 
			
			return -1;
		}

		public synchronized int size() {
			return list.size();
		}
	}
	
	private final class DataModel extends AbstractTableModel<APICallResult> {

		private final IndexedData data =
			new IndexedData();

		public DataModel() {
			super( new TableColumnBuilder()
				.add("Timestamp" )
				.add( "Base URI" )
				.add( "Query hash")
				.add("Cached until")
				.add("Duration")
				.add("Status")
			);
		}

		public APICallResult getCallResult(APIQuery query,boolean required) {
			final APICallResult result =
				data.get( query );
			if ( result == null && required ) {
				throw new RuntimeException("Internal error, did not find API query call result for "+query);
			}
			return result;
		}

		public void callResultUpdated(APICallResult result) {

			final int index = 
				data.getIndexFor( result );
			
			if ( index != -1 ) {
				notifyRowChanged( index );						
			}
		}

		public void addEntry(APICallResult entry) {

			final int removedIndex = 
				data.purge(); // remove excess entry
			
			if ( removedIndex != -1 ) {
				notifyRowRemoved( removedIndex );
			}

			final int newIndex = 
				data.addEntry( entry );
			
			notifyRowInserted( newIndex );
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex) 
		{
			final APICallResult value = getRow( modelRowIndex );
			if ( value == null ) {
				return "<invalid row index "+modelColumnIndex+">";
			}
			
			switch( modelColumnIndex ) {
			case 0:
				final Date d = new Date( value.timestamp );
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( d );
			case 1:
				return value.baseURI.toString();				
			case 2:
				return value.query.getHashString();
			case 3:
				return value.cachedUntil;
			case 4:
				if ( value.duration > 0 ) {
					return ""+value.duration+" ms";
				}
				return "...";
			case 5:
				return value.status;
			default:
				throw new RuntimeException("Unhandled column index "+modelColumnIndex);
			}
		}

		@Override
		public APICallResult getRow(int modelRow) {
			return data.getRow( modelRow );
		}

		@Override
		public int getRowCount() {
			return data.size();
		}
	}
	
	private final class CustomTableRenderer extends DefaultTableCellRenderer {
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) 
		{

			final APICallResult modelObject  =
				model.getRow( row );
			
			super.getTableCellRendererComponent(table, value, false , hasFocus,
					row, column);
			
			if ( modelObject == null ) {
				return this;
			}
			
			if ( modelObject.isOK() ) {
				setBackground( Color.GREEN );
			} else if ( modelObject.isPending() ) {
				setBackground( Color.YELLOW );
			} else if ( modelObject.isError() ) {
				setBackground( Color.RED);
			}

			if ( modelObject.isError() ) {
				setToolTipText( modelObject.status );
			} else {
				setToolTipText( null );
			}
			
			return this;
		}
	}

	@Override
	protected JPanel createPanel() {
		
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		
		table.setModel( this.model );
		
		final JScrollPane pane = new JScrollPane( table );
		pane.setPreferredSize( new Dimension(400,200 ) );
		
		result.add( pane , constraints(0,0).resizeBoth().end() );
		return result;
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback) {
		this.apiClient.addRequestObserver( observer );
	}
	
	@Override
	protected void onDetachHook() {
		this.apiClient.removeRequestObserver( observer );
	}
	
	@Override
	protected void disposeHook() {
		this.apiClient.removeRequestObserver( observer );
	}

	private final class Observer implements IAPIRequestObserver {

		@Override
		public void requestFailed(URI baseURI, APIQuery query, Throwable exception) {

			final APICallResult result = model.getCallResult( query , true );
			result.status = "ERROR: "+exception.getMessage();
			result.duration = System.currentTimeMillis() - result.timestamp;
			model.callResultUpdated( result );
		}

		@Override
		public void requestFinished(URI baseURI, APIQuery query,EveDate cachedUntil) {
			
			final APICallResult result = model.getCallResult( query , true );
			result.status = APICallResult.STATUS_MSG_OK;
			result.duration = System.currentTimeMillis() - result.timestamp;
			if ( cachedUntil != null ) {
				result.cachedUntil = 
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( cachedUntil.getLocalTime() );
			} else {
				result.cachedUntil = "<unknown>";
			}
			model.callResultUpdated( result );
		}

		@Override
		public void requestStarted(URI baseURI, APIQuery query) {

			boolean isNewEntry = false;
			APICallResult entry = model.getCallResult( query , false );
			if ( entry == null ) {
				entry = new APICallResult();
				isNewEntry = true;
			}
			entry.timestamp = System.currentTimeMillis();
			entry.query = query;
			entry.baseURI = baseURI;
			
			if ( isNewEntry ) {
				model.addEntry( entry );
			} else {
				entry.duration = 0; // reset duration
				entry.status = APICallResult.STATUS_MSG_PENDING;
				model.callResultUpdated( entry );
			}
		}
	}
	
	public void setMaxEntryCount(int maxCount) {
		if ( maxCount < 1 ) {
			throw new IllegalArgumentException("Entry count must be at least 1");
		}
		this.maxEntries = maxCount;
	}

}
