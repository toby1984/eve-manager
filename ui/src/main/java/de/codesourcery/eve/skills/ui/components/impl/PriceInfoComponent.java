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
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.PriceInfo;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;

public class PriceInfoComponent extends AbstractEditorComponent {

	private final String message;
	private final PriceInfo.Type priceType;

	private JTextField minPrice = new JTextField("0.00");
	private JTextField avgPrice = new JTextField("0.00");
	private JTextField maxPrice = new JTextField("0.00");

	private final ActionListener listener =
		new ActionListener( ) {

		@Override
		public void actionPerformed(ActionEvent e) {
			if ( e.getSource() instanceof JTextField) {
				validateInputField( (JTextField) e.getSource() );
			}
		}

	};

	public PriceInfoComponent(String message,PriceInfo.Type infoType) {
		if ( message == null ) {
			throw new IllegalArgumentException("message cannot be NULL");
		}
		if ( infoType == null ) {
			throw new IllegalArgumentException("infoType cannot be NULL");
		}
		
		infoType.assertNotAny();
		
		this.message = message;
		this.priceType = infoType;
	}

	@Override
	protected JButton createCancelButton() {
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton() {
		return new JButton("ok");
	}

	public double getMinPrice() {
		return getValue( minPrice );
	}

	public double getMaxPrice() {
		return getValue( maxPrice );
	}
	
	public double getAveragePrice() {
		return getValue( avgPrice );
	}
	
	protected double getValue(JTextField input) throws NumberFormatException {
		final String value = input.getText();
		if ( StringUtils.isBlank( value ) ) {
			throw new NumberFormatException("You need to enter a positive number");
		}

		final double val = Double.parseDouble( value );
		if ( val < 0 ) {
			throw new NumberFormatException("You need to enter a positive number");
		}
		return val;

	}

	protected void validateInputField(JTextField input) {
		try {
			getValue( input );
		} 
		catch(NumberFormatException e) {
		}
	}

	@Override
	protected JPanel createPanelHook() {

		final JPanel  panel = new JPanel();
		panel.setLayout( new GridBagLayout() );

		final String kind;
		
		switch( priceType ) {
		case BUY:
			kind="buy";
			break;
		case SELL:
		default:
			throw new RuntimeException("Invalid data in switch/case: PriceInfo.Type.ALL cannot be queried from user");
		}

		minPrice.addActionListener( listener );
		maxPrice.addActionListener( listener );
		avgPrice.addActionListener( listener );


		final TextArea desc = new TextArea();
		desc.setText( message );
		desc.setEditable( false );

		panel.add( desc , constraints(0,0).width(2).resizeBoth().end() );

		panel.add( new JLabel( "Minimum "+kind+" price" ), constraints( 0,1 ).useRelativeWidth().end() );
		panel.add( minPrice , constraints( 1,1 ).useRemainingWidth().end() );

		panel.add( new JLabel( "Average "+kind+" price" ), constraints( 0,2 ).useRelativeWidth().end() );
		panel.add( avgPrice , constraints( 1,2 ).useRemainingWidth().end() );

		panel.add( new JLabel( "Maximum "+kind+" price" ), constraints( 0,3 ).useRelativeWidth().end() );
		panel.add( maxPrice , constraints( 1,3 ).useRemainingWidth().end() );

		return panel;
	}

	@Override
	protected boolean hasValidInput()
	{
		return true;
	}

}
