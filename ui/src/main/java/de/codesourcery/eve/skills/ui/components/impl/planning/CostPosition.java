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

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.ui.model.ITreeNode;
import de.codesourcery.eve.skills.utils.ISKAmount;

public abstract class CostPosition 
{
	private ITreeNode treeNode;
	private String description;
	private int quantity;
	
	private ISKAmount pricePerUnit = ISKAmount.ZERO_ISK;
	
	private Kind kind;
	private Type type;
	
	private boolean unknownCost;
	
	public enum Kind {
		VARIABLE_COSTS,
		FIXED_COSTS,
		ONE_TIME_COSTS;
	}
	
	public enum Type {
		TOTAL,
		INDIVIDUAL;
	}
	
	public int getQuantity()
	{
		return quantity;
	}
	
	@Override
	public String toString()
	{
		return "type="+type+" ,kind="+kind+",unknown_costs="+unknownCost+" , price="+pricePerUnit+" , quantity="+
		quantity+" , description="+description+",node="+treeNode;
	}
	
	public void copyFrom(CostPosition other) {
		this.treeNode = other.getTreeNode();
		this.description = other.description;
		this.quantity = other.quantity;
		this.pricePerUnit = other.pricePerUnit;
		this.kind = other.kind;
		this.type = other.type;
		this.unknownCost = other.unknownCost;
	}
	
	public void incQuantity(int amount) {
		if ( amount < 0 ) {
			throw new IllegalArgumentException("amount cannot be negative");
		}
		this.quantity+=amount;
	}
	
	protected void setDescription(String desc) {
		
		if ( StringUtils.isBlank(desc) ) {
			throw new IllegalArgumentException("desc cannot be blank.");
		}
		this.description = desc;
	}
	
	public CostPosition(ITreeNode treeNode, 
			int quantity , 
			String description, Kind kind, Type type) 
	{
		this(treeNode, quantity, description, ISKAmount.ZERO_ISK, kind, type, true );
	}
	
	public CostPosition(ITreeNode treeNode, 
			int quantity , 
			String description,
			ISKAmount pricePerUnit,
			Kind kind, 
			Type type) 
	{
		this(treeNode, quantity, description, pricePerUnit, kind, type, false );
	}
	
	public CostPosition(ITreeNode treeNode, 
			int quantity , 
			String description,
			ISKAmount pricePerUnit,
			Kind kind, 
			Type type,
			boolean unknownCost) 
	{
		if ( quantity < 0 ) {
			throw new IllegalArgumentException("Quantity must be >= 0");
		}
		
		this.treeNode = treeNode;
		this.unknownCost = unknownCost;
		
		this.quantity = quantity;
		if ( StringUtils.isBlank(description) ) {
			throw new IllegalArgumentException(
					"description cannot be blank.");
		}
		
		if ( pricePerUnit == null ) {
			throw new IllegalArgumentException("pricePerUnit cannot be NULL");
		}
		
		if ( kind == null ) {
			throw new IllegalArgumentException("kind cannot be NULL");
		}
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		
		if ( unknownCost ) {
			this.pricePerUnit = ISKAmount.ZERO_ISK;
		} else {
			this.pricePerUnit = pricePerUnit;
		}
		this.description = description;
		this.kind = kind;
		this.type = type;
	}
	
	public ISKAmount getPricePerUnit()
	{
		return pricePerUnit;
	}
	
	public boolean hasUnknownCost() {
		return this.unknownCost;
	}
	
	public ITreeNode getTreeNode()
	{
		return treeNode;
	}

	public String getDescription()
	{
		return description;
	}

	public Kind getKind()
	{
		return kind;
	}

	public Type getType()
	{
		return type;
	}
}