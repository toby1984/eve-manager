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

import java.io.IOException;

/**
 * Persistence provider for the application's
 * configuration data.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface IAppConfigProvider {
	
	public void addChangeListener(IAppConfigChangeListener l);
	
	public void removeChangeListener(IAppConfigChangeListener l);

	public void appConfigChanged(String... properties);
	
	public AppConfig getAppConfig();
	
	public void save() throws IOException;
}
