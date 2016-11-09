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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import de.interactive_instruments.etf.testdriver.TestRunLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import org.mockito.Mockito;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class ResultCollectorTest {

	static IFile attachmentDir = null;
	private static Dao<TestTaskResultDto> dao;

	private static TestRunLogger loggerMock = Mockito.mock(TestRunLogger.class);

	@BeforeClass
	public static void setUp() throws IOException, InvalidStateTransitionException, StorageException, InitializationException, ConfigurationException {
		BsxTestUtils.ensureInitialization();
		dao = BsxTestUtils.DATA_STORAGE.getDao(TestTaskResultDto.class);
		attachmentDir = IFile.createTempDir("etf-bsxds-test");
	}

	@AfterClass
	public static void tearDown() throws IOException {
		if (attachmentDir != null) {
			// attachmentDir.deleteDirectory();
		}
	}

	@Test
	public void testCollectorStates() throws IOException, ObjectWithIdNotFoundException, StorageException {
		final TestResultCollector c = new BsxDsResultCollector(Mockito.mock(BsxDataStorage.class),
				loggerMock, attachmentDir.expandPath("Result1.xml"), attachmentDir, BsxTestUtils.TASK_DTO_1);



	}



	@Test
	public void testCollectorWithPersistance() throws IOException, ObjectWithIdNotFoundException, StorageException {
		final TestResultCollector c = new BsxDsResultCollector(BsxTestUtils.DATA_STORAGE,
				loggerMock, attachmentDir.expandPath("Result2.xml"), attachmentDir, BsxTestUtils.TASK_DTO_1);

		// Start Test Task
		final String id = c.startTestTask(BsxTestUtils.ETS_DTO_1.getId().getId());
		assertEquals(1, c.currentModelType());

		// Start Test Module
		c.startTestModule(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getId().getId());
		assertEquals(2, c.currentModelType());

		// Start Test Case
		c.startTestCase(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId());
		assertEquals(3, c.currentModelType());

		// Start Test Step (1)
		c.startTestStep(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId());
		assertEquals(4, c.currentModelType());

		// Start assertion
		c.startTestAssertion(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(0).getId().getId());
		assertEquals(5, c.currentModelType());
		c.addMessage("TR.Template.1", "TOKEN.1", "Value.1", "TOKEN.2", "Value.2", "TOKEN.3", "Value.3");
		c.saveAttachment(new StringReader("Message in Attachment"), "Message.1", "text/plain", "Message");
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(0).getId().getId(), 2);

		// Still in Test Step context
		assertEquals(4, c.currentModelType());

		// Start assertion
		c.startTestAssertion(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(1).getId().getId());
		assertEquals(5, c.currentModelType());
		c.addMessage("TR.Template.1", "TOKEN.1", "Value.1", "TOKEN.2", "Value.2", "TOKEN.3", "Value.3");
		c.saveAttachment(new StringReader("Message in Attachment"), "Message.1", "text/plain", "Message");
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(1).getId().getId(), 1);

		// End Test Step, back in Test Case context
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId(), 2);
		assertEquals(3, c.currentModelType());

		// Test calling another Test Step

		// Start Test Step (2)
		c.startTestStep(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(1).getId().getId());
		assertEquals(4, c.currentModelType());

		// Call a Test Step (3)
		c.startTestStep(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getId().getId());
		assertEquals(4, c.currentModelType());

		// Start assertion
		c.startTestAssertion(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getTestAssertions().get(0).getId().getId());
		assertEquals(5, c.currentModelType());
		c.addMessage("TR.Template.1", "TOKEN.1", "Value.1", "TOKEN.2", "Value.2", "TOKEN.3", "Value.3");
		c.saveAttachment(new StringReader("Message in Attachment"), "Message.1", "text/plain", "Message");
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getTestAssertions().get(0).getId().getId(), 2);

		// In Test Step (3) context
		assertEquals(4, c.currentModelType());

		// End Test Step call (3)
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(2).getId().getId(), 2);
		assertEquals(4, c.currentModelType());

		// End Test Step (2), back in Test Case context
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(1).getId().getId(), 2);
		assertEquals(3, c.currentModelType());

		// End Test Case
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId(), 2);
		assertEquals(2, c.currentModelType());

		// End Test Module
		c.end(BsxTestUtils.ETS_DTO_1.getTestModules().get(0).getId().getId(), 2);
		assertEquals(1, c.currentModelType());

		// End Test Task
		c.end(BsxTestUtils.ETS_DTO_1.getId().getId(), 2);
		assertEquals(-1, c.currentModelType());

		// Get TestTaskResult
		final TestTaskResultDto result = dao.getById(EidFactory.getDefault().createUUID(id)).getDto();
		assertEquals(id, result.getId().getId());

	}

}
