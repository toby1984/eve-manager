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
package de.codesourcery.eve.skills.production.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.accountdata.IUserAccountChangeListener;
import de.codesourcery.eve.skills.accountdata.IUserAccountStore;
import de.codesourcery.eve.skills.accountdata.UserAccountChangeListenerAdapter;
import de.codesourcery.eve.skills.assets.IAssetManager;
import de.codesourcery.eve.skills.assets.IAssetManager.IAssetChangeListener;
import de.codesourcery.eve.skills.datamodel.Asset;
import de.codesourcery.eve.skills.datamodel.AssetList;
import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.production.BlueprintWithAttributes;
import de.codesourcery.eve.skills.production.IBlueprintLibrary;
import de.codesourcery.eve.skills.util.Misc;

public class InMemoryBlueprintLibrary implements IBlueprintLibrary
{

	private static final Logger log = Logger
	.getLogger(InMemoryBlueprintLibrary.class);

	// guarded-by: changeListener
	private final List<IBlueprintLibraryChangeListener> changeListener =
		new ArrayList<IBlueprintLibraryChangeListener>();

	private final IUserAccountStore userAccountStore;
	private final IStaticDataModel dataModel;
	private final IAssetManager assetManager;

	private final Object CACHE_LOCK = new Object();
	// guarded-by: CACHE_LOCK
	private CacheImpl data;

	private final IUserAccountChangeListener userAccountListener =
		new UserAccountChangeListenerAdapter() {

		@Override
		public void characterRemoved(UserAccount account, ICharacter c)
		{
			getCache().characterRemoved( c );
		}

		@Override
		public void userAccountRemoved(UserAccount account)
		{
			for ( ICharacter c : account.getCharacters() ) {
				getCache().characterRemoved( c );
			}

		}
	};

	private final IAssetChangeListener assetChangeListener =
		new IAssetChangeListener() {

		@Override
		public void assetsChanged(ICharacter character)
		{
			InMemoryBlueprintLibrary.this.assetsChanged( character );
		}

	};

	protected final IStaticDataModel getDataModel() {
		return this.dataModel;
	}

	protected interface IDoWithExclusiveAccess 
	{
		public void doWhileLocked( CacheImpl cache);
	}

	protected class CacheImpl 
	{
		private final Object CACHE_LOCK = new Object();

		// guarded-by: CACHE_LOCK
		private Map<Long,Map<CharacterID,BlueprintWithAttributesImpl> > blueprintsByType =
			new HashMap<Long,Map<CharacterID,BlueprintWithAttributesImpl> > ();

		// guarded-by: CACHE_LOCK
		private Map<CharacterID,Map<Long,BlueprintWithAttributesImpl> > blueprintsByCharacter =
			new HashMap<CharacterID,Map<Long,BlueprintWithAttributesImpl> > ();

		public List<BlueprintWithAttributesImpl> getBlueprintsByType(InventoryType blueprintType) 
		{
			final List<BlueprintWithAttributesImpl> result =
				new ArrayList<BlueprintWithAttributesImpl>();

			synchronized( CACHE_LOCK ) {
				final Map<CharacterID, BlueprintWithAttributesImpl> map = 
					blueprintsByType.get( blueprintType.getId() );

				if ( map != null ) {
					result.addAll( map.values() ); 
				}
				return result;
			}
		}

		public void doWithExclusiveAccess(IDoWithExclusiveAccess visitor) {
			synchronized( CACHE_LOCK ) {
				visitor.doWhileLocked( this );
			}
		}

		public void characterRemoved(ICharacter c)
		{
			final Map<Long, BlueprintWithAttributesImpl> removed;
			synchronized( CACHE_LOCK ) {
				removed = 
					blueprintsByCharacter.remove( c.getCharacterId() );

				for ( Map<CharacterID,BlueprintWithAttributesImpl> entry : blueprintsByType.values() ) {
					entry.remove( c.getCharacterId() );
				}
			}

			if ( removed != null && ! removed.isEmpty() ) {
				notifyListeners( new LinkedList<BlueprintWithAttributesImpl>(),
						new LinkedList<BlueprintWithAttributesImpl>(),
						new ArrayList<BlueprintWithAttributesImpl>( removed.values() )
				);
			}
		}

