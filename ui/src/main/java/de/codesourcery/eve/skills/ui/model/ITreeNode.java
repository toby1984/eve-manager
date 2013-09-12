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
import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public interface ITreeNode extends TreeNode
{

    public List<ITreeNode> getChildren();

    public int addChild(ITreeNode n);

    public int[] addChildren(Collection<ITreeNode> n);

    public void addChild(ITreeNode n, int index);

    public int replaceChild(ITreeNode oldChild, ITreeNode newChild);

    public int removeChild(ITreeNode child);

    public void removeChildren();

    public void setParent(ITreeNode parent);

    public Object getValue();

    public boolean hasValue();

    public ITreeNode getChildWithValue(Object value);

    public void setValue(Object value);

    /**
     * Returns the path from the tree root to this node.
     * 
     * The current node is the last component in the tree path.
     * 
     * @return
     */
    public TreePath getPathToRoot();

}
