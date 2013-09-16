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
package de.codesourcery.eve.skills.ui.components.impl;

import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.codesourcery.eve.skills.datamodel.Blueprint;
import de.codesourcery.eve.skills.datamodel.CharacterID;
import de.codesourcery.eve.skills.datamodel.Decryptor;
import de.codesourcery.eve.skills.datamodel.ICharacter;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.datamodel.ManufacturingJobRequest;
import de.codesourcery.eve.skills.datamodel.Prerequisite;
import de.codesourcery.eve.skills.datamodel.RequiredMaterial;
import de.codesourcery.eve.skills.datamodel.Requirements;
import de.codesourcery.eve.skills.datamodel.SlotAttributes;
import de.codesourcery.eve.skills.datamodel.TrainedSkill;
import de.codesourcery.eve.skills.db.dao.IStaticDataModelProvider;
import de.codesourcery.eve.skills.db.datamodel.Activity;
import de.codesourcery.eve.skills.db.datamodel.Skill;
import de.codesourcery.eve.skills.production.BlueprintWithAttributes;
import de.codesourcery.eve.skills.production.IBlueprintLibrary;
import de.codesourcery.eve.skills.production.InventionChanceCalculator;
import de.codesourcery.eve.skills.ui.components.AbstractComponent;
import de.codesourcery.eve.skills.ui.components.ComponentWrapper;
import de.codesourcery.eve.skills.ui.components.ICharacterSelectionProviderAware;
import de.codesourcery.eve.skills.ui.components.IDoubleClickSelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionListener;
import de.codesourcery.eve.skills.ui.components.ISelectionProvider;
import de.codesourcery.eve.skills.ui.components.impl.planning.CreateProductionTemplateComponent;
import de.codesourcery.eve.skills.ui.frames.WindowManager;
import de.codesourcery.eve.skills.ui.frames.WindowManager.IWindowProvider;
import de.codesourcery.eve.skills.ui.utils.ImprovedSplitPane;
import de.codesourcery.eve.skills.ui.utils.PopupMenuBuilder;
import de.codesourcery.eve.skills.util.AmountHelper;
import de.codesourcery.eve.skills.utils.DateHelper;
import de.codesourcery.utils.StringTablePrinter;
import de.codesourcery.utils.StringTablePrinter.Padding;


