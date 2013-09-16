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
package de.codesourcery.eve.skills.ui.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;
import de.codesourcery.eve.skills.ui.model.DefaultTreeModel;
import de.codesourcery.eve.skills.ui.model.DefaultTreeNode;
import de.codesourcery.eve.skills.ui.model.FilteringTreeModel;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.model.ITreeNode.ITreeNodeVisitor;
import de.codesourcery.eve.skills.ui.model.IViewFilter;
import de.codesourcery.eve.skills.ui.model.LazyTreeNode;

public class MarketGroupTreeModelBuilder 
{
	private final IStaticDataModel dataModel;
	private FilteringTreeModel treeModel;
	private JTree tree;
	
	public MarketGroupTreeModelBuilder(IStaticDataModel dataModel)	{
		if (dataModel == null) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.dataModel = dataModel;
	}
	
	private FilteringTreeModel getTreeModel() {
		if ( treeModel == null ) {
			treeModel = createTreeModel(false);
		}
		return treeModel;
	}
	
	public void dispose() {
		if ( treeModel != null ) {
			treeModel.dispose();
		}
	}
	
	public void viewFilterChanged(final boolean populateAllTreeNodes,final boolean expandAllPaths) 
	{
		System.out.println("viewFilterChanged(): fetchAll = "+populateAllTreeNodes+" , expandPaths = "+expandAllPaths);
		
		// rebuild tree by re-populating all
		// lazy nodes that have already been fetched
		
		if ( treeModel != null ) {
			treeModel.dispose();
		}
		treeModel = createTreeModel( true );
		tree.setModel( treeModel );
		
		if ( expandAllPaths ) 
		{
			final ITreeNode root = (ITreeNode) treeModel.getRoot();
			
			final List<ITreeNode> toExpand = new ArrayList<>();
			root.visitInOrder( new ITreeNodeVisitor() {

				@Override
				public boolean visit(ITreeNode node) 
				{
					if ( node.getValue() instanceof MarketGroup && node.hasChildren() ) 
					{
						boolean expand = true;
						for( ITreeNode child : node.getChildren() ) {
							if ( ! (child.getValue() instanceof InventoryType ) ) 
							{
								expand = false;
								break;
							}
						}
						if ( expand ) {
							toExpand.add( node );
						}
					}
					return true;
				}
			});
			for ( ITreeNode node : toExpand ) {
				tree.expandPath( node.getPathToRoot() );
			}
		}
	}
	
	public void setTreeFilter(IViewFilter<ITreeNode> filter) {
		getTreeModel().setViewFilter( filter );
	}
	
