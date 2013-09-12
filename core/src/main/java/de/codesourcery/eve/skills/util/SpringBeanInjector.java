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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Parses a classes fields and methods for {@link Resource} annotations
 * and injects the corresponding spring beans.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class SpringBeanInjector implements BeanFactoryPostProcessor {

	private static final Logger log = Logger
			.getLogger(SpringBeanInjector.class);
	
	private static SpringBeanInjector INSTANCE;

	private BeanFactory factory;

	public SpringBeanInjector() {
		if ( INSTANCE != null ) {
			log.warn("SpringBeanInjector(): Creating singleton more than once ?");
		}
		INSTANCE = this;
	}
	
	/**
	 * UNIT-TESTS ONLY.
	 * 
	 * @param instance
	 */
	public static void setInstance(SpringBeanInjector instance) {
		INSTANCE = instance;
	}

	public static SpringBeanInjector getInstance() {
		return INSTANCE;
	}

	protected Object getBean(String name) {
		return factory.getBean( name );
	}

	public void injectDependencies(Object obj) {
		
		if ( obj == null ) {
			throw new IllegalArgumentException("Cannot inject into NULL object");
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("injectDependencies(): Injecting dependencies into "+obj.getClass().getName() );
		}
		
		try {
			doInjectDependencies( obj );
		} catch (Exception e) {
			throw new RuntimeException("Failed to inject dependencies into "+obj.getClass().getName() , e);
		}	
	}

	private void doInjectDependencies(Object obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Class<?> currentClasz = obj.getClass();
		do {
			// inject fields
			for ( Field f : currentClasz.getDeclaredFields() ) {
				
				final int m = f.getModifiers(); 
				if ( Modifier.isStatic( m ) || Modifier.isFinal( m ) ) {
					continue;
				}
				
				final Resource annot = f.getAnnotation( Resource.class );
				if ( annot != null ) {
					if ( ! f.isAccessible() ) {
						f.setAccessible( true );
					}
					
					if ( log.isTraceEnabled() ) {
						log.trace("doInjectDependencies(): Setting field "+
								f.getName()+" with bean '"+annot.name()+"'");
					}
					f.set( obj , getBean( annot.name() ) ); 
				}
			}
			
			// inject methods
			for ( Method method : currentClasz.getDeclaredMethods() ) {
				
				final int m = method.getModifiers(); 
				if ( Modifier.isStatic( m ) || Modifier.isAbstract( m ) ) {
					continue;
				}
				
				if ( method.getParameterTypes().length != 1 ) {
					continue;
				}
				
				if ( ! method.getName().startsWith("set" ) ) {
					continue;
				}
				
				final Resource annot = method.getAnnotation( Resource.class );
				if ( annot != null ) {
					if ( ! method.isAccessible() ) {
						method.setAccessible( true );
					}
					
					if ( log.isTraceEnabled() ) {
						log.trace("doInjectDependencies(): Invoking setter method "+
								method.getName()+" with bean '"+annot.name()+"'");
					}
					
					method.invoke( obj , getBean( annot.name() ) ); 
				}
			}			

			currentClasz = currentClasz.getSuperclass();
			
		} while ( currentClasz != null );
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}
}
