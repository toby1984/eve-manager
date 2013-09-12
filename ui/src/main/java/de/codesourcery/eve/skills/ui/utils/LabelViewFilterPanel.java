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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.ui.model.AbstractViewFilter;
import de.codesourcery.eve.skills.ui.model.IViewFilter;

/**
 * <code>JPanel</code> that wraps a textfield with a 'clear' button
 * and provides a {@link IViewFilter} that filters items
 * by their label.
 * 
 * Subclasses get notified immediately (on each keypress) 
 * whenever the textfield {@link IStaticDataModel} being changed.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class LabelViewFilterPanel<T> extends JPanel
{
	private final JTextField textField = new JTextField();
	private final JButton clearButton = new JButton("Clear");
	
	private final MyViewFilter viewFilter = new MyViewFilter();
	
	private final class MyViewFilter extends AbstractViewFilter<T> 
	{
		private String filterExpression = null;
		
		@Override
		public boolean isHiddenUnfiltered(T blueprint)
		{
			if ( filterExpression == null ) {
				return false;
			}
			return ! getStringFor( blueprint ).toLowerCase().contains( filterExpression );
		}
		
		public void setFilterExpression(String filterExpression)
		{
			
			boolean filterChanged = false;
			if ( filterExpression != null && filterExpression.length() > 0 ) {
				final String newValue = filterExpression.toLowerCase();
				filterChanged = ! StringUtils.equals( this.filterExpression , newValue );
				this.filterExpression = newValue;
			} else {
				filterChanged = this.filterExpression != null;
				this.filterExpression = null;
			}
			
			if ( filterChanged ) {
				LabelViewFilterPanel.this.filterChanged();
			}
		}
	};
	
	protected abstract String getStringFor(T item);
	
	public IViewFilter<T> getViewFilter() {
		return viewFilter;
	}
	
	public LabelViewFilterPanel(String label,int columns) 
	{
		super();
		
		clearButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				textField.setText( null );
			}
		});
	
		textField.setColumns( columns );
		textField.getDocument().addDocumentListener( new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				searchFieldChanged( LabelViewFilterPanel.this , textField.getText() );				
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				searchFieldChanged( LabelViewFilterPanel.this , textField.getText() );
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				searchFieldChanged( LabelViewFilterPanel.this , textField.getText() );
			}} );
		
		setLayout( new GridBagLayout() );
		
		new GridLayoutBuilder().add(
			new GridLayoutBuilder.HorizontalGroup(
				new GridLayoutBuilder.FixedCell( new JLabel(label) ) ,
				new GridLayoutBuilder.FixedCell( textField ) ,
				new GridLayoutBuilder.FixedCell( clearButton )
			)
		).addTo( this );
	}
	
	public String getText() { return textField.getText(); }
	
	public void setText(String s) { textField.setText( s ); }
	
	protected final void searchFieldChanged(LabelViewFilterPanel<T> field , String newValue) {
		viewFilter.setFilterExpression( newValue );
	}
	
	protected abstract void filterChanged();
}
