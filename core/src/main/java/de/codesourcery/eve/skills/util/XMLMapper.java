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
package de.codesourcery.eve.skills.util;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.beanutils.ConvertUtils;

/**
 * Crude XML &lt;-&gt; bean mapper.
 * 
 * This class uses reflection to map a bean's fields
 * to XML and vice versa. The generated XML contains
 * one tag per bean, writing the bean's properties
 * as attributes. By default, the attribute names
 * resemble the bean's field names. This can be 
 * changed (to trade readability for speed) by
 * using the {@link XMLMapper#XMLMapper(Map)} constructor
 * and providing a property-to-attribute name mapping.
 * @author tobias.gierke@code-sourcery.de
 */
public class XMLMapper {

	private static final String OUTPUT_ENCODING = "ISO-8859-1";

	private Map<String,String> propertyNameMappings =
		new HashMap<String,String>();

	private static final String QUOTED_NIL = "\\NIL";
	private static final String NIL = "NIL";

	/**
	 * Provides converters to map field values to
	 * strings and vice versa.
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
	public interface IFieldConverters {
		/**
		 * Returns the converter to use for a given field.
		 * 
		 * If this method returns <code>null</code> ,
		 * the Apache Commons bean-utils converters will
		 * be used for this field.
		 * @param f
		 * @return field converter or <code>null</code> to 
		 * use the default converter.
		 */
		public IFieldConverter getConverter(Field f);
	}

	/**
	 * Converts Objects to strings and vice versa.
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
	public interface IFieldConverter {

		public String[] toString(Object fieldValue);

		public Object toObject(String[] values,Class<?> targetType);
	}

	public XMLMapper() {
	}

	/**
	 * Converter that uses Apache Commons beanutils
	 * to convert bean property values to a string
	 * representation and vice versa.
	 */
	public static final IFieldConverter DEFAULT_CONVERTER =
		new IFieldConverter() {

		@Override
		public Object toObject(String[] values , Class<?> targetType) {
			// filter 
			if ( values != null ) {
				final int len = values.length;

				if ( ! targetType.isArray() ) {
					if ( len != 1 ) {
						throw new IllegalArgumentException("Cannot assign array to non-array type "+targetType.getName());
					}
					if ( QUOTED_NIL.equals( values[0]) ) {
						return NIL;
					} else if ( NIL.equals( values[0] ) ) {
						return null;
					}
					return ConvertUtils.convert( values[0] , targetType );
				}
				for ( int i = 0 ; i < len ; i++ ) {
					if ( QUOTED_NIL.equals( values[i] ) ) {
						values[i] = NIL;
					} else if ( NIL.equals( values[i] ) ) {
						values[i] = null;
					}
				}
			}
			return ConvertUtils.convert( values , targetType );
		}

		@Override
		public String[] toString(Object fieldValue) {
			if ( fieldValue == null ) {
				return new String[] { NIL };
			} else if ( NIL.equals( fieldValue ) ) {
				return new String[] { QUOTED_NIL };
			}

			if ( fieldValue.getClass().isArray() ) { // convert array
				final int len =
					Array.getLength( fieldValue );
				final String[] result =
					new String[ len ];
				for ( int i = 0 ; i < len ; i++ ) {
					final Object val = Array.get( fieldValue , i );
					if ( val == null ) {
						result[i] = NIL;
					} else if ( NIL.equals( val ) ) {
						result[i] = QUOTED_NIL;
					} else {
						result[i] = (String) ConvertUtils.convert( val );
					}
				}
				return result;
			}

			if ( NIL.equals( fieldValue ) ) {
				return new String[] { QUOTED_NIL };
			}
			return new String[] { ConvertUtils.convert( fieldValue ) }; 
		}
	};

	private static IFieldConverters DEFAULT_CONVERTERS =
		new IFieldConverters() {

		@Override
		public IFieldConverter getConverter(Field f) {
			return DEFAULT_CONVERTER;
		}
	};		

	// tmp storage 
	private static final StringBuilder builder =
		new StringBuilder();

	private static String quote(String s) {
		if ( s== null || s.trim().length() == 0 ) {
			return s;
		}

		final char[] chars = s.toCharArray();
		builder.setLength(0);
		final int len = chars.length;
		for ( int i = 0 ; i < len ; i++ ) {
			final char c = chars[i];
			if ( c == '\\' || c == ',' ) {
				builder.append( '\\' );
			}
			builder.append( c );
		}
		return builder.toString();
	}

	public XMLMapper(Map<String,String> propertyNameMappings) {
		if (propertyNameMappings == null) {
			throw new IllegalArgumentException(
			"propertyNameMappings cannot be NULL");
		}
		this.propertyNameMappings = propertyNameMappings;
	}

	private static final class BeanDescription {
		private Map<String,Field> fields = new HashMap<String,Field>();

		public BeanDescription() {
		}

		public void addField(Field f) {
			this.fields.put( f.getName() , f );
		}

		public Collection<Field> getFields() { return fields.values(); }

		public Field getFieldByName(String name) {
			final Field result = fields.get( name );
			if ( result == null ) {
				throw new IllegalArgumentException("Unknown field "+name);
			}
			return result;
		}
	}

	private BeanDescription createBeanDescription(Class<?> clasz) {

		BeanDescription result = new BeanDescription();
		for ( java.lang.reflect.Field f : clasz.getDeclaredFields() ) {
			final int modifiers = f.getModifiers();
			if ( Modifier.isFinal( modifiers ) ||
					Modifier.isStatic( modifiers ) ||
					Modifier.isTransient( modifiers ) ) 
			{
				continue;
			}
			if ( ! f.isAccessible() ) {
				f.setAccessible( true );
			}
			result.addField( f );
		}
		return result;
	}

	private static final String toAttributeValue(String[] s) {

		final StringBuilder builder2 = new StringBuilder();

		final int len = s.length;
		for ( int i = 0 ; i < len ; i++ ) {
			builder2.append( quote( s[i] ) );
			if ( (i+1) < len ) {
				builder2.append( ',' );
			}
		}
		return builder2.toString();
	}

	private static final String[] fromAttributeValue(String attrValue) {

		if ( QUOTED_NIL.equals( attrValue ) ) {
			return new String[] { NIL };
		}

		final List<String> result = new ArrayList<String>();
		builder.setLength(0);
		final char[] chars = attrValue.toCharArray();
		final int len = attrValue.length();

		for ( int i = 0 ; i < len ; i++ ) {
			final char c = chars[i];
			if ( c == '\\' ) {
				i++;
				if ( i < len ) {
					builder.append( chars[i] );
				}
				continue;
			} else if ( c == ',' ) {
				result.add( builder.toString() );
				builder.setLength(0);
			} else {
				builder.append( c );
			}
		}

		if ( builder.length() > 0 ) {
			result.add( builder.toString() );
		}

		return result.toArray( new String[ result.size() ] );
	}


	public void write(Collection<?> beans,OutputStream outstream) throws XMLStreamException, IntrospectionException, IllegalArgumentException, IllegalAccessException {
		write( beans , DEFAULT_CONVERTERS , outstream );
	}
	
	public void write(Collection<?> beans,IFieldConverters converters,OutputStream outstream) throws XMLStreamException, IntrospectionException, IllegalArgumentException, IllegalAccessException {

		final XMLOutputFactory factory = XMLOutputFactory.newInstance();
		final XMLStreamWriter writer = factory.createXMLStreamWriter(outstream,OUTPUT_ENCODING);
		try {
			writer.writeStartDocument(OUTPUT_ENCODING, "1.0");
			writer.writeStartElement("rows");

			if ( beans.isEmpty() ) {
				writer.writeEndDocument();
				return;
			}

			final Class<?> beanClass = beans.iterator().next().getClass();

			final BeanDescription desc = createBeanDescription( beanClass );

			final Collection<Field> fields = desc.getFields();
			if ( fields.isEmpty() ) {
				writer.writeEndDocument();
				return;
			}

			for ( Object bean : beans ) {
				writer.writeStartElement("row");
				for ( Field f : fields ) {
					final Object fieldValue = f.get( bean );
					final String[] values = 
						converters.getConverter( f ).toString( fieldValue );

					final String attrName =
						this.propertyNameMappings.get( f.getName() );
					if ( values == null ) {
						writer.writeAttribute( f.getName(), NIL );
					} else if ( attrName == null ) {
						writer.writeAttribute( f.getName(), toAttributeValue( values ) );
					} else {
						writer.writeAttribute( attrName , toAttributeValue( values ) );
					}
				}
				writer.writeEndElement();
			}
			writer.writeEndDocument();
		} 
		finally {
			writer.close();
		}
	}
	
	public <T> Collection<T> read(Class<T> clasz , InputStream instream) throws XMLStreamException, IOException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
		return read( clasz , DEFAULT_CONVERTERS , instream );
	}

	public <T> Collection<T> read(Class<T> clasz , IFieldConverters converters,InputStream instream) throws XMLStreamException, IOException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {

		final Collection<T> result = new ArrayList<T>();

		try {

			final BeanDescription desc = createBeanDescription( clasz );

			/* 
			 * Create inverse mapping attribute name -> field. 
			 */
			final Map<String,Field> inverseMapping =
				new HashMap<String,Field>();

			if ( ! this.propertyNameMappings.isEmpty() ) {

				// key = property name  / value = attribute name
				for ( Map.Entry<String,String> propToAttribute : this.propertyNameMappings.entrySet() ) {
					inverseMapping.put( propToAttribute.getValue() , 
							desc.getFieldByName( propToAttribute.getKey() ) 
					);
				}

			} else { // create default mappings
				for ( Field f : desc.getFields() ) {
					inverseMapping.put( f.getName() , f );
				}
			}

			final int fieldCount = desc.getFields().size();

			final XMLInputFactory factory = XMLInputFactory.newInstance();
			final XMLStreamReader parser = factory.createXMLStreamReader(instream);

			boolean inRow = false;

			final Constructor<T> constructor =
				clasz.getConstructor(new Class<?>[0] );

			for (int event = parser.next();  event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) 
			{
				switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					if ( "row".equals( parser.getLocalName() ) ) { // parse row
						if ( inRow ) {
							throw new XMLStreamException("Found nested <row> tag ?", parser.getLocation() );
						}
						inRow = true;

						final T bean = constructor.newInstance(new Object[0]);
						for ( int i = 0 ; i < fieldCount ; i++ ) {
							final String attrName = parser.getAttributeLocalName( i );
							final String attrValue = parser.getAttributeValue( i );
							final Field field = inverseMapping.get( attrName );

							if ( ! NIL.equals( attrValue ) ) {
								final Object fieldValue = 
									converters.getConverter( field ).toObject( 
											fromAttributeValue( attrValue ) , field.getType() 
									);
								field.set( bean , fieldValue );
							} else {
								field.set( bean , null );
							}

						}
						result.add( bean );
					}
					break;

				case XMLStreamConstants.END_ELEMENT:
					if ( "row".equals( parser.getLocalName() ) ) { // parse row
						if ( ! inRow ) {
							throw new XMLStreamException("Found </row> tag without start tag at ",parser.getLocation() );
						}
						inRow = false;
					}
					break;						

				}
			} 
		} finally {
			instream.close();
		}

		return result;
	}

}
