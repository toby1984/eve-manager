package de.codesourcery.eve.skills.db.dao;

import de.codesourcery.eve.skills.db.datamodel.ItemAttributeType;

public class ItemAttributeTypeDAO extends HibernateDAO<ItemAttributeType,Long> implements IItemAttributeTypeDAO {

	public ItemAttributeTypeDAO() {
		super(ItemAttributeType.class);
	}
}
