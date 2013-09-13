package de.codesourcery.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Helper class to generate an ASCII table.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class StringTablePrinter {

	private final Padding[] padding;
	private final String[] columnTitles;
	
	private final List<String[]> rows = new ArrayList<>();
	
	public static enum Padding {
		LEFT,
		RIGHT,
		CENTERED;
	}
	
	private interface PaddingProvider 
	{
		public Padding getPadding(int colIndex);
	}
	
	public static void main(String[] args) {
		
		StringTablePrinter printer = new StringTablePrinter("first column" , "second column" , "third column");
		
		printer.add( "right" , "center" , "left" );
		printer.add( "1" , "row 2" , "row 2" );
		printer.add( "123" , "row 3" , "row 3" );
		
		printer.padLeft( 0 ).padCenter( 1 ).padRight( 2 );
		
		System.out.println( printer );
	}
	
	public StringTablePrinter(String... columnTitles) 
	{
		if (ArrayUtils.isEmpty( columnTitles ) ) {
			throw new IllegalArgumentException("columnTitles must not be NULL/empty");
		}
		this.columnTitles=columnTitles;
		this.padding = new Padding[columnTitles.length];
		setPaddingForAllColumns(Padding.CENTERED);
	}
	
	public int getColumnCount() {
		return columnTitles.length;
	}
	
	public StringTablePrinter setPaddingForAllColumns(Padding padding) {
		for ( int i = 0 ; i < this.padding.length ; i++ ) 
		{
			this.padding[i] = padding;
		}
		return this;
	}
	
	public StringTablePrinter padCenter(int column) {
		this.padding[column] = Padding.CENTERED;
		return this;
	}
	
	public StringTablePrinter padLeft(int column) {
		this.padding[column] = Padding.LEFT;
		return this;
	}	
	
	public StringTablePrinter padRight(int column) {
		this.padding[column] = Padding.RIGHT;
		return this;
	}		
	
	public StringTablePrinter add(String... columnValues) 
	{
		if (ArrayUtils.isEmpty( columnTitles ) ) {
			throw new IllegalArgumentException("column values must not be NULL/empty");
		}		
		if ( columnValues.length != columnTitles.length ) {
			throw new IllegalArgumentException("value count ("+columnValues.length+") does not match header column count ("+columnTitles.length+")");
		}
		rows.add( columnValues );
		return this;
	}
	
	public StringTablePrinter sortBy(final int column) 
	{
		Collections.sort( rows , new Comparator<String[]>() {

			@Override
			public int compare(String[] row1, String[] row2) {
				return row1[column].compareTo( row2[column] );
			}
		});
		return this;
	}
	
	public String toString() 
	{
		final int[] maxColWidth = new int[ columnTitles.length ];
		
		for ( int i = 0 ; i < columnTitles.length ; i++ ) {
			maxColWidth[i] = columnTitles[i].length();
		}
		
		for ( String[] row : rows ) 
		{
			for ( int i = 0 ; i < columnTitles.length ; i++ ) {
				maxColWidth[i] = Math.max( maxColWidth[i] , row[i].length() );
			}
		}
		
		for ( int i = 0 ; i < columnTitles.length ; i++ ) {
			if ( maxColWidth[i] > 0 ) {
				maxColWidth[i] += 2; // add one whitespace at start and end of each column
			}
			if ( padding[i] == Padding.CENTERED && (maxColWidth[i]%2) != 0 ) // make sure we can properly center text 
			{
				maxColWidth[i]++;
			}
		}
		
		final StringBuilder buffer = new StringBuilder();	
		
		// render table header (column titles are centered)
		renderSeparator('=', maxColWidth, buffer);
		renderRowValues( columnTitles , maxColWidth, buffer , new PaddingProvider() {

			@Override
			public Padding getPadding(int colIndex) {
				return Padding.CENTERED;
			}
		});
		
		renderSeparator('=', maxColWidth, buffer);		
		
		final PaddingProvider provider = new PaddingProvider() {

			@Override
			public Padding getPadding(int colIndex) {
				return padding[colIndex];
			}
		};
		for (Iterator<String[]> it = rows.iterator(); it.hasNext();) 
		{
			final String[] row = it.next();
			renderRowValues( row , maxColWidth, buffer , provider );
			renderSeparator('-', maxColWidth, buffer);	
		}
		return buffer.toString();
	}

	private void renderRowValues(String[] rowValues , final int[] maxColWidth,final StringBuilder buffer,PaddingProvider paddingProvider) {
		for ( int i = 0 ; i < columnTitles.length ; i++ ) 
		{
			if ( i == 0 ) {
				buffer.append("|");
			}
			final String text = pad( rowValues[i] , maxColWidth[i] , paddingProvider.getPadding( i ) );
			buffer.append( text);
			buffer.append("|");
		}
		buffer.append("\n");
	}

	private void renderSeparator(char delimiter, final int[] maxColWidth, final StringBuilder buffer) 
	{
		for ( int i = 0 ; i < columnTitles.length ; i++ ) 
		{
			if ( i == 0 ) {
				buffer.append("+");
			}
			final String text = StringUtils.repeat( Character.toString(delimiter) , maxColWidth[i] );
			buffer.append( text);
			buffer.append("+");
		}
		buffer.append("\n");			
	}
	
	private String pad(String input,int fieldLength, Padding padding) {
		if ( input.length() >= fieldLength ) {
			return input;
		}
		
		switch(padding) 
		{
			case CENTERED:
				int remainingOnEachSide = (fieldLength-input.length())/2;
				return StringUtils.repeat(" " , remainingOnEachSide )+input+StringUtils.repeat(" " , fieldLength-remainingOnEachSide-input.length() );
			case LEFT:
				return StringUtils.repeat(" " , fieldLength-input.length() )+input;
			case RIGHT:
				return input+StringUtils.repeat(" " , fieldLength-input.length() );
			default:
				throw new RuntimeException("Unhandled switch/case: "+padding);
		}
	}
}
