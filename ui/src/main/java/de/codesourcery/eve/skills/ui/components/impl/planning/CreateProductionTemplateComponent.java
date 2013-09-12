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
package de.codesourcery.eve.skills.ui.components.impl.planning;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.Decryptor;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;
import de.codesourcery.eve.skills.db.dao.IRegionDAO;
import de.codesourcery.eve.skills.market.IMarketDataProvider;
import de.codesourcery.eve.skills.production.InventionChanceCalculator;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.IComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.nodeeditors.BlueprintEditorComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.nodeeditors.ManufacturingJobEditorComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.nodeeditors.NodeEditorComponent;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.BlueprintNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.CopyJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.INodeValueChangeListener;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.InventionJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ManufacturingJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ProductionPlanNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.RequiredMaterialNode;
import de.codesourcery.eve.skills.ui.config.IAppConfigProvider;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder.IActionWithDisabledText;
import de.codesourcery.eve.skills.util.IStatusCallback;

public class CreateProductionTemplateComponent extends AbstractComponent
{
	private static final Logger log = Logger
	.getLogger(CreateProductionTemplateComponent.class);

	private final JTree tree = new JTree();

	@Resource(name="static-datamodel")
	private IStaticDataModel dataModel;

	@Resource(name="appconfig-provider")
	private IAppConfigProvider appConfigProvider;

	@Resource(name="marketdata-provider")
	private IMarketDataProvider marketDataProvider;

	@Resource(name="region-dao")
	private IRegionDAO regionDAO;

	private final ProductionPlanTemplateTreeBuilder treeBuilder;

	private final CostStatementComponent costStatement =
		new CostStatementComponent();

	private final JPanel editorPanel = new JPanel();
	private IComponent editorComponent;

	/**
	 * Invoked from node editor components after
	 * the user edited a node property.
	 */
	private final INodeValueChangeListener nodeChangeListener = new INodeValueChangeListener() {

		@Override
		public void nodeValueChanged(ITreeNode node)
		{
			CreateProductionTemplateComponent.this.nodeValueChanged(node);
		}
	};

	private final TreeCellRenderer renderer = new DefaultTreeCellRenderer() {

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
					row, hasFocus);

			if ( ! ( value instanceof ITreeNode )) {
				// tree is initially empty and uses the default tree model 
				// which returns DefaultMutableTreeNode instances
				return this;
			}

			setMonospacedFont( this );

			final ITreeNode node = (ITreeNode) value;

			if ( node instanceof InventionJobNode )
			{
				final InventionJobNode inventionNode = (InventionJobNode) node;
				setText( "Invention of "+inventionNode.getQuantity()+"x "+
						inventionNode.getTech2Blueprint().getName()+
						" ( "+inventionNode.getDetails().getNumberOfRuns()+" invention jobs) ");
			}

			else if ( node instanceof CopyJobNode ) 
			{
				final CopyJobNode  job = (CopyJobNode) node;
				setText("Create "+job.getQuantity()+" copies of "+job.getTech1Blueprint().getName() );
			}
			else if ( node instanceof BlueprintNode ) 
			{
				final BlueprintNode bpNode = (BlueprintNode) node;

				final String text; 

				String blueprintName = bpNode.getBlueprint().getName()+
				" ( Tech "+bpNode.getBlueprint().getTechLevel();

				if ( bpNode.isRequiresOriginal() ) {
					blueprintName  += " ORIGINAL )";
				} else if ( bpNode.isRequiresCopy() ) {
					blueprintName  += " copy )";
				} else {
					blueprintName  += " )";
				}

				if ( bpNode.getBlueprint().getTechLevel() == 2 ) {
					text = formatNicely( "*  "+blueprintName , bpNode.getQuantity() );
				} else {
					text = formatNicely( blueprintName , bpNode.getQuantity() );
				}
				setText( text);
			}
			else if ( node instanceof ManufacturingJobNode ) 
			{
				final ManufacturingJobRequest request = 
					( (ManufacturingJobNode) node).getManufacturingJobRequest();

				setText( "Production of "+request.getQuantity()+"x  "+
						request.getBlueprint().getProductType().getName() 
				);
			} 
			else if ( node instanceof RequiredMaterialNode ) {
				setText( formatNicely( (RequiredMaterialNode) node ) );
			} 

