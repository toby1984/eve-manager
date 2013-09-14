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

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

public class GridLayoutBuilder
{
	
	private final List<ILayoutContainer<?>> elements =
		new ArrayList<ILayoutContainer<?>>();
	
	private boolean debugMode = false;

	public interface ILayoutElement {
		
		/**
		 * Returns the parent container of
		 * this layout element.
		 * 
		 * @return parent or <code>null</code> if this
		 * is a top-level layout element (=has no parent)
		 */
		public ILayoutElement getParent();
		
		public void setParent(ILayoutElement parent);
		
		/**
		 * Adds this layout element at a given
		 * grid location so that it occupies
		 * a given number of table cells. 
		 * 
		 * @param container
		 * @param x absolute X location in table cells (0 = left-most cell)
		 * @param y absolute Y location in table cells (0 = top-most cell)
		 * @param width width in table cells
		 * @param height height in table cells
		 */
		public void addToContainer(IContainer container,int x,int y,int width,int height);
		
		/**
		 * Returns the width of this
		 * layout element in table cells.
		 * @return
		 */
		public int getWidth();
		
		/**
		 * Returns the height of this
		 * layout element in table cells.
		 * @return
		 */
		public int getHeight();
		
		/**
		 * Returns the number of 
		 * table columns this layout
		 * element occupies.
		 * @return
		 */
		public int getColumnCount();
		
		/**
		 * Returns the number of 
		 * table rows this layout
		 * element occupies.
		 * @return
		 */
		public int getRowCount();
	}
	
	public interface IContainer {
		
		/**
		 * 
		 * @param comp
		 * @param x
		 * @param y
		 * @param width
		 * @param height layout hints, may be <code>null</code> or empty.
		 * @param hints
		 */
		public void add(ILayoutElement elem, java.awt.Component comp,int x , int y , int width , int height,EnumSet<LayoutHints> hints);
	}
	
	protected IContainer wrap( final java.awt.Container targetContainer ) {
		
		return new IContainer() {

			@Override
			public void add(ILayoutElement elem,Component comp, int x, int y, int width, int height,EnumSet<LayoutHints> hints)
			{
				final ConstraintsBuilder builder = new ConstraintsBuilder().width( width ).height( height ).x( x ).y( y );
				
				String resize="none";
				if ( hints.contains( LayoutHints.NO_RESIZING ) ) {
					builder.noResizing();
				} else {
					resize="resize_both";
					builder.weightX(0.5d).weightY(0.5d).resizeBoth();
				}
				
				if ( debugMode ) 
				{
					System.out.println(" x="+x+" , y= "+y+" , " +
							"width="+width+" ("+elem.getWidth()+") , height="+height+" ("+elem.getHeight()+") , hints="+hints+", resize="+resize+", component="+elem);
				}				
				
				if ( hints.contains( LayoutHints.ALIGN_RIGHT ) ) {
					builder.anchorEast();
				}
					
				targetContainer.add( comp , builder.end() );
			}};
	}
	
	public enum LayoutHints {
		ALIGN_RIGHT,
		NO_RESIZING;
	}
	
	public static class FixedCell extends Cell {
		
		public FixedCell(Component c) {
			super( c , LayoutHints.NO_RESIZING );
		}
		
		public FixedCell(Component c,LayoutHints... hints) {
			super( c , EnumSet.of( LayoutHints.NO_RESIZING , hints ) );
		}
	}
	
	public static class Cell implements ILayoutElement {

		private final java.awt.Component component;
		private ILayoutElement parent;
		
		public final String name;
		private final int width;
		private final int height;
		private final EnumSet<LayoutHints> hints;
		
		public Cell() {
			this( null );
		}
		
		public Cell(Component component) {
			this( component , 1 , 1 );
		}
		
		public Cell(String name , Component component,EnumSet<LayoutHints> hints) {
			this( component , 1 , 1 ,  name, hints.toArray(new LayoutHints[0]) );
		}
		
		public Cell(Component component,EnumSet<LayoutHints> hints) {
			this( component , 1 , 1 ,  null, hints.toArray(new LayoutHints[0]) );
		}
		
		public Cell(String name , Component component,LayoutHints... hints) {
			this( component , 1 , 1 , name , hints );
		}		
		
		public Cell(Component component,LayoutHints... hints) {
			this( component , 1 , 1 , null, hints );
		}
		
		public Cell(int width,int height) {
			this(null,width,height);
		}
		
		public Cell(String name , Component component,int width,int height) {
			this( component ,width , height , name , (LayoutHints[]) null );
		}
		
