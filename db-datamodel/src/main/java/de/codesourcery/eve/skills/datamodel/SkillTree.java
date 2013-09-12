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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.SkillGroup;

public class SkillTree {
	
	private final Map<Integer,Skill> unknownSkills = 
		new HashMap<Integer,Skill>();

	private final Map<Integer,SkillGroup> categories= 
		new HashMap<Integer,SkillGroup>();

	public SkillTree() {
	}

	public SkillGroup getOrCreateSkillGroup(int id) {
		SkillGroup result = categories.get( id );
		if ( result == null ) {
			result = new SkillGroup( id , this );
			categories.put( id , result );
		}
		return result;
	}
	
	public void validate() {
		
		for ( Iterator<Skill> it = unknownSkills.values().iterator() ; it.hasNext() ; ) {
			final Skill s = it.next();
			getSkill( s.getTypeId() ); // fails if skill is unknown 
			it.remove();
		}
		
		if ( ! unknownSkills.isEmpty() ) {
			System.err.println( unknownSkills.values() );
			
			throw new RuntimeException("Skill tree is invalid: " +
					unknownSkills.size()+" skills are not assigned to categories");
		}
		
		for ( SkillGroup i : getSkillGroups() ) 
		{
			for ( Skill skill : i.getSkills() ) {
				skill.assertIsPublishedFlagSet();
			}
		}
	}
	
	public Skill getOrCreateSkill(SkillGroup cat , int typeId,Boolean isPublished) {
		
		Skill existing = cat.getSkill( typeId , false );
		if ( existing == null ) {
			existing = unknownSkills.get( typeId );
			if ( existing == null ) {
				existing = new Skill( typeId , isPublished );
			} else {
				unknownSkills.remove( existing.getName() );
				existing.updateIsPublished( isPublished );				
			}
			cat.addSkill( existing );
		} else {
			existing.updateIsPublished( isPublished );
		}
		return existing;
	}

	public Skill getOrCreateSkill(int typeId,Boolean isPublished) {

		Skill result = unknownSkills.get( typeId );
		if ( result != null ) {
			result.updateIsPublished( isPublished );
			return result;
		}
		
		for ( SkillGroup cat : this.categories.values() ) {
			result = cat.getSkill( typeId , false );
			if ( result != null ) {
				result.updateIsPublished( isPublished );
				return result;
			}
		}
		
		result = new Skill( typeId , isPublished );
		unknownSkills.put( typeId , result );
		return result;
	}

	public Collection<SkillGroup> getSkillGroups() {
		return categories.values();
	}

	public Skill getSkill(int typeId) {
		for ( SkillGroup cat : this.categories.values() ) {
			Skill result = cat.getSkill( typeId , false );
			if ( result != null ) {
				return result;
			}
		}
		throw new RuntimeException("Unknown skill >"+typeId+"<");
	}

	public void removeSkill(Skill skill) {

		unknownSkills.remove( skill.getName() );
		
		for ( SkillGroup cat : this.categories.values() ) {
			cat.removeSkill( skill );
		}
	}
	
	/**
	 * Debug.
	 * @return
	 */
	public String getAsString() {
		
		final StringBuilder result = new StringBuilder("Skilltree:\n\n");
		
		for ( SkillGroup cat : getSkillGroups() ) {
			result.append("==== SkillGroup ").append( cat.getName()).append("\n");
			for ( Skill s : cat.getSkills() ) {
				result.append("      * ").append(s).append("\n");
				result.append("           Description: ").append( s.getDescription() ).append("\n");
			}
		}
		return result.toString();
	}

	public Skill getSkillByName(String skillName) {
		
		for ( SkillGroup group : this.categories.values() ) {
			final Skill result = group.getSkillByName( skillName );
			if ( result != null ) {
				return result;
			}
		}
		throw new IllegalArgumentException("Found no skill named >"+skillName+"<");
	}

}
