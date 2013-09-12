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
package de.codesourcery.eve.skills.ui.spreadsheet;

import java.util.ArrayList;
import java.util.List;

public class TableRow
{
	private final SpreadSheetTableModel model;
	
	private List<ITableCell> cells = new ArrayList<ITableCell>();
	
	public TableRow(SpreadSheetTableModel model) {
		if ( model == null ) {
			throw new IllegalArgumentException(
					"model cannot be NULL");
		}
		this.model = model;
	}
	
	public int getCellCount() {
		return cells.size();
	}
	
	public ITableCell getCell(int index) {
		if ( index < 0 ) {
			throw new IllegalArgumentException("Invalid index "+index);
		}
		if ( index < cells.size() ) {
			return cells.get(index);
		}
		
		final int rowIndex = model.getRowIndex(this);
		for ( int i = cells.size() ; i <= index ; i++ ) {
			cells.add( model.getTableFactory().createEmptyCell( rowIndex , i ) );
		}
		
		model.rowChanged( this , rowIndex );
		
		return cells.get( index );
	}
	
	public void addCell(ITableCell cell) {
		if ( cell == null ) {
			throw new IllegalArgumentException(
					"cell cannot be NULL");
		}
		cells.add( cell );
		model.rowChanged( this , model.getRowIndex( this ) );
	}
}
