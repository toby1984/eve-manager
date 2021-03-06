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

import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.utils.ResizingTextField;

public class TotalItemVolumeComponent<X> extends AbstractComponent
{
    private final String title;

    private final ResizingTextField textField = new ResizingTextField();

    private final IDataProvider<X> dataProvider;

    public interface IDataProvider<X>
    {
        public double getVolumePerUnit(X obj);

        public int getQuantity(X obj);
    }

    public TotalItemVolumeComponent(String title, IDataProvider<X> dataProvider) {

        if ( StringUtils.isBlank( title ) )
        {
            throw new IllegalArgumentException( "title cannot be blank." );
        }

        if ( dataProvider == null )
        {
            throw new IllegalArgumentException( "dataProvider cannot be NULL" );
        }

        this.title = title;
        this.dataProvider = dataProvider;
    }

    public TotalItemVolumeComponent(IDataProvider<X> dataProvider) {
        this( "Selected volume", dataProvider );
    }

    @Override
    protected JPanel createPanel()
    {
        textField.setColumns( 10 );
        textField.setEditable( false );
        textField.setHorizontalAlignment( JTextField.TRAILING );

        JPanel textFieldPanel = new JPanel();
        textField.setLayout( new GridBagLayout() );
        textFieldPanel.setBorder( BorderFactory.createTitledBorder( title ) );
        textFieldPanel.add( textField, constraints( 0, 0 ).weightX( 0.5 ).weightY( 0.5 )
                .resizeHorizontally().useRelativeWidth().end() );

        setVolumeLabel( 0.0d );
        
        return textFieldPanel;
    }

    public void setItems(Collection<X> items)
    {
        double volume = 0.0;
        for (X item : items)
        {
            final double quantity = this.dataProvider.getQuantity( item );

            final double volPerUnit = this.dataProvider.getVolumePerUnit( item );

            volume += ( quantity * volPerUnit );
        }
        final double vol = volume;
        setVolumeLabel(vol);
    }

	private void setVolumeLabel(final double vol) 
	{
		runOnEventThread( new Runnable() {

            @Override
            public void run()
            {
                final DecimalFormat FORMAT =
                        new DecimalFormat( "###,###,###,###,##0.0##" );
                textField.setText( FORMAT.format( vol ) + " m3" );
            }
        } );
	}
}
