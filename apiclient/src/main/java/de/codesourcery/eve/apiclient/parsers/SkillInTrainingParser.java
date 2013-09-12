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

import de.codesourcery.eve.apiclient.datamodel.SkillInTraining;
import de.codesourcery.eve.apiclient.exceptions.UnparseableResponseException;
import de.codesourcery.eve.skills.datamodel.IStaticDataModel;
import de.codesourcery.eve.skills.utils.ISystemClock;

public class SkillInTrainingParser extends AbstractResponseParser<SkillInTraining> {

	public static final URI URI = toURI("/char/SkillInTraining.xml.aspx");
	
	private SkillInTraining result;
	private final IStaticDataModel provider;
	
	public SkillInTrainingParser(IStaticDataModel provider,ISystemClock clock) {
		super(clock);
		this.provider = provider;
	}
	
	/*
<eveapi version="2">
  <currentTime>2008-08-17 06:43:00</currentTime>
  <result>
    <currentTQTime offset="0">2008-08-17 06:43:00</currentTQTime>
    <trainingEndTime>2008-08-17 15:29:44</trainingEndTime>
    <trainingStartTime>2008-08-15 04:01:16</trainingStartTime>
    <trainingTypeID>3305</trainingTypeID>
    <trainingStartSP>24000</trainingStartSP>
    <trainingDestinationSP>135765</trainingDestinationSP>
    <trainingToLevel>4</trainingToLevel>
    <skillInTraining>1</skillInTraining>
  </result>
  <cachedUntil>2008-08-17 06:58:00</cachedUntil>
</eveapi>
	 */
	
	@Override
	void parseHook(Document document) throws UnparseableResponseException {
		
		final Element resultNode =
			getResultElement( document );

		result = new SkillInTraining();
		
		final boolean inTraining = Integer.parseInt( getChildValue( resultNode , "skillInTraining" ) ) != 0;
		result.setSkillInTraining( inTraining );
		
		if ( inTraining ) {
			result.setCurrentTQTime( parseDate( getChildValue( resultNode , "currentTQTime" ) ) );
			result.setTrainingStartTime( parseDate( getChildValue( resultNode , "trainingStartTime" ) ) );
			result.setTrainingEndTime( parseDate( getChildValue( resultNode , "trainingEndTime" ) ) );
			if ( this.provider != null ) {
				final int typeId =
					Integer.parseInt( getChildValue( resultNode , "trainingTypeID" ) );
				result.setSkill( provider.getSkillTree().getSkill( typeId ) );
			}
			result.setTrainingStartSP( Integer.parseInt( getChildValue( resultNode , "trainingStartSP" ) ) );
			result.setTrainingDestinationSP( Integer.parseInt( getChildValue( resultNode , "trainingDestinationSP" ) ) );
			result.setPlannedLevel( Integer.parseInt( getChildValue( resultNode , "trainingToLevel"  ) ) );
		}

	}

	@Override
	public URI getRelativeURI() {
		return URI;
	}

	@Override
	public SkillInTraining getResult() throws IllegalStateException {
		assertResponseParsed();
		return result;
	}

	@Override
	public void reset() {
		result = null;
	}

}
