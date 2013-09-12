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
package de.codesourcery.eve.skills.ui.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * This is a work-around to fix the
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6723524
 * rendering bug in Nimbus Look &amp; Feel.
 * 
 * Code shamelessly stolen from a user comment
 * there (done by dlemmermann).
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public class FixedBooleanTableCellRenderer extends DefaultTableCellRenderer 
{
	private JCheckBox renderer;
	
	public FixedBooleanTableCellRenderer() {
		renderer = new JCheckBox();
		renderer.setHorizontalAlignment( JLabel.CENTER );
	}
	
	public static void attach(JTable table) {
		table.setDefaultRenderer(Boolean.class  , new FixedBooleanTableCellRenderer() );
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) 
	{
		
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		if ( ! ( value instanceof Boolean ) ) {
			return this;
		}
		
		final Boolean b = (Boolean) value;
		renderer.setSelected(b);
		
		if (isSelected) {
			renderer.setForeground(table.getSelectionForeground());
			renderer.setBackground(table.getSelectionBackground());
		} else {
			Color bg = getBackground();
			renderer.setForeground(getForeground());
			// We have to create a new color object because Nimbus returns
			// a color of type DerivedColor, which behaves strange, not sure
			// why.
			renderer.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue()));
			renderer.setOpaque(true);
		}
		return renderer;
	}
}
