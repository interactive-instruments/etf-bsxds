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

import static de.interactive_instruments.etf.dal.dao.basex.BsxTestUtils.*;
import static de.interactive_instruments.etf.test.TestDtos.*;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import junit.framework.TestCase;

import org.junit.*;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.exceptions.StoreException;
import de.interactive_instruments.etf.dal.dto.IncompleteDtoException;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTaskResultDaoTest {

    private static WriteDao<TestTaskResultDto> writeDao;

    @BeforeClass
    public static void setUp() throws ConfigurationException, InvalidStateTransitionException, InitializationException,
            StorageException, ObjectWithIdNotFoundException, IOException {
        BsxTestUtils.ensureInitialization();
        writeDao = ((WriteDao) DATA_STORAGE.getDao(TestTaskResultDto.class));

        ExecutableTestSuiteDaoTest.setUp();

        BsxTestUtils.forceDeleteAndAdd(ETS_DTO_1);
        BsxTestUtils.forceDeleteAndAdd(ETS_DTO_2);

        BsxTestUtils.forceDeleteAndAdd(TO_DTO_1);
    }

    @AfterClass
    public static void tearDown()
            throws InvalidStateTransitionException, StorageException, InitializationException, ConfigurationException {
        ExecutableTestSuiteDaoTest.tearDown();
    }

    @Before
    public void clean() {
        try {
            BsxTestUtils.forceDelete(writeDao, TTR_DTO_1.getId());
            BsxTestUtils.forceDelete(writeDao, TTR_DTO_2.getId());
            BsxTestUtils.forceDelete(DATA_STORAGE.getDao(TestTaskResultDto.class), TR_DTO_1.getId());
        } catch (StorageException e) {}
    }

    @Test
    public void test_1_1_existsAndAddAndDelete() throws StorageException, ObjectWithIdNotFoundException {
        BsxTestUtils.existsAndAddAndDeleteTest(TTR_DTO_1);
    }

    @Test
    public void test_2_0_add_and_get() throws StorageException, ObjectWithIdNotFoundException {
        BsxTestUtils.forceDeleteAndAdd(TO_DTO_1);
        BsxTestUtils.forceDeleteAndAdd(ETS_DTO_1);
        BsxTestUtils.forceDeleteAndAdd(ETS_DTO_2);
        // TestTask required for parent reference
        BsxTestUtils.forceDeleteAndAdd(TR_DTO_1, false);

        final PreparedDto<TestTaskResultDto> preparedDto = BsxTestUtils.addAndGetByIdTest(TTR_DTO_1);

        assertNotNull(preparedDto.getDto().getAttachments());
        assertEquals(1, preparedDto.getDto().getAttachments().size());
        assertNotNull(preparedDto.getDto().getTestObject());
        // force proxied DTO to resolve
        assertNotNull(preparedDto.getDto().getTestObject().getLabel());
        if (ProxyAccessor.class.isAssignableFrom(preparedDto.getDto().getTestObject().getClass())) {
            assertEquals(TO_DTO_1.toString().trim(),
                    ((ProxyAccessor) preparedDto.getDto().getTestObject()).getCached().toString().trim());
        } else {
            assertEquals(TO_DTO_1.toString().trim(), preparedDto.getDto().getTestObject().toString().trim());
        }
        assertNotNull(preparedDto.getDto().getTestModuleResults());

        writeDao.delete(TTR_DTO_1.getId());
        TestCase.assertFalse(writeDao.exists(TTR_DTO_1.getId()));
    }

    // @Test
    public void test_4_0_streaming() throws StorageException, ObjectWithIdNotFoundException, IOException, URISyntaxException {
        BsxTestUtils.forceDeleteAndAdd(TO_DTO_1);
        BsxTestUtils.forceDeleteAndAdd(ETS_DTO_1);
        BsxTestUtils.forceDeleteAndAdd(ETS_DTO_2);
        // TestTask required for parent reference
        BsxTestUtils.forceDeleteAndAdd(TR_DTO_1, false);

        final PreparedDto<TestTaskResultDto> preparedDto = BsxTestUtils.addAndGetByIdTest(TTR_DTO_1);

        final IFile tmpFile = IFile.createTempFile("etf", ".html");
        tmpFile.deleteOnExit();
        final FileOutputStream fop = new FileOutputStream(tmpFile);

        OutputFormat htmlReportFormat = null;
        for (final OutputFormat outputFormat : writeDao.getOutputFormats().values()) {
            if ("text/html".equals(outputFormat.getMediaTypeType().getType())) {
                htmlReportFormat = outputFormat;
                break;
            }
        }
        assertNotNull(htmlReportFormat);

        preparedDto.streamTo(htmlReportFormat, null, fop);

        final IFile cmpResult = new IFile(getClass().getClassLoader().getResource("cmp/TestTaskResult.html").toURI());
        assertTrue(cmpResult.exists());

        assertEquals(trimAllWhitespace(cmpResult.readContent().toString()),
                trimAllWhitespace(tmpFile.readContent().toString()));
    }

    @Test(expected = StoreException.class)
    public void test_7_1_stream_file_to_store()
            throws StorageException, ObjectWithIdNotFoundException, FileNotFoundException, IncompleteDtoException,
            URISyntaxException {
        final IFile taskTestResultFile = getTestResourceFile(
                "database/testtaskresult.xml");
        ((StreamWriteDao<TestTaskResultDto>) writeDao).add(new FileInputStream(taskTestResultFile));
    }

}
