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
package de.codesourcery.eve.skills.ui.spreadsheet;

import java.util.HashMap;



public class CellAttributes extends HashMap<String,Object>
{

	public enum Alignment {
		CENTERED,
		LEFT,
		RIGHT;
	}
	
	/**
	 * Attribute: Render any text in this cell BOLD.
	 * Value: Boolean
	 */
	public static final String  RENDER_BOLD = "bold_text";
	
	/**
	 * Attribute: Alignment of this cell's contents.
	 * Value: {@link Alignment}
	 */
	public static final String  ALIGNMENT = "alignment";
	
	/**
	 * Attribute: Function used to determine whether this 
	 * cell should be rendered as being highlighted
	 * Value: {@link IHighlightingFunction}
	 */
	public static final String HIGHLIGHTING_FUNCTION = "highlighting_function";
	
	public CellAttributes() {
	}
	
	public void setAttribute(String name) {
		put( name , Boolean.TRUE );
	}
	
	public boolean hasAttribute(String name) {
		return containsKey( name );
	}
	
	public Object getAttribute(String name) {
		return get( name );
	}
	
	public void setAttribute(String name,Object value) {
		put( name , value );
	}
}
