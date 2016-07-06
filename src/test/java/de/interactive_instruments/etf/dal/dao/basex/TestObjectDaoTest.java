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

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.basex.transformers.EidAdapter;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.model.item.EidFactory;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.model.std.IdFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestConstants.DATA_STORAGE;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
		try {
			writeDao.delete(EidFactory.getDefault().createAndPreserveStr("0c1582de-fe75-3d0c-b6f9-982bfc79c008"));
		} catch (ObjectWithIdNotFoundException | StoreException e) {
		}
		try {
			BsxTestConstants.DATA_STORAGE.reset();
		} catch (StoreException e) {
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
	public void test_11_basic_unmarshalling() throws JAXBException, IOException {
		final IFile testObjectXmlFile = new IFile(getClass().getClassLoader().getResource(
				"database/testobjects.xml").getPath());
		testObjectXmlFile.expectFileIsReadable();
		final Unmarshaller um = BsxTestConstants.DATA_STORAGE.createUnmarshaller();
		um.setAdapter(new EidAdapter());
		final DataStorageResult dtos = (DataStorageResult) um.unmarshal(new StringReader(testObjectXmlFile.readContent().toString()));
		assertNotNull(dtos);
		assertNotNull(dtos.getTestObjects());
	}


	@Test
	public void test_2_getById() throws StoreException, ObjectWithIdNotFoundException {
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		writeDao.add(BsxTestConstants.TO_DTO_1);
		assertTrue(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		final PreparedDto<TestObjectDto> preparedDto = writeDao.
				getById(BsxTestConstants.TO_DTO_1.getId());
		final TestObjectDto dto =  preparedDto.getDto();
		assertNotNull(dto);
		assertEquals(BsxTestConstants.TO_DTO_1.getId(), dto.getId());
		assertEquals(BsxTestConstants.TO_DTO_1.toString(), dto.toString());
		writeDao.delete(BsxTestConstants.TO_DTO_1.getId());
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
	}

	@Test
	public void test_3_pagination() {
		throw new IllegalArgumentException();
	}

	@Test
	public void test_4_streaming() {
		throw new IllegalArgumentException();
	}

	@Test
	public void test_5_update() throws StoreException, ObjectWithIdNotFoundException {
		assertFalse(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));
		writeDao.add(BsxTestConstants.TO_DTO_1);
		assertTrue(writeDao.exists(BsxTestConstants.TO_DTO_1.getId()));

		final String originalLabel = BsxTestConstants.TO_DTO_1.getLabel();

		// Query dto
		final PreparedDto<TestObjectDto> preparedDto = writeDao.
				getById(BsxTestConstants.TO_DTO_1.getId());
		assertNull(preparedDto.getDto().getReplacedBy());
		// Change its label
		preparedDto.getDto().setLabel("NEW LABEL");
		assertEquals("NEW LABEL", preparedDto.getDto().getLabel());
		// Write back
		final TestObjectDto newDto = writeDao.update(preparedDto.getDto());
		assertEquals("NEW LABEL", newDto.getLabel());

		// Check that the old one still exists, same ID, same label, but with a reference
		final PreparedDto<TestObjectDto> preparedOldDto = writeDao.
				getById(BsxTestConstants.TO_DTO_1.getId());
		assertEquals(originalLabel, preparedOldDto.getDto().getLabel());
		assertFalse("NEW LABEL".equals(preparedOldDto.getDto().getLabel()));
		assertNotNull(preparedOldDto.getDto().getReplacedBy());
		assertEquals(newDto.getId(), preparedOldDto.getDto().getReplacedBy().getId());

		// And check that the new one exists
		assertEquals("NEW LABEL", ((TestObjectDto)preparedOldDto.getDto().getReplacedBy()).getLabel());

		// query and compare new one
		final PreparedDto<TestObjectDto> preparedNewDto = writeDao.
				getById(newDto.getId());
		assertEquals(newDto.toString(), preparedNewDto.getDto().toString());
	}

	@Test
	public void test_6_updateWithReset() throws StoreException, ObjectWithIdNotFoundException {
		DATA_STORAGE.reset();
		throw new IllegalArgumentException();
	}
}
