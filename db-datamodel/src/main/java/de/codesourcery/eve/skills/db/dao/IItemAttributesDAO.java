package de.codesourcery.eve.skills.db.dao;

import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.ItemWithAttributes;

public interface IItemAttributesDAO {

	public ItemWithAttributes getAttributes(InventoryType type);
}