		public List<BlueprintWithAttributesImpl> getBlueprintsByCharacter(CharacterID owningCharacterId) 
		{
			if ( owningCharacterId == null ) {
				throw new IllegalArgumentException("owning character id cannot be NULL");
			}

			final List<BlueprintWithAttributesImpl> result =
				new ArrayList<BlueprintWithAttributesImpl>();

			synchronized( CACHE_LOCK ) {
				final Map<Long, BlueprintWithAttributesImpl> map = 
					blueprintsByCharacter.get( owningCharacterId );

				if ( map != null ) {
					result.addAll( map.values() ); 
				}
				return result;
			}
		}

		public List<BlueprintWithAttributesImpl> getAllBlueprints() 
		{
			final List<BlueprintWithAttributesImpl> result =
				new ArrayList<BlueprintWithAttributesImpl>();

			synchronized( CACHE_LOCK ) {
				for( Map<CharacterID,BlueprintWithAttributesImpl> map : blueprintsByType.values() ) {
					result.addAll( map.values() );
				}
				return result;
			}
		}

		public boolean contains(InventoryType blueprintType) {
			if ( blueprintType == null ) {
				throw new IllegalArgumentException(
				"blueprintType cannot be NULL");
			}
			synchronized ( CACHE_LOCK ) {
				return blueprintsByType.containsKey( blueprintType.getId() );
			}
		}

		public boolean contains(InventoryType blueprintType,CharacterID owningCharacter) 
		{
			if ( blueprintType==null ) {
				throw new IllegalArgumentException("blueprint type cannot be NULL");
			}

			if ( owningCharacter == null ) {
				throw new IllegalArgumentException("owningCharacter cannot be NULL");
			}

			synchronized( CACHE_LOCK ) { 
				final Map<Long, BlueprintWithAttributesImpl> map = 
					blueprintsByCharacter.get( owningCharacter );

				if ( map != null ) {
					return map.containsKey( blueprintType.getId() );
				}
			}
			return false;
		}

		public boolean removeBlueprint(BlueprintWithAttributesImpl blueprint) {

			if ( blueprint == null ) {
				throw new IllegalArgumentException("blueprint cannot be NULL");
			}

			boolean removed = false;

			synchronized( CACHE_LOCK ) {
				final Map<Long, BlueprintWithAttributesImpl> map1 = 
					blueprintsByCharacter.get( blueprint.getOwningCharacterId() );


				if ( map1 != null ) {
					removed |= ( map1.remove( blueprint.getBlueprintTypeId() ) != null );
				} else {
					log.warn("removeBlueprint(): Failed to remove blueprint "+blueprint+" from by-character map");
				}

				final Map<CharacterID, BlueprintWithAttributesImpl> map2 = 
					blueprintsByType.get( blueprint.getBlueprintTypeId() );

				if ( map2 != null ) {
					removed |= ( map2.remove( blueprint.getOwningCharacterId() ) != null );
				} else {
					log.warn("removeBlueprint(): Failed to remove blueprint "+blueprint+" from by-type map");
				}
			}

			if ( removed ) {
				notifyListeners( 
						new LinkedList<BlueprintWithAttributesImpl>(),
						new LinkedList<BlueprintWithAttributesImpl>(),
						Collections.singletonList( blueprint )
				);
			}
			return removed;
		}
		
		public void addBlueprint( BlueprintWithAttributesImpl blueprint ) {
			internalAddBlueprint( blueprint , blueprint.getBlueprint().getType().getBlueprintType() );
		}

