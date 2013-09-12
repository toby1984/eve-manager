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
package de.codesourcery.eve.skills.ui.components.impl.planning.nodeeditors;

import static de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.LayoutHints.ALIGN_RIGHT;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.INodeValueChangeListener;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ManufacturingJobNode;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.FixedCell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.HorizontalGroup;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.VerticalGroup;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class ManufacturingJobEditorComponent extends NodeEditorComponent 
{
	private final JTextField quantity = new JTextField("0");

	private ManufacturingJobNode jobRequest;

	private final ActionListener actionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e)
		{
			final Object src = e.getSource();

			final ManufacturingJobRequest job =
				ManufacturingJobEditorComponent.this.jobRequest.getManufacturingJobRequest();

			if ( src == quantity && jobRequest.hasEditableQuantity() ) 
			{
				final int newQuantity = getAsIntValue( quantity );
				job.setQuantity( newQuantity );
				notifyListener( jobRequest );
			}
		}
	};

	public ManufacturingJobEditorComponent(INodeValueChangeListener listener) 
	{
		super( listener );
	}

	protected int getAsIntValue(JTextComponent comp) {
		return Integer.parseInt( comp.getText() );
	}

	@Override
	protected JPanel createPanel() 
	{
		final JPanel result = new JPanel();

		quantity.setColumns( 10 );

		quantity.addActionListener( actionListener );

		final VerticalGroup vGroup = new VerticalGroup();

		if ( jobRequest.hasEditableQuantity() ) {
			vGroup.add( new HorizontalGroup( 
					new FixedCell( new JLabel("Produced quantity",JLabel.RIGHT) ) , new FixedCell( quantity , ALIGN_RIGHT ) )
			);
		}

		new GridLayoutBuilder().add(vGroup ).addTo( result );

		// populate input fields
		final ManufacturingJobRequest job =
			ManufacturingJobEditorComponent.this.jobRequest.getManufacturingJobRequest();
		if ( jobRequest != null ) {
			quantity.setText( "" + job.getQuantity() );
		} else {
			quantity.setText( "0" );
		}

		return result;
	}

	public void setManufacturingJobRequest(final ManufacturingJobNode node) 
	{
		this.jobRequest = node;
	}

	protected static String toString(ISKAmount amount) {
		return AmountHelper.formatISKAmount( amount )+" ISK";
	}

}