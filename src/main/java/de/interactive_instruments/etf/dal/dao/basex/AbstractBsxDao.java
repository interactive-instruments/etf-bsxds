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

import static de.interactive_instruments.etf.dal.dao.basex.BsxDataStorage.ETF_NAMESPACE_DECL;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.io.IOUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.XQuery;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.Filter;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.PreparedDtoCollection;
import de.interactive_instruments.etf.dal.dao.exceptions.RetrieveException;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.DefaultEidMap;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidMap;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * BaseX based Data Access Object for read only operations
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
abstract class AbstractBsxDao<T extends Dto> implements Dao<T> {

    protected final String queryPath;
    protected final BsxDsCtx ctx;
    protected final String typeName;
    protected final String xqueryStatement;
    protected final EidMap<OutputFormat> outputFormatIdMap = new DefaultEidMap<>();
    protected final Map<String, OutputFormat> outputFormatLabelMap = new HashMap<>();
    protected ConfigProperties configProperties;
    protected boolean initialized = false;
    private final GetDtoResultCmd getDtoResultCmd;
    protected long lastModificationDate = System.currentTimeMillis();

    protected AbstractBsxDao(final String queryPath, final String typeName, final BsxDsCtx ctx,
            final GetDtoResultCmd<T> getDtoResultCmd) throws StorageException {
        this.queryPath = queryPath;
        this.ctx = ctx;
        this.typeName = typeName;
        this.getDtoResultCmd = getDtoResultCmd;
        try {
            xqueryStatement = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                    "xquery/" + typeName + "-xdb.xquery"), "UTF-8");
        } catch (IOException e) {
            throw new StorageException("Could not load XQuery resource for " + typeName, e);
        }
    }

    protected void ensureType(final T t) {
        if (!this.getDtoType().isAssignableFrom(t.getClass())) {
            throw new IllegalArgumentException(
                    "Item " + t.getDescriptiveLabel() + " is not of type " + this.getDtoType().getSimpleName());
        }
        if (t.getId() == null) {
            throw new IllegalArgumentException(
                    "Item " + t.getClass().getName() + t.hashCode() + " has no ID");
        }
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    protected final IFile getFile(final EID eid) {
        final IFile file = ctx.getStoreDir().secureExpandPathDown(
                typeName + "-" + BsxDataStorage.ID_PREFIX + eid.getId() + ".xml");
        file.exists();
        return file;
    }

    protected final List<IFile> getFiles(final Collection<T> dtos) {
        final String filePathPrefix = ctx.getStoreDir() + File.separator + typeName + "-" + BsxDataStorage.ID_PREFIX;
        final List<IFile> files = new ArrayList<>(dtos.size());
        files.addAll(dtos.stream().map(dto -> new IFile(filePathPrefix + dto.getId() + ".xml")).collect(Collectors.toList()));
        return files;
    }

    @Override
    public boolean exists(final EID eid) {
        return getFile(eid).exists();
        /* alternative implementation try { return "true".equals(new XQuery(ETF_NAMESPACE_DECL + "exists(db:open('etf-ds')"+queryPath+"[@id = 'EID"+eid.toString()+"'])").execute(ctx.getBsxCtx())); } catch (BaseXException e) { throw new IllegalStateException("Internal error in isDisabled(), ", e); } */
    }

    public boolean exists2(final EID eid) {
        try {
            return "true".equals(new XQuery(ETF_NAMESPACE_DECL +
                    "exists(db:open('etf-ds')" + queryPath + "[@id = 'EID" + eid.toString() + "'])").execute(ctx.getBsxCtx()));
        } catch (BaseXException e) {
            throw new IllegalStateException("Internal error in isDisabled(), ", e);
        }
    }

    @Override
    public boolean isDisabled(final EID eid) {
        if (!exists(eid)) {
            return false;
        }
        try {
            final StringBuilder query = new StringBuilder(ETF_NAMESPACE_DECL
                    + "db:open('etf-ds')");
            query.append(queryPath);
            query.append("[@id = 'EID");
            query.append(eid.toString());
            query.append("']/etf:disabled='true'");
            final String result = new XQuery(query.toString()).execute(ctx.getBsxCtx());
            return "true".equals(result);
        } catch (final BaseXException e) {
            throw new IllegalStateException("Internal error in isDisabled(), ", e);
        }
    }

    @Override
    public ConfigPropertyHolder getConfigurationProperties() {
        if (configProperties == null) {
            this.configProperties = new ConfigProperties();
        }
        return configProperties;
    }

    @Override
    public void init() throws ConfigurationException, InitializationException, InvalidStateTransitionException {
        if (initialized == true) {
            throw new InvalidStateTransitionException(getClass().getSimpleName() + " is already initialized");
        }

        if (configProperties != null) {
            configProperties.expectAllRequiredPropertiesSet();
        }
        try {
            // XML
            final XsltOutputTransformer xmlItemCollectionTransformer = new XsltOutputTransformer(
                    this, "DsResult2Xml", "text/xml", "xslt/DsResult2Xml.xsl");
            initAndAddTransformer(xmlItemCollectionTransformer);

            // JSON
            final XsltOutputTransformer jsonItemCollectionTransformer = new XsltOutputTransformer(
                    this, "DsResult2Json", "application/json", "xslt/DsResult2Json.xsl", "xslt");
            initAndAddTransformer(jsonItemCollectionTransformer);

        } catch (IOException | TransformerConfigurationException e) {
            throw new InitializationException(e);
        }
        doInit();
        initialized = true;
    }

    private void initAndAddTransformer(final XsltOutputTransformer outputFormat)
            throws ConfigurationException, InvalidStateTransitionException, InitializationException {
        outputFormat.getConfigurationProperties().setPropertiesFrom(configProperties, true);
        outputFormat.init();
        outputFormatIdMap.put(outputFormat.getId(), outputFormat);
        outputFormatLabelMap.put(outputFormat.getLabel(), outputFormat);
    }

    protected void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException {}

    @Override
    public final PreparedDtoCollection<T> getAll(final Filter filter) throws StorageException {
        try {
            final BsXQuery bsXQuery = createPagedQuery(filter);
            return new BsxPreparedDtoCollection(bsXQuery, getDtoResultCmd);
        } catch (BaseXException e) {
            ctx.getLogger().error(e.getMessage());
            throw new RetrieveException(e);
        }
    }

    @Override
    public PreparedDto<T> getById(final EID eid, final Filter filter) throws StorageException, ObjectWithIdNotFoundException {
        if (!exists(eid)) {
            throw new ObjectWithIdNotFoundException(this, eid.getId());
        }
        try {
            final BsXQuery bsXQuery = createIdQuery(BsxDataStorage.ID_PREFIX + eid.getId(), filter);
            return new BsxPreparedDto(eid, bsXQuery, getDtoResultCmd);
        } catch (IOException e) {
            ctx.getLogger().error(e.getMessage());
            throw new ObjectWithIdNotFoundException(this, eid.getId());
        }
    }

    @Override
    public PreparedDtoCollection<T> getByIds(final Set<EID> set, final Filter filter)
            throws StorageException, ObjectWithIdNotFoundException {
        throw new StorageException("Not implemented yet");
    }

    private BsXQuery createPagedQuery(final Filter filter) throws BaseXException {
        return new BsXQuery(this.ctx, xqueryStatement).parameter(filter).parameter("function", "paged").parameter("selection",
                typeName);
    }

    private BsXQuery createIdQuery(final String id, final Filter filter) throws BaseXException {
        return new BsXQuery(this.ctx, xqueryStatement).parameter(filter).parameter("qids", id).parameter("function", "byId")
                .parameter("selection", typeName);
    }

    @Override
    public boolean isInitialized() {
        return xqueryStatement != null;
    }

    @Override
    public void release() {
        initialized = false;
    }

    @Override
    public EidMap<OutputFormat> getOutputFormats() {
        return outputFormatIdMap;
    }

    public long getLastModificationDate() {
        return lastModificationDate;
    }

}
