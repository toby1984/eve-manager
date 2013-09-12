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
package de.codesourcery.eve.skills.db.dao;

import java.util.List;

import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.AssemblyLine;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;

public interface IAssemblyLineDAO extends IReadOnlyDAO<AssemblyLine, Long>  
{

	public List<AssemblyLine> getAssemblyLines(Region region, Activity activity);
	
	public List<SolarSystem> getSolarSystemsFor(Region region, Activity activity);
	
	public List<AssemblyLine> getAssemblyLines(SolarSystem system, Activity activity);

	public List<Station> getStationsFor(Region region, Activity selectedActivity); 
	
	public List<Station> getStationsFor(Region region, SolarSystem solarSystem,Activity selectedActivity);

	public List<AssemblyLine> getAssemblyLines(Station station,
			Activity activity); 
}