		private void internalAddBlueprint( BlueprintWithAttributesImpl blueprint , InventoryType blueprintType) {

			if ( blueprint == null ) {
				throw new IllegalArgumentException("blueprint cannot be NULL");
			}

			if ( contains( blueprintType ) ) {
				throw new IllegalArgumentException("Cannot add "+blueprint+" multiple times");
			}

			// add to 'by type' map
			synchronized( CACHE_LOCK ) {
				Map<CharacterID, BlueprintWithAttributesImpl> map1 = 
					blueprintsByType.get( blueprint.getBlueprintTypeId() );

				if ( map1 == null ) {
					map1 = new HashMap<CharacterID, BlueprintWithAttributesImpl>();
					blueprintsByType.put( blueprint.getBlueprintTypeId() , map1 );
				}
				map1.put( blueprint.getOwningCharacterId() , blueprint );

				// add to 'by character' map

				Map<Long, BlueprintWithAttributesImpl> map2 = 
					blueprintsByCharacter.get( blueprint.getOwningCharacterId() );
				if ( map2 == null ) {
					map2 = new HashMap<Long, BlueprintWithAttributesImpl>();
					blueprintsByCharacter.put( blueprint.getOwningCharacterId() , map2 );
				}
				map2.put( blueprintType.getTypeId() , blueprint );
			}
		}

		public void updateOwningCharacter(BlueprintWithAttributesImpl blueprint,
				CharacterID characterId)
		{

			boolean removalSuccessful = false;
			synchronized ( CACHE_LOCK) 
			{
				removalSuccessful = this.removeBlueprint( blueprint );
				if ( removalSuccessful )
				{
					blueprint.setOwningCharacterId( characterId );
					addBlueprint( blueprint );
				}
			}

			if ( removalSuccessful ) {
				notifyListeners( 
						new LinkedList<BlueprintWithAttributesImpl>(),
						Collections.singletonList( blueprint ),
						new LinkedList<BlueprintWithAttributesImpl>()

				);
			}
		}

		public void assetsChanged(ICharacter character)
		{

			log.info("assetsChanged(): Syncing blueprints with assets of "+character.getName() );

			final List<BlueprintWithAttributesImpl> newlyAdded =
				new ArrayList<BlueprintWithAttributesImpl> ();

			final List<BlueprintWithAttributesImpl> updatedBlueprints =
				new ArrayList<BlueprintWithAttributesImpl>();

			final AssetList allAssets = assetManager.getAssets( character );
			log.info("assetsChanged(): "+character.getName()+" has "+allAssets.size()+" assets.");
			
			final AssetList mergedAssets = 
				allAssets.createMergedAssetListByType(true,true);
			
			// holds all blueprints we've found among the assets
			final Map<Long,Asset> blueprintsFromAssets =
				new HashMap<Long, Asset>();

			// holds all blueprints found among the assets
			// that aren't known to us yet
			final Map<Long,Asset> newBlueprints =
				new HashMap<Long, Asset>();
			
			synchronized( CACHE_LOCK ) 
				{
				// set 'foundInAssets' flag for all blueprints we already know about
				for ( Asset a : mergedAssets ) 
				{
					if ( ! a.getType().isBlueprint() ) {
						continue;
					}

					blueprintsFromAssets.put( a.getType().getId() , a );

					final Map<CharacterID, BlueprintWithAttributesImpl> knownBlueprints = 
						blueprintsByType.get( a.getType().getId() );

					if ( knownBlueprints == null ) {
						newBlueprints.put( a.getType().getId() , a );
						continue;
					}

					final BlueprintWithAttributesImpl knownBlueprint= 
						knownBlueprints.get( character.getCharacterId() );

					if ( knownBlueprint == null ) {
						if ( log.isDebugEnabled() ) {
							log.debug("assetsChanged(): [ "+character.getName()+" ] new blueprint: "+a.getType().getName() );
						}
						newBlueprints.put( a.getType().getId() , a );
						continue;
					}

					if ( ! knownBlueprint.isFoundInAssets() ) {
						if ( log.isDebugEnabled() ) {
							log.debug("assetsChanged(): [ "+character.getName()+" ] "+
									knownBlueprint.getBlueprint().getType().getBlueprintType().getName()
									+" changes 'isFoundInAssets' false -> true");
						}
						knownBlueprint.setFoundInAssets( true );
						updatedBlueprints.add( knownBlueprint );
					}
				}

				// clears 'foundInAssets' flag for all blueprints we know about
				// that haven't been in the asset list
				final Map<Long, BlueprintWithAttributesImpl> map = 
					blueprintsByCharacter.get( character.getCharacterId() );

				if ( map != null ) {
					for ( BlueprintWithAttributesImpl knownBlueprint : map.values() ) {
						if ( knownBlueprint.isFoundInAssets() && 
								! blueprintsFromAssets.containsKey( knownBlueprint.getBlueprintTypeId() ) ) 
						{
							knownBlueprint.setFoundInAssets( false );
							updatedBlueprints.add( knownBlueprint );
							if ( log.isDebugEnabled() ) {
								log.debug("assetsChanged(): [ "+character.getName()+" ] "+
										knownBlueprint.getBlueprint().getType().getBlueprintType().getName()
										+" changes 'isFoundInAssets' true -> false");
							}
						}
					}
				}

				// add new blueprints we didn't know about before
				for ( Asset newBlueprint : newBlueprints.values() ) {

					final BlueprintWithAttributesImpl bp =
						new BlueprintWithAttributesImpl( character.getCharacterId(),
								newBlueprint.getType().getId(),
								0,
								0,
								true);

					/*
					 * Do NOT call addBlueprint() from here ,
					 * addBlueprint() calls BlueprintWithAttributesImpl#getBlueprint()
					 * which tries to lazily fetch the associated InventoryType
					 * (which we already know about anyway) 
					 * and this code might NOT be currently running 
					 * on the EDT (and this is a no-go since the
					 * Hibernate Session needs to be confined to the EDT).
					 */
					internalAddBlueprint( bp , newBlueprint.getType() );
					newlyAdded.add( bp );
				}
			} // end of synchronized block

			// notify listeners
			notifyListeners( newlyAdded , updatedBlueprints , new ArrayList<BlueprintWithAttributesImpl>() );
		}

