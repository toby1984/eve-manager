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
import java.util.Collections;
import java.util.List;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.db.datamodel.InventoryCategory;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.ui.model.DefaultTreeModel;
import de.codesourcery.eve.skills.ui.model.DefaultTreeNode;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.model.LazyTreeNode;

public class ItemTreeBuilder {

	private final IStaticDataModel dataModel;
	
	private DefaultTreeModel treeModel;
	private ITreeNode rootNode;

	public ItemTreeBuilder(IStaticDataModel dataModel) {
		if (dataModel == null) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.dataModel = dataModel;
	}
	
	public DefaultTreeModel getTreeModel() {
		if (treeModel == null) {
			treeModel = new DefaultTreeModel(getRoot());
		}
		return treeModel;
	}

	private ITreeNode getRoot() {
		if (rootNode == null) {
			rootNode = createTree();
		}
		return rootNode;
	}

	private ITreeNode createTree() {

		final ITreeNode root = new DefaultTreeNode();

		final List<InventoryCategory> categories = new ArrayList<InventoryCategory>(
				dataModel.getInventoryCategories());

		Collections.sort(categories, InventoryCategory.BY_NAME_COMPARATOR);

		for (InventoryCategory cat : categories) {

			if (!cat.isPublished()) {
				continue;
			}

			final ITreeNode catNode = new DefaultTreeNode(cat);

			root.addChild(catNode);

			final List<InventoryGroup> groups = new ArrayList<InventoryGroup>(
					dataModel.getInventoryGroups(cat));

			Collections.sort(groups, InventoryGroup.BY_NAME_COMPARATOR);

			for (InventoryGroup group : groups) {
				if (group.isPublished()) {
					catNode.addChild(new LazyTreeNode(group));
				}
			}
		}
		return root;
	}
}