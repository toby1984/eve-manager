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
package de.codesourcery.eve.skills.ui.utils;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.ui.IMain;

public final class SpringUtil {
	
	public static final String SPRING_CONTEXT_PATH = 
		"/de/codesourcery/eve/skills/ui/spring-eve-skills-ui.xml";

	private static SpringUtil INSTANCE = new SpringUtil();
	
	private final ClassPathXmlApplicationContext context;
	
	protected SpringUtil() {
		context = 
			new ClassPathXmlApplicationContext(
					SPRING_CONTEXT_PATH);
	}
	
	public static SpringUtil getInstance() {
		synchronized( SpringUtil.class ) {
			if ( INSTANCE == null ) {
				INSTANCE = new SpringUtil();
			}
			return INSTANCE;
		}
	}
	
	public void shutdown() {
		synchronized( SpringUtil.class ) {
			context.close();
			INSTANCE = null;
		}
	}
	
	public IStaticDataModel getDataModel() {
		return (IStaticDataModel) context.getBean( "static-datamodel" );
	}
	
	public IMain getMain() {
		return (IMain) context.getBean("main");
	}
}
