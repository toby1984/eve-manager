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
package de.codesourcery.planning.swing;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A bounding box that encloses a 
 * rectangular view area.
 * 
 * All rectangles used by this class 
 * are specified as (x,y,width,height)
 * where (x,y) is the rectangle's upper-left corner 
 * and (x+width,y+height) is the rectangle's lower-right
 * corner (all view coordinates). 
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class BoundingBox {

	protected int x1 = 0;
	protected int y1 = 0;
	protected int x2 = 0;
	protected int y2 = 0;

	/**
	 * Creates a bounding box with x=0,y=0,width=0,height=0. 
	 */
	public BoundingBox() {
	}

	/**
	 * Creates bounding box from a rectangle(x1,y1,x2,y2).
	 * 
	 * @param rect Rectangle(x1,y1,x2,y2)
	 * @return a bounding box with x =min(x1,x2 ) , y = min(y1,y2) , 
	 *  			width = max(x1,x2) - min(x1,x2) ,
	 *  			height  = max(y1,y2) - min(y1,y2) )
	 */
	public static BoundingBox createFromCoordinates(Rectangle2D.Float rect) {
		return new BoundingBox( 
				Math.round( rect.x ), 
				Math.round( rect.y ), 
				Math.round( rect.width ), 
				Math.round( rect.height ) );
	}
	
	/**
	 * Tests whether this bounding box encloses
	 * a given point.
	 * 
	 * @param x
	 * @param y
	 * @return <code>true</code> if <code>( x1 <= x && x2 >= x ) && ( y1 <= y && y2 >= y)</code>
	 */
	public boolean contains(int x,int y) {
		return ( x1 <= x && x <= x2 ) && ( y >= y1 && y <= y2 );
	}
	
	/**
	 * Tests whether this bounding box encloses
	 * a given point.
	 * 
	 * @param point
	 * @return <code>true</code> if <code>( x1 <= point.x && x2 >= point.x ) && ( y1 <= point.y && y2 >= point.y)</code>	 
	 */
	public boolean contains(Point2D.Float point) {
		return contains( Math.round( point.x ) , Math.round( point.y ) );
	}

	/**
	 * Creates bounding box from a rectangle(x1,y1,width,height).	 
	 * 
	 * @param rect rectangle(x1,y1,width,height)
	 * @throws IllegalArgumentException when <code>rect</code> is <code>null</code> or
	 * <code>rect</code> has a negative height / width.
	 */
	public BoundingBox(Rectangle2D.Float rect) {

		if (rect == null) {
			throw new IllegalArgumentException("rectangle cannot be NULL");
		}

		if ( rect.width < 0 || rect.height < 0 ) {
			throw new IllegalArgumentException("rect.width and rect.height cannot be negative");
		}

		this.x1 = Math.round( rect.x );
		this.y1 = Math.round( rect.y );
		this.x2 = Math.round( rect.x + rect.width );
		this.y2 = Math.round( rect.y + rect.height );
	}
	
	public BoundingBox incHeight(int increment) {
		if ( increment < 0 ) {
			throw new IllegalArgumentException("Increment cannot be < 0");
		}
		this.y2 += increment;
		return this;
	}
	
	public BoundingBox incWidth(int increment) {
		if ( increment < 0 ) {
			throw new IllegalArgumentException("Increment cannot be < 0");
		}
		this.x2 += increment;
		return this;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param box
	 * @throws IllegalArgumentException when <code>box</code> is <code>null</code>
	 */
	public BoundingBox(BoundingBox...box ) {

		if (box != null && box.length>=1) {
			this.x1 = box[0].x1;
			this.x2 = box[0].x2;
			this.y1 = box[0].y1;
			this.y2 = box[0].y2;
			final int len = box.length;
			for ( int i = 1 ; i < len ; i++ ) {
				add( box[i] );
			}
		}
	}

	/**
	 * Creates a bounding box with width=0 and
	 * height=0 starting at a given point.
	 * 
	 * @param x upper-left corner X
	 * @param y upper-left corner Y
	 */
	public BoundingBox(int x,int y) {
		this.x1 = this.x2 = x;
		this.y1 = this.y2 = y;
	}	

	/**
	 * Creates a bounding box from two points.
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public BoundingBox(int x1,int y1 , int x2 , int y2 ) {

		if ( x1 < x2 ) {
			this.x1 = x1;
			this.x2 = x2;
		} else {
			this.x1 = x2;
			this.x2 = x1;
		}

		if ( y1 < y2 ) {
			this.y1 = y1;
			this.y2 = y2;
		} else {
			this.y1 = y2;
			this.y2 = y1;
		}
	}	

	/**
	 * Returns this bounding boxes left-upper corner.
	 * 
	 * @return (x,y) in view coordinates
	 */
	public Point2D.Float getUpperLeftCorner() {
		return new Point2D.Float( x1 , y1 );
	}

	/**
	 * Returns this bounding boxes lower-right corner.
	 * 
	 * @return (x,y) in view coordinates
	 */	
	public Point2D.Float getLowerRightCorner() {
		return new Point2D.Float( x2 , y2 );
	}


	/**
	 * Returns the X-coordinate of this
	 * bounding boxes left-upper corner.
	 * 
	 * @return x-coordinate (view coordinates)
	 */	
	public int getX() {
		return x1;
	}

	/**
	 * Returns the Y-coordinate of this
	 * bounding boxes left-upper corner.
	 * 
	 * @return y-coordinate (view coordinates)
	 */		
	public int getY() {
		return y1;
	}

	/**
	 * Returns the X coordinate of this
	 * bounding boxes lower-right corner.
	 * 
	 * @return x-coordinate (view coordinates)
	 */		
	public int getMaxX() {
		return x2;
	}

	/**
	 * Returns the Y-coordinate of this
	 * bounding boxes lower-right corner.
	 * 
	 * @return y-coordinate (view coordinates)
	 */			
	public int getMaxY() {
		return y2;
	}

	/**
	 * Returns the width of this bounding box.
	 * 
	 * @return width (view coordinates) , always &gt;= 0
	 */
	public int getWidth() {
		return x2 - x1;
	}

	/**
	 * Returns the height of this bounding box.
	 * 
	 * @return height (view coordinates) , always >= 0
	 */	
	public int getHeight() {
		return y2 - y1;
	}

	/**
	 * Adds a rectangular view area 
	 * described by (x,y,width,height) to this bounding box.
	 * 
	 * This bounding box will be updated so that it
	 * encloses this area.
	 * 
	 * @param tmpRect a rectangle(x,y,width,height) , view coordinates , may be <code>null</code>
	 */
	public BoundingBox add(Rectangle2D.Float tmpRect) {

		if ( tmpRect == null ) {
			return this;
		}

		add( Math.round( tmpRect.x ) , Math.round( tmpRect.y ) );
		add( Math.round( tmpRect.x + tmpRect.width ) , Math.round( tmpRect.y + tmpRect.height ) );
		return this;
	}

	public BoundingBox add(BoundingBox box) {

		if ( box != null ) {
			add( box.getLowerRightCorner() );
			add( box.getUpperLeftCorner() );
		}
		return this;
	}

	public BoundingBox add(int x,int y,int width,int height) {

		if ( x < x1 ) {
			x1 = x;
		}

		final int x2 = x+width; 
		if ( x2 > this.x2 ) {
			this.x2 = x2;
		}

		if ( y < y1 ) {
			y1 = y;
		}

		final int y2 = y+height;
		if ( y2> this.y2 ) {
			this.y2 = y2;
		}

		return this;
	}

	/**
	 * Adds a point to this bounding box.
	 * 
	 * This bounding box will be updated so that
	 * it encloses this point.
	 * 
	 * @param x
	 * @param y
	 */
	public BoundingBox add(int x, int y) {

		if ( x < x1 ) {
			x1 = x;
		}

		if ( x > x2 ) {
			x2 = x;
		}

		if ( y < y1 ) {
			y1 =  y;
		}

		if ( y > y2 ) {
			y2 = y;
		}
		return this;
	}

	/**
	 * Adds a point to this bounding box.
	 * 
	 * This bounding box will be updated so that
	 * it encloses this point.
	 * 
	 * @param point a point (view coordinates) , may be <code>null</code>
	 */
	public BoundingBox add(Point2D.Float point) {

		if ( point != null ) {
			add( Math.round( point.x ) , Math.round( point.y ) );
		}
		return this;
	}

	/**
	 * Adds a point to this bounding box.
	 * 
	 * This bounding box will be updated so that
	 * it encloses this point.
	 * 
	 * @param point a point (view coordinates) , may be <code>null</code>
	 */
	public BoundingBox add(Point point) {

		if ( point != null ) {
			add( point.x , point.y );
		}
		return this;
	}

	/**
	 * Returns the coordinates of this bounding box
	 * as a rectangle(x,y,width,height).
	 * 
	 * @return a bounding box(x,y,width,height)
	 */
	public Rectangle2D.Float toRectangle() {
		return new Rectangle2D.Float( x1 , y1 , getWidth() , getHeight() );
	}

	@Override
	public String toString() {
		return "BoundingBox[ "+ 
		"x1="+x1+
		",y1="+y1+
		",x2="+x2+
		",y2="+y2+
		",width="+getWidth()+
		",height="+getHeight()+
		" ]";
	}
}
