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

import static de.interactive_instruments.etf.test.TestDtos.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dto.result.AttachmentDto;
import de.interactive_instruments.etf.dal.dto.result.TestResultStatus;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.TestRunLogger;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResultCollectorTest {

    static IFile attachmentDir = null;
    private static Dao<TestTaskResultDto> dao;

    private static TestRunLogger loggerMock = Mockito.mock(TestRunLogger.class);

    @BeforeClass
    public static void setUp() throws IOException, InvalidStateTransitionException, StorageException, InitializationException,
            ConfigurationException, ObjectWithIdNotFoundException {

        BsxTestUtils.ensureInitialization();
        dao = BsxTestUtils.DATA_STORAGE.getDao(TestTaskResultDto.class);
        BsxTestUtils.forceDeleteAndAdd(TAG_DTO_1);
        BsxTestUtils.forceDeleteAndAdd(TAG_DTO_2);
        BsxTestUtils.forceDeleteAndAdd(TOT_DTO_1);
        BsxTestUtils.forceDeleteAndAdd(TOT_DTO_2);
        BsxTestUtils.forceDeleteAndAdd(TOT_DTO_3);
        BsxTestUtils.forceDeleteAndAdd(TASK_DTO_1.getTestObject());

        attachmentDir = IFile.createTempDir("etf-bsxds-test");

        Mockito.when(loggerMock.getLogFile()).thenReturn(new IFile(attachmentDir.expandPath("log.txt")));
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (attachmentDir != null) {
            // attachmentDir.deleteDirectory();
        }
    }

    @Test
    public void testCollector() throws IOException, ObjectWithIdNotFoundException, StorageException {
        final TestResultCollector c = new BsxDsResultCollector(BsxTestUtils.DATA_STORAGE,
                loggerMock, attachmentDir.expandPath("Result2.xml"), attachmentDir, TASK_DTO_1);

        // Start Test Task
        final String id = c.startTestTask(ETS_DTO_1.getId().getId());
        assertEquals(1, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Start Test Module
        c.startTestModule(ETS_DTO_1.getTestModules().get(0).getId().getId());
        assertEquals(2, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Start Test Case
        c.startTestCase(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId());
        assertEquals(3, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Start Test Step (1)
        c.startTestStep(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId());
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Start assertion
        c.startTestAssertion(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions()
                .get(0).getId().getId());
        assertEquals(5, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));
        c.addMessage("TR.Template.1", "TOKEN.1", "Value.1", "TOKEN.2", "Value.2", "TOKEN.3", "Value.3");
        c.saveAttachment(new StringReader("Message in Attachment"), "Message.1", "text/plain", "Message");
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(0).getId()
                .getId(), TestResultStatus.PASSED_MANUAL.value());
        assertEquals(TestResultStatus.PASSED_MANUAL, c.status(""));

        // Still in Test Step context
        assertEquals(4, c.currentModelType());

        // Start assertion
        c.startTestAssertion(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions()
                .get(1).getId().getId());
        assertEquals(5, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));
        c.addMessage("TR.Template.1", "TOKEN.1", "Value.1", "TOKEN.2", "Value.2", "TOKEN.3", "Value.3");
        c.saveAttachment(new StringReader("Message in Attachment"), "Message.1", "text/plain", "Message");
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(1).getId()
                .getId(), TestResultStatus.PASSED_MANUAL.value());
        assertEquals(TestResultStatus.PASSED_MANUAL, c.status(""));

        // End Test Step, back in Test Case context
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId());
        assertEquals(3, c.currentModelType());
        assertEquals(TestResultStatus.PASSED_MANUAL, c.status(""));

        // Test calling another Test Step
        // Start Test Step (2)
        c.startTestStep(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(1).getId().getId());
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Call a Test Step (3)
        c.startTestStep(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getId().getId());
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Start assertion
        c.startTestAssertion(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getTestAssertions()
                .get(0).getId().getId());
        assertEquals(5, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));
        c.addMessage("TR.Template.1", "TOKEN.1", "Value.1", "TOKEN.2", "Value.2", "TOKEN.3", "Value.3");
        c.saveAttachment(new StringReader("Message in Attachment"), "Message.1", "text/plain", "Message");
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getTestAssertions().get(0).getId()
                .getId(), TestResultStatus.FAILED.value());
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // In Test Step (3) context
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // End Test Step call (3)
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getId().getId());
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // End Test Step (2), back in Test Case context
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(1).getId().getId());
        assertEquals(3, c.currentModelType());
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // End Test Case
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId());
        assertEquals(2, c.currentModelType());
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // Start another Test Case which depends on the first one
        c.startTestCase(ETS_DTO_1.getTestModules().get(0).getTestCases().get(1).getId().getId());
        assertEquals(3, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Start Test Step
        c.startTestStep(ETS_DTO_1.getTestModules().get(0).getTestCases().get(1).getTestSteps().get(0).getId().getId());
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Call first Test Case
        c.startTestCase(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId());
        assertEquals(3, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Start Test Step (1)
        c.startTestStep(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId());
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.UNDEFINED, c.status(""));

        // Add an attachment
        c.saveAttachment("Test String", "Label", "text/plain", null);

        // Fail Test Step (1)
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId(),
                TestResultStatus.FAILED.value());
        assertEquals(3, c.currentModelType());
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // End called, first Test Case
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId());
        assertEquals(4, c.currentModelType());
        assertEquals(TestResultStatus.SKIPPED, c.status(""));

        // End Test Step, back in Test Case context
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(1).getTestSteps().get(0).getId().getId());
        assertEquals(3, c.currentModelType());
        // failed test case = skipped
        assertEquals(TestResultStatus.SKIPPED, c.status(""));

        // End Test Case, back in Test Module context
        c.end(ETS_DTO_1.getTestModules().get(0).getTestCases().get(1).getId().getId());
        assertEquals(2, c.currentModelType());
        // aggregated status of all Test Cases in the context
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // End Test Module
        c.end(ETS_DTO_1.getTestModules().get(0).getId().getId());
        assertEquals(1, c.currentModelType());
        // first Test Case failed
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // End Test Task
        c.end(ETS_DTO_1.getId().getId());
        assertEquals(-1, c.currentModelType());
        assertEquals(TestResultStatus.FAILED, c.status(""));

        // Get TestTaskResult
        final TestTaskResultDto result = dao.getById(EidFactory.getDefault().createUUID(id)).getDto();
        assertEquals(id, result.getId().getId());
    }

    @Test
    public void testCollectorInternalError() throws IOException, ObjectWithIdNotFoundException, StorageException {
        final TestResultCollector c = new BsxDsResultCollector(BsxTestUtils.DATA_STORAGE,
                loggerMock, attachmentDir.expandPath("Result3.xml"), attachmentDir, TASK_DTO_1);
        final String id = c.internalError("Error message", "ERROR message in file".getBytes(), "text/plain");

        final TestTaskResultDto result = dao.getById(EidFactory.getDefault().createUUID(id)).getDto();
        assertEquals("Error message", result.getErrorMessage());
        assertNotNull(result.getAttachments());
        // Log file and error message in file
        assertEquals(2, result.getAttachments().size());
        AttachmentDto attachment = null;
        for (final AttachmentDto a : result.getAttachments()) {
            if ("internalError".equals(a.getType())) {
                attachment = a;
            }
        }
        assertNotNull(attachment);
        assertEquals("Internal error", attachment.getLabel());
        assertEquals("ERROR message in file", new IFile(attachment.getReferencedData()).readContent().toString());
    }

}
