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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Delete;
import org.basex.core.cmd.Flush;
import org.slf4j.Logger;
import org.xml.sax.*;

import de.interactive_instruments.IFile;
import de.interactive_instruments.MdUtils;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.EtfXpathEvaluator;
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
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
abstract class AbstractBsxStreamWriteDao<T extends Dto> extends AbstractBsxWriteDao<T> implements StreamWriteDao<T> {

    private final Schema schema;

    protected AbstractBsxStreamWriteDao(final String queryPath, final String typeName,
            final BsxDsCtx ctx, final GetDtoResultCmd<T> getDtoResultCmd) throws StorageException {
        super(queryPath, typeName, ctx, getDtoResultCmd);
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
                logger.error("Validation error ({}:{}): {} ", exception.getColumnNumber(), exception.getLineNumber(),
                        exception.getMessage());
                throw new SAXException(exception);
            }
        }

        @Override
        public void fatalError(final SAXParseException exception) throws SAXException {
            if (!exception.getMessage().startsWith("cvc-id")) {
                logger.error("Fatal validation error ({}:{}): {} ", exception.getColumnNumber(), exception.getLineNumber(),
                        exception.getMessage());
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
            final XPath xpath = EtfXpathEvaluator.newXPath();
            final String xpathExpression = this.queryPath + "[1]/@id";
            final Object oid = xpath.evaluate(xpathExpression, new InputSource(new ByteArrayInputStream(buffer)),
                    XPathConstants.STRING);
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
                if (ctx.getLogger().isDebugEnabled()) {
                    // Write the buffer to a temp file
                    itemFile = IFile.createTempFile("etf_stream", UUID.randomUUID().toString());
                    FileUtils.writeByteArrayToFile(itemFile, buffer);
                }
                throw e;
            }

            if (exists(id)) {
                doDelete(id, false);
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
        } catch (ObjectWithIdNotFoundException | ClassCastException | XPathExpressionException | IllegalStateException
                | IOException | ParserConfigurationException | SAXException e) {
            if (itemFile != null) {
                try {
                    if (ctx.getLogger().isDebugEnabled()) {
                        ctx.getLogger().debug("Failed to add streamed Dto. Intermediate file has been kept: {}", itemFile);
                    } else {
                        ctx.getLogger().error(
                                "Failed to add streamed Dto. Intermediate file has been deleted as Log level is not set to debug.");
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
                ((RepositoryItemDto) dto).setItemHash(MdUtils.checksumAsHexStr(buffer));
            }
            if (hook != null) {
                dto = hook.doChangeBeforeStore(dto);
                Objects.requireNonNull(dto, "Implementation error: doChangeBeforeStreamUpdate() returned null")
                        .ensureBasicValidity();
            }
            // do not update as Id would change
            doDelete(dto.getId(), false);
            add(dto);
            return dto;
        } catch (IncompleteDtoException | ObjectWithIdNotFoundException | IOException e) {
            throw new StoreException(e);
        }
    }
}
