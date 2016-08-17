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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Delete;
import org.basex.core.cmd.Flush;

import de.interactive_instruments.IFile;
import de.interactive_instruments.Version;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.WriteDaoListener;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.ModelItemDto;
import de.interactive_instruments.etf.dal.dto.RepositoryItemDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;

/**
 * BaseX based Data Access Object for read and write operations
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
abstract class BsxWriteDao<T extends Dto> extends BsxDao<T> implements WriteDao<T> {

	private final List<WriteDaoListener> listeners = new ArrayList<>(2);

	protected BsxWriteDao(final String queryPath, final String typeName,
			final BsxDsCtx ctx, final GetDtoResultCmd<T> getDtoResultCmd) throws StoreException {
		super(queryPath, typeName, ctx, getDtoResultCmd);
	}

	@Override
	public final void add(final T t) throws StoreException {
		ensureType(t);
		doMarshallAndAdd(t);
		fireEvent(WriteDaoListener.EventType.ADD, t);
	}

	protected void doMarshallAndAdd(final T t) throws StoreException {
		final IFile item = getFile(t.getId());
		try {
			if (!item.createNewFile()) {
				throw new StoreException("Item " + t.getDescriptiveLabel() + " already exists!");
			}
		} catch (IOException e) {
			item.delete();
			throw new StoreException(e);
		}
		try {
			FileUtils.touch(item);
			ctx.createMarshaller().marshal(t, item);
			new Add(item.getName(), item.getAbsolutePath()).execute(ctx.getBsxCtx());
			new Flush().execute(ctx.getBsxCtx());
		} catch (JAXBException e) {
			ctx.getLogger().error("Object {} cannot be marshaled.\n\tProperties: {}",
					t.getDescriptiveLabel(), t.toString());
			if (ctx.getLogger().isDebugEnabled()) {
				ctx.getLogger().debug("Path to corrupted file: {}", item.getAbsolutePath());
			} else {
				// File contains invalid content
				item.delete();
			}
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new StoreException(e);
		}
	}

	@Override
	public final void addAll(final Collection<T> collection) throws StoreException {
		// OPTIMIZE could be tuned
		final List<IFile> files = getFiles(collection);
		final Dto[] colArr = collection.toArray(new Dto[collection.size()]);
		for (int i = 0; i < colArr.length; i++) {
			try {
				if (!files.get(i).createNewFile()) {
					throw new StoreException("Item " + colArr[i].getDescriptiveLabel() +
							" already exists!");
				}
			} catch (IOException e) {
				throw new StoreException(e);
			}
			try {
				FileUtils.touch(files.get(i));
				ctx.createMarshaller().marshal(colArr[i], files.get(i));
			} catch (JAXBException e) {
				if (ctx.getLogger().isDebugEnabled()) {
					ctx.getLogger().debug("Object {} cannot be marshaled.\n\tProperties: {}\n\tPath to file: {}",
							colArr[i].getDescriptiveLabel(), colArr[i].toString(), files.get(i).getAbsolutePath());
				} else {
					// File may contain invalid content
					files.get(i).delete();
				}
				throw new IllegalArgumentException(e);
			} catch (IOException e) {
				throw new StoreException(e);
			}
		}
		try {
			for (int i = 0; i < files.size(); i++) {
				new Add(files.get(i).getName(), files.get(i).getAbsolutePath()).execute(ctx.getBsxCtx());
			}
			new Flush().execute(ctx.getBsxCtx());
		} catch (BaseXException e) {
			throw new IllegalArgumentException(e);
		}
		fireEvent(WriteDaoListener.EventType.ADD, colArr);
	}

	private byte[] createHash(final String str) {
		int hash = 31;
		for (int i = 0; i < str.length(); i++) {
			hash = hash * 71 + str.charAt(i);
		}
		return ByteBuffer.allocate(4).putInt(hash).array();
	}

	@Override
	public final T update(final T t) throws StoreException, ObjectWithIdNotFoundException {
		ensureType(t);
		doUpdate(t);
		fireEvent(WriteDaoListener.EventType.UPDATE, t);
		return t;
	}

	protected T doUpdate(final T t) throws StoreException, ObjectWithIdNotFoundException {
		if (t instanceof RepositoryItemDto) {
			// get old dto from db and set "replacedBy" property to the new dto
			final RepositoryItemDto oldDtoInDb = ((RepositoryItemDto) getById(t.getId()).getDto());
			final ModelItemDto replacedBy = oldDtoInDb.getReplacedBy();
			if (replacedBy != null) {
				throw new IllegalArgumentException(
						"Item " + oldDtoInDb.getDescriptiveLabel()
								+ " cannot be updated as it is replaced by the newer item "
								+ replacedBy.getDescriptiveLabel());
			}
			// Check version
			final Version oldVersion;
			if (oldDtoInDb.getVersion() == null) {
				oldVersion = new Version("1.0.0");
				oldDtoInDb.setVersion(new Version(oldVersion));
			} else {
				oldVersion = new Version(oldDtoInDb.getVersion());
			}

			// increment version and add new dto
			((RepositoryItemDto) t).setVersion(oldVersion.incBugfix());
			t.setId(EidFactory.getDefault().createUUID(
					oldDtoInDb.getId().toString() + "." + ((RepositoryItemDto) t).getVersionAsStr()));
			((RepositoryItemDto) t).setItemHash(createHash(t.toString()));

			// Set replaceBy property and write back
			oldDtoInDb.setReplacedBy((RepositoryItemDto) t);
			doDeleteAndAdd((T) oldDtoInDb);
			fireEvent(WriteDaoListener.EventType.UPDATE, oldDtoInDb);

			// Add new one
			doMarshallAndAdd(t);
		} else {
			doDeleteAndAdd(t);
		}
		return t;
	}

	protected void doDeleteAndAdd(final T t) throws StoreException, ObjectWithIdNotFoundException {
		// OPTIMIZE could be tuned
		doDelete(t.getId(), false);
		doMarshallAndAdd(t);
	}

	@Override
	public final Collection<T> updateAll(final Collection<T> collection) throws StoreException, ObjectWithIdNotFoundException {
		// OPTIMIZE could be tuned
		final List<T> updatedDtos = new ArrayList<T>(collection.size());
		for (final T dto : collection) {
			updatedDtos.add(doUpdate(dto));
		}
		fireEvent(WriteDaoListener.EventType.UPDATE, updatedDtos.toArray(new Dto[updatedDtos.size()]));
		return updatedDtos;
	}

	@Override
	public final void delete(final EID eid) throws StoreException, ObjectWithIdNotFoundException {
		doDelete(eid, true);
		fireEvent(WriteDaoListener.EventType.DELETE, eid);
	}

	protected void doDelete(final EID eid, boolean clean) throws StoreException, ObjectWithIdNotFoundException {
		final IFile oldItem = getFile(eid);
		if (!oldItem.exists()) {
			throw new ObjectWithIdNotFoundException(this, eid.toString());
		}
		try {
			// Delete single item in the etf db
			new Delete(oldItem.getName()).execute(ctx.getBsxCtx());
			new Flush().execute(ctx.getBsxCtx());

			if (clean) {
				doCleanAfterDelete(eid);
			}
		} catch (BaseXException e) {
			throw new StoreException(e.getMessage());
		}

		if (!oldItem.delete()) {
			ctx.getLogger().error("File {} could not be deleted", oldItem.getAbsolutePath());
		}
	}

	protected abstract void doCleanAfterDelete(final EID eid) throws BaseXException;

	@Override
	public final void deleteAll(final Collection<EID> collection) throws StoreException, ObjectWithIdNotFoundException {
		// OPTIMIZE could be tuned
		for (final EID eid : collection) {
			doDelete(eid, true);
		}
		fireEvent(WriteDaoListener.EventType.DELETE, collection.toArray(new EID[collection.size()]));
	}

	protected final void fireEvent(WriteDaoListener.EventType eventType, Dto... dtos) {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).writeOperationPerformed(eventType, dtos);
		}
	}

	protected final void fireEvent(WriteDaoListener.EventType eventType, EID... ids) {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).writeOperationPerformed(eventType, ids);
		}
	}

	@Override
	public void registerListener(final WriteDaoListener listener) {
		listeners.add(listener);
	}

	@Override
	public void deregisterListener(final WriteDaoListener listener) {
		listeners.remove(listener);
	}
}
