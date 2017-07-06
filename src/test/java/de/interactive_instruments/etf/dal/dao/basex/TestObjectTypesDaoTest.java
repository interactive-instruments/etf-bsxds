/**
 * Copyright 2010-2017 interactive instruments GmbH
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

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.*;
import static de.interactive_instruments.etf.test.TestDtos.TOT_DTO_1;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.exceptions.*;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestObjectTypesDaoTest {

	private static final String TO_DTO_1_REPLACED_ID = "aa1b77e2-59d5-3ce6-bbe2-eb2b4c4ae61d";
	final int maxDtos = 250;
	private static WriteDao<TestObjectTypeDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException,
			StorageException, ObjectWithIdNotFoundException, IOException {
		BsxTestUtils.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TestObjectTypeDto.class));
	}

	@Before
	public void clean() throws StorageException {
		BsxTestUtils.forceDelete(writeDao, TOT_DTO_1.getId());
	}

	@Test
	public void test_1_0_add() throws StorageException, ObjectWithIdNotFoundException {
		assertTrue(writeDao.isInitialized());

		notExistsOrDisabled(TOT_DTO_1);
		writeDao.add(TOT_DTO_1);
		existsAndNotDisabled(TOT_DTO_1);
	}

	@Test
	public void test_1_1_addExisting() throws StorageException, ObjectWithIdNotFoundException {
		try {
			test_1_0_add();
		} catch (Exception e) {
			ExcUtils.suppress(e);
		}
		existsAndNotDisabled(TOT_DTO_1);
		boolean exceptionThrown = false;
		try {
			writeDao.add(TOT_DTO_1);
		} catch (StorageException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
	}

}
