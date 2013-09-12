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
package de.codesourcery.eve.skills.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Builder that holds column descriptions
 * for rendering a table widget.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class TableColumnBuilder {

	public static final Comparator<String> STRING_COMPARATOR = new Comparator<String>() {

		@Override
		public int compare(String arg0, String arg1) {
			return arg0.compareTo( arg1 );
		}
	};
	
	@SuppressWarnings("unchecked")
	public static final Comparator<Comparable> GENERIC_COMPARATOR = new Comparator<Comparable>() {

		@Override
		public int compare(Comparable o1,Comparable o2)
		{
			final int result = o1.compareTo( o2 );
			return result;
		}};
	
	private final List<TableColumn> columns =
		new ArrayList<TableColumn>();
	
	public static final class TableColumn {
		
		private final String name;
		private final Class<?> dataType;
		private final Comparator<?> comparator;
		
		public TableColumn(String name,Class<?> dataType , Comparator<?> comp) {
			if ( name == null ) {
				throw new IllegalArgumentException("name cannot be NULL");
			}
			if ( dataType == null ) {
				throw new IllegalArgumentException("dataType cannot be NULL");
			}
			this.name = name;
			this.dataType = dataType;
			this.comparator = comp;
		}
		
		public String getName() { return name; }
		public Class<?> getDataType() { return dataType; }
		public Comparator<?> getComparator() {
			if ( comparator == null ) {
				throw new IllegalStateException("Table column "+this+" has no comparator assigned ");
			}
			return comparator; 
		}
		
		public boolean supportsSorting() {
			return this.comparator != null;
		}
		
		@Override
		public String toString() {
			return "TableColumn[ name="+name+" , data type = "+dataType.getName()+
			" , supports_sorting="+supportsSorting()+" ]";
		}
	}
	
	public int getColumnCount() { return columns.size(); }
	
	public TableColumn getColumn(int index) {
		return columns.get( index );
	}
	
	public String[] getColumnNames() {
		final String[] result = new String[ columns.size() ];
		for ( int i = 0 ; i < columns.size() ; i++ ) {
			result[i] = columns.get( i ).getName();
		}
		return result;
	}
	
	public List<TableColumn> getColumns() {
		return columns;
	}
	
	public TableColumnBuilder addAll(Collection<String> columnNames,Class<?> columnClass) {
		for ( String s : columnNames ) {
			add( s , columnClass );
		}
		return this;
	}
	
	public TableColumnBuilder addAll(Collection<String> columnNames) {
		return addAll( columnNames ,String.class );
	}
	
	public TableColumnBuilder add(String columnName) {
		columns.add( new TableColumn( columnName , String.class , STRING_COMPARATOR ) );
		return this;
	}
	
	public TableColumnBuilder add(String columnName,Class<?> dataType) {
		
		Comparator<?> comp = null;
		if ( dataType == String.class ) {
			comp = STRING_COMPARATOR;
		} else if ( Comparable.class.isAssignableFrom( dataType ) ) {
			comp = GENERIC_COMPARATOR;
		}
		
		return add( columnName , dataType , comp );
	}	
	
	public TableColumnBuilder add(String columnName,Class<?> dataType,Comparator<?> comp) {
		columns.add( new TableColumn( columnName , dataType , comp ) );
		return this;
	}

	public String getColumnName(int columnIndex) {
		return this.columns.get( columnIndex ).getName();
	}

	public Class<?> getColumnClass(int columnIndex) {
		return this.columns.get( columnIndex ).getDataType();
	}	
}
