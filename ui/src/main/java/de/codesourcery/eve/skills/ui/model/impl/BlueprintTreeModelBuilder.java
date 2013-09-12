package de.codesourcery.eve.skills.ui.model.impl;

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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;

import org.springframework.dao.EmptyResultDataAccessException;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.db.dao.IStaticDataModelProvider;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;
import de.codesourcery.eve.skills.ui.components.IDoubleClickSelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.model.AbstractViewFilter;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.SelectionListenerHelper;

/**
 * Component that lets the user choose a specific blueprint from
 * a tree view of all available blueprints.
 *
 * <pre>
 * The list of displayed blueprints may be filtered by applying 
 * a filter ( see {@link #setBlueprintFilter(IBlueprintFilter)} ). 
 * Note that you may add a {@link IDoubleClickSelectionListener} instead
 * of a {link ISelectionListener} to intercept double-clicks. A custom
 * context menu may be displayed by using {@link #setPopupMenuBuilder(PopupMenuBuilder)}.
 * 
 * This class lazily populates the tree view (because of 
 * the high cost involved in populating a {@link Blueprint} instance
 * with all required data). Lazy fetching might render
 * unexpanded nodes as having children that actually have none.
 * </pre>
 * @author tobias.gierke@code-sourcery.de
 * @see #addSelectionListener(ISelectionListener)
 * @see IDoubleClickSelectionListener
 */
public class BlueprintTreeModelBuilder {
	
	private final SelectionListenerHelper<Blueprint> listenerHelper =
		new SelectionListenerHelper<Blueprint>();
	
	private final MarketGroupTreeModelBuilder treeBuilder;

	public BlueprintTreeModelBuilder(final IStaticDataModelProvider dataModelProvider) 
	{
		treeBuilder = new MarketGroupTreeModelBuilder( dataModelProvider.getStaticDataModel()) 
		{
			@Override
			protected List<InventoryType> getMembers(MarketGroup group) 
			{
				final List<InventoryType> result = new ArrayList<>();
				
				// step 1: find all items in this market group
				final List<InventoryType> candidates = dataModelProvider.getStaticDataModel().getInventoryTypes( group );
				for ( InventoryType item : candidates ) 
				{
					final Blueprint  bp;
					try {
						bp = dataModelProvider.getStaticDataModel().getBlueprintByProduct( item );
						result.add( bp.getType().getBlueprintType() );
					} catch(EmptyResultDataAccessException e) {
						// ok, no blueprint available
					}
				}
				return result;
			}
		};
		treeBuilder.setTreeFilter(  new AbstractViewFilter<ITreeNode>() {

			@Override
			public boolean isHiddenUnfiltered(ITreeNode node)
			{
				
				// never hide the root node 
				// (otherwise the whole tree will always be hidden)
				if ( node.getParent() == null ) {
					return false;
				}
				
				return false;
			}
		} );
	}
	
	/**
	 * Add selection listener.
	 * 
	 * Note that you may also add {@link IDoubleClickSelectionListener}s
	 * here.
	 * @param l
	 */
	public void addSelectionListener(ISelectionListener<Blueprint> l) {
		listenerHelper.addSelectionListener( l );
	}
	
	public void removeSelectionListener(ISelectionListener<Blueprint> l) {
		listenerHelper.removeSelectionListener( l );
	}

	public void attach(JTree tree) {
		this.treeBuilder.attach( tree );
	}

	public void dispose()
	{
		this.treeBuilder.dispose();
	}	
}