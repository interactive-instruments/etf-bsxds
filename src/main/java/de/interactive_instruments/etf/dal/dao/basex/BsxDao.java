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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.interactive_instruments.etf.model.DefaultEidMap;
import org.apache.commons.io.IOUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.XQuery;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.Filter;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.PreparedDtoCollection;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidMap;
import de.interactive_instruments.etf.model.OutputFormat;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * BaseX based Data Access Object for read only operations
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
abstract class BsxDao<T extends Dto> implements Dao<T> {

	protected final String queryPath;
	protected final BsxDsCtx ctx;
	protected final String typeName;
	protected final String xqueryStatement;
	protected final EidMap<OutputFormat> outputFormats = new DefaultEidMap<>();
	private final GetDtoResultCmd getDtoResultCmd;

	protected void ensureType(final T t) {
		if (!this.getDtoType().isAssignableFrom(t.getClass())) {
			throw new IllegalArgumentException(
					"Item " + t.getDescriptiveLabel() + " is not of type " + this.getDtoType().getSimpleName());
		}
	}

	protected BsxDao(final String queryPath, final String typeName, final BsxDsCtx ctx, final GetDtoResultCmd<T> getDtoResultCmd) throws StoreException {
		this.queryPath = queryPath;
		this.ctx = ctx;
		this.typeName = typeName;
		this.getDtoResultCmd = getDtoResultCmd;
		try {
			xqueryStatement = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
					"xquery/" + typeName + "-xdb.xquery"), "UTF-8");
		} catch (IOException e) {
			throw new StoreException("Could not load XQuery resource for " + typeName, e);
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
	}

	@Override
	public ConfigPropertyHolder getConfigurationProperties() {
		return new ConfigProperties();
	}

	@Override
	public void init() throws ConfigurationException, InitializationException, InvalidStateTransitionException {

	}

	@Override
	public PreparedDtoCollection<T> getAll(final Filter filter) throws StoreException {
		try {
			final BsXQuery bsXQuery = createPagedQuery(filter);
			return new BsxPreparedDtoCollection(bsXQuery, getDtoResultCmd);
		} catch (BaseXException e) {
			ctx.getLogger().error(e.getMessage());
			throw new StoreException(e);
		}
	}

	@Override
	public PreparedDto<T> getById(final EID eid, final Filter filter) throws StoreException, ObjectWithIdNotFoundException {
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
	public PreparedDtoCollection<T> getByIds(final Set<EID> set, final Filter filter) throws StoreException, ObjectWithIdNotFoundException {
		throw new StoreException("Not implemented yet");
	}

	private BsXQuery createPagedQuery(final Filter filter) throws BaseXException {
		return new BsXQuery(this.ctx, xqueryStatement).parameter(filter).
				parameter("function", "paged").
				parameter("selection", typeName);
	}

	private BsXQuery createIdQuery(final String id, final Filter filter) throws BaseXException {
		return new BsXQuery(this.ctx, xqueryStatement).parameter(filter).
				parameter("qids", id).
				parameter("function", "byId").
				parameter("selection", typeName);
	}

	@Override
	public boolean isInitialized() {
		return xqueryStatement != null;
	}

	@Override
	public void release() {

	}

	@Override
	public EidMap<OutputFormat> getOutputFormats() {
		return outputFormats;
	}
}
