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
import java.util.List;

import org.springframework.dao.DataRetrievalFailureException;

import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.AssemblyLine;
import de.codesourcery.eve.skills.db.datamodel.Constellation;
import de.codesourcery.eve.skills.db.datamodel.Faction;
import de.codesourcery.eve.skills.db.datamodel.InventoryCategory;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.MarketGroup;
import de.codesourcery.eve.skills.db.datamodel.NPCCorporation;
import de.codesourcery.eve.skills.db.datamodel.Race;
import de.codesourcery.eve.skills.db.datamodel.Region;
import de.codesourcery.eve.skills.db.datamodel.SolarSystem;
import de.codesourcery.eve.skills.db.datamodel.Station;

/**
 * Provides access to entities from the static 
 * EVE Online(tm) database export.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IStaticDataModel {

	// solar systems
	public SolarSystem getSolarSystem(Long id);
	
	public Collection<SolarSystem> getAllSolarSystems();
	
	// races
	public Race getRace(Long id);
	
	public Collection<Race> getRaces();
	
	// constellations
	public Constellation getConstellation(Long id);
	
	public Collection<Constellation> getAllConstellations();
	
	// factions
	public Faction getFaction(Long id);
	
	public Collection<Faction> getAllFactions();
	
	// regions
	public List<Region> getAllRegions();
	
	// stations
	public Station getStation(Long id);
	
	public Collection<Station> getAllStations();
	
	// skill tree
	public SkillTree getSkillTree();
	
	// items / inventory types etc.
	public InventoryGroup getInventoryGroup(Long id);
	
	public List<InventoryGroup> getInventoryGroups(InventoryCategory category);
	
	/**
	 * Searches inventory types by 'fuzzy' (substring) matching.
	 * 
	 * @param name
	 * @param marketOnly Only return types that can be traded on the market
	 * @return
	 */	
	public List<InventoryType> getInventoryTypesByName(String name,boolean marketOnly);
	
	/**
	 * Searches an inventory type by name (exact match).
	 * 
	 * @param name
	 * @return
	 */		
	public InventoryType getInventoryTypeByName(String name);
	
	public InventoryType getInventoryType(Long id);
	
	public List<InventoryType>  getInventoryTypes(InventoryGroup group);
	
	public List<InventoryType> getInventoryTypes();
	
	public List<InventoryType> getInventoryTypes(MarketGroup group);
	
	public InventoryCategory getInventoryCategory(Long id);
	
	public List<InventoryCategory> getInventoryCategories();
	
	public List<MarketGroup> getMarketGroups();
	
	// blueprints
	
	/**
	 * 
	 * CATEGORY ( Ship )
	 *    |
	 *    + GROUP ( CRUISER )
	 *        |
	 *        +- Blueprint
	 *        
	 * Returns groups of all ITEMS 
	 * that may be produced from blueprints.
	 */
	public List<InventoryGroup> getBlueprintProductGroups();
	
	/**
	 * Returns all blueprints that produce
	 * items of a given group.
	 * 
	 * @param group
	 * @return
	 * @see #getBlueprintProductGroups()
	 */
	public List<Blueprint> getBlueprintsByProductGroup(InventoryGroup group);
	
	/**
	 * Performs a search for all
	 * blueprints that produce a given item
	 * that is identified by it's name or a part thereof (fuzzy search).
	 *   
	 * @param name
	 * @return
	 */
	public List<Blueprint> getBlueprintsByProductName(String name);
	
	/**
	 * Returns blueprint that produces a given item.
	 * 
	 * @param type
	 * @return
	 * @throws DataRetrievalFailureException if no matching blueprint could be found	 
	 */
	public Blueprint getBlueprintByProduct(InventoryType type);
	
	/**
	 * Returns a blueprint by ID.
	 * 
	 * @param blueprint
	 * @return
	 * @throws DataRetrievalFailureException if no matching blueprint could be found	 
	 */
	public Blueprint getBlueprint(InventoryType blueprint);
	
	/**
	 * Takes a Tech1 blueprint and returns all
	 * Tech2 blueprints that can be invented from it.
	 * 
	 * @param blueprint Tech1 blueprint to find Tech2 variations for
	 * @return
	 */
	public List<Blueprint> getTech2Variations(final Blueprint blueprint);
			
	public Region getRegion(long regionId);

	/**
	 * Retrieves a blueprint by name.
	 * 
	 * @param name
	 * @return
	 * @throws DataRetrievalFailureException if no matching blueprint could be found
	 */
	public Blueprint getBlueprintByName(String name) throws DataRetrievalFailureException;
	
	public Blueprint getTech1Variation(final Blueprint tech2Blueprint) throws DataRetrievalFailureException;
	
	// NPC corps
	public NPCCorporation getNPCCorporation(long id);
	
	// assembly lines
	public List<AssemblyLine> getAssemblyLines(Region region, Activity activity);
	
	public List<AssemblyLine> getAssemblyLines(SolarSystem system, Activity activity);
	
	public List<AssemblyLine> getAssemblyLines(Station station, Activity activity);
	
	/**
	 * Returns all solar systems within a region that have assembly
	 * lines for a given activity.
	 *  
	 * @param region
	 * @param activity
	 * @return
	 */
	public List<SolarSystem> getSolarSystemsFor(final Region region, final Activity activity);

	public List<Station> getStationsFor(Region region , SolarSystem solarSystem,Activity selectedActivity);
	
	public List<Station> getStationsFor(final Region region, final Activity activity);

	/**
	 * 
	 * TODO: Maybe the wrong interface for this method.
	 * @param item
	 * @return returns the refining outcome for this item (assuming perfect skills)
	 */
	public List<ItemWithQuantity> getRefiningOutcome(InventoryType item);
}
