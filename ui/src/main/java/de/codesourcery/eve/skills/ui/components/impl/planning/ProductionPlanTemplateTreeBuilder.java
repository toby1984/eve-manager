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

import java.util.List;

import org.springframework.dao.DataRetrievalFailureException;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.Decryptor;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;
import de.codesourcery.eve.skills.datamodel.Requirements;
import de.codesourcery.eve.skills.datamodel.Blueprint.Kind;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.production.InventionChanceCalculator;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.BlueprintNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.CopyJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.InventionJobDetails;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.InventionJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ManufacturingJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ProductionPlanNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.RequiredMaterialNode;
import de.codesourcery.eve.skills.ui.model.DefaultTreeModel;
import de.codesourcery.eve.skills.ui.model.ITreeNode;

public class ProductionPlanTemplateTreeBuilder
{
	private final IStaticDataModel dataModel;
	private ManufacturingJobRequest productionPlan;
	
	private final DefaultTreeModel treeModel = 
		new DefaultTreeModel();
	
	public ProductionPlanTemplateTreeBuilder(IStaticDataModel dataModel) {
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.dataModel = dataModel;
	}

	public void setJobRequest(ManufacturingJobRequest productionPlan)
	{
		this.productionPlan = productionPlan;
	}

	public ManufacturingJobRequest getJobRequest()
	{
		return productionPlan;
	}
	
	// =====================
	
	private ITreeNode createTree() {

		final Requirements requirementsFor = 
			this.productionPlan.getBlueprint().getRequirementsFor( Activity.MANUFACTURING );

		final ManufacturingJobNode jobNode = new ManufacturingJobNode( this.productionPlan );
		
		for ( RequiredMaterial r : requirementsFor.getRequiredMaterials() ) {
			jobNode.addChild( createNode( r ) );
		}

		jobNode.addChild( createBlueprintNode( this.productionPlan.getBlueprint() , 1 , Kind.ANY ) );

		return jobNode;
	}
	
	public ManufacturingJobNode getJobNode() {
		return (ManufacturingJobNode ) getTreeModel().getRoot();
	}
	
	protected BlueprintNode createBlueprintNode(Blueprint bp, int quantity, Blueprint.Kind requiredKind) {
		return new BlueprintNode( bp , quantity , requiredKind);
	}

	protected ManufacturingJobNode createManufacturingSubplanNode(ManufacturingJobRequest parent, RequiredMaterialNode node) 
	{
		
		/*
		 * TODO: Try to look-up blueprint from blueprint library and use those ME/PE levels.
		 */
		final ManufacturingJobRequest newRequest =
			new ManufacturingJobRequest( node.getBlueprintForProduction() );
		
		newRequest.setCharacter( this.productionPlan.getCharacter() );
		newRequest.setMaterialEfficiency( 0 ); 
		newRequest.setProductionEfficiency( 0 );
		newRequest.setQuantity( node.getQuantity() );
		newRequest.setSlotAttributes( this.productionPlan.getSlotAttributes() );
		
		final ManufacturingJobNode result=
			new ManufacturingJobNode( newRequest );

		final Requirements requirements = 
			node.getBlueprintForProduction().getRequirementsFor( Activity.MANUFACTURING );
		
		final Skill peSkill = 
			Skill.getProductionEfficiencySkill( dataModel.getSkillTree() );
		
		for ( RequiredMaterial m : requirements.getRequiredMaterials() ) {
			
			// determine real quantity
			final int quantity = Math.round( m.calcRequiredMaterial( newRequest , peSkill , false ) );
			
			final RequiredMaterial actualMaterial =
				new RequiredMaterial( m , quantity );
			
			result.addChild( createNode( actualMaterial ) );
		}

		// manufacturing something always requires either a blueprint copy or original
		result.addChild( createBlueprintNode( node.getBlueprintForProduction() , 1 , Kind.ANY ) );

		return result;
	}	
	
