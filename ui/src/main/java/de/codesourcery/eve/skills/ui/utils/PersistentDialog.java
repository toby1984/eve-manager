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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.lang.StringUtils;

public class PersistentDialog extends JDialog implements IPersistentDialog
{
	private final JCheckBox isDialogEnabled;
	private final PersistenceType type;
	private String id;
	
	private final JButton okButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");
	
	private boolean wasCancelled = false;
	
	public enum Kind {
		INFO,
		CANCEL;
	}
	
	public PersistentDialog(String id,Icon icon ,
			String title, 
			String label, 
			PersistenceType type,
			Kind kind) 
	{
		super( null ,  title , ModalityType.APPLICATION_MODAL );
		
		if ( kind == null ) {
			throw new IllegalArgumentException("kind cannot be NULL");
		}
		
		if (StringUtils.isBlank(id)) {
			throw new IllegalArgumentException(
					"id cannot be blank.");
		}
		
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		if (StringUtils.isBlank(label)) {
			throw new IllegalArgumentException("label cannot be blank.");
		}

		this.id = id;
		this.type = type;

		// configure checkbox
		isDialogEnabled = new JCheckBox( "Always show this dialog ?" , true );
		isDialogEnabled.setHorizontalTextPosition(SwingConstants.RIGHT);
		
		// add panel
		final JPanel content = new JPanel();
		content.setLayout( new GridBagLayout() );
		
		content.add( new JLabel( icon ) , new ConstraintsBuilder().x(0).y(0).anchorWest().noResizing().useRelativeWidth().end() );
		content.add( new JLabel( label ) , new ConstraintsBuilder().x(1).y(0).anchorWest().resizeBoth().useRemainingWidth().end() );
		content.add( isDialogEnabled , new ConstraintsBuilder().x(0).y(1).width(2).anchorWest().noResizing().end() );
		
		final JPanel buttonPanel = new JPanel();
		buttonPanel.add( okButton );
		
		final ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if ( e.getSource() == cancelButton ) {
					wasCancelled = true;
				}
				dispose();
			}
		};
		
		okButton.addActionListener( listener );		
		
		if ( kind == Kind.CANCEL ) {
			buttonPanel.add( cancelButton );
			cancelButton.addActionListener( listener );
		}
		
		content.add( buttonPanel , new ConstraintsBuilder().x(0).y(2).useRemainingSpace().end() );
		
		getContentPane().add( content );
		
		pack();
		
		setLocationRelativeTo( null );
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); 
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e)
			{
				wasCancelled = true;
				dispose();
			}
		} );
	}
	
	public boolean wasCancelled() {
		return wasCancelled;
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
	}
	
	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return type;
	}

	@Override
	public boolean isDisabledByUser()
	{
		return ! isDialogEnabled.isSelected();
	}

}