	public void attach(JTree tree) 
	{
		if (tree == null) {
			throw new IllegalArgumentException("tree cannot be NULL");
		}
		
		if ( this.tree != null && this.tree != tree ) {
			throw new IllegalStateException("Already attached to "+this.tree);
		}
		
		this.tree = tree;
		this.tree.setModel( getTreeModel() );
		
		this.tree.addTreeWillExpandListener( new TreeWillExpandListener() {
			
			@Override
			public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException 
			{
				final ITreeNode node = (ITreeNode) event.getPath().getLastPathComponent();
				fetchChildren( node , false );
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException { }
		});
	}
	
	public ITreeNode getTreeNodeFor(MarketGroup group) {
		
		final ITreeNode rootNode = (ITreeNode) getTreeModel().getRoot();

		final ITreeNode result = findMarketGroupNode( rootNode , group );
		
		if ( result != null ) {
			// make sure children are fetched
			fetchChildren( result , false );
			return result;
		}
		throw new RuntimeException("Unable to find tree node for group "+group);
	}
	
	private boolean contains(ITreeNode node, MarketGroup group) {
		return node.getValue() instanceof MarketGroup && ((MarketGroup) node.getValue()).getId().equals( group.getId() );
	}
	
	private ITreeNode findMarketGroupNode(ITreeNode current , MarketGroup group) {
		
		if ( contains( current , group ) ) {
			return current;
		}
		
		for ( ITreeNode child : current.getChildren() ) {
			final ITreeNode result = findMarketGroupNode( child , group );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
	
	private void fetchChildren( ITreeNode node , boolean force)
	{
		if ( ! ( node instanceof LazyTreeNode) ) {
			return;
		}
		
		final LazyTreeNode lazyNode = (LazyTreeNode) node;
		if ( ( !force && lazyNode.childrenFetched() ) || ! ( lazyNode.getValue() instanceof MarketGroup) ) {
			return;
		}
		
		// fetch children
		final List<InventoryType> inventoryTypes = getMembers( (MarketGroup) lazyNode.getValue() );
		setChildren(lazyNode, inventoryTypes);
	}

	private void setChildren(final LazyTreeNode lazyNode,final Collection<InventoryType> input) 
	{
		final List<InventoryType> inventoryTypes = new ArrayList<>(input);
		Collections.sort( inventoryTypes , InventoryType.BY_NAME_COMPARATOR );
		
		lazyNode.removeChildren();
		
		for ( InventoryType item : inventoryTypes ) {
			lazyNode.addChild( new DefaultTreeNode( item ) );
		}
		lazyNode.setChildrenFetched();
	}
	
	protected List<InventoryType> getMembers(MarketGroup group) {
		return dataModel.getInventoryTypes( group );
	}

	private FilteringTreeModel createTreeModel(boolean populateAllNodes) 
	{
		long time = -System.currentTimeMillis();
		
		final IdentityHashMap<MarketGroup,ITreeNode> nodes = new IdentityHashMap<MarketGroup,ITreeNode> ();
		
		// construct tree
		final List<MarketGroup> marketGroups = dataModel.getLeafMarketGroups();
		System.out.println("createTreeModel( populateAll = "+populateAllNodes+"): Filtering "+marketGroups.size()+" leaf market groups");
		
//		int debugCount=0;
		for ( MarketGroup marketGroup : marketGroups ) 
		{
//			System.out.print(".");
//			if ( (debugCount++ % 60 ) == 0 ) {
//				System.out.println();
//			}
			
			final ITreeNode node = getOrCreateTreeNode(marketGroup , nodes );			
			if ( populateAllNodes ) 
			{
				final List<InventoryType> members = getMembers( marketGroup );
				
				if ( ! members.isEmpty() ) {
					for ( InventoryType type : members ) {
						node.addChild( new DefaultTreeNode( type ) );
					}
				} else {
					nodes.remove( marketGroup );
					continue;
				}
			}
			
			if ( marketGroup.getParent() != null ) 
			{
				MarketGroup current = marketGroup;
				while ( current != null )
				{
					final ITreeNode toAdd = getOrCreateTreeNode( current  , nodes );
					if ( current.getParent() != null ) 
					{
						ITreeNode parent = getOrCreateTreeNode( current.getParent() , nodes );
						boolean add = true;
						for ( ITreeNode child : parent.getChildren() ) {
							if ( ObjectUtils.equals( child.getValue() , current ) ) {
								add = false;
								break;
							}
						}
						if ( add ) {
							parent.addChild( toAdd );
						}
					}
					current = current.getParent();
				}
			}
		}
		
		System.out.println("createTreeModel( populateAll = "+populateAllNodes+"): Initial tree creation took "+(time+System.currentTimeMillis())+" ms");
		
		final ITreeNode root =  new DefaultTreeNode();
		// convert all nodes without children to LazyTreeNode instances 
		for ( ITreeNode node : nodes.values() ) 
		{
			final MarketGroup g = (MarketGroup) node.getValue();
			if ( g.getParent() == null ) { // top-level market group, add to root node
				root.addChild( wrapIfLeafNode( node ) );
			} else {
				wrapIfLeafNode( node );
			}
		}

		final FilteringTreeModel model =  new FilteringTreeModel( new DefaultTreeModel( root ) );
		
		// sort tree nodes alphabetically
		final Comparator<ITreeNode> COMPARATOR = new Comparator<ITreeNode>() {
			@Override
			public int compare(ITreeNode o1, ITreeNode o2) 
			{
				if ( o1.getValue() instanceof MarketGroup && o2.getValue() instanceof MarketGroup) {
					final MarketGroup g1 = (MarketGroup) o1.getValue();
					final MarketGroup g2 = (MarketGroup) o2.getValue();
					return g1.getName().compareTo( g2.getName() );
				} 
				else if ( o1.getValue() instanceof InventoryType && o2.getValue() instanceof InventoryType) 
				{
					final InventoryType g1 = (InventoryType) o1.getValue();
					final InventoryType g2 = (InventoryType) o2.getValue();
					return g1.getName().compareTo( g2.getName() );
				}
				throw new RuntimeException("Internal error,unhandled node values: "+o1.getValue()+" / "+o2.getValue() );
			}
		};		
		model.sortChildren( root , COMPARATOR , true );
		
		time += System.currentTimeMillis();
		System.out.println("createTreeModel( populateAll = "+populateAllNodes+") took "+time+" ms");
		return model;
	}
	
	protected static ITreeNode wrapIfLeafNode(ITreeNode nodeToBeWrapped) 
	{
		if ( nodeToBeWrapped.getChildCount() == 0 ) 
		{
			final LazyTreeNode lazyNode = new LazyTreeNode( nodeToBeWrapped.getValue() );
			if ( nodeToBeWrapped.getParent() != null ) {
				((ITreeNode) nodeToBeWrapped.getParent()).replaceChild( nodeToBeWrapped , lazyNode );
			}
			return lazyNode;
		}  
		return nodeToBeWrapped;
	}

	private ITreeNode getOrCreateTreeNode(MarketGroup group,IdentityHashMap<MarketGroup, ITreeNode> nodes) 
	{
		ITreeNode result = nodes.get( group );
		if ( result == null ) {
			result = new DefaultTreeNode( group );
			nodes.put( group, result );
		}
		return result;
	}
}