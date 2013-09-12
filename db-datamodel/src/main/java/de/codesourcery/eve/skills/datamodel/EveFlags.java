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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Misc. flags used by the EVE API , among
 * other things asset storage location details
 * are encoded using these flags.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public enum EveFlags {
	NONE(0,"None"),
	WALLET(1,"Wallet"),
	FACTORY(2,"Factory"),
	HANGAR(4,"Hangar"),
	CARGO(5,"Cargo"),
	BRIEFCASE(6,"Briefcase"),
	SKILL(7,"Skill"),
	REWARD(8,"Reward"),
	CONNECTED(9,"Character in station connected"),
	DISCONNECTED(10,"Character in station offline"),
	LOSLOT0(11,"Low power slot 1",1),
	LOSLOT1(12,"Low power slot 2",2),
	LOSLOT2(13,"Low power slot 3",3),
	LOSLOT3(14,"Low power slot 4",4),
	LOSLOT4(15,"Low power slot 5",5),
	LOSLOT5(16,"Low power slot 6",6),
	LOSLOT6(17,"Low power slot 7",7),
	LOSLOT7(18,"Low power slot 8",8),
	MEDSLOT0(19,"Medium power slot 1",1),
	MEDSLOT1(20,"Medium power slot 2",2),
	MEDSLOT2(21,"Medium power slot 3",3),
	MEDSLOT3(22,"Medium power slot 4",4),
	MEDSLOT4(23,"Medium power slot 5",5),
	MEDSLOT5(24,"Medium power slot 6",6),
	MEDSLOT6(25,"Medium power slot 7",7),
	MEDSLOT7(26,"Medium power slot 8",8),
	HISLOT0(27,"High power slot 1",1),
	HISLOT1(28,"High power slot 2",2),
	HISLOT2(29,"High power slot 3",3),
	HISLOT3(30,"High power slot 4",4),
	HISLOT4(31,"High power slot 5",5),
	HISLOT5(32,"High power slot 6",6),
	HISLOT6(33,"High power slot 7",7),
	HISLOT7(34,"High power slot 8",8),
	FIXED_SLOT(35,"Fixed Slot"),
	PROMENADESLOT1(40,"Promenade Slot 1"),
	PROMENADESLOT2(41,"Promenade Slot 2"),
	PROMENADESLOT3(42,"Promenade Slot 3"),
	PROMENADESLOT4(43,"Promenade Slot 4"),
	PROMENADESLOT5(44,"Promenade Slot 5"),
	PROMENADESLOT6(45,"Promenade Slot 6"),
	PROMENADESLOT7(46,"Promenade Slot 7"),
	PROMENADESLOT8(47,"Promenade Slot 8"),
	PROMENADESLOT9(48,"Promenade Slot 9"),
	PROMENADESLOT10(49,"Promenade Slot 10"),
	PROMENADESLOT11(50,"Promenade Slot 11"),
	PROMENADESLOT12(51,"Promenade Slot 12"),
	PROMENADESLOT13(52,"Promenade Slot 13"),
	PROMENADESLOT14(53,"Promenade Slot 14"),
	PROMENADESLOT15(54,"Promenade Slot 15"),
	PROMENADESLOT16(55,"Promenade Slot 16"),
	CAPSULE(56,"Capsule"),
	PILOT(57,"Pilot"),
	PASSENGER(58,"Passenger"),
	BOARDING_GATE(59,"Boarding gate"),
	CREW(60,"Crew"),
	SKILL_IN_TRAINING(61,"Skill in training"),
	CORPMARKET(62,"Corporation Market Deliveries / Returns"),
	LOCKED(63,"Locked item, can not be moved unless unlocked"),
	UNLOCKED(64,"Unlocked item, can be moved"),
	OFFICE_SLOT_1(70,"Office slot 1"),
	OFFICE_SLOT_2(71,"Office slot 2"),
	OFFICE_SLOT_3(72,"Office slot 3"),
	OFFICE_SLOT_4(73,"Office slot 4"),
	OFFICE_SLOT_5(74,"Office slot 5"),
	OFFICE_SLOT_6(75,"Office slot 6"),
	OFFICE_SLOT_7(76,"Office slot 7"),
	OFFICE_SLOT_8(77,"Office slot 8"),
	OFFICE_SLOT_9(78,"Office slot 9"),
	OFFICE_SLOT_10(79,"Office slot 10"),
	OFFICE_SLOT_11(80,"Office slot 11"),
	OFFICE_SLOT_12(81,"Office slot 12"),
	OFFICE_SLOT_13(82,"Office slot 13"),
	OFFICE_SLOT_14(83,"Office slot 14"),
	OFFICE_SLOT_15(84,"Office slot 15"),
	OFFICE_SLOT_16(85,"Office slot 16"),
	BONUS(86,"Bonus"),
	DRONEBAY(87,"Drone Bay"),
	BOOSTER(88,"Booster"),
	IMPLANT(89,"Implant"),
	SHIPHANGAR(90,"Ship Hangar"),
	SHIPOFFLINE(91,"Ship Offline"),
	RIGSLOT0(92,"Rig power slot 1",1),
	RIGSLOT1(93,"Rig power slot 2",2),
	RIGSLOT2(94,"Rig power slot 3",3),
	RIGSLOT3(95,"Rig power slot 4",4),
	RIGSLOT4(96,"Rig power slot 5",5),
	RIGSLOT5(97,"Rig power slot 6",6),
	RIGSLOT6(98,"Rig power slot 7",7),
	RIGSLOT7(99,"Rig power slot 8",8),
	FACTORY_OPERATION(100,"Factory Background Operation"),
	CORPSAG2(116,"Corp Security Access Group 2"),
	CORPSAG3(117,"Corp Security Access Group 3"),
	CORPSAG4(118,"Corp Security Access Group 4"),
	CORPSAG5(119,"Corp Security Access Group 5"),
	CORPSAG6(120,"Corp Security Access Group 6"),
	CORPSAG7(121,"Corp Security Access Group 7"),
	SECONDARYSTORAGE(122,"Secondary Storage"),
	CAPTAINSQUARTERS(123,"Captains Quarters"),
	WIS_PROMENADE(124,"Wis Promenade"),
	SUBSYSTEM0(125,"Sub system slot 0",0),
	SUBSYSTEM1(126,"Sub system slot 1",1),
	SUBSYSTEM2(127,"Sub system slot 2",2),
	SUBSYSTEM3(128,"Sub system slot 3",3),
	SUBSYSTEM4(129,"Sub system slot 4",4),
	SUBSYSTEM5(130,"Sub system slot 5",5),
	SUBSYSTEM6(131,"Sub system slot 6",6),
	SUBSYSTEM7(132,"Sub system slot 7",7);

	private String displayName;
	private int id;
	private final int slotNumber;

	private EveFlags(int id,String displayName) {
		this.displayName = displayName;
		this.id = id;
		this.slotNumber = -1;
	}
	
	private EveFlags(int id,String displayName,int slotNumber) {
		this.displayName = displayName;
		this.id = id;
		this.slotNumber = slotNumber;
	}
	
	public int getId() {
		return id;
	}
	
	public SlotType getSlotType() {
		
		if ( isRigSlot() ) {
			return SlotType.RIG;
		} else if ( isHighSlot() ) {
			return SlotType.HIGH;
		} else if ( isMedSlot() ) {
			return SlotType.MEDIUM;
		} else if ( isLowSlot() ) {
			return SlotType.LOW;
		} else if ( isSubsystemSlot() ) {
			return SlotType.SUBSYSTEM;
		} 
			
		return SlotType.NONE;
	}
	
	public boolean hasSlotType(SlotType type) {
		return getSlotType() == type;
	}

	public boolean isShipSlot() {
		return isRigSlot() || isLowSlot() || isMedSlot() || isHighSlot() || isSubsystemSlot();
	}
	
	public boolean isSubsystemSlot() {
		switch(this) {
		case SUBSYSTEM0:
		case SUBSYSTEM1:
		case SUBSYSTEM2:
		case SUBSYSTEM3:
		case SUBSYSTEM4:
		case SUBSYSTEM5:
		case SUBSYSTEM6:
		case SUBSYSTEM7:
			return true;
		}
		return false;
	}
	
	public boolean isRigSlot() {
		switch( this ) {
		case RIGSLOT0:
		case RIGSLOT1:
		case RIGSLOT2:
		case RIGSLOT3:
		case RIGSLOT4:
		case RIGSLOT5:
		case RIGSLOT6:
		case RIGSLOT7:			
			return true;
		}
		return false;
	}
	
	public boolean isLowSlot() {
		switch( this ) {
		case LOSLOT0:
		case LOSLOT1:
		case LOSLOT2:
		case LOSLOT3:
		case LOSLOT4:
		case LOSLOT5:
		case LOSLOT6:
		case LOSLOT7:
			return true;
		}
		return false;
	}

	public boolean isMedSlot() {
		switch( this ) {
		case MEDSLOT0:
		case MEDSLOT1:
		case MEDSLOT2:
		case MEDSLOT3:
		case MEDSLOT4:
		case MEDSLOT5:
		case MEDSLOT6:
		case MEDSLOT7:
			return true;
		}
		return false;
	}

	public boolean isHighSlot() {
		switch( this ) {
		case HISLOT0:
		case HISLOT1:
		case HISLOT2:
		case HISLOT3:
		case HISLOT4:
		case HISLOT5:
		case HISLOT6:
		case HISLOT7:
			return true;
		}
		return false;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public String toString() {
		return displayName;
	}

	public int getShipSlotNumber() {
		if ( ! isShipSlot() ) {
			throw new UnsupportedOperationException("getShipSlotNumber() invoked on non ship-slot flag "+this);
		}

		return slotNumber; 
	}

	private static final String FLAGS = "0,None,None                                                                                                 \n" + 
			"1,Wallet,Wallet                                                                                             \n" + 
			"2,Factory,Factory                                                                                           \n" + 
			"4,Hangar,Hangar                                                                                             \n" + 
			"5,Cargo,Cargo                                                                                               \n" + 
			"6,Briefcase,Briefcase                                                                                       \n" + 
			"7,Skill,Skill                                                                                               \n" + 
			"8,Reward,Reward                                                                                             \n" + 
			"9,Connected,Character in station connected                                                                  \n" + 
			"10,Disconnected,Character in station offline                                                                \n" + 
			"11,LoSlot0,Low power slot 1                                                                                 \n" + 
			"12,LoSlot1,Low power slot 2                                                                                 \n" + 
			"13,LoSlot2,Low power slot 3                                                                                 \n" + 
			"14,LoSlot3,Low power slot 4                                                                                 \n" + 
			"15,LoSlot4,Low power slot 5                                                                                 \n" + 
			"16,LoSlot5,Low power slot 6                                                                                 \n" + 
			"17,LoSlot6,Low power slot 7                                                                                 \n" + 
			"18,LoSlot7,Low power slot 8                                                                                 \n" + 
			"19,MedSlot0,Medium power slot 1                                                                             \n" + 
			"20,MedSlot1,Medium power slot 2                                                                             \n" + 
			"21,MedSlot2,Medium power slot 3                                                                             \n" + 
			"22,MedSlot3,Medium power slot 4                                                                             \n" + 
			"23,MedSlot4,Medium power slot 5                                                                             \n" + 
			"24,MedSlot5,Medium power slot 6                                                                             \n" + 
			"25,MedSlot6,Medium power slot 7                                                                             \n" + 
			"26,MedSlot7,Medium power slot 8                                                                             \n" + 
			"27,HiSlot0,High power slot 1                                                                                \n" + 
			"28,HiSlot1,High power slot 2                                                                                \n" + 
			"29,HiSlot2,High power slot 3                                                                                \n" + 
			"30,HiSlot3,High power slot 4                                                                                \n" + 
			"31,HiSlot4,High power slot 5                                                                                \n" + 
			"32,HiSlot5,High power slot 6                                                                                \n" + 
			"33,HiSlot6,High power slot 7                                                                                \n" + 
			"34,HiSlot7,High power slot 8                                                                                \n" + 
			"35,Fixed Slot,Fixed Slot                                                                                    \n" + 
			"40,PromenadeSlot1,Promenade Slot 1                                                                          \n" + 
			"41,PromenadeSlot2,Promenade Slot 2                                                                          \n" + 
			"42,PromenadeSlot3,Promenade Slot 3                                                                          \n" + 
			"43,PromenadeSlot4,Promenade Slot 4                                                                          \n" + 
			"44,PromenadeSlot5,Promenade Slot 5                                                                          \n" + 
			"45,PromenadeSlot6,Promenade Slot 6                                                                          \n" + 
			"46,PromenadeSlot7,Promenade Slot 7                                                                          \n" + 
			"47,PromenadeSlot8,Promenade Slot 8                                                                          \n" + 
			"48,PromenadeSlot9,Promenade Slot 9                                                                          \n" + 
			"49,PromenadeSlot10,Promenade Slot 10                                                                        \n" + 
			"50,PromenadeSlot11,Promenade Slot 11\n" + 
			"51,PromenadeSlot12,Promenade Slot 12\n" + 
			"52,PromenadeSlot13,Promenade Slot 13\n" + 
			"53,PromenadeSlot14,Promenade Slot 14\n" + 
			"54,PromenadeSlot15,Promenade Slot 15\n" + 
			"55,PromenadeSlot16,Promenade Slot 16\n" + 
			"56,Capsule,Capsule\n" + 
			"57,Pilot,Pilot\n" + 
			"58,Passenger,Passenger\n" + 
			"59,Boarding Gate,Boarding gate\n" + 
			"60,Crew,Crew\n" + 
			"61,Skill In Training,Skill in training\n" + 
			"62,CorpMarket,Corporation Market Deliveries / Returns\n" + 
			"63,Locked,Locked item, can not be moved unless unlocked\n" + 
			"64,Unlocked,Unlocked item, can be moved\n" + 
			"70,Office Slot 1,Office slot 1\n" + 
			"71,Office Slot 2,Office slot 2\n" + 
			"72,Office Slot 3,Office slot 3\n" + 
			"73,Office Slot 4,Office slot 4\n" + 
			"74,Office Slot 5,Office slot 5\n" + 
			"75,Office Slot 6,Office slot 6\n" + 
			"76,Office Slot 7,Office slot 7\n" + 
			"77,Office Slot 8,Office slot 8\n" + 
			"78,Office Slot 9,Office slot 9\n" + 
			"79,Office Slot 10,Office slot 10\n" + 
			"80,Office Slot 11,Office slot 11\n" + 
			"81,Office Slot 12,Office slot 12\n" + 
			"82,Office Slot 13,Office slot 13\n" + 
			"83,Office Slot 14,Office slot 14\n" + 
			"84,Office Slot 15,Office slot 15\n" + 
			"85,Office Slot 16,Office slot 16\n" + 
			"86,Bonus,Bonus\n" + 
			"87,DroneBay,Drone Bay\n" + 
			"88,Booster,Booster\n" + 
			"89,Implant,Implant\n" + 
			"90,ShipHangar,Ship Hangar\n" + 
			"91,ShipOffline,Ship Offline\n" + 
			"92,RigSlot0,Rig power slot 1\n" + 
			"93,RigSlot1,Rig power slot 2\n" + 
			"94,RigSlot2,Rig power slot 3\n" + 
			"95,RigSlot3,Rig power slot 4\n" + 
			"96,RigSlot4,Rig power slot 5\n" + 
			"97,RigSlot5,Rig power slot 6\n" + 
			"98,RigSlot6,Rig power slot 7\n" + 
			"99,RigSlot7,Rig power slot 8\n" + 
			"100,Factory Operation,Factory Background Operation\n" + 
			"116,CorpSAG2,Corp Security Access Group 2\n" + 
			"117,CorpSAG3,Corp Security Access Group 3\n" + 
			"118,CorpSAG4,Corp Security Access Group 4\n" + 
			"119,CorpSAG5,Corp Security Access Group 5\n" + 
			"120,CorpSAG6,Corp Security Access Group 6\n" + 
			"121,CorpSAG7,Corp Security Access Group 7\n" + 
			"122,SecondaryStorage,Secondary Storage\n" + 
			"123,CaptainsQuarters,Captains Quarters\n" + 
			"124,Wis Promenade,Wis Promenade\n" + 
			"125,SubSystem0,Sub system slot 0\n" + 
			"126,SubSystem1,Sub system slot 1\n" + 
			"127,SubSystem2,Sub system slot 2\n" + 
			"128,SubSystem3,Sub system slot 3\n" + 
			"129,SubSystem4,Sub system slot 4\n" + 
			"130,SubSystem5,Sub system slot 5\n" + 
			"131,SubSystem6,Sub system slot 6\n" + 
			"132,SubSystem7,Sub system slot 7\n";
	
	public static void main(String[] args) {
	
		final Pattern PATTERN =
			Pattern.compile( "^([0-9]+),(.*?),(.*)$");
		
		for ( String line : FLAGS.split("\n" ) ) {
			final String trimmed = StringUtils.strip( line );
			
			final Matcher m = PATTERN.matcher( line );
			if ( ! m.matches() ) {
				throw new RuntimeException();
			}
			
			final int id = Integer.parseInt( m.group(1) );
			final String name = m.group(2).toUpperCase().replaceAll(" ", "_");
			final String displayName = StringUtils.strip( m.group(3) );
			System.out.println( name+"("+id+",\""+displayName+"\")," );
		}
	}

	public static EveFlags fromTypeId(int id) {
		for ( EveFlags f : values() ) {
			if ( f.id == id ) {
				return f;
			}
		}
		throw new IllegalArgumentException("Unknown Eve flag "+id);
	} 
	

}
