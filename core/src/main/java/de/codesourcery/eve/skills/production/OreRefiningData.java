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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.Station;

/**
 * Class that holds ore refining data not found in the official database dump.
 * 
 * TODO: Hack - database lacks data on ore refining...
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class OreRefiningData
{
	private static final List<String> MINERAL_NAMES;

	private final IStaticDataModel dataModel;
	private SortedMap<String,List<ImmutableItemWithQuantity>> oresByName=null;
	private SortedMap<String , RefiningOutcome> refiningOutcomes;
	private List<String> basicOreNames;

	static 
	{
		MINERAL_NAMES = new ArrayList<String>();
		MINERAL_NAMES.addAll( 
				Arrays.asList( 
						new String[] { 
								"Tritanium" , 
								"Pyerite",
								"Mexallon",
								"Isogen",
								"Nocxium" ,
								"Zydrine",
								"Megacyte",
						"Morphite"}
						));
	}	

	public OreRefiningData(IStaticDataModel dataModel) {
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		this.dataModel = dataModel;
		setup();
	}

	public static boolean isMineral(InventoryType t) {
		return MINERAL_NAMES.contains( t.getName() );
	}

	public static List<String> getMineralNames() {
		return Collections.unmodifiableList( MINERAL_NAMES );
	}

	/**
	 * Desired ore variant to be used
	 * by {@link RefiningCalculator#reverseRefine(List, ICharacter, Station, float)}.
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
	public enum OreVariant {
		BASIC {
			@Override
			protected String getOreVariantName(RefiningOutcome outcome)
			{
				return outcome.getBasicVariantName();
			}

			@Override
			public String getDisplayName()
			{
				return "Basic";
			}

			@Override
			public double getYieldModifier()
			{
				return 1.0d;
			}
		},
		MEDIUM {
			@Override
			protected String getOreVariantName(RefiningOutcome outcome)
			{
				return outcome.getMediumVariantName();
			}

			@Override
			public String getDisplayName()
			{
				return "Medium (+5% yield)";
			}

			@Override
			public double getYieldModifier()
			{
				return 1.05d;
			}
		},
		IMPROVED {
			@Override
			protected String getOreVariantName(RefiningOutcome outcome)
			{
				return outcome.getImprovedVariantName();
			}

			@Override
			public String getDisplayName()
			{
				return "Improved (+10% yield)";
			}

			@Override
			public double getYieldModifier()
			{
				return 1.1d;
			}
		};

		protected abstract String getOreVariantName(RefiningOutcome outcome);

		protected InventoryType getOreType(IStaticDataModel dataModel , RefiningOutcome outcome) {
			return dataModel.getInventoryTypeByName( getOreVariantName( outcome ) );
		}

		public abstract String getDisplayName();

		public abstract double  getYieldModifier();
		
		@Override
		public final String toString() {
			return getDisplayName();
		}		
	}

	/**
	 * Holds data about the refining outcome
	 * for a single batch of a given ore.
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
	public final class RefiningOutcome {

		protected final InventoryType[] oreTypes;

		protected String[] ores;

		protected int trit;
		protected int pyerite;
		protected int mex;
		protected int iso;
		protected int noc;
		protected int zyd;
		protected int meg;
		protected int mor;

		public RefiningOutcome(InventoryType[] oreTypes) 
		{
			if (oreTypes == null) {
				throw new IllegalArgumentException("oreTypes must not be NULL");
			}
			this.oreTypes = oreTypes;
		}
		
		public InventoryType getType(OreVariant variant) 
		{
			switch(variant) {
				case BASIC:
					return oreTypes[0];
				case MEDIUM:
					return oreTypes[1];
				case IMPROVED:
					return oreTypes[2];
				default:
					throw new IllegalArgumentException("Unhandled switch/case: "+variant);
			}
		}
		
		private InventoryType getBasicType() {
			return getType(OreVariant.BASIC);
		}

		@Override
		public boolean equals(Object obj)
		{
			if ( obj instanceof RefiningOutcome ) {
				return getBasicType().getId().equals( ((RefiningOutcome) obj).getBasicType().getId() );
			}
			return false; 
		}

		public boolean yields(InventoryType mineral) {
			return getMineralYieldFor( mineral , OreVariant.BASIC) > 0 ; 
		}

		@Override
		public int hashCode()
		{
			return getBasicType().getId().hashCode(); 
		}

		/**
		 * Returns the outcome of refining one batch
		 * of a given ore.
		 * 
		 * @param name
		 * @return
		 */
		public List<ImmutableItemWithQuantity> getOutcome(String name) {
			float bonus=1.0f;
			boolean found = false;
			for ( String ore : ores ) {
				if ( ore.equals( name ) ) {
					found = true;
					break;
				}
				bonus += 0.05f;
			}
			if ( ! found ) {
				throw new IllegalArgumentException("Unknown ore name '"+name+"'");
			}

			final List<ImmutableItemWithQuantity> result =
					new ArrayList<ImmutableItemWithQuantity> ();

			if ( trit != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Tritanium" ) ,  trit , bonus ) );
			if ( pyerite != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Pyerite" ) ,  pyerite, bonus ) );
			if ( mex != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Mexallon" ) ,  mex, bonus  ) );
			if ( iso != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Isogen" ) ,  iso, bonus  ) );
			if ( noc != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Nocxium" ) ,  noc, bonus ) );
			if ( zyd != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Zydrine" ) ,  zyd, bonus ) );
			if ( meg  != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Megacyte" ) ,  meg, bonus  ) );
			if ( mor != 0 ) result.add( new ImmutableItemWithQuantity( dataModel.getInventoryTypeByName( "Morphite" ) ,  mor, bonus  ) );

			return result;
		}

		public int getMineralYieldFor(InventoryType mineral, OreVariant oreVariant) {
			if ( mineral == null ) {
				throw new IllegalArgumentException("mineral cannot be NULL");
			}
			return getMineralYieldFor( mineral.getName(), oreVariant );
		}

		public int getMineralYieldFor(String mineralName,OreVariant oreVariant) {

			if ( StringUtils.isBlank(mineralName) ) {
				throw new IllegalArgumentException(
						"mineral name cannot be blank.");
			}

			int result;
			if ( "Tritanium".equals( mineralName) ) {
				result = trit;
			} else if ( "Pyerite".equals( mineralName) ) {
				result = pyerite;
			} else if ( "Mexallon".equals( mineralName) ) {
				result = mex;
			} else if ( "Isogen".equals( mineralName) ) {
				result = iso;
			} else if ( "Nocxium".equals( mineralName) ) {
				result = noc;
			} else if ( "Zydrine".equals( mineralName) ) {
				result = zyd;
			} else if ( "Megacyte".equals( mineralName) ) {
				result = meg;
			} else if ( "Morphite".equals( mineralName) ) {
				result = mor;
			} else {
				throw new RuntimeException("Unreachable code reached.");
			}

			return (int) Math.floor( result*oreVariant.getYieldModifier() );
		}

		public void addToMap(Map<String,List<ImmutableItemWithQuantity> > map) {

			for ( String ore : ores ) {
				map.put( ore , getOutcome( ore ) );
				refiningOutcomes.put( ore , this );
			}

		}

		/**
		 * Returns the name for the basic (0% yield bonus)
		 * variant of this ore.
		 * 
		 * @return
		 */
		public String getBasicVariantName()
		{
			return ores[0];
		}
		
		public String getVariantName(OreVariant variant) 
		{
			if (variant == null) {
				throw new IllegalArgumentException("variant must not be NULL");
			}
			switch(variant) {
				case BASIC:
					return getBasicVariantName();
				case MEDIUM:
					return getMediumVariantName();
				case IMPROVED:
					return getImprovedVariantName();
				default:
					throw new IllegalArgumentException("Unhandled switch/case: "+variant);
			}
		}

		/**
		 * Returns the name for the intermediate (5% yield bonus)
		 * variant of this ore.
		 * 
		 * @return
		 */
		public String getMediumVariantName()
		{
			return ores[1];
		}

		/**
		 * Returns the name for the improved (10% yield bonus)
		 * variant of this ore.
		 * 
		 * @return
		 */		
		public String getImprovedVariantName()
		{
			return ores[2];
		}
	}

	/**
	 * Returns the name of an ore's 'basic' variant.
	 * 
	 * @param oreName
	 * @return
	 */
	public String getBasicVariantName(String oreName) {
		final RefiningOutcome outcome = refiningOutcomes.get( oreName );
		if ( outcome == null ) {
			throw new IllegalArgumentException("Unknown ore name '"+oreName+"'");
		}
		return outcome.getBasicVariantName();
	}

	public List<String> getOreNames(OreVariant variant) {

		final Set<RefiningOutcome> tmp = new HashSet<RefiningOutcome> ( this.refiningOutcomes.values() );

		final List<String> result = new ArrayList<String>();
		for ( RefiningOutcome outcome : tmp )
		{
			result.add( outcome.getVariantName( variant ) );
		}
		return result;
	}
	
	public InventoryType getVariantType(String oreName,OreVariant variant) 
	{
		final Set<RefiningOutcome> tmp = new HashSet<RefiningOutcome> ( this.refiningOutcomes.values() );

		for ( RefiningOutcome outcome : tmp )
		{
			for (OreVariant v : OreVariant.values() ) {
				if ( oreName.equals( outcome.getVariantName( v ) ) ) 
				{
					return outcome.getType( variant );
				}
			}
		}
		throw new IllegalArgumentException("Unknown ore name: >"+oreName+"<");
	}

	public List<String> getAllBasicOreNames() {

		if ( basicOreNames == null ) 
		{
			// gather unique outcomes
			final Set<RefiningOutcome> tmp = new HashSet<RefiningOutcome> ( this.refiningOutcomes.values() );

			final List<String> result = new ArrayList<String>();
			for ( RefiningOutcome outcome : tmp )
			{
				result.add( outcome.getBasicVariantName() );
			}
			basicOreNames = result;
		}
		return basicOreNames;
	}

	private static final class ImmutableItemWithQuantity extends ItemWithQuantity {

		public ImmutableItemWithQuantity(InventoryType type,
				int quantity , float bonus ) 
		{
			super( type , Math.round( quantity * bonus ) );
		}

		@Override
		public void setQuantity(int quantity)
		{
			throw new UnsupportedOperationException("Cannot invoke setQuantity() on immutable object");
		}

		@Override
		public void setType(InventoryType type)
		{
			throw new UnsupportedOperationException("Cannot invoke setType() on immutable object");
		}
	}

	private void addOutcome(String[] oreNames, 
			int trit, int pyerite,int mex, int iso, int noc, int zyd, int meg,int mor) 
	{ 
		final InventoryType[] types = new InventoryType[ oreNames.length ];
		for (int i = 0 ; i < oreNames.length ; i++ ) {
			types[i] = this.dataModel.getInventoryTypeByName( oreNames[i] );
		}
		final RefiningOutcome result = new RefiningOutcome( types );

		result.ores = oreNames;
		result.trit = trit;
		result.pyerite = pyerite;
		result.mex = mex;
		result.iso = iso;
		result.noc = noc;
		result.zyd = zyd;
		result.meg = meg;
		result.mor = mor;

		result.addToMap( this.oresByName );
	}

	private void setup() {

		oresByName = new TreeMap<String, List<ImmutableItemWithQuantity>>();
		refiningOutcomes = new TreeMap<String, RefiningOutcome>();

		addOutcome( new String[]{ "Veldspar" , "Concentrated Veldspar" , "Dense Veldspar" } , 1000 ,0,0,0,0,0,0,0 );
		addOutcome( new String[]{ "Scordite" , "Condensed Scordite" , "Massive Scordite" }, 833,416,0,0,0,0,0,0 );
		addOutcome( new String[]{ "Pyroxeres" , "Solid Pyroxeres" , "Viscous Pyroxeres" },844,59,120,0,11,0,0,0 ); 
		addOutcome( new String[]{ "Plagioclase" , "Azure Plagioclase" , "Rich Plagioclase" },256,512,256,0,0,0,0,0);
		addOutcome( new String[]{ "Omber" , "Silvery Omber" , "Golden Omber" }, 307,123,0,307,0,0,0,0);
		addOutcome( new String[]{ "Kernite" , "Luminous Kernite" , "Fiery Kernite" }, 386,0,773,386,0,0,0,0);
		addOutcome( new String[]{ "Jaspet" ,"Pure Jaspet" , "Pristine Jaspet" }, 259,259,518,0,259,8,0,0);
		addOutcome( new String[]{ "Hemorphite" , "Vivid Hemorphite" , "Radiant Hemorphite" }, 212,0,0,212,424,28,0,0);
		addOutcome( new String[]{ "Hedbergite" , "Vitric Hedbergite" , "Glazed Hedbergite" }, 0,0,0,708,354,32,0,0);
		addOutcome( new String[]{ "Gneiss" , "Iridescent Gneiss" , "Prismatic Gneiss" }, 3700 , 0 ,3700 ,700 ,0,171,0,0);
		addOutcome( new String[]{ "Dark Ochre" , "Onyx Ochre" , "Obsidian Ochre" }, 25500,0,0,0,500,250,0,0);
		addOutcome( new String[]{ "Crokite" , "Sharp Crokite" , "Crystalline Crokite" }, 38000 ,0,0,0,331,663,0,0);
		addOutcome( new String[]{ "Spodumain" , "Bright Spodumain" , "Gleaming Spodumain" }, 71000 ,9000 ,0,0,0,0,140,0);
		addOutcome( new String[]{ "Bistot" , "Triclinic Bistot" , "Monoclinic Bistot" }, 0,1200,0,0,0,341,170,0);
		addOutcome( new String[]{ "Arkonor" , "Crimson Arkonor" , "Prime Arkonor" }, 10000,0,0,0,0,166,333,0);
		addOutcome( new String[]{ "Mercoxit" , "Magma Mercoxit" , "Vitreous Mercoxit" } , 0,0,0,0,0,0,0,530);
	}

	public List<? extends ItemWithQuantity> getRefiningOutcome(String oreName) 
	{
		final List<? extends ItemWithQuantity> result= oresByName.get( oreName );

		if ( result == null ) {
			throw new IllegalArgumentException("Cannot determine refining outcome of ore '"+oreName+"'");
		}
		return result;		
	}

	public RefiningOutcome getRawOutcome(String ore) {
		return refiningOutcomes.get( ore );
	}

	public List<RefiningOutcome> getOresThatYield(InventoryType mineral) {

		final List<RefiningOutcome> result =
				new ArrayList<RefiningOutcome>();

		for ( RefiningOutcome ore : this.refiningOutcomes.values() ) {
			if ( ore.yields( mineral ) ) {
				result.add( ore );
			}
		}

		return result;
	}

	/**
	 * Returns the minerals that are refined from
	 * some ore (assuming perfect skills , standing and refining yield).
	 * 
	 * @param ore the ore that should be refined
	 * @return
	 */
	public List<? extends ItemWithQuantity> getRefiningOutcome(InventoryType ore) {

		if ( ! ore.isOre() ) {
			throw new IllegalArgumentException("Type "+ore+" is no ore");
		}

		return getRefiningOutcome( ore.getName() );
	}
}