		public Cell(Component component,int width,int height) {
			this( component ,width , height , null , (LayoutHints[]) null );
		}
		
		public Cell noResize() {
			hints.add( LayoutHints.NO_RESIZING );
			return this;
		}
		
		public Cell(Component component,int width,int height,String name,LayoutHints... hints) {
			this.component = component;
			this.width = width;
			this.name = name;
			this.height = height;
			if ( ArrayUtils.isEmpty( hints ) ) {
				this.hints = EnumSet.noneOf( LayoutHints.class );
			} else {
				this.hints = ( hints.length == 1 ) ? EnumSet.of( hints[0] ) : 
					EnumSet.of( hints[0] , (LayoutHints[]) ArrayUtils.subarray( hints , 1 , hints.length ) );
			}
		}

		@Override
		public void addToContainer(IContainer container,int x,int y,int width,int height)
		{
			container.add( this, component, x , y , width , height , hints );
		}

		@Override
		public int getColumnCount() { return 0; }

		@Override
		public int getHeight() { return height; }

		@Override
		public int getRowCount() { return 0; }

		@Override
		public int getWidth() { return width; }

		@Override
		public ILayoutElement getParent() { return parent; }

		@Override
		public void setParent(ILayoutElement parent) { this.parent = parent; }
		
		@Override
		public String toString() {
			return name != null ? name : component.toString();
		}
		
	}
	
	public static abstract class AbstractLayoutContainer<T> implements Iterable<ILayoutElement>, ILayoutContainer<T> {
		
		private final List<ILayoutElement> elements =
			new ArrayList<ILayoutElement>();
		
		private ILayoutElement parent;
		
		public AbstractLayoutContainer() {
		}
		
		public AbstractLayoutContainer(ILayoutElement... elements) {
			add( elements );
		}
		
		@Override
		public void setParent(ILayoutElement parent)
		{
			this.parent = parent;
		}
		
		@Override
		public ILayoutElement getParent()
		{
			return parent;
		}

		/* (non-Javadoc)
		 * @see de.codesourcery.eve.skills.ui.utils.ILayoutCOntainer#add(de.codesourcery.eve.skills.ui.utils.GridLayoutBuilder.ILayoutElement)
		 */
		@SuppressWarnings("unchecked")
		public T add(ILayoutElement... elements) {
			for ( ILayoutElement e : elements ) {
				this.elements.add( e );
			}
			return (T) this;
		}
		
		public Iterator<ILayoutElement> iterator() {
			return elements.iterator();
		}
		
		/* (non-Javadoc)
		 * @see de.codesourcery.eve.skills.ui.utils.ILayoutCOntainer#isEmpty()
		 */
		public boolean isEmpty() {
			return elements.isEmpty();
		}
		
		protected List<ILayoutElement> elements() {
			return elements;
		}
		
		@Override
		public int getHeight()
		{
			int maxHeight = 0;
			for ( ILayoutElement e : elements ) {
				final int h = e.getHeight();
				if ( h > maxHeight ) {
					maxHeight = h;
				}
			}
			return maxHeight;
		}

		@Override
		public int getWidth()
		{
			int width = 0;
			for ( ILayoutElement e : elements ) {
				width += e.getWidth();
			}
			return width;
		}
	}
	
	public static class HorizontalGroup extends AbstractLayoutContainer<HorizontalGroup> {

		public HorizontalGroup() {
		}
		
		public HorizontalGroup(ILayoutElement... elements) {
			super( elements );
		}
		
		@Override
		public void addToContainer(IContainer container,int x,int y,int width,int height) 
		{
			int currentX = x;
			int currentY = y;
			int remainingWidth=width;
			
			for ( Iterator<ILayoutElement> it = iterator() ; it.hasNext() ; ) 
			{
				final ILayoutElement e = it.next();
				int w = e.getWidth();
				
				if ( ! it.hasNext() ) { // last element in row occupies remaining width
					w = remainingWidth;
				} 
				
//				if ( e instanceof VerticalGroup ) {
//					w = width;
//				}
				
				e.addToContainer(container, currentX, currentY, w  , height );
				
//				if ( ! ( e instanceof VerticalGroup ) ) {
					remainingWidth -= w;
					currentX += w;
//				}
			}
		}

		@Override
		public int getRowCount()
		{
			int result = 0;
			boolean hasCells = false;
			for(ILayoutElement e: elements() ) {
				if ( e instanceof ILayoutContainer<?> ) {
					result += e.getRowCount();
				} else {
					hasCells = true;
				}
			}
			
			if ( result == 0 ) {
				if ( hasCells ) {
					return 1;
				}
				return 0;
			}
			return 1;			
		}

