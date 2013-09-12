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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class ViewDataModelChangeEvent
{
	
	public enum Type {
		ITEM_ADDED,
		ITEM_REMOVED,
		ITEM_CHANGED,
		MODEL_RELOADED;
	}
	
	private final Object object;
	private final IViewDataModel<?> model;
	private final int firstRow;
	private final int lastRow;
	private final Type type;
	
	public ViewDataModelChangeEvent(IViewDataModel<?> source) 
	{
		this( source , Type.MODEL_RELOADED );
	}
	
	public ViewDataModelChangeEvent(IViewDataModel<?> source, Type type) 
	{
		if ( type != Type.MODEL_RELOADED ) {
			throw new IllegalArgumentException("This constructor accepts only type "+Type.MODEL_RELOADED);
		}
		if ( source == null ) {
			throw new IllegalArgumentException("source cannot be NULL");
		}
		
		this.object = null;
		this.model = source;
		this.type = type;
		this.firstRow = this.lastRow = -1;
	}
	
	public ViewDataModelChangeEvent(Object object , IViewDataModel<?> source, int row , Type type) 
	{
		if ( type == null ) {
			throw new IllegalArgumentException("Event type cannot be NULL");
		}
		if ( source == null ) {
			throw new IllegalArgumentException("source cannot be NULL");
		}
		
		this.object = object;
		this.model = source;
		this.type = type;
		this.firstRow = this.lastRow = row;
	}
	
	
	public ViewDataModelChangeEvent(Object object, IViewDataModel<?> source, 
			int firstRow , int lastRow , Type type) 
	{
		super();
		if ( source == null ) {
			throw new IllegalArgumentException("source cannot be NULL");
		}
		if ( type == null ) {
			throw new IllegalArgumentException("type cannot be NULL");
		}
		this.firstRow = firstRow;
		this.lastRow = lastRow;
		this.object = object;
		this.model = source;
		this.type = type;
	}
	
	public int firstRow() {
		return firstRow;
	}
	
	public int lastRow() {
		return lastRow;
	}

	public Object getItem()
	{
		return object;
	}

	public IViewDataModel<?> getModel()
	{
		return model;
	}

	public Type getType()
	{
		return type;
	}
	
	@Override
	public String toString()
	{
		return ReflectionToStringBuilder.toString( this );
	}
	
}
