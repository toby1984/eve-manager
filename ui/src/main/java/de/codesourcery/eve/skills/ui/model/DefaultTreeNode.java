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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.ObjectUtils;

public class DefaultTreeNode implements ITreeNode
{

    private ITreeNode parent;
    private final List<ITreeNode> children = new ArrayList<ITreeNode>();

    private Object value;

    public DefaultTreeNode() {
    }

    public DefaultTreeNode(Object value) {
        this.value = value;
    }

    public ITreeNode getChildWithValue(Object value)
    {
        for (ITreeNode child : children)
        {
            if ( ObjectUtils.equals( child.getValue(), value ) )
            {
                return child;
            }
        }
        return null;
    }

    @Override
    public void removeChildren()
    {
        children.clear();
    }

    @Override
    public TreePath getPathToRoot()
    {

        final List<ITreeNode> stack = new ArrayList<ITreeNode>();
        ITreeNode current = this;
        do
        {
            stack.add( current );
            current = (ITreeNode) current.getParent();
        } while ( current != null );

        Collections.reverse( stack );
        return new TreePath( stack.toArray( new ITreeNode[stack.size()] ) );
    }

    @Override
    public int[] addChildren(Collection<ITreeNode> n)
    {
        final int[] result = new int[n.size()];
        int firstIndex = children.size();
        children.addAll( n );

        final int len = result.length;
        Iterator<ITreeNode> child = children.iterator();
        for (int i = 0; i < len; i++)
        {
            child.next().setParent( this );
            result[i] = firstIndex++;
        }
        return result;
    }

    @Override
    public Object getValue()
    {
        return value;
    }

    @Override
    public boolean hasValue()
    {
        return value != null;
    }

    @Override
    public void setValue(Object value)
    {
        this.value = value;
    }

    @Override
    public Enumeration<ITreeNode> children()
    {
        final Iterator<ITreeNode> it = children.iterator();
        return new Enumeration<ITreeNode>() {

            @Override
            public boolean hasMoreElements()
            {
                return it.hasNext();
            }

            @Override
            public ITreeNode nextElement()
            {
                return it.next();
            }
        };
    }

    @Override
    public boolean getAllowsChildren()
    {
        return true;
    }

    @Override
    public TreeNode getChildAt(int childIndex)
    {
        return children.get( childIndex );
    }

    @Override
    public int getChildCount()
    {
        return children.size();
    }

    @Override
    public int getIndex(TreeNode node)
    {
        int i = 0;
        for (ITreeNode n : this.children)
        {
            if ( n == node )
            {
                return i;
            }
            i++;
        }
        return - 1;
    }

    @Override
    public ITreeNode getParent()
    {
        return parent;
    }

    @Override
    public boolean isLeaf()
    {
        return children.isEmpty();
    }

    @Override
    public int addChild(ITreeNode n)
    {
        final int index = children.size();
        n.setParent( this );
        children.add( n );
        return index;
    }

    @Override
    public void addChild(ITreeNode n, int index)
    {
        children.add( index, n );
        n.setParent( this );
    }

    @Override
    public int removeChild(ITreeNode child)
    {
        int index = 0;

        for (Iterator<ITreeNode> it = children.iterator(); it.hasNext(); index++)
        {
            if ( it.next() == child )
            {
                it.remove();
                child.setParent( null );
                return index;
            }
        }
        return - 1;
    }

    @Override
    public void setParent(ITreeNode parent)
    {
        this.parent = parent;
    }

    @Override
    public List<ITreeNode> getChildren()
    {
        return children;
    }

    @Override
    public int replaceChild(ITreeNode oldChild, ITreeNode newChild)
    {
        int childIndex = getIndex( oldChild );
        if ( childIndex == - 1 )
        {
            throw new IllegalArgumentException( "Unable to find child " + oldChild
                    + " below " + this );
        }
        children.remove( childIndex );
        oldChild.setParent( null );
        children.add( childIndex, newChild );
        newChild.setParent( this );
        return childIndex;
    }

    @Override
    public String toString()
    {
        return value != null ? value.toString() : "";
    }
}
