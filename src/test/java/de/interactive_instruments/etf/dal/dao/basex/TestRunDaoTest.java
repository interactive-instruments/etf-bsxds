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
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import junit.framework.TestCase;

import org.junit.*;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRunDaoTest {

	private static WriteDao<TestRunDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException, StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TestRunDto.class));

		TestTaskResultDaoTest.setUp();

		final WriteDao<TestTaskResultDto> tagDao = (WriteDao<TestTaskResultDto>) DATA_STORAGE.getDao(TestTaskResultDto.class);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TO_DTO_1);
	}

	@AfterClass
	public static void tearDown() throws StoreException, ConfigurationException, InitializationException, InvalidStateTransitionException, ObjectWithIdNotFoundException {
		TestTaskResultDaoTest.tearDown();
		BsxTestUtil.addTest(BsxTestUtil.TR_DTO_1);
	}

	@Before
	public void clean() {
		try {
			writeDao.delete(BsxTestUtil.TR_DTO_1.getId());
		} catch (ObjectWithIdNotFoundException | StoreException e) {}
	}

	@Test
	public void test_1_1_existsAndAddAndDelete() throws StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.existsAndAddAndDeleteTest(BsxTestUtil.TR_DTO_1);
	}

	@Test
	public void test_2_0_add_and_get() throws StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.addTest(BsxTestUtil.TR_DTO_1);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TTR_DTO_1);
		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TTR_DTO_2);

		final PreparedDto<TestRunDto> preparedDto = BsxTestUtil.getByIdTest(BsxTestUtil.TR_DTO_1);

		assertNotNull(preparedDto.getDto().getTestTasks());
		assertEquals(2, preparedDto.getDto().getTestTasks().size());

		assertEquals("FAILED", preparedDto.getDto().getTestTasks().get(0).getTestTaskResult().getResultStatus().toString());

		writeDao.delete(BsxTestUtil.TR_DTO_1.getId());
		TestCase.assertFalse(writeDao.exists(BsxTestUtil.TR_DTO_1.getId()));
	}

}
