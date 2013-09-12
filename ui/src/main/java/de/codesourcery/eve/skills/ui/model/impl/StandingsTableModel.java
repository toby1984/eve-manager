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
package de.codesourcery.eve.skills.ui.model.impl;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.codesourcery.eve.skills.datamodel.INamedEntity;
import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.ui.model.AbstractTableModel;
import de.codesourcery.eve.skills.ui.model.TableColumnBuilder;

public class StandingsTableModel<T extends INamedEntity> extends AbstractTableModel<Standing<T>> {

	private final DecimalFormat STANDING = new DecimalFormat("#0.00");

	private List<Standing<T>> standings =
		new ArrayList<Standing<T>>();

	private static final class DoubleStringComparator implements Comparator<String> {

		private final DecimalFormat STANDING = new DecimalFormat("#0.00");
		
		@Override
		public int compare(String o1, String o2)
		{
			try {
				final Double val1 = STANDING.parse( o1 ).doubleValue();
				final Double val2 = STANDING.parse( o2 ).doubleValue();
				return val1.compareTo( val2 );
			}
			catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}};
		
	public StandingsTableModel(String nameColumnTitle) {
		super( new TableColumnBuilder().add( nameColumnTitle ).add("Standing" , 
				String.class , new DoubleStringComparator() ) );
	}
	

	@Override
	protected Object getColumnValueAt(int modelRowIndex,
			int modelColumnIndex)
	{
		
		final Standing<T> row = getRow( modelRowIndex );
		
		switch( modelColumnIndex ) {
			case 0:
				return row.getFrom().getName();
			case 1:
				return STANDING.format( row.getValue() );
			default:
				throw new IllegalArgumentException("Invalid column "+modelColumnIndex);
		}
	}

	@Override
	public Standing<T> getRow(int modelRow)
	{
		return standings.get( modelRow );
	}

	@Override
	public int getRowCount()
	{
		return standings.size();
	}
	
	public void refresh(final Collection<Standing<T>> data) {
		
		standings.clear();
		if ( data != null && ! data.isEmpty() ) {
			standings.addAll( data );
			Collections.sort( standings , new Comparator<Standing<T>>() {

				@Override
				public int compare(Standing<T> o1, Standing<T> o2)
				{
					return o1.getFrom().getName().compareTo( o2.getFrom().getName() );
				}} );
		}
		modelDataChanged();
	}

}