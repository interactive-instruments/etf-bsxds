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

import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.PreparedDtoCollectionResult;
import de.interactive_instruments.etf.dal.dao.PreparedDtoResult;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigPropertyHolder;

import java.util.Set;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
class BsxDao<T extends Dto> implements Dao<T> {

	protected final String queryPath;
	protected final BsxDbCtx ctx;
	protected final String typeName;

	protected BsxDao(final String queryPath, final String typeName, final BsxDbCtx ctx) {
		this.queryPath = queryPath;
		this.ctx = ctx;
		this.typeName = typeName;
	}

	@Override public String getId() {
		return null;
	}

	@Override public Class<T> getDtoType() {
		return null;
	}

	@Override public PreparedDtoCollectionResult<T> getAll(final int i, final int i1) throws StoreException {
		return null;
	}

	@Override public boolean exists(final EID eid) throws StoreException {
		return false;
	}

	@Override public ConfigPropertyHolder getConfigurationProperties() {
		return null;
	}

	@Override public void init() throws ConfigurationException, InitializationException, InvalidStateTransitionException {

	}

	@Override public boolean isInitialized() {
		return false;
	}

	@Override public void release() {

	}

	@Override public PreparedDtoResult<T> getById(final EID eid) throws StoreException, ObjectWithIdNotFoundException {
		return null;
	}

	@Override public PreparedDtoCollectionResult<T> getByIds(final Set<EID> set) throws StoreException, ObjectWithIdNotFoundException {
		return null;
	}
}