		@Override
		public int getColumnCount()
		{
			int result = 0;
			boolean hasCells = false;
			for( ILayoutElement e : elements() ) {
				if ( e instanceof ILayoutContainer<?> ) {
					result += e.getColumnCount();
				} else {
					hasCells = true;
				}
			}
			
			if ( result == 0 ) {
				if ( hasCells ) {
					return 1;
				}
			}
			return result;
		}
	}
	
	public static class VerticalGroup extends AbstractLayoutContainer<VerticalGroup> {

		public VerticalGroup() {
		}
		
		public VerticalGroup(ILayoutElement... elements) {
			super( elements );
		}
		
		@Override
		public void addToContainer(IContainer container,int x,int y,int width,int height) 
		{
			int currentX = x;
			int currentY = y;
			int remainingHeight=height;
			
			for ( Iterator<ILayoutElement> it = iterator() ; it.hasNext() ; ) 
			{
				final ILayoutElement e = it.next();
				int h = e.getHeight();
				
				if ( ! it.hasNext() ) { // last element in column occupies any remaining width
					h = remainingHeight;
				} 
				
//				if ( e instanceof HorizontalGroup) {
//					h = height;
//				}
				
				e.addToContainer( container, currentX, currentY, width  , h );
				
				remainingHeight -= h;
				currentY += h;
			}			
		}
		
		@Override
		public int getHeight()
		{
			int maxHeight = 0;
			for ( ILayoutElement e : elements() ) {
				final int h = e.getHeight();
				maxHeight+=h;
			}
			return maxHeight;
		}
		
		@Override
		public int getWidth()
		{
			int width = 0;
			for ( ILayoutElement e : elements() ) {
				final int w = e.getWidth();
				if ( w > width ) {
					width = w;
				}
			}
			return width;
		}

		@Override
		public int getRowCount()
		{
			int result = 0;
			boolean hasCells = false;
			for( ILayoutElement e : elements() ) {
				if ( e instanceof ILayoutContainer<?> ) {
					result += e.getRowCount();
				} else {
					hasCells = true;
				}
			}
			
			if ( result == 0 ) {
				if ( hasCells ) {
					return 1;
				}
			}
			return result;
			
		}

		@Override
		public int getColumnCount()
		{

			int result = 0;
			boolean hasCells = false;
			for(ILayoutElement e: elements() ) {
				if ( e instanceof ILayoutContainer<?> ) {
					result += e.getColumnCount();
				} else {
					hasCells = true;
				}
			}
			
			if ( result == 0 ) {
				if ( hasCells ) {
					return 1; // no containers as children, only cells
				}
				return 0;
			}
			return 1;	
		}
	}
	
	public GridLayoutBuilder add(ILayoutContainer<?> container) {
		elements.add( container );
		return this;
	}
	
	public void addTo(java.awt.Container container) {
		container.setLayout( new GridBagLayout() );
		addTo( wrap( container ) );
	}
	
	public void addTo(IContainer container) {
		
		int x = 0;
		int y = 0;
		int w = getMaxWidth();
		int h = getMaxHeight();
		
		if ( debugMode ) {
			System.out.println("Max. width: "+w);
			System.out.println("Max. height: "+h);
		}
		for ( ILayoutContainer<?> c : this.elements ) {
			if ( c instanceof HorizontalGroup ) {
				c.addToContainer( container , x ,y , w , h );
				x+= w;
			} else if ( c instanceof VerticalGroup ) {
				c.addToContainer( container , x ,y , w , h );
				y+= h;
			}
		}
	}
	
	public int getRowCount() {
		int rows = 0;
		for ( ILayoutContainer<?> c : this.elements ) {
			rows+=c.getRowCount();
		}
		return rows;
	}
	
	public int getColumnCount() {
		int cols = 0;
		for ( ILayoutContainer<?> c : this.elements ) {
			cols+=c.getColumnCount();
		}
		return cols;
	}
	
	public int getMaxHeight() {
		int height = 0;
		for ( ILayoutContainer<?> c : this.elements ) {
			final int h = c.getHeight();
			if ( h > height ) {
				height = h;
			}
		}
		return height;
	}
	
	public int getMaxWidth() 
	{
		int width = 0;
		for ( ILayoutContainer<?> c : this.elements ) 
		{
			final int w = c.getWidth();
			if ( w > width ) {
				width = w;
			}
		}
		return width;
	}
	
	public GridLayoutBuilder enableDebugMode() {
		this.debugMode = true;
		return this;
	}
}
