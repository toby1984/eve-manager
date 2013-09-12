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
package de.codesourcery.eve.skills.accountdata;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;

public interface IUserAccountChangeListener {

	public void userAccountAdded(UserAccount account);
	
	public void userAccountAboutToBeRemoved(UserAccount account);
	
	public void userAccountRemoved(UserAccount account);
	
	public void userAccountEdited(UserAccount account);
	
	public void characterAdded(UserAccount account,ICharacter c);
	
	public void characterAboutToRemoved(UserAccount account,ICharacter c);
	
	public void characterRemoved(UserAccount account,ICharacter c);
	
	public void characterEdited(UserAccount account,ICharacter c);
}
