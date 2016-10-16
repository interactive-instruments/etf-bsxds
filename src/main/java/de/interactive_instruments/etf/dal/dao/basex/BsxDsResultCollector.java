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

package de.interactive_instruments.etf.dal.dao.basex.resultcollector;

import de.interactive_instruments.IFile;
import de.interactive_instruments.MimeTypeUtils;
import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.basex.AbstractBsxStreamWriteDao;
import de.interactive_instruments.etf.dal.dto.result.TestResultStatus;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.testdriver.AbstractCalledTestCaseResultCollector;
import de.interactive_instruments.etf.testdriver.AbstractTestResultCollector;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.TestRunLogger;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.MimeTypeUtilsException;
import de.interactive_instruments.exceptions.StorageException;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.*;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
final class BsxDsResultCollector extends AbstractTestResultCollector {

	static final String ETF_NS = "http://www.interactive-instruments.de/etf/2.0";
	static final String ETF_RESULT_XSD = "http://services.interactive-instruments.de/etf/schema/model/resultSet.xsd";
	static final String ETF_NS_PREFIX = "etf";
	static final String ID_PREFIX = "EID";

	private final TestRunLogger logger;
	private final IFile tmpDir;
	private final IFile attachmentDir;
	private final IFile resultFile;
	private final XMLStreamWriter writer;
	private final Random random = new Random();
	private final TestTaskDto testTaskDto;
	private final Deque<ResultModelItem> results = new LinkedList<>();
	private final Map<String, Attachment> attachments = new HashMap<>();
	private final List<Message> messages = new ArrayList<>();
	private final DataStorage dataStorage;

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
			if(!results.isEmpty()) {
				writer.writeStartElement("parent");
				writer.writeAttribute("ref", ID_PREFIX+results.getLast().id);
				writer.writeEndElement();
			}

			writer.writeStartElement("resultedFrom");
			writer.writeAttribute("ref", ID_PREFIX+resultedFrom);
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
			writer.writeAttribute("id", ID_PREFIX+id);

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
			writer.writeAttribute("href", "file://"+attachmentFile.getAbsolutePath());
			writer.writeEndElement();