	protected Blueprint getBlueprintFor( RequiredMaterialNode node ) {
		try {
			return dataModel.getBlueprintByProduct( node.getRequiredMaterial().getType() );
		} catch(DataRetrievalFailureException e) {
			return null;
		}
	}
	
	protected Blueprint getBlueprintFor(RequiredMaterial m) {

		try {
			return dataModel.getBlueprintByProduct( m.getType() );
		} 
		catch(DataRetrievalFailureException e) {
			return null;
		}
	}	
	
	protected ITreeNode createNode(RequiredMaterial r, int quantity) {
		final RequiredMaterialNode result =
			new RequiredMaterialNode( r , getBlueprintFor( r )  );
		result.setQuantity( quantity );
		return result;
	}
	
	protected ITreeNode createNode(RequiredMaterial r) {
		return new RequiredMaterialNode( r , getBlueprintFor( r )  );
	}
	
	public void convertToRequiredMaterial(ManufacturingJobNode selection)
	{
		
		RequiredMaterial product = null;
		int quantity=0;
		
		final InventoryType productType = 
			selection.getManufacturingJobRequest().getBlueprint().getProductType();
		
		if ( selection.getParent() instanceof ManufacturingJobNode) 
		{
		ManufacturingJobNode parent = (ManufacturingJobNode) selection.getParent();
		final Requirements requirements = 
			parent.getManufacturingJobRequest().getBlueprint().getRequirementsFor( Activity.MANUFACTURING );
		
		for ( RequiredMaterial mat : requirements.getRequiredMaterials() ) 
		{
			if ( mat.getType().equals( productType ) ) 
			{
				final Skill peSkill =
					Skill.getProductionEfficiencySkill( dataModel.getSkillTree() );
				
				quantity = (int) mat.calcRequiredMaterial( parent.getManufacturingJobRequest() ,
						peSkill , false );
				product = mat;
				break;
			}
		}
		} else if ( selection.getParent() instanceof InventionJobNode ) {
			
			// special case: producing a component that's required for invention (e.g. data interface)
			// TODO: Currently I assume that the only 
			// TODO: requirements of an invention job (apart from datacores that cannot be 'produced')
			// TODO: are data interfaces ... is this correct in all cases (ship invention ?)
			final InventionJobNode node = (InventionJobNode) selection.getParent();
			product = InventionChanceCalculator.getRequiredDataInterface( node.getTech1Blueprint() );
			quantity = 1;
		}
		
		if ( product == null ) {
			throw new RuntimeException("Internal error, unable to find required amount for "+productType.getName());
					
		}
		
		treeModel.replaceChild( selection.getParent() , selection , 
				createNode( product , quantity ) 
		);
	}

	public void convertToRequiredBlueprint(InventionJobNode jobNode)
	{
		final ITreeNode parent = jobNode.getParent();
		final BlueprintNode newNode = new BlueprintNode( jobNode.getTech2Blueprint() , 1  , Kind.ANY ); // TODO: Fix quantity
		treeModel.replaceChild( parent , jobNode , newNode );
	}
	
	protected ManufacturingJobRequest getManufacturingJobRequest() {
		return (ManufacturingJobRequest) ((ITreeNode) treeModel.getRoot()).getValue();
	}
	
