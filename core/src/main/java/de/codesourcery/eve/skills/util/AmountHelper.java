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
package de.codesourcery.eve.skills.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import de.codesourcery.eve.skills.utils.ISKAmount;

public class AmountHelper {

	private AmountHelper() {
	}
	
	public static long parseISKAmount(String amount) {
		final DecimalFormat format = 
			new DecimalFormat("###,###,###,###,##0.00");
		try {
			return Math.round( format.parse( amount ).floatValue() * 100.0f );
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static NumberFormat createOrderVolumeNumberFormat() {
		return new DecimalFormat("###,###,###,###,##0.00");
	}
	
	public static String formatISKAmount(ISKAmount amount) {
		
		final DecimalFormat format = 
			new DecimalFormat("###,###,###,###,##0.00");
		return format.format( amount.toDouble() );
	}
	
	public static String formatISKAmount(double amount) {
		
		final DecimalFormat format = 
			new DecimalFormat("###,###,###,###,##0.00");
		return format.format( amount );
	}
	
	public static String formatISKAmount(long amount) {
		
		final DecimalFormat format = 
			new DecimalFormat("###,###,###,###,##0.00");
		return format.format( (double) amount / 100.0d );
	}
	
	public static String formatISKAmount(BigDecimal amount) {
		
		final DecimalFormat format = 
			new DecimalFormat("###,###,###,###,##0.00");
		
		return format.format( amount.doubleValue() / 100.0d );
	}
	
}
