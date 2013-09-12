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
import java.text.DecimalFormat;
import java.util.Collections;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.model.impl.MarketGroupTreeModelBuilder;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;


public class ItemBrowserComponent extends AbstractComponent implements ICharacterSelectionProviderAware {

	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;
	
	private MarketGroupTreeModelBuilder treeModelBuilder;
	private final JTree itemTree = new JTree();
	
	private final JTextArea itemDetails =
		new JTextArea( 20 , 60 ); 
	
	private ISelectionProvider<ICharacter> selectionProvider;
	
	public ItemBrowserComponent() 
	{
		super();
		itemTree.setRootVisible( false );
	}
	
	@Override
	public String getTitle() {
		return "Item browser";
	}
	
	protected void treeSelectionChanged(ITreeNode newSelection) {
		
		if ( newSelection == null ) {
			itemDetails.setText(null);
			return;
		}
		
		if ( ! ( newSelection.getValue() instanceof InventoryType) ) {
			return;
		}
		
		itemDetails.setText( getDetailsFor( (InventoryType) newSelection.getValue() ) );
	}
	
	protected String getDetailsFor(InventoryType type) 
	{
		final StringBuffer result =
			new StringBuffer();
		
		final DecimalFormat VOLUME_FORMAT =
			new DecimalFormat("###,###,##0.0#");
	
		result.append( type.getDescription() );
		result.append("\n\nVolume: "+VOLUME_FORMAT.format( type.getVolume() )+" m3" );
		return result.toString();
	}
	
	@Override
	protected JPanel createPanel() {
		
		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );
		
		treeModelBuilder = new MarketGroupTreeModelBuilder( dataModel );
		
		treeModelBuilder.attach( itemTree );
		
		itemTree.getSelectionModel().addTreeSelectionListener( new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if ( e.getPath() != null ) {
					treeSelectionChanged( (ITreeNode) e.getPath().getLastPathComponent() );
				} else {
					treeSelectionChanged( null );
				}
			}
		});
		
		itemTree.setCellRenderer( new DefaultTreeCellRenderer() 
		{
			public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) 
			{
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				final ITreeNode node = (ITreeNode) value;
				if ( node.getValue() instanceof MarketGroup) {
					setText( ((MarketGroup) node.getValue() ).getName() );
				} else if ( node.getValue() instanceof InventoryType ) {
					setText( ((InventoryType) node.getValue() ).getName() );
				}
				return this;
			};
		} );
		
		// text area
		itemDetails.setLineWrap( true );
		itemDetails.setWrapStyleWord( true );
		itemDetails.setEditable( false );
		
		// context menu
		final PopupMenuBuilder menuBuilder = new PopupMenuBuilder();
		menuBuilder.addItem("Refine..." , new AbstractAction() {

			@Override
			public boolean isEnabled()
			{
				return getSelectedType() != null && selectionProvider.getSelectedItem() != null ;
			}
			
			private InventoryType getSelectedType() {
				final TreePath selection = itemTree.getSelectionPath();
				if ( selection != null && selection.getPathCount() > 0 ) {
					final ITreeNode node = (ITreeNode) selection.getLastPathComponent();
					if ( node.getValue() instanceof InventoryType ) {
						return (InventoryType) node.getValue();
					}
				}
				return null;
			}
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final ICharacter currentCharacter = selectionProvider.getSelectedItem();
				final InventoryType t = getSelectedType();
				if ( t != null && currentCharacter != null ) {
					
					final RefiningComponent comp = new RefiningComponent( currentCharacter );
					comp.setModal( true );
					comp.setItemsToRefine( 
						Collections.singletonList( 
							new ItemWithQuantity( getSelectedType() , 1 ) 
						) 
					);
					ComponentWrapper.wrapComponent( comp ).setVisible( true );
				}
				
			}
		} );
		menuBuilder.attach( itemTree );
		
		result.add( new JScrollPane( itemTree ) , constraints(0,0).resizeBoth().end() );
		result.add( new JScrollPane( itemDetails ) , constraints(1,0).resizeBoth().end() );
		return result;
	}

	@Override
	public void setSelectionProvider(ISelectionProvider<ICharacter> provider)
	{
		assertDetached();
		if ( provider == null ) {
			throw new IllegalArgumentException("selection provider cannot be NULL");
		}
		this.selectionProvider = provider;
	}

}
