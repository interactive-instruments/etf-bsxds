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
import de.interactive_instruments.etf.testdriver.AbstractTestCaseResultCollector;
import de.interactive_instruments.etf.testdriver.AbstractTestCollector;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
final class BsxDsTestCaseResultCollector extends AbstractTestCaseResultCollector {

	private final XmlTestResultWriter writer;
	private final File testCaseResultFile;
	private final ByteArrayOutputStream bos;
	private final AbstractTestCaseResultCollector parentCollector;

	/**
	 * Ctor for called Test Cases
	 *
	 * @param parentCollector
	 */
	/*
	BsxDsTestCaseResultCollector(final AbstractTestCaseResultCollector parentCollector, final IFile testCaseResultFile) {
		super(TestCaseResultCollectorState.WRITING_TEST_CASE_RESULT);
		this.parentCollector = parentCollector;
		bos = null;
		this.testCaseResultFile = testCaseResultFile;
		try {
			final FileOutputStream fos = new FileOutputStream(testCaseResultFile);
			writer = new XmlTestResultWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(fos, "UTF-8"));
		}catch (XMLStreamException | IOException e) {
			throw new IllegalStateException(e);
		}
	}
	*/

	/**
	 * Ctor for called Test Cases
	 *
	 * @param parentCollector
	 */
	BsxDsTestCaseResultCollector(final AbstractTestCaseResultCollector parentCollector) {
		this.parentCollector = parentCollector;
		bos = new ByteArrayOutputStream(512);
		testCaseResultFile = null;
		try {
			writer = new XmlTestResultWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(bos, "UTF-8"));
		}catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	public void writeTo(OutputStream outputStream) throws IOException {
		if(bos!=null) {
			bos.flush();
			bos.writeTo(outputStream);
		}else{
			IOUtils.copy(new FileInputStream(testCaseResultFile), outputStream);
		}
	}

	@Override protected String startTestCaseResult(final String resultedFrom, final long startTimestamp) throws Exception {
		return writer.writeStartTestCaseResult(resultedFrom, startTimestamp);
	}

	@Override protected String startTestStepResult(final String resultedFrom, final long startTimestamp) throws Exception {
		return writer.writeStartTestStepResult(resultedFrom, startTimestamp);
	}

	@Override protected String startTestAssertionResult(final String resultedFrom, final long startTimestamp) throws Exception {
		return writer.writeStartTestAssertionResult(resultedFrom, startTimestamp);
	}

	@Override protected String finishTestCaseResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception {
		final String id =  writer.writeEndTestCaseResult(testModelItemId, status, stopTimestamp);
		writer.close();
		return id;
	}

	@Override protected String finishTestStepResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception {
		return writer.writeEndTestStepResult(testModelItemId, status, stopTimestamp);
	}

	@Override protected String finishTestAssertionResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception {
		return writer.writeEndTestAssertionResult(testModelItemId, status, stopTimestamp);
	}

	@Override protected void notifyError() {
		// TODO
	}

	@Override protected AbstractTestCollector createCalledTestCaseResultCollector(final AbstractTestCollector parentCollector, final String testModelItemId, final long startTimestamp) {
		return new BsxDsTestCaseResultCollector(this);
	}

	@Override protected AbstractTestCollector createCalledTestStepResultCollector(final AbstractTestCollector parentCollector, final String testModelItemId, final long startTimestamp) {
		// TODO
		return new BsxDsTestCaseResultCollector(this);
	}

	@Override protected void mergeResultFromCollector(final AbstractTestCollector collector) {
		// TODO
	}

	@Override protected String currentResultItemId() {
		// TODO
		return null;
	}
}
