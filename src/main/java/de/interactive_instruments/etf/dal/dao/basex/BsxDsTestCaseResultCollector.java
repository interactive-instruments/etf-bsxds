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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.testdriver.AbstractTestCaseResultCollector;
import de.interactive_instruments.etf.testdriver.AbstractTestCollector;
import de.interactive_instruments.etf.testdriver.TestTaskEndListener;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class BsxDsTestCaseResultCollector extends AbstractTestCaseResultCollector implements BsxDsResultCollectorWriter {

	private final XmlTestResultWriter writer;
	private final ByteArrayOutputStream bos;
	private final List<String> testStepAttachmentIds;

	/**
	 * Ctor for called Test Cases
	 *
	 * @param parentCollector
	 * @param testStepAttachmentIds
	 */
	BsxDsTestCaseResultCollector(final AbstractTestCollector parentCollector, final List<String> testStepAttachmentIds,
			final IFile testCaseResultFile, final String testModelItemId, final long startTimestamp) {
		this(parentCollector, testStepAttachmentIds, testModelItemId, startTimestamp);
	}

	/**
	 * Ctor for called Test Cases
	 *
	 * @param parentCollector
	 * @param testStepAttachmentIds
	 */
	BsxDsTestCaseResultCollector(final AbstractTestCollector parentCollector, final List<String> testStepAttachmentIds,
			final String testCaseId, final long startTimestamp) {
		super(parentCollector, testCaseId);
		this.testStepAttachmentIds = testStepAttachmentIds;
		bos = new ByteArrayOutputStream(512);
		try {
			writer = new XmlTestResultWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(bos, "UTF-8"), 100);
			writer.writeStartTestCaseResult(testCaseId, startTimestamp);
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	public void writeTo(OutputStream outputStream) throws IOException {
		bos.writeTo(outputStream);
	}

	@Override
	protected String doStartTestCaseResult(final String resultedFrom, final long startTimestamp) throws Exception {
		return writer.writeStartTestCaseResult(resultedFrom, startTimestamp);
	}

	@Override
	protected String doStartTestStepResult(final String resultedFrom, final long startTimestamp) throws Exception {
		return writer.writeStartTestStepResult(resultedFrom, startTimestamp);
	}

	@Override
	protected String doStartTestAssertionResult(final String resultedFrom, final long startTimestamp) throws Exception {
		return writer.writeStartTestAssertionResult(resultedFrom, startTimestamp);
	}

	@Override
	protected String endTestCaseResult(final String testModelItemId, final int status, final long stopTimestamp)
			throws Exception {
		return writer.writeEndTestCaseResult(testModelItemId, status, stopTimestamp);
	}

	@Override
	protected String endTestStepResult(final String testModelItemId, final int status, final long stopTimestamp)
			throws Exception {
		writer.finalizeMessages();
		if (!testStepAttachmentIds.isEmpty()) {
			writer.addAttachmentRefs(testStepAttachmentIds);
			testStepAttachmentIds.clear();
		}
		return writer.writeEndTestStepResult(testModelItemId, status, stopTimestamp);
	}

	@Override
	protected String endTestAssertionResult(final String testModelItemId, final int status, final long stopTimestamp)
			throws Exception {
		writer.finalizeMessages();
		return writer.writeEndTestAssertionResult(testModelItemId, status, stopTimestamp);
	}

	@Override
	protected void startInvokedTests() {
		try {
			writer.writeStartInvokedTests();
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void endInvokedTests() {
		try {
			writer.writeEndInvokedTests();
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void startTestAssertionResults() {
		try {
			writer.writeStartTestAssertionResults();
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void endTestAssertionResults() {
		try {
			writer.writeEndTestAssertionResults();
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void doAddMessage(final String s) {
		writer.addMessage(s);
	}

	@Override
	protected void doAddMessage(final String s, final Map<String, String> map) {
		writer.addMessage(s, map);
	}

	@Override
	protected void doAddMessage(final String s, final String... strings) {
		writer.addMessage(s, strings);
	}

	@Override
	protected void notifyError() {}

	@Override
	protected AbstractTestCollector createCalledTestCaseResultCollector(final AbstractTestCollector parentCollector,
			final String testModelItemId, final long startTimestamp) {
		return new BsxDsTestCaseResultCollector(this, testStepAttachmentIds, testModelItemId, startTimestamp);
	}

	@Override
	protected AbstractTestCollector createCalledTestStepResultCollector(final AbstractTestCollector parentCollector,
			final String testModelItemId, final long startTimestamp) {
		return new BsxDsTestStepResultCollector(this, testStepAttachmentIds, testModelItemId, startTimestamp);
	}

	@Override
	protected void mergeResultFromCollector(final AbstractTestCollector collector) {
		try {
			writer.flush();
			((BsxDsResultCollectorWriter) collector).writeTo(bos);
		} catch (Exception e) {
			logger.error("Failed to append collector results: ", e);
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected String currentResultItemId() {
		return writer.currentResultItemId();
	}

	@Override
	public void release() {

	}

	@Override
	public void registerTestTaskEndListener(final TestTaskEndListener listener) {
		throw new UnsupportedOperationException(
				"Operation not supported by collector, "
						+ "illegal delegation from parent collector");
	}
}
