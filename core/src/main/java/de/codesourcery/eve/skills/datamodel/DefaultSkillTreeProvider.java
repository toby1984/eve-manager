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
package de.codesourcery.eve.skills.datamodel;

import java.io.IOException;
import java.util.Date;

import de.codesourcery.eve.apiclient.exceptions.APIErrorException;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.apiclient.parsers.SkillTreeParser;
import de.codesourcery.eve.skills.db.dao.ISkillTreeDAO;
import de.codesourcery.eve.skills.util.ClasspathInputStreamProvider;
import de.codesourcery.eve.skills.util.Misc;
import de.codesourcery.eve.skills.utils.ISystemClock;

/**
 * Skill tree provider that loads skills from an existing XML file.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class DefaultSkillTreeProvider implements ISkillTreeDAO {

	private final SkillTree skillTree;

	public DefaultSkillTreeProvider(ISystemClock clock) throws UnparseableResponseException, APIErrorException, IOException {
		final SkillTreeParser parser = new SkillTreeParser( clock );
		parser.parse( new Date() , Misc.readFile( new ClasspathInputStreamProvider("/skills.xml") ) ); 
		skillTree = parser.getResult();
	}
	
	@Override
	public SkillTree getSkillTree() {
		return skillTree;
	}

}
