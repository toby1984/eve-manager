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
package de.codesourcery.eve.skills.production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.db.datamodel.Station;
import de.codesourcery.eve.skills.production.OreRefiningData.OreVariant;
import de.codesourcery.eve.skills.production.OreRefiningData.RefiningOutcome;

public class RefiningCalculator
{
	private final IStaticDataModel dataModel;
	private final OreRefiningData oreRefiningData;
	
	public RefiningCalculator(IStaticDataModel dataModel) {
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.dataModel = dataModel;
		this.oreRefiningData = new OreRefiningData( dataModel );
	}
	
	/**
	 * Calculates refining yield for a given item, character and station.
	 * 
	 * Note that this method does NOT factor in the station tax.
	 * 
	 * @param item
	 * @param character
	 * @param station Station (ONLY used to get the efficiency of the refining plant)
	 * @return yield in percent ( 0..1)
	 */
	public double calculateRefiningYield(InventoryType item,ICharacter character,Station station ) 
	{
		final int refineryEfficiencySkill =
			character.getSkillLevel(
					Skill.getRefineryEfficiencySkill( dataModel.getSkillTree() )
			).getLevel();
		
		final int refiningSkill =
			character.getSkillLevel(
					Skill.getRefiningSkill( dataModel.getSkillTree() )
			).getLevel();
		
		final int scrapMetalProcessingSkill =
			character.getSkillLevel(
					Skill.getScrapMetalProcessingSkill( dataModel.getSkillTree() )
			).getLevel();
		
		final double stationYield = station.getReprocessingEfficiency();
		
		return calculateYield(character, refineryEfficiencySkill,
				refiningSkill, scrapMetalProcessingSkill, stationYield,
				item);		
	}
	
	/**
	 * Takes a list of minerals and quantities and calculates the amount of ore
	 * that would need to be refined in order to yield the requested
	 * quantities.
	 * 
	 * @param requiredMinerals
	 * @param character the character used for refining
	 * @param station station where refining will take place
	 * @param standingWithStationOwner the character's standing with the station owners
	 * @param oreVariant the kind of ore (basic, medium with 5% yield bonus, improved with
	 * 10% yield bonus) to be used for the calculations.
	 * 
	 * @return list of ores with quantities that need to be refined
	 * to yield at least the requested amount of minerals
	 */
	public List<ItemWithQuantity> reverseRefine(List<? extends ItemWithQuantity> requiredMinerals,
			ICharacter character,
			Station station,
			float standingWithStationOwner,
			de.codesourcery.eve.skills.production.OreRefiningData.OreVariant oreVariant) 
	{
		
		if ( oreVariant == null ) {
			throw new IllegalArgumentException("ore variant cannot be NULL");
		}
		
		if ( requiredMinerals.isEmpty() ) {
			return Collections.emptyList();
		}
		
		// merge item quantities by type
		final Map<Long,ItemWithQuantity >  requiredMineralsByType =
			new HashMap<Long,ItemWithQuantity>();
		
		for ( ItemWithQuantity mineral : requiredMinerals ) 
		{
			if ( ! OreRefiningData.isMineral( mineral.getType() ) ) {
				throw new IllegalArgumentException("Not a mineral: "+mineral.getType() );
			}
			
			ItemWithQuantity existing = requiredMineralsByType.get( mineral.getType().getId() );
			/*
			 * Note: It's VITAL that the code following this for() loop operates on COPIES
			 * of the ItemWithQuantity instances provided by the caller (since it will change the quantities) 
			 */
			if ( existing == null ) {
				existing = new ItemWithQuantity( mineral );
				requiredMineralsByType.put( mineral.getType().getId() , existing );
			} else {
				existing.mergeWith( mineral );
			}
		}
		
		/*
		 * Sort minerals descending by required quantity
		 * We try to fulfill greater requested quantities before
		 * those with a lesser amount because most ores yield more
		 * than one mineral type and maybe those 'by-products' will
		 * already meet the demand.
		 */
		final List<ItemWithQuantity> minerals = 
			new ArrayList<ItemWithQuantity>( requiredMineralsByType.values() );
		
		Collections.sort( minerals , new Comparator<ItemWithQuantity>() {

			@Override
			public int compare(ItemWithQuantity o1, ItemWithQuantity o2)
			{
				if ( o1.getQuantity() > o2.getQuantity() ) {
					return -1;
				} else if ( o1.getQuantity() < o2.getQuantity() ) {
					return 1;
				}
				return 0;
			}} );

		final List<ItemWithQuantity> result =
			new ArrayList<ItemWithQuantity>();
		
		final float stationTaxFactor = 
			calculateStationTax(standingWithStationOwner);
		
		boolean moreMineralsRequired = false;
		do
		{
			moreMineralsRequired = false;
			for ( ItemWithQuantity mineral : minerals ) 
			{
				if ( requiredMineralsByType.get( mineral.getType().getId() ).getQuantity() <= 0 ) {
					continue;
				}
				
				moreMineralsRequired = true;
				
				final List<RefiningOutcome> ores = 
					this.oreRefiningData.getOresThatYield( mineral.getType() );

				// find ore with highest yield for the given mineral
				final RefiningOutcome bestOre =
					findOreWithHighestYield( ores , mineral.getType() );

				// get inventory type for the desired target ore quality
				final InventoryType oreType =
					oreVariant.getOreType( dataModel , bestOre );
				
				// calculate yield factor ( 0.00 - 1.0 ) 
				final double yieldFactor =
					calculateRefiningYield( oreType , character, station);
				
				final double requiredQuantity = mineral.getQuantity();
				
				final int minsPerBatch = bestOre.getMineralYieldFor( mineral.getType() ,oreVariant );
				final double realMinsPerBatch = (double) minsPerBatch * yieldFactor;
				int requiredBatches = (int) Math.ceil( requiredQuantity / realMinsPerBatch );
				
				do {
					final double stationTake =
						requiredBatches * realMinsPerBatch * stationTaxFactor;
					if ( ( realMinsPerBatch * requiredBatches - stationTake ) < requiredQuantity ) {
						requiredBatches++;
						continue;
					}
					break;
				} while ( true );
				
				result.add( new ItemWithQuantity( oreType , requiredBatches * oreType.getPortionSize() ) );
				
				// subtract yield from requirements
				for ( ItemWithQuantity yield : bestOre.getOutcome( oreType.getName() ) ) {
					
					final ItemWithQuantity required =
						requiredMineralsByType.get( yield.getType().getId() );
					
					if ( required != null ) {
						final int quantity = 
							(int) ( yield.getQuantity() * requiredBatches * yieldFactor );
					
						required.decQuantity( quantity );
					}
				}
				
			}
		} while ( moreMineralsRequired );
		
		if ( result.isEmpty() ) {
			throw new RuntimeException("Internal error - no suitable ores found ?");
		}
		return result;
	}
	