		public BlueprintWithAttributesImpl getBlueprint(CharacterID owningCharacterId,
				Blueprint blueprint)
		{

			synchronized ( CACHE_LOCK ) {
				Map<Long, BlueprintWithAttributesImpl> byType = blueprintsByCharacter.get( owningCharacterId );
				if ( byType != null ) {
					return byType.get( blueprint.getType().getBlueprintType().getTypeId() );
				}
			}
			return null;
		}

	}

	final class BlueprintWithAttributesImpl extends BlueprintWithAttributes {

		private final Object LOCK = new Object();

		private Blueprint blueprint;

		public BlueprintWithAttributesImpl(CharacterID owningCharacterId,
				Long blueprintTypeId, boolean foundInAssets) {
			super(owningCharacterId, blueprintTypeId, foundInAssets);
		}
		
		public BlueprintWithAttributesImpl(CharacterID owningCharacterId,
				Long blueprintTypeId, int meLevel, int peLevel,
				boolean foundInAssets) {
			super(owningCharacterId, blueprintTypeId, meLevel, peLevel, foundInAssets);
		}

		public void setOwningCharacterId(CharacterID owningCharacterId)
		{
			if ( owningCharacterId == null ) {
				throw new IllegalArgumentException(
				"owningCharacterId cannot be NULL");
			}

			this.owningCharacterId = owningCharacterId;
		}

		@Override
		public Blueprint getBlueprint()
		{
			synchronized( LOCK ) {
				if ( blueprint == null ) {
					blueprint = dataModel.getBlueprint( dataModel.getInventoryType( super.getBlueprintTypeId() ) );
				}
			}
			return blueprint;
		}

		public void setMeLevel(int meLevel)
		{
			this.meLevel = meLevel;
		}

		void setPeLevel(int peLevel)
		{
			this.peLevel = peLevel;
		}

		public void setFoundInAssets(boolean foundInAssets)
		{
			this.foundInAssets = foundInAssets;
		}
	}

	public InMemoryBlueprintLibrary(IStaticDataModel dataModel,IAssetManager assetManager, IUserAccountStore userAccountStore) 
	{
		if ( dataModel == null ) {
			throw new IllegalArgumentException("dataModel cannot be NULL");
		}
		if ( assetManager == null ) {
			throw new IllegalArgumentException("assetManager cannot be NULL");
		}
		if ( userAccountStore == null ) {
			throw new IllegalArgumentException("userAccountStore cannot be NULL");
		}
		this.userAccountStore = userAccountStore;
		this.dataModel = dataModel;
		this.assetManager = assetManager;

		this.userAccountStore.addChangeLister( this.userAccountListener );
		this.assetManager.addAssetChangeListener( this.assetChangeListener );
	}

