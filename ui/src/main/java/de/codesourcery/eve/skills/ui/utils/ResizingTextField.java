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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.JComponent;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

public class ResizingTextField extends JTextField
{

    public ResizingTextField() {
        this( "" );
    }

    /**
     * Initialize the KlangLabel
     * 
     * @param lblText
     *            initial label text
     */
    public ResizingTextField(String text) {
        super( text );

        FontMetrics fm = this.getFontMetrics( this.getFont() );
        int height = fm.getHeight();

        this.setMaximumSize( new java.awt.Dimension( 10000, height + 6 ) );
        // this.setPreferredSize( new java.awt.Dimension( 0, height + 6 ) );
    }

    /**
     * Override of set text, so that long labels will look correct
     * 
     * @param text
     *            new text for the object
     */
    @Override
    public void setText(String text)
    {
        super.setText( text );

        setPreferredSize( calcSize( text ) );

        // find top-level parent container
        Container parent = getParent();
        while ( parent != null )
        {
            if ( parent.getParent() != null && parent.getParent() instanceof JComponent )
            {
                parent = parent.getParent();
            }
            else
            {
                break;
            }
        }

        if ( parent instanceof JComponent )
        {
            ( (JComponent) getParent() ).revalidate();
            ( (JComponent) getParent() ).repaint();
        }
    }

    private Dimension calcSize(String text)
    {

        final FontMetrics fm = this.getFontMetrics( this.getFont() );
        final int height = fm.getHeight();

        final int colCount = Math.max( getColumns(), 1 );
        final String realText =
                StringUtils.isBlank( text ) ? StringUtils.leftPad( "X", colCount, " " )
                        : " " + text + " ";

        int width = fm.stringWidth( realText ) + 10;
        return new Dimension( width, height + 6 );
    }

    @Override
    public Dimension getPreferredSize()
    {
        return calcSize( getText() );
    }

}