	public void convertToInventionJob(BlueprintNode tech2BlueprintNode, Blueprint tech1Blueprint)
	{

		/*
		 * Calculate required number of Tech2 blueprint copies
		 */
		final ManufacturingJobNode parent = (ManufacturingJobNode) tech2BlueprintNode.getParent();
		
		final InventionJobDetails details =
			new InventionJobDetails(
					tech1Blueprint,
					tech2BlueprintNode.getBlueprint(), 
					1 // will be populated later
			);

		final int averageNumberOfInventionJobs =
			calculateEstimatedRequiredNumberOfInventionJobs(
					parent.getManufacturingJobRequest().getCharacter(),
					details,
					parent.getQuantity() );
		
		details.setNumberOfRuns( averageNumberOfInventionJobs );
		
		final int numberOfTech2Copies =
			calculateRequiredNumberOfTech2Copies( parent.getManufacturingJobRequest().getCharacter()
				, details, parent.getQuantity() );
		
		final InventionJobNode newNode1 = 
			new InventionJobNode( details , numberOfTech2Copies );
		
		final List<RequiredMaterial> materials = 
			tech1Blueprint.getRequirementsFor( Activity.INVENTION ).getRequiredMaterials();
		
		for ( RequiredMaterial m : materials ) 
		{
			// data interface is not consumed and required only once
			// no matter how many invention job runs are set 
			if (  isDataInterface( tech1Blueprint , m ) ) {
				newNode1.addChild( createNode( m ) );
			} else {
				// hint: since this is not a data interface, it must be a datacore
				// or some other (consumable) component
				
				final RequiredMaterial newMat = 
					RequiredMaterial.createNew(  m , averageNumberOfInventionJobs  * m.getQuantity() );
				
				newNode1.addChild( createNode( newMat ) );
			}
		}
		
		newNode1.addChild( 
			createCopyJob(tech1Blueprint, averageNumberOfInventionJobs)
		); 
		
		final InventionJobNode newNode = 
			newNode1;
		
		treeModel.replaceChild( tech2BlueprintNode.getParent() , tech2BlueprintNode , newNode );		
	}
	
	protected boolean isDataInterface(Blueprint tech1Blueprint , RequiredMaterial material) {
		final InventoryType type = material.getType();
		final RequiredMaterial mat =
			InventionChanceCalculator.getRequiredDataInterface( tech1Blueprint );
		return mat.getType().equals( type );
	}
	
	protected CopyJobNode createCopyJob(Blueprint tech1Blueprint,int quantity) {
		
		final CopyJobNode result =
			new CopyJobNode( tech1Blueprint , quantity );
		
		result.addChild(
			createBlueprintNode( tech1Blueprint , 1, Blueprint.Kind.ORIGINAL )
		);
		return result;
	}

	/**
	 * Calculates the average number of invention jobs
	 * needed to create at least the given
	 * number of Tech2 blueprint copies.
	 */
	private int calculateRequiredAverageNumberOfInventionJobs(Blueprint tech1Blueprint,int minimumQuantity)
	{

		if ( minimumQuantity <= 0 ) {
			throw new IllegalArgumentException("Minimum Tech2 blueprint quantity must be >=1");
		}
		
		final InventionChanceCalculator calculator =
			new InventionChanceCalculator(dataModel);
		
		final double inventionChance =
			calculator.calculateInventionChance( tech1Blueprint , 
				getManufacturingJobRequest().getCharacter(), 
				0 , Decryptor.NONE );
		
		final int averageNumberOfInventionJobs = (int) Math.ceil( 
				minimumQuantity / inventionChance );
		
		return averageNumberOfInventionJobs;
	}
	
	public DefaultTreeModel getTreeModel()
	{
		return treeModel;
	}
	
	public void setupView()
	{

		if ( getJobRequest() == null ) {
			throw new IllegalStateException("Cannot setup view - no job request set?");
		} 

		treeModel.setRoot( createTree() );
		treeModel.modelChanged();
	}
	
	public void nodeValueChanged(ProductionPlanNode n) {
		
		if ( n instanceof ManufacturingJobNode ) {
			updateRequirements( (ManufacturingJobNode) n );
		} else if ( n instanceof BlueprintNode ) {
			final BlueprintNode  node = (BlueprintNode) n;
			if ( node.getParent() instanceof ManufacturingJobNode) {
				updateRequirements( (ManufacturingJobNode) node.getParent() );
			}
		}
	}

