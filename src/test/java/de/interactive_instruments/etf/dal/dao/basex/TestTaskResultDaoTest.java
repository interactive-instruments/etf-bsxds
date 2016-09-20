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

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.DATA_STORAGE;
import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.trimAllWhitespace;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.net.URISyntaxException;

import junit.framework.TestCase;

import org.junit.*;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.exceptions.StoreException;
import de.interactive_instruments.etf.dal.dto.IncompleteDtoException;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTaskResultDaoTest {

	private static WriteDao<TestTaskResultDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException, StorageException, ObjectWithIdNotFoundException, IOException {
		BsxTestUtils.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TestTaskResultDto.class));

		ExecutableTestSuiteDaoTest.setUp();

		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.ETS_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.ETS_DTO_2);

		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.TO_DTO_1);
	}

	@AfterClass
	public static void tearDown() throws InvalidStateTransitionException, StorageException, InitializationException, ConfigurationException {
		ExecutableTestSuiteDaoTest.tearDown();
	}

	@Before
	public void clean() {
		try {
			BsxTestUtils.forceDelete(writeDao, BsxTestUtils.TTR_DTO_1.getId());
			BsxTestUtils.forceDelete(writeDao, BsxTestUtils.TTR_DTO_2.getId());
			BsxTestUtils.forceDelete(DATA_STORAGE.getDao(TestTaskResultDto.class), BsxTestUtils.TR_DTO_1.getId());
		} catch (StorageException e) {}
	}

	@Test
	public void test_1_1_existsAndAddAndDelete() throws StorageException, ObjectWithIdNotFoundException {
		BsxTestUtils.existsAndAddAndDeleteTest(BsxTestUtils.TTR_DTO_1);
	}

	@Test
	public void test_2_0_add_and_get() throws StorageException, ObjectWithIdNotFoundException {
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.TO_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.ETS_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.ETS_DTO_2);
		// TestTask required for parent reference
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.TR_DTO_1, false);

		final PreparedDto<TestTaskResultDto> preparedDto = BsxTestUtils.addAndGetByIdTest(BsxTestUtils.TTR_DTO_1);

		assertNotNull(preparedDto.getDto().getAttachments());
		assertEquals(1, preparedDto.getDto().getAttachments().size());
		assertNotNull(preparedDto.getDto().getTestObject());
		assertEquals(BsxTestUtils.TO_DTO_1.toString().trim(), preparedDto.getDto().getTestObject().toString().trim());
		assertNotNull(preparedDto.getDto().getTestModuleResults());

		writeDao.delete(BsxTestUtils.TTR_DTO_1.getId());
		TestCase.assertFalse(writeDao.exists(BsxTestUtils.TTR_DTO_1.getId()));
	}

	@Test
	public void test_4_0_streaming() throws StorageException, ObjectWithIdNotFoundException, IOException, URISyntaxException {
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.TO_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.ETS_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.ETS_DTO_2);
		// TestTask required for parent reference
		BsxTestUtils.forceDeleteAndAdd(BsxTestUtils.TR_DTO_1, false);

		final PreparedDto<TestTaskResultDto> preparedDto = BsxTestUtils.addAndGetByIdTest(BsxTestUtils.TTR_DTO_1);

		final IFile tmpFile = IFile.createTempFile("etf", ".html");
		tmpFile.deleteOnExit();
		final FileOutputStream fop = new FileOutputStream(tmpFile);

		OutputFormat htmlReportFormat = null;
		for (final OutputFormat outputFormat : writeDao.getOutputFormats().values()) {
			if ("text/html".equals(outputFormat.getMediaTypeType().getType())) {
				htmlReportFormat = outputFormat;
				break;
			}
		}
		assertNotNull(htmlReportFormat);

		preparedDto.streamTo(htmlReportFormat, null, fop);

		final IFile cmpResult = new IFile(getClass().getClassLoader().getResource("cmp/TestTaskResult.html").toURI());
		assertTrue(cmpResult.exists());

		assertEquals(trimAllWhitespace(cmpResult.readContent().toString()), trimAllWhitespace(tmpFile.readContent().toString()));
	}

	@Test(expected = StoreException.class)
	public void test_7_1_stream_file_to_store() throws StorageException, ObjectWithIdNotFoundException, FileNotFoundException, IncompleteDtoException {
		final IFile taskTestResultFile = new IFile(getClass().getClassLoader().getResource(
				"database/testtaskresult.xml").getPath());
		((StreamWriteDao<TestTaskResultDto>) writeDao).add(new FileInputStream(taskTestResultFile));
	}

}
