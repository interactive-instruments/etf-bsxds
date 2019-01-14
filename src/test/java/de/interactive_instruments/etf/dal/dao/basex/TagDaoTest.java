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
import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.getTestResourceFile;
import static de.interactive_instruments.etf.test.TestDtos.TAG_DTO_1;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.*;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TagDaoTest {

    private static WriteDao<TagDto> writeDao;

    private static EID streamingTagId = EidFactory.getDefault().createUUID("EIDfe1f3796-0ebf-4960-a6f7-f935e087fa4b");

    private final static Filter ALL = new Filter() {
        @Override
        public int offset() {
            return 0;
        }

        @Override
        public int limit() {
            return 2000;
        }
    };

    @BeforeClass
    public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException,
            IOException {
        BsxTestUtils.ensureInitialization();
        writeDao = ((WriteDao) DATA_STORAGE.getDao(TagDto.class));
    }

    @Before
    public void clean() {
        try {
            writeDao.delete(TAG_DTO_1.getId());
        } catch (ObjectWithIdNotFoundException | StorageException e) {}
    }

    @Test
    public void test_1_1_existsAndAddAndDelete() throws StorageException, ObjectWithIdNotFoundException {
        BsxTestUtils.existsAndAddAndDeleteTest(TAG_DTO_1);
    }

    @Test
    public void test_2_0_getById() throws StorageException, ObjectWithIdNotFoundException {
        assertFalse(writeDao.exists(TAG_DTO_1.getId()));
        writeDao.add(TAG_DTO_1);
        assertTrue(writeDao.exists(TAG_DTO_1.getId()));

        final PreparedDto<TagDto> preparedDto = writeDao.getById(TAG_DTO_1.getId());

        // Check internal ID
        assertEquals(TAG_DTO_1.getId(), preparedDto.getDtoId());
        final TagDto dto = preparedDto.getDto();
        assertNotNull(dto);
        assertEquals(TAG_DTO_1.getId(), dto.getId());
        assertEquals(TAG_DTO_1.toString(), dto.toString());
    }

    @Test
    public void test_2_1_delete() throws StorageException, ObjectWithIdNotFoundException {
        writeDao.deleteAllExisting(Collections.singleton(TAG_DTO_1.getId()));
        assertFalse(writeDao.exists(TAG_DTO_1.getId()));
        assertFalse(writeDao.isDisabled(TAG_DTO_1.getId()));
        assertFalse(writeDao.available(TAG_DTO_1.getId()));

        boolean exceptionThrown = false;
        try {
            writeDao.getById(TAG_DTO_1.getId()).getDto();
        } catch (ObjectWithIdNotFoundException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);

        writeDao.add(TAG_DTO_1);
        assertTrue(writeDao.exists(TAG_DTO_1.getId()));
        assertFalse(writeDao.isDisabled(TAG_DTO_1.getId()));
        assertTrue(writeDao.available(TAG_DTO_1.getId()));
        assertEquals(TAG_DTO_1.getLabel(), writeDao.getById(TAG_DTO_1.getId()).getDto().getLabel());
    }

    @Test
    public void test_2_2_doubleAdd() throws StorageException, ObjectWithIdNotFoundException {
        writeDao.deleteAllExisting(Collections.singleton(TAG_DTO_1.getId()));
        assertFalse(writeDao.exists(TAG_DTO_1.getId()));
        assertFalse(writeDao.isDisabled(TAG_DTO_1.getId()));
        assertFalse(writeDao.available(TAG_DTO_1.getId()));

        writeDao.add(TAG_DTO_1);
        assertTrue(writeDao.exists(TAG_DTO_1.getId()));
        assertFalse(writeDao.isDisabled(TAG_DTO_1.getId()));
        assertTrue(writeDao.available(TAG_DTO_1.getId()));
        assertEquals(TAG_DTO_1.getLabel(), writeDao.getById(TAG_DTO_1.getId()).getDto().getLabel());

        boolean exceptionThrown = false;
        try {
            writeDao.add(TAG_DTO_1);
        } catch (StorageException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        assertTrue(writeDao.exists(TAG_DTO_1.getId()));
        assertFalse(writeDao.isDisabled(TAG_DTO_1.getId()));
        assertTrue(writeDao.available(TAG_DTO_1.getId()));
        assertEquals(TAG_DTO_1.getLabel(), writeDao.getById(TAG_DTO_1.getId()).getDto().getLabel());
    }

    @Test
    public void test_7_1_testStreamIntoStore()
            throws StorageException, ObjectWithIdNotFoundException, FileNotFoundException, URISyntaxException {
        writeDao.deleteAllExisting(Collections.singleton(streamingTagId));
        final IFile tagFile = getTestResourceFile("database/tag.xml");
        ((StreamWriteDao<TagDto>) writeDao).add(new FileInputStream(tagFile));
        assertEquals("TAG LABEL", writeDao.getById(streamingTagId).getDto().getLabel());
        final PreparedDtoCollection<TagDto> collectionResult = writeDao.getAll(ALL);
        assertTrue(collectionResult.keySet().contains(streamingTagId));
    }

    @Test
    public void test_7_2_testDoubleStreamIntoStore()
            throws StorageException, ObjectWithIdNotFoundException, FileNotFoundException, URISyntaxException {

        final int sizeBefore = writeDao.getAll(ALL).size();
        final IFile tagFile = getTestResourceFile("database/tag.xml");
        ((StreamWriteDao<TagDto>) writeDao).add(new FileInputStream(tagFile));

        // assertTrue(exceptionThrown);
        assertEquals("TAG LABEL", writeDao.getById(streamingTagId).getDto().getLabel());
        final PreparedDtoCollection<TagDto> collectionResult = writeDao.getAll(ALL);
        assertEquals(sizeBefore, collectionResult.size());
        assertTrue(collectionResult.keySet().contains(streamingTagId));
    }

}
