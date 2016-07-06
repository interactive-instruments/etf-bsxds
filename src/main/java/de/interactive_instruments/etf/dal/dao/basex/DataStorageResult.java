/*
 * Copyright ${year} interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.interactive_instruments.etf.dal.dao.basex;

import de.interactive_instruments.etf.dal.dto.capabilities.*;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.dal.dto.test.TranslationTemplateDto;

import java.util.List;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class DataStorageResult {

	private List<ComponentDto> components;
	//private List<ParameterizedTestCaseDto> testCases;
	//private List<ParameterizedTestAssertionDto> testAssertions;
	private List<ExecutableTestSuiteDto> executableTestSuites;
	private List<TestObjectTypeDto> testObjectTypes;
	private List<TestObjectDto> testObjects;
	//private List<CredentialDto> credentials;
	//private List<UserDto> users;
	private List<TagDto> tags;
	private List<TranslationTemplateDto> translationTemplates;
	private List<ResultStyleDto> resultStyles;
	// private List<AbstractTestSuiteDto> abstractTestSuites;
	// private List<TestRunTemplateDto> testRunTemplates;
	private List<TestRunDto> testRuns;

	public List<ComponentDto> getComponents() {
		return components;
	}

	public List<ExecutableTestSuiteDto> getExecutableTestSuites() {
		return executableTestSuites;
	}

	public List<TestObjectTypeDto> getTestObjectTypes() {
		return testObjectTypes;
	}

	public List<TestObjectDto> getTestObjects() {
		return testObjects;
	}

	public List<TagDto> getTags() {
		return tags;
	}

	public List<TranslationTemplateDto> getTranslationTemplates() {
		return translationTemplates;
	}

	public List<ResultStyleDto> getResultStyles() {
		return resultStyles;
	}

	public List<TestRunDto> getTestRuns() {
		return testRuns;
	}
}
