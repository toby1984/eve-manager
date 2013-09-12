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

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JSplitPane;

/**
 * Hacked {@link JSplitPane} that supports
 * calling {@link #setDividerLocation(double)} while
 * not being visible (unlike the original one
 * that just ignores the method call ....grrrr).
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class ImprovedSplitPane extends JSplitPane
{
	
	// HACK to set proportional divider location
	// without the JSplitPane being visible
	private boolean isPainted;
	private boolean hasProportionalLocation;
	private double proportionalLocation;

	public ImprovedSplitPane() {
		super();
	}

	public ImprovedSplitPane(int newOrientation, boolean newContinuousLayout,
			Component newLeftComponent, Component newRightComponent) {
		super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
	}

	public ImprovedSplitPane(int newOrientation, boolean newContinuousLayout) {
		super(newOrientation, newContinuousLayout);
	}

	public ImprovedSplitPane(int newOrientation, Component newLeftComponent,
			Component newRightComponent) {
		super(newOrientation, newLeftComponent, newRightComponent);
	}

	public ImprovedSplitPane(int newOrientation) {
		super(newOrientation);
	}

	public void setDividerLocation(double proportionalLocation) {
		if (!isPainted) {       
			hasProportionalLocation = true;
			this.proportionalLocation = proportionalLocation;
		}
		else
			super.setDividerLocation(proportionalLocation);
	}

	public void paint(Graphics g) {
		if (!isPainted) {       
			if (hasProportionalLocation)
				super.setDividerLocation(proportionalLocation);
			isPainted = true;
		}
		super.paint(g);
	} 

}
