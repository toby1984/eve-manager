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
package de.codesourcery.eve.apiclient.parsers;

import java.net.URI;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.codesourcery.eve.apiclient.datamodel.ServerStatus;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class ServerStatusParser extends AbstractResponseParser<ServerStatus> {

	private static final URI URI = toURI("/server/ServerStatus.xml.aspx");

	private ServerStatus serverStatus =
		new ServerStatus();
	
	public ServerStatusParser(ISystemClock clock) {
		super(clock);
	}
	
	@Override
	public URI getRelativeURI() {
		return URI;
	}
	
	/*
<?xml version='1.0' encoding='UTF-8'?>
<eveapi version="2">
  <currentTime>2008-11-24 20:14:29</currentTime>
  <result>
    <serverOpen>True</serverOpen>
    <onlinePlayers>38102</onlinePlayers>
  </result>
  <cachedUntil>2008-11-24 20:17:29</cachedUntil>
</eveapi>
	 */
	
	@Override
	protected void parseHook(Document document)
			throws UnparseableResponseException 
	{
		final Element result = getResultElement( document );
		serverStatus.setServerOpen( Boolean.parseBoolean( getChildValue( result , "serverOpen" ) ) );
		serverStatus.setPlayerCount( Integer.parseInt( getChildValue( result , "onlinePlayers" ) ) );
	}

	public ServerStatus getResult() {
		assertResponseParsed();
		return serverStatus;
	}

	@Override
	public void reset() {
		serverStatus = new ServerStatus();
	}



}
