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
package de.codesourcery.eve.skills.ui.components;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractEditorComponent extends AbstractComponent implements IEditorComponent {

	private boolean wasCancelled = false;

	private JButton okButton;
	private JButton cancelButton;
	
	private final ActionListener myListener = new ActionListener() {
		@Override
		public final void actionPerformed(ActionEvent e) {
			Object src = e.getSource();

			if ( src == okButton ) {
				okButtonClicked();
			} else if ( src == cancelButton ) {
				cancelButtonClicked();
			}
		}
	}; 

	// guarded-by: listeners
	private final List<IEditorListener> listeners =
		new ArrayList<IEditorListener>(); 

	public AbstractEditorComponent() {
		super();
	}

	public AbstractEditorComponent(String title) {
		super(title);
	}

	public void addEditorListener(IEditorListener listener ) {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (listeners) {
			listeners.add (listener );
		}
	}

	protected static boolean isBlank(JTextField textField) {
		return StringUtils.isBlank( textField.getText() );
	}

	public void removeEditorListener(IEditorListener listener ) {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized (listeners) {
			listeners.remove(listener );
		}
	}

	private void notifyEditorListeners(boolean success) {

		final List<IEditorListener> copy;
		synchronized (listeners) {
			copy =
				new ArrayList<IEditorListener>( this.listeners );

		}
		for ( IEditorListener l : copy ) {
			if ( success ) {
				l.editingFinished( this );
			} else {
				l.editingCancelled( this );
			}
		}
	}

	@Override
	protected final JPanel createPanel() {

		final JPanel result = new JPanel();
		result.setLayout( new BorderLayout() );

		result.add( createPanelHook() , BorderLayout.NORTH );

		final JPanel buttonPanel = new JPanel();
		populateButtonPanel( buttonPanel );
		result.add( buttonPanel ,BorderLayout.SOUTH );
		return result;
	}

	protected void populateButtonPanel(JPanel buttonPanel) {

		okButton = createOkButton();
		okButton.addActionListener( myListener );

		cancelButton = createCancelButton();
		cancelButton.addActionListener( myListener );

		buttonPanel.add( okButton );
		buttonPanel.add( cancelButton );
	}

	protected abstract JButton createOkButton();

	protected abstract JButton createCancelButton();

	protected abstract JPanel createPanelHook();

	protected void okButtonClickedHook() {

	}
	
	/**
	 * Check whether the user input is valid.
	 * 
	 * @return <code>true</code> if the user input is valid, otherwise <code>false</code>.
	 * @throws Exception
	 */
	protected abstract boolean hasValidInput(); 
	
	/**
	 * Invoked after the user pressed
	 * the 'ok' (save) button.
	 * 
	 * This method makes sure that 
	 * {@link #hasValidInput()} yields <code>true</code>
	 * and <b>only</b> then {@link #okButtonClickedHook()}
	 * is invoked.
	 * 
	 * Subclasses may invoke this method to 'fake'
	 * a button click.
	 */
	protected final void okButtonClicked() {
		
		if ( ! hasValidInput() ) {
			log.info("okButtonClicked(): User input invalid.");
			return;
		}
		okButtonClickedHook();
		notifyEditorListeners( true );
		wasCancelled = false;
	}

	protected void cancelButtonClickedHook() {
	}

	public final boolean wasCancelled() {
		return wasCancelled;
	}

	protected final void cancelButtonClicked() {
		cancelButtonClickedHook();
		notifyEditorListeners( false );
		wasCancelled = true;
	}

}
