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
import java.util.Collections;
import java.util.List;

import de.codesourcery.eve.skills.db.datamodel.Activity;

public class Requirements {

	private final List<RequiredMaterial> requiredMaterials = 
		new ArrayList<RequiredMaterial>();

	private final List<Prerequisite> requiredSkills = 
		new ArrayList<Prerequisite>();
	
	private final Activity activity;

	public Requirements(Activity activity) {
		if (activity == null) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}
		this.activity = activity;
	}

	public void addRequiredMaterial(RequiredMaterial mat) {
		if (mat == null) {
			throw new IllegalArgumentException("material cannot be NULL");
		}
		this.requiredMaterials.add(mat);
	}

	public List<RequiredMaterial> getRequiredMaterials() {
		return Collections.unmodifiableList(requiredMaterials);
	}

	public void addRequiredSkill(Prerequisite r) {
		if (r == null) {
			throw new IllegalArgumentException("required skill cannot be NULL");
		}
		this.requiredSkills.add(r);
	}

	public List<Prerequisite> getRequiredSkills() {
		return Collections.unmodifiableList(requiredSkills);
	}

	public Activity getActivity() {
		return activity;
	}

}
