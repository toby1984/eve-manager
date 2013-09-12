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
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.TransactionType;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class TransactionTypeParser extends AbstractResponseParser<List<TransactionType>>
{

	public static final URI uri = toURI("/eve/RefTypes.xml.aspx");
	
	private List<TransactionType> result;
	
	/*
<?xml version="1.0" encoding="UTF-8"?>
<eveapi version="2">
  <currentTime>2009-05-13 01:55:56</currentTime>
  <result>
    <rowset name="refTypes" key="refTypeID" columns="refTypeID,refTypeName">
      <row refTypeID="0" refTypeName="Undefined"/>
      <row refTypeID="1" refTypeName="Player Trading"/>
      <row refTypeID="2" refTypeName="Market Transaction"/>
      <row refTypeID="3" refTypeName="GM Cash Transfer"/>
      <row refTypeID="4" refTypeName="ATM Withdraw"/>
      <row refTypeID="5" refTypeName="ATM Deposit"/>
      <row refTypeID="6" refTypeName="Backward Compatible"/>	 
	 */
	
	public TransactionTypeParser(ISystemClock clock) {
		super(clock);
	}
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException
	{
		
		final List<TransactionType> tmpResult =
			new ArrayList<TransactionType>();
			
		for ( Row r : parseRowSet( "refTypes" , document ).getRows() ) {
			
			tmpResult.add(
				new TransactionType(
					r.getLong( "refTypeID"),
					r.get( "refTypeName" )
				)
			);
		}

		this.result = tmpResult;
	}

	@Override
	public URI getRelativeURI()
	{
		return uri;
	}

	@Override
	public List<TransactionType> getResult() throws IllegalStateException
	{
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset()
	{
		result = null;
	}

}
