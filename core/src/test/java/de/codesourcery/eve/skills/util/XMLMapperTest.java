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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.lang.ObjectUtils;

import de.codesourcery.eve.skills.util.XMLMapper.IFieldConverter;
import de.codesourcery.eve.skills.util.XMLMapper.IFieldConverters;

public class XMLMapperTest extends TestCase {

	public static final class SimpleBean1 {
		private String property1;
	}

	public static final class SimpleBean2 {
		private transient String property1;
	}

	public static final class SimpleBean3 {
		private int[] property;
	}

	public static final class SimpleBean4 {
		private String[] property;
	}

	public static final class SimpleBean5 {
		private String[] property1;
		private int[] property2;
		private float[] property3;
	}

	public void testPropertyNameMapping() throws Exception {

		final Map<String,String> mappings =
			new HashMap<String,String>();
		
		mappings.put("property1" , "a" );
		mappings.put("property2" , "b" );
		mappings.put("property3" , "c" );
		
		final XMLMapper mapper =
			new XMLMapper( mappings );

		final SimpleBean5 bean = new SimpleBean5();
		bean.property1 = new String[] { "X" };
		bean.property2 = new int[] { 1 , 2 , 3};
		bean.property3 = new float[] { 3.14f };

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		System.out.println("Result = "+result);
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row c=\"3.14\" b=\"1,2,3\" a=\"X\"></row></rows>",result);

		final Collection<SimpleBean5> read = 
			mapper.read( SimpleBean5.class , converters , new ByteArrayInputStream( out.toByteArray() ) );

		assertNotNull( read );
		assertEquals( 1 , read.size() );
		
