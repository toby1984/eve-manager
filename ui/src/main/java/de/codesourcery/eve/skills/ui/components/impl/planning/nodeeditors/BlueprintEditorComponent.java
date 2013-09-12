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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import de.codesourcery.eve.skills.datamodel.Blueprint.Kind;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.BlueprintNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.INodeValueChangeListener;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.FixedCell;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.HorizontalGroup;
import de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.VerticalGroup;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class BlueprintEditorComponent extends NodeEditorComponent 
{
	private final JTextField meLevel = new JTextField("0");
	private final JTextField peLevel = new JTextField("0");

	private BlueprintNode blueprintNode;

	private final ActionListener actionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e)
		{
			final Object src = e.getSource();

			boolean valueChanged = false;
			if ( src == meLevel ) 
			{
				final int newME = getAsIntValue( meLevel );
				if ( blueprintNode.getMeLevel() != newME ) 
				{
					blueprintNode.setMeLevel( newME );
					valueChanged = true;
				}
			} 
			else if ( src == peLevel ) 
			{
				final int newPE = getAsIntValue( peLevel );
				if ( newPE != blueprintNode.getPeLevel() ) {
					blueprintNode.setPeLevel( newPE );
					valueChanged = true;
				}
			}

			if ( valueChanged ) {
				notifyListener( blueprintNode );
			}
		}
	};

	public BlueprintEditorComponent(INodeValueChangeListener listener) 
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

		meLevel.setColumns( 3  );
		peLevel.setColumns( 3 );

		meLevel.addActionListener( actionListener );
		peLevel.addActionListener( actionListener );

		final VerticalGroup vGroup = new VerticalGroup()
		.add( new HorizontalGroup( new FixedCell( new JLabel("BPO ME" ) ) , new FixedCell( meLevel ) ) )
		.add( new HorizontalGroup( new FixedCell( new JLabel("BPO PE" ) ) , new FixedCell( peLevel ) ) );

		new GridLayoutBuilder().add(vGroup ).addTo( result );

		// populate input fields
		final BlueprintNode job =
			blueprintNode;
		
		if ( job != null ) {
			meLevel.setText( "" + job.getMeLevel() );
			peLevel.setText( "" + job.getPeLevel() );
		} else {
			meLevel.setText( "0" );
			peLevel.setText( "0" );
		}
		
		meLevel.setEditable( job != null && job.getBlueprintKind() != Kind.COPY );
		peLevel.setEditable( job != null && job.getBlueprintKind() != Kind.COPY );

		return result;
	}

	public void setBlueprintNode(final BlueprintNode node) 
	{
		this.blueprintNode = node;
	}

	protected static String toString(ISKAmount amount) {
		return AmountHelper.formatISKAmount( amount )+" ISK";
	}

}
