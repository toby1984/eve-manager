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
package de.codesourcery.eve.skills.ui.components.impl.planning.treenodes;

import de.codesourcery.eve.skills.ui.model.DefaultTreeNode;

public abstract class ProductionPlanNode extends DefaultTreeNode
{
	private int quantity;
	
	public ProductionPlanNode(Object value,int quantity) {
		super( value );
		if ( quantity<= 0) {
			throw new IllegalArgumentException("Quantity cannot be < 0");
		}
		this.quantity = quantity;
	}
	
	public int getQuantity()
	{
		return quantity;
	}
	
	public void setQuantity(int quantity)
	{
		if ( quantity <= 0) {
			throw new IllegalArgumentException("Quantity cannot be <= 0 , was: "+quantity);
		}
		this.quantity = quantity;
	}
	
	public abstract boolean hasEditableQuantity();
}
