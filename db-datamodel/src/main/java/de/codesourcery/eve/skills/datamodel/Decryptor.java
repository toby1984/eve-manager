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

public enum Decryptor {

	NONE(1.0f,-4,-4,0) {
		@Override
		public String getDisplayName() {
			return "None";
		}
	},
	TYPE_1(0.6f,-6,-3,9),
	TYPE_2(1.0f,-3,0,2),
	TYPE_3(1.1f,-1,-1,0),
	TYPE_4(1.2f,-2,1,1),
	TYPE_5(1.8f,-5,-2,4);
	
	private final float chanceModifier;
	private final int peModifier;
	private final int meModifier;
	private final int runModifier;
	
	private Decryptor(float chanceModifier, int peModifier, int meModifier,
			int runModifier) 
	{
		this.chanceModifier = chanceModifier;
		this.peModifier = peModifier;
		this.meModifier = meModifier;
		this.runModifier = runModifier;
	}
	
	public String getDisplayName() {
		return "chance "+chanceModifier+", PE "+peModifier+" , ME "+meModifier+", runs "+runModifier;
	}
	
	public float getChanceModifier() {
		return chanceModifier;
	}
	
	public int getPEModifier() {
		return peModifier;
	}
	
	public int getMEModifier() {
		return peModifier;
	}
	
	public int getRunModifier() {
		return runModifier;
	}
	
	private String prettyPrint(int val) {
		if ( val > 0 ) {
			return "+"+val;
		}
		return "";
	}
	
}