			writer.writeEndElement();
		}
	}

	private final class Message {
		private final String translationTemplateId;
		private final List<String> arguments;

		public Message(final String translationTemplateId) {
			this.translationTemplateId = translationTemplateId;
			this.arguments = null;
		}

		public Message(final String translationTemplateId, final String[] arguments) {
			if(arguments.length%2!=0) {
				throw new IllegalStateException("There is at least one invalid token value pair");
			}
 			this.translationTemplateId = translationTemplateId;
			this.arguments = Arrays.asList(arguments);
		}

		public Message(final String translationTemplateId, final Map<String,String> arguments) {
			this.translationTemplateId = translationTemplateId;
			this.arguments = new ArrayList<>();
			for (final Map.Entry<String, String> entry : arguments.entrySet()) {
				this.arguments.add(entry.getKey());
				this.arguments.add(entry.getValue());
			}
		}

		void write() throws XMLStreamException {
			writer.writeStartElement("message");
			writer.writeAttribute("ref", translationTemplateId);
			if(arguments!=null) {
				writer.writeStartElement("translationArguments");
				for (int i = 0; i < arguments.size(); i+=2) {
					writer.writeStartElement("argument");
					writer.writeAttribute("token", arguments.get(i));
					writer.writeCharacters(arguments.get(i+1));
					writer.writeEndElement();

				}
				writer.writeEndElement();
			}
			writer.writeEndElement();
		}
	}

	public BsxDsResultCollector(final DataStorage dataStorage, final TestRunLogger logger, final IFile resultFile, final IFile attachmentDir, final TestTaskDto testTaskDto) {
		this.logger = logger;
		this.tmpDir = attachmentDir.secureExpandPathDown("tmp");
		this.tmpDir.mkdirs();
		this.attachmentDir = attachmentDir;
		this.testTaskDto = testTaskDto;
		this.resultFile = resultFile;
		this.tmpDir.setIdentifier("Test Task "+testTaskDto.getId()+" temporary directory ");
		this.dataStorage = dataStorage;
		try {
			final FileOutputStream fos = new FileOutputStream(resultFile);
			writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fos, "UTF-8");
			writer.writeStartDocument("UTF-8", "1.0");
		}catch (XMLStreamException | IOException e) {
			throw new IllegalStateException(e);
		}
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

	private String writeEidAndMarkResultModelItem(final String resultedFrom, final long startTimestamp) throws XMLStreamException {
		long time = startTimestamp << 32;
		time |= ((startTimestamp & 0xFFFF00000000L) >> 16);
		time |= 0x1000 | ((startTimestamp >> 48) & 0x0FFF);
		final String genId = new UUID(time, random.nextLong()).toString();
		writer.writeAttribute("id", ID_PREFIX+genId);
		results.addLast(new ResultModelItem(genId, startTimestamp, resultedFrom));
		return genId;
	}

	protected String startTestTaskResult(final String resultedFrom, final long startTimestamp) throws XMLStreamException {
		writer.writeStartElement(ETF_NS_PREFIX, "TestTaskResult", ETF_NS);
		writer.setPrefix(ETF_NS_PREFIX, ETF_NS);
		writer.writeNamespace(ETF_NS_PREFIX, ETF_NS);
		writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
				ETF_NS+" "+ETF_RESULT_XSD);
		writer.writeAttribute("xmlns", ETF_NS);
		final String id = UUID.randomUUID().toString();
		writer.writeAttribute("id", ID_PREFIX+id);
		results.addLast(new ResultModelItem(id, startTimestamp, resultedFrom));
		writer.writeStartElement("testObject");
		writer.writeAttribute("ref", ID_PREFIX+testTaskDto.getTestObject().getId().getId());
		writer.writeEndElement(); // testObject
		writer.writeStartElement("testModuleResults");
		return id;
	}

	protected String startTestModuleResult(final String resultedFrom, final long startTimestamp) throws XMLStreamException {
		writer.writeStartElement("TestModuleResult");
		final String id = writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
		writer.writeStartElement("testCaseResults");
		return id;
	}

	protected String startTestCaseResult(final String resultedFrom, final long startTimestamp) throws XMLStreamException {
		writer.writeStartElement("TestCaseResult");
		final String id = writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
		writer.writeStartElement("testStepResults");
		return id;
	}

	protected String startTestStepResult(final String resultedFrom, final long startTimestamp) throws XMLStreamException {
		writer.writeStartElement("TestStepResult");
		final String id = writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
		writer.writeStartElement("testAssertionResults");
		return id;
	}

	protected String startTestAssertionResult(final String resultedFrom, final long startTimestamp) throws XMLStreamException {
		writer.writeStartElement("TestAssertionResult");
		return writeEidAndMarkResultModelItem(resultedFrom, startTimestamp);
	}

	@Override protected AbstractCalledTestCaseResultCollector createCalledTestCaseResultCollector(final String s, final long l) {
		return null;
	}

	@Override protected TestResultCollector mergeTestCaseResultSubCollector() {
		return null;
	}

	// Finish  results
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String finishResultModelItem(final int status, final long stopTimestamp) throws XMLStreamException {
		final ResultModelItem resultModelItem = results.removeLast();
		resultModelItem.write(status, stopTimestamp);
		writer.writeEndElement();
		return resultModelItem.getResultedFromId();
	}

	protected String finishTestTaskResult(final String testModelItemId, final int status, final long stopTimestamp) throws XMLStreamException, FileNotFoundException, StorageException {
		writer.writeEndElement();
		if(!attachments.isEmpty()) {
			writer.writeStartElement("attachments");
			for (final Attachment attachment : attachments.values()) {
				attachment.write();
			}
			attachments.clear();
		}
		writer.writeEndElement();
		final String id = finishResultModelItem(status, stopTimestamp);
		writer.writeEndDocument();
		writer.flush();
		writer.close();
		try {
			((AbstractBsxStreamWriteDao) dataStorage.getDao(
					TestTaskResultDto.class)).addAndValidate(new FileInputStream(resultFile));
		}catch (StorageException e) {
			logger.error("Failed to stream result file into store: {}", resultFile.getPath());
			throw e;
		}
		return id;
	}

	protected String finishTestModuleResult(final String testModelItemId, final int status, final long stopTimestamp) throws XMLStreamException {
		writer.writeEndElement();
		return finishResultModelItem(status, stopTimestamp);
	}

	protected String finishTestCaseResult(final String testModelItemId, final int status, final long stopTimestamp) throws XMLStreamException {
		writer.writeEndElement();
		return finishResultModelItem(status, stopTimestamp);
	}

	protected String finishTestStepResult(final String testModelItemId, final int status, final long stopTimestamp) throws XMLStreamException {
		writer.writeEndElement();
		return finishResultModelItem(status, stopTimestamp);
	}

	protected String finishTestAssertionResult(final String testModelItemId, final int status, final long stopTimestamp) throws XMLStreamException {
		if (!messages.isEmpty()) {
			writer.writeStartElement("messages");
			for (final Message message : this.messages) {
				message.write();
			}
			writer.writeEndElement();
			messages.clear();
		}
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
		messages.add(new Message(translationTemplateId));
	}

	@Override public void addMessage(final String translationTemplateId, final Map<String, String> tokenValuePairs) {
		messages.add(new Message(translationTemplateId, tokenValuePairs));
	}

	@Override public void addMessage(final String translationTemplateId, final String... tokensAndValues) {
		messages.add(new Message(translationTemplateId, tokensAndValues));
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

	@Override public String saveAttachment(final Reader reader, final String label, final String mimeType, final String type) throws IOException {
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
		IOUtils.copy(reader, new FileOutputStream(attachmentFile), "UTF-8");
		attachments.put(eid,
				new Attachment(eid, attachmentFile, label, "UTF-8", mimeType, type));
		return eid;
	}

	@Override public void internalError(final String translationTemplateId, final Map<String, String> tokenValuePairs, final Throwable e) {
		throw new IllegalStateException("Not implemented");
	}

	@Override public void internalError(final Throwable e) {
		throw new IllegalStateException("Not implemented");
	}
}
