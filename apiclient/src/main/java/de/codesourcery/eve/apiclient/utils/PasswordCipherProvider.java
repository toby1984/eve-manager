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

import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NullCipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

/**
 * Cipher provider for PBE (password-based encryption).
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class PasswordCipherProvider implements ICipherProvider {

	private static final Logger log = Logger
			.getLogger(PasswordCipherProvider.class);
	
	private final IPasswordProvider passwordProvider;
	
	public PasswordCipherProvider(IPasswordProvider passwordProvider) {
		this.passwordProvider = passwordProvider;
	}
	
	/**
	 * Returns the password to use for encryption / decryption.
	 * 
	 * @return password to use or <code>null</code> for no encryption at all.
	 */
	protected char[] getPassword() {
		return passwordProvider.getPassword();
	}

	@Override
	public Cipher createCipher(boolean decrypt) {

		final char[] password = getPassword();

		if ( ArrayUtils.isEmpty( password ) ) {
			log.warn("createCipher(): No password , returning NULL cipher.");
			return new NullCipher();
		}

		try {

			final int iterations = 20;

			final byte[] salt =
				new byte[] { (byte) 0xab , (byte) 0xfe, 0x03, 0x47, (byte) 0xde, (byte) 0x99,(byte) 0xff, 0x1c };

			final PBEParameterSpec pbeSpec =
				new PBEParameterSpec( salt , iterations );

			final PBEKeySpec spec = new PBEKeySpec( password , salt , iterations );
			final SecretKeyFactory fac = SecretKeyFactory.getInstance("PBEWithMD5andDES");
			final SecretKey secretKey;
			try {
				secretKey = fac.generateSecret( spec );
			} 
			catch(InvalidKeySpecException e) {
				throw e;
			}

			final Cipher cipher=
				Cipher.getInstance("PBEWithMD5andDES");
			if ( decrypt ) {
				cipher.init( Cipher.DECRYPT_MODE , secretKey , pbeSpec );
			} else {
				cipher.init( Cipher.ENCRYPT_MODE , secretKey , pbeSpec );
			}
			return cipher;
		} 
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}		

}
