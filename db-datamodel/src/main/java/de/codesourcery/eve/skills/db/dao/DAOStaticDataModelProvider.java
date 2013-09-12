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

import java.util.Collection;
import java.util.List;

import org.springframework.dao.DataRetrievalFailureException;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.SkillTree;
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

public class DAOStaticDataModelProvider implements IStaticDataModelProvider {
	
	private IInventoryCategoryDAO inventoryCategoryDAO;
	private IInventoryGroupDAO inventoryGroupDAO;
	private IInventoryTypeDAO inventoryTypeDAO;
	private IStationDAO stationDAO;
	private ISolarSystemDAO solarSystemDAO;
	private ISkillTreeDAO skillTreeDAO;
	private IFactionDAO factionDAO;
	private IRaceDAO raceDAO;
	private IRegionDAO regionDAO;
	private IConstellationDAO constellationDAO;
	private IBlueprintTypeDAO blueprintTypeDAO;
	private ICorporationDAO corporationDAO;
	private INPCCorporationDAO npcCorporationDAO;
	private IAssemblyLineDAO assemblyLineDAO;
	private IMarketGroupDAO marketGroupDAO;

	private final IStaticDataModel dataModel = new IStaticDataModel() {

		@Override
		public Collection<Constellation> getAllConstellations() {
			return constellationDAO.fetchAll();
		}

		@Override
		public Collection<Faction> getAllFactions() {
			return factionDAO.fetchAll();
		}

		@Override
		public Collection<SolarSystem> getAllSolarSystems() {
			return solarSystemDAO.fetchAll();
		}

		@Override
		public Collection<Station> getAllStations() {
			return stationDAO.fetchAll();
		}

		@Override
		public Constellation getConstellation(Long id) {
			return constellationDAO.fetch( id );
		}

		@Override
		public Faction getFaction(Long id) {
			return factionDAO.fetch( id );
		}

		@Override
		public Race getRace(Long id) {
			return raceDAO.fetch( id );
		}

		@Override
		public Collection<Race> getRaces() {
			return raceDAO.fetchAll();
		}

		@Override
		public SolarSystem getSolarSystem(Long id) {
			return solarSystemDAO.fetch( id );
		}

		@Override
		public Station getStation(Long id) {
			return stationDAO.fetch(id);
		}

		@Override
		public SkillTree getSkillTree() {
			return skillTreeDAO.getSkillTree();
		}

		@Override
		public InventoryGroup getInventoryGroup(Long id) {
			return inventoryGroupDAO.fetch( id );
		}

		@Override
		public InventoryType getInventoryType(Long id) {
			return inventoryTypeDAO.fetch( id );
		}

		@Override
		public InventoryCategory getInventoryCategory(Long id) {
			return inventoryCategoryDAO.fetch( id );
		}

		@Override
		public Blueprint getBlueprintByProduct(InventoryType type) {
			return blueprintTypeDAO.getBlueprintByProduct( type );
		}

		@Override
		public List<Blueprint> getBlueprintsByProductName(String name) {
			return blueprintTypeDAO.getBlueprintsByProductName( name );
		}

		@Override
		public List<InventoryCategory> getInventoryCategories() {
			return inventoryCategoryDAO.fetchAll();
		}

		@Override
		public List<InventoryGroup> getInventoryGroups(InventoryCategory cat) {
			return inventoryGroupDAO.getInventoryGroups( cat );
		}

		@Override
		public List<InventoryType> getInventoryTypes() {
			return inventoryTypeDAO.fetchAll();
		}

		@Override
		public List<InventoryType> getInventoryTypesByName(String name,boolean marketOnly) {
			return inventoryTypeDAO.searchTypesByName( name , marketOnly );
		}

		@Override
		public List<InventoryType> getInventoryTypes(InventoryGroup group) {
			return inventoryTypeDAO.getInventoryTypes( group );
		}

		@Override
		public Blueprint getBlueprint(InventoryType blueprint) {
			return blueprintTypeDAO.getBlueprint( blueprint );
		}

		@Override
		public List<InventoryGroup> getBlueprintProductGroups() {
			return inventoryGroupDAO.getBlueprintProductGroups();
		}

		@Override
		public List<Blueprint> getBlueprintsByProductGroup(InventoryGroup group) {
			return blueprintTypeDAO.getBlueprintsByProductGroup( group  );
		}
		
		@Override
		public Region getRegion(long regionId) {
			return regionDAO.fetch( regionId );
		}

		@Override
		public Blueprint getBlueprintByName(String name)
				throws DataRetrievalFailureException
		{
			return blueprintTypeDAO.getBlueprintByName( name );
		}

		@Override
		public List<Blueprint> getTech2Variations(Blueprint blueprint) {
			return blueprintTypeDAO.getTech2Variations( blueprint );
		}

		@Override
		public Blueprint getTech1Variation(Blueprint tech2Blueprint)
				throws DataRetrievalFailureException
		{
			return blueprintTypeDAO.getTech1Variation( tech2Blueprint );
		}

		@Override
		public NPCCorporation getNPCCorporation(long id)
				throws DataRetrievalFailureException
		{
			return npcCorporationDAO.fetch( id );
		}

		@Override
		public List<AssemblyLine> getAssemblyLines(Region region,
				Activity activity)
		{
			return assemblyLineDAO.getAssemblyLines(region, activity);
		}

		@Override
		public List<AssemblyLine> getAssemblyLines(SolarSystem system,
				Activity activity)
		{
			return assemblyLineDAO.getAssemblyLines(system, activity);
		}

		@Override
		public List<Region> getAllRegions()
		{
			return regionDAO.fetchAll();
		}

		@Override
		public List<SolarSystem> getSolarSystemsFor(Region region,
				Activity activity)
		{
			return assemblyLineDAO.getSolarSystemsFor( region , activity);
		}

		@Override
		public List<Station> getStationsFor(Region region, SolarSystem solarSystem,
				Activity selectedActivity)
		{
			return assemblyLineDAO.getStationsFor(region , solarSystem, selectedActivity );
		}

		@Override
		public List<Station> getStationsFor(Region region, Activity activity)
		{
			return assemblyLineDAO.getStationsFor(region, activity);
		}

		@Override
		public List<AssemblyLine> getAssemblyLines(Station station,
				Activity activity)
		{
			return assemblyLineDAO.getAssemblyLines( station , activity );
		}

		@Override
		public List<ItemWithQuantity> getRefiningOutcome(InventoryType item)
		{
			return blueprintTypeDAO.getRefiningOutcome( item );
		}

		@Override
		public InventoryType getInventoryTypeByName(String name)
		{
			return inventoryTypeDAO.getInventoryTypeByName(name);
		}

		@Override
		public List<MarketGroup> getMarketGroups() {
			return marketGroupDAO.fetchAll();
		}

		@Override
		public List<InventoryType> getInventoryTypes(MarketGroup group) {
			return inventoryTypeDAO.getInventoryTypes( group );
		}

	};
	
