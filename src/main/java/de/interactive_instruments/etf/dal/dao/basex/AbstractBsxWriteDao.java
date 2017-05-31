/**
 * Copyright 2010-2017 interactive instruments GmbH
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Delete;
import org.basex.core.cmd.Flush;
import org.basex.core.cmd.XQuery;

import de.interactive_instruments.IFile;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.Version;
import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dao.WriteDaoListener;
import de.interactive_instruments.etf.dal.dao.exceptions.StoreException;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.ModelItemDto;
import de.interactive_instruments.etf.dal.dto.RepositoryItemDto;
import de.interactive_instruments.etf.model.Disableable;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.model.EidSet;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 * BaseX based Data Access Object for read and write operations
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
abstract class AbstractBsxWriteDao<T extends Dto> extends AbstractBsxDao<T> implements WriteDao<T> {

	private final List<WriteDaoListener> listeners = new ArrayList<>(2);

	protected AbstractBsxWriteDao(final String queryPath, final String typeName,
			final BsxDsCtx ctx, final GetDtoResultCmd<T> getDtoResultCmd) throws StorageException {
		super(queryPath, typeName, ctx, getDtoResultCmd);
	}

	protected final void updateLastModificationDate() {
		this.lastModificationDate = System.currentTimeMillis();
	}

	// Fires the 'add' event
	@Override
	public final void add(final T t) throws StorageException {
		ensureType(t);
		doMarshallAndAdd(t);
		fireEventAdd(t);
		updateLastModificationDate();
	}

	protected void doMarshallAndAdd(final T t) throws StorageException {
		final IFile item = getFile(t.getId());
		try {
			if (!item.createNewFile()) {
				if (t instanceof Disableable && isDisabled(t.getId())) {
					// Will be flushed later
					new Delete(item.getName()).execute(ctx.getBsxCtx());
				} else {
					throw new StorageException("Item " + t.getDescriptiveLabel() + " already exists!");
				}
			}
		} catch (StorageException e) {
			item.delete();
			throw e;
		} catch (IOException e) {
			item.delete();
			throw new StorageException(e);
		}
		try {
			FileUtils.touch(item);
			ctx.createMarshaller().marshal(t, item);
			new Add(item.getName(), item.getAbsolutePath()).execute(ctx.getBsxCtx());
			new Flush().execute(ctx.getBsxCtx());
		} catch (IOException | JAXBException e) {
			ctx.getLogger().error("Object {} cannot be marshaled.\n\tProperties: {}",
					t.getDescriptiveLabel(), t.toString());
			if (ctx.getLogger().isDebugEnabled()) {
				try {
					final IFile tmpFile = IFile.createTempFile("etf-bsxds", ".xml");
					item.copyTo(tmpFile.getPath());
					ctx.getLogger().debug("Path to corrupt file: {}", tmpFile.getAbsolutePath());
				} catch (IOException ign) {
					ExcUtils.suppress(ign);
				}
			}
			item.delete();
			throw new StorageException(e);
		}
	}

	// Fires the 'add' event
	@Override
	public final void addAll(final Collection<T> collection) throws StorageException {
		// OPTIMIZE could be tuned
		final List<IFile> files = getFiles(collection);
		final Dto[] colArr = collection.toArray(new Dto[collection.size()]);
		final boolean disableable = colArr[0] instanceof Disableable;
		for (int i = 0; i < colArr.length; i++) {
			final IFile item = files.get(i);
			try {
				if (!item.createNewFile()) {
					if (disableable && isDisabled(colArr[i].getId())) {
						// Will be flushed later
						new Delete(item.getName()).execute(ctx.getBsxCtx());
					} else {
						throw new StoreException("Item " + colArr[i].getDescriptiveLabel() +
								" already exists!");
					}
				}
			} catch (StorageException e) {
				throw e;
			} catch (IOException e) {
				throw new StoreException(e);
			}
			try {
				FileUtils.touch(item);
				ctx.createMarshaller().marshal(colArr[i], item);
			} catch (JAXBException e) {
				if (ctx.getLogger().isDebugEnabled()) {
					ctx.getLogger().debug("Object {} cannot be marshaled.\n\tProperties: {}\n\tPath to file: {}",
							colArr[i].getDescriptiveLabel(), colArr[i].toString(), item.getAbsolutePath());
				} else {
					// File may contain invalid content
					item.delete();
				}
				throw new StoreException(e);
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
			throw new StoreException(e);
		}
		fireEventAdd(collection);
		updateLastModificationDate();
	}

	protected byte[] createHash(final String str) {
		int hash = 31;
		for (int i = 0; i < str.length(); i++) {
			hash = hash * 71 + str.charAt(i);
		}
		return ByteBuffer.allocate(4).putInt(hash).array();
	}

	protected byte[] createHash(final byte[] bytes) {
		int hash = 31;
		for (int i = 0; i < bytes.length; i++) {
			hash = hash * 71 + bytes[i];
		}
		return ByteBuffer.allocate(4).putInt(hash).array();
	}

	@Override
	public void replace(final T t) throws StorageException, ObjectWithIdNotFoundException {
		ensureType(t);
		doDeleteAndAdd(t);
		fireEventUpdate(t);
		updateLastModificationDate();
	}

	@Override
	public final T update(final T t) throws StorageException, ObjectWithIdNotFoundException {
		ensureType(t);
		doUpdate(t);
		updateLastModificationDate();
		return t;
	}

	// Fires the update event.
	protected T doUpdate(final T t) throws StorageException, ObjectWithIdNotFoundException {
		if (t instanceof RepositoryItemDto) {
			// get old dto from db and set "replacedBy" property to the new dto
			final RepositoryItemDto oldDtoInDb = ((RepositoryItemDto) getById(t.getId()).getDto());
			final ModelItemDto replacedBy = oldDtoInDb.getReplacedBy();
			if (replacedBy != null) {
				throw new StoreException(
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
			fireEventUpdate(oldDtoInDb);
			updateLastModificationDate();

			// Add new one
			doMarshallAndAdd(t);
		} else {
			doDeleteAndAdd(t);
		}
		return t;
	}

	protected void doDeleteAndAdd(final T t) throws StorageException, ObjectWithIdNotFoundException {
		// OPTIMIZE could be tuned
		doDeleteOrDisable(Collections.singleton(t.getId()), false);
		doMarshallAndAdd(t);
	}

	@Override
	public final Collection<T> updateAll(final Collection<T> collection)
			throws StorageException, ObjectWithIdNotFoundException {
		// OPTIMIZE could be tuned
		final List<T> updatedDtos = new ArrayList<T>(collection.size());
		for (final T dto : collection) {
			updatedDtos.add(doUpdate(dto));
		}
		updateLastModificationDate();
		return updatedDtos;
	}

	// Fires the delete event.
	@Override
	public final void delete(final EID eid) throws StorageException, ObjectWithIdNotFoundException {
		fireEventDelete(eid);
		doDeleteOrDisable(Collections.singleton(eid), true);
		updateLastModificationDate();
	}

	protected void doDeleteOrDisable(final Collection<EID> eids, boolean clean)
			throws StorageException, ObjectWithIdNotFoundException {
		final T disabledDto;
		if (Disableable.class.isAssignableFrom(this.getDtoType())) {
			// Check if IDs exist
			for (final EID eid : eids) {
				final IFile oldItem = getFile(eid);
				if (!oldItem.exists()) {
					throw new ObjectWithIdNotFoundException(this, eid.toString());
				}
			}
			updateProperty(eids, "etf:disabled", "true");
		} else {
			for (final EID eid : eids) {
				// ID checks are done in doDelete()
				doDelete(eid, clean);
			}
		}
	}

	protected void doDelete(final EID eid, boolean clean) throws StorageException, ObjectWithIdNotFoundException {
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
			throw new StorageException(e);
		} finally {
			if (!oldItem.delete()) {
				ctx.getLogger().error("File {} could not be deleted", oldItem.getAbsolutePath());
			}
		}
	}

	/**
	 * Update a property in the XML database. The changes are not synced with the backup files!
	 *
	 * @param ids IDS to change
	 * @param propertyXpath the property to change WITHOUT leading '/'
	 * @param newValue the new property value
	 */
	protected void updateProperty(final Collection<EID> ids, final String propertyXpath, final String newValue) {
		final StringBuilder query = new StringBuilder("declare namespace etf = "
				+ "\"http://www.interactive-instruments.de/etf/2.0\";"
				+ " for $item in db:open('etf-ds')");
		query.append(queryPath);
		query.append('[');
		query.append(SUtils.concatStrWithPrefixAndSuffix(" or ", "@id = 'EID", "'", ids));
		query.append("]/");
		query.append(propertyXpath);
		query.append(" return replace value of node $item with '");
		query.append(newValue);
		query.append('\'');
		try {
			new XQuery(query.toString()).execute(ctx.getBsxCtx());
		} catch (final BaseXException e) {
			ctx.getLogger().error("Internal error in updateProperty(). Query: {}", query.toString(), e);
			throw new IllegalStateException("Internal error in updateProperty()", e);
		}
	}

	protected abstract void doCleanAfterDelete(final EID eid) throws BaseXException;

	// Fires the delete event.
	@Override
	public final void deleteAll(final Set<EID> eids) throws StorageException, ObjectWithIdNotFoundException {
		// OPTIMIZE could be optimized
		for (final EID eid : eids) {
			fireEventDelete(eid);
		}
		doDeleteOrDisable(eids, true);
		updateLastModificationDate();
	}

	protected final void fireEventDelete(final EID eid) throws ObjectWithIdNotFoundException, StorageException {
		if (!listeners.isEmpty()) {
			final PreparedDto<T> dto = getById(eid);
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).writeOperationPerformed(WriteDaoListener.EventType.DELETE, dto);
			}
		}
	}

	protected final void fireEventUpdate(final Dto updatedDto) {
		if (!listeners.isEmpty()) {
			final BsxResolvedDto dto = new BsxResolvedDto(updatedDto);
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).writeOperationPerformed(WriteDaoListener.EventType.UPDATE, dto);
			}
		}
	}

	protected final void fireEventAdd(final T addedDto) {
		if (!listeners.isEmpty()) {
			final BsxResolvedDto dto = new BsxResolvedDto(addedDto);
			for (int i = 0; i < listeners.size(); i++) {
				listeners.get(i).writeOperationPerformed(WriteDaoListener.EventType.UPDATE, dto);
			}
		}
	}

	protected final void fireEventAdd(final Collection<T> addedDtos) {
		if (!listeners.isEmpty()) {
			for (final T addedDto : addedDtos) {
				final BsxResolvedDto dto = new BsxResolvedDto(addedDto);
				for (int i = 0; i < listeners.size(); i++) {
					listeners.get(i).writeOperationPerformed(WriteDaoListener.EventType.UPDATE, dto);
				}
			}
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