		final SimpleBean5 readBean = read.iterator().next() ;
		assertArrayEquals( new String[] { "X" } , readBean.property1 );
		assertArrayEquals( new int[] { 1 , 2 ,3 } , readBean.property2 );
		assertArrayEquals( new float[] { 3.14f } , readBean.property3 );
		
	}
	
	private void assertArrayEquals(int[] expected , int[] actual) {
		
		boolean result;
		if ( expected == null || actual == null ) {
			result = expected == actual;
		} else {
			if ( expected.length != actual.length ) {
				result = false;
			} else {
				for ( int i = 0 ; i < expected.length ; i++ ) {
					if ( ! ObjectUtils.equals( expected[i], actual[i] ) ) {
						throw new AssertionFailedError("expected: "+ObjectUtils.toString( expected )+" , got: "
								+ObjectUtils.toString( actual ) );
					}
				}
				return;
			}
		}
		
		if ( !result ) {
			throw new AssertionFailedError("expected: "+ObjectUtils.toString( expected )+" , got: "
					+ObjectUtils.toString( actual ) );
		}
	}
	
	private void assertArrayEquals(float[] expected , float[] actual) {
		
		boolean result;
		if ( expected == null || actual == null ) {
			result = expected == actual;
		} else {
			if ( expected.length != actual.length ) {
				result = false;
			} else {
				for ( int i = 0 ; i < expected.length ; i++ ) {
					if ( ! ObjectUtils.equals( expected[i], actual[i] ) ) {
						throw new AssertionFailedError("expected: "+ObjectUtils.toString( expected )+" , got: "
								+ObjectUtils.toString( actual ) );
					}
				}
				return;
			}
		}
		
		if ( !result ) {
			throw new AssertionFailedError("expected: "+ObjectUtils.toString( expected )+" , got: "
					+ObjectUtils.toString( actual ) );
		}
	}
	
	private <X> void assertArrayEquals(X[] expected , X[] actual) {
		
		boolean result;
		if ( expected == null || actual == null ) {
			result = expected == actual;
		} else {
			if ( expected.length != actual.length ) {
				result = false;
			} else {
				for ( int i = 0 ; i < expected.length ; i++ ) {
					if ( ! ObjectUtils.equals( expected[i], actual[i] ) ) {
						throw new AssertionFailedError("expected: "+ObjectUtils.toString( expected )+" , got: "
								+ObjectUtils.toString( actual ) );
					}
				}
				return;
			}
		}
		
		if ( !result ) {
			throw new AssertionFailedError("expected: "+ObjectUtils.toString( expected )+" , got: "
					+ObjectUtils.toString( actual ) );
		}
	}
	
	public void testSimpleBeanToXML() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean1 bean = new SimpleBean1();
		bean.property1 = "testvalue";

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property1=\"testvalue\"></row></rows>",result);
	}

	public void testNullValueSerialization() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean1 bean = new SimpleBean1();

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property1=\"NIL\"></row></rows>",result);

		final Collection<SimpleBean1> read = 
			mapper.read( SimpleBean1.class , converters , new ByteArrayInputStream( out.toByteArray() ) );


		assertNotNull( read );
		assertEquals( 1 , read.size() );
		assertNull( read.iterator().next().property1 );
	}	

	public void testNullArraySerialization() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean4 bean = new SimpleBean4();

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property=\"NIL\"></row></rows>",result);

		final Collection<SimpleBean4> read = 
			mapper.read( SimpleBean4.class , converters , new ByteArrayInputStream( out.toByteArray() ) );


		assertNotNull( read );
		assertEquals( 1 , read.size() );
		assertNull( read.iterator().next().property );
	}		

	public void testNullArrayElementSerialization() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean4 bean = new SimpleBean4();

		bean.property = new String[] { "a" , null , "b" };

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property=\"a,NIL,b\"></row></rows>",result);

		final Collection<SimpleBean4> read = 
			mapper.read( SimpleBean4.class , converters , new ByteArrayInputStream( out.toByteArray() ) );

		assertNotNull( read );
		assertEquals( 1 , read.size() );
		final String[] parsed = read.iterator().next().property; 
		assertNotNull( parsed );
		assertEquals( 3 , parsed.length );
		assertEquals( "a" , parsed[0] );
		assertEquals( null , parsed[1] );
		assertEquals( "b" , parsed[2] );
	}		

	public void testNilQuoting1() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean4 bean = new SimpleBean4();

		bean.property = new String[] { "a" , "NIL" , "b" };

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property=\"a,\\\\NIL,b\"></row></rows>",result);

		final Collection<SimpleBean4> read = 
			mapper.read( SimpleBean4.class , converters , new ByteArrayInputStream( out.toByteArray() ) );

		assertNotNull( read );
		assertEquals( 1 , read.size() );
		final String[] parsed = read.iterator().next().property; 
		assertNotNull( parsed );
		assertEquals( 3 , parsed.length );
		assertEquals( "a" , parsed[0] );
		assertEquals( "NIL" , parsed[1] );
		assertEquals( "b" , parsed[2] );
	}		

	public void testNilQuoting2() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean1 bean = new SimpleBean1();

		bean.property1 = "NIL";

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		System.out.println("Result ================> "+result);
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property1=\"\\\\NIL\"></row></rows>",result);

		final Collection<SimpleBean1> read = 
			mapper.read( SimpleBean1.class , converters , new ByteArrayInputStream( out.toByteArray() ) );

		assertNotNull( read );
		assertEquals( 1 , read.size() );
		final String parsed = read.iterator().next().property1; 
		assertEquals( "NIL" , parsed );
	}	

	public void testTransientFieldsAreSkipped() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean2 bean = new SimpleBean2();

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows></rows>" , result );
	}

	public void testQuotingWorks() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean1 bean = new SimpleBean1();
		bean.property1="a text \\ with , some , quotes";

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property1=\"a text \\\\ with \\, some \\, quotes\"></row></rows>",result);
	}	

	public void testArraySerialization() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean3 bean = new SimpleBean3();
		bean.property = new int[] { 1 , 2, 3 };

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property=\"1,2,3\"></row></rows>",result);

		Collection<SimpleBean3> read = 
			mapper.read( SimpleBean3.class , converters , new ByteArrayInputStream( out.toByteArray() ) );

		assertNotNull( read );
		assertEquals( 1 , read.size() );
		int[] parsed = read.iterator().next().property;
		assertNotNull( parsed );
		assertEquals( 3 , parsed.length );
		assertEquals( 1 , parsed[0] );
		assertEquals( 2 , parsed[1] );
		assertEquals( 3 , parsed[2] );
	}	

	public void testStringArraySerializationWithQuoting() throws Exception {

		final XMLMapper mapper =
			new XMLMapper();

		final SimpleBean4 bean = new SimpleBean4();
		bean.property = new String[] { "needs,quoting" , "needs\\quoting" };

		final List<Object> beans = new ArrayList<Object>();
		beans.add( bean );

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		final IFieldConverters converters = new IFieldConverters() {

			@Override
			public IFieldConverter getConverter(Field f) {
				return XMLMapper.DEFAULT_CONVERTER; 
			}

		};

		mapper.write( beans , converters , out );

		final String result = new String( out.toByteArray() );
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rows><row property=\"needs\\,quoting,needs\\\\quoting\"></row></rows>",result);

		Collection<SimpleBean4> read = 
			mapper.read( SimpleBean4.class , converters , new ByteArrayInputStream( out.toByteArray() ) );

		assertNotNull( read );
		assertEquals( 1 , read.size() );
		String[] parsed = read.iterator().next().property;
		assertNotNull( parsed );
		assertEquals( 2 , parsed.length );
		assertEquals( "needs,quoting" , parsed[0] );
		assertEquals( "needs\\quoting" , parsed[1] );
	}		
}
