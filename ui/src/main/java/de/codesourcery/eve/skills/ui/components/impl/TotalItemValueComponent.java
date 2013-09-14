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

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.utils.ResizingTextField;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.ISKAmount;

public class TotalItemValueComponent<X> extends AbstractComponent
{
    private final String title;

    private final ResizingTextField textField = new ResizingTextField() {
    	protected java.awt.Dimension calcSize(String text) 
    	{
    		Dimension result1 = super.calcSize(text);
    		Dimension result2 = super.calcSize(title);    
    		return new Dimension(Math.max(result1.width,result2.width) , Math.max(result1.height,result2.height));
    	};
    };

    private final IDataProvider<X> dataProvider;

    public interface IDataProvider<X>
    {
        public int getQuantity(X obj);

        public ISKAmount getPricePerUnit(X obj);
    }

    public TotalItemValueComponent(String title, IDataProvider<X> dataProvider) {

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

    public TotalItemValueComponent(IDataProvider<X> dataProvider) {
        this( "Selected ISK value", dataProvider );
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
                .resizeHorizontally().useRelativeWidth() );

        setISKAmountLabel( ISKAmount.ZERO_ISK );
        return textFieldPanel;
    }

    public void setItems(Collection<X> items)
    {
        long amount = 0;
        for (X item : items)
        {
            final int quantity = this.dataProvider.getQuantity( item );
            final ISKAmount ppU = this.dataProvider.getPricePerUnit( item );

            amount += ppU.multiplyBy( quantity ).toLong();
        }
        setISKAmountLabel( new ISKAmount( amount ) );
    }
    
    private void setISKAmountLabel(final ISKAmount iskAmount) {
        runOnEventThread( new Runnable() {

            @Override
            public void run()
            {
                textField.setText( AmountHelper.formatISKAmount( iskAmount ) + " ISK" );
            }
        } );
    }
}
