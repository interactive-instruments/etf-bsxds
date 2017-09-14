/**
 * Copyright 2017 European Union, interactive instruments GmbH
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.dal.dao.basex;

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.DATA_STORAGE;
import static de.interactive_instruments.etf.test.TestDtos.*;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.*;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.test.TestDtos;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRunDaoTest {

	private static WriteDao<TestRunDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException,
			StorageException, ObjectWithIdNotFoundException, IOException {
		BsxTestUtils.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TestRunDto.class));

		TestTaskResultDaoTest.setUp();

		final WriteDao<TestTaskResultDto> tagDao = (WriteDao<TestTaskResultDto>) DATA_STORAGE.getDao(TestTaskResultDto.class);
		BsxTestUtils.forceDeleteAndAdd(TO_DTO_1);
	}

	@AfterClass
	public static void tearDown() throws StorageException, ConfigurationException, InitializationException,
			InvalidStateTransitionException, ObjectWithIdNotFoundException {
		TestTaskResultDaoTest.tearDown();
		BsxTestUtils.forceDelete(writeDao, TR_DTO_1.getId());
	}

	@Before
	public void clean() {
		try {
			writeDao.delete(TR_DTO_1.getId());
		} catch (ObjectWithIdNotFoundException | StorageException e) {}
	}

	@Test
	public void test_2_0_add_and_get() throws StorageException, ObjectWithIdNotFoundException {
		// Ensure structure
		TR_DTO_1.setTestTasks(new ArrayList<TestTaskDto>() {
			{
				this.add(TestDtos.TASK_DTO_1);
				this.add(TestDtos.TASK_DTO_2);
			}
		});
		TASK_DTO_1.setTestTaskResult(TTR_DTO_1);
		TASK_DTO_2.setTestTaskResult(TTR_DTO_2);

		BsxTestUtils.addTest(TR_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(TTR_DTO_1);
		BsxTestUtils.forceDeleteAndAdd(TTR_DTO_2);

		final PreparedDto<TestRunDto> preparedDto = BsxTestUtils.getByIdTest(TR_DTO_1);

		assertNotNull(preparedDto.getDto().getTestTasks());
		assertEquals(2, preparedDto.getDto().getTestTasks().size());

		assertEquals("FAILED", preparedDto.getDto().getTestTasks().get(0).getTestTaskResult().getResultStatus().toString());

		writeDao.delete(TR_DTO_1.getId());
		TestCase.assertFalse(writeDao.exists(TR_DTO_1.getId()));
		// Must be deleted as well
		BsxTestUtils.notExistsOrDisabled(TTR_DTO_1);
		BsxTestUtils.notExistsOrDisabled(TTR_DTO_2);
	}
}
