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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.regex.Pattern;

public final class PlainTextTransferable implements Transferable {

	private static final DataFlavor[] supportedFlavors =
		new DataFlavor[] { DataFlavor.stringFlavor };
	
	private final String text;
	
	public PlainTextTransferable(StringBuffer buffer) {
		this( buffer.toString() );
	}
	
	public PlainTextTransferable(String text) {
		if ( text == null ) {
			throw new IllegalArgumentException("text cannot be NULL");
		}
		this.text = convertLinefeeds( text );
	}
	
	public void putOnClipboard() {
		final Clipboard clipboard = 
			Toolkit.getDefaultToolkit().getSystemClipboard(); 

		clipboard.setContents( this , null );
	}
	
	private static String convertLinefeeds(String input) {
		if ( input.contains( "\r" ) ) {
			return input;
		}
		if ( Misc.NEW_LINE.equals("\n" ) ) {
			return input;
		}
		return input.replaceAll("\n" , Pattern.quote( Misc.NEW_LINE ) );
	}
	
	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException
	{
		if ( ! flavor.equals( DataFlavor.stringFlavor ) ) {
			throw new UnsupportedFlavorException( flavor );
		}
		return text;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors()
	{
		return supportedFlavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return flavor.equals( DataFlavor.stringFlavor );
	}
}
