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

import de.interactive_instruments.etf.dal.dao.PreparedDtoResult;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestConstants.DATA_STORAGE;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestObjectDaoTest {

	private static WriteDao<TestObjectDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException {
		BsxTestConstants.ensureInitialization();
		writeDao = ((WriteDao)DATA_STORAGE.getDao(TestObjectDto.class));

		try {
			writeDao.delete(BsxTestConstants.TO_DTO_1.getId());
		} catch (ObjectWithIdNotFoundException | StoreException e) {
		}
	}

	@Test
	public void test_1_existsAndAddAndDelete() throws StoreException, ObjectWithIdNotFoundException {
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		writeDao.add(BsxTestConstants.TO_DTO_1);
		assertTrue(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		writeDao.delete(BsxTestConstants.TO_DTO_1.getId());
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
	}

	@Test
	public void test_2_getById() throws StoreException, ObjectWithIdNotFoundException {
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		writeDao.add(BsxTestConstants.TO_DTO_1);
		assertTrue(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		final PreparedDtoResult<TestObjectDto> preparedDto = writeDao.
				getById(BsxTestConstants.TO_DTO_1.getId());
		final TestObjectDto dto =  preparedDto.getDto();
		assertEquals(BsxTestConstants.TO_DTO_1.getId(), dto.getId());
		assertEquals(BsxTestConstants.TO_DTO_1.toString(), dto.toString());
		writeDao.delete(BsxTestConstants.TO_DTO_1.getId());
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
	}

	@Test
	public void test_3_pagination() {

	}

	@Test
	public void test_4_streaming() {

	}

	@Test
	public void test_5_update() throws StoreException, ObjectWithIdNotFoundException {
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		writeDao.add(BsxTestConstants.TO_DTO_1);
		assertTrue(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));

		final String originalLabel = BsxTestConstants.TO_DTO_1.getLabel();

		// Query dto
		final PreparedDtoResult<TestObjectDto> preparedDto = writeDao.
				getById(BsxTestConstants.TO_DTO_1.getId());
		assertEquals(null, preparedDto.getDto().getReplacedBy());
		// Change its label
		preparedDto.getDto().setLabel("NEW LABEL");
		assertEquals("NEW LABEL", preparedDto.getDto().getLabel());
		// Write back
		final TestObjectDto newDto = writeDao.update(preparedDto.getDto());
		assertEquals("NEW LABEL", newDto.getLabel());

		// Check that the old one still exists, same ID, same label, but with a reference
		final PreparedDtoResult<TestObjectDto> preparedOldDto = writeDao.
				getById(BsxTestConstants.TO_DTO_1.getId());
		assertEquals(originalLabel, preparedOldDto.getDto().getLabel());
		assertFalse("NEW LABEL".equals(preparedOldDto.getDto().getLabel()));
		assertNotNull(preparedOldDto.getDto().getReplacedBy());
		assertEquals(newDto.getId(), preparedOldDto.getDto().getReplacedBy().getId());

		// And check that the new one exists
		assertEquals("NEW LABEL", ((TestObjectDto)preparedOldDto.getDto().getReplacedBy()).getLabel());

		// query and compare new one
		final PreparedDtoResult<TestObjectDto> preparedNewDto = writeDao.
				getById(newDto.getId());
		assertEquals(newDto, preparedNewDto.getDto());
	}

	@Test
	public void test_6_updateWithReset() throws StoreException, ObjectWithIdNotFoundException {
		DATA_STORAGE.reset();
	}
}