	protected void assetsChanged(ICharacter character)
	{
		log.info("assetsChanged(): character "+character.getCharacterId());

		getCache().assetsChanged( character );
	}

	protected List<Asset> findBlueprint(ICharacter character, Blueprint blueprint) {

		List<Asset> results =
			new ArrayList<Asset> ();

		for ( Asset asset : assetManager.getAssets( character ) ) {
			if ( asset.getType().equals( blueprint.getType().getBlueprintType() ) ) {
				results.add( asset );
			}
		}

		return results;
	}

	protected CacheImpl createCache() {
		return new CacheImpl();
	}

	protected final CacheImpl getCache() {
		synchronized( CACHE_LOCK ) {
			if ( data == null ) {
				data = createCache();
			}
			return data;
		}
	}

	@Override
	public void addBlueprint(ICharacter owningCharacter, Blueprint blueprint,
			int meLevel, int peLevel)
	{
		final boolean foundInAssets = ! findBlueprint(owningCharacter, blueprint).isEmpty();

		final BlueprintWithAttributesImpl impl =
			new BlueprintWithAttributesImpl
			( 
					owningCharacter.getCharacterId() , 
					blueprint.getType().getBlueprintType().getId(),
					meLevel , peLevel , foundInAssets 
			);

		getCache().addBlueprint( impl ); 
	}

	@Override
	public boolean containsBlueprint(InventoryType blueprintType)
	{
		if ( blueprintType == null ) {
			throw new IllegalArgumentException("blueprint type cannot be NULL");
		}
		return getCache().contains( blueprintType );
	}

