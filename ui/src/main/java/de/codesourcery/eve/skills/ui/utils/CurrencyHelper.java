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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import de.codesourcery.eve.skills.util.AmountHelper;

/**
 * 
 * @author tobias.gierke@code-sourcery.de
 * @deprecated Use {@link AmountHelper} instead.
 */
public class CurrencyHelper {

	private static ThreadLocal<NumberFormat> NF = 
		new ThreadLocal<NumberFormat>();
	
	private CurrencyHelper() {
	}
	
	public static final String amountToString(float amount) {
		
		NumberFormat format = NF.get();
		if ( format == null ) {
			format = new DecimalFormat("###,###,##0.00");
			NF.set( format );
		}
		return format.format( amount );
	}
}
