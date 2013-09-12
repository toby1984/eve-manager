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
import java.util.Comparator;
import java.util.List;

import javax.swing.tree.TreeModel;

/**
 * Generic mutable tree model.
 * 
 * This tree model takes care  of
 * notifying any registered listeners 
 * whenever the underlying model data is changed.
 *   
 * @author tobias.gierke@code-sourcery.de
 */
public interface ITreeModel extends TreeModel {
	
	public List<ITreeNode> getChildren(ITreeNode parent);
	
	public int addChild(ITreeNode parent,ITreeNode n);
	
	public void addChildren(ITreeNode parent,Collection<ITreeNode> nodes);
	
	public void addChild(ITreeNode parent, ITreeNode n,int index);
	
	public int replaceChild(ITreeNode parent , ITreeNode oldChild, ITreeNode newChild);

	public int removeChild(ITreeNode parent , ITreeNode child);
	
	public void setRoot(ITreeNode rootNode);
	
	/**
	 * This method DOES NOT notify any listeners
	 * of changes to the sorted node(s).
	 * 
	 * @param parent
	 * @param comp
	 * @param recursive
	 */
	public void sortChildren(ITreeNode parent, Comparator<ITreeNode> comp, boolean recursive);
	
	public void modelChanged();

	public void dispose();
	
	/**
	 * Used when a {@link FilteringTreeModel} wants
	 * to control/intercept listener notifications.
	 *  
	 * @param yesNo
	 */
	public void setListenerNotificationEnabled(boolean yesNo);
	
	public void nodeValueChanged(ITreeNode jobNode);
	
}
