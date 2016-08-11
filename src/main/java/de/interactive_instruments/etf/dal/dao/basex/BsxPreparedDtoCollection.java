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
import java.util.*;

import javax.xml.bind.JAXBException;

import org.basex.core.cmd.XQuery;

import de.interactive_instruments.etf.dal.dao.PreparedDtoCollection;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.EID;

/**
 * A prepared XQuery statement for querying multiple items - without their references!
 * Every inherited Map or the streamTo method will execute the request.
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
final class BsxPreparedDtoCollection<T extends Dto> extends AbstractBsxPreparedDto implements PreparedDtoCollection<T> {

	private final GetDtoResultCmd<T> getter;
	private List<T> cachedDtos;
	private HashMap<EID, T> mappedDtos;

	BsxPreparedDtoCollection(final XQuery xquery, final BsxDsCtx ctx, final GetDtoResultCmd<T> getter) {
		super(xquery, ctx);
		this.getter = getter;
	}

	@Override
	public Iterator<T> iterator() {
		enusreDtosQueried();
		return cachedDtos.iterator();
	}

	@Override
	public int size() {
		enusreDtosQueried();
		return cachedDtos.size();
	}

	@Override
	public boolean isEmpty() {
		enusreDtosQueried();
		return cachedDtos.isEmpty();
	}

	@Override
	public boolean containsValue(final Object value) {
		ensureMap();
		return mappedDtos.containsValue(value);
	}

	@Override
	public T put(final EID key, final T value) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
	}

	@Override
	public void putAll(final Map<? extends EID, ? extends T> m) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
	}

	@Override
	public void clear() {
		if (cachedDtos != null) {
			cachedDtos.clear();
			if (mappedDtos != null) {
				mappedDtos.clear();
			}
		}
	}

	@Override
	public Set<EID> keySet() {
		ensureMap();
		return mappedDtos.keySet();
	}

	@Override
	public Collection<T> asCollection() {
		enusreDtosQueried();
		return cachedDtos;
	}

	@Override
	public Collection<T> values() {
		return asCollection();
	}

	@Override
	public Set<Entry<EID, T>> entrySet() {
		ensureMap();
		return mappedDtos.entrySet();
	}

	@Override
	public T _internalGet(final Object o) {
		ensureMap();
		return mappedDtos.get(o);
	}

	@Override
	public T _internalRemove(final Object o) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
	}

	@Override
	public boolean _internalContainsKey(final Object o) {
		ensureMap();
		return mappedDtos.containsKey(o);
	}

	/**
	 * Ensures that the list is filled with the results of the query
	 */
	private void enusreDtosQueried() {
		if (cachedDtos == null) {
			try {
				final ByteArrayOutputStream output = new ByteArrayOutputStream();
				xquery.execute(ctx.getBsxCtx(), output);
				final DsResultSet result = (DsResultSet) ctx.createUnmarshaller().unmarshal(
						new ByteArrayInputStream(output.toByteArray()));
				cachedDtos = Objects.requireNonNull(
						getter.getMainDtos(result), "Database returned no data");
			} catch (IOException | JAXBException e) {
				logError(e);
				throw new IllegalStateException(e);
			}
		}
	}

	/**
	 * Ensures the map get filled with the items from the list
	 */
	private void ensureMap() {
		if (mappedDtos == null) {
			enusreDtosQueried();
			if (cachedDtos != null) {
				mappedDtos = new HashMap<>();
				for (int i = 0; i < cachedDtos.size(); i++) {
					mappedDtos.put(cachedDtos.get(i).getId(), cachedDtos.get(i));
				}
			}
		}
	}

	@Override
	public int compareTo(final PreparedDtoCollection o) {
		if (!(o instanceof PreparedDtoCollection)) {
			return -1;
		}
		final BsxPreparedDtoCollection bsxO = (BsxPreparedDtoCollection) o;
		if (cachedDtos != null && bsxO.cachedDtos != null) {
			final int sizeCmp = cachedDtos.size() - bsxO.cachedDtos.size();
			if (sizeCmp != 0) {
				return sizeCmp;
			}
			for (int i = 0; i < cachedDtos.size(); i++) {
				final int cmp = cachedDtos.get(i).getId().compareTo(
						((Dto) bsxO.cachedDtos.get(i)).getId());
				if (cmp != 0) {
					return cmp;
				}
			}
		}
		return this.xquery.toString().compareTo(bsxO.xquery.toString());
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("BsxPreparedDtoCollection{");
		sb.append("xquery=").append(xquery.toString());
		sb.append(", cachedDtos=").append(cachedDtos != null ? cachedDtos.size() : "unresolved");
		sb.append('}');
		return sb.toString();
	}

	@Override
	public void release() {
		clear();
	}
}
