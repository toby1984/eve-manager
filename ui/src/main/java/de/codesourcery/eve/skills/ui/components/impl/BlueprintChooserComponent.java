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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Resource;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.db.dao.IStaticDataModelProvider;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;
import de.codesourcery.eve.skills.production.IBlueprintLibrary;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.IDoubleClickSelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.model.impl.BlueprintTreeModelBuilder;
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
public class BlueprintChooserComponent extends AbstractComponent {

	@Resource(name = "datamodel-provider")
	private IStaticDataModelProvider dataModelProvider;

	@Resource(name="blueprint-library")
	private IBlueprintLibrary blueprintLibrary;

	private final SelectionListenerHelper<Blueprint> listenerHelper =
		new SelectionListenerHelper<Blueprint>();

	private final BlueprintTreeModelBuilder treeModelBuilder;
	private ISelectionProvider<ICharacter>  charProvider;

	private final JTree tree = new JTree();
	private PopupMenuBuilder popupMenuBuilder;

	public BlueprintChooserComponent() {
		this.treeModelBuilder = new BlueprintTreeModelBuilder( this.dataModelProvider );
		this.treeModelBuilder.attach( tree );
	}
	
	public void setCharacterProvider(ISelectionProvider<ICharacter>  charProvider) {
		if ( charProvider == null ) {
			throw new IllegalArgumentException("charProvider cannot be NULL");
		}
		this.charProvider = charProvider;
	}

	@Override
	protected JPanel createPanel() {

		JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );

		// add tree
		tree.setRootVisible(false);

		tree.getSelectionModel().addTreeSelectionListener(
				new TreeSelectionListener() {

					@Override
					public void valueChanged(TreeSelectionEvent e) {
						treeNodeSelected( e.getPath() );
					}
				}
		);

		tree.addMouseListener( new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if ( e.getClickCount() != 2 ) {
					return;
				}

				final int row = tree.getRowForLocation( e.getX(), e.getY() );

				if ( row == -1 ) {
					return;
				}

				final TreePath selectionPath = tree.getSelectionPath();

				final TreePath path =
					tree.getPathForRow( row );

				if ( path == null ) {
					return;
				}

				// tree expansion is also triggered by a double-click,
				// make sure the double-click wasn't just an expansion trigger
				if ( ObjectUtils.equals( selectionPath , path ) ) {
					treeNodeDoubleClicked( path );
				}
			}
		} );
		tree.setCellRenderer(new BlueprintTreeRendererer());		

		panel.add( new JScrollPane( tree ) , constraints().resizeBoth().end() );
		return panel;
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

	/**
	 * Sets the context menu builder to be used.
	 * 
	 * @param builder Popup menu builder, not <code>null</code>.
	 * @see #removePopupMenuBuilder();
	 * @throws IllegalStateException If called while the component
	 * is not in detached state
	 */
	public void setPopupMenuBuilder(PopupMenuBuilder builder) 
	{
		assertDetached();

		removePopupMenuBuilder();

		this.popupMenuBuilder = builder;
		if ( this.popupMenuBuilder != null ) {
			builder.attach( tree );
		}
	}

	/**
	 * Removes any currently attached context menu builder.
	 * 
	 * It's safe to call this method when
	 * no context menu builder is currently set.
	 * 
	 * @throws IllegalStateException If called while the component
	 * is not in detached state
	 * @see #setPopupMenuBuilder(PopupMenuBuilder)	 
	 */
	public void removePopupMenuBuilder() {

		assertDetached();

		if ( this.popupMenuBuilder != null ) {
			this.popupMenuBuilder.detach( tree );
			this.popupMenuBuilder = null;
		}
	}

	public Blueprint getCurrentlySelectedBlueprint() {
		final TreePath path = tree.getSelectionPath();
		if (path != null && path.getPathCount() > 0) {
			return getSelectedBlueprint((ITreeNode) path.getLastPathComponent());
		}
		return null;
	}

	@Override
	protected void disposeHook() {
		this.treeModelBuilder.dispose();
	}

	public Blueprint getSelectedBlueprint(ITreeNode node) {
		if (node.getValue() instanceof InventoryType) {
			return dataModelProvider.getStaticDataModel().getBlueprint( (InventoryType) node.getValue() );
		}
		return null;
	}

	protected void treeNodeDoubleClicked(TreePath path) {

		final ITreeNode node = (ITreeNode) path.getLastPathComponent();
		final Blueprint bp = getSelectedBlueprint(node);
		if ( bp == null ) {
			return;
		}

		for ( ISelectionListener<Blueprint> l : listenerHelper.getListeners() ) 
		{
			if ( l instanceof IDoubleClickSelectionListener<?> ) {
				((IDoubleClickSelectionListener<Blueprint>) l).doubleClicked( bp );
			}
		}
	}

	protected void treeNodeSelected(TreePath path) {

		final ITreeNode node = (ITreeNode) path.getLastPathComponent();

		final Blueprint bp = getSelectedBlueprint(node);
		if (bp != null) {
			listenerHelper.selectionChanged( bp );
		}
	}

	private final class BlueprintTreeRendererer extends DefaultTreeCellRenderer {
		
		@Override
		public Component getTreeCellRendererComponent(JTree tree,
				Object current, boolean sel, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {

			super.getTreeCellRendererComponent(tree, current, sel, expanded,
					leaf, row, hasFocus);

			final Object value = ((ITreeNode) current).getValue();
			if (value instanceof MarketGroup) 
			{
				setText( ((MarketGroup) value).getName() );
			}
			else if (value instanceof InventoryType) 
			{
				final Blueprint bp = getSelectedBlueprint( (ITreeNode) current );
				if ( bp.getTechLevel() != 1 ) {
					setText( bp.getName()+" [ Tech"+bp.getTechLevel()+" ]" );
				} else {
					setText( bp.getName() );
				}

				if ( ! sel ) 
				{
					final ICharacter character = charProvider != null ? charProvider.getSelectedItem() : null;
					if ( character != null ) {
						boolean charOwnsBlueprint = blueprintLibrary.ownsBlueprint(character , bp );

						if ( charOwnsBlueprint ) {
							setForeground( Color.GREEN );
						} 
					}
				} 
			}
			return this;
		}
	}
}