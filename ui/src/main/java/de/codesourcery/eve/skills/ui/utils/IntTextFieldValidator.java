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

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.StringUtils;

public class IntTextFieldValidator
{
	private final JTextComponent comp;
	private final boolean checkRange;
	private final int minValue;
	private final int maxValue;
	
	private final DocumentFilter filter = new DocumentFilter() {
		
		private final StringBuffer myBuffer = new StringBuffer();
		
		@Override
		public void insertString(FilterBypass fb, int offset,
				String string, AttributeSet attr)
				throws BadLocationException
		{
			if ( isInteger( string ) ) {
				
				if ( checkRange ) {
					myBuffer.setLength( 0 );
					myBuffer.append( comp.getText() );
					myBuffer.insert( offset , string );
					if ( ! isWithinRange( myBuffer.toString() ) ) {
						return;
					}
				}	
				super.insertString(fb, offset, string, attr);
			}
		}
		
		@Override
		public void remove(FilterBypass fb, int offset, int length)
				throws BadLocationException
		{
			if ( checkRange) {
				myBuffer.setLength( 0 );
				myBuffer.append( comp.getText() );
				myBuffer.delete( offset , offset+length );
				if ( !isInteger( myBuffer.toString() ) ||  ! isWithinRange( myBuffer.toString() ) ) 
				{
					return;
				}
			}
			super.remove(fb, offset, length);
		}
		
		@Override
		public void replace(FilterBypass fb, int offset, int length,
				String text, AttributeSet attrs)
				throws BadLocationException
		{
			if ( isInteger( text ) ) {
				if ( checkRange) {
					myBuffer.setLength( 0 );
					myBuffer.append( comp.getText() );
					myBuffer.replace( offset,offset+length,text );
					if ( ! isWithinRange( myBuffer.toString() ) ) {
						return;
					}
				}
				super.replace(fb, offset, length, text, attrs);
			}
		}
	
	};
	
	public IntTextFieldValidator(final JTextComponent comp,int minValue,int maxValue) {
		if ( comp == null ) {
			throw new IllegalArgumentException("component cannot be NULL");
		}
		this.comp = comp;
		this.checkRange = true;
		this.minValue = minValue;
		this.maxValue = maxValue;
		attachFilter( comp );
		
	}
	
	public IntTextFieldValidator(final JTextComponent comp) 
	{
		if ( comp == null ) {
			throw new IllegalArgumentException("component cannot be NULL");
		}
		this.checkRange = false;
		this.comp = comp;
		this.minValue = 0;
		this.maxValue = 0;
		attachFilter( comp );
	}
	
	protected void attachFilter(JTextComponent comp) {

		// Casting is bad although seems to be 
		// the 'official way' as it's in the Swing tutorials as well....
		final AbstractDocument doc = (AbstractDocument) comp.getDocument();

		final String val = comp.getText();
		if ( ! isInteger(val ) || ! isWithinRange( val ) ) {
			comp.setText( Integer.toString( minValue ) );
		}
		doc.setDocumentFilter( filter );
	}
	
	protected boolean isWithinRange(String s) {
		final int val = Integer.parseInt( s );
		return minValue <= val && val <= maxValue;
	}
	
	protected static boolean isInteger(String s) {
		if ( StringUtils.isBlank( s ) ) {
			return false;
		}
		for ( char c : s.toCharArray() ) {
			if ( ! Character.isDigit( c ) && c != '-' ) {
				return false;
			}
		}
		return true;
	}
}
