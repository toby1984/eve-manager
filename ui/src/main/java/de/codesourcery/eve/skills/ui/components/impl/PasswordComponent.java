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
import java.awt.Window;

import javax.annotation.Resource;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.apiclient.utils.ICipherProvider;
import de.codesourcery.eve.apiclient.utils.IPasswordProvider;
import de.codesourcery.eve.apiclient.utils.PasswordCipherProvider;
import de.codesourcery.eve.skills.ui.components.AbstractEditorComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;

public class PasswordComponent extends AbstractEditorComponent {

	public enum Mode {
		SET_PASSWORD,
		QUERY_PASSWORD;
	}
	
	@Resource(name="appconfig-provider")
	private IAppConfigProvider appConfigProvider;
	
	private final JPasswordField password1 =
		new JPasswordField();
	
	private final JPasswordField password2 =
		new JPasswordField();	
	
	private final JCheckBox savePassword =
		new JCheckBox("Store password on hard disk (unsafe!) ?");
	
	private final String message;
	private final Mode mode;
	
	public PasswordComponent(String title, String message , Mode mode) {
		super(title);
		this.message = message;
		this.mode = mode;
	}
	
	public boolean isSavePassword() {
		return savePassword.isSelected();
	}
	
	public static ICipherProvider createCipherProvider(String title,
			String message,
			Mode mode , 
			final Window parent) 
	{
		
		final PasswordComponent comp=
			new PasswordComponent( title , message, mode );
		
		final IPasswordProvider passwordProvider = new IPasswordProvider() {

			@Override
			public char[] getPassword() {
				
				final Window window =
						ComponentWrapper.wrapComponent( parent , comp );
				
				window.setVisible(true);
				return comp.getPassword1();
			}
		};
		
		return new PasswordCipherProvider( passwordProvider );
	}
	
	@Override
	protected void cancelButtonClickedHook() {
		clearPasswords();
	}
	
	@Override
	public final boolean isModal() {
		return true;
	}
	
	protected static void clearArray(char[] a) {
		if ( a != null ) {
			for ( int i = 0 ; i < a.length ; i++ ) {
				a[i] = 0;
			}
		}
	}
	protected void clearPasswords() {
		clearArray( password1.getPassword() );
		clearArray( password2.getPassword() );
	}

	@Override
	protected JButton createCancelButton() {
		return new JButton("Cancel");
	}

	@Override
	protected JButton createOkButton() {
		return new JButton("Ok");
	}
	
	public char[] getPassword1() {
		return password1.getPassword();
	}
	
	public char[] getPassword2() {
		return password2.getPassword();
	}	
	
	/**
	 * Resets this dialog editor.
	 * 
	 * @see #isCompleted()
	 */
	public void reset() {
		password1.setText(null);
		password2.setText(null);
	}
	
	@Override
	protected void okButtonClickedHook() {
		
		if ( ArrayUtils.isEmpty( getPassword1() ) ) {
			displayError("You need to enter a password)");
			return;
		}
		
		if ( mode == Mode.SET_PASSWORD ) {
			if ( ArrayUtils.isEmpty( getPassword1() ) ||
					getPassword1().length < 4 ) 
			{
				displayError("You need to enter a password that is at least 4 characters long");
				return;
			}
			
			if ( ! ArrayUtils.isEquals( getPassword1(), getPassword2() ) ) {
				displayError("Password mismatch - you need to enter the same password twice");
				return;
			}
		}
		
	}

	@Override
	protected JPanel createPanelHook() {
		
		final JPanel panel =
			new JPanel();
		
		panel.setLayout( new GridBagLayout() );
		
		int y = 0;
		if ( ! StringUtils.isBlank( message ) ) {
			panel.add( new JLabel( message ) ,
					constraints( 0 ,y++ ).width(2).end() 
			);
		}
		
		password1.setColumns( 10 );
		password2.setColumns( 10 );
		
		switch( mode ) {
		case SET_PASSWORD:
			
			panel.add( new JLabel("Enter new password:") , constraints(0,y).end() );
			panel.add( password1 , constraints( 1 , y++ ).end() );
			
			panel.add( new JLabel("Confirm password:") , constraints(0,y).end() );
			panel.add( password2 , constraints( 1 , y++ ).end() );
			
			break;
		case QUERY_PASSWORD:
			panel.add( new JLabel("Enter password:") , constraints(0,y).end() );
			panel.add( password1 , constraints( 1 , y++ ).end() );			
			break;
		default:
			throw new RuntimeException("Unreachable code reached");
		}
		
		panel.add( this.savePassword , constraints( 0 , y ).anchorEast().useRemainingWidth().end() );
		return panel;
	}

	@Override
	protected boolean hasValidInput()
	{
		return true;
	}

}
