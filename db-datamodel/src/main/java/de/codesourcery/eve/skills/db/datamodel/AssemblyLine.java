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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import de.codesourcery.eve.skills.datamodel.Standing;
import de.codesourcery.eve.skills.utils.ISKAmount;

@Entity
@Table(name = "ramAssemblyLines")
// @org.hibernate.annotations.Proxy(lazy=false)
public class AssemblyLine
{

	/*
	 * mysql> desc ramAssemblyLines;

	 */

	@Id
	@Column(name = "assemblyLineID")
	private Long id;

	@OneToOne
	@JoinColumn(name = "assemblyLineTypeID")
	private AssemblyLineType type;

	@OneToOne
	@JoinColumn(name = "containerID")
	private Station station;

	@Column(name = "costInstall")
	private double installationCost;

	@Column(name = "costPerHour")
	private double costPerHour;

	@Column(name = "discountPerGoodStandingPoint")
	private double goodStandingDiscount;

	@Column(name = "surchargePerBadStandingPoint")
	private double badStandingSurcharge;

	@Column(name = "minimumStanding")
	private double minimumStanding;

	@org.hibernate.annotations.Type(
			type = "de.codesourcery.eve.skills.db.dao.ActivityUserType"
	)
	@Column(name = "activityID",nullable=false)
	private Activity activity;

	@OneToOne
	@JoinColumn(name = "ownerID")
	private NPCCorporation owner;

	public Long getId()
	{
		return id;
	}

	public AssemblyLineType getType()
	{
		return type;
	}

	public Station getStation()
	{
		return station;
	}

	public ISKAmount getInstallationCost()
	{
		return new ISKAmount( installationCost );
	}

	public ISKAmount getCostPerHour()
	{
		return new ISKAmount( costPerHour );
	}
	
	/**
	 * 
	 * @param standing
	 * @return a positive fraction (0..100) if the standing
	 * is positive towards the owning corp, otherwise 0.0 or a
	 * negative fraction (0... -100)
	 */
	public double getDiscountPercent(Standing<NPCCorporation> standing) {
		
		if ( standing == null ) {
			return 0.0;
		}

		final double val = standing.getValue();
		


		if ( val >= getMinimumStanding() ) {
			final double delta = Math.abs( 
					Math.max( 0 , val ) ) - Math.max( getMinimumStanding() , 0 );  
			return delta * getGoodStandingDiscount();
		}
		final double delta = Math.abs( 
				Math.max( getMinimumStanding() , 0 ) - Math.max( getMinimumStanding() , val ) );  
		return -delta * getBadStandingSurcharge();
	}
	
	public ISKAmount getEffectiveCostPerHour(Standing<NPCCorporation> standing) {
		if ( standing == null ) {
			throw new IllegalArgumentException("standing cannot be NULL");
		}
		
		if ( ! this.owner.getId().equals( standing.getFrom().getId() ) ) {
			throw new IllegalArgumentException("Wrong corp passed ?");
		}
		
		final ISKAmount discountAmount =
			new ISKAmount( getCostPerHour().toDouble() * getDiscountPercent( standing ) );
		
		return getCostPerHour().addTo( discountAmount );
	}
	
	public ISKAmount getEffectiveInstallationCosts(Standing<NPCCorporation> standing) 
	{
		if ( standing == null ) {
			throw new IllegalArgumentException("standing cannot be NULL");
		}
		
		if ( ! this.owner.getId().equals( standing.getFrom().getId() ) ) {
			throw new IllegalArgumentException("Wrong corp passed ?");
		}
		
		final ISKAmount discountAmount =
			new ISKAmount( getInstallationCost().toDouble() * getDiscountPercent( standing ) );
		
		return getInstallationCost().addTo( discountAmount );
	}

	public double getGoodStandingDiscount()
	{
		return goodStandingDiscount;
	}

	public double getBadStandingSurcharge()
	{
		return badStandingSurcharge;
	}

	public double getMinimumStanding()
	{
		return minimumStanding;
	}

	public Activity getActivity()
	{
		return activity;
	}

	public NPCCorporation getOwner()
	{
		return owner;
	}

}
