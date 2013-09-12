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
package de.codesourcery.eve.skills.db.datamodel;


public enum StationService
{
	BOUNTY_MISSIONS(1,"Bounty missions"),
	ASSASSINATION_MISSIONS( 2 , "Assasination missions"),
	COURIER_MISSIONS(4, "Courier missions"),
	INTERBUS(8,"Interbus"),
	REPROCESSING(16,"Reprocessing"),
	REFINING(32,"Refining"),
	MARKET(64,"Market"),
	BLACK_MARKET(128,"Black market"),
	STOCK_EXCHANGE(1024,"Stock exchange"),
	CLONING(512,"Cloning"),
	SURGERY(1024,"Surgery"),
	DNA_THERAPY(2048,"DNA therapy"),
	REPAIR(4096,"Repair facilities"),
	FACTORY(8192,"Factory"),
	LABORATORY(16384,"Laboratoy"),
	GAMBLING(32768,"Gambling"),
	FITTING(65536,"Fitting"),
	PAINTSHOP(131072,"Paintshop"),
	NEWS(262144,"News"),
	STORAGE(524288,"Storage"),
	INSURANCE(1048576 , "Insurance"),
	DOCKING(2097152,"Docking"),
	OFFICE_RENTAL(4194304,"Office renting"),
	JUMP_CLONE_FACILITY(8388608 , "Jump clone facility"),
	LP_STORE(16777216,"Loyalty point store"),
	NAVY_OFFICES(33554432,"Navy offices");
	
	/*
	 mysql> select * from staServices;
+-----------+------------------------+----------------------------------+
| serviceID | serviceName            | description                      |
+-----------+------------------------+----------------------------------+
|         1 | Bounty Missions        |                                  |
|         2 | Assassination Missions |                                  |
|         4 | Courier Missions       |                                  |
|         8 | Interbus               |                                  |
|        16 | Reprocessing Plant     |                                  |
|        32 | Refinery               |                                  |
|        64 | Market                 |                                  |
|       128 | Black Market           |                                  |
|       256 | Stock Exchange         |                                  |
|       512 | Cloning                |                                  |
|      1024 | Surgery                |                                  |
|      2048 | DNA Therapy            |                                  |
|      4096 | Repair Facilities      |                                  |
|      8192 | Factory                |                                  |
|     16384 | Laboratory             |                                  |
|     32768 | Gambling               |                                  |
|     65536 | Fitting                |                                  |
|    131072 | Paintshop              |                                  |
|    262144 | News                   |                                  |
|    524288 | Storage                |                                  |
|   1048576 | Insurance              | Used to buy insurance for ships. |
|   2097152 | Docking                |                                  |
|   4194304 | Office Rental          |                                  |
|   8388608 | Jump Clone Facility    |                                  |
|  16777216 | Loyalty Point Store    |                                  |
|  33554432 | Navy Offices           |                                  |
+-----------+------------------------+----------------------------------+
	 */
	
	private final long serviceID;
	private final String serviceName;
	
	private StationService(long id , String name) {
		this.serviceID = id;
		this.serviceName = name;
	}

	public String getName()
	{
		return serviceName;
	}
	
	public static StationService fromTypeId(long id) {
		
		for ( StationService s : values() ) {
			if ( s.serviceID == id ) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unknown station service type ID "+id);
	}
	
	public long getId()
	{
		return serviceID;
	}
	
}
