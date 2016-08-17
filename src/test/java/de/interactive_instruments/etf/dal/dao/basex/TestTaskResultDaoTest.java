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

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtil.DATA_STORAGE;
import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtil.trimAllWhitespace;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.Filter;
import de.interactive_instruments.etf.model.OutputFormat;
import junit.framework.TestCase;

import org.junit.*;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTaskResultDaoTest {

	private static WriteDao<TestTaskResultDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException, StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TestTaskResultDto.class));

		ExecutableTestSuiteDaoTest.setUp();

		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.ETS_DTO_1);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.ETS_DTO_2);

		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TO_DTO_1);
	}

	@AfterClass
	public static void tearDown() throws InvalidStateTransitionException, StoreException, InitializationException, ConfigurationException {
		ExecutableTestSuiteDaoTest.tearDown();
	}

	@Before
	public void clean() {
		try {
			BsxTestUtil.forceDelete(writeDao, BsxTestUtil.TTR_DTO_1.getId());
			BsxTestUtil.forceDelete(writeDao, BsxTestUtil.TTR_DTO_2.getId());
			BsxTestUtil.forceDelete(DATA_STORAGE.getDao(TestTaskResultDto.class), BsxTestUtil.TR_DTO_1.getId());
		} catch (StoreException e) {}
	}

	@Test
	public void test_1_1_existsAndAddAndDelete() throws StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.existsAndAddAndDeleteTest(BsxTestUtil.TTR_DTO_1);
	}

	@Test
	public void test_2_0_add_and_get() throws StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TO_DTO_1);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.ETS_DTO_1);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.ETS_DTO_2);
		// TestTask required for parent reference
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TR_DTO_1, false);

		final PreparedDto<TestTaskResultDto> preparedDto = BsxTestUtil.addAndGetByIdTest(BsxTestUtil.TTR_DTO_1);

		assertNotNull(preparedDto.getDto().getAttachments());
		assertEquals(1, preparedDto.getDto().getAttachments().size());
		assertNotNull(preparedDto.getDto().getTestObject());
		assertEquals(BsxTestUtil.TO_DTO_1.toString().trim(), preparedDto.getDto().getTestObject().toString().trim());
		assertNotNull(preparedDto.getDto().getTestModuleResults());

		writeDao.delete(BsxTestUtil.TTR_DTO_1.getId());
		TestCase.assertFalse(writeDao.exists(BsxTestUtil.TTR_DTO_1.getId()));
	}

	@Test
	public void test_4_0_streaming() throws StoreException, ObjectWithIdNotFoundException, IOException, URISyntaxException {
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TO_DTO_1);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.ETS_DTO_1);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.ETS_DTO_2);
		// TestTask required for parent reference
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TR_DTO_1, false);

		final PreparedDto<TestTaskResultDto> preparedDto = BsxTestUtil.addAndGetByIdTest(BsxTestUtil.TTR_DTO_1);

		final IFile tmpFile = IFile.createTempFile("etf",".html");
		tmpFile.deleteOnExit();
		final FileOutputStream fop = new FileOutputStream(tmpFile);
		final OutputFormat outputFormat = writeDao.getOutputFormats().values().iterator().next();

		preparedDto.streamTo(outputFormat, null, fop);

		final IFile cmpResult = new IFile(getClass().getClassLoader().getResource("cmp/TestTaskResult.html").toURI());
		assertTrue(cmpResult.exists());

		assertEquals(trimAllWhitespace(cmpResult.readContent().toString()), trimAllWhitespace(tmpFile.readContent().toString()));
	}

}