	@Override
	public boolean containsBlueprint(Blueprint blueprint)
	{
		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}
		return containsBlueprint( blueprint.getType().getBlueprintType() );
	}

	@Override
	public List<? extends BlueprintWithAttributes> getBlueprints()
	{
		return getCache().getAllBlueprints();
	}

	@Override
	public List<? extends BlueprintWithAttributes> getBlueprints(
			ICharacter owningCharacter)
			{
		if ( owningCharacter == null ) {
			throw new IllegalArgumentException("owning character cannot be NULL");
		}
		return getCache().getBlueprintsByCharacter( owningCharacter.getCharacterId() );
			}

	@Override
	public List<? extends BlueprintWithAttributes> getBlueprints(
			ICharacter owningCharacter, Blueprint... blueprints)
			{
		if ( owningCharacter == null ) {
			throw new IllegalArgumentException("owning character cannot be NULL");
		}

		if ( blueprints == null ) {
			throw new IllegalArgumentException("blueprints cannot be NULL");
		}

		final List<? extends BlueprintWithAttributes> result =
			getCache().getBlueprintsByCharacter( owningCharacter.getCharacterId() );

		outer:		
			for (Iterator<? extends BlueprintWithAttributes> it = result.iterator(); it.hasNext();) 
			{
				final BlueprintWithAttributes blueprintWithAttributes = 
					it.next();

				for ( Blueprint bp : blueprints ) {
					if ( blueprintWithAttributes.getBlueprintTypeId().equals( bp.getType().getBlueprintType().getId() ) ) {
						continue outer;
					}
				}
				it.remove();
			}
		return result;
			}

	@Override
	public List<? extends BlueprintWithAttributes> getBlueprints(Blueprint blueprint)
	{
		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}
		return getCache().getBlueprintsByType( blueprint.getType().getBlueprintType() );
	}

	@Override
	public List<? extends BlueprintWithAttributes> getBlueprints( InventoryType blueprintType)
	{
		if ( blueprintType == null ) {
			throw new IllegalArgumentException("blueprintType cannot be NULL");
		}

		return getCache().getBlueprintsByType( blueprintType );
	}

	@Override
	public boolean ownsBlueprint(ICharacter owningCharacter, Blueprint blueprint)
	{
		if ( owningCharacter  == null ) {
			throw new IllegalArgumentException("owningCharacter, blueprint cannot be NULL");
		}

		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}

		final boolean result =
			getCache().contains( blueprint.getType().getBlueprintType() , owningCharacter.getCharacterId() );
		
		if ( log.isDebugEnabled() ) {
			log.debug("ownsBlueprint(): character "+owningCharacter.getCharacterId()+" owns "+
					blueprint.getType().getBlueprintType().getName()+" => "+result);
		}
		return result;
	}

	@Override
	public boolean ownsBlueprint(ICharacter owningCharacter,
			InventoryType blueprintType)
	{
		if ( owningCharacter == null ) {
			throw new IllegalArgumentException("owningCharacter cannot be NULL");
		}

		if ( blueprintType == null ) {
			throw new IllegalArgumentException("blueprintType cannot be NULL");
		}

		return getCache().contains( blueprintType , owningCharacter.getCharacterId() );
	}

	@Override
	public void removeBlueprint(BlueprintWithAttributes blueprint)
	{
		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}
		getCache().removeBlueprint( (BlueprintWithAttributesImpl) blueprint );
	}

	@Override
	public void update(BlueprintWithAttributes blueprint, int meLevel,
			int peLevel)
	{
		final BlueprintWithAttributesImpl existing = 
			getCache().getBlueprint( blueprint.getOwningCharacterId() , blueprint.getBlueprint() );

		if ( existing != null ) 
		{
			existing.setMeLevel( meLevel );
			existing.setPeLevel( peLevel );

			if ( blueprint instanceof BlueprintWithAttributesImpl) {
				((BlueprintWithAttributesImpl) blueprint).setMeLevel( meLevel );
				((BlueprintWithAttributesImpl) blueprint).setPeLevel( peLevel );
			}

			notifyListeners( 
					new LinkedList<BlueprintWithAttributesImpl>(),
					new LinkedList<BlueprintWithAttributesImpl>(),
					Collections.singletonList( existing )
			);

		} else {
			log.error("update(): Couldn't find blueprint "+blueprint+" in library ?");
			throw new NoSuchElementException("Couldn't find blueprint "+blueprint+" in library ?");
		}
	}

	@Override
	public void updateOwningCharacter(BlueprintWithAttributes blueprint,
			ICharacter newOwner)
	{
		if ( blueprint == null ) {
			throw new IllegalArgumentException("blueprint cannot be NULL");
		}

		if ( newOwner == null ) {
			throw new IllegalArgumentException("newOwner cannot be NULL");
		}

		getCache().updateOwningCharacter( (BlueprintWithAttributesImpl) blueprint , newOwner.getCharacterId() );
	}

	protected void notifyListeners(final List<BlueprintWithAttributesImpl> newlyAdded,
			final List<BlueprintWithAttributesImpl> updatedBlueprints,
			final List<BlueprintWithAttributesImpl> list) 
	{

		final Runnable runnable = new Runnable() {

			@Override
			public void run()
			{
				final List<IBlueprintLibraryChangeListener> copy;
				synchronized (changeListener) {
					copy = new ArrayList<IBlueprintLibraryChangeListener>( changeListener );
				}

				final List<? extends BlueprintWithAttributes>  wrappedAdded=
					Collections.unmodifiableList( newlyAdded );

				final List<? extends BlueprintWithAttributes>  wrappedChanged=
					Collections.unmodifiableList( updatedBlueprints );

				final List<? extends BlueprintWithAttributes>  wrappedRemoved=
					Collections.unmodifiableList( list );

				for ( IBlueprintLibraryChangeListener l : copy ) {
					try {
						l.blueprintsChanged( wrappedAdded , wrappedChanged, wrappedRemoved );
					} catch(Exception e) {
						log.error("notifyListeners(): Listener "+l+" failed " , e );
					}
				}				
			}};

			// make sure listeners are invoked from the EDT
			Misc.runOnEventThread( runnable );
	}

	@Override
	public void addChangeListener(IBlueprintLibraryChangeListener listener)
	{
		if ( listener == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}

		synchronized ( changeListener ) {
			changeListener.add( listener );
		}
	}

	@Override
	public void removeChangeListener(IBlueprintLibraryChangeListener listener)
	{
		if ( listener == null ) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}

		synchronized ( changeListener ) {
			changeListener.remove( listener );
		}		
	}

}
