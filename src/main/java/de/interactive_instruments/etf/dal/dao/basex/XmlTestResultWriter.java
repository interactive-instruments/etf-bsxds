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
import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.etf.dal.dto.result.TestResultStatus;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
final class TestResultWriter {

	static final String ETF_NS = "http://www.interactive-instruments.de/etf/2.0";
	static final String ETF_RESULT_XSD = "http://services.interactive-instruments.de/etf/schema/model/resultSet.xsd";
	static final String ETF_NS_PREFIX = "etf";
	static final String ID_PREFIX = "EID";

	private final Deque<ResultModelItem> results = new LinkedList<>();
	private final Map<String, Attachment> attachments = new HashMap<>();
	private final List<Message> messages = new ArrayList<>();
	private final Random random = new Random();

	private final XMLStreamWriter writer;

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

	String writeEidAndMarkResultModelItem(final String resultedFrom, final long startTimestamp) throws XMLStreamException {
		long time = startTimestamp << 32;
		time |= ((startTimestamp & 0xFFFF00000000L) >> 16);
		time |= 0x1000 | ((startTimestamp >> 48) & 0x0FFF);
		final String genId = new UUID(time, random.nextLong()).toString();
		writer.writeAttribute("id", ID_PREFIX+genId);
		results.addLast(new ResultModelItem(genId, startTimestamp, resultedFrom));
		return genId;
	}





	void close() {
		writer.flush();
		writer.close();
	}

}
