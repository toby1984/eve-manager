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
package de.codesourcery.eve.skills.ui.model.impl;

import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import junit.framework.TestCase;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.AbstractViewFilter;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;

public class AbstractTableModelTest extends TestCase {

	public static final class TestData {
		private String value1;
		private int value2;
		
		public TestData(String value1, int value2) {
			super();
			this.value1 = value1;
			this.value2 = value2;
		}
	}
	
	private static final class TestModel extends AbstractTableModel<TestData> {

		final List<TestData> data;
		
		public TestModel(List<TestData> data) {
			super(
				new TableColumnBuilder()
				.add("col1" , String.class )
					.add( "col2" , Integer.class ) 
			);
			this.data= data;
		}

		@Override
		protected Object getColumnValueAt(int modelRowIndex,
				int modelColumnIndex) 
		{
			TestData obj = data.get( modelRowIndex );
			switch( modelColumnIndex  ) {
			case 0:
				return obj.value1;
			case 1:
				return obj.value2;
				default:
					throw new IllegalArgumentException("Invalid column "+modelColumnIndex);
			}
		}
		
		public void setData( List<TestData> data) {
			this.data.clear();
			this.data.addAll( data );
			modelDataChanged();
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public TestData getRow(int modelRow) {
			return data.get( modelRow );
		}
		
	}

	private static TestModel createModel(final List<TestData> data) {
		return new TestModel( data );
	}
	
	public void testEmptyModel() {
		
		final  AbstractTableModel<TestData> model = 
			createModel( new ArrayList<TestData>() );
		
		assertEquals( 2 , model.getColumnCount() );
		assertEquals( "col1" , model.getColumnName(0 ) );
		assertEquals( "col2" , model.getColumnName(1 ) );
		assertSame( String.class , model.getColumnClass( 0 ) );
		assertSame( Integer.class , model.getColumnClass( 1 ) );
		
		assertEquals( 0 , model.getRowCount() );
	}
	
	public void testModelWithOneElement() {
		
		
		ArrayList<TestData> data = new ArrayList<TestData>();
		data.add( new TestData("blubb" , 42 ) );
		
		final  AbstractTableModel<TestData> model = 
			createModel( data );
		
		assertEquals( 1 , model.getRowCount() );
		
		assertEquals( new TestData("blubb" , 42 ) , model , 0 );
	}
	
	public void testModelWithTwoElements() {
		
		ArrayList<TestData> data = new ArrayList<TestData>();
		data.add( new TestData("blubb" , 42 ) );
		data.add( new TestData("blubb2" , 43 ) );
		
		final  AbstractTableModel<TestData> model = 
			createModel( data );
		
		assertEquals( 2 , model.getRowCount() );
		
		assertEquals( new TestData("blubb" , 42 ) , model , 0 );
		assertEquals( new TestData("blubb2" , 43 ) , model , 1 );
	}	
	
	public void testEventModelDataChanged() {
		
		final  AbstractTableModel<TestData> model = 
			createModel( new ArrayList<TestData>() );
		
		TableModelListener listener = createMock( TableModelListener.class );
		listener.tableChanged( isA(TableModelEvent.class ) );
		replay( listener );
		
		model.addTableModelListener( listener );
		model.modelDataChanged();
		model.removeTableModelListener( listener );
		model.modelDataChanged();

		verify( listener );
	}	
	
	private void assertEquals( TestData data , AbstractTableModel<TestData>  model, int viewRow) {
		
		// emulate JTable behaviour
		int modelRow = model.getRowSorter().convertRowIndexToModel( viewRow );
		
		final Object val1 = model.getValueAt( modelRow , 0  );
		final Object val2 = model.getValueAt( modelRow , 1  );
		assertEquals( data.value1 , val1 );
		assertEquals( data.value2 , val2 );
	}
	
	@SuppressWarnings("unchecked")
	public void testSorting() {
		
		ArrayList<TestData> data = new ArrayList<TestData>();
		data.add( new TestData("z" , 43 ) );
		data.add( new TestData("a" , 42 ) );
		
		final  AbstractTableModel<TestData> model = 
			createModel( data );

		RowSorter.SortKey key = new SortKey( 0 , SortOrder.ASCENDING );
		model.getRowSorter().setSortKeys( Arrays.asList( key ) );
		
		assertEquals( 1, model.getRowSorter().getSortKeys().size() );
		
		RowSorter.SortKey currentKey =
			(SortKey) model.getRowSorter().getSortKeys().get(0);
		
		assertEquals( 0, currentKey.getColumn() );
		assertEquals( SortOrder.ASCENDING , currentKey.getSortOrder() );
		
		assertEquals( new TestData("a" , 42 ) , model , 0 );
		assertEquals( new TestData("z" , 43 ) , model , 1 );

		model.getRowSorter().setSortKeys( Arrays.asList( new SortKey( 0 , SortOrder.DESCENDING) ) );
		
		assertEquals( new TestData("z" , 43 ) , model , 0 );
		assertEquals( new TestData("a" , 42 ) , model , 1 );
	}		
	
	public void testFiltering() {
		
		ArrayList<TestData> data = new ArrayList<TestData>();
		data.add( new TestData("a" , 42 ) );
		data.add( new TestData("z" , 43 ) );
		
		final  AbstractTableModel<TestData> model = 
			createModel( data );

		assertEquals( 2 , model.getRowSorter().getViewRowCount() );
		
		model.setViewFilter( new AbstractViewFilter<TestData>() {
			@Override
			public boolean isHiddenUnfiltered(TestData value) {
				return "z".equals( value.value1 );
			}} );
		
		assertEquals( 1 , model.getRowSorter().getViewRowCount() );

		assertEquals( new TestData("a" , 42 ) , model , 0 );
		
		model.setViewFilter( new AbstractViewFilter<TestData>() {
			@Override
			public boolean isHiddenUnfiltered(TestData value) {
				return "a".equals( value.value1 );
			}} );
		
		assertEquals( 1 , model.getRowSorter().getViewRowCount() );

		assertEquals( new TestData("z" , 43 ) , model , 0 );		
		
		model.setViewFilter( null );
		
		assertEquals( 2 , model.getRowSorter().getViewRowCount() );

		assertEquals( new TestData("a" , 42 ) , model , 0 );
		assertEquals( new TestData("z" , 43 ) , model , 1 );	
	}
	
	public void testNestedFilters() {
		
		ArrayList<TestData> data = new ArrayList<TestData>();
		data.add( new TestData("a" , 42 ) );
		data.add( new TestData("z" , 43 ) );
		
		final  AbstractTableModel<TestData> model = 
			createModel( data );

		assertEquals( 2 , model.getRowSorter().getViewRowCount() );
		
		model.setViewFilter( new AbstractViewFilter<TestData>() {
			@Override
			public boolean isHiddenUnfiltered(TestData value) {
				return "z".equals( value.value1 );
			}} );
		
		assertEquals( 1 , model.getRowSorter().getViewRowCount() );

		assertEquals( new TestData("a" , 42 ) , model , 0 );
		
		model.addViewFilter( new AbstractViewFilter<TestData>() {
			@Override
			public boolean isHiddenUnfiltered(TestData value) {
				return "a".equals( value.value1 );
			}} );
		
		assertEquals( 0 , model.getRowSorter().getViewRowCount() );
	}
	
	
}
