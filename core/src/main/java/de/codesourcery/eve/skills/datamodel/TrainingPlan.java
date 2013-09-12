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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.model.ICharacterProvider;

public class TrainingPlan implements ICharacterProvider {

	public final List<PlannedSkill> plan =
		new ArrayList<PlannedSkill>();
	
	private final CharacterView characterView;
	
	protected final class CharacterView implements ICharacter {

		protected final ICharacter character;
		
		public CharacterView(ICharacter c) {
			this.character = c;
		}
		
		@Override
		public boolean canTrainSkill(Skill s) {
			
			if ( Skill.canBeTrainedBy( s , character ) ) {
				return true;
			}
			
			return Skill.canBeTrainedBy( s , this );
		}
		
		@Override
		public ICharacter cloneCharacter() {
			throw new RuntimeException("cloning not yet implemented");
		}
		
		@Override
		public Attributes getAttributes() {
			return character.getAttributes();
		}
		
		@Override
		public int getCurrentLevel(Skill s) {
			final PlannedSkill planned  = getPlannedSkill( s );
			
			final int realLvl = character.getCurrentLevel( s );
			if ( planned == null) {
				return  realLvl;
			}
			return realLvl > planned.getPlannedTo() ? realLvl : planned.getPlannedTo();
		}
		
		@Override
		public int getCurrentSkillPoints(Skill s) {
			return character.getCurrentSkillPoints(s);
		}
		
		@Override
		public ImplantSet getImplantSet() {
			return character.getImplantSet();
		}
		
		@Override
		public String getName() {
			return character.getName();
		}
		@Override
		public TrainedSkill getSkillLevel(Skill skill) {
			
			PlannedSkill planned = getPlannedSkill( skill );
			
			TrainedSkill trained =
				character.getSkillLevel( skill );;
				
			if ( planned == null ) {
				return trained;
			}
			
			int lvl =
				planned.getPlannedTo() > trained.getLevel() ? planned.getPlannedTo() : trained.getLevel();
			return new TrainedSkill( skill , skill.getSkillpointsForLevel( lvl ) );
		}
		
		@Override
		public int getSkillpoints() {
			return character.getSkillpoints();
		}
		
		@Override
		public boolean hasSkill(Skill s) {
			return hasSkill( s , 1 );
		}
		
		@Override
		public boolean hasSkill(Skill s, int requiredLevel) {
			if ( character.hasSkill( s , requiredLevel ) ) {
				return true;
			}
			PlannedSkill planned = getPlannedSkill(s );
			return planned != null && planned.getPlannedTo() >= requiredLevel;
		}
		
		@Override
		public void setSkill(Skill s, int trainedLevel) {
			throw new UnsupportedOperationException("Do not modify a character directly, modify the training plan instead.");
		}

		@Override
		public Date getLastUpdateTimestamp() {
			return character.getLastUpdateTimestamp();
		}

		@Override
		public CharacterID getCharacterId() {
			return character.getCharacterId();
		}

		@Override
		public void setCharacterId(CharacterID id) {
			character.setCharacterId( id );
		}

		@Override
		public void setName(String name) {
			character.setName( name );
		}

		@Override
		public CharacterDetails getCharacterDetails() {
			return character.getCharacterDetails();
		}

		@Override
		public boolean isFullyInitialized() {
			return character.isFullyInitialized();
		}

		@Override
		public void setFullyInitialized() {
			character.setFullyInitialized();
		}

		@Override
		public Map<Integer, TrainedSkill> getSkills() {
			return character.getSkills();
		}

		@Override
		public void reconcile(ICharacter character) {
			character.reconcile( character );
		}

		@Override
		public long calcTrainingTime(SkillTree skillTree,Skill s, int targetLevel) {
			return character.calcTrainingTime( skillTree , s , targetLevel );
		}

	}
	
	public TrainingPlan(ICharacterProvider charProvider) {
		
		final ICharacter c = charProvider.getCharacer();
		if ( c == null) {
			throw new IllegalArgumentException("character provider returned NULL ?");
		}
		this.characterView = new CharacterView( c );
	}
	
	public ICharacter getCharacter() {
		return characterView;
	}
	
	public List<PlannedSkill> getPlannedSkills() {
		return plan;
	}
	
	public long calcTrainingTime(SkillTree tree) {
		
		long seconds = 0;
		
		final ICharacter cloned = characterView.character.cloneCharacter();
		
		for ( PlannedSkill s : plan ) {
			int currentLevel = cloned.getCurrentLevel( s.getSkill() );
			if ( currentLevel < s.getPlannedTo() ) {
				seconds += s.getSkill().calcTrainingTime( tree, cloned , s.getPlannedTo() );
				cloned.setSkill( s.getSkill() , s.getPlannedTo() );
			}
		}
		return seconds;
	}
	
	public boolean hasSkill(Skill s ) {
		return characterView.hasSkill( s );
	}
	
	public boolean hasSkill(Skill s , int level) {
		return characterView.hasSkill(s , level);
	}
	
	public PlannedSkill getPlannedSkill(Skill s) {
		for ( PlannedSkill planned : plan ) {
			if ( planned.getSkill().equals( s ) ) {
				return planned;
			}
		}
		return null;
	}
	
	public void addSkill( Skill s , int plannedTo) {
	
		if ( hasSkill( s , plannedTo ) ) {
			return ;
		}
		
		final PlannedSkill alreadyPlanned =  
			getPlannedSkill( s );
		
		if ( alreadyPlanned == null ) {
			final PlannedSkill newSkill = new PlannedSkill( s , plannedTo );
			for ( Prerequisite p : s.getPrerequisites() ) {
				
				if ( p.getSkill().equals( s ) ) {
					
					if ( p.getRequiredLevel() > newSkill.getPlannedTo() ) {
						newSkill.setPlannedTo( p.getRequiredLevel() );
					}
					continue;
				}
				
				// add prerequisites
				if ( ! hasSkill( p.getSkill() , p.getRequiredLevel() ) ) {
					addSkill( p.getSkill() , p.getRequiredLevel() );
				}
			}
			plan.add( newSkill );
		} else {
			// TODO: Skill downranking might invalidate previously met prerequisites and
			// TODO: require redrawing etc. ....
			alreadyPlanned.setPlannedTo( plannedTo ); 
		}
	}

	@Override
	public ICharacter getCharacer() {
		return characterView;
	}
	
}
