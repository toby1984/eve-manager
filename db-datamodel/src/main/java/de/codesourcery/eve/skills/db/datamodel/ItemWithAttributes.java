package de.codesourcery.eve.skills.db.datamodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.Prerequisite;

public class ItemWithAttributes {

	private static final Logger LOG = Logger.getLogger(ItemWithAttributes.class);
	
	private final InventoryType type;
	private final Map<ItemAttributeType,ItemAttribute> attributes = new HashMap<>();
	
	private final ItemAttributes attributeWrapper = new ItemAttributes() {

		@Override
		public Map<AttributeCategory, List<ItemAttribute>> getAttributesByCategory() {
			return ItemWithAttributes.this.getAttributesByCategory();
		}

		@Override
		public Map<ItemAttributeType, ItemAttribute> getAttributes() {
			return ItemWithAttributes.this.getAttributesMap();
		}

		@Override
		public boolean isRequiredSkillAttribute(ItemAttribute attr) 
		{
			return attr.getType().getAttributeName().startsWith("requiredSkill");
		}
	};
	
	public interface ItemAttributes 
	{
		public Map<AttributeCategory,List<ItemAttribute>> getAttributesByCategory();
		
		public Map<ItemAttributeType, ItemAttribute> getAttributes();
		
		public boolean isRequiredSkillAttribute(ItemAttribute attr);
	}
	
	public ItemWithAttributes(InventoryType type, List<ItemAttribute> attrs) {
		if (type == null) {
			throw new IllegalArgumentException(
					"type must not be NULL");
		}
		this.type = type;
		for ( ItemAttribute a : attrs ) {
			this.attributes.put( a.getType() , a );
		}
	}
	
	private Map<AttributeCategory,List<ItemAttribute>> getAttributesByCategory() {
		final Map<AttributeCategory,List<ItemAttribute>> result = new HashMap<>();
		
		for ( ItemAttribute a : attributes.values() ) {
			List<ItemAttribute> list = result.get(a.getCategory() );
			if ( list == null) {
				list = new ArrayList<>();
				result.put( a.getCategory(),list);
			}
			list.add( a );
		}
		return result;
	}
	
	public InventoryType getType() {
		return type;
	}
	
	public ItemAttributes getAttributes() {
		return attributeWrapper;
	}
	
	private Map<ItemAttributeType, ItemAttribute> getAttributesMap() {
		return new HashMap<>(attributes);
	}
	
	private ItemAttribute getAttributeByName(String attrName) {
		for ( ItemAttribute attr : attributes.values() ) {
			if ( attrName.equals( attr.getType().getAttributeName() ) ) {
				return attr;
			}
		}
		return null;
	}
	
	public List<Prerequisite> getRequiredSkills(IStaticDataModel dataModel) 
	{
		final String attrNamePrefix ="requiredSkill";
		final List<Prerequisite> result = new ArrayList<>();
		
		for ( int i = 1 ; i < 100 ; i++) {
			String attrName = attrNamePrefix+i;
			final ItemAttribute skillAttr = getAttributeByName( attrName );
			if ( skillAttr == null ) {
				break;
			}
			
			final int skillId = skillAttr.safeGetIntValue();
			final Skill skill = dataModel.getSkillTree().getSkill( skillId );
			
			final ItemAttribute skillLvlAttr = getAttributeByName( attrNamePrefix+i+"Level" );
			final int reqLevel = skillLvlAttr.safeGetIntValue();
			result.add( new Prerequisite( skill , reqLevel ) );
		}
		return result;
	}
}