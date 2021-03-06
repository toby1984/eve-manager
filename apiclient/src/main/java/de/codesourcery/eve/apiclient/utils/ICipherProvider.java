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
package de.codesourcery.eve.apiclient.utils;

import javax.crypto.Cipher;

import de.codesourcery.eve.apiclient.cache.FilesystemResponseCache;

/**
 * Helper interface used by {@link FilesystemResponseCache#FilesystemResponseCache(java.io.File, ICipherProvider)}.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public interface ICipherProvider {

	/**
	 * Create cipher for encryption/decryption.
	 * 
	 * @param decrypt <code>true</code> to create an encryption cipher,
	 * <code>false</code> to create an encryption cipher
	 * @return
	 */
	public Cipher createCipher(boolean decrypt);
}
