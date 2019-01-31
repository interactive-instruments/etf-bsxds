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

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import org.basex.core.BaseXException;

import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class TestTaskDao extends AbstractBsxWriteDao<TestTaskDto> {

    protected TestTaskDao(final BsxDsCtx ctx) throws StorageException, IOException, TransformerConfigurationException {
        super("/etf:TestRun", "TestRun", ctx,
                null);
        // TODO get test run result
    }

    @Override
    protected void doCleanBeforeDelete(final EID eid) throws BaseXException {}

    @Override
    protected void doCleanAfterDelete(final EID eid) throws BaseXException {}

    @Override
    public boolean exists(final EID eid) {
        // TODO check TestRun
        return true;
    }

    @Override
    public Class<TestTaskDto> getDtoType() {
        return TestTaskDto.class;
    }

    @Override
    public boolean isDisabled(final EID eid) {
        return false;
    }
}