public class BlueprintBrowserComponent extends AbstractComponent implements ICharacterSelectionProviderAware,
ActionListener , IDoubleClickSelectionListener<Blueprint> {

	public static final Logger log = Logger
	.getLogger(BlueprintBrowserComponent.class);

	private final BlueprintChooserComponent blueprintChooser = new BlueprintChooserComponent();
	private final JTextArea textArea = new JTextArea(50, 70);

	@Resource(name = "datamodel-provider")
	private IStaticDataModelProvider dataModelProvider;

	@Resource(name="blueprint-library")
	private IBlueprintLibrary blueprintLibrary;

	private JRadioButton posButton = new JRadioButton("POS");
	private JRadioButton npcStationButton = new JRadioButton("NPC station",true);

	private JTextField selectedME = new JTextField("0");
	private JTextField selectedPE = new JTextField("0");

	private JTextField quantity = new JTextField("1");
	
	private PopupMenuBuilder popupMenuBuilder = new PopupMenuBuilder();

	private ISelectionProvider<ICharacter> charProvider;
	private ISelectionListener<ICharacter> selectionListener =  new ISelectionListener<ICharacter>() 
	{
		@Override
		public void selectionChanged(ICharacter selected) {
			characterSelected(selected);
		}
	};

	protected void characterSelected(ICharacter character) {
		refresh();
	}

	@Override
	protected void onAttachHook(IComponentCallback callback) {
		blueprintChooser.setPopupMenuBuilder( popupMenuBuilder );
		blueprintChooser.onAttach( callback );
		blueprintChooser.addSelectionListener( this );
		charProvider.addSelectionListener(this.selectionListener);
		if ( isInitialized() ) {
			refresh();
		}
	}

	protected int getRequestedQuantity() {
		return Integer.parseInt( this.quantity.getText() ); 
	}

	@Override
	protected void onDetachHook() {

		blueprintChooser.removeSelectionListener( this );
		blueprintChooser.onDetach();
		blueprintChooser.removePopupMenuBuilder();
		
		if (charProvider != null) {
			charProvider.removeSelectionListener(this.selectionListener);
		}
	}

	@Override
	protected void disposeHook() {
		blueprintChooser.dispose();
	}

	@Override
	protected JPanel createPanel() {

		popupMenuBuilder.addItem( "Create production plan..." , new AbstractAction() 
		{
			@Override
			public boolean isEnabled()
			{
				return blueprintChooser.getCurrentlySelectedBlueprint() != null;
			}
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				final Blueprint blueprint = blueprintChooser.getCurrentlySelectedBlueprint();
				if ( blueprint != null ) {
					doubleClicked( blueprint );
				}
			}
		} );
		
		// add text fields
		final JPanel textFields = new JPanel();
		textFields.setLayout(new GridBagLayout());

		selectedME.setColumns( 5 );
		selectedPE.setColumns( 5 );
		quantity.setColumns( 6 );
		
		selectedME.setHorizontalAlignment( JTextField.RIGHT );
		selectedPE.setHorizontalAlignment( JTextField.RIGHT );
		quantity.setHorizontalAlignment( JTextField.RIGHT );

		selectedME.addActionListener(this);
		selectedPE.addActionListener(this);
		quantity.addActionListener( this );

		textFields.add(new JLabel("ME"), constraints(0, 0).resizeBoth()
				.end());
		textFields.add(selectedME, constraints(1, 0).weightX(0.1)
				.anchorWest().end());

		textFields.add(new JLabel("PE"), constraints(0, 1).resizeBoth()
				.end());

		textFields.add(selectedPE, constraints(1, 1).weightX(0.1)
				.anchorWest().end());

		textFields.add(new JLabel("Quantity"), constraints(0, 2).resizeBoth()
				.end());

		textFields.add(quantity, constraints(1, 2).weightX(0.1).anchorWest().end());

		// add combobox with cost calculators

		// add POS / NPC station buttons + label
		JPanel buttonPanel = new JPanel() ;

		final ButtonGroup group = new ButtonGroup();
		group.add( posButton );
		group.add( npcStationButton );

		posButton.addActionListener( this );
		npcStationButton.addActionListener( this );

		buttonPanel.add( posButton );
		buttonPanel.add( npcStationButton );

		textFields.add( new JLabel("Location" ) , constraints( 0 , 3 ).noResizing().end() );
		textFields.add( buttonPanel, constraints( 1 , 3 ).useRemainingWidth().end() );

		// add text area
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		
		setMonospacedFont(textArea);
		
		final JPanel rightPanel =
			new JPanel();
		
		rightPanel.setLayout(new GridBagLayout());
		rightPanel.add(textFields, constraints(0, 0).noResizing().end());
		rightPanel.add(new JScrollPane(textArea), constraints(0, 1).useRemainingSpace().end());
		
		final ImprovedSplitPane splitPane =
			new ImprovedSplitPane(JSplitPane.HORIZONTAL_SPLIT , 
					this.blueprintChooser.createPanel() , rightPanel );
				
		splitPane.setDividerLocation( 0.4 );
		
		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.add( splitPane , constraints(0, 0).resizeBoth().end() );

		return panel;
	}

	protected void refresh() 
	{
		final Blueprint bp = getCurrentlySelectedBlueprint();
		if (bp != null) 
		{
			displayBlueprint( toBlueprintWithAttributes( bp ) ); 
		}
	}
	
	private BlueprintWithAttributes toBlueprintWithAttributes(final Blueprint bp) 
	{
		final BlueprintWithAttributes existing = getBestBlueprintFromLibrary( bp );
		if ( existing != null ) {
			return existing;
		}
		
		int me = bp.getTechLevel() > 1 ? -4 : 0;
		int pe = bp.getTechLevel() > 1 ? -4 : 0;		
		
		final CharacterID charId = getCurrentlySelectedCharacter().getCharacterId();
		return new BlueprintWithAttributes(charId,bp.getType().getBlueprintType().getId(), me , pe , false) 
		{
			@Override
			public Blueprint getBlueprint() {
				return bp;
			}
		};
	}

	private Blueprint getCurrentlySelectedBlueprint() {
		if ( blueprintChooser != null ) {
			return blueprintChooser.getCurrentlySelectedBlueprint();
		}
		return null;		
	}
	
	protected void displayBlueprint(final BlueprintWithAttributes bpWithAttrs) {

		if ( getCurrentlySelectedCharacter() == null ) {
			textArea.setText("No character selected.");
			return;
		}

		final Blueprint bp = bpWithAttrs.getBlueprint();
		
		final StringBuilder text = new StringBuilder();
		
		final InventionChanceCalculator calculator =
			new InventionChanceCalculator( this.dataModelProvider.getStaticDataModel() );
		
		appendLine(text, "Blueprint name", bp.getName());

		appendLine(text, "Produces", bp.getProductType().getName() );

		appendLine(text);

		appendLine(text, "Tech level", "" + bp.getTechLevel());

		appendLine(text);
		
		if ( calculator.canBeUsedForInvention( bp ) ) {
			
			text.append("T2 variations:\n\n");
			
			final List<Blueprint> tech2Blueprints = 
				dataModelProvider.getStaticDataModel().getTech2Variations( bp );
			for ( Iterator<Blueprint> it = tech2Blueprints.iterator() ; it.hasNext() ; ) {
				text.append( it.next().getName() ).append("\n");
				if ( ! it.hasNext() ) {
					text.append("\n");
				}
			}
		}		

		text.append("\n---------------- Your skills ---------------\n\n");

		appendLine(text, "Industry skill", "" + getIndustrySkillLevel() );

		appendLine(text, "Production efficiency skill", "" + getProductionEfficiencySkillLevel());

		appendLine(text, "Research skill", "" + getResearchSkillLevel());

		appendLine(text, "Metallurgy skill", "" + getMetallurgySkillLevel());

		text.append("\n---------------- BPO data ---------------\n\n");

		appendLine(text, "BPO ME", "" + getBPOMaterialEfficiency());
		appendLine(text, "BPO PE", "" + getBPOProductionEfficiency());

		appendLine(text, "BPO Waste factor", "" + bp.getWasteFactor());
		appendLine(text, "BPO Productivity modifier", ""+ bp.getProductivityModifier());

		text.append("\n");
		appendLine(text, "Base price", AmountHelper.formatISKAmount( bp.getBasePrice() ) + " ISK" );
		
		text.append("\n");
		appendLine(text, "Copy time", calcCopyTime( bp ) );

		text.append("\n---------------- Production times---------------\n\n");

		appendLine(text, "Production time (base)", "" + calcProductionTime(bp, 0));

		appendLine(text, "Production time", "" + calcProductionTime(bp, getBPOProductionEfficiency()));

		text.append("\n---------------- Research times---------------\n\n");

		String researchDuration;
		if ( bp.getTechLevel() > 1 ) {
			researchDuration = "<Blueprint copy, cannot research>";
		} else if ( bpWithAttrs.getMeLevel() != getBPOMaterialEfficiency() ) {
			researchDuration = ""+calcMEResearchTime(bp, bpWithAttrs.getMeLevel() , getBPOMaterialEfficiency() );
		} else {
			researchDuration = "<no research necessary>";
		}

		appendLine(text, "ME research lvl "+bpWithAttrs.getMeLevel()+" => "+getBPOMaterialEfficiency(), "" + researchDuration );

		if ( bp.getTechLevel() > 1 ) {
			researchDuration = "<Blueprint copy, cannot research>";
		} else if ( bpWithAttrs.getPeLevel() != getBPOProductionEfficiency() ) {
			researchDuration = ""+calcMEResearchTime(bp, bpWithAttrs.getPeLevel() , getBPOProductionEfficiency() );
		} else {
			researchDuration = "<no research necessary>";
		}
		
		appendLine(text, "PE research lvl "+bpWithAttrs.getPeLevel()+" => "+ getBPOProductionEfficiency(), "" + researchDuration );

		renderRequirements(Activity.MANUFACTURING , bp, text);

		if ( calculator.canBeUsedForInvention( bp ) ) {
			
			renderRequirements(Activity.INVENTION, bp, text);

			appendLine(text, "Base invention chance", ""+( bp.getBaseInventionChance() * 100.0f)+" %" );

			List<Skill> dcSkills = new ArrayList<Skill>();
			String error="";
			try {
				dcSkills = calculator.getInventionDatacoreSkills( bp );

			} catch(Exception e) {
				error = "Failed to determine invention skills for "+
				bp.getName()+" (ID "+bp.getType().getBlueprintType().getId()+ ") - error: "+e.getMessage();
				
				log.error("displayBlueprint(): Failed to determine invention skills for "+
						bp.getName()+" (ID "+bp.getType().getBlueprintType().getId()+ ")" ,e );
			}

			if ( ! dcSkills.isEmpty() ) 
			{
				
				final Skill skill1 = dcSkills.get(0);
				final Skill skill2 = dcSkills.get(1);
				
				appendLine(text, "Datacore skill ("+skill1.getName()+" )", getSkillLevel( skill1 )+" (you)" );
				appendLine(text, "Datacore skill ("+skill2.getName()+" )", getSkillLevel( skill2 )+" (you)");
				
				final Skill racialSkill = calculator.getRacialSkill( bp );
				
				appendLine(text, "Racial skill ( "+racialSkill.getName()+") ", 
						getSkillLevel( racialSkill)+" (you)");

				final float inventionChance =
					calculator.calculateInventionChance(
							bp , 
							getCurrentlySelectedCharacter() , 
							0, // TODO: item meta-level => make configurable
							Decryptor.NONE // TODO: Let user choose decryptor to use 
					);

				final DecimalFormat DF =
					new DecimalFormat("##0.000");

				appendLine(text, "Your invention chance (no decryptors/no meta-items)", ""+DF.format( 100.0f* inventionChance)+" %" );
			} else {
				text.append("\nYour invention chance: <internal error: "+error+">");
			}
		} else if ( bp.getTechLevel() == 1 ) {
			text.append("\nInvention: No T2 blueprint(s) found");
		}

		textArea.setText(text.toString());
		textArea.setCaretPosition(0);
	}

	protected int getSkillLevel(Skill s) {
		return getCurrentlySelectedCharacter().getSkillLevel( s ).getLevel();
	}

	private void renderRequirements(final Activity activity , final Blueprint bp,final StringBuilder text) 
	{

		final Requirements reqs = bp.getRequirementsFor( activity );

		text.append("\n---------------- Materials [ "+activity.getName()+" ] ---------------\n\n");

		final List<RequiredMaterial> sortedMats = new ArrayList<>( reqs.getRequiredMaterials() );
		Collections.sort( sortedMats, new Comparator<RequiredMaterial>() {

			@Override
			public int compare(RequiredMaterial o1, RequiredMaterial o2) 
			{
				if ( o1.isSubjectToManufacturingWaste() && ! o2.isSubjectToManufacturingWaste() ) {
					return -1;
				} 
				if ( ! o1.isSubjectToManufacturingWaste() && o2.isSubjectToManufacturingWaste() ) {
					return 1;
				}
				return o1.getType().getName().compareTo( o2.getType().getName() );
			}
		} );
		
		final StringTablePrinter tablePrinter = new StringTablePrinter("Item" , "Initial" , "You" , "Waste %" , "dmg % / run" );
		
		final DecimalFormat FORMAT = new DecimalFormat("##0.00");
		
		for (RequiredMaterial mat : sortedMats ) {

			final float initial = mat.getQuantity()*getRequestedQuantity();
			final float you = calcRequiredMaterial(bp, mat);

			float waste = 100.0f * ( you / initial) - 100.0f;
			if (waste < 0.0f) {
				waste = 0.0f;
			}
			
			final String sInitial = Float.toString( initial );			
			final String sWaste = FORMAT.format(waste);
			final String sYou = Float.toString( you );

			if ( activity == Activity.MANUFACTURING ) 
			{
				String sDmg = "";
				if ( mat.getDamagePerJob() != 1.0d && mat.getDamagePerJob() > 0.0d ) {
					sDmg = Double.toString( mat.getDamagePerJob()*100.0 );
				} 
				
				String wasteString = "";
				if ( mat.isSubjectToStationWaste() ) {
					wasteString += "(1)";
				}
				if ( mat.isSubjectToSkillWaste() ) {
					wasteString += "(2)";
				}
				if ( mat.isSubjectToBPMWaste() ) {
					wasteString += "(3)";
				}
				
				if ( ! "".equals( wasteString ) ) 
				{
					tablePrinter.add( mat.getType().getName() + " " + wasteString , sInitial , sYou , sWaste , sDmg );
				} 
				else 
				{
					tablePrinter.add( mat.getType().getName()  , sInitial , sYou , sWaste , sDmg );	
				}
			} 
			else {
				tablePrinter.add( mat.getType().getName()  , sInitial , "--" , "--" , "--" );	
			}
		}

		tablePrinter.setPaddingForAllColumns(Padding.LEFT).padRight(0);
		text.append( tablePrinter.toString() );

		if ( activity == Activity.MANUFACTURING ) {
			text.append("\n(1) Material is subject to station waste.");
			text.append("\n(2) Material is subject to character PE level waste.");
			text.append("\n(3) Material is subject to blueprint ME level waste.\n");
		}

		final List<Prerequisite> requiredSkills = reqs.getRequiredSkills();

		text.append("\n---------------- Skills [ "+activity.getName()+" ] ---------------\n\n");

		for (Prerequisite r : requiredSkills) {

			final String hasSkill = meetsRequirement(r);
			appendLine(text, r.getSkill().getName(), "" + r.getRequiredLevel()
					+ hasSkill);
		}
	}
	
	private ManufacturingJobRequest createManufacturingJobRequest(Blueprint blueprint,
			ICharacter character,
			int bpoME,
			int bpoPE,
			int numberOfRuns,
			SlotAttributes slot) 
	{
		final ManufacturingJobRequest job =  new ManufacturingJobRequest( blueprint );

		job.setCharacter( character );
		job.setMaterialEfficiency( bpoME );
		job.setProductionEfficiency( bpoPE );
		job.setQuantity( getRequestedQuantity() );
		job.setSlotAttributes( slot );
		return job;
	}

	private int getMetallurgySkillLevel() {
		final ICharacter c = getCurrentlySelectedCharacter();

		if (c == null) {
			return 0;
		}

		final Skill metallury = Skill.getMetallurgySkill( getDataModel().getSkillTree() );

		if (!c.hasSkill(metallury)) {
			return 0;
		}
		return c.getSkillLevel(metallury).getLevel();
	}

	private int getScienceSkillLevel() {
		final ICharacter c = getCurrentlySelectedCharacter();

		if (c == null) {
			return 0;
		}

		final Skill science = Skill.getScienceSkill( getDataModel().getSkillTree() );

		if (!c.hasSkill(science)) {
			return 0;
		}
		return c.getSkillLevel(science).getLevel();
	}	

	private int getResearchSkillLevel() {
		final ICharacter c = getCurrentlySelectedCharacter();

		if (c == null) {
			return 0;
		}

		final Skill research = Skill.getResearchSkill( getDataModel().getSkillTree() );

		if (!c.hasSkill(research)) {
			return 0;
		}
		return c.getSkillLevel(research).getLevel();		
	}

	private int getIndustrySkillLevel() {

		final ICharacter c = getCurrentlySelectedCharacter();

		if (c == null) {
			return 0;
		}

		final Skill industry = Skill.getIndustrySkill(getDataModel()
				.getSkillTree());

		if (!c.hasSkill(industry)) {
			return 0;
		}
		return c.getSkillLevel(industry).getLevel();
	}

	private String calcProductionTime(Blueprint bp, int PE) {

		long time = getRequestedQuantity() * bp.calculateProductionTime(PE, getIndustrySkillLevel(),
				1.0f, // implant modifier,
				getSlotLocation() );

		return DateHelper.durationToString(time * 1000);
	}

	private String calcCopyTime(Blueprint bp) {

		/*
		 Copy Time ={Blueprint Base Copy Time} * ( 1 - (0.05 * {Science Skill Level} ) * {Copy Slot Modifier} * {Implant Modifier}
		 */
		// TODO: Implant modifier is not honored here
		final long time = bp.calculateCopyTime( getScienceSkillLevel() , getSlotLocation() , 1.0f );
		return DateHelper.durationToString(time * 1000 );
	}

	private String calcMEResearchTime(Blueprint bp, int fromME,int toME) {

		if ( fromME > toME ) {
			throw new IllegalArgumentException("fromME must be <= toME");
		}
		// TODO: Implant modifier is not honored here (always set to 1.0f) !!!
		final long time1 = bp.calculateMEResearchTime( getMetallurgySkillLevel(), getSlotLocation() , 1.0f )*fromME;
		final long time2 = bp.calculateMEResearchTime( getMetallurgySkillLevel(), getSlotLocation() , 1.0f )*toME;
		final long delta = time2-time1;
		return DateHelper.durationToString( delta * 1000);
	}

	private String calcPEResearchTime(Blueprint bp, int fromPE,int toPE) 
	{
		if ( fromPE > toPE ) {
			throw new IllegalArgumentException("fromPE must be <= toPE");
		}
		// TODO: Implant modifier is not honored here (always set to 1.0f) !!!
		final long time1 = bp.calculatePEResearchTime( getMetallurgySkillLevel(), getSlotLocation() , 1.0f )*fromPE;
		final long time2 = bp.calculatePEResearchTime( getMetallurgySkillLevel(), getSlotLocation() , 1.0f )*toPE;
		final long delta = time2-time1;
		return DateHelper.durationToString( delta * 1000);		
	}

	protected SlotAttributes getSlotLocation() {
		if ( posButton.isSelected() ) {
			return SlotAttributes.HIGHSEC_POS;
		}
		return SlotAttributes.HIGHSEC_NPC_STATION;
	}

	private String meetsRequirement(Prerequisite skillRequirement) {

		String result = " <no character selected>";

		final ICharacter currentChar = getCurrentlySelectedCharacter();

		if (currentChar == null) {
			return result;
		}

		if (!currentChar.hasSkill(skillRequirement.getSkill())) {
			return " [ NOT TRAINED ]";
		}

		final TrainedSkill trained = currentChar.getSkillLevel(skillRequirement
				.getSkill());

		if (trained.getLevel() < skillRequirement.getRequiredLevel()) {
			return " [ current level: " + trained.getLevel() + " ]";
		}
		return "";
	}

	protected static void appendLine(StringBuilder builder) {
		builder.append("\n");
	}

	protected static void appendSeparator(StringBuilder builder) {
		builder.append("--------------------------\n");
	}

	protected static void appendLine(StringBuilder builder, String col1,
			String col2) 
	{
		appendLine( builder , 25 , col1 , col2 );
	}

	protected static void appendLine(StringBuilder builder, int len , String col1,String col2) 
	{
		builder.append(StringUtils.rightPad(col1, len , ' ')).append(" : ").append(col2).append("\n");
	}

	protected IStaticDataModel getDataModel() {
		return this.dataModelProvider.getStaticDataModel();
	}

	public void setSelectionProvider(
			ISelectionProvider<ICharacter> selectionProvider) {
		this.charProvider = selectionProvider;
		this.blueprintChooser.setCharacterProvider( selectionProvider );
	}

	protected ICharacter getCurrentlySelectedCharacter() {
		if (this.charProvider == null) {
			return null;
		}

		return charProvider.getSelectedItem();
	}

	private int getProductionEfficiencySkillLevel() {

		final ICharacter character =
			getCurrentlySelectedCharacter();

		if ( character == null ) {
			return 0;
		}

		final Skill PE = Skill.getProductionEfficiencySkill(getDataModel()
				.getSkillTree());

		if (!character.hasSkill(PE)) {
			return 0;
		}
		return character.getCurrentLevel(PE);
	}

	private float calcRequiredMaterial(Blueprint bp, RequiredMaterial material) 
	{
		final ManufacturingJobRequest jobRequest = createManufacturingJobRequest(
				bp  ,
				getCurrentlySelectedCharacter(),
				getBPOMaterialEfficiency(),
				getBPOProductionEfficiency(),
				getRequestedQuantity(), getSlotLocation() 
		);
		
		final Skill peSkill = Skill.getProductionEfficiencySkill( dataModelProvider.getStaticDataModel().getSkillTree() );
		return material.calcRequiredMaterial( jobRequest , peSkill , false );
	}

	private int getBPOMaterialEfficiency() 
	{
		return getInteger( selectedME );		
	}

	private int getBPOProductionEfficiency() 
	{
		return getInteger( selectedPE );
	}
	
	private int getInteger(JTextField tf) 
	{
		final String val = tf.getText();
		try {
			if ( StringUtils.isNotBlank(val) ) {
				return Integer.parseInt(val.trim());
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	protected static boolean isNumeric(String s) 
	{
		if ( StringUtils.isBlank( s ) ) {
			return false;
		}

		try {
			Integer.parseInt( s );
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		final Object src = e.getSource();

		if ( src == quantity ) {
			final String q = quantity.getText();
			if ( ! isNumeric( q ) || Integer.parseInt( q ) < 1 ) {
				quantity.setText("1");
			} 
			// adjust quantity so that numberOfRuns = ( quantity / blueprint.portionSize) is an even number
			final double currentQuantity = Integer.parseInt( q );
			final int portionSize = getCurrentlySelectedBlueprint().getPortionSize() ;
			
			final int adjustedQuantity =
				(int) ( Math.ceil( currentQuantity / portionSize ) * portionSize );
			
			if ( adjustedQuantity != currentQuantity ) {
				quantity.setText( Integer.toString( adjustedQuantity ) );
			}
			
			refresh();
		} else if ( src == selectedME) {
			final String me = selectedME.getText();
			if ( ! isNumeric(me) ) {
				selectedME.setText("0");
			}
			refresh();
		} else if ( src == selectedPE) {
			final String pe = selectedPE.getText();
			if ( ! isNumeric(pe) ) {
				selectedPE.setText("0");
			}
			refresh();
		} else if ( src == posButton || src == npcStationButton ) {
			refresh();
		}
	}
	
	private BlueprintWithAttributes getBestBlueprintFromLibrary(Blueprint bp) 
	{
		final ICharacter character =
			getCurrentlySelectedCharacter();
		if ( character == null ) {
			return null;
		}

		@SuppressWarnings("unchecked")
		final List<BlueprintWithAttributes> existingBlueprints = 
			(List<BlueprintWithAttributes>) blueprintLibrary.getBlueprints( character , bp );
		
		if ( existingBlueprints.isEmpty() ) {
			return null;
		}
		
		BlueprintWithAttributes best = null;
		for ( BlueprintWithAttributes blueprint : existingBlueprints ) {
			if ( best == null || blueprint.getMeLevel() > best.getMeLevel() ) {
				best = blueprint;
			}
		}
		return best;
	}

	@Override
	public void selectionChanged(Blueprint bp) 
	{
		if (bp != null) {
			
			quantity.setText( ""+bp.getPortionSize() );
			
			final BlueprintWithAttributes existing = getBestBlueprintFromLibrary( bp );

			final int me;
			final int pe;
			if ( existing == null ) 
			{
				me = bp.getTechLevel() == 2 ? -4 : 0;
				pe = bp.getTechLevel() == 2 ? -4 : 0;
			} 
			else 
			{
				me = existing.getMeLevel();
				pe = existing.getPeLevel();
			}
			
			selectedME.setText( Integer.toString( me ) );
			selectedPE.setText( Integer.toString( pe ) );
			displayBlueprint( toBlueprintWithAttributes(bp));
		}	
	}

	@Override
	public void doubleClicked(Blueprint data)
	{
		final ICharacter c = getCurrentlySelectedCharacter();
		if ( c == null ) {
			return;
		}
		
		final String WINDOW_KEY = "productionplan_template_editor";
		
		final CreateProductionTemplateComponent comp =
			new CreateProductionTemplateComponent();
		
		final ManufacturingJobRequest job =
			createManufacturingJobRequest(data,c,
					getBPOMaterialEfficiency() ,
					getBPOProductionEfficiency(),
					getRequestedQuantity(), getSlotLocation() );
		
		comp.setManufacturingJobRequest( job ); 
		
		WindowManager.getInstance().getWindow( WINDOW_KEY , new IWindowProvider() {

			@Override
			public Window createWindow()
			{
				return ComponentWrapper.wrapComponent( "Create production plan template" , comp );
			}} ).setVisible( true );
	}
}