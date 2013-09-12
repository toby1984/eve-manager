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
package de.codesourcery.eve.skills.db.datamodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.SkillTree;

public class SkillGroup {
	
	private final SkillTree tree;
	
	private int id;
	private String name;
	private final Map<Integer,Skill> skills = new HashMap<Integer,Skill>();

	@Override
	public String toString() {
		return "SkillGroup[ "+name+" ] = { "+skills.values()+" }";
	}
	
	public SkillGroup(int id ,SkillTree tree) {
		if ( tree == null ) {
			throw new IllegalArgumentException("skill tree cannot be NULL");
		}
		this.id = id;
		this.tree = tree;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.core.ICategory#getName()
	 */
	public String getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( ! (obj instanceof SkillGroup) ) {
			return false;
		}
		
		final SkillGroup cat = (SkillGroup) obj;
		
		if ( this.name != null && cat.name != null) {
			return name.equals( cat.name );
		}
		return this.name == cat.name;
	}
	
	public Skill getSkill(int typeId,boolean failIfMissing) {
		final Skill result = skills.get( typeId );
		if ( result == null && failIfMissing ) {
			throw new RuntimeException("Unknown skill >"+typeId+"<");
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.core.ICategory#getSkill(java.lang.String)
	 */
	public Skill getSkill(int typeId) {
		return getSkill(typeId,true);
	}
	
	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.core.ICategory#setName(java.lang.String)
	 */
	public void setName(String name) {
		if ( name == null || name.trim().length() == 0 ) {
			throw new IllegalArgumentException("name cannot be blank / NULL");
		}
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.core.ICategory#getId()
	 */
	public int getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.core.ICategory#setId(java.lang.String)
	 */
	public void setId(int id ) {
		this .id = id;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.eve.skills.core.ICategory#getSkillTree()
	 */
	public SkillTree getSkillTree() {
		return tree;
	}

	public Skill getOrCreateSkill(int typeId,Boolean isPublished) {
		return tree.getOrCreateSkill( this , typeId , isPublished );
	}
	
	public void addSkill(Skill skill) {
		if ( skills.put( skill.getTypeId() , skill ) != null ) {
			throw new RuntimeException("Duplicate skill "+skill.getTypeId() );
		}
		skill.setCategory( this );
	}

	public Collection<Skill> getSkills() {
		return skills.values();
	}
	
	public long getSkillpoints(ICharacter character) {
		long trained = 0;
		for ( Skill s : this.skills.values() ) {
			trained += character.getSkillLevel( s ).getSkillpoints();
		}
		return trained;
	}
	
	public long getMaximumSkillpoints() {
		long max = 0;
		for ( Skill s : this.skills.values() ) {
			if ( s.isPublished() ) {
				max += s.getMaximumSkillpoints();
			}
		}
		return max;
	}

	public void removeSkill(Skill skill) {
		skills.remove( skill.getTypeId() );
	}

	public Skill getSkillByName(String skillName) {
		
		if (StringUtils.isBlank(skillName)) {
			throw new IllegalArgumentException("skillName cannot be blank.");
		}
		
		for ( Skill s : skills.values() ) {
			if ( skillName.equalsIgnoreCase( s.getName() ) ) {
				return s;
			}
		}
		return null;
	}

}
