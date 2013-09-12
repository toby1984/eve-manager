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
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;

import de.codesourcery.eve.skills.ui.components.AbstractComponent.ILabelProvider;
import de.codesourcery.eve.skills.ui.spreadsheet.CellAttributes.Alignment;

public class SpreadSheetCellRenderer extends AbstractSpreadSheetTableCellRenderer
{

	public void attachTo(JTable table) {
		table.setDefaultRenderer( ITableCell.class , this );
	}
	
	private final ILabelProvider<ITableCell> labelProvider;
	private Font boldFont;
	
	public SpreadSheetCellRenderer() {
		this.labelProvider = new ILabelProvider<ITableCell>() {

			@Override
			public String getLabel(ITableCell obj)
			{
				return obj.getValue() != null ? obj.getValue().toString() : "";
			}
		};
	}
	
	public SpreadSheetCellRenderer(ILabelProvider<ITableCell> provider) {
		this.labelProvider = provider;
	}
	
	protected Font getBoldFont(JTable table) {
		if ( boldFont == null ) {
			boldFont = table.getFont().deriveFont( Font.BOLD );
		}
		return boldFont;
	}
	
	private ITableCell getCell(JTable table,int row,int column) {
		if ( ! ( table.getModel() instanceof SpreadSheetTableModel ) ) {
			return null;
		}
		
		final SpreadSheetTableModel model = 
			(SpreadSheetTableModel) table.getModel();
		
		final ITableCell cell =
			model.getCell( row , column);
		
		return cell;
	}
	
	private ITableCell cell;
	
	@Override
	protected void renderHook(JTable table, Object value, int row, int column,boolean isSelected)
	{
		
		cell =
			getCell( table , row , column );
		
		if ( cell == null ) {
			super.renderHook(table, value, row, column,isSelected);
			return;
		}
		
		setFont( getFont( table , cell ) );
		
		final CellAttributes cellAttributes = cell.getAttributes();
		final Alignment alignment = (Alignment) cellAttributes.getAttribute( CellAttributes.ALIGNMENT );
		if ( alignment != null ) {
			switch( alignment ) {
				case LEFT:
					setHorizontalAlignment( JLabel.LEFT );
					break;
				case RIGHT:
					setHorizontalAlignment( JLabel.RIGHT );
					break;
				default:
					setHorizontalAlignment( JLabel.CENTER );
			}
		} else {
			setHorizontalAlignment( JLabel.CENTER );
		}
		
		setBackground( table.getBackground() );
		
		if ( ! isSelected ) {
			final IHighlightingFunction function =
				(IHighlightingFunction) cellAttributes.getAttribute( CellAttributes.HIGHLIGHTING_FUNCTION );
			if ( function != null && function.requiresHighlighting( cell ) ) {
				setBackground( Color.RED );
			}
		}
	}

	protected Font getFont(JTable table , ITableCell cell) 
	{
		
		final boolean isHeaderCell =
			cell.getAttributes().hasAttribute( CellAttributes.RENDER_BOLD ) ;
		
		if ( isHeaderCell ) {
			return getBoldFont( table );
		}
		return table.getFont();
	}

	@Override
	protected String getLabelFor(JTable table, Object value, int row, int column)
	{
		if ( cell == null ) {
			return "";
		}
		return labelProvider.getLabel( cell );
	}
}