			return this;
		}

		private String formatNicely( RequiredMaterialNode requiredMaterialNode )
		{
			final RequiredMaterial material = requiredMaterialNode.getRequiredMaterial();

			if ( requiredMaterialNode.canBeProduced() ) {
				return formatNicely( "*  "+material.getType().getName(), requiredMaterialNode.getQuantity() );
			}
			return formatNicely( material.getType().getName(), requiredMaterialNode.getQuantity() );
		}

		private String formatNicely(String itemName , double quantity ) {
			return StringUtils.rightPad( itemName.trim() , 40 ) +
			StringUtils.leftPad( ""+( (long) quantity)  , 15 );
		}
	};

	public CreateProductionTemplateComponent() {
		super();
		this.treeBuilder = new ProductionPlanTemplateTreeBuilder( this.dataModel );
	}

	protected void nodeValueChanged(ITreeNode node)
	{
		System.out.println("======> Node value edited: "+node.getValue()+ " <======= " );
		this.treeBuilder.nodeValueChanged( (ProductionPlanNode) node );
		costStatement.refresh();
	}

	private final class DeleteSubPlanAction extends AbstractAction {

		private ManufacturingJobNode selection;

		@Override
		public boolean isEnabled()
		{

			final ITreeNode currentSelection = 
				getCurrentlySelectedTreeNode() ;

			if ( currentSelection.getParent() == null ) {
				// cannot delete top-level production plan
				return false;
			}

			final boolean result =
				currentSelection instanceof ManufacturingJobNode;

			selection = ( currentSelection instanceof  ManufacturingJobNode) ?
					(ManufacturingJobNode) currentSelection : null;
			return result;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{ 
			if ( selection == null ) { // should never happen...
				return;
			}

			treeBuilder.convertToRequiredMaterial( selection );
			costStatement.refresh();
		}
	}

	private final class DeleteInventionJobAction extends AbstractAction {

		private InventionJobNode jobNode;

		@Override
		public boolean isEnabled()
		{

			jobNode = null;

			final ITreeNode currentSelection = 
				getCurrentlySelectedTreeNode() ;

			if ( ! ( currentSelection instanceof InventionJobNode ) ) {
				return false;
			}

			jobNode = (InventionJobNode) currentSelection;
			return true;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			treeBuilder.convertToRequiredBlueprint( jobNode );
			costStatement.refresh();
		}

	}

	private final class CreateInventionJobAction extends AbstractAction implements IActionWithDisabledText{

		private Blueprint tech1Blueprint; 
		private boolean tech1BlueprintExists;
		private BlueprintNode tech2BlueprintNode;

		@Override
		public boolean isEnabled()
		{

			tech1Blueprint = null;
			tech1BlueprintExists = false;
			tech2BlueprintNode = null;

			final ITreeNode currentSelection = 
				getCurrentlySelectedTreeNode() ;

			if ( ! ( currentSelection instanceof BlueprintNode ) ) {
				return false;
			}

			tech2BlueprintNode = (BlueprintNode) currentSelection;

			if ( tech2BlueprintNode.getBlueprint().getTechLevel() != 2 ) {
				tech2BlueprintNode=null;
				return false;
			}

			try {
				tech1Blueprint = 
					getT1BlueprintForInvention( tech2BlueprintNode.getBlueprint() );
			} 
			catch(EmptyResultDataAccessException e) {
				return false;
			}

			tech1BlueprintExists = true;
			final boolean result =
				canBeSuccessfullyUsedForInvention( tech1Blueprint );

			return result;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if ( tech1Blueprint == null ) {
				return; 
			}

			treeBuilder.convertToInventionJob( tech2BlueprintNode , tech1Blueprint );
			costStatement.refresh();
		}

		@Override
		public String getDisabledText()
		{
			if ( tech2BlueprintNode == null ) {
				return null;
			}

			if ( ! tech1BlueprintExists ) {
				return "Found no Tech1 blueprint to use for invention of "+tech2BlueprintNode.getBlueprint().getName();
			}
			return "You currently lack the skills to successfully invent "+tech2BlueprintNode.getBlueprint().getName();
		}

	}

	private final class CreateSubPlanAction extends AbstractAction {

		private RequiredMaterialNode selection;

		@Override
		public boolean isEnabled()
		{

			final ITreeNode currentSelection = 
				getCurrentlySelectedTreeNode() ;

			final boolean result =
				canBeManufactured( currentSelection );

			selection = ( currentSelection instanceof  RequiredMaterialNode) ?
					(RequiredMaterialNode) currentSelection : null;
					return result;
		}

		protected boolean canBeManufactured(ITreeNode node) 
		{
			if ( node == null ) {
				return false;
			}

			if ( node instanceof RequiredMaterialNode ) {
				if ( ((RequiredMaterialNode) node).canBeProduced() ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{ 
			if ( selection == null ) { // should never happen...
				return;
			}

			// find parent job request
			ManufacturingJobRequest parentJob=null;
			ITreeNode current = selection;
			do {
				if ( current instanceof ManufacturingJobNode ) {
					parentJob = ((ManufacturingJobNode) current).getManufacturingJobRequest();
					break;
				}
				current = (ITreeNode) current.getParent();
			} while ( current != null );

			if ( parentJob == null ) {
				throw new RuntimeException("Internal error - found no parent job for "+selection);
			}
			treeBuilder.convertToManufacturingJob( parentJob , selection );
			
			costStatement.refresh();
		}
	}

	/**
	 * Returns whether the currently active
	 * character has a chance of successfully
	 * inventing a blueprint (invention chance > 0).
	 * 
	 * @param tech1Blueprint
	 * @return
	 */
	protected boolean canBeSuccessfullyUsedForInvention(Blueprint tech1Blueprint) {

		final InventionChanceCalculator calculator =
			new InventionChanceCalculator( dataModel );

		final double chance =
			calculator.calculateInventionChance( tech1Blueprint,
					treeBuilder.getJobRequest().getCharacter(), 0 , Decryptor.NONE );

		return chance > 0.0d;
	}

	@Override
	protected JPanel createPanel()
	{
		tree.setRootVisible( true );
		tree.setCellRenderer( renderer );
		tree.setModel( this.treeBuilder.getTreeModel() );

		final PopupMenuBuilder menuBuilder = 
			new PopupMenuBuilder()
		.addItem( "Produce" , new CreateSubPlanAction() )
		.addItem( "Buy on market" , new DeleteSubPlanAction() )
		.addItem( "Buy on market", new DeleteInventionJobAction() )
		.addItem( "Invent from T1 blueprint" , new CreateInventionJobAction( ) );

		menuBuilder.attach( tree );

		tree.getSelectionModel().addTreeSelectionListener( new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				final TreePath selectedPath = e.getPath();
				if ( selectedPath == null || selectedPath.getPathCount() == 0 ) {
					selectedNodeChanged( null );
				} else {
					selectedNodeChanged( (ITreeNode) selectedPath.getLastPathComponent() );
				}
			} });

		final JScrollPane treePane = new JScrollPane( tree );

		// add editor panel 
		editorPanel.setLayout( new GridBagLayout() );
		editorPanel.setBorder( BorderFactory.createLineBorder( Color.black ) );

		// cost statement panel
		final JPanel costPanel =
			costStatement.getPanel();

		final JPanel combinedPanel = 
			new JPanel();

		combinedPanel.setLayout( new GridBagLayout() );

		combinedPanel.add( costPanel , constraints(0,0).resizeBoth().useRemainingWidth().end() );
		combinedPanel.add( editorPanel , constraints(0,1).resizeBoth().useRemainingSpace().end() );

		final JPanel result = new JPanel();
		result.setLayout( new GridBagLayout() );

		final JSplitPane splitPane = 
			new JSplitPane( JSplitPane.HORIZONTAL_SPLIT ,  treePane , combinedPanel );

		result.add( splitPane , constraints().resizeBoth().end() );

		costStatement.refresh();
		return result;
	}

	/**
	 * 
	 * @param <T>
	 * @param editorClass  editor class to return, class constructor must take a INodeValueChangeListener as argument
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T extends NodeEditorComponent> T getEditorComponent(Class<T> editorClass) {
		if ( this.editorComponent != null && this.editorComponent.getClass() == editorClass ) {
			return (T) this.editorComponent;
		}
		try {
			return editorClass.getConstructor( INodeValueChangeListener.class ).newInstance( 
					this.nodeChangeListener 
			);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to instantiate editor component "+editorClass.getName(),e);
		}
	}

	protected void selectedNodeChanged(ITreeNode n)
	{
		final IComponent newEditor;
		if ( n instanceof ManufacturingJobNode ) 
		{
			final ManufacturingJobNode node = (ManufacturingJobNode) n;

			if ( node.getParent() == null ) 
			{ // only let the user edit the produced quantity of the top-level job
				final ManufacturingJobEditorComponent editor =
					getEditorComponent( ManufacturingJobEditorComponent.class ); // class constructor must take a INodeValueChangeListener as argument

				editor.setManufacturingJobRequest( node );
				
				newEditor = editor;
			} else {
				newEditor = null;
			}
		}
		else if ( n instanceof BlueprintNode && n.getParent() instanceof ManufacturingJobNode) 
		{
			final BlueprintNode node = (BlueprintNode) n;

			final BlueprintEditorComponent editor =
				getEditorComponent( BlueprintEditorComponent.class ); // class constructor must take a INodeValueChangeListener as argument

			editor.setBlueprintNode( node );
			newEditor = editor;
		} else {
			newEditor = null;
		}

		setEditorComponent( newEditor );
	}

	protected ITreeNode getCurrentlySelectedTreeNode() 
	{
		final TreePath selection =
			tree.getSelectionPath();

		if ( selection == null || ! ( selection.getLastPathComponent() instanceof ITreeNode ) ) {
			return null;
		}

		return (ITreeNode) selection.getLastPathComponent();
	}

	@Override
	protected void onDetachHook()
	{

		costStatement.onDetach();

		if ( this.editorComponent != null ) {
			this.editorComponent.onDetach();
		}
	}

	@Override
	protected void disposeHook()
	{

		costStatement.dispose();

		if ( this.editorComponent != null ) {
			this.editorComponent.dispose();
		}
	}

	protected void setEditorComponent(IComponent comp) {

		if ( comp != null && this.editorComponent == comp ) {
			return;
		}

		editorPanel.removeAll();

		if ( comp != null ) 
		{
			if ( this.editorComponent != null ) {
				this.editorComponent.onDetach(); // detach old component
				this.editorComponent = null;
			}

			this.editorComponent = comp;

			// always attach() before invoking getPanel() on a component
			this.editorComponent.onAttach( new IComponentCallback() {

				@Override
				public void dispose(IComponent caller) { }

				@Override
				public IStatusCallback getStatusCallback()
				{
					return getStatusCallback();
				}
			} 
			);

			editorPanel.add( this.editorComponent.getPanel() , constraints().resizeBoth().end() );

		} else {
			editorComponent = null;
		}

		editorPanel.invalidate();
		editorPanel.revalidate();

		getPanel().invalidate();
		getPanel().revalidate();

		getPanel().repaint();
	}

	@Override
	protected void onAttachHook(IComponentCallback callback)
	{

		final IRegionQueryCallback priceCallback =
			RegionSelectionDialog.createCallback( regionDAO );

		costStatement.setCostCalculator( 
				new TreeNodeCostCalculator(priceCallback , appConfigProvider, marketDataProvider) 
		);

		treeBuilder.setupView();

		costStatement.setManufacturingJobRequest( treeBuilder.getJobNode() );
		costStatement.onAttach( callback );
	}

	private Blueprint getT1BlueprintForInvention(Blueprint tech2Blueprint) 
	{
		return dataModel.getTech1Variation( tech2Blueprint );
	}

	public void setManufacturingJobRequest(ManufacturingJobRequest request) {
		if ( request == null ) {
			throw new IllegalArgumentException("job request cannot be NULL");
		}
		this.treeBuilder.setJobRequest( request );
	}

}
