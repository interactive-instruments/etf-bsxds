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
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
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
		assertFalse(writeDao.exists(BsxTestUtil.ETS_DTO_1.getId()));
		writeDao.add(BsxTestUtil.ETS_DTO_1);
		assertTrue(writeDao.exists(BsxTestUtil.ETS_DTO_1.getId()));

		final PreparedDto<ExecutableTestSuiteDto> preparedDto = writeDao.getById(BsxTestUtil.ETS_DTO_1.getId());
		// Check internal ID
		assertEquals(BsxTestUtil.ETS_DTO_1.getId(), preparedDto.getDtoId());
		final ExecutableTestSuiteDto dto = preparedDto.getDto();
		assertNotNull(dto);
		assertEquals(BsxTestUtil.ETS_DTO_1.getId(), dto.getId());
		assertEquals(BsxTestUtil.ETS_DTO_1.toString(), dto.toString());
		assertNotNull(dto.getParameters());
		assertEquals("Parameter.1.key",dto.getParameters().getParameter("Parameter.1.key").getName());
		assertEquals("Parameter.1.value",dto.getParameters().getParameter("Parameter.1.key").getDefaultValue());

		assertEquals("Parameter.2.key",dto.getParameters().getParameter("Parameter.2.key").getName());
		assertEquals("Parameter.2.value",dto.getParameters().getParameter("Parameter.2.key").getDefaultValue());
	}

}