	@Override
	public IStaticDataModel getStaticDataModel() {
		return dataModel;
	}
	
	// ==================== setters ================

	public void setInventoryCategoryDAO(IInventoryCategoryDAO inventoryCategoryDAO) {
		this.inventoryCategoryDAO = inventoryCategoryDAO;
	}

	public void setInventoryGroupDAO(IInventoryGroupDAO inventoryGroupDAO) {
		this.inventoryGroupDAO = inventoryGroupDAO;
	}

	public void setInventoryTypeDAO(IInventoryTypeDAO inventoryTypeDAO) {
		this.inventoryTypeDAO = inventoryTypeDAO;
	}

	public void setStationDAO(IStationDAO stationDAO) {
		this.stationDAO = stationDAO;
	}

	public void setSolarSystemDAO(ISolarSystemDAO solarSystemDAO) {
		this.solarSystemDAO = solarSystemDAO;
	}

	public void setSkillTreeDAO(ISkillTreeDAO skillTreeDAO) {
		this.skillTreeDAO = skillTreeDAO;
	}

	public void setFactionDAO(IFactionDAO factionDAO) {
		this.factionDAO = factionDAO;
	}

	public void setRaceDAO(IRaceDAO raceDAO) {
		this.raceDAO = raceDAO;
	}

	public void setBlueprintTypeDAO(IBlueprintTypeDAO blueprintTypeDAO) {
		this.blueprintTypeDAO = blueprintTypeDAO;
	}

	public IBlueprintTypeDAO getBlueprintTypeDAO() {
		return blueprintTypeDAO;
	}

	public void setCorporationDAO(ICorporationDAO corporationDAO) {
		this.corporationDAO = corporationDAO;
	}

	public ICorporationDAO getCorporationDAO() {
		return corporationDAO;
	}

	public void setRegionDAO(IRegionDAO regionDAO) {
		this.regionDAO = regionDAO;
	}

	public void setNpcCorporationDAO(INPCCorporationDAO npcCorporationDAO)
	{
		this.npcCorporationDAO = npcCorporationDAO;
	}

	public void setAssemblyLineDAO(IAssemblyLineDAO assemblyLineDAO)
	{
		this.assemblyLineDAO = assemblyLineDAO;
	}

	public void setMarketGroupDAO(IMarketGroupDAO marketGroupDAO) {
		this.marketGroupDAO = marketGroupDAO;
	}

}
