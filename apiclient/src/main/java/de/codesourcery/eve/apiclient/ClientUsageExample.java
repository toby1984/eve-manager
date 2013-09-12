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
package de.codesourcery.eve.apiclient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import de.codesourcery.eve.apiclient.datamodel.APIKey;
import de.codesourcery.eve.apiclient.datamodel.APIResponse;
import de.codesourcery.eve.apiclient.datamodel.Credentials;
import de.codesourcery.eve.apiclient.datamodel.RequestOptions;
import de.codesourcery.eve.apiclient.datamodel.ServerStatus;
import de.codesourcery.eve.skills.datamodel.IBaseCharacter;

public class ClientUsageExample {
	
	public static void main(String[] args) throws URISyntaxException, IOException {
		
		final APIKey limitedKey = 
			APIKey.createLimitedKey( "3qE5IzAo9l5Xizy0Nc9CXiXkzWzATGUQOUDNfmSMtRwL8RDUxQ0G4Sdlrs7hFZzi" ); 

		final Credentials credentials = 
			new Credentials( 2546100L , limitedKey );
		
		final HttpAPIClient client = 
			new HttpAPIClient();
		
		final APIResponse<ServerStatus> serverStatus = 
			client.getServerStatus( RequestOptions.DEFAULT );
		
		System.out.println("Response from server: "+serverStatus);
		
		client.getServerStatus( RequestOptions.DEFAULT );
		
		if ( serverStatus.getPayload().isServerOpen() ) {
			
			System.out.println("Listing characters...");
			
			final APIResponse<Collection<IBaseCharacter>> responseFromServer = 
				client.getAvailableCharacters( credentials , RequestOptions.DEFAULT );
			
			for ( IBaseCharacter c : responseFromServer.getPayload() ) {
				System.out.println("# Got character: "+c.getName()+" / character ID "+c.getCharacterId() );
			}
			
		} else {
			System.out.println("Server currently not open.");
		}
		
		client.dispose();
	}

}
