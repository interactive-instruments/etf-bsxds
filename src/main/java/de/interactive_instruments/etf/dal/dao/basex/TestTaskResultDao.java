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

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.io.IOUtils;
import org.basex.core.BaseXException;
import org.slf4j.Logger;
import org.xml.sax.*;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.TestTaskResultWriteDao;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.model.DefaultEidMap;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidMap;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;

/**
 * Test Task Result Data Access Object
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class TestTaskResultDao extends BsxWriteDao<TestTaskResultDto> implements TestTaskResultWriteDao {

	private final Schema schema;

	private static class TestTaskResultValidationErrorHandler implements ErrorHandler {

		private final Logger logger;

		private TestTaskResultValidationErrorHandler(final Logger logger) {
			this.logger = logger;
		}

		@Override
		public void warning(final SAXParseException exception) throws SAXException {

		}

		@Override
		public void error(final SAXParseException exception) throws SAXException {
			if (!exception.getMessage().startsWith("cvc-id")) {
				throw new IllegalStateException(exception);
			}
		}

		@Override
		public void fatalError(final SAXParseException exception) throws SAXException {
			if (!exception.getMessage().startsWith("cvc-id")) {
				throw new IllegalStateException(exception);
			}
		}

	}

	protected TestTaskResultDao(final BsxDsCtx ctx) throws StoreException, IOException, TransformerConfigurationException {
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
					this, "html", "text/html", "xslt/default/TestTaskResult2DefaultReport.xsl", "xslt/default/");
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

	@Override
	public void add(final InputSource inputSource) throws StoreException {
		try {
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			final XMLReader reader = spf.newSAXParser().getXMLReader();
			final ValidatorHandler vh = schema.newValidatorHandler();
			TestTaskResultValidationErrorHandler eh = new TestTaskResultValidationErrorHandler(ctx.getLogger());
			vh.setErrorHandler(eh);
			reader.setContentHandler(vh);
			reader.parse(inputSource);

			final IFile dest = new IFile("/tmp/todo");

			try {
				// move
				final IFile resultFile = new IFile(URI.create(inputSource.getSystemId()));
				if (resultFile.exists()) {
					resultFile.renameTo(dest);
				}
			} catch (IllegalArgumentException e) {
				OutputStream outputStream = null;
				try {
					outputStream = new FileOutputStream(dest);
					int read = 0;
					byte[] bytes = new byte[1024];
					while ((read = inputSource.getByteStream().read(bytes)) != -1) {
						outputStream.write(bytes, 0, read);
					}
				} catch (IOException ew) {
					IFile.closeQuietly(outputStream);
				}
			}
			ctx.getLogger().trace("Wrote result to {}", dest);
		} catch (IllegalStateException | IOException | ParserConfigurationException | SAXException e) {
			throw new StoreException(e);
		}
	}
}
