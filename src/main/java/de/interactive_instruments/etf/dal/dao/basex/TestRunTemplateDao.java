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

import java.util.Collection;

import de.interactive_instruments.etf.dal.dto.capabilities.TestRunTemplateDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class TestRunTemplateDao extends AbstractBsxStreamWriteDao<TestRunTemplateDto> {

    protected TestRunTemplateDao(final BsxDsCtx ctx) throws StorageException {
        super("/etf:TestRunTemplate", "TestRunTemplate", ctx,
                (dsResultSet) -> dsResultSet.getTestRunTemplates());
    }

    @Override
    protected void doCleanAfterDelete(final EID eid) {}

    @Override
    protected void doDeleteOrDisable(final Collection<EID> eids, final boolean clean)
            throws StorageException, ObjectWithIdNotFoundException {
        for (final EID eid : eids) {
            // ID checks are done in doDelete()
            doDelete(eid, clean);
        }
    }

    @Override
    public Class<TestRunTemplateDto> getDtoType() {
        return TestRunTemplateDto.class;
    }

}
