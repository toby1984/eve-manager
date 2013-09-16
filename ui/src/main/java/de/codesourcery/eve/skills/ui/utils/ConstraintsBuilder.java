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
package de.codesourcery.eve.skills.ui.utils;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Builder for constructing {@link GridBagConstraints}
 * instances.
 *
 *  This builder uses a fluent-API style. Call the
 *  {@link #end()} method to obtain the 
 *  <code>GridBagConstraints</code> instance
 *  configured by this builder.
 * @author tobias.gierke@code-sourcery.de
 */
public final class ConstraintsBuilder {
	
	private GridBagConstraints cnstrs;

	public ConstraintsBuilder() {
		this(0,0);
	}
	
	public ConstraintsBuilder(int x,int y) {
		cnstrs = new GridBagConstraints(
				x, // x
				y, // y
				1, // width
				1,// height
				0.5d, // weightX
				0.5d, // weightY
				GridBagConstraints.NORTHWEST, // anchor
				GridBagConstraints.BOTH, 
				new Insets(2,2,2,2) ,  
				0 , // padX 
				0 // padY
				);		
	}
	
	private boolean gridWidthSet = false;
	private boolean gridHeightSet  = false;
	
	private boolean weightXSet = false;
	private boolean weightYSet = false;
	
	private boolean anchorSet = false;
	
	private boolean fillSet = false;
	
	public GridBagConstraints end() {
		return cnstrs;
	}
	
	protected void assertNotSet(String msg , boolean isSet) {
		if ( isSet ) {
			throw new RuntimeException( msg+" has already been set ?");
		}
	}
	
	public ConstraintsBuilder weightX(double weight) {
		assertNotSet( "weightX" , weightXSet );
		weightXSet = true;
		cnstrs.weightx = weight;
		return this;
	}
	
	public ConstraintsBuilder weightY(double weight) {
		assertNotSet( "weightX" , weightYSet );
		weightYSet = true;
		cnstrs.weighty = weight;
		return this;
	}
	
	public ConstraintsBuilder x(int x) {
		cnstrs.gridx = x;
		return this;
	}
	
	public ConstraintsBuilder y(int y) {
		cnstrs.gridy = y;
		return this;
	}
	
	public ConstraintsBuilder width(int w) {
		assertNotSet( "gridWidth" , gridWidthSet);
		gridWidthSet = true;
		cnstrs.gridwidth = w;
		return this;
	}		
	
	public ConstraintsBuilder anchorEast() {
		return setAnchor( GridBagConstraints.LINE_END );
	}
	
	public ConstraintsBuilder useRelativeWidth() {
		return width( GridBagConstraints.RELATIVE );
	}		
	
	public ConstraintsBuilder useRemainingWidth() {
		return width( GridBagConstraints.REMAINDER );
	}		
	
	public ConstraintsBuilder useRelativeHeight() {
		return height( GridBagConstraints.RELATIVE );
	}	
	
	public ConstraintsBuilder useRemainingSpace() {
		useRemainingHeight();
		useRemainingWidth();
		return this;
	}
	
	public ConstraintsBuilder useRemainingHeight() {
		return height( GridBagConstraints.REMAINDER );
	}	
	
	public ConstraintsBuilder resizeHorizontally() {
		return fill( GridBagConstraints.HORIZONTAL );
	}
	
	public ConstraintsBuilder resizeVertically() {
		return fill( GridBagConstraints.VERTICAL );
	}	
	
	public ConstraintsBuilder noResizing() {
		fill( GridBagConstraints.NONE );
		weightX( 0.0d );
		weightY( 0.0d );
		return this;
	}		
	
	public ConstraintsBuilder resizeBoth() {
		return fill( GridBagConstraints.BOTH );
	}		
	
	public ConstraintsBuilder height(int h) {
		assertNotSet( "gridHeight" , gridHeightSet);
		gridHeightSet = true;
		cnstrs.gridheight= h;
		return this;
	}
	
	public ConstraintsBuilder fill(int fill) {
		assertNotSet( "fill" , fillSet );
		fillSet = true;
		cnstrs.fill = fill;
		return this;
	}
	
	private ConstraintsBuilder setAnchor(int anchor) {
		assertNotSet( "anchor" , anchorSet );
		anchorSet = true;
		cnstrs.anchor = anchor;
		return this;
	}

	public ConstraintsBuilder anchorWest() {
		return setAnchor( GridBagConstraints.WEST );
	}
	
	public ConstraintsBuilder anchorCenter() {
		return setAnchor( GridBagConstraints.CENTER );
	}	

	public ConstraintsBuilder anchorNorth()
	{
		return setAnchor( GridBagConstraints.NORTH );
	}

	public ConstraintsBuilder anchorSouth()
	{
		return setAnchor( GridBagConstraints.SOUTH );
	}

}