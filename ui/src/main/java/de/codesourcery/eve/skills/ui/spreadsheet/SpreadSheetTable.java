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

import java.awt.Component;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class SpreadSheetTable extends JTable
{
	
	public SpreadSheetTable() {
		super();
	}

	public SpreadSheetTable(int numRows, int numColumns) {
		super(numRows, numColumns);
	}

	public SpreadSheetTable(Object[][] rowData, Object[] columnNames) {
		super(rowData, columnNames);
	}

	public SpreadSheetTable(TableModel dm, TableColumnModel cm,
			ListSelectionModel sm) {
		super(dm, cm, sm);
	}

	public SpreadSheetTable(TableModel dm, TableColumnModel cm) {
		super(dm, cm);
	}

	public SpreadSheetTable(TableModel dm) {
		super(dm);
	}

	public SpreadSheetTable(Vector rowData, Vector columnNames) {
		super(rowData, columnNames);
	}

	// Returns the preferred height of a row.
	// The result is equal to the tallest cell in the row.
	protected int getPreferredRowHeight(int rowIndex, int margin)
	{
		// Get the current default height for all rows
		int height = getRowHeight();

		// Determine highest cell in the row
		for (int c = 0; c < getColumnCount(); c++) {
			TableCellRenderer renderer = getCellRenderer(rowIndex, c);
			Component comp = prepareRenderer(renderer, rowIndex, c);
			int h = comp.getPreferredSize().height + 2 * margin;
			height = Math.max(height, h);
		}
		return height;
	}

	// The height of each row is set to the preferred height of the
	// tallest cell in that row.
	public void packRows(int margin)
	{
		packRows(0, getRowCount(), margin);
	}
	
	@Override
	public void setModel(TableModel dataModel)
	{
		super.setModel(dataModel);
		packRows( 2 );
	}

	// For each row >= start and < end, the height of a
	// row is set to the preferred height of the tallest cell
	// in that row.
	protected void packRows(int start, int end, int margin)
	{
		for (int r = 0; r < getRowCount(); r++) {
			// Get the preferred height
			int h = getPreferredRowHeight(r, margin);

			// Now set the row height using the preferred height
			if ( getRowHeight(r) != h ) {
				setRowHeight(r, h);
			}
		}
	}

}
