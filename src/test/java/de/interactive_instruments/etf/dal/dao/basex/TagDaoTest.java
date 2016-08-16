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
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class TagDaoTest {

	private static WriteDao<TagDto> writeDao;

	@BeforeClass
	public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException, StoreException {
		BsxTestUtil.ensureInitialization();
		writeDao = ((WriteDao) DATA_STORAGE.getDao(TagDto.class));
	}

	@Before
	public void clean() {
		try {
			writeDao.delete(BsxTestUtil.TAG_DTO_1.getId());
		} catch (ObjectWithIdNotFoundException | StoreException e) {}
	}

	@Test
	public void test_1_1_existsAndAddAndDelete() throws StoreException, ObjectWithIdNotFoundException {
		BsxTestUtil.existsAndAddAndDeleteTest(BsxTestUtil.TAG_DTO_1);
	}

	@Test
	public void test_2_0_getById() throws StoreException, ObjectWithIdNotFoundException {
		assertFalse(writeDao.exists(BsxTestUtil.TAG_DTO_1.getId()));
		writeDao.add(BsxTestUtil.TAG_DTO_1);
		assertTrue(writeDao.exists(BsxTestUtil.TAG_DTO_1.getId()));

		final PreparedDto<TagDto> preparedDto = writeDao.getById(BsxTestUtil.TAG_DTO_1.getId());
		// Check internal ID
		assertEquals(BsxTestUtil.TAG_DTO_1.getId(), preparedDto.getDtoId());
		final TagDto dto = preparedDto.getDto();
		assertNotNull(dto);
		assertEquals(BsxTestUtil.TAG_DTO_1.getId(), dto.getId());
		assertEquals(BsxTestUtil.TAG_DTO_1.toString(), dto.toString());
	}

}
