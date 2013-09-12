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

import java.awt.Color;
import java.util.Date;

import de.codesourcery.planning.IFactory;
import de.codesourcery.planning.IFactorySlot;
import de.codesourcery.planning.IJob;
import de.codesourcery.planning.ISlotType;

/**
 * Provides various labels used for rendering
 * a planning canvas.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public interface ILabelProvider {

	public String getLabel(IFactorySlot slot);

	public String getLabel(ISlotType type);

	public String getTitle();

	public String getLabel(IFactory f);

	public String getLabel( IJob job );
	
	public Color getColorFor(IJob job);
	
	public String getTimelineLabel(Date date);
}