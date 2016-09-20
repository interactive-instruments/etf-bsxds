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
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class ResultCollectorTest {

	static IFile attachmentDir = null;
	private static Dao<TestTaskResultDto> dao;

	@BeforeClass
	public static void setUp() throws IOException, InvalidStateTransitionException, StorageException, InitializationException, ConfigurationException {
		BsxTestUtil.ensureInitialization();
		dao = BsxTestUtil.DATA_STORAGE.getDao(TestTaskResultDto.class);
		attachmentDir = IFile.createTempDir("etf-bsxds-test");
	}

	@AfterClass
	public static void tearDown() throws IOException {
		if(attachmentDir!=null) {
			// attachmentDir.deleteDirectory();
		}
	}

	@Test
	public void testCollector() throws IOException, ObjectWithIdNotFoundException, StorageException {
		final TestResultCollector testResultCollector = new BsxDsResultCollector(BsxTestUtil.DATA_STORAGE,
				new TestTestRunLogger(),attachmentDir.expandPath("Result.xml"), attachmentDir, BsxTestUtil.TASK_DTO_1);
		final String id = testResultCollector.start(BsxTestUtil.ETS_DTO_1.getId().getId());

		testResultCollector.start(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getId().getId());

		testResultCollector.start(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId());

		testResultCollector.start(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId());

		testResultCollector.start(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(0).getId().getId());

		testResultCollector.addMessage("TR.Template.1", "TOKEN.1", "Value.1", "TOKEN.2", "Value.2", "TOKEN.3", "Value.3");
		testResultCollector.saveAttachment(new StringReader("Message in Attachment"), "Message.1", "text/plain", "Message");

		testResultCollector.end(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getTestAssertions().get(0).getId().getId(), 2);

		testResultCollector.end(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getTestSteps().get(0).getId().getId(), 2);

		testResultCollector.end(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getTestCases().get(0).getId().getId(), 2);

		testResultCollector.end(BsxTestUtil.ETS_DTO_1.getTestModules().get(0).getId().getId(), 2);

		testResultCollector.end(BsxTestUtil.ETS_DTO_1.getId().getId(), 2);

		// Get TestTaskResult
		final TestTaskResultDto result = dao.getById(EidFactory.getDefault().createUUID(id)).getDto();
		assertEquals(id, result.getId().getId());

	}


}
