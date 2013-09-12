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
package de.codesourcery.eve.skills.ui.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

public class DefaultAppConfigProvider implements IAppConfigProvider , DisposableBean {

	private static final String DEFAULT_FILENAME = ".eve_skills_config.properties";

	private static final Logger log = Logger
			.getLogger(DefaultAppConfigProvider.class);
	
	// guarded-by: listeners
	private final List<IAppConfigChangeListener> listeners =
		new ArrayList<IAppConfigChangeListener>();
	
	private final AppConfig config;
	
	public DefaultAppConfigProvider() throws IOException {
		this( ! StringUtils.isBlank( System.getProperty( "user.home" ) )? 
			  new File( System.getProperty("user.home") ,DEFAULT_FILENAME) :
			  new File( DEFAULT_FILENAME ) );
	}
	
	public DefaultAppConfigProvider(File file) throws IOException {
		log.info("DefaultAppConfigProvider(): Loading application config from "+file);
		config = new AppConfig( this , file );
	}
	
	@Override
	public AppConfig getAppConfig() {
		return config;
	}

	@Override
	public synchronized void save() throws IOException {
		log.info("save(): Saving configuration.");
		config.save();
	}

	@Override
	public void addChangeListener(IAppConfigChangeListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		
		synchronized (listeners) {
			listeners.add( l );
		}
	}

	@Override
	public void removeChangeListener(IAppConfigChangeListener l) {
		
		if (l == null) {
			throw new IllegalArgumentException("listener cannot be NULL");
		}
		
		synchronized (listeners) {
			listeners.remove( l );
		}		
	}

	@Override
	public void appConfigChanged(final String... properties ) {
		
		log.info("appConfigChanged(): Config(s) changed: "+
				ObjectUtils.toString( properties ) );
		
		List<IAppConfigChangeListener> copy;
		synchronized( listeners ) {
			copy = new ArrayList<IAppConfigChangeListener>( listeners );
		}
		for ( IAppConfigChangeListener l : copy ) {
			try {
				l.appConfigChanged( getAppConfig() , properties);
			} catch(Exception e) {
				log.error("appConfigChanged(): Listener "+l+" threw exception",e);
			}
		}
		
	}

	@Override
	public void destroy() throws Exception {
		save();
	}

}
