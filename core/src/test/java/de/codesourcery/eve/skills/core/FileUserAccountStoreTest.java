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
package de.codesourcery.eve.skills.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import junit.framework.TestCase;
import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.APIKey.KeyType;
import de.codesourcery.eve.apiclient.utils.IPasswordProvider;
import de.codesourcery.eve.apiclient.utils.PasswordCipherProvider;
import de.codesourcery.eve.skills.accountdata.FileUserAccountStore;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.UserAccount;
import de.codesourcery.eve.skills.util.IInOutStreamProvider;
import de.codesourcery.eve.skills.util.StringInputStreamProvider;

public class FileUserAccountStoreTest extends TestCase {

	private static final String XML = "<?xml version=\"1.0\" ?><useraccounts fileVersion=\"1\">\n" + 
			"  <useraccount displayName=\"test account1\">\n" + 
			"    <credentials>\n" + 
			"      <userId>12345</userId>\n" + 
			"      <apikeys>\n" + 
			"        <apikey type=\"limited\">deadbeef</apikey>\n" + 
			"      </apikeys>\n" + 
			"    </credentials>\n" + 
			"    <characters>\n" + 
			"      <character characterId=\"123456\" name=\"twink37\" />\n" + 
			"      <character characterId=\"1234567\" name=\"mainchar\" />\n" + 
			"    </characters>\n" + 
			"  </useraccount>\n" + 
			"  <useraccount displayName=\"test account2\">\n" + 
			"    <credentials>\n" + 
			"      <userId>54321</userId>\n" + 
			"      <apikeys>\n" + 
			"        <apikey type=\"limited\">2l33t</apikey>\n" + 
			"        <apikey type=\"full\">2l33t2</apikey>\n" + 
			"      </apikeys>\n" + 
			"    </credentials>\n" + 
			"    <characters />\n" + 
			"  </useraccount>\n" + 		
			"  <useraccount displayName=\"empty account\">\n" + 
			"    <credentials />\n" + 
			"    <characters />\n" + 
			"  </useraccount>\n" + 		
			"</useraccounts>";
	
	private static final class InOutStream implements IInOutStreamProvider {

		private final InputStream in;
		private final OutputStream out;
		
		public InOutStream(byte[] input , OutputStream out) {
			this.in = new ByteArrayInputStream( input );
			this.out = out;
		}
		
		public InOutStream(InputStream in , OutputStream out) {
			this.in = in;
			this.out = out;
		}
		
		@Override
		public InputStream createInputStream() throws IOException {
			return in;
		}

		@Override
		public OutputStream createOutputStream() throws IOException {
			return out;
		}
		
	}
	
	private PasswordCipherProvider createCipherProvider(final String password) {
		return new PasswordCipherProvider( new IPasswordProvider() {

			@Override
			public char[] getPassword() {
				return password.toCharArray();
			}
		});
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testLoad() {
		
		FileUserAccountStore store =
			new FileUserAccountStore();
		
		store.setInputProvider( new IInOutStreamProvider() {

			@Override
			public InputStream createInputStream() throws IOException {
				return new StringInputStreamProvider( XML ).createInputStream(); 
			}

			@Override
			public OutputStream createOutputStream() throws IOException {
				throw new UnsupportedOperationException("createOutputStream");
			}
		} );
		
		store.unlock();
		verify( store );
	}
	
	public void testStore() throws IOException {

		final ByteArrayOutputStream out =
			new ByteArrayOutputStream();
		
		FileUserAccountStore store =
			new FileUserAccountStore();
		
		store.setInputProvider( new IInOutStreamProvider() {

			@Override
			public InputStream createInputStream() throws IOException {
				return new StringInputStreamProvider( XML ).createInputStream(); 
			}

			@Override
			public OutputStream createOutputStream() throws IOException {
				return out;
			}
		} );
		
		store.unlock();
		
		verify( store );

		final PasswordCipherProvider cipherProvider = 
			createCipherProvider("test");
			
		store.setCipherProvider( cipherProvider );
		store.persist( );
		
		assertTrue( out.toByteArray().length > 0 );
		assertFalse( new String( out.toByteArray() ).startsWith("<?xml" ) );
		
		final FileUserAccountStore store2 = new FileUserAccountStore();
		store2.setCipherProvider( cipherProvider );
		store2.setInputProvider( new InOutStream( out.toByteArray() , null ) );
		store2.unlock();
		
		verify( store2 );
	}
	
	private void verify(FileUserAccountStore store) {
		final List<UserAccount> accounts =
			store.getAccounts();
		
		assertNotNull( accounts );
		assertEquals( 3 , accounts.size() );
		
		// validate account #1
		UserAccount acct = store.getAccountByDisplayName( "test account1" );
		assertNotNull( acct );
		assertEquals( "test account1" , acct.getName() );
		assertNotNull( acct.getCredentials() );
		assertEquals( 12345L , acct.getCredentials().getUserId() );
		
		APIKey apiKey =
			acct.getCredentials().getKeyWithType( KeyType.LIMITED_KEY );
		assertNotNull( apiKey );
		assertEquals( "deadbeef" , apiKey.getValue() );
		
		assertFalse( acct.getCredentials().hasKey( KeyType.FULL_ACCESS ) );
		
		final List<ICharacter> chars = acct.getCharacters();
		assertNotNull( chars );
		assertEquals( 2 , chars.size() );
		
		ICharacter c = acct.getCharacterByID( new CharacterID("123456" ) );
		assertEquals( new CharacterID("123456") , c.getCharacterId() );
		assertEquals( "twink37" , c.getName() );
		
		c = acct.getCharacterByID( new CharacterID("1234567" ) );
		assertEquals( new CharacterID("1234567") , c.getCharacterId() );
		assertEquals( "mainchar" , c.getName() );		
		
		// validate account #2
		acct = store.getAccountByDisplayName( "test account2" );
		assertNotNull( acct );
		assertEquals( "test account2" , acct.getName() );
		assertNotNull( acct.getCredentials() );
		assertEquals( 54321L , acct.getCredentials().getUserId() );
		
		apiKey =
			acct.getCredentials().getKeyWithType( KeyType.LIMITED_KEY );
		assertNotNull( apiKey );
		assertEquals( "2l33t" , apiKey.getValue() );
		
		apiKey =
			acct.getCredentials().getKeyWithType( KeyType.FULL_ACCESS);
		assertNotNull( apiKey );
		assertEquals( "2l33t2" , apiKey.getValue() );		
		
		// validate account #3	
		acct = store.getAccountByDisplayName( "empty account" );
		assertNotNull( acct );
		assertEquals( "empty account" , acct.getName() );
		assertNotNull(acct.getCredentials() );
		assertEquals( 0 , acct.getCredentials().getUserId() );
		assertFalse( acct.getCredentials().hasKey( KeyType.LIMITED_KEY ) );
		assertFalse( acct.getCredentials().hasKey( KeyType.FULL_ACCESS ) );
	}
}
