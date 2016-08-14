/**
 * Copyright 2010-2016 interactive instruments GmbH
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
package de.interactive_instruments.etf.dal.dao.basex;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.*;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.EtfConstants;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.MetaDataItemDto;
import de.interactive_instruments.etf.dal.dto.RepositoryItemDto;
import de.interactive_instruments.etf.dal.dto.capabilities.*;
import de.interactive_instruments.etf.dal.dto.result.*;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.dal.dto.test.*;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateDto;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateParameterDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.exceptions.*;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
class BsxTestUtil {

	final static BsxDataStorage DATA_STORAGE;

	final static TestObjectDto TO_DTO_1;

	static IFile DATA_STORAGE_DIR;

	final static TestObjectTypeDto TOT_DTO_1;
	final static TestObjectTypeDto TOT_DTO_2;
	final static TestObjectTypeDto TOT_DTO_3;

	final static TagDto TAG_DTO_1;
	final static TagDto TAG_DTO_2;
	final static TagDto TAG_DTO_3;

	final static TestRunDto TR_DTO_1;

	final static TestTaskDto TASK_DTO_1;
	final static TestTaskDto TASK_DTO_2;

	static final TestItemTypeDto ASSERTION_TYPE_1;
	static final TestItemTypeDto TESTSTEP_TYPE_2;

	static final ExecutableTestSuiteDto ETS_DTO_1;
	static final ExecutableTestSuiteDto ETS_DTO_2;

	static final TestTaskResultDto TTR_DTO_1;
	static final TestTaskResultDto TTR_DTO_2;

	static final TranslationTemplateBundleDto TTB_DTO_1;

	static final ComponentDto COMP_DTO_1;

	static {
		DATA_STORAGE = new BsxDataStorage();

		COMP_DTO_1 = new ComponentDto();
		setBasicProperties(COMP_DTO_1, 1);

		TAG_DTO_1 = new TagDto();
		setBasicProperties(TAG_DTO_1, 1);

		TAG_DTO_2 = new TagDto();
		setBasicProperties(TAG_DTO_2, 2);

		TAG_DTO_3 = new TagDto();
		setBasicProperties(TAG_DTO_3, 3);

		TO_DTO_1 = new TestObjectDto();
		setBasicProperties(TO_DTO_1, 1);

		TOT_DTO_1 = new TestObjectTypeDto();
		setBasicProperties(TOT_DTO_1, 1);

		TOT_DTO_2 = new TestObjectTypeDto();
		setBasicProperties(TOT_DTO_2, 2);

		TOT_DTO_3 = new TestObjectTypeDto();
		setBasicProperties(TOT_DTO_3, 3);

		TO_DTO_1.addTestObjectType(TOT_DTO_1);
		final ResourceDto resourceDto = new ResourceDto();
		resourceDto.setName("Resource.1");
		resourceDto.setUri(URI.create("http://nowhere.com"));
		TO_DTO_1.addResource(resourceDto);
		TO_DTO_1.setTags(new ArrayList<TagDto>() {
			{
				add(TAG_DTO_1);
			}
		});

		TTB_DTO_1 = new TranslationTemplateBundleDto();
		setBasicProperties(TTB_DTO_1, 1);

		final List<TranslationTemplateDto> translationTemplateDtos = new ArrayList<TranslationTemplateDto>() {
			{
				final TranslationTemplateDto template1En = new TranslationTemplateDto(
						"TR.Template.1", Locale.ENGLISH.toLanguageTag(),
						"TR.Template.1 with three tokens: {TOKEN.3} {TOKEN.1} {TOKEN.2}");
				final TranslationTemplateDto template1De = new TranslationTemplateDto(
						"TR.Template.1", Locale.GERMAN.toLanguageTag(),
						"TR.Template.1 mit drei tokens: {TOKEN.3} {TOKEN.1} {TOKEN.2}");
				final TranslationTemplateDto template2En = new TranslationTemplateDto(
						"TR.Template.2", Locale.ENGLISH.toLanguageTag(),
						"TR.Template.2 with three tokens: {TOKEN.5} {TOKEN.4} {TOKEN.6}");
				final TranslationTemplateDto template2De = new TranslationTemplateDto(
						"TR.Template.2", Locale.GERMAN.toLanguageTag(),
						"TR.Template.2 mit drei tokens: {TOKEN.5} {TOKEN.4} {TOKEN.6}");
				add(template1En);
				add(template1De);
				add(template2En);
				add(template2De);
			}
		};
		TTB_DTO_1.addTranslationTemplates(translationTemplateDtos);

		ASSERTION_TYPE_1 = new TestItemTypeDto();
		setBasicProperties(ASSERTION_TYPE_1, 1);

		TESTSTEP_TYPE_2 = new TestItemTypeDto();
		setBasicProperties(TESTSTEP_TYPE_2, 2);

		ETS_DTO_1 = new ExecutableTestSuiteDto();
		ETS_DTO_1.setTranslationTemplateBundle(TTB_DTO_1);
		createEtsStructure(ETS_DTO_1, 1);

		TTR_DTO_1 = new TestTaskResultDto();
		createResultStructure(TTR_DTO_1, ETS_DTO_1, 1);

		TASK_DTO_1 = new TestTaskDto();
		setBasicProperties(TASK_DTO_1, 1);
		TASK_DTO_1.setTestObject(TO_DTO_1);
		TASK_DTO_1.setExecutableTestSuite(ETS_DTO_1);
		TASK_DTO_1.setTestTaskResult(TTR_DTO_1);

		ETS_DTO_2 = new ExecutableTestSuiteDto();
		ETS_DTO_2.setTranslationTemplateBundle(TTB_DTO_1);
		createEtsStructure(ETS_DTO_2, 2);

		TTR_DTO_2 = new TestTaskResultDto();
		createResultStructure(TTR_DTO_2, ETS_DTO_2, 2);

		TASK_DTO_2 = new TestTaskDto();
		setBasicProperties(TASK_DTO_2, 2);
		TASK_DTO_2.setTestObject(TO_DTO_1);
		TASK_DTO_2.setExecutableTestSuite(ETS_DTO_2);
		TASK_DTO_2.setTestTaskResult(TTR_DTO_2);

		TR_DTO_1 = new TestRunDto();
		setBasicProperties(TR_DTO_1, 1);
		TR_DTO_1.setLabel("Tag." + toStrWithTrailingZeros(1) + ".label");
		TR_DTO_1.setStartTimestamp(new Date(0));
		TR_DTO_1.setDefaultLang(Locale.ENGLISH.toLanguageTag());
		TR_DTO_1.setTestTasks(new ArrayList<TestTaskDto>() {
			{
				add(TASK_DTO_1);
				add(TASK_DTO_2);
			}
		});
	}

	static String toStrWithTrailingZeros(int i) {
		return String.format("%05d", i);
	}

	static String toStrWithTrailingZeros(long i) {
		return String.format("%05d", i);
	}

	static void setBasicProperties(final Dto dto, final long i) {
		final String name = dto.getClass().getSimpleName() + "." + toStrWithTrailingZeros(i);
		dto.setId(EidFactory.getDefault().createUUID(name));
		if (dto instanceof MetaDataItemDto) {
			final MetaDataItemDto mDto = ((MetaDataItemDto) dto);
			mDto.setLabel(name + ".label");
			mDto.setDescription(name + ".description");
		}
		if (dto instanceof RepositoryItemDto) {
			final RepositoryItemDto rDto = ((RepositoryItemDto) dto);
			rDto.setAuthor(name + ".author");
			rDto.setCreationDate(new Date(0));
			rDto.setVersionFromStr("1.0.0");
			rDto.setItemHash(name.getBytes());
		}
		if (dto instanceof ResultModelItemDto) {
			final ResultModelItemDto rDto = ((ResultModelItemDto) dto);
			rDto.setStartTimestamp(new Date(0));
			rDto.setResultStatus(TestResultStatus.FAILED);
			rDto.setDuration(1000);
		}
	}

	static private final int testModuleSize = 5;
	static private final int testCaseSize = 3;
	static private final int testStepSize = 3;
	static private final int testAssertionSize = 3;

	static int idR = 555555;

	static void createResultStructure(final TestTaskResultDto ttrDto, final ExecutableTestSuiteDto etsDto, final int i) {
		setBasicProperties(ttrDto, i);
		ttrDto.setResultedFrom(etsDto);
		ttrDto.setTestObject(TO_DTO_1);
		final AttachmentDto logFile = new AttachmentDto();
		setBasicProperties(logFile, 1);
		logFile.setLabel("Log file");
		logFile.setReferencedData(URI.create("http://logfile"));
		logFile.setEncoding("UTF-8");
		logFile.setMimeType("text/plain");
		ttrDto.addAttachment(logFile);

		// Create Test Suite Results
		final List<TestModuleResultDto> testSuiteResultDtos = new ArrayList<TestModuleResultDto>();
		for (int tsi = 0; tsi < testModuleSize; tsi++) {
			final TestModuleResultDto testSuiteResultDto = new TestModuleResultDto();
			setBasicProperties(testSuiteResultDto, idR--);
			testSuiteResultDto.setParent(ttrDto);
			testSuiteResultDto.setResultedFrom(etsDto.getTestModules().get(tsi));

			final List<TestCaseResultDto> testCaseResultDtos = new ArrayList<>();
			for (int tci = 0; tci < testCaseSize; tci++) {
				final TestCaseResultDto testCaseResultDto = new TestCaseResultDto();
				setBasicProperties(testCaseResultDto, idR--);
				testCaseResultDto.setParent(testSuiteResultDto);
				testCaseResultDto.setResultedFrom(
						((TestModuleDto) testSuiteResultDto.getResultedFrom()).getTestCases().get(tci));

				final List<TestStepResultDto> testStepResultDtos = new ArrayList<>();
				for (int tsti = 0; tsti < testStepSize; tsti++) {
					final TestStepResultDto testStepResultDto = new TestStepResultDto();
					setBasicProperties(testStepResultDto, idR--);
					testStepResultDto.setParent(testCaseResultDto);
					testStepResultDto.setResultedFrom(
							((TestCaseDto) testCaseResultDto.getResultedFrom()).getTestSteps().get(tsti));

					final List<TestAssertionResultDto> testAssertionResultDtos = new ArrayList<>();
					for (int ta = 0; ta < testAssertionSize; ta++) {
						final TestAssertionResultDto testAssertionResultDto = new TestAssertionResultDto();
						setBasicProperties(testAssertionResultDto, idR--);
						testAssertionResultDto.setParent(testStepResultDto);
						testAssertionResultDto.setResultedFrom(
								((TestStepDto) testStepResultDto.getResultedFrom()).getTestAssertions().get(ta));

						testAssertionResultDto.setMessages(new ArrayList<TranslationTemplateParameterDto>() {
							{
								final TranslationTemplateParameterDto messages1 = new TranslationTemplateParameterDto();
								messages1.setRefTemplateName("TR.Template.1");
								messages1.addTokenValue("TOKEN.1", "Value1");
								messages1.addTokenValue("TOKEN.2", "Value2");
								messages1.addTokenValue("TOKEN.3", "Value3");
								final TranslationTemplateParameterDto messages2 = new TranslationTemplateParameterDto();
								messages2.setRefTemplateName("Template.2");
								messages2.addTokenValue("TOKEN.4", "Value4");
								messages2.addTokenValue("TOKEN.5", "Value5");
								messages2.addTokenValue("TOKEN.6", "Value6");
								add(messages1);
								add(messages2);
							}
						});

						testAssertionResultDtos.add(testAssertionResultDto);
					}
					testStepResultDto.setTestAssertionResults(testAssertionResultDtos);
					testStepResultDtos.add(testStepResultDto);
				}
				testCaseResultDto.setTestStepResults(testStepResultDtos);
				testCaseResultDtos.add(testCaseResultDto);

			}
			testSuiteResultDto.setTestCaseResults(testCaseResultDtos);
			testSuiteResultDtos.add(testSuiteResultDto);
		}
		ttrDto.setTestModuleResults(testSuiteResultDtos);
	}

	static int idE = 111111;

	static void createEtsStructure(ExecutableTestSuiteDto etsDto, int i) {
		setBasicProperties(etsDto, i);
		etsDto.setTestDriver(COMP_DTO_1);
		etsDto.setSupportedTestObjectTypes(new ArrayList<TestObjectTypeDto>() {
			{
				add(TOT_DTO_1);
			}
		});

		// Create Test Modules
		final List<TestModuleDto> testModuleDtos = new ArrayList<TestModuleDto>();
		for (int tmi = 0; tmi < testModuleSize; tmi++) {
			final TestModuleDto testModuleDto = new TestModuleDto();
			setBasicProperties(testModuleDto, (i + 1) + (tmi + 1) * 1000);
			testModuleDto.setParent(etsDto);

			final List<TestCaseDto> testCaseDtos = new ArrayList<>();
			for (int tci = 0; tci < testCaseSize; tci++) {
				final TestCaseDto testCaseDto = new TestCaseDto();
				setBasicProperties(testCaseDto, idE++);
				testCaseDto.setParent(testModuleDto);

				final List<TestStepDto> testStepDtos = new ArrayList<>();
				for (int tsti = 0; tsti < testStepSize; tsti++) {
					final TestStepDto testStepDto = new TestStepDto();
					setBasicProperties(testStepDto, idE++);
					testStepDto.setParent(testCaseDto);
					testStepDto.setStatementForExecution("ExecutionStatement");
					testStepDto.setType(TESTSTEP_TYPE_2);

					final List<TestAssertionDto> testAssertionDtos = new ArrayList<>();
					for (int ta = 0; ta < testAssertionSize; ta++) {
						final TestAssertionDto testAssertionDto = new TestAssertionDto();
						setBasicProperties(testAssertionDto, idE++);
						testAssertionDto.setParent(testStepDto);
						testAssertionDto.setExpectedResult("ExpectedResult");
						testAssertionDto.setExpression("Expression");
						testAssertionDto.setType(ASSERTION_TYPE_1);
						testAssertionDto.addTranslationTemplateWithName("TR.Template.1");
						testAssertionDto.addTranslationTemplateWithName("TR.Template.2");
						testAssertionDtos.add(testAssertionDto);
					}
					testStepDto.setTestAssertions(testAssertionDtos);
					testStepDtos.add(testStepDto);
				}
				testCaseDto.setTestSteps(testStepDtos);
				testCaseDtos.add(testCaseDto);

			}
			testModuleDto.setTestCases(testCaseDtos);
			testModuleDtos.add(testModuleDto);
		}
		etsDto.setTestModules(testModuleDtos);
	}

	static void ensureInitialization() throws ConfigurationException, InvalidStateTransitionException, InitializationException, StoreException {
		if (!DATA_STORAGE.isInitialized()) {

			if (System.getenv("ETF_DS_DIR") != null) {
				DATA_STORAGE_DIR = new IFile(System.getenv("ETF_DS_DIR"));
				DATA_STORAGE_DIR.mkdirs();
			} else if (new IFile("./build").exists()) {
				DATA_STORAGE_DIR = new IFile("./build/tmp/etf-ds");
				DATA_STORAGE_DIR.mkdirs();
			} else {
				DATA_STORAGE_DIR = null;
			}

			assertTrue(DATA_STORAGE_DIR != null && DATA_STORAGE_DIR.exists());
			DATA_STORAGE.getConfigurationProperties().setProperty(EtfConstants.ETF_DATASOURCE_DIR, DATA_STORAGE_DIR.getAbsolutePath());
			DATA_STORAGE.init();
			BsxTestUtil.DATA_STORAGE.reset();
		}
	}

	static void forceDelete(final Dao dao, final EID eid) throws StoreException {
		try {
			((WriteDao)dao).delete(eid);
		} catch (ObjectWithIdNotFoundException e) {
			ExcUtils.suppress(e);
		}
		assertFalse(dao.exists(eid));
	}

	static WriteDao getDao(final Dto dto) {
		assertNotNull(dto);
		final Dao dao = DATA_STORAGE.getDao(dto.getClass());
		assertNotNull(dao);
		assertTrue(dao.isInitialized());
		return (WriteDao) dao;
	}

	static void forceAdd(final Dto dto) throws StoreException, ObjectWithIdNotFoundException {
		final WriteDao dao = getDao(dto);

		forceDelete(dao, dto.getId());
		assertFalse(dao.exists(dto.getId()));
		try {
			((WriteDao)dao).add(dto);
		} catch (StoreException e) {
			ExcUtils.suppress(e);
		}
		assertTrue(dao.exists(dto.getId()));
		assertNotNull(dao.getById(dto.getId()).getDto());
	}

	static void existsAndAddAndDeleteTest(final Dto dto) throws StoreException, ObjectWithIdNotFoundException {
		final WriteDao dao = getDao(dto);

		assertFalse(dao.exists(dto.getId()));
		dao.add(dto);
		assertTrue(dao.exists(dto.getId()));
		dao.delete(dto.getId());
		assertFalse(dao.exists(dto.getId()));
	}

	/**
	 *
	 * @return queried Dto
	 */
	static <T extends Dto> PreparedDto<T> addAndGetByIdTest(final T dto) throws StoreException, ObjectWithIdNotFoundException {
		final WriteDao dao = getDao(dto);

		assertFalse(dao.exists(dto.getId()));
		dao.add(dto);
		assertTrue(dao.exists(dto.getId()));

		final PreparedDto<T> preparedDto = dao.getById(dto.getId());
		// Check internal ID
		assertEquals(dto.getId(), preparedDto.getDtoId());
		final T queriedDto = preparedDto.getDto();
		assertNotNull(dto);
		assertEquals(dto.getId(), queriedDto.getId());
		assertEquals(dto.getDescriptiveLabel(), queriedDto.getDescriptiveLabel());

		// Check compareTo
		final PreparedDto<T> preparedDto2 = dao.getById(dto.getId());
		assertEquals(0, preparedDto2.compareTo(preparedDto));
		// Will execute the query
		assertEquals(preparedDto2.getDtoId(), preparedDto2.getDto().getId());
		assertEquals(0, preparedDto2.compareTo(preparedDto));

		return preparedDto;
	}

}
