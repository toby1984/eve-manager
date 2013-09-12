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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.ArrayUtils;

public class SpreadSheetTableModel extends AbstractTableModel
{

	private final List<TableRow> rows =
		new ArrayList<TableRow>();

	private final ITableFactory factory;

	public SpreadSheetTableModel(ITableFactory factory) {
		if ( factory == null ) {
			throw new IllegalArgumentException("factory cannot be NULL");
		}
		this.factory = factory;
	}

	public ITableFactory getTableFactory() { return factory; }

	public static void main(String[] args)
	{

		final ITableFactory cellFactory = new ITableFactory() {

			@Override
			public ITableCell createEmptyCell(int row, int column)
			{
				return new SimpleCell();
			}

			@Override
			public TableRow createRow(SpreadSheetTableModel tableModel)
			{
				return new TableRow( tableModel );
			}

		};
		
		final SpreadSheetTableModel model = 
			new SpreadSheetTableModel( cellFactory );

		final JTable table = new JTable( model );
		table.setFillsViewportHeight( true );

		final JFrame frame = new JFrame("Test");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//		table.setPreferredSize( new Dimension(400,200 ) );
		table.setBorder( BorderFactory.createLineBorder(Color.black ) );
		
		frame.getContentPane().add( new JScrollPane( table ) );

		SwingUtilities.invokeLater( new Runnable() {

			@Override
			public void run()
			{
				frame.pack();
				frame.setLocationRelativeTo( null );
				
				frame.setVisible( true );
				
				model.addRow( new SimpleCell("First row" ) );
				model.addRow( new SimpleCell("Second row #1" ) , new SimpleCell("Second row #2" ));
				model.addRow( new SimpleCell("Third row #1" ) , new SimpleCell("Third row #2" ) , 
						new SimpleCell("Third row #3" ) );
			}
		} );

	}

	public void addEmptyRow() {
		addRow();
	}

	public void addRow(ITableCell... cells) {
		final TableRow r = factory.createRow( this );
		if ( ! ArrayUtils.isEmpty( cells ) ) {
			for ( ITableCell c : cells ) {
				r.addCell( c );
			}
		}

		int index = rows.size();
		rows.add( r );
		fireTableStructureChanged();
//		fireTableRowsInserted( index ,index );
	}

	public void clear() {
		if ( ! rows.isEmpty() ) {
			rows.clear();
			fireTableStructureChanged();
		}
	}

	void rowChanged(TableRow r,int index) 
	{
		if ( index >= 0 ) {
			fireTableStructureChanged();
//			fireTableRowsUpdated( index , index );
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return ITableCell.class;
	}

	@Override
	public int getColumnCount()
	{
		int count = 0;
		for ( TableRow r : rows ) {
			if ( r.getCellCount() > count ) {
				count = r.getCellCount();
			}
		}
		return count;
	}
	
	public List<TableRow> getRows() {
		return new ArrayList<TableRow>( this.rows );
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return " ";
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return getCell(rowIndex, columnIndex).getValue();
	}

	public ITableCell getCell(int rowIndex, int columnIndex)
	{
		if ( rowIndex < 0 || rowIndex >= rows.size() ) {
			throw new IllegalArgumentException("Invalid row index "+rowIndex);
		}
		return rows.get( rowIndex ).getCell( columnIndex );
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return getCell( rowIndex , columnIndex ).isEditable();
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex)
	{
		getCell(rowIndex, columnIndex).setValue( value );
	}

	public int getRowIndex(TableRow tableRow)
	{
		int i = 0;
		for ( TableRow r : rows ) {
			if ( r == tableRow ) {
				return i;
			}
			i++;
		}
		return -1;
	}

}
