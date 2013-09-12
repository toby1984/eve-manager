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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.ItemWithQuantity;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;
import de.codesourcery.eve.skills.datamodel.Requirements;
import de.codesourcery.eve.skills.datamodel.SkillTree;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.BlueprintType;
import de.codesourcery.eve.skills.db.datamodel.InventoryGroup;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.TypeActivityMaterials;
import de.codesourcery.eve.skills.db.datamodel.TypeMaterial;
import de.codesourcery.eve.skills.exceptions.NoTech1VariantException;

public class BlueprintTypeDAO extends HibernateDAO<BlueprintType,Long> implements IBlueprintTypeDAO {

	public static final Logger log = Logger.getLogger(BlueprintTypeDAO.class);

	private ISkillTreeDAO skillTreeDAO;
	private ITypeActivityMaterialsDAO typeActivityMaterialsDAO;

	public BlueprintTypeDAO() {
		super(BlueprintType.class);
	}

	private BlueprintType getBlueprintTypeFor(final InventoryType type) {

		return execute( new HibernateCallback<BlueprintType>() {

			@SuppressWarnings("unchecked")
			@Override
			public BlueprintType doInSession(Session session) {
				final Query query = 
					session.createQuery("from BlueprintType where productType = :type");

				query.setParameter("type" , type );
				return getExactlyOneResult( (List<BlueprintType>) query.list() );
			}} );
	}

	@Override
	public Blueprint getBlueprintByProduct(InventoryType type) {
		return createBlueprint( getBlueprintTypeFor( type ) );
	}

	/*

	 */

	public List<ItemWithQuantity> getRefiningOutcome(InventoryType item) {

		if ( item == null ) {
			throw new IllegalArgumentException("item cannot be NULL");
		}

		log.debug("getRefiningOutcome(): item="+item);

		List<ItemWithQuantity>  result = fetchRefiningMaterials(item );
		log.debug("getRefiningOutcome(): Item "+item+" refines into "+result.size()+" materials.");
		return result;
	}

	protected List<TypeActivityMaterials> fetchRequirements(final Activity activity , final BlueprintType blueprint) 
	{
		if ( activity == null ) {
			throw new IllegalArgumentException("activity cannot be NULL");
		}

		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}

		if ( activity == Activity.REFINING ) 
		{
			throw new RuntimeException("Unsupported activity "+activity+" - use fetchRefiningMaterials() instead");
		}

		final InventoryType item = blueprint.getBlueprintType();		

		/*
		 * note: TypeActivityMaterials belongs to a database view that has been removed
		 * from the EVE DB dump and is now split across two tables , 
		 * ramTypeRequirements and invTypeMaterials.
		 * 
		 * invTypeMaterials - holds references to all materials that are not
		 * subject to manufacturing waste. If something has no entry here, 
		 * it cannot be produced / reprocessed. THIS TABLE USES THE PRODUCT TYPE ID (NOT THE
		 * BLUEPRINT TYPE ID) AS INDEX.
		 * 
		 * ramTypeRequirements - holds references to all materials that are
		 * subject to manufacturing waste as well as all skill requirements etc.
		 */

