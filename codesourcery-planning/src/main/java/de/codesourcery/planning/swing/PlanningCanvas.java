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

import static de.codesourcery.planning.Duration.oneDay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import de.codesourcery.planning.DateRange;
import de.codesourcery.planning.Duration;
import de.codesourcery.planning.Duration.Type;
import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.ISlotType;
import de.codesourcery.planning.swing.DateAxis.ITimelineCallback;
import de.codesourcery.planning.swing.IRegionClickedListener.ClickType;

/**
 * Renders usage of {@link IFactory} instances
 * within a given date range.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class PlanningCanvas extends JPanel
{
	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG_LAYOUT = false;

	private final List<IFactory> factories =
		new ArrayList<IFactory>();

	private final DateAxis dateAxis = new DateAxis();
	
	private final ILabelProvider labelProvider;
	
	private IToolTipProvider toolTipProvider;

	private ISlotRendererFactory rendererFactory = 
		new SimpleSlotRendererFactory();

	/**
	 * Lock that needs to be held while the
	 * {@link #paint(Graphics)} method is executing.
	 */
	private final Object RENDERING_LOCK = new Object();

	private final List<RegionOfInterest> regions =
		new ArrayList<RegionOfInterest> ();

	private final SelectionManager selectionManager = 
		new SelectionManager();
	
	private final class HighlightedArea {
		
		private final RegionOfInterest area;
		private final boolean isSelection;
		
		public HighlightedArea(RegionOfInterest region) {
			this.area = region;
			this.isSelection = false;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if ( obj instanceof HighlightedArea ) {
				return getBounds().equals( ((HighlightedArea) obj).getBounds() );
			}
			return false;
		}
		
		public HighlightedArea(RegionOfInterest region,boolean isSelection) {
			this.area = region;
			this.isSelection = isSelection;
		}
		
		public BoundingBox getBounds() {
			return isSelection ? area.getSelectionHighlightingArea() : area.box;
		}
	}
	
	private final class SelectionManager {

		private RegionOfInterest currentSelection = null;
		private HighlightedArea highlightedArea;
		
		public void setCurrentSelection(Graphics g , RegionOfInterest region) {
			
			if ( equals( currentSelection , region ) ) {
				return;
			}
			
			if ( region == null ) {
				render( g , null );
			} else {
				render( g , new HighlightedArea( region , true ) );
			}
			this.currentSelection = region;
		}
		
		public void highlight( Graphics g , RegionOfInterest interest ) 
		{
			if ( currentSelection != null ) {
				return;
			}
			
			render( g , interest != null ? new HighlightedArea(interest ) : null );
		}
		
		public RegionOfInterest getCurrentSelection() {
			return currentSelection;
		}
		
		private void render(Graphics g, HighlightedArea box ) 
		{
			
			final Graphics2D graphics = (Graphics2D) g;
			
			if ( highlightedArea != null ) 
			{
				if ( highlightedArea.equals( box ) ) {
					return; // the box is already highlighted
				}
				// clear old highlight
				graphics.setXORMode( Color.RED );
				drawSelection( graphics , highlightedArea );
			} else {
				// nothing highlighted
				graphics.setXORMode( Color.RED );
			}

			this.highlightedArea = box;
			if ( box != null ) {
				drawSelection( graphics  , box );
			}
			
			graphics.setPaintMode();
		}
		
		private void drawSelection(Graphics2D g , HighlightedArea area) {
			
			final BoundingBox box = area.getBounds();
			final Stroke s = g.getStroke();
			g.setStroke( new BasicStroke( 3 ) );
			g.drawRect( box.getX() ,
					box.getY() , 
					box.getWidth(),
					box.getHeight() );
			g.setStroke( s );		 
		}
		
		private boolean equals(RegionOfInterest a, RegionOfInterest b ) {
			return ObjectUtils.equals( a , b );
		}

		public void repaint(Graphics g)
		{
			if ( highlightedArea != null ) {
				final Graphics2D graphics = (Graphics2D) g;
				graphics.setXORMode( Color.RED );
				drawSelection( graphics , highlightedArea );
				graphics.setPaintMode();
			}
		}
	}
	
	// guarded-by: RENDERING_LOCK
	private final IRenderOptions options; 

	private final List<IRegionClickedListener> listeners =
		new ArrayList<IRegionClickedListener>();

	private final MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {

		@Override
		public void mouseMoved(MouseEvent e)
		{
			// check if mouse pointer is above a region of interest
			final RegionOfInterest region = 
				(RegionOfInterest) getRegionOfInterest( e.getX(), e.getY() );
			
			selectionManager.highlight( getGraphics() , region );
		}

	};
	
	private interface Notifier {
		public void notify(IRegionClickedListener.ClickType type ,IRegionClickedListener listener);
	}

	private final MouseListener mouseListener = new MouseAdapter() {
		
		private boolean valuesSaved=false;
		private int oldDismissal;
		private int oldInitial;
		
		@Override
		public void mouseEntered(MouseEvent e)
		{
			ToolTipManager manager = ToolTipManager.sharedInstance();
			
			if ( ! valuesSaved ) {
				oldDismissal = manager.getDismissDelay();
				oldInitial = manager.getInitialDelay();
				valuesSaved = true;
			} 
			manager.setDismissDelay( 1000 * 1000 );
			manager.setInitialDelay( 10 );
		}
		
		@Override
		public void mouseExited(MouseEvent e)
		{
			if ( valuesSaved ) {
				final ToolTipManager manager = ToolTipManager.sharedInstance();
				manager.setDismissDelay( oldDismissal );
				manager.setInitialDelay( oldInitial );
			}
		}
		
		@Override
		public void mouseClicked(MouseEvent e)
		{

			final RegionOfInterest region = (RegionOfInterest)
				getRegionOfInterest( e.getX(), e.getY() );

			if ( region == null || region.isSelectable() ) {
				selectionManager.setCurrentSelection( getGraphics() , (RegionOfInterest) region );
			}

			if ( region == null ) {
				return;
			}
			
			final IRegionClickedListener.ClickType clickType;
			if ( e.isPopupTrigger() ) {
				clickType = ClickType.POPUP_TRIGGER;
			} else if ( e.getClickCount() == 1 ) {
				clickType = ClickType.SINGLE_CLICK;
			} else {
				clickType = ClickType.DOUBLE_CLICK;
			}
			
			final Notifier notifier;
			final Object contents = region.getContents();
			if ( contents instanceof IFactorySlot) {
				notifier = new Notifier() {

					@Override
					public void notify(IRegionClickedListener.ClickType type , IRegionClickedListener listener)
					{
						listener.clicked( (IFactorySlot) contents , type );
					}};
			} else if ( contents instanceof PointOnTimeline) {
				notifier = new Notifier() {

					@Override
					public void notify(IRegionClickedListener.ClickType type , IRegionClickedListener listener)
					{
						listener.clicked( (PointOnTimeline) contents , type );
					}};
			} else {
				throw new RuntimeException("Unhandled content: "+contents);
			}

			// notify listeners
			synchronized( listeners ) {
				final List<IRegionClickedListener> copy = 
					new ArrayList<IRegionClickedListener>( listeners );
				for (IRegionClickedListener listener : copy) {
					notifier.notify( clickType , listener );
				} 
			}
		}
	};

	private static final class RegionOfInterest implements IRegionOfInterest {

		private final BoundingBox box;
		private final BoundingBox selectionHighlighting;
		private final Object contents;
		public boolean isSelectable = true;

		public RegionOfInterest(BoundingBox b, Object contents) {
			this( b , null , contents  , true );
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if ( obj instanceof RegionOfInterest ) {
				final RegionOfInterest other = (RegionOfInterest) obj;
				return ObjectUtils.equals( box , other.box ) &&
				ObjectUtils.equals( selectionHighlighting , other.selectionHighlighting );
			}
			return false; 
		}

		public RegionOfInterest(BoundingBox b, BoundingBox selectionHighlightingArea , Object contents) 
		{
			this( b , selectionHighlightingArea , contents , true );
		}
		
		public RegionOfInterest(BoundingBox b, BoundingBox selectionHighlightingArea , Object contents,boolean isSelectable) 
		{
			
			if ( b == null ) {
				throw new IllegalArgumentException("bounding box cannot be NULL");
			}
			if ( contents == null ) {
				throw new IllegalArgumentException("contents cannot be NULL");
			}
			
			this.isSelectable = isSelectable;
			this.selectionHighlighting = selectionHighlightingArea;
			this.box = b;
			this.contents = contents;
		}

		public RegionOfInterest(BoundingBox boundingBox, Object contents,
				boolean selectable) 
		{
			this( boundingBox , null , contents , selectable );
		}

		public boolean contains( int x, int y ) {
			return box.contains( x,y );
		}

		public BoundingBox getSelectionHighlightingArea() {
			return selectionHighlighting != null ? selectionHighlighting : box;
		}

		@Override
		public Object getContents()
		{
			return contents;
		}

		@Override
		public String toString()
		{
			return "pos: "+box+" , contents: "+contents.toString();
		}

		public boolean isSelectable()
		{
			return isSelectable;
		}
	}

	private final class MyRenderOptions implements IRenderOptions {

		private final Font labelFont;

		public MyRenderOptions(Font labelFont) {
			this.labelFont = labelFont;
		}

		@Override
		public int getFactoryTitleYOffset() { return 10; }

		@Override
		public int getTitleYOffset() { return 10; }

		@Override
		public int getSlotYOffset() { return 10; }

		@Override
		public int getSlotTimelineXOffset() { return 5; }

		@Override
		public int getSlotTimelineDrawHeight()  { return 10; }

		@Override
		public int getRightBorder()  { return 10; }
		
		@Override
		public int getLeftBorder()  { return 10; }

		@Override
		public int getSlotTitleRightBorder() { return 10; }

		@Override
		public Font getLabelFont() { return labelFont != null ? labelFont : getFont() ; }

	};

	protected interface ISlotRendererFactory {

		public ISlotRenderer getSlotRenderer(IFactorySlot slot);
	}

	protected ISlotRendererFactory getSlotRendererFactory() {
		return rendererFactory;
	}

	/**
	 * Returns the 'region of interest' for a given
	 * (x,y) viewport coordinate.
	 *  
	 * @param x
	 * @param y
	 * @return region of interest or <code>null</code> if there
	 * is nothing at this coordinate
	 */
	public IRegionOfInterest getRegionOfInterest(int x ,int y ) {

		synchronized (RENDERING_LOCK) {
			for ( RegionOfInterest region : regions ) {
				if ( region.contains( x , y ) ) {
					return region;
				}
			}
		}
		return null;
	}

	protected void addRegionOfInterest(BoundingBox box, Object contents) {
		addRegionOfInterest( box , box , contents );
	}
	
	protected void addRegionOfInterest(BoundingBox box, Object contents,boolean selectable) {
		addRegionOfInterest( box , box , contents , selectable );
	}

	protected void addRegionOfInterest(BoundingBox box, BoundingBox selectionHighlightingArea, Object contents) 
	{
		addRegionOfInterest( box , selectionHighlightingArea , contents ,true );
	}
	
	protected void addRegionOfInterest(BoundingBox box, BoundingBox selectionHighlightingArea, Object contents,boolean selectable) 
	{

		final RegionOfInterest region;
		if ( selectionHighlightingArea != null ) 
		{
			region = new RegionOfInterest(
					new BoundingBox( box ) , new BoundingBox( selectionHighlightingArea ) ,
					contents , selectable );
		} else {
			region = new RegionOfInterest( new BoundingBox( box ) , contents , selectable );
		}

		this.regions.add( region );
	}

	public void addRegionClickedListener(IRegionClickedListener l) {
		if ( l == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized ( listeners ) {
			listeners.add( l );
		}
	}

	public void removeRegionClickedListener(IRegionClickedListener l) {
		if ( l == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		synchronized ( listeners ) {
			listeners.remove( l );
		}
	}

	public IRenderOptions getRenderOptions() {
		return options;
	}

	public PlanningCanvas(ILabelProvider labelProvider) {

		if ( labelProvider == null ) {
			throw new IllegalArgumentException("labelProvider cannot be NULL");
		}

		addMouseListener( this.mouseListener );
		addMouseMotionListener( mouseMotionListener );
		
		this.options = createRenderOptions();
		this.labelProvider = labelProvider;

		setBackground( Color.WHITE);
	}
	
	@Override
	public void addNotify()
	{
		super.addNotify();
		ToolTipManager.sharedInstance().registerComponent( this );
	}
	
	@Override
	public void removeNotify()
	{
		super.removeNotify();
		ToolTipManager.sharedInstance().unregisterComponent( this );
	}

	/**
	 * Subclassing hook, CALLED FROM CONSTRUCTOR !!
	 * @return
	 */
	protected MyRenderOptions createRenderOptions() {

//		final String[] fontnames =
//			GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
//		for (int i = 0; i < fontnames.length; i++) {
//			System.out.println("Got font: "+fontnames[i] );
//		}

		final Font font = Font.decode( "Serif-BOLD-12" );
		Font labelFont;
		if ( font != null ) {
			labelFont = font;
		} else {
			System.out.println("Unable to use custom font Serif-BOLD-12 , font not found");
			labelFont = getFont().deriveFont( Font.BOLD , 14 );
		}
		return new MyRenderOptions( labelFont );
	}

	public void addFactories(IFactory... factories) 
	{
		if ( factories== null ) {
			throw new IllegalArgumentException("factory cannot be NULL");
		}
		
		for ( IFactory f : factories ) {
			this.factories.add( f );
		}
	}

	public void setStartDate(Date startDate)
	{
		if ( startDate == null ) {
			throw new IllegalArgumentException("startDate cannot be NULL");
		}
		
		final Date strippedDate =
			DateUtils.round( startDate , Calendar.DAY_OF_MONTH );
		
		final boolean repaintRequired = ! ObjectUtils.equals( dateAxis.getStartDate(), strippedDate );
		dateAxis.setStartDate( startDate );
		if ( repaintRequired ) {
			repaint();
		}
	}

	public Duration getDateRange()
	{
		return dateAxis.getRange();
	}

	public Date getStartDate()
	{
		return dateAxis.getStartDate();
	}

	public void setDateRange(Duration dateRange)
	{
		if ( dateRange == null ) {
			throw new IllegalArgumentException("dateRange cannot be NULL");
		}
		
		if ( dateRange.isUnknown() ) { 
			throw new IllegalArgumentException("dateRange cannot be UNKNOWN");
		}
		
		final Duration rounded = dateRange.roundToDuration( Type.DAYS );
		
		if ( rounded.shorterThan( oneDay() ) ) {
			throw new IllegalArgumentException("dateRange cannot be shorter than one day");
		}
		

		final boolean repaintRequired = ! ObjectUtils.equals( dateAxis.getRange(), rounded );
		dateAxis.setRange( rounded );
		if ( repaintRequired ) {
			repaint();
		}
	}

	private final class RenderContext {

		public final BoundingBox viewport;
		public final Graphics2D graphics;
		public int currentY = 0;
		public final IRenderOptions options;
		public int  xTimelineStart=0;

		public RenderContext(Graphics2D g,IRenderOptions options) {
			this.graphics=g;
			this.options = options;
			viewport =
				createBoundingBox( options.getLeftBorder() , 0 , 
						getWidth() - options.getRightBorder() , getHeight() );
		}
		
		public Rectangle2D getStringBounds(String s) {
			return graphics.getFontMetrics().getStringBounds( s , graphics );
		}

		public Rectangle2D getLabelBounds(String s) {
			return graphics.getFontMetrics( options.getLabelFont() ).getStringBounds( s , graphics );
		}

		public int getFontAscent() {
			return getFontMetrics( getFont() ).getAscent();
		}

		public void resetRendererState() {
			currentY = 0;
		}
		
		public void drawCenteredString(BoundingBox box, String s ) {
			
			int x= Math.round( box.getX()+ ( box.getWidth() / 2.0f ) );
			int y = Math.round( box.getY() + ( box.getHeight() / 2.0f ) );
			
			final Rectangle2D bounds = getStringBounds( s );
			x -= Math.round( bounds.getWidth() / 2.0f );
			y += Math.round( bounds.getHeight() / 2.0f );
			
			graphics.drawString(s , x, y-2 );
		}

		public void drawString(int x, int y , String s ) {
			graphics.drawString(s , x, y+getFontAscent() );
		}

		public void drawLabel(int x, int y , String s ) {
			final Font old = graphics.getFont();
			graphics.setFont( options.getLabelFont() );
			graphics.drawString(s , x, y+getFontAscent() );
			graphics.setFont( old );
		}
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint(g);

		synchronized( RENDERING_LOCK ) 
		{

			long renderTime = -System.currentTimeMillis();
			final RenderContext ctx = 
				new RenderContext((Graphics2D) g , options );

			render( ctx , true );

			ctx.resetRendererState();

			this.regions.clear(); // populated again by subsequent render() invocation

			render( ctx , false );

			// draw current selection / highlight on top
			selectionManager.repaint( g );

			renderTime += System.currentTimeMillis();
			System.out.println("Rendering time: "+renderTime+" ms.");
		}
	}

	protected void render(RenderContext ctx, boolean layoutOnly) {

		renderTitle(ctx, layoutOnly );

		for ( IFactory f : this.factories ) {
			renderFactory( f , ctx , layoutOnly );
		}
	}

	protected BoundingBox renderFactory(IFactory f , RenderContext ctx, boolean layoutOnly) {

		final BoundingBox box1 = renderFactoryTitle( f , ctx  , layoutOnly );

		final Map<ISlotType, List<IFactorySlot>> slotsByType = 
			f.getSlotsGroupedByType();

		boolean renderDateAxis = true;
		for ( ISlotType type : slotsByType.keySet() ) 
		{
			for ( IFactorySlot slot : slotsByType.get( type ) ) 
			{
				final SlotLayoutHints box =
					renderSlot( slot , ctx , renderDateAxis , layoutOnly );

				if ( renderDateAxis ) { // render date axis only once
					renderDateAxis = false;
				}
				
				if ( box1 != null  && box != null ) {
					box1.add( box.total );
				}
			}
		}

		return box1;
	}

	private static final class SlotLayoutHints {
		public BoundingBox slotTitle;
		public BoundingBox slotTimeline;
		public BoundingBox total;
	}

	private SlotLayoutHints renderSlot(IFactorySlot slot, RenderContext ctx,boolean renderDateAxis,
			boolean layoutOnly)
	{
		
		if ( renderDateAxis ) {
			renderDateAxis( ctx , layoutOnly );
		}

		final BoundingBox slotTitleBounds = 
			renderSlotTitle( slot , ctx  , layoutOnly );

		final BoundingBox timelineBounds =
			renderSlotTimeline( slotTitleBounds.getWidth() , slotTitleBounds.getHeight() , slot , ctx , layoutOnly );

		debugBoundingBox( Color.YELLOW, ctx , timelineBounds , layoutOnly );
		debugBoundingBox( Color.GREEN  ,ctx, slotTitleBounds, layoutOnly);

		slotTitleBounds.add( timelineBounds );

		ctx.currentY += ( slotTitleBounds.getHeight()+ctx.options.getSlotYOffset() );

		final SlotLayoutHints layout = new SlotLayoutHints();
		layout.slotTimeline = timelineBounds;
		layout.slotTitle = slotTitleBounds;
		layout.total = new BoundingBox( timelineBounds , slotTitleBounds )
		.incHeight( ctx.options.getSlotYOffset() ); // include Y offset in bounding box

		if ( ! layoutOnly ) {
			addRegionOfInterest( new BoundingBox( slotTitleBounds , timelineBounds ), slot , false );
		}

		return layout;
	}

	private BoundingBox renderDateAxis(final RenderContext ctx, boolean layoutOnly)
	{
		BoundingBox result= dateAxis.render( createTimelineCallback(ctx , layoutOnly ) , layoutOnly);
		
		ctx.currentY += 20;
		
		return result;
	}

	private ITimelineCallback createTimelineCallback(final RenderContext ctx,boolean layoutOnly)
	{
		int x = layoutOnly ? ctx.options.getLeftBorder() : ctx.xTimelineStart;
		final int y = ctx.currentY;
		// the next line is TRICKY:
		// one the first pass (layoutOnly=true) , ctx.viewPort.getWidth() is equal to this.getWidth()
		// (=the components real width)
		// while on the SECOND pass (layoutOnly=false), ctx.viewPort.getWidth() is already 
		// the width adjusted by the timeline X offset
		final int width = layoutOnly ? ctx.viewport.getWidth() : ctx.viewport.getWidth();

		final BoundingBox box = new BoundingBox( x , y , width , 25 );
		final ITimelineCallback callback = new ITimelineCallback() {

			@Override
			public void drawString(Color color, int x, int y, String s)
			{
				final Color old = ctx.graphics.getColor();
				ctx.graphics.setColor( color );
				ctx.drawString( x , y , s );
				ctx.graphics.setColor( old );
			}

			@Override
			public BoundingBox getBoundingBox()
			{
				return box;
			}

			@Override
			public ILabelProvider getLabelProvider()
			{
				return labelProvider;
			}

			@Override
			public Rectangle2D getStringBounds(String s)
			{
				return ctx.getStringBounds( s );
			}

			@Override
			public Graphics2D getGraphics()
			{
				return ctx.graphics;
			}
		};
		return callback;
	}

	private BoundingBox renderSlotTimeline(int xOffset , int height  , IFactorySlot slot,
			RenderContext ctx, boolean layoutOnly)
	{

		BoundingBox result = null;

		final ITimelineCallback callback = 
			createTimelineCallback(ctx, layoutOnly);

		final Calendar current = Calendar.getInstance();
		current.setTime( dateAxis.getStartDate() );
		current.set(Calendar.MILLISECOND , 0 );

		final Date endDate = dateAxis.getEndDate();
		
		int startX = 0;
		int width = 0;
		while( current.getTime().compareTo( endDate ) <= 0 ) 
		{
			final BoundingBox dateAxisBounds = 
				dateAxis.getBoundingBoxFor( callback , current.getTime() , dateAxis.getTickDuration() );
			
			if ( startX == 0 ) {
				startX = dateAxisBounds.getX();
			}
			if ( width == 0) {
				width = dateAxisBounds.getWidth();
			}
			
			final BoundingBox box =
				renderSlotUsageOnDay( startX ,
					width,
					height , 
					slot , 
					current.getTime() , 
					ctx , 
					layoutOnly 
				);

			if ( result == null ) {
				result = box;
			} else {
				result.add( box );
			}

			if ( ! layoutOnly ) {
				addRegionOfInterest( box , new PointOnTimeline( current.getTime() , slot ) );
			}
			
			startX += width;
			current.add( Calendar.SECOND , (int) Math.floor( dateAxis.getTickDuration().toSeconds() ) );
		}

		final BoundingBox boundingBox = result != null ? result : new BoundingBox();
		return boundingBox;
	}

	/**
	 * Renders usage of a production slot
	 * for a single day.
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
	protected interface ISlotRenderer {
		
		/**
		 * Render slot usage on a given day.
		 * 
		 * The drawable area is currently only restricted in it's
		 * height (meaning each invocation may very well choose to draw
		 * using a different width each time it's called).
		 * 
		 * @param slot the slot for which usage should be rendered
		 * @param date The day on which usage should be rendered (ignore time part)
		 * @param x the upper-left corner (panel viewport coordinates)
		 * @param y the upper-left corner (panel viewport coordinates)
		 * @param maxHeight Maximum height of drawable region that may be used. The
		 * paint region's bounding box therefore has x,y) as top-left corner
		 * and ( ? , y+maxHeight ) as lower-right corner. 
		 * @param ctx rendering context
		 * @param layoutOnly <code>true</code> if nothing
		 * should be drawn , only layout constraints should be calculated 
		 * @return the bounding box that encloses the 
		 * rendered graphics, never <code>null</code>
		 */
		public BoundingBox renderSlot( IFactorySlot slot ,
				Date date , 
				int x , 
				int y ,
				int maxWidth,
				int maxHeight,
				RenderContext ctx , 
				boolean layoutOnly);
	}

	private final class SimpleSlotRendererFactory implements ISlotRendererFactory {

		private final SimpleSlotRenderer renderer = new SimpleSlotRenderer();

		@Override
		public ISlotRenderer getSlotRenderer(IFactorySlot slot)
		{
			return renderer; 
		}

	}

	private final class SimpleSlotRenderer implements ISlotRenderer {

		private NumberFormat DF = new DecimalFormat("#00");
		
		@Override
		public BoundingBox renderSlot(IFactorySlot slot, Date date, int x,
				int y,int maxWith , int maxHeight, RenderContext ctx, boolean layoutOnly)
		{

			if ( ! layoutOnly ) {
				final Color old =
					ctx.graphics.getColor();

				final int boxHeight = maxHeight;

				ctx.graphics.setColor( Color.BLACK );
				ctx.graphics.drawRect( x , y , maxWith, boxHeight );

				final Color color;
				final List<de.codesourcery.planning.IJob> jobsOnDay = 
					slot.getJobsOnDay( date );
				
				if ( ! jobsOnDay.isEmpty() ) {
					if ( jobsOnDay.size() == 1 ) {
						color = getColorForLastJobOnDay( jobsOnDay );
					} else {
						color = Color.RED;
					}
				} else {
					color = Color.GREEN;
				}

				ctx.graphics.setColor( color );
				final Paint oldPaint = ctx.graphics.getPaint();
				if ( jobsOnDay.size() > 1 ) {
					Color previousColor =
						getColorForLastJobOnDay( slot.getJobsOnDay( previousDay( date ) ) );
					if ( previousColor == null ) {
						previousColor = Color.RED;
					}
					
					Color nextColor =
						getColorForLastJobOnDay( slot.getJobsOnDay( nextDay( date ) ) );
					
					if ( nextColor == null ) {
						nextColor = Color.RED;
					}
					ctx.graphics.setPaint( new GradientPaint(x+1 , y+1 , previousColor , x+1+maxWith-1 , y , Color.GREEN ) );
				}
				ctx.graphics.fill( new Rectangle( x+1 , y+1 , maxWith-1, boxHeight-1 ) );
				ctx.graphics.setPaint(oldPaint);
				
				final int utilization = Math.round( slot.getUtilization( DateRange.forDay( date ) ) *100.0f );
				if ( utilization > 0 ) {
					ctx.graphics.setColor( Color.BLACK);
					ctx.drawCenteredString( new BoundingBox(x+1,y+1,x+maxWith-1 , y+boxHeight - 1 ) ,
							DF.format( utilization) );
				}
				ctx.graphics.setColor( old );
			}
			return createBoundingBox( x , y  , maxWith , maxHeight );
		}
		
		private Date nextDay(Date d) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime( d );
			cal.add( Calendar.DAY_OF_MONTH , 1 );
			return cal.getTime();
		}
		
		private Date previousDay(Date d) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime( d );
			cal.add( Calendar.DAY_OF_MONTH , -1 );
			return cal.getTime();
		}
		
		private Color getColorForLastJobOnDay(List<de.codesourcery.planning.IJob> jobsOnDay) {
			
			final Color color;
			if ( ! jobsOnDay.isEmpty() ) {
				if ( jobsOnDay.size() == 1 ) {
					color = labelProvider != null ? labelProvider.getColorFor( jobsOnDay.get(0) ) : null;
				} else {
					color = Color.RED;
				}
			} else {
				color = Color.GREEN;
			}
			return color;
		}

	}

	protected BoundingBox renderSlotUsageOnDay(int x, int maxWidth , int maxHeight , IFactorySlot slot,
			Date time, RenderContext ctx, boolean layoutOnly)
	{
		return getSlotRendererFactory()
		.getSlotRenderer( slot )
		.renderSlot( slot , 
				time , 
				x , 
				ctx.currentY ,
				maxWidth,
				maxHeight,
				ctx , 
				layoutOnly );
	}

	private BoundingBox renderSlotTitle(IFactorySlot slot,
			RenderContext ctx, boolean layoutOnly)
	{
		final String label = labelProvider.getLabel( slot );
		if ( StringUtils.isBlank( label ) ) {
			return null;
		}

		final Rectangle2D labelBounds = ctx.getLabelBounds( label );

		final int y = ctx.currentY;

		int width; 
		if ( ! layoutOnly ) {
			width = ctx.xTimelineStart; 
			ctx.drawLabel( ctx.viewport.getX() , y , label );
		} else {
			width = (int) (labelBounds.getWidth() + ctx.options.getSlotTitleRightBorder());
		}

		final BoundingBox boundingBox = 
			createBoundingBox( ctx.viewport.getX() , y , width , (int) labelBounds.getHeight() );

		// make sure all slot titles are rendered 
		// at the same width ( = width of longest slot title)
		if ( layoutOnly && width > ctx.xTimelineStart ) 
		{
			ctx.xTimelineStart = ctx.viewport.getX()+width;
		}

		return boundingBox;
	}

	// DEBUG ONLY
	protected static void debugBoundingBox(Color color , RenderContext ctx , BoundingBox box , boolean layoutOnly ) {

		if ( DEBUG_LAYOUT && ! layoutOnly ) {
			final Color old = ctx.graphics.getColor();
			ctx.graphics.setColor( color );
			ctx.graphics.drawRect( box.getX() , box.getY() , box.getWidth() , box.getHeight() );
			ctx.graphics.setColor( old );
		}
	}

	private BoundingBox renderFactoryTitle(IFactory f, RenderContext ctx,
			boolean layoutOnly)
	{

		final String label = labelProvider.getLabel( f );

		if ( StringUtils.isBlank(label ) ) {
			return null;
		}

		final int x = ctx.viewport.getX();
		final int y = ctx.currentY;

		final Rectangle2D stringBounds = ctx.getLabelBounds( label );

		ctx.currentY += stringBounds.getHeight() + ctx.options.getFactoryTitleYOffset();

		if ( ! layoutOnly ) {
			ctx.drawLabel( x , y , label );
			return null;
		}

		return createBoundingBox( x , y , stringBounds )
		.incHeight( ctx.options.getFactoryTitleYOffset() ); // include Y offset in bounding box
	}

	protected static BoundingBox createBoundingBox(int x, int y, Rectangle2D stringBounds)
	{
		return BoundingBox.createFromCoordinates(
				new Rectangle2D.Float( 
						x ,
						y , 
						x+Math.round( stringBounds.getWidth() ),
						y+Math.round( stringBounds.getHeight() ) ) 
		);
	}

	protected static BoundingBox createBoundingBox(int x,int y , int width , int height) {
		return BoundingBox.createFromCoordinates(new Rectangle2D.Float( x ,y , x+width , y+height ) );
	}

	protected BoundingBox renderTitle(RenderContext ctx,boolean layoutOnly) {

		final String label = this.labelProvider.getTitle();
		if ( StringUtils.isBlank( label ) ) {
			return null;
		}

		final Rectangle2D labelBox = ctx.getLabelBounds( label );
		final int labelWidth = (int) labelBox.getWidth();

		final int xCenter = ctx.viewport.getX() + ( ctx.viewport.getWidth() / 2 );
		final int xDraw = (int) Math.round( xCenter - ( labelWidth / 2.0d ) );
		final int yDraw = ctx.viewport.getY();

		ctx.currentY += labelBox.getHeight() + ctx.options.getTitleYOffset();

		if ( ! layoutOnly ) {
			ctx.drawLabel( xDraw , yDraw  , label );
			return null;
		}

		return createBoundingBox( 
				ctx.viewport.getX() , 
				ctx.viewport.getY() ,
				labelBox 
		)
		.incHeight( ctx.options.getTitleYOffset() ); // include Y offset in bounding box
	}

	public void setToolTipProvider(IToolTipProvider toolTipProvider)
	{
		this.toolTipProvider = toolTipProvider;
	}

	@Override
	public String getToolTipText(MouseEvent event)
	{
		IRegionOfInterest region = getRegionOfInterest( event.getX() , event.getY() );
		if ( region != null) {
			if ( toolTipProvider != null ) {
				return toolTipProvider.getToolTipText( region );
			}
		}
		return null;
	}
}
