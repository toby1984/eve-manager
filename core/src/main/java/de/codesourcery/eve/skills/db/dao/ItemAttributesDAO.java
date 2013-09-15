package de.codesourcery.eve.skills.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import de.codesourcery.eve.skills.db.datamodel.AttributeCategory;
import de.codesourcery.eve.skills.db.datamodel.InventoryType;
import de.codesourcery.eve.skills.db.datamodel.ItemWithAttributes;
import de.codesourcery.eve.skills.db.datamodel.ItemAttribute;
import de.codesourcery.eve.skills.db.datamodel.ItemAttributeType;

public class ItemAttributesDAO extends JdbcTemplate implements IItemAttributesDAO
{
	public ItemAttributesDAO() {
	}

	@Override
	public ItemWithAttributes getAttributes(InventoryType type) 
	{
		final String sql = "select map.valueInt,map.valueFloat,attrs.* from dgmTypeAttributes map , dgmAttributeTypes attrs where " +
				"map.typeID="+type.getId()+" and attrs.attributeID=map.attributeID";
		
		@SuppressWarnings("unchecked")
		final List<ItemAttribute> attributes = query(sql,new RowMapper() {

			/*
| valueInt | valueFloat | attributeID | attributeName | description | iconID | defaultValue | published | 
 displayName | unitID | stackable | highIsGood | categoryID |
			 
			 */
			@Override
			public ItemAttribute mapRow(ResultSet rs, int rowNum) throws SQLException 
			{
				final ItemAttributeType type = new ItemAttributeType();
				type.setId( rs.getLong("attributeID" ) );
				type.setAttributeName( rs.getString("attributeName" ) );
				type.setDescription( rs.getString("description" ) );
				type.setDefaultValue( rs.getDouble("defaultValue"));
				type.setPublished( rs.getInt("published" ) );
				type.setDisplayName( rs.getString("displayName" ) );
				type.setStackable( rs.getInt("stackable") );
				type.setHighIsGood( rs.getInt("highIsGood") );
				type.setCategory( AttributeCategory.valueOf( (Long) rs.getObject("categoryID" ) ) );
				
				final ItemAttribute attribute = new ItemAttribute( type );
				attribute.setFloatValue( (Double) rs.getObject( "valueFloat" ) );
				attribute.setIntValue( (Integer) rs.getObject("valueInt" ) );
				return attribute;
			}
		} );
		return new ItemWithAttributes(type,attributes);
	}
}