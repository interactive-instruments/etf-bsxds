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

import java.io.IOException;

import org.junit.*;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExecutableTestSuiteDaoTest {

	private static WriteDao<ExecutableTestSuiteDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException, StoreException, ObjectWithIdNotFoundException, IOException {
		BsxTestUtil.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(ExecutableTestSuiteDto.class));

		TestObjectDaoTest.setUp();

		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TTB_DTO_1);

		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.COMP_DTO_1);

		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.ASSERTION_TYPE_1);

		BsxTestUtil.forceDeleteAndAdd(BsxTestUtil.TESTSTEP_TYPE_2);
	}

	@AfterClass
	public static void tearDown() throws ConfigurationException, InvalidStateTransitionException, InitializationException, StoreException {
		TestObjectDaoTest.tearDown();
	}

	@Before
	public void clean() {
		try {
			writeDao.delete(BsxTestUtil.ETS_DTO_1.getId());
			writeDao.delete(BsxTestUtil.ETS_DTO_2.getId());
		} catch (ObjectWithIdNotFoundException | StoreException e) {}
	}

	@Test
	public void test_2_0_add_and_get() throws StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.existsAndAddAndDeleteTest(BsxTestUtil.ETS_DTO_1);
	}

}