	protected void updateRequirements(ManufacturingJobNode changedJob) {
		
		final ManufacturingJobRequest jobRequest = 
			changedJob.getManufacturingJobRequest();
		
		treeModel.nodeValueChanged( changedJob );
		
		for ( ITreeNode childNode : changedJob.getChildren() ) 
		{
			if ( childNode instanceof RequiredMaterialNode ) 
			{
				final RequiredMaterialNode jobNode = (RequiredMaterialNode) childNode;
				
				final int newQuantity =
					calcRequiredMaterial( jobNode.getRequiredMaterial() , jobRequest );
				
				jobNode.setQuantity( newQuantity );
				treeModel.nodeValueChanged( jobNode );
			} 
			else if ( childNode instanceof ManufacturingJobNode ) {
				
				// find associated required material in parent job
				final ManufacturingJobNode jobNode = (ManufacturingJobNode) childNode;
				
				final InventoryType producedItem = jobNode.getProduct();
				final Requirements requirements = 
					jobRequest.getBlueprint().getRequirementsFor( Activity.MANUFACTURING );
				
				RequiredMaterial match=null;
				for (RequiredMaterial mat : requirements.getRequiredMaterials() ) {
					if ( mat.getType().equals( producedItem ) ) {
						match= mat;
						break;
					}
				}
				if ( match == null ) {
					throw new RuntimeException("Internal error - cannot find produced item "+
							producedItem+" in requirements of blueprint "+jobRequest.getBlueprint().getName());
				}
				
				int newQuantity =
					calcRequiredMaterial(match , changedJob.getManufacturingJobRequest() );
				
				jobNode.setQuantity( newQuantity );
				treeModel.nodeValueChanged( jobNode );
				
				updateRequirements( jobNode ); // recurse
				
			} 
			else if ( childNode instanceof InventionJobNode) 
			{
			
				final InventionJobNode child = (InventionJobNode) childNode;
				final ManufacturingJobNode parent = (ManufacturingJobNode) childNode.getParent();

				final ICharacter character = 
					parent.getManufacturingJobRequest().getCharacter();
				
				final int newNumberOfJobs = 
						calculateEstimatedRequiredNumberOfInventionJobs(
								character,
								child.getDetails(), 
								parent.getQuantity() 
						);
				
				final int oldNumberOfJobs = child.getDetails().getNumberOfRuns();
				System.out.println(" Current number of invention jobs: "+oldNumberOfJobs);
				System.out.println(" New number of invention jobs: "+newNumberOfJobs);
				
				if ( oldNumberOfJobs == newNumberOfJobs ) { // nothing changed
					continue;
				}
					
				final int requiredNumberOfTech2Copies = 
					calculateRequiredNumberOfTech2Copies( 
						character , 
						child.getDetails() , 
						parent.getQuantity() 
				);
				
				System.out.println("=> required number of Tech2 copies: "+requiredNumberOfTech2Copies);
				System.out.println("=> required number of invention jobs: "+newNumberOfJobs);
				
				child.setQuantity( requiredNumberOfTech2Copies );
				child.getDetails().setNumberOfRuns( newNumberOfJobs );
				treeModel.nodeValueChanged( child );
			
				for ( ITreeNode inventionChild : child.getChildren() ) 
				{
					
					if ( inventionChild instanceof CopyJobNode ) {
						updateRequirements( (CopyJobNode) inventionChild ); // recurse 
						continue;
					}
					
					if ( ! ( inventionChild instanceof RequiredMaterialNode ) ) {
						continue;
					}
					
					final RequiredMaterialNode n = (RequiredMaterialNode) inventionChild;
					if ( isDataInterface( child.getTech1Blueprint() , n.getRequiredMaterial() ) ) 
					{
						// required quantity is always 1
						continue;
					}
					
					System.out.println("=> material "+n.getRequiredMaterial().getType()+" ( quantity = "+
							n.getRequiredMaterial().getQuantity() );
					
					final double quantityPerRun = 
						n.getRequiredMaterial().getQuantity() / oldNumberOfJobs;
					
					System.out.println("=> material "+n.getRequiredMaterial().getType()+" ( quantityPerRun = "+
							quantityPerRun+" , new_number_of_jobs="+newNumberOfJobs);					
					
					n.setQuantity( (int) Math.ceil( quantityPerRun * newNumberOfJobs ) );
					treeModel.nodeValueChanged( inventionChild );
				}
			} 
			else if ( childNode instanceof CopyJobNode ) 
			{
				updateRequirements( (CopyJobNode) childNode );
			}
		}
	}
	
