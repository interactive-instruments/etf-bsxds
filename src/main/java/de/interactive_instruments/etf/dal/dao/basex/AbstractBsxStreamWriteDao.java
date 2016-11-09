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
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import de.interactive_instruments.etf.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Delete;
import org.basex.core.cmd.Flush;
import org.slf4j.Logger;
import org.xml.sax.*;

import de.interactive_instruments.IFile;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dao.exceptions.StoreException;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.IncompleteDtoException;
import de.interactive_instruments.etf.dal.dto.RepositoryItemDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
abstract class AbstractBsxStreamWriteDao<T extends Dto> extends BsxWriteDao<T> implements StreamWriteDao<T> {

	private final Schema schema;

	protected AbstractBsxStreamWriteDao(final String queryPath, final String typeName,
			final BsxDsCtx ctx, final GetDtoResultCmd<T> getDtoResultCmd) throws StorageException {
		super(queryPath, typeName, ctx, getDtoResultCmd);
		/*
		try {
			final InputStream storageSchema = getClass().getClassLoader().getResourceAsStream("schema/td/testDriverResponse.xsd");
			Objects.requireNonNull(storageSchema, "Internal error reading the ets schema");
			final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			sf.setResourceResolver(new BsxSchemaResourceResolver());
			schema = sf.newSchema(new StreamSource(storageSchema));
		} catch (SAXException e) {
			throw new StorageException("Could not load schema: ", e);
		}
		*/
		schema = ((BsxDataStorage) ctx).getSchema();
	}

	private static class ValidationErrorHandler implements ErrorHandler {

		private final Logger logger;

		public ValidationErrorHandler(final Logger logger) {
			this.logger = logger;
		}

		@Override
		public void warning(final SAXParseException exception) throws SAXException {

		}

		@Override
		public void error(final SAXParseException exception) throws SAXException {
			if (!exception.getMessage().startsWith("cvc-id")) {
				logger.error("Validation error ({}:{}): {} ", exception.getColumnNumber(), exception.getLineNumber(), exception.getMessage());
				throw new SAXException(exception);
			}
		}

		@Override
		public void fatalError(final SAXParseException exception) throws SAXException {
			if (!exception.getMessage().startsWith("cvc-id")) {
				logger.error("Fatal validation error ({}:{}): {} ", exception.getColumnNumber(), exception.getLineNumber(), exception.getMessage());
				throw new SAXException(exception);
			}
		}
	}

	EID addAndValidate(final InputStream inputStream) throws StorageException {
		try {
			// Create copy of stream in memory
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			IOUtils.copy(inputStream, byteArrayOutputStream);
			final byte[] buffer = byteArrayOutputStream.toByteArray();
			return addAndValidate(buffer);
		} catch (IOException e) {
			throw new StorageException(e);
		}
	}

	private EID addAndValidate(final byte[] buffer) throws StorageException {
		IFile itemFile = null;
		try {
			// Parse ID
			final XPath xpath = XmlUtils.newXPath();
			final String xpathExpression = this.queryPath + "[1]/@id";
			final Object oid = xpath.evaluate(xpathExpression, new InputSource(new ByteArrayInputStream(buffer)), XPathConstants.STRING);
			if (SUtils.isNullOrEmpty((String) oid)) {
				throw new StorageException("Could not query id (" + xpathExpression + ")");
			}
			final String withoutEID = ((String) oid).substring(3);
			final EID id = EidFactory.getDefault().createAndPreserveStr(withoutEID);

			// Validate input
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			final XMLReader reader = spf.newSAXParser().getXMLReader();
			final ValidatorHandler vh = schema.newValidatorHandler();
			final ValidationErrorHandler eh = new ValidationErrorHandler(ctx.getLogger());
			vh.setErrorHandler(eh);
			reader.setContentHandler(vh);

			try {
				reader.parse(new InputSource(new ByteArrayInputStream(buffer)));
			} catch (IOException | SAXException e) {
				// Validation failed. Check if the intermediate file should be kept
				if(ctx.getLogger().isDebugEnabled()) {
					// Write the buffer to a temp file
					itemFile = IFile.createTempFile("etf_stream", UUID.randomUUID().toString());
					FileUtils.writeByteArrayToFile(itemFile, buffer);
				}
				throw e;
			}

			if (exists(id)) {
				delete(id);
			}

			// Create file
			itemFile = getFile(id);
			itemFile.createNewFile();
			FileUtils.writeByteArrayToFile(itemFile, buffer);

			new Add(itemFile.getName(), itemFile.getAbsolutePath()).execute(ctx.getBsxCtx());
			new Flush().execute(ctx.getBsxCtx());
			ctx.getLogger().trace("Wrote result to {}", itemFile.getPath());
			if (!exists(id)) {
				throw new StorageException("Unable to query streamed Dto by ID");
			}
			return id;
		} catch (ObjectWithIdNotFoundException | ClassCastException | XPathExpressionException | IllegalStateException | IOException | ParserConfigurationException | SAXException e) {
			if (itemFile != null) {
				try {
					if (ctx.getLogger().isDebugEnabled()) {
						ctx.getLogger().debug("Failed to add streamed Dto. Intermediate file has been kept: {}", itemFile);
					} else {
						ctx.getLogger().error("Failed to add streamed Dto. Intermediate file has been deleted as Log level is not set to debug.");
						itemFile.delete();
						new Delete(itemFile.getName()).execute(ctx.getBsxCtx());
						new Flush().execute(ctx.getBsxCtx());
					}
				} catch (final BaseXException e2) {
					ExcUtils.suppress(e2);
				}
			}
			throw new StoreException(e);
		}

	}

	@Override
	public final T add(final InputStream inputStream, final ChangeBeforeStoreHook<T> hook) throws StorageException {
		try {
			// Create copy of stream in memory
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			IOUtils.copy(inputStream, byteArrayOutputStream);
			final byte[] buffer = byteArrayOutputStream.toByteArray();

			final EID id = addAndValidate(buffer);
			T dto = getById(id).getDto();
			if (dto instanceof RepositoryItemDto) {
				final byte[] hash = createHash(buffer);
				((RepositoryItemDto) dto).setItemHash(hash);
			}
			if (hook != null) {
				dto = hook.doChangeBeforeStore(dto);
				Objects.requireNonNull(dto, "Implementation error doChangeBeforeStreamUpdate returned null").ensureBasicValidity();
			}
			// do not update as Id would change
			delete(dto.getId());
			add(dto);
			return dto;
		} catch (IncompleteDtoException | ObjectWithIdNotFoundException | IOException e) {
			throw new StoreException(e);
		}
	}
}