	private static RefiningOutcome findOreWithHighestYield(List<RefiningOutcome> ores , InventoryType mineral) {
		
		if ( ores.isEmpty() ) {
			throw new IllegalArgumentException("List of ores cannot be empty");
		}
		
		RefiningOutcome result = null;
		for ( RefiningOutcome ore : ores ) 
		{
			if ( result == null ) {
				result = ore;
			} else {
				if (    ore.getMineralYieldFor( mineral , OreVariant.IMPROVED) > 
					 result.getMineralYieldFor( mineral , OreVariant.IMPROVED ) ) 
				{
					result = ore;
				}
			}
		}
		return result;
	}
	
	/**
	 * Calculates outcome of refining one or more items with
	 * a character at a specific station.
	 * 
	 * <pre>
	 * Note that the returned results do <b>NOT</b>
	 * have the {@link RefiningResults#getRefinedValue()}
	 * property populated.
	 * </pre>
	 * @param stuffToRefine
	 * @param character
	 * @param station
	 * @return
	 */
	public List<RefiningResults> refine(List<? extends ItemWithQuantity> stuffToRefine,
			ICharacter character,
			Station station,
			float standingWithStationOwner) 
	{
		
		final List<RefiningResults> results =
			new ArrayList<RefiningResults>();
		
		if ( stuffToRefine.isEmpty() ) {
			return results;
		}
		
		final int refineryEfficiencySkill =
			character.getSkillLevel(
					Skill.getRefineryEfficiencySkill( dataModel.getSkillTree() )
			).getLevel();
		
		final int refiningSkill =
			character.getSkillLevel(
					Skill.getRefiningSkill( dataModel.getSkillTree() )
			).getLevel();
		
		final int scrapMetalProcessingSkill =
			character.getSkillLevel(
					Skill.getScrapMetalProcessingSkill( dataModel.getSkillTree() )
			).getLevel();
		
		final double stationYield = station.getReprocessingEfficiency();
		
		final float taxFactor = calculateStationTax(standingWithStationOwner);
		
		for ( ItemWithQuantity item : stuffToRefine ) {
			
			final InventoryType itemType = item.getType();
			final double yield=
				calculateYield(character, 
					refineryEfficiencySkill,
					refiningSkill, 
					scrapMetalProcessingSkill, 
					stationYield,
					itemType);
			
			final int numberOfPortions = (int) Math.floor( item.getQuantity() / itemType.getPortionSize() );
			final int unrefinedQuantity = item.getQuantity() - ( numberOfPortions * itemType.getPortionSize() );
			
			final RefiningResults result = new RefiningResults( itemType , unrefinedQuantity );

			final List<? extends ItemWithQuantity> refiningOutcome = getRefiningOutcome(itemType);
			for ( ItemWithQuantity material : refiningOutcome ) 
			{
				final int perfectQuantity = numberOfPortions * material.getQuantity();
				final int yourQuantity = (int) Math.round( numberOfPortions * material.getQuantity() * yield);
				result.addResult( new RefiningResult( material.getType() , yourQuantity , perfectQuantity , taxFactor) );
				
			}
			results.add( result );
		}
		
		return results;
	}

