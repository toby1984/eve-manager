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

public enum SlotLocation {
		NON_CALIDARI_OUTPOST(0.75f, 1.0f,0.75f, 0.75f) {
			@Override
			public String getDisplayName() {
				return "Non-caldari outpost";
			}
		},	
		CALIDARI_OUTPOST(0.5f, 1.0f,0.75f, 0.75f) {
			@Override
			public String getDisplayName() {
				return "Caldari outpost";
			}
		},	
		HIGHSEC_POS(1.5f, 1.0f,0.75f, 0.75f) {
			@Override
			public String getDisplayName() {
				return "POS >= 0.5";
			}
		},
		LOWSEC_POS(1.0f, 1.0f,0.75f, 0.75f) {
			@Override
			public String getDisplayName() {
				return "POS < 0.5";
			}
		},		
		HIGHSEC_NPC_STATION(2.5f,1.0f,1.0f,1.0f) {
			@Override
			public String getDisplayName() {
				return "NPC Station >= 0.5";
			}
		},
		LOWSEC_NPC_STATION(2.0f,1.0f,1.0f,1.0f) {
			@Override
			public String getDisplayName() {
				return "NPC Station <= 0.5";
			}
		};
		
		/*
        * Empire station in 0.5+ - multiplier: 2.5 (10 hours) - 200.000 ISK
        * Empire station in 0.4 and lower - multiplier: 2.0 (8 hours) - 150.000 ISK
        * POS in 0.5+ - multiplier: 1.5 (6 hours)- 120.000 ISK
        * POS in 0.4 or lower - multiplier: 1.0 (4 hours) - 90.000 ISK
        * non-Caldari Outpost - multiplier: 0.75 (3 hours) - 75.000 ISK
        * Caldari Outpost - multiplier: 0.5 (2 hours) - 50.000 ISK 
		 */
		private final float inventionTimeModifier;
		private final float copyTimeModifier;
		private final float researchTimeModifier;
		private final float productionTimeModifier;

		
		private SlotLocation(float inventionTimeModifier,float copyTimeModifier, float researchTimeModifier,
				float productionTimeModifier) {
			this.inventionTimeModifier = inventionTimeModifier;
			this.copyTimeModifier = copyTimeModifier;
			this.researchTimeModifier = researchTimeModifier;
			this.productionTimeModifier = productionTimeModifier;
		}

		public float getInventionTimeModifier() {
			return inventionTimeModifier;
		}
		
		public float getCopyTimeModifier() {
			return copyTimeModifier;
		}
		
		public float getResearchTimeModifier() {
			return researchTimeModifier;
		}
		
		public float getProductionTimeModifier() {
			return productionTimeModifier;
		}
		
		public abstract String getDisplayName();
}
