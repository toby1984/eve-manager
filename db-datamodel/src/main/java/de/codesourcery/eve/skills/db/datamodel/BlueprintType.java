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
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="invBlueprintTypes")
// @org.hibernate.annotations.Proxy(lazy=false)
public class BlueprintType {

	/*
	+--------------------------+-------------+------+-----+---------+-------+
| Field                    | Type        | Null | Key | Default | Extra |
+--------------------------+-------------+------+-----+---------+-------+
| blueprintTypeID          | smallint(6) | NO   | PRI | NULL    |       |
| parentBlueprintTypeID    | smallint(6) | YES  | MUL | NULL    |       |
| productTypeID            | smallint(6) | YES  | MUL | NULL    |       |
| productionTime           | int(11)     | YES  |     | NULL    |       |
| techLevel                | smallint(6) | YES  |     | NULL    |       |
| researchProductivityTime | int(11)     | YES  |     | NULL    |       |
| researchMaterialTime     | int(11)     | YES  |     | NULL    |       |
| researchCopyTime         | int(11)     | YES  |     | NULL    |       |
| researchTechTime         | int(11)     | YES  |     | NULL    |       |
| productivityModifier     | int(11)     | YES  |     | NULL    |       |
| materialModifier         | smallint(6) | YES  |     | NULL    |       |
| wasteFactor              | smallint(6) | YES  |     | NULL    |       |
| maxProductionLimit       | int(11)     | YES  |     | NULL    |       |
+--------------------------+-------------+------+-----+---------+-------+
	 */
	
	@Id
	@Column(name="blueprintTypeID")
	private Long blueprintType;
	
	@OneToOne
	@JoinColumn(name="blueprintTypeID")
	public InventoryType type;

	@ManyToOne
	@JoinColumn(name="parentBlueprintTypeID",nullable=true,updatable=false)
	private BlueprintType parentBlueprint;

	@ManyToOne
	@JoinColumn(name="productTypeID", nullable=false)
//	@PrimaryKeyJoinColumn(name="productTypeID")
	private InventoryType productType;

	@Column(name="productionTime")
	private int productionTime;

	@Column(name="techLevel")
	private int techLevel;

	@Column(name="researchProductivityTime")
	private int researchProductivityTime;

	@Column(name="researchMaterialTime")
	private int researchMaterialTime;

	@Column(name="researchCopyTime")
	private int researchCopyTime;

	@Column(name="researchTechTime")
	private int researchTechTime;

	@Column(name="productivityModifier")
	private int productivityModifier;

	@Column(name="materialModifier")
	private int materialModifier;

	@Column(name="wasteFactor")
	private int wasteFactor;

	@Column(name="maxProductionLimit")
	private int maxProductionLimit;
	
	public BlueprintType getParentBlueprint() {
		return parentBlueprint;
	}

	public void setParentBlueprint(BlueprintType parentBlueprint) {
		this.parentBlueprint = parentBlueprint;
	}

	public InventoryType getProductType() {
		return productType;
	}

	public void setProductType(InventoryType productType) {
		this.productType = productType;
	}

	public int getProductionTime() {
		return productionTime;
	}

	public void setProductionTime(int productionTime) {
		this.productionTime = productionTime;
	}

	public int getNumberOfProducedItemsPerRun() {
		return getProductType().getPortionSize();
	}
	
	public int getTechLevel() {
		return techLevel;
	}

	public void setTechLevel(int techLevel) {
		this.techLevel = techLevel;
	}

	public int getResearchProductivityTime() {
		return researchProductivityTime;
	}

	public void setResearchProductivityTime(int researchProductivityTime) {
		this.researchProductivityTime = researchProductivityTime;
	}

	public int getResearchMaterialTime() {
		return researchMaterialTime;
	}

	public void setResearchMaterialTime(int researchMaterialTime) {
		this.researchMaterialTime = researchMaterialTime;
	}

	public int getResearchCopyTime() {
		return researchCopyTime;
	}

	public void setResearchCopyTime(int researchCopyTime) {
		this.researchCopyTime = researchCopyTime;
	}

	public int getResearchTechTime() {
		return researchTechTime;
	}

	public void setResearchTechTime(int researchTechTime) {
		this.researchTechTime = researchTechTime;
	}

	public int getProductivityModifier() {
		return productivityModifier;
	}

	public void setProductivityModifier(int productivityModifier) {
		this.productivityModifier = productivityModifier;
	}

	public int getMaterialModifier() {
		return materialModifier;
	}

	public void setMaterialModifier(int materialModifier) {
		this.materialModifier = materialModifier;
	}

	public int getWasteFactor() {
		return wasteFactor;
	}

	public void setWasteFactor(int wasteFactor) {
		this.wasteFactor = wasteFactor;
	}

	public int getMaxProductionLimit() {
		return maxProductionLimit;
	}

	public void setMaxProductionLimit(int maxProductionLimit) {
		this.maxProductionLimit = maxProductionLimit;
	}

	public InventoryType getBlueprintType() {
		return type;
	}

}
