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

import javax.annotation.Resource;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.datamodel.AttributeEnhancer;
import de.codesourcery.eve.skills.datamodel.Attributes;
import de.codesourcery.eve.skills.datamodel.CharacterDetails;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.ImplantSet;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.db.dao.IStaticDataModelProvider;
import de.codesourcery.eve.skills.db.datamodel.AttributeType;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.utils.CurrencyHelper;
import de.codesourcery.eve.skills.ui.utils.UITask;

public class CharacterSheetComponent extends AbstractComponent implements ICharacterSelectionProviderAware {

	private final ISelectionListener<ICharacter> listener =
		new ISelectionListener<ICharacter>() {

			@Override
			public void selectionChanged(ICharacter selected) {
				CharacterSheetComponent.this.selectionChanged( selected , false );
			}
		};
	
	@Resource(name="useraccount-store")
	private IUserAccountStore userAccountStore;
	
	@Resource(name="datamodel-provider")
	private IStaticDataModelProvider dataModelProvider;
	
	private ISelectionProvider<ICharacter> selectionProvider;
	private final JTextArea textArea = new JTextArea();
			
	public void setSelectionProvider( ISelectionProvider<ICharacter> selectionProvider) {
		assertDetached();
		this.selectionProvider = selectionProvider;
	}
	
	@Override
	protected void onAttachHook(IComponentCallback callback) {
		selectionProvider.addSelectionListener( listener );
		selectionChanged( selectionProvider.getSelectedItem() , true );
	}
	
	@Override
	protected void onDetachHook() {
		selectionProvider.removeSelectionListener( listener );
	}
	
	@Override
	protected void disposeHook() {
		if ( selectionProvider != null ) {
			selectionProvider.removeSelectionListener( listener );
		}
	}
	
	@Override
	protected JPanel createPanel() {
		
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		
		textArea.setRows( 10 );
		textArea.setColumns( 35 );
		
		setMonospacedFont( textArea );
		
		textArea.setWrapStyleWord( true );
		textArea.setLineWrap(true);
		textArea.setEditable( false );
		
		final JScrollPane pane =
			new JScrollPane( textArea );
			
		result.add( pane , constraints(0,0).resizeBoth().useRemainingSpace().end() );
		return result;
	}
	
	protected void setLabel(final String msg) {
		runOnEventThread( new Runnable() {

			@Override
			public void run() {
				textArea.setText( msg );
			}} );
	}

	protected void selectionChanged(ICharacter selected,boolean reconcile) {
		
		if ( selected == null ) {
			setLabel("Please select a character.");
			return;
		}
		
		submitTask( new UpdateUITask( selected , reconcile ) );
	}
	
	private final class UpdateUITask extends UITask {

		private final boolean reconcile;
		private final ICharacter character;
		
		private ICharacter reconciled;
		
		public UpdateUITask(ICharacter character,boolean reconcile) {
			if (character == null) {
				throw new IllegalArgumentException("character cannot be NULL");
			}
			this.character = character;
			this.reconcile = reconcile;
		}
		
		@Override
		public void beforeExecution() {
			if ( reconcile ) {
				setLabel("Fetching character data...");
			}
		}
		
		@Override
		public String getId() {
			return "update_character_sheet_"+character.getCharacterId().getValue();
		}

		@Override
		public void run() throws Exception {

			if ( reconcile ) {
				
				final CharacterID id =
					character.getCharacterId();
				
				displayStatus("Fetching character data for "+character.getName());
				
				userAccountStore.reconcile( id);
				
				reconciled = userAccountStore.getAccountByCharacterID( id ).getCharacterByID( id );
			} else {
				reconciled = character;	
			}
		}
		
		@Override
		public void successHook() throws Exception {
			final CharacterDetails details = reconciled.getCharacterDetails();
			
			final StringBuilder result = new StringBuilder();
			
			result.append("Name: "+reconciled.getName()+" \n\n" );
			result.append("Race: "+details.getRace() +"\n");
			result.append("Corp: "+details.getCorporation().getName()+"\n\n");
			
			final float realAmount =
				details.getBalance() / 100.0f;
			
			result.append("Account balance: "+CurrencyHelper.amountToString( realAmount )+"\n\n" );
			
			final int currentSkillpoints =
				reconciled.getSkillpoints();
			
			result.append("Current skillpoints: "+
					Skill.skillPointsToString( currentSkillpoints )+" SP");
			
			final int cloneSkillpoints =
					details.getCloneSkillPoints();
			
			if ( cloneSkillpoints < currentSkillpoints ) {
				result.append("  [ no/insufficient clone !!! ]\n\n");
			} else {
				result.append("  [ "+Skill.skillPointsToString( cloneSkillpoints) +" protected ]\n\n");
			}
			
			// base attributes
			
			result.append("\n------------- Attributes -------------\n\n");
			
			final Attributes attrs = reconciled.getAttributes();
			final AttributeType[] types = AttributeType.values();
			
			final ImplantSet implants =
				reconciled.getImplantSet();
			
			for ( int i = 0 ; i < types.length ; i++ ) {
				
				final AttributeEnhancer enhancer =
					implants.getAttributeEnhancer( types[i] );
				
				final String implant;
				if ( enhancer != null ) {
					implant = " [ +"+ implants.getAttributeModifier( types[i] )+" ]";
				} else {
					implant = "";
				}
				result.append( rightPad( types[i].getDisplayName() ) +" : "+attrs.getBaseValue( types[i] ) + implant);
				result.append("\n");
			}
			
			final SkillTree tree = dataModelProvider.getStaticDataModel().getSkillTree();
			
			result.append("\n------------- Effective attributes -------------\n\n");
			
			for ( int i = 0 ; i < types.length ; i++ ) {
				result.append( rightPad( types[i].getDisplayName() )+" : "+
						attrs.getEffectiveAttributeValue( tree , types[i] ) +"\n");
			}
			setLabel( result.toString() );
		}
		
		@Override
		public void failureHook(Throwable t) throws Exception {
			setLabel("Failed to query API server: "+t.getMessage() );
		}
		
	}

	protected static String rightPad(String s) {
		return StringUtils.rightPad( s , 20 );
	}
}
