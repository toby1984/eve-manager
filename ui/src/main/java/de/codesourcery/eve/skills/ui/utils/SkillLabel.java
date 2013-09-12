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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import de.codesourcery.eve.skills.datamodel.TrainedSkill;

public class SkillLabel extends JLabel {

	private static final int MAX_LEVELS = 5;

	private static final int BAR_VERT_GAP = 10;
	private static final int BAR_HORIZ_GAP_WIDTH = 3;

	private static final int BAR_WIDTH = 100;

	private static final int BAR_HEIGHT = 7;
	
	private Color background;

	private TrainedSkill trainedSkill;

	public SkillLabel() {
	}

	public interface IRenderCallback {
		
		public Color getTextSelectionColor();
		
		public Color getTextNonSelectionColor();
		
		public Icon getLeafIcon();
		
		public Icon getOpenIcon();
		
		public Icon getClosedIcon();
		
		public Color getBackgroundSelectionColor();

		public Color getBackgroundNonSelectionColor();		
		
	}
	
	/**
	 * Configures the renderer based on the passed in components.
	 * The value is set from messaging the tree with
	 * <code>convertValueToText</code>, which ultimately invokes
	 * <code>toString</code> on <code>value</code>.
	 * The foreground color is set based on the selection and the icon
	 * is set based on the <code>leaf</code> and <code>expanded</code>
	 * parameters.
	 */
	public void setupTreeCellRendererComponent(JTree tree, Object value,
			boolean sel,
			boolean expanded,
			boolean leaf, int row,
			boolean hasFocus,
			IRenderCallback callback) 
	{

		Color fg = null;

		if (sel) {
			fg = callback.getTextSelectionColor();
		} else {
			fg = callback.getTextNonSelectionColor();
		}

		setForeground(fg);

		Icon icon = null;
		if (leaf) {
			icon = callback.getLeafIcon();
		} else if (expanded) {
			icon = callback.getOpenIcon();
		} else {
			icon = callback.getClosedIcon();
		}

		if (!tree.isEnabled()) {
			setEnabled(false);
			LookAndFeel laf = UIManager.getLookAndFeel();
			Icon disabledIcon = laf.getDisabledIcon(tree, icon);
			if (disabledIcon != null) icon = disabledIcon;
			setDisabledIcon(icon);
		} else {
			setEnabled(true);
			setIcon(icon);
		}
		setComponentOrientation(tree.getComponentOrientation());
		
		final Color bg;
		if ( sel ) {
			bg = callback.getBackgroundSelectionColor();
		} else {
			bg = callback.getBackgroundNonSelectionColor();
		}
		
		setBackground( bg );
		this.background = bg;
	}	

	@Override
	public void paint(Graphics g) {

		// clear background
		if ( background != null ) {
			g.setColor( background );
			g.fillRect(  0 , 0 , getWidth() , getHeight() );
		}

		// calculate bar position
		super.paint(g);

		final FontMetrics metrics = g.getFontMetrics();

		final Rectangle2D bounds = 
			metrics.getStringBounds( getText(), g );

		int x;
		if ( bounds.getMinX() >= 0 ) {
			x = (int) bounds.getMinX();
		} else {
			x = (int) -bounds.getMinX();
		}

		// draw bar
		int y;
		final Icon icon = getIcon();
		if ( icon != null ) {
			x += icon.getIconWidth();
			x += getIconTextGap();
			
			if ( icon.getIconHeight() > metrics.getHeight() ) {
				y = icon.getIconHeight();
			} else {
				y = metrics.getHeight();
			}
		} else {
			y = metrics.getHeight();
		}

		y+= BAR_VERT_GAP;

		final int currentLevel = 
			this.trainedSkill.getLevel();
		
		Color currentColor = g.getColor();
		final int singleBarWidth = ( BAR_WIDTH / MAX_LEVELS ) - BAR_HORIZ_GAP_WIDTH;
		for ( int i = 1 ; i <= MAX_LEVELS ; i++ ) {

			g.setColor( currentColor );
			g.drawRect( x , y , singleBarWidth , BAR_HEIGHT );
			
			if ( currentLevel >= i ) {
				g.fillRect( x , y , singleBarWidth , BAR_HEIGHT );
			} else if ( this.trainedSkill.isPartiallyTrained( i ) ) { 
				final float fraction = trainedSkill.getFractionOfLevelTrained( i ) / 100.0f;
				int width = Math.round( ( singleBarWidth - 2 ) *fraction );
				if ( width < 1 ) {
					width = 1;
				}
				g.setColor( Color.GRAY );
				g.fillRect( x+1 , y+1 , width , BAR_HEIGHT-1 );
			}

			x+= (singleBarWidth + BAR_HORIZ_GAP_WIDTH);
		}
	}

	private Dimension calculatePreferredSize()  {

		final Dimension result = super.getPreferredSize();
		
		if ( getText() == null ) {
			return result;
		}

		final Graphics g =
			getGraphics();

		if ( g == null ) {
			return result;
		}

		final Rectangle2D bounds = 
			g.getFontMetrics().getStringBounds( getText(), g );

		double width =
			result.width > bounds.getWidth() ? result.width : bounds.getWidth();

		double height =
			result.height > bounds.getHeight() ? result.height : bounds.getHeight();

		if ( width < BAR_WIDTH ) {
			width = BAR_WIDTH;
		}

		height += (BAR_VERT_GAP+BAR_HEIGHT+1);

		return new Dimension( (int) Math.round( width ) , (int) Math.round( height ) );
	}

	@Override
	public Dimension getPreferredSize() {
		return calculatePreferredSize();
	}

	public void setTrainedSkill(TrainedSkill skill) {
		this.trainedSkill = skill;
	}
	
}
