/**
 * Copyright 2017-2019 European Union, interactive instruments GmbH
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
import static de.interactive_instruments.etf.test.TestDtos.ASSERTION_TYPE_1;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.test.TestItemTypeDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class TestItemTypeDaoTest {

	private static WriteDao<TestItemTypeDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException,
			StorageException, IOException {
		BsxTestUtils.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TestItemTypeDto.class));
	}

	@Before
	public void clean() {
		try {
			writeDao.delete(ASSERTION_TYPE_1.getId());
		} catch (ObjectWithIdNotFoundException | StorageException e) {}
	}

	@Test
	public void test_1_1_existsAndAddAndDelete() throws StorageException, ObjectWithIdNotFoundException {
		BsxTestUtils.existsAndAddAndDeleteTest(ASSERTION_TYPE_1);
	}

	@Test
	public void test_2_0_getById() throws StorageException, ObjectWithIdNotFoundException {
		final PreparedDto<TestItemTypeDto> preparedDto = BsxTestUtils.addAndGetByIdTest(ASSERTION_TYPE_1);

		writeDao.delete(ASSERTION_TYPE_1.getId());
		TestCase.assertFalse(writeDao.exists(ASSERTION_TYPE_1.getId()));
	}

}
