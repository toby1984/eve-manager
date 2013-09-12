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

import static javax.swing.JOptionPane.ERROR_MESSAGE;

import java.awt.Component;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import org.apache.log4j.Logger;

public class Misc {

	public static final Logger log = Logger.getLogger(Misc.class);
	
	public static final String NEW_LINE;
	
	static {
		NEW_LINE = 
			System.getProperty("line.separator");
	}
	
	private static final NewLine newLine = new NewLine();
	
	public static final class NewLine {
		
		private final String value;
		
		private NewLine() {
			this( NEW_LINE );
		}
		
		private NewLine(String val) {
			 this.value = val;
		}
		
		@Override
		public String toString()
		{
			return value;
		}
		
		public NewLine twice() {
			return new NewLine( NEW_LINE + NEW_LINE );
		}
	}
	
	public static NewLine newLine() {
		return newLine;
	}

	public static void runOnEventThread(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void runOnEventThreadLater(Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}
	
	public static void displayInfo(String message) {
		displayInfo( null , message );
	}

	public static void displayInfo(Component parent, String message) {
		JOptionPane.showMessageDialog(parent, message, "Info",
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * 
	 * @param table <code>JTable</code> that MUST be 
	 * contained in a <code>JScrollPane</code>.
	 * @param row
	 */
	public static void scrollTableToRow(JTable table , int row)
	{

		// get the parent viewport  
		Component parent = table.getParent();  
		while (parent != null && !(parent instanceof JViewport))  
		{  
			parent = parent.getParent();  
		}  
		Rectangle rec = null;  
		int height = table.getRowHeight();  
		if (parent == null)  
		{  
			// no parent so use 0 and 1 as X and width  
			rec = new Rectangle(0, row * height, 1, height);  
		}  
		else  
		{  
			// use the X pos and width of the current viewing rectangle   
			rec = ((JViewport)parent).getViewRect();  
			rec.y = row * height;  
			rec.height = height;  
		}  
		table.scrollRectToVisible(rec);  
	}
	
	public static void displayError(Component parent, String message) {
		displayError(parent, message, null);
	}

	public static void displayError(Throwable t) {
		displayError(null, "An unexpected error occured", t);
	}

	public static void displayError(String message, Throwable t) {
		displayError(null, message, t);
	}
	
	public static int setTooltipDismissalDelay(int milliseconds) {
		final int oldDelay = ToolTipManager.sharedInstance().getDismissDelay();
		ToolTipManager.sharedInstance().setDismissDelay(milliseconds);
		return oldDelay;
	}

	public static void displayError(Component parent, String message, Throwable t) {

		if ( t != null ) {
			log.error("displayError(): " + message, t);
		} else {
			log.error("displayError(): " + message );
		}

		final StringBuilder msg = new StringBuilder(message);

		if (t != null) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();

			final PrintWriter writer = new PrintWriter(out, true);
			t.printStackTrace(writer);

			msg.append("\n\nException:\n\n").append(
					new String(out.toByteArray()));
		}

		JOptionPane.showMessageDialog(parent, msg.toString(), "Error",
				ERROR_MESSAGE);

	}
}
