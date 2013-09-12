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
package de.codesourcery.eve.skills.ui.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

public class DefaultTreeModel extends AbstractTreeModel
{

    public static final Logger log = Logger.getLogger( DefaultTreeModel.class );

    private ITreeNode rootNode;

    public DefaultTreeModel() {
    }

    @Override
    public void dispose()
    {
        rootNode = null;
    }

    public DefaultTreeModel(ITreeNode root) {
        this.rootNode = root;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        return ( (ITreeNode) parent ).getChildAt( index );
    }

    @Override
    public int getChildCount(Object parent)
    {
        return ( (ITreeNode) parent ).getChildCount();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        return ( (ITreeNode) parent ).getIndex( (ITreeNode) child );
    }

    @Override
    public Object getRoot()
    {
        return this.rootNode;
    }

    @Override
    public boolean isLeaf(Object node)
    {
        return ( (ITreeNode) node ).isLeaf();
    }

    @Override
    public void addChildren(ITreeNode parent, Collection<ITreeNode> nodes)
    {

        final TreePath path = parent.getPathToRoot();

        final int[] indices = parent.addChildren( nodes );

        final TreeModelEvent event =
                new TreeModelEvent( this, path, indices, nodes
                        .toArray( new ITreeNode[nodes.size()] ) );

        fireEvent( EventType.ADDED, event );
    }

    public void modelChanged()
    {
        fireEvent( EventType.STRUCTURE_CHANGED, new TreeModelEvent( this,
                new Object[] { getRoot() } ) );
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public int addChild(ITreeNode parent, ITreeNode n)
    {
        return parent.addChild( n );
    }

    @Override
    public void addChild(ITreeNode parent, ITreeNode n, int index)
    {
        parent.addChild( n, index );
    }

    @Override
    public int removeChild(ITreeNode parent, ITreeNode child)
    {
        return parent.removeChild( child );
    }

    @Override
    public void setRoot(ITreeNode rootNode)
    {
        if ( rootNode == null )
        {
            throw new IllegalArgumentException( "rootNode cannot be NULL" );
        }
        this.rootNode = rootNode;
    }

    @Override
    public void sortChildren(ITreeNode parent, Comparator<ITreeNode> comp,
            boolean recursive)
    {

        final List<ITreeNode> children = getChildren( parent );
        Collections.sort( children, comp );
        if ( recursive )
        {
            for (ITreeNode child : children)
            {
                sortChildren( child, comp, true );
            }
        }
    }

    @Override
    public List<ITreeNode> getChildren(ITreeNode parent)
    {
        return parent.getChildren();
    }

    @Override
    public int replaceChild(ITreeNode parent, ITreeNode oldChild, ITreeNode newChild)
    {
        final int result = parent.replaceChild( oldChild, newChild );

        final TreeModelEvent event = new TreeModelEvent( this, parent.getPathToRoot() );

        fireEvent( EventType.STRUCTURE_CHANGED, event );

        return result;
    }

    public void structureChanged(ITreeNode startNode)
    {
        final TreeModelEvent event = new TreeModelEvent( this, startNode.getPathToRoot() );

        fireEvent( EventType.STRUCTURE_CHANGED, event );
    }

    public void nodeValueChanged(ITreeNode jobNode)
    {

        TreePath path = jobNode.getPathToRoot();

        if ( path.getPathCount() == 1 )
        {
            fireEvent( EventType.CHANGED, new TreeModelEvent( this, path ) );
        }
        else
        {
            path = path.getParentPath();
            final int[] childIndices =
                    new int[] { ( (ITreeNode) path.getLastPathComponent() )
                            .getIndex( jobNode ) };
            final Object[] children = new Object[] { jobNode };
            fireEvent( EventType.CHANGED, new TreeModelEvent( this, path, childIndices,
                    children ) );
        }

    }

}