	/**
	 * Returns the percentage a station owner will 
	 * take from refined materials given a specific standing.
	 *  
	 * @param standingWithStationOwner
	 * @return station tax in percent ( 0.0 ... 1.0 )
	 */
	public static float calculateStationTax(float standingWithStationOwner)
	{
		float taxFactor;
		if ( standingWithStationOwner <= 0.0 ) {
			taxFactor = 0.05f;
		} else if ( standingWithStationOwner >= 6.67 ) {
			taxFactor = 0.0f;
		} else {
			taxFactor = ( 5.0f - ( 0.75f * standingWithStationOwner ) ) / 100.0f;
			if ( taxFactor > 0.05f ) {
				taxFactor = 0.05f;
			}
		}
		return taxFactor;
	}

	private List<? extends ItemWithQuantity> getRefiningOutcome(final InventoryType itemType)
	{
		if ( itemType.isOre() ) {
			return oreRefiningData.getRefiningOutcome( itemType );
		}
		return dataModel.getRefiningOutcome( itemType );
	}

	/**
	 * 
	 * @param character
	 * @param refineryEfficiencySkill
	 * @param refiningSkill
	 * @param scrapMetalProcessingSkill
	 * @param stationYield
	 * @param itemType
	 * @return refining yield in percent ( 0.0 ... 1.0 )
	 */
	private double calculateYield(ICharacter character,
			final int refineryEfficiencySkill, final int refiningSkill,
			final int scrapMetalProcessingSkill, final double stationYield,
			final InventoryType itemType)
	{
		
		/*
		 * Effective Refining Yield = {Station Equipment Yield} + 0.375 * (1 + (Refining Skill Level} * 0.02)) * 
         * (1 + ( {Refining Efficiency Skill Level} * 0.04)) * (1 + ({Ore Specific Processing Skill Level} * 0.05))
		 */
		
		double yield;
		if ( itemType.isOre() ) 
		{
			final int oreProcessingSkill =
				getOreProcessingSkillLevel(character, itemType);
			
			yield = stationYield+0.375d*( 1.0d + ( refiningSkill*0.02 ) )*
				( 1.0d + ( refineryEfficiencySkill*0.04d ) )*
				( 1.0d + ( oreProcessingSkill*0.05d));
		} else {
			yield = stationYield+0.375d*( 1.0d + ( refiningSkill*0.02 ) )*
			( 1.0d + ( refineryEfficiencySkill*0.04d ) )*
			( 1.0d + ( scrapMetalProcessingSkill*0.05d));
		}
		
		if ( yield > 1.0d ) {
			yield = 1.0d;
		}
		return yield;
	}

	private int getOreProcessingSkillLevel(ICharacter character, final InventoryType itemType)
	{
		final String basicName =
			 oreRefiningData.getBasicVariantName( itemType.getName() ) ;
		
		if ( basicName.equals( itemType.getName() ) ) {
			return character.getSkillLevel( Skill.getOreProcessingSkill( itemType , dataModel.getSkillTree() ) ).getLevel();			
		}
		
		final InventoryType basicType =
			dataModel.getInventoryTypeByName( basicName );
		
		return character.getSkillLevel( Skill.getOreProcessingSkill( basicType , dataModel.getSkillTree() ) ).getLevel();			
	}
}
