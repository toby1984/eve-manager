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

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.db.datamodel.AttributeCategory;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.ItemWithAttributes;
import de.codesourcery.eve.skills.db.datamodel.ItemAttribute;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.model.impl.MarketGroupTreeModelBuilder;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.utils.StringTablePrinter;


public class ItemBrowserComponent extends AbstractComponent implements ICharacterSelectionProviderAware {

	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;
	
	private MarketGroupTreeModelBuilder treeModelBuilder;
	private final JTree itemTree = new JTree();
	
	private final JTextArea itemDetails = new JTextArea( 20 , 60 ); 
	
	private ISelectionProvider<ICharacter> selectionProvider;
	
	public ItemBrowserComponent() 
	{
		super();
		itemTree.setRootVisible( false );
		
	  	final Font currFont = itemDetails.getFont();
    	itemDetails.setFont(new Font("monospaced", currFont.getStyle(), currFont.getSize()));		
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
		final StringBuffer result = new StringBuffer();
		
		final DecimalFormat VOLUME_FORMAT = new DecimalFormat("###,###,##0.0#");
	
		result.append( type.getDescription() );
		result.append("\n\nVolume: "+VOLUME_FORMAT.format( type.getVolume() )+" m3\n\n" );
		
		final ItemWithAttributes item = dataModel.getItem( type );
		
		List<Prerequisite> requiredSkills = item.getRequiredSkills( dataModel );
		if ( ! requiredSkills.isEmpty() ) 
		{
			result.append("Required skills:\n\n");
			
			StringTablePrinter printer = new StringTablePrinter("Skill","Level","Your level");
			ICharacter currentChar = selectionProvider.getSelectedItem();
			for (Iterator<Prerequisite> it = requiredSkills.iterator(); it.hasNext();) 
			{
				final Prerequisite prerequisite = (Prerequisite) it.next();
				String yourLevel = "--";
				if ( currentChar != null ) {
					yourLevel = Integer.toString( currentChar.getCurrentLevel( prerequisite.getSkill() ) );
				}
				printer.add( prerequisite.getSkill().getName() , Integer.toString( prerequisite.getRequiredLevel() ) , yourLevel );
			}
			result.append( printer );
		}
		
		final Map<AttributeCategory, List<ItemAttribute>> allCategories = item.getAttributes().getAttributesByCategory();
		final List<AttributeCategory> categories = new ArrayList<>(allCategories.keySet());
		Collections.sort( categories , new Comparator<AttributeCategory>() {

			@Override
			public int compare(AttributeCategory o1, AttributeCategory o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		for (Iterator<AttributeCategory> it = categories.iterator(); it.hasNext();) 
		{
			final AttributeCategory cat = (AttributeCategory) it.next();
			result.append( toString( cat , allCategories.get(cat) ) );	
			if (it.hasNext()) {
				result.append("\n");
			}
		}
		return result.toString();
	}
	
	private String toString(AttributeCategory cat, List<ItemAttribute> attrs) {
		
		int maxNameLen = 0;
		for ( ItemAttribute attr : attrs ) {
			maxNameLen = Math.max(maxNameLen, getAttributeDisplayName( attr ).length() );
		}
		String result = "\n============\n"+
		       cat.getName()+
		       "\n============";
		       
		final List<ItemAttribute> sorted = new ArrayList<>(attrs);
		Collections.sort( sorted , new Comparator<ItemAttribute>() {

			@Override
			public int compare(ItemAttribute o1, ItemAttribute o2) {
				return getAttributeDisplayName( o1).compareTo( getAttributeDisplayName( o2 ) );
			}
		} );
		for (Iterator<ItemAttribute> it = sorted.iterator(); it.hasNext();) {
			ItemAttribute itemAttribute = it.next();
			result += "\n"+StringUtils.rightPad( getAttributeDisplayName(itemAttribute) , maxNameLen)+" : "+getAttributeDisplayValue(itemAttribute);
		}       
		return result;
	}
	
	private String getAttributeDisplayName(ItemAttribute attr) 
	{
		String displayName = attr.getType().getDisplayName();
		String attributeName = attr.getType().getAttributeName();
		if ( displayName == null ) {
			return attributeName;
		}
		return displayName+" ("+attributeName+")";
	}
	
	private String getAttributeDisplayValue(ItemAttribute attr) 
	{
		String sValue = "<no value?>";
		if ( attr.getFloatValue() != null && attr.getIntValue() != null ) {
			sValue = "float: "+attr.getFloatValue()+" | int: "+attr.getIntValue();
		} else if ( attr.getFloatValue() != null ) {
			sValue = Double.toString( attr.getFloatValue() );
		} else if ( attr.getIntValue() != null ) {
			sValue = Integer.toString( attr.getIntValue() );
		}
		return sValue;
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