		// fetch special requirements first
		final List<TypeActivityMaterials> specialMaterials = execute( new HibernateCallback<List<TypeActivityMaterials>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<TypeActivityMaterials> doInSession(Session session) {
				final Query query = 
					session.createQuery("from TypeActivityMaterials " +
					"where typeID = :type and activityID = :activity");

				query.setParameter("type" , item );
				query.setParameter("activity" , 
						activity ,
						Hibernate.custom( ActivityUserType.class ) );
				return (List<TypeActivityMaterials>) query.list();					
			}
		} );

		/* Add stuff from "simple materials" table
		 * only if we're trying to manufacture a T1 item.
		 * 
		 * Since the "simple materials" table resembles
		 * reprocessing data as well, ignore it when 
		 * manufacturing items with tech level >1 since
		 * they will yield the mats used for production 
		 * of the corresponding T1 items which is WRONG.
		 */

		if ( activity == Activity.MANUFACTURING ) 
		{
			specialMaterials.addAll( getSimpleRequirements( blueprint , specialMaterials) );
		}

		return specialMaterials;
	}

	private List<TypeActivityMaterials> getSimpleRequirements(BlueprintType blueprint,
			List<TypeActivityMaterials> specialMaterials) 
			{
		/*
		 * While Jercy's method looks fine to me for T1 manufacturing, it seems
		 * to be slightly more complicated for T2.
		 * 
		 * For example, looking at the Tritanium requirement to build 1 unit of
		 * Medium Shield Transporter II.
		 * 
		 * invTypeMaterials will give you a figure of 1660 tritanium. However,
		 * the actual job quote asks for just 336 Tritanium.
		 * 
		 * The difference is due to the requirement for the T1 module. Looking
		 * up the T1 version in invTypeMaterials indicates that the T1 module
		 * requires 1355 tritanium.
		 * 
		 * 1660-1355 = 305. Adding the 10% ME wastage on top gets you to the 336
		 * Tritanium requested by the quote.
		 * 
		 * The key to this is the recycle field in ramTypeRequirements. This
		 * field is 1 for the T1 module requirement, but 0 for everything else.
		 * Which gives us the differentiation as to why we have to remove the
		 * Tritanium in the T1 module, but not that in the R.A.M. - Shield Tech.
		 * 
		 * So it looks like we actually need three elements to make up the full
		 * manufacturing requirement:
		 * 
		 * (1) Records from ramTypeRequirements for activityID=1,typeID=blueprintTypeID 		 
		 * (2) Records from invTypeMaterials for typeID=productTypeID 
		 * (3) Records from invTypeMaterials for typeID=requiredTypeID from (2) where recycle=1
		 * 
		 * ( (2)-(3) )*wasteFactor then becomes your Raw Materials 
		 * (1) becomes your Extra Materials and Skills (differentiated by the categoryID of the requiredTypeID)
		 */

		final Map<InventoryType , TypeActivityMaterials> allMaterials =
			new HashMap<InventoryType , TypeActivityMaterials>();

		final List<ItemWithQuantity> simpleMaterials = 
			fetchRefiningMaterials( blueprint.getProductType() );

		for ( ItemWithQuantity mat : simpleMaterials ) 
		{
			final TypeActivityMaterials simpleMaterial = toTypeActivityMaterial(blueprint,mat);

			// all materials from this table
			// are subject to manufacturing waste
			simpleMaterial.setSubjectToManufacturingWaste( true );

			allMaterials.put( simpleMaterial.getRequiredType() , simpleMaterial );
		}

		// since the invTypeMaterials always holds the
		// total production/reprocessing amount for a given item, we 
		// need to subtract the materials required for
		// producing any recycleable 'special' materials of the item

		for ( TypeActivityMaterials specialMaterial : specialMaterials ) 
		{
			if ( ! specialMaterial.isRecycle() ) {
				continue;
			}

			final List<ItemWithQuantity>  t1Parts = 
				fetchRefiningMaterials( specialMaterial.getRequiredType() );

			for ( ItemWithQuantity mat : t1Parts ) 
			{
				final TypeActivityMaterials existing =
					allMaterials.get( mat.getType() );

				if ( existing != null )
				{
					existing.setQuantity( existing.getQuantity() - mat.getQuantity() );
				}
			}
		}

		// remove everything with a negative quantity here
		final Iterator<Map.Entry<InventoryType , TypeActivityMaterials>> it =
			allMaterials.entrySet().iterator();

		while( it.hasNext() ) {
			final TypeActivityMaterials mat = it.next().getValue();
			if ( mat.getQuantity() <= 0 ) {
				it.remove();
			}
		}

		return new ArrayList<TypeActivityMaterials>( allMaterials.values() );
	}

	private TypeActivityMaterials toTypeActivityMaterial(BlueprintType blueprint,
			ItemWithQuantity mat) 
	{
		final TypeActivityMaterials simpleMaterial =
			new TypeActivityMaterials();

		simpleMaterial.setActivity( Activity.MANUFACTURING  );
		simpleMaterial.setQuantity( mat.getQuantity() );
		simpleMaterial.setType( blueprint.getProductType() );
		simpleMaterial.setRequiredType( mat.getType() );	
		return simpleMaterial;
	}

	private List<ItemWithQuantity> fetchRefiningMaterials(final InventoryType item) 
	{
		final List<TypeMaterial> materials = 
			execute( new HibernateCallback<List<TypeMaterial>>() 
					{

				@SuppressWarnings("unchecked")
				@Override
				public List<TypeMaterial> doInSession(Session session) 
				{
					final Query query = 
						session.createQuery("from TypeMaterial " +
						"where typeID = :type");

					query.setParameter("type" , item );
					return (List<TypeMaterial>) query.list();					
				}
					});

		final List<ItemWithQuantity> result = new ArrayList<ItemWithQuantity>();
		for ( TypeMaterial m : materials ) {
			result.add( new ItemWithQuantity( m.getType() , m.getQuantity() ) );
		}
		return result;
	}

	protected Requirements createRequirements(Activity activity,BlueprintType blueprint) 
	{
		final List<TypeActivityMaterials> data=
			fetchRequirements( activity  , blueprint ); 

		final Requirements requirements =
			new Requirements( activity );

		for ( TypeActivityMaterials requirement : data ) {

			if ( requirement.getRequiredType().isSkill() ) {
				final Prerequisite r =
					new Prerequisite();
				final int lvl =
					requirement.getQuantity() ;
				if ( lvl <= 0 ) {
					throw new RuntimeException("Requirement "+requirement+" with skill lvl <= 0 ?");
				}
				r.setRequiredLevel( lvl);
				r.setSkill( getSkillTree().getSkill( requirement.getRequiredType().getId().intValue() ) );
				requirements.addRequiredSkill( r );

			} else {

				final RequiredMaterial mat = new RequiredMaterial( requirement.getRequiredType() , requirement.getQuantity() );

				if ( activity == Activity.MANUFACTURING ) 
				{
					// ignore bogus data in dump
					if ( requirement.getQuantity() <= 0 ) {
						continue;
					}
					if ( requirement.isSubjectToManufacturingWaste() || 
							isSubjectToManufacturingWaste(blueprint , requirement) ) 
					{
						mat.setSubjectToManufacturingWaste( true );
					}
				}

				mat.setDamagePerJob( requirement.getDamagePerJob() );
				mat.setSupportsRecycling( requirement.isRecycle() );
				requirements.addRequiredMaterial( mat );
			}
		}
		return requirements;
	}

	/*
	QUESTION:

	So where exactly in the database dump DO you find 
	the attribute that says wether a material is "raw" or "extra" 
	for each specific blueprint (or item) ?

	ANSWER:

	An attribute does not exist but you can 
	determine whether a material is raw or extra by 
	examining table typeActivityMaterials in the data dump.

	The data we need to look at is 
	activityID 1 for the blueprintTypeID and 
	activityID 6 for the productTypeID that the blueprint in question produces.

	a.) A material is raw if the activity 6 qty is greater than or equal to the activty 1 qty.

	b.) A material is extra if activity 6 does not exist for the activity 1 typeID.

	Ignore any activity 6 typeIDs that don't have an activity 1 counterpart.				 
	 */	
	protected boolean isSubjectToManufacturingWaste(final BlueprintType blueprint , 
			final TypeActivityMaterials requirement) 
	{
		// select quantity for activity 6 of the product 
		// the BP produces and of the material in question

		final List<TypeActivityMaterials> typeActivityForProduct =
			execute( new HibernateCallback<List<TypeActivityMaterials>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<TypeActivityMaterials> doInSession(Session session) {
					final Query query = 
						session
						.createQuery("from TypeActivityMaterials " +
								"where typeID = :type and activityID = :activity and"+
						" requiredTypeID = :requiredType");

					query.setParameter("type" , blueprint.getProductType() );
					query.setParameter("requiredType" , requirement.getRequiredType() );
					query.setParameter("activity" , 
							Activity.REFINING ,
							Hibernate.custom( ActivityUserType.class ) );
					return (List<TypeActivityMaterials>) query.list();					
				}} );		

		if ( typeActivityForProduct.isEmpty() ) {
			return false;
		}

		if ( typeActivityForProduct.size() != 1 ) {
			throw new RuntimeException("Internal error for "+
					requirement.getType()+
					", expected exactly one result but got "+typeActivityForProduct);
		}
		final TypeActivityMaterials mat = typeActivityForProduct.get(0);

		if ( mat.getQuantity() >= requirement.getQuantity() ) {
			return true;
		}
		return false;
	}

	protected Map<Activity,Requirements> fetchRequirements(final BlueprintType blueprint) {

		final Map<Activity,Requirements> result =
			new HashMap<Activity, Requirements>();

		result.put( Activity.MANUFACTURING , createRequirements( Activity.MANUFACTURING , blueprint ) );

		if ( blueprint.getTechLevel() == 1 ) { // only Tech1 BPs can be used for invention
			result.put( Activity.INVENTION, createRequirements( Activity.INVENTION, blueprint ) );
		}

		return result;
	}

	@Override
	public List<Blueprint> getBlueprintsByProductName(final String name) {
		final List<BlueprintType> types = execute( new HibernateCallback<List<BlueprintType>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<BlueprintType> doInSession(Session session) {
				final Query query = 
					session.createQuery("select b from " +
							"BlueprintType b inner join InventoryType t " +
					"on b.productType = t where t.typeName like :name");

				query.setParameter("name" , name );
				return  (List<BlueprintType>) query.list();
			}} );

		final List<Blueprint> result =
			new ArrayList<Blueprint>();

		for(BlueprintType print : types ) {
			result.add( createBlueprint( print ) );
		}

		return result;
	}

	@Override
	public Blueprint getBlueprint(final InventoryType blueprint) {
		final BlueprintType type= execute( new HibernateCallback<BlueprintType>() {

			@SuppressWarnings("unchecked")
			@Override
			public BlueprintType doInSession(Session session) {
				final Query query = 
					session.createQuery("from BlueprintType where blueprintTypeID = :type");

				query.setParameter( "type" , blueprint.getId() );
				try {
					return getExactlyOneResult( (List<BlueprintType>) query.list() );
				} catch(EmptyResultDataAccessException e) {
					log.error("failed to locate blueprint "+blueprint);
					throw e;
				}
			}} );

		return createBlueprint( type );
	}

	@Override
	public List<Blueprint> getBlueprintsByProductGroup(final InventoryGroup group) {

		return execute( new HibernateCallback<List<Blueprint>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<Blueprint> doInSession(Session session) {
				final Query query = 
					session.createQuery(" select b from " +
							"BlueprintType b , InventoryType t " +
					" where b.productType = t.typeId and t.groupId = :group");

				query.setParameter("group" , group );

				final List<BlueprintType> types =
					(List<BlueprintType>) query.list();

				final List<Blueprint> result =
					new ArrayList<Blueprint>();

				for ( BlueprintType type : types ) {
					result.add( createBlueprint( type ) );
				}

				return result;
			}} );
	}

	protected Blueprint createBlueprint(final BlueprintType type) {
		return new Blueprint( type ) {

			@Override
			protected Map<Activity, Requirements> fetchRequirements() {
				return BlueprintTypeDAO.this.fetchRequirements( type );
			}
		};
	}

	protected SkillTree getSkillTree() {
		return skillTreeDAO.getSkillTree();
	}

	public void setSkillTreeDAO(ISkillTreeDAO skillTreeDAO) {
		this.skillTreeDAO = skillTreeDAO;
	}

	@Override
	public Blueprint getBlueprintByName(final String name)
	{
		return execute( new HibernateCallback<Blueprint>() {

			@SuppressWarnings("unchecked")
			@Override
			public Blueprint doInSession(Session session) {
				final Query query = 
					session.createQuery(" select b from " +
							"BlueprintType b , InventoryType t " +
					" where b.blueprintType = t.typeId and t.name = :blueprintName");

				query.setParameter("blueprintName" , name );

				final List<BlueprintType> types =
					(List<BlueprintType>) query.list();

				if ( types.size() == 1 ) {
					return createBlueprint( types.get(0) );
				}

				throw new IncorrectResultSizeDataAccessException("Expected one blueprint with name '"+name+"'",
						1 , types.size() );
			}} );
	}

	@Override
	public List<Blueprint> getTech2Variations(final Blueprint blueprint) {

		if ( blueprint.getTechLevel() != 1 ) {
			throw new IllegalArgumentException("This method requires a Tech1 blueprint");
		}
		/*
		 * [...] you take the invBlueprintTypes.productTypeID field for the 
		 * tech 1 blueprint and look it up in invMetaTypes.parentTypeID 
		 * with invMetaTypes.metaGroup='2' 
		 * that will give you the tech 2 items that can be invented.		 
		 */

		return execute( new HibernateCallback<List<Blueprint>>() {

			@SuppressWarnings("unchecked")
			@Override
			public List<Blueprint> doInSession(Session session) {
				final Query query = 
					session.createQuery(" select b from " +
							"BlueprintType b , InventoryMetaType g " +
							" where g.metaGroupId = 2 and g.parentType = :product "+
					" and b.productType = g.id" );

				query.setParameter("product" , blueprint.getType().getProductType() );

				final List<BlueprintType> types =
					(List<BlueprintType>) query.list();

				final List<Blueprint> result =
					new ArrayList<Blueprint>();

				for ( BlueprintType type : types ) {
					result.add( createBlueprint( type ) );
				}

				return result;
			}} );		
	}

	@Override
	public Blueprint getTech1Variation(final Blueprint tech2Blueprint) throws NoTech1VariantException {
		return getTech1Variation(tech2Blueprint.getType() );
	}

	protected Blueprint getTech1Variation(final BlueprintType tech2Blueprint) throws NoTech1VariantException 
	{

		if ( tech2Blueprint.getTechLevel() != 2 ) {
			throw new IllegalArgumentException("This method requires a Tech2 blueprint");
		}
		/*
		 * [...] you take the invBlueprintTypes.productTypeID field for the 
		 * tech 1 blueprint and look it up in invMetaTypes.parentTypeID 
		 * with invMetaTypes.metaGroup='2' 
		 * that will give you the tech 2 items that can be invented.		 
		 */

		return execute( new HibernateCallback<Blueprint>() {

			@SuppressWarnings("unchecked")
			@Override
			public Blueprint doInSession(Session session) {

				final Query query = 
					session.createSQLQuery( "select b.* from " +
							"invBlueprintTypes b , invMetaTypes g " +
							" where g.typeID = "+tech2Blueprint.getProductType().getId()+
					" and b.productTypeID = g.parentTypeID" ).addEntity(BlueprintType.class);

				final List<BlueprintType> types =
					(List<BlueprintType>) query.list();

				if ( types.isEmpty() ) {
					throw new NoTech1VariantException( tech2Blueprint );
				} else if ( types.size() > 1 ) {
					throw new IncorrectResultSizeDataAccessException(
							"Unable to find Tech1 variant of "+tech2Blueprint.getBlueprintType() , 1 ,types.size() );
				}
				return createBlueprint( types.get(0) );
			}} );		
	}

}
