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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.model.AbstractViewFilter;
import de.codesourcery.eve.skills.ui.model.IViewFilter;

public class ByLabelFilterComponent<X> extends AbstractComponent
{

	private final JButton clearButton =
		new JButton("Clear");
	
	private final JTextField nameField =
		new JTextField();
	
	private final String title;
	
	private final IItemLabelProvider<X> labelProvider;
	
	private final IViewFilter<X> viewFilter = 
		new AbstractViewFilter<X>() {

			@Override
			public boolean isHiddenUnfiltered(X value)
			{
				final String expected = nameField.getText();
				if ( expected != null && expected.trim().length() > 0 ) 
				{
					final String label = 
						labelProvider.getLabelFor( value );
					
					if ( label != null ) {
						return ! label.toLowerCase().contains( expected.toLowerCase() );
					}
				}
				return false;
			}
	};
	
	public interface IItemLabelProvider<X> {
		
		public String getLabelFor(X obj);
		
		public void viewFilterChanged(IViewFilter<X> filter);
	}
	
	public ByLabelFilterComponent(String title , IItemLabelProvider<X> labelProvider) {
		
		if ( StringUtils.isBlank( title ) ) {
			throw new IllegalArgumentException("title cannot be blank.");
		}
		
		if ( labelProvider == null ) {
			throw new IllegalArgumentException("labelProvider cannot be NULL");
		}
		
		this.title = title;
		this.labelProvider = labelProvider;
	}
	
	public IViewFilter<X> getViewFilter() {
		return viewFilter;
	}
	
	@Override
	protected JPanel createPanel()
	{
		final JPanel result =
			new JPanel();
		
		result.setLayout( new GridBagLayout() );
		result.setBorder( BorderFactory.createTitledBorder( title ) );
		
		// textfield
		this.nameField.getDocument().addDocumentListener( new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				labelProvider.viewFilterChanged( getViewFilter() );
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				labelProvider.viewFilterChanged( getViewFilter() );				
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				labelProvider.viewFilterChanged( getViewFilter() );				
			}
		} );
		
		nameField.setColumns( 20 );
		result.add( nameField , constraints( 0 , 0 ).resizeHorizontally().useRelativeWidth().end() );
		
		// button
		clearButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				nameField.setText("");
			}
		});
		result.add( clearButton , constraints( 1 , 0 ).noResizing().useRemainingWidth().end() );
		
		return result;
	}
	
}
