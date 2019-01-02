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
import java.util.*;

import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.XmlUtils;
import de.interactive_instruments.etf.dal.dao.PreparedDtoCollection;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.DefaultEidMap;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.model.EidMap;

/**
 * A prepared XQuery statement for querying multiple items - without their references!

 * Every inherited Map or the streamTo method will execute the either a request
 * that queries the whole Dtos or a simple request that queries all IDs.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class BsxPreparedDtoCollection<T extends Dto> extends AbstractBsxPreparedDto implements PreparedDtoCollection<T> {

	private final GetDtoResultCmd<T> getter;
	private List<T> cachedDtos;
	private HashMap<EID, T> mappedDtos;
	private HashSet<EID> ids;

	BsxPreparedDtoCollection(final BsXQuery bsXQuery, final GetDtoResultCmd<T> getter) {
		super(bsXQuery);
		this.getter = getter;
	}

	private BsxPreparedDtoCollection(final BsxPreparedDtoCollection<T> preparedDtoCollection) {
		super(preparedDtoCollection.bsXquery.createCopy());
		this.getter = preparedDtoCollection.getter;
		this.ids = preparedDtoCollection.ids;
		if (preparedDtoCollection.mappedDtos != null) {
			this.mappedDtos = new HashMap<>(preparedDtoCollection.mappedDtos);
		} else if (this.cachedDtos != null) {
			this.cachedDtos = new ArrayList<>(preparedDtoCollection.cachedDtos);
		}
	}

	@Override
	public EidMap<T> createCopy() {
		return new BsxPreparedDtoCollection(this);
	}

	@Override
	public Iterator<T> iterator() {
		enusreDtosQueried();
		return cachedDtos.iterator();
	}

	@Override
	public int size() {
		if (cachedDtos == null) {
			if (ids == null) {
				return ensureIdsQueried().size();
			}
			return ids.size();
		}
		return cachedDtos.size();
	}

	@Override
	public boolean isEmpty() {
		if (cachedDtos == null) {
			if (ids == null) {
				return ensureIdsQueried().isEmpty();
			}
			return ids.isEmpty();
		}
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
	public void removeAll(final Collection<?> collection) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
	}

	@Override
	public EidMap<T> unmodifiable() {
		return this;
	}

	@Override
	public EidMap<T> getAll(final Collection<?> keys) {
		ensureMap();
		final EidMap map = new DefaultEidMap();
		for (final Object key : keys) {
			final T t = mappedDtos.get(key);
			if (t != null) {
				map.put(key, t);
			}
		}
		return map;
	}

	@Override
	public void clear() {
		if (cachedDtos != null) {
			cachedDtos.clear();
			if (mappedDtos != null) {
				mappedDtos.clear();
			}
		}
		if (ids != null) {
			ids.clear();
		}
	}

	@Override
	public Set<EID> keySet() {
		if (mappedDtos == null) {
			if (ids == null) {
				return ensureIdsQueried();
			}
			return ids;
		}
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
		if (ids != null && !ids.contains(o)) {
			return null;
		}
		ensureMap();
		return mappedDtos.get(o);
	}

	@Override
	public T _internalRemove(final Object o) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
	}

	@Override
	public boolean _internalContainsKey(final Object o) {
		if (ids != null && !ids.contains(o)) {
			return false;
		}
		ensureMap();
		return mappedDtos.containsKey(o);
	}

	/**
	 *
	 */
	private Set<EID> ensureIdsQueried() {
		try {
			final ByteArrayOutputStream output = new ByteArrayOutputStream(32784);
			bsXquery.createCopy().parameter("fields", "@id").execute(output);
			final XPath xpath = XmlUtils.newXPath();
			final String xpathExpression = "/etf:DsResultSet/etf:*[1]/etf:*/@id";
			final NodeList ns = ((NodeList) xpath.evaluate(xpathExpression, new InputSource(
					new ByteArrayInputStream(output.toByteArray())), XPathConstants.NODESET));
			ids = new HashSet<>();
			for (int index = 0; index < ns.getLength(); index++) {
				ids.add(EidFactory.getDefault().createUUID(ns.item(index).getTextContent()));
			}
			return ids;
		} catch (ClassCastException | NullPointerException | XPathExpressionException | IOException e) {
			// Use fallback
			logError(e);
			ensureMap();
			return mappedDtos.keySet();
		}
	}

	/**
	 * Ensures that the list is filled with the results of the query
	 */
	private void enusreDtosQueried() {
		if (cachedDtos == null) {
			try {
				final ByteArrayOutputStream output = new ByteArrayOutputStream(65568);
				bsXquery.execute(output);
				final DsResultSet result = (DsResultSet) bsXquery.getCtx().createUnmarshaller().unmarshal(
						new ByteArrayInputStream(output.toByteArray()));
				cachedDtos = getter.getMainDtos(result);
				if (cachedDtos == null) {
					throw new BsxPreparedDtoException("Data storage returned no data for collection");
				}
				// consistency check
				if (ids != null) {
					// Inconsistencies can be avoided by not reusing the prepared collection for multiple calls.
					if (ids.size() != cachedDtos.size()) {
						throw new ConcurrentModificationException("Data storage changed since last call. "
								+ "Actual size is " + cachedDtos.size() + ", previous size was " + ids.size() + ".");
					}
					for (final T cachedDto : cachedDtos) {
						if (!ids.contains(cachedDto.getId())) {
							throw new ConcurrentModificationException("Data storage changed since last call. "
									+ "The object " + cachedDto.getId() + " was not fetched before.");
						}
					}
				}
			} catch (IOException | JAXBException e) {
				logError(e);
				throw new BsxPreparedDtoException(e);
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
				mappedDtos = new HashMap<>((int) (cachedDtos.size() * 0.5));
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
		return toString().compareTo(bsxO.toString());
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("BsxPreparedDtoCollection{");
		sb.append("xquery='").append(bsXquery.toString());
		sb.append("', size=");
		if (cachedDtos != null) {
			sb.append(cachedDtos.size());
		} else if (ids != null) {
			sb.append(ids.size());
		} else {
			sb.append("unknown");
		}
		sb.append(", ids={");
		if (mappedDtos == null) {
			if (ids != null) {
				sb.append(SUtils.concatStr(",", ids));
			} else {
				sb.append("unresolved");
			}
		} else {
			sb.append(SUtils.concatStr(",", mappedDtos.keySet()));
		}
		sb.append("}}");
		return sb.toString();
	}

	@Override
	public void release() {
		clear();
	}
}
