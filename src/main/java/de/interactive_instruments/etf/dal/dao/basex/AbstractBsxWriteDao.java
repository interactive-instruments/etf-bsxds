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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Delete;
import org.basex.core.cmd.Flush;
import org.basex.core.cmd.XQuery;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.iter.Iter;
import org.basex.query.value.item.Item;
import org.basex.query.value.node.DBNode;

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

    private void flush() throws StorageException {
        try {
            new Flush().execute(ctx.getBsxCtx());
        } catch (final BaseXException e) {
            throw new StorageException(e);
        }
    }

    private void checkItemNotExists(final IFile item, final Dto t, final boolean disableable) throws StorageException {
        try {
            if (!item.createNewFile()) {
                if (disableable && isDisabled(t.getId())) {
                    // Attempt to overwrite a disabled item
                    ((Disableable) t).setDisabled(false);
                    // Will be flushed later, in upper functions
                    new Delete(item.getName()).execute(ctx.getBsxCtx());
                } else {
                    throw new StorageException("Item " + t.getDescriptiveLabel() + " already exists!");
                }
            }
        } catch (StorageException e) {
            if (isDisabled(t.getId())) {
                item.delete();
                flush();
            }
            throw e;
        } catch (IOException e) {
            if (isDisabled(t.getId())) {
                item.delete();
                flush();
            }
            throw new StorageException(e);
        }
        if (disableable) {
            // Attempt to overwrite a disabled item
            ((Disableable) t).setDisabled(false);
        }
    }

    private void marshallingFailed(final Dto t, final IFile item) {
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
    }

    protected void doMarshallAndAdd(final T t) throws StorageException {
        final IFile item = getFile(t.getId());
        checkItemNotExists(item, t, t instanceof Disableable);
        try {
            FileUtils.touch(item);
            ctx.createMarshaller().marshal(t, item);
            new Add(item.getName(), item.getAbsolutePath()).execute(ctx.getBsxCtx());
            flush();
        } catch (IOException | JAXBException e) {
            marshallingFailed(t, item);
            throw new StorageException(e);
        }
    }

    // Fires the 'add' event
    @Override
    public final void addAll(final Collection<T> collection) throws StorageException {
        if (collection.isEmpty()) {
            return;
        }
        final List<IFile> files = getFiles(collection);
        final Dto[] colArr = collection.toArray(new Dto[collection.size()]);
        final boolean disableable = colArr[0] instanceof Disableable;
        for (int i = 0; i < colArr.length; i++) {
            final IFile item = files.get(i);
            checkItemNotExists(item, colArr[i], disableable);
            try {
                FileUtils.touch(item);
                ctx.createMarshaller().marshal(colArr[i], item);
            } catch (IOException | JAXBException e) {
                marshallingFailed(colArr[i], item);
                flush();
                throw new StoreException(e);
            }
        }
        try {
            for (int i = 0; i < files.size(); i++) {
                new Add(files.get(i).getName(), files.get(i).getAbsolutePath()).execute(ctx.getBsxCtx());
            }
            flush();
        } catch (BaseXException e) {
            throw new StoreException(e);
        }
        fireEventAdd(collection);
        updateLastModificationDate();
    }

    @Override
    public void replace(final T t, final EID newId) throws StorageException, ObjectWithIdNotFoundException {
        ensureType(t);
        doDeleteAndAdd(t, newId);
        fireEventUpdate(t);
        updateLastModificationDate();
    }

    @Override
    public final T update(final T t, final EID newId) throws StorageException, ObjectWithIdNotFoundException {
        ensureType(t);
        doUpdate(t, newId);
        updateLastModificationDate();
        return t;
    }

    // Fires the update event.
    protected T doUpdate(final T t, final EID newId) throws StorageException, ObjectWithIdNotFoundException {
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

            final EID changedId;
            if (newId == null) {
                changedId = EidFactory.getDefault().createUUID(
                        oldDtoInDb.getId().toString() + "." + ((RepositoryItemDto) t).getVersionAsStr());
            } else {
                if (exists(newId)) {
                    throw new StorageException("An item with ID " + newId + " already exists");
                }
                changedId = newId;
            }
            t.setId(changedId);
            if (exists(t.getId())) {
                ctx.getLogger().warn("Overwriting existing Dto " + t.getId());
                doDeleteOrDisable(Collections.singleton(t.getId()), false);
            }

            // do internal updates
            doUpdateProperties(t);
            // ensure ID is not changed
            t.setId(changedId);
            // Increment version and change hash
            ((RepositoryItemDto) t).setVersion(oldVersion.incBugfix());
            ((RepositoryItemDto) t).setItemHash(SUtils.fastCalcHashAsHexStr(t.toString()));

            // Set replaceBy property and write back
            // TODO we can not use changeProperty here because the property does not exist, maybe addProperty is required.
            oldDtoInDb.setReplacedBy((RepositoryItemDto) t);
            doDeleteAndAdd((T) oldDtoInDb, null);
            fireEventUpdate(oldDtoInDb);
            updateLastModificationDate();

            // Add new one
            doMarshallAndAdd(t);
        } else {
            doDeleteAndAdd(t, newId);
        }
        return t;
    }

    protected void doUpdateProperties(final T t) {}

    protected void doDeleteAndAdd(final T t, final EID newId) throws StorageException, ObjectWithIdNotFoundException {
        // OPTIMIZE could be tuned
        doDelete(t.getId(), false);
        if (newId != null) {
            t.setId(newId);
        }
        doMarshallAndAdd(t);
    }

    @Override
    public final Collection<T> updateAll(final Collection<T> collection)
            throws StorageException, ObjectWithIdNotFoundException {
        if (collection.isEmpty()) {
            return collection;
        }
        final List<T> updatedDtos = new ArrayList<>(collection.size());
        for (final T dto : collection) {
            updatedDtos.add(doUpdate(dto, null));
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

    // Performance: flush after clean
    protected void doDelete(final EID eid, boolean clean) throws StorageException, ObjectWithIdNotFoundException {
        final IFile oldItem = getFile(eid);
        if (!oldItem.exists()) {
            throw new ObjectWithIdNotFoundException(this, eid.toString());
        }
        try {
            if (clean) {
                try {
                    doCleanBeforeDelete(eid);
                } catch (Exception e) {
                    ExcUtils.suppress(e);
                }
            }

            // Delete single item in the etf db
            new Delete(oldItem.getName()).execute(ctx.getBsxCtx());
            new Flush().execute(ctx.getBsxCtx());

            if (clean) {
                try {
                    doCleanAfterDelete(eid);
                } catch (Exception e) {
                    ExcUtils.suppress(e);
                }
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
     * @param ids
     *            IDS to change
     * @param propertyXpath
     *            the property to change WITHOUT leading '/'
     * @param newValue
     *            the new property value
     */
    protected void updateProperty(final Collection<EID> ids, final String propertyXpath, final String newValue) {
        final StringBuilder updateQuery = new StringBuilder(ETF_NAMESPACE_DECL
                + " for $item in db:open('etf-ds')");
        final String targetIdPredicates = SUtils.concatStrWithPrefixAndSuffix(" or ", "@id = 'EID", "'", ids);
        updateQuery.append(queryPath);
        updateQuery.append('[');
        updateQuery.append(targetIdPredicates);
        updateQuery.append("]/");
        updateQuery.append(propertyXpath);
        updateQuery.append(" return replace value of node $item with '");
        updateQuery.append(newValue);
        updateQuery.append('\'');
        try {
            new XQuery(updateQuery.toString()).execute(ctx.getBsxCtx());
        } catch (final BaseXException e) {
            ctx.getLogger().error("Internal error in updateProperty(). Query: {}", updateQuery, e);
            throw new IllegalStateException("Internal error in updateProperty()", e);
        }

        // Todo implement BackgroundTaskWorker with a PriorityBlockingQueue
        final String queryUpdatedData = ETF_NAMESPACE_DECL + "db:open('etf-ds')" + queryPath + "[" + targetIdPredicates + "]";
        // Serialize the updated data
        try (final QueryProcessor proc = new QueryProcessor(queryUpdatedData, ctx.getBsxCtx())) {
            final Iter iter = proc.iter();
            for (Item item; (item = iter.next()) != null;) {
                final EID eid = EidFactory.getDefault().createUUID(
                        new String(((DBNode) item).attribute("id".getBytes())));
                final IFile file = getFile(eid);
                if (!file.exists()) {
                    ctx.getLogger().error("Can not find backup file for {}", eid);
                    continue;
                }
                try (OutputStream fileoutput = new FileOutputStream(file)) {
                    proc.getSerializer(fileoutput).serialize(item);
                } catch (IOException e) {
                    ctx.getLogger().error("Can write backup update file for {}", eid);
                }
            }
        } catch (final QueryException e) {
            ctx.getLogger().error("Internal error in updateProperty(). Query: {}", queryUpdatedData, e);
            throw new IllegalStateException("Internal error in updateProperty()", e);
        }
    }

    protected void doCleanBeforeDelete(final EID eid) throws BaseXException {}

    protected abstract void doCleanAfterDelete(final EID eid) throws BaseXException;

    // Fires the delete event.
    @Override
    public final void deleteAll(final Set<EID> eids) throws StorageException, ObjectWithIdNotFoundException {
        if (eids.isEmpty()) {
            return;
        }
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
