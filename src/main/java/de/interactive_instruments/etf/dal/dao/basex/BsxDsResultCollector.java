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

package de.interactive_instruments.etf.testdriver;

import de.interactive_instruments.IFile;
import de.interactive_instruments.MimeTypeUtils;
import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.etf.dal.dto.result.TestResultStatus;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.MimeTypeUtilsException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.*;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public final class BsxDsResultCollector extends AbstractTestResultCollector {

	private static final String ETF_NS = "http://www.interactive-instruments.de/etf/2.0";
	private static final String ETF_RESULT_XSD = "http://services.interactive-instruments.de/etf/schema/model/result.xsd";
	private static final String ETF_NS_PREFIX = "etf";

	private final TestRunLogger logger;
	private final IFile tmpDir;
	private final IFile attachmentDir;
	private final IFile resultFile;
	private final XMLStreamWriter writer;
	private final Random random = new Random();
	private final TestTaskDto testTaskDto;
	private final Deque<ResultModelItem> results = new LinkedList<>();
	private final Map<String, Attachment> attachments = new HashMap<>();

	private final class ResultModelItem {
		private final String id;
		private final long startTimestamp;
		private final String resultedFrom;

		ResultModelItem(final String id, final long currentTime, final String resultedFrom) {
			this.id = id;
			this.startTimestamp = currentTime;
			this.resultedFrom = resultedFrom;
		}

		void write(final int status, final long stopTimestamp) throws XMLStreamException {
			writer.writeStartElement("resultedFrom");
			writer.writeAttribute("ref", resultedFrom);
			writer.writeEndElement();

			writer.writeStartElement("startTimestamp");
			writer.writeCharacters(TimeUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(startTimestamp));
			writer.writeEndElement();

			writer.writeStartElement("duration");
			writer.writeCharacters(String.valueOf(stopTimestamp-startTimestamp));
			writer.writeEndElement();

			writer.writeStartElement("status");
			writer.writeCharacters(TestResultStatus.toString(status));
			writer.writeEndElement();
		}

		public String getResultedFromId() {
			return id;
		}
	}

	private final class Attachment {
		private final IFile attachmentFile;
		private final String id;
		private final String label;
		private final String encoding;
		private final String mimeType;
		private final String type;

		public Attachment(final String id, final IFile attachmentFile, final String label, final String encoding, final String mimeType, final String type) {
			this.id = id;
			this.attachmentFile = attachmentFile;
			this.label = label;
			this.encoding = encoding;
			this.mimeType = mimeType;
			this.type = type;
		}

		void write() throws XMLStreamException {
			writer.writeStartElement("Attachment");
			if(type!=null) {
				writer.writeAttribute("type", type);
			}
			writer.writeAttribute("id", id);

			writer.writeStartElement("label");
			writer.writeCharacters(label);
			writer.writeEndElement();

			writer.writeStartElement("encoding");
			writer.writeCharacters(encoding);
			writer.writeEndElement();

			writer.writeStartElement("mimeType");
			writer.writeCharacters(mimeType);
			writer.writeEndElement();

			writer.writeStartElement("referencedData");
			writer.writeAttribute("href", attachmentFile.getAbsolutePath());
			writer.writeEndElement();
		}
	}

	public BsxDsResultCollector(final TestRunLogger logger, final IFile resultFile, final IFile tmpDir, final IFile attachmentDir, final TestTaskDto testTaskDto) throws IOException, XMLStreamException {
		this.logger = logger;
		this.tmpDir = tmpDir;
		this.attachmentDir = attachmentDir;
		this.testTaskDto = testTaskDto;
		this.resultFile = resultFile;
		this.tmpDir.setIdentifier("Test Task "+testTaskDto.getId()+" temporary directory ");

		final FileOutputStream fos = new FileOutputStream(new IFile(tmpDir, "tmp_result.xml"));
		writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos, "UTF-8");
		writer.setDefaultNamespace(ETF_NS);
		writer.setPrefix(ETF_NS_PREFIX, ETF_NS);
		writer.writeStartDocument("UTF-8", "1.0");
	}

	// TODO remove in 2.1.0
	@Deprecated
	@Override public IFile getAttachmentDir() {
		return attachmentDir;
	}

	// TODO remove in 2.1.0
	@Deprecated
	@Override public IFile getResultFile() {
		return resultFile;
	}

	// TODO remove in 2.1.0
	@Deprecated
	@Override public void finish() {

	}

	// Start writing results
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String writeEidAndMarkResultModelItem(final String resultedFrom, final long startTimestamp) throws IllegalStateException {
		try {
			long time = startTimestamp << 32;
			time |= ((startTimestamp & 0xFFFF00000000L) >> 16);
			time |= 0x1000 | ((startTimestamp >> 48) & 0x0FFF);
			final String genId = new UUID(time, random.nextLong()).toString();
			writer.writeAttribute("id", "EID"+genId);
			results.addLast(new ResultModelItem(genId, startTimestamp, resultedFrom));
			return genId;
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String startTestTaskResult(final String resultedFrom, final long startTimestamp) throws IllegalStateException {
		try {
			writer.writeStartElement("TestTaskResult");
			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
					ETF_NS+" "+ETF_RESULT_XSD);
			final String id = UUID.randomUUID().toString();
			writer.writeAttribute("id", "EID"+id);
			results.addLast(new ResultModelItem(id, startTimestamp, resultedFrom));
			writer.writeStartElement("testObject");
			writer.writeAttribute("ref", testTaskDto.getTestObject().getId().getId());
			writer.writeEndElement(); // testObject
			this.currentState=ResultListenerState.WRITING_TEST_TASK_RESULT;
			return id;
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String startTestModuleResult(final String resultedFrom, final long startTimestamp) throws IllegalStateException {
		try {
			writer.writeStartElement("TestModuleResult");
			this.currentState=ResultListenerState.WRITING_TEST_MODULE_RESULT;
			return writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String startTestCaseResult(final String resultedFrom, final long startTimestamp) throws IllegalStateException {
		try {
			writer.writeStartElement("TestCaseResult");
			this.currentState=ResultListenerState.WRITING_TEST_CASE_RESULT;
			return writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String startTestStepResult(final String resultedFrom, final long startTimestamp) throws IllegalStateException {
		try {
			writer.writeStartElement("TestStepResult");
			this.currentState=ResultListenerState.WRITING_TEST_STEP_RESULT;
			return writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String startTestAssertionResult(final String resultedFrom, final long startTimestamp) throws IllegalStateException {
		try {
			writer.writeStartElement("TestAssertionResult");
			this.currentState=ResultListenerState.WRITING_TEST_ASSERTION_RESULT;
			return writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	// Finish  results
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String finishResultModelItem(final int status, final long stopTimestamp) throws IllegalStateException {
		final ResultModelItem resultModelItem = results.removeLast();
		try {
			resultModelItem.write(status, stopTimestamp);
			writer.writeEndElement();
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
		return resultModelItem.getResultedFromId();
	}

	protected String finishTestTaskResult(final String testModelItemId, final int status, final long stopTimestamp) throws IllegalStateException {
		try {
			writer.writeStartElement("Attachments");
			for (final Attachment attachment : attachments.values()) {
				attachment.write();
			}
			writer.writeEndElement();
			final String id = finishResultModelItem(status, stopTimestamp);
			writer.writeEndDocument();
			writer.flush();
			writer.close();
			return id;
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(e);
		}
	}

	protected String finishTestModuleResult(final String testModelItemId, final int status, final long stopTimestamp) throws IllegalStateException {
		return finishResultModelItem(status, stopTimestamp);
	}

	protected String finishTestCaseResult(final String testModelItemId, final int status, final long stopTimestamp) throws IllegalStateException {
		return finishResultModelItem(status, stopTimestamp);
	}

	protected String finishTestStepResult(final String testModelItemId, final int status, final long stopTimestamp) throws IllegalStateException {
		return finishResultModelItem(status, stopTimestamp);
	}

	protected String finishTestAssertionResult(final String testModelItemId, final int status, final long stopTimestamp) throws IllegalStateException {
		return finishResultModelItem(status, stopTimestamp);
	}


	@Override public File getTempDir() {
		return tmpDir;
	}


	@Override public TestRunLogger getLogger() {
		return this.logger;
	}


	@Override public TestResultStatus status(final String testModelItemId) throws IllegalArgumentException {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public boolean statusEqualsAny(final String testModelItemId, final String... testResultStatus) throws IllegalArgumentException {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void addMessage(final String translationTemplateId) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void addMessage(final String translationTemplateId, final Map<String, String> tokenValuePairs) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void addMessage(final String translationTemplateId, final String... tokensAndValues) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public String markAttachment(final String fileName, final String label, final String encoding, final String mimeType, final String type) throws IOException {
		final IFile attachmentFile = tmpDir.secureExpandPathDown(fileName);
		attachmentFile.expectFileIsReadable();
		final String eid = UUID.randomUUID().toString();
		attachments.put(eid, new Attachment(eid, attachmentFile, label, encoding, mimeType, type));
		return eid;
	}

	@Override public String saveAttachment(final InputStream inputStream, final String label, final String mimeType, final String type) throws IOException {
		final String eid = UUID.randomUUID().toString();
		String extension=".txt";
		if(mimeType!=null) {
			try {
				extension=MimeTypeUtils.getFileExtensionForMimeType(mimeType);
			} catch (MimeTypeUtilsException e) {
				ExcUtils.suppress(e);
			}
		}
		final IFile attachmentFile = attachmentDir.secureExpandPathDown(eid + extension);
		attachmentFile.writeContent(inputStream);
		attachments.put(eid,
				new Attachment(eid, attachmentFile, label, "UTF-8", mimeType, type));
		return eid;
	}

	@Override public void internalError(final String translationTemplateId, final Map<String, String> tokenValuePairs, final Throwable e) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void internalError(final Throwable e) {
		throw new IllegalStateException("Unimplemented");
	}
}
