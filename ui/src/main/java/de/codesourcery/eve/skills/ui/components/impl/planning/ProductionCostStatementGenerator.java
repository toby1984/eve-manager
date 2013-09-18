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

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.Blueprint.Kind;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.exceptions.NoTech1VariantException;
import de.codesourcery.eve.skills.exceptions.PriceInfoUnavailableException;
import de.codesourcery.eve.skills.production.InventionChanceCalculator;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.BlueprintNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.CopyJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.InventionJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.ManufacturingJobNode;
import de.codesourcery.eve.skills.ui.components.impl.planning.treenodes.RequiredMaterialNode;
import de.codesourcery.eve.skills.ui.config.IRegionQueryCallback;
import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.ui.utils.RegionSelectionDialog;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class ProductionCostStatementGenerator
{
	private static final Logger log = Logger
	.getLogger(ProductionCostStatementGenerator.class);

	private final IStaticDataModel staticDataModel;
	private final TreeNodeCostCalculator nodeCalculator;
	private final ManufacturingJobNode node;
	
	public ProductionCostStatementGenerator(ManufacturingJobNode node,
			TreeNodeCostCalculator nodeCalculator,
			IStaticDataModel dataModel) 
	{
		if ( node == null ) {
			throw new IllegalArgumentException("node cannot be NULL");
		}
		if ( nodeCalculator == null ) {
			throw new IllegalArgumentException("nodeCalculator cannot be NULL");
		}
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.staticDataModel = dataModel;
		this.nodeCalculator = nodeCalculator;
		this.node = node;
	}

	public CostStatement createCostStatement() {

		final CostStatement result =
			new CostStatement();

		createCostStatement( this.node , result );

		aggregate( result );
		
		return result;
	}

	private void aggregate(CostStatement result)
	{

		if ( ! result.iterator().hasNext() ) {
			return;
		}
outer:		
		while( true ) {
			for ( CostPosition pos : result ) 
			{
				for ( CostPosition otherPos : result ) 
				{
					if ( pos == otherPos ) {
						continue;
					}
					
					if ( canBeJoinedWith( pos , otherPos ) ) {
						join( result , pos , otherPos );
						continue outer;
					}
				}
			}
			break;
		}
		
	}

	protected void createCostStatement(ITreeNode node, CostStatement result) {
		for ( ITreeNode n : node.getChildren() ) {
			if ( n instanceof BlueprintNode) {
				addToStatement( (BlueprintNode) n, result );
			} else if ( n instanceof RequiredMaterialNode) {
				addToStatement( (RequiredMaterialNode) n, result );
			} else if ( n instanceof ManufacturingJobNode ) {
				createCostStatement( n , result );
			} 
			else if ( n instanceof InventionJobNode ) {
				addToStatement( (InventionJobNode) n, result );
			} 
			else if ( n instanceof CopyJobNode ) {
				addToStatement( (CopyJobNode) n, result );
			}
		}
	}
	
	private void addToStatement(CopyJobNode n, CostStatement result)
	{
		if ( n.getChildCount() != 1 ) {
			throw new RuntimeException("Internal error, copy job node needs to have exactly 1 child");
		}
		
		addToStatement( (BlueprintNode) n.getChildAt(0 ) , result );
	}

	/**
	 * Join two cost positions.
	 * 
	 * @param statement the cost statement both positions are currently a part of 
	 * @param thisPos
	 * @param other
	 */
	protected static void join(CostStatement statement , 
			CostPosition thisPos , 
			CostPosition other) 
	{
		
		if ( thisPos.getKind() != other.getKind() ) {
			throw new IllegalArgumentException("Cannot join cost positions - kinds differ");
		}
		if ( thisPos.getType() != other.getType() ) {
			throw new IllegalArgumentException("Cannot join cost positions - types differ");
		}
		if ( thisPos.hasUnknownCost() != other.hasUnknownCost() ) {
			throw new IllegalArgumentException("Cannot join cost positions - one of them has an unknown ISK amount");
		}
		
		if ( ! thisPos.getPricePerUnit().equals( other.getPricePerUnit() ) ) {
			throw new IllegalArgumentException("Cannot join cost positions - different prices per unit");
		}
		
		if ( thisPos instanceof RequiredMaterialCostPosition ) {
			thisPos.incQuantity( other.getQuantity() );
			statement.remove( other );
			return;
		} 
		
		if ( thisPos instanceof BlueprintCostPosition ) 
		{
			final BlueprintCostPosition p1 = (BlueprintCostPosition) thisPos;
			final BlueprintCostPosition p2 = (BlueprintCostPosition) other;
		
			if ( p1.isOriginal() && p2.isOriginal() ) {
				statement.remove( other );
			} else if ( p1.isAny() ) {
				statement.remove( p1 );
			} else if ( p2.isAny() ) {
				statement.remove( p2 );
			}
			return;
		}
		
		throw new RuntimeException("Internal error , don't know how to join "+thisPos+" and "+other);
	}
	
	private static boolean areTypesCompatible( CostPosition thisPos , CostPosition other) 
	{
		if (  thisPos.getClass() != other.getClass() ) {
			return false;
		}
		
		if ( thisPos instanceof RequiredMaterialCostPosition) 
		{
			final RequiredMaterialCostPosition p1 = (RequiredMaterialCostPosition) thisPos;
			final RequiredMaterialCostPosition p2 = (RequiredMaterialCostPosition) other;
			return p1.getMaterial().hasSameTypeAs( p2.getMaterial() );
		}
		else if ( thisPos instanceof BlueprintCostPosition ) 
		{
			final BlueprintCostPosition p1 = (BlueprintCostPosition) thisPos;
			final BlueprintCostPosition p2 = (BlueprintCostPosition) other;
			if ( ! p1.getBlueprint().equals( p2.getBlueprint() ) ) {
				return false;
			}
			
			if ( ( p1.isOriginal() && p2.isCopy() ) ||
				 ( p1.isCopy()     && p2.isOriginal() ) ) 
			{
				return false;
			}
		}
		return true;
	}
	
	protected static boolean canBeJoinedWith(CostPosition thisPos , 
			CostPosition other) 
	{
		if ( ! areTypesCompatible( thisPos , other ) )
		{
			return false;
		}
		
		if ( thisPos.getKind() != other.getKind() ) {
			return false;
		}
		if ( thisPos.getType() != other.getType() ) {
			return false;
		}
		if ( thisPos.hasUnknownCost() != other.hasUnknownCost() ) {
			return false;
		}
		if ( ! thisPos.getPricePerUnit().equals( other.getPricePerUnit() ) ) {
			return false;
		}
		return true;
	}

	private void addToStatement(InventionJobNode node, CostStatement result)
	{

		for ( ITreeNode n : node.getChildren() ) 
		{
			if ( n instanceof BlueprintNode) 
			{
				final BlueprintNode bpNode = (BlueprintNode) n;
				final Blueprint bp = bpNode.getBlueprint();
				
				addToStatement( bpNode , result );
				
				// BPO original once
				result.add( new BlueprintCostPosition(
				bpNode,
				1,
				bp.getName()+" BPO",
				bp.getBasePrice(),
				CostPosition.Kind.ONE_TIME_COSTS,
				CostPosition.Type.INDIVIDUAL) );				
			}
			else if ( n instanceof RequiredMaterialNode) 
			{
				final RequiredMaterialNode rNode = (RequiredMaterialNode) n;
				final RequiredMaterial material = rNode.getRequiredMaterial();

				final RequiredMaterial dataInterface = 
					InventionChanceCalculator.getRequiredDataInterface( node.getTech1Blueprint() );

				if ( dataInterface.getType().equals( material.getType() ) ) 
				{

					final String description = material.getType().getName();
					ISKAmount price;
					boolean priceAvailable = false;
					try {
						price = getPrice( rNode );
						priceAvailable = true;
					}
					catch (PriceInfoUnavailableException e) {
						price = ISKAmount.ZERO_ISK;
					}

					result.add( new RequiredMaterialCostPosition( n,
							1,
							description,
							price,
							CostPosition.Kind.ONE_TIME_COSTS,
							material.getType(),							
							CostPosition.Type.INDIVIDUAL,
							! priceAvailable
					) );

				} else {
					addToStatement( rNode , result );
				}

			} else if ( n instanceof ManufacturingJobNode ) {
				createCostStatement( n , result );
			} 
			else if ( n instanceof InventionJobNode ) {
				addToStatement( (InventionJobNode) n, result );
			} else if ( n instanceof CopyJobNode ) {
				addToStatement( (CopyJobNode) n , result );
			}
		}		
	}

	protected ISKAmount getPrice(RequiredMaterialNode n) throws PriceInfoUnavailableException {
		return this.nodeCalculator.getAveragePrice( n.getRequiredMaterial().getType() );
	}

	protected void addToStatement(RequiredMaterialNode n, CostStatement result)
	{

		final String description = 
			n.getRequiredMaterial().getType().getName();
		
		try {
			final ISKAmount  amount =
				getPrice( n );

			result.add( new RequiredMaterialCostPosition( n , 
			n.getQuantity(),
			description,
			amount,
			CostPosition.Kind.FIXED_COSTS,
			n.getRequiredMaterial().getType(),
			CostPosition.Type.INDIVIDUAL) );
		}
		catch (PriceInfoUnavailableException e) 
		{
			result.add( new RequiredMaterialCostPosition( n , 
			n.getQuantity(),
			description,
			CostPosition.Kind.FIXED_COSTS,
			CostPosition.Type.INDIVIDUAL) );
		}
	}
	
	private static final class RequiredMaterialCostPosition extends CostPosition {

		public RequiredMaterialCostPosition(
				ITreeNode treeNode,
				int quantity,
				String description,
				ISKAmount pricePerUnit,
				de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind kind,
				InventoryType itemType,					
				Type type, 
				boolean unknownCost) {
			super(treeNode, quantity, description, pricePerUnit, kind, type, itemType , unknownCost);
		}

		public RequiredMaterialCostPosition(
				ITreeNode treeNode,
				int quantity,
				String description,
				ISKAmount pricePerUnit,
				de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind kind,
				InventoryType itemType,				
				Type type
				) {
			super(treeNode, quantity, description, pricePerUnit, kind, itemType, type);
		}

		public RequiredMaterialCostPosition( ITreeNode treeNode,
				int quantity,
				String description,
				de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind kind,
				Type type) 
		{
			super(treeNode, quantity, description, kind, type);
		}
		
		public RequiredMaterial getMaterial() {
			return ((RequiredMaterialNode) getTreeNode()).getRequiredMaterial();
		}
		
	}
	
	/**
	 * Blueprint cost positions must
	 * not be joined if they reference
	 * a BPO (since you require only one BPO to produce something).
	 * @author tobias.gierke@code-sourcery.de
	 */
	private static final class BlueprintCostPosition extends CostPosition {

		private BlueprintCostPosition(
				BlueprintNode treeNode,
				int quantity,
				String description,
				ISKAmount pricePerUnit,
				de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind kind,
				Type type, boolean unknownCost) 
		{
			super(treeNode, quantity, description, pricePerUnit, kind, type, null , unknownCost);
		}

		public boolean isCopy() {
			return getBlueprintKind() == Blueprint.Kind.COPY;
		}
		
		public boolean isOriginal() {
			return getBlueprintKind() == Blueprint.Kind.ORIGINAL;
		}
		
		public boolean isAny() {
			return getBlueprintKind() == Blueprint.Kind.ANY;
		}
		
		private BlueprintCostPosition(
				BlueprintNode treeNode,
				int quantity,
				String description,
				ISKAmount pricePerUnit,
				de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind kind,
				Type type) {
			super(treeNode, quantity, description, pricePerUnit, kind, null , type);
		}

		private BlueprintCostPosition(
				BlueprintNode treeNode,
				int quantity,
				String description,
				de.codesourcery.eve.skills.ui.components.impl.planning.CostPosition.Kind kind,
				Type type) {
			super(treeNode, quantity, description, kind, type);
		}
		
		public Blueprint getBlueprint() {
			return getNode().getBlueprint();
		}
		
		public Blueprint.Kind getBlueprintKind() {
			return getNode().getBlueprintKind();
		}
		
		@Override
		public String toString()
		{
			return getNode().getBlueprint().getName()+" [kind="+getNode().getBlueprintKind()+"]";
		}
		
		public BlueprintNode getNode() {
			return (BlueprintNode) getTreeNode();
		}
		
	}
	
	protected String getDescription(BlueprintNode node) {
		final Blueprint bp = node.getBlueprint();
		if ( bp.getTechLevel() == 1 ) {
			if ( node.getBlueprintKind() == Kind.COPY ) {
				return bp.getName()+" Copy";
			}
			// kind = { ANY , ORIGINAL } 
			return bp.getName()+" BPO";
		} 
		return bp.getName()+" ( Tech 2 )";
	}

	protected void addToStatement(BlueprintNode node,CostStatement statement) {

		final Blueprint bp = node.getBlueprint();
		if ( bp.getTechLevel() == 1 ) 
		{
			if ( node.getBlueprintKind() == Kind.COPY ) {
				statement.add( new BlueprintCostPosition(
				node,
				node.getQuantity(),
				getDescription( node ),
				ISKAmount.ZERO_ISK,
				CostPosition.Kind.VARIABLE_COSTS,
				CostPosition.Type.INDIVIDUAL
				) );
			} else {
				statement.add( new BlueprintCostPosition(
				node,
				node.getQuantity(),
				getDescription( node ),
				bp.getBasePrice(),
				CostPosition.Kind.ONE_TIME_COSTS,
				CostPosition.Type.INDIVIDUAL) );
			}
		} else {
			
			ISKAmount amount = ISKAmount.ZERO_ISK;
			boolean priceUnknown = true;
			try {
				staticDataModel.getTech1Variation( bp );
			} catch(NoTech1VariantException e) {
				amount = bp.getBasePrice();
				priceUnknown = false;
			}
			
			statement.add( new BlueprintCostPosition(
			node,
			node.getQuantity(),
			getDescription( node ) ,
			amount ,
			CostPosition.Kind.ONE_TIME_COSTS,
			CostPosition.Type.INDIVIDUAL,
			priceUnknown ) );
		}
	}
}
