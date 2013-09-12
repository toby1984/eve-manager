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
package de.codesourcery.eve.skills.datamodel;

import org.apache.commons.lang.StringUtils;

public class TransactionType
{
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
	
	private final long id;
	private final String name;
	
	public TransactionType(long id, String name) {
		if ( id < 0 ) {
			throw new IllegalArgumentException("Invalid refType ID "+id);
		}
		
		if ( StringUtils.isBlank(name) ) {
			throw new IllegalArgumentException("name cannot be blank.");
		}
		
		this.id = id;
		this.name = name;
	}
	
	public Long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
}
