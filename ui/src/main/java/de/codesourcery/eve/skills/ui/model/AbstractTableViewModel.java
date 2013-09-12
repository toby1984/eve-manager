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
package de.codesourcery.eve.skills.ui.model;


/**
 * Subclasses need to perform proper
 * synchronization using the   
 * @author tobias.gierke@code-sourcery.de
 */
public abstract class AbstractTableViewModel<T> extends AbstractTableModel<T> implements IViewDataModelChangeListener
{
	private final IViewDataModel<T> model;
	
	protected AbstractTableViewModel(TableColumnBuilder builder, IViewDataModel<T> model) {
		super(builder);
		if ( model == null ) {
			throw new IllegalArgumentException("model cannot be NULL");
		}
		this.model = model;
		model.addDataModelChangeListener( this );
	}
	
	protected IViewDataModel<T> getModel() {
		return model;
	}
	
	@Override
	protected void disposeHook()
	{
		model.removeDataModelChangeListener( this );
	}

	@Override
	public T getRow(int modelRow)
	{
		return model.getValueAt( modelRow );
	}

	@Override
	public int getRowCount()
	{
		return model.getSize();
	}

	protected int getRowFor(T obj) {
		return model.getIndexFor( obj );
	}
	
	@Override
	public void dataModelChanged(ViewDataModelChangeEvent event)
	{
		
		if ( log.isDebugEnabled() ) {
			log.debug("dataModelChanged(): Received = "+event);
		}
		
		final int firstRow = Math.min( event.firstRow() , event.lastRow() );
		final int lastRow = Math.max( event.firstRow() , event.lastRow() );
		
		if ( firstRow < 0 || lastRow < 0 ) {
			modelDataChanged();
			return;
		}
		
		switch( event.getType() ) {
			case ITEM_ADDED:
				notifyRowsInserted(firstRow,lastRow);
				break;
			case ITEM_REMOVED:
				notifyRowsRemoved( firstRow , lastRow );
				break;
			case ITEM_CHANGED:
				notifyRowsChanged( firstRow , lastRow );
				break;
			case MODEL_RELOADED:
				modelDataChanged();
				break;
				default:
					log.warn("dataModelChanged(): Ignoring unhandled event "+event);
		}
		
	}
}
