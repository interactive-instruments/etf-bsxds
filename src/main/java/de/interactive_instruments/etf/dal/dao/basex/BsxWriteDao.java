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
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;

import org.basex.core.BaseXException;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Flush;

import org.apache.commons.io.FileUtils;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;

/**
 * BsxWriteDao
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
abstract class BsxWriteDao<T extends Dto> extends BsxDao<T> implements WriteDao<T> {

	protected BsxWriteDao(final String queryPath, final String typeName, final BsxDbCtx ctx) {
		super(queryPath, typeName, ctx);
	}

	@Override public void add(final T t) throws StoreException {
		try{
			final StringWriter sw = new StringWriter();
			ctx.getJaxbCtx().createMarshaller().marshal(t, sw);
			createFileAndAddToDb(typeName, t.getId(),sw.getBuffer());
		} catch (JAXBException e) {
			throw new StoreException(e.getMessage());
		}
	}

	private void createFileAndAddToDb(final String type, final EID id, final StringBuffer buffer) throws StoreException {
		final IFile item = ctx.getStoreDir().secureExpandPathDown(type + "-" + id.toString() + ".xml");
		try {
			if (!item.createNewFile()) {
				throw new StoreException("Item already exists!");
			}
			item.writeContent(buffer);
			addToDb(item);
		} catch (IOException e) {
			throw new StoreException(e.toString());
		}
	}

	void addToDb(final File file) throws BaseXException, StoreException {
		try {
			FileUtils.touch(file);
		} catch (IOException e) {
			throw new StoreException(e.getMessage());
		}
		new Add(file.getName(), file.getAbsolutePath()).execute(ctx.getBsxCtx());
		new Flush().execute(ctx.getBsxCtx());
	}

	@Override public void addAll(final Collection<T> collection) throws StoreException {
		// TODO optimize
		for (final T t : collection) {
			add(t);
		}
	}

	@Override public void update(final T t) throws StoreException, ObjectWithIdNotFoundException {

	}

	@Override public void updateAll(final Collection<T> collection) throws StoreException, ObjectWithIdNotFoundException {

	}

	@Override public void delete(final EID eid) throws StoreException, ObjectWithIdNotFoundException {

	}

	@Override public void deleteAll(final Collection<EID> collection) throws StoreException, ObjectWithIdNotFoundException {

	}
}