	protected void updateRequirements(CopyJobNode childNode) {
		final InventionJobNode parent = (InventionJobNode) childNode.getParent();
		childNode.setQuantity( parent.getDetails().getNumberOfRuns() );
		treeModel.nodeValueChanged( childNode );
	}
	
	protected int calculateRequiredNumberOfTech2Copies(ICharacter character,
			InventionJobDetails details,
			int requiredProductionQuantity) 
	{
		final Blueprint tech1Blueprint = details.getTech1Blueprint();
		final Blueprint tech2Blueprint = details.getTech2Blueprint();
		
		System.out.println("Tech 1: "+tech1Blueprint.getType().getBlueprintType() );
		System.out.println("Tech 2: "+tech2Blueprint.getType().getBlueprintType() );
		
		final InventionChanceCalculator calculator = 
			new InventionChanceCalculator( dataModel );
		
		// determine max. number of runs an invented BPC might have
		final int maxNumberOfRuns =
			calculator.calcMaximumNumberOfRunsPossible( tech1Blueprint , details.getDecryptor() );
		
		System.out.println("Max. number of runs per BPC: "+maxNumberOfRuns);
		
		final int maxQuantityPerBPC =
			maxNumberOfRuns * tech2Blueprint.getPortionSize();
		
		System.out.println("Max. produced quantity per run: "+maxQuantityPerBPC);
		
		final int requiredNumberOfTech2Copies =
			(int) Math.ceil( (double) requiredProductionQuantity / (double) maxQuantityPerBPC );
		
		System.out.println("Required number of Tech2 BPCs to produce "+requiredProductionQuantity+" x "+
				tech2Blueprint.getProductType()+" : "+requiredNumberOfTech2Copies );
		
		return requiredNumberOfTech2Copies;
	}
	
	protected int calculateEstimatedRequiredNumberOfInventionJobs(ICharacter character,
			InventionJobDetails details,
			int requiredProductionQuantity) 
	{
	
		final Blueprint tech1Blueprint = details.getTech1Blueprint();
		
		final InventionChanceCalculator calculator = 
			new InventionChanceCalculator( dataModel );
		
		// calculate required quantity of Tech2 copies
		final int requiredNumberOfTech2Copies = 
			calculateRequiredNumberOfTech2Copies( character,details,requiredProductionQuantity );
		
		final float inventionChance =
			calculator.calculateInventionChance( 
					tech1Blueprint ,
					character,
					details.getMetaLevel(),
					details.getDecryptor() );
		
		System.out.println("Invention chance: "+inventionChance);
		
		// calculate estimated number of invention jobs required
		final int result = (int) Math.ceil( requiredNumberOfTech2Copies / inventionChance );
		
		System.out.println("\n=================\nRequired number of invention jobs: "+result);
		
		return result;
	}
	
	protected int calcRequiredMaterial(RequiredMaterial mat,ManufacturingJobRequest jobRequest) {
		final Skill skill = Skill.getProductionEfficiencySkill( this.dataModel.getSkillTree() );
		return Math.round( mat.calcRequiredMaterial( jobRequest, skill , false ) );
		

	}
	
	public void convertToManufacturingJob(ManufacturingJobRequest parent , RequiredMaterialNode selection)
	{
		final ITreeNode subPlan = createManufacturingSubplanNode( parent, selection );
		treeModel.replaceChild( selection.getParent() , selection , subPlan );		
	}
}
