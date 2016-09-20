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

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Delete;
import org.basex.core.cmd.Flush;
import org.slf4j.Logger;
import org.xml.sax.*;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.model.*;
import de.interactive_instruments.exceptions.*;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;

/**
 * Test Task Result Data Access Object
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
final class TestTaskResultDao extends AbstractBsxStreamWriteDao<TestTaskResultDto> {

	private final Schema schema;

	private static class ValidationErrorHandler implements ErrorHandler {

		private final Logger logger;

		private ValidationErrorHandler(final Logger logger) {
			this.logger = logger;
		}

		@Override
		public void warning(final SAXParseException exception) throws SAXException {

		}

		@Override
		public void error(final SAXParseException exception) throws SAXException {
			if (!exception.getMessage().startsWith("cvc-id")) {
				throw new SAXException(exception);
			}
		}

		@Override
		public void fatalError(final SAXParseException exception) throws SAXException {
			if (!exception.getMessage().startsWith("cvc-id")) {
				throw new SAXException(exception);
			}
		}
	}

	protected TestTaskResultDao(final BsxDsCtx ctx) throws StorageException, IOException, TransformerConfigurationException {
		super("/etf:TestTaskResult", "TestTaskResult", ctx,
				(dsResultSet) -> dsResultSet.getTestTaskResults());
		schema = ((BsxDataStorage) ctx).getSchema();
		configProperties = new ConfigProperties("etf.webapp.base.url");
	}

	@Override
	protected void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException {
		final XsltOutputTransformer reportTransformer;
		try {
			reportTransformer = new XsltOutputTransformer(
					this, "html", "text/html", "xslt/default/TestRun2DefaultReport.xsl", "xslt/default/");
			reportTransformer.getConfigurationProperties().setPropertiesFrom(this.configProperties, true);
			reportTransformer.init();
			outputFormats.put(reportTransformer.getId(), reportTransformer);
		} catch (IOException | TransformerConfigurationException e) {
			throw new InitializationException(e);
		}
	}

	@Override
	protected void doCleanAfterDelete(final EID eid) throws BaseXException {

	}

	@Override
	public Class<TestTaskResultDto> getDtoType() {
		return TestTaskResultDto.class;
	}

}
