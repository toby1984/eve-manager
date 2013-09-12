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
package de.codesourcery.eve.skills.model;

import de.codesourcery.eve.skills.datamodel.AttributeEnhancer;
import de.codesourcery.eve.skills.datamodel.Character;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.db.dao.ISkillTreeDAO;
import de.codesourcery.eve.skills.db.datamodel.AttributeType;

public class TestCharProvider implements ICharacterProvider {

	private final ICharacter character;
	
	public TestCharProvider(ISkillTreeDAO skillTreeProvider) {
		
		character = new Character("dummy");
		
		// base attributes
		character.getAttributes().setBaseValue(AttributeType.INTELLIGENCE , 20.0f );
		character.getAttributes().setBaseValue(AttributeType.PERCEPTION , 20.0f );
		character.getAttributes().setBaseValue(AttributeType.CHARISMA, 8.0f );
		character.getAttributes().setBaseValue(AttributeType.WILLPOWER, 21.0f );
		character.getAttributes().setBaseValue(AttributeType.MEMORY, 20.0f );
		
		// set implants
		character.getImplantSet().setImplant( 
				new AttributeEnhancer(AttributeType.INTELLIGENCE , 4 , 1 ) 
		);
		
		character.getImplantSet().setImplant( 
				new AttributeEnhancer(AttributeType.PERCEPTION , 4 , 1 ) 
		);
		
		character.getImplantSet().setImplant( 
				new AttributeEnhancer(AttributeType.CHARISMA , 1 , 1 ) 
		);
		
		character.getImplantSet().setImplant( 
				new AttributeEnhancer(AttributeType.WILLPOWER , 4 , 1 ) 
		);
		
		character.getImplantSet().setImplant( 
				new AttributeEnhancer(AttributeType.MEMORY , 4 , 1 ) 
		);
		
		// set base learning skills
		final SkillTree tree = skillTreeProvider.getSkillTree();
	}
	
	@Override
	public ICharacter getCharacer() {
		return character;
	}

}
