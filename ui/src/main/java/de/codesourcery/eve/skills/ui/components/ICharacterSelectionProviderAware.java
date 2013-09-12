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
package de.codesourcery.eve.skills.ui.components;

import de.codesourcery.eve.skills.datamodel.ICharacter;

/**
 * Implemented by classes that need to know when
 * the currently active character changes.
 * 
 * Since Java implements generics using type erasure
 * those classes cannot simply implement <code>ISelectionProviderAware<ICharacter></code>.
 * @author tobias.gierke@code-sourcery.de
 */
public interface ICharacterSelectionProviderAware extends ISelectionProviderAware<ICharacter>
{

}
