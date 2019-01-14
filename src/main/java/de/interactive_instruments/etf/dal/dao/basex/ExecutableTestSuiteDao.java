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

import java.util.List;

import javax.xml.validation.Schema;

import org.basex.core.BaseXException;
import org.slf4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.ModelItemTreeNode;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.dal.dto.test.TestModelItemDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.exceptions.StorageException;

/**
 * Executable Test Suite Data Access Object
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class ExecutableTestSuiteDao extends AbstractBsxStreamWriteDao<ExecutableTestSuiteDto> {

    private final Schema schema;

    protected ExecutableTestSuiteDao(final BsxDsCtx ctx) throws StorageException {
        super("/etf:ExecutableTestSuite", "ExecutableTestSuite", ctx,
                (dsResultSet) -> dsResultSet.getExecutableTestSuites());
        schema = ((BsxDataStorage) ctx).getSchema();
    }

    @Override
    protected void doCleanAfterDelete(final EID eid) throws BaseXException {}

    @Override
    public Class<ExecutableTestSuiteDto> getDtoType() {
        return ExecutableTestSuiteDto.class;
    }

    private static class ValidationErrorHandler implements ErrorHandler {

        private final Logger logger;

        private ValidationErrorHandler(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public void warning(final SAXParseException exception) throws SAXException {

        }

        @Override
        public void error(final SAXParseException exception) throws SAXException {
            if (!exception.getMessage().startsWith("cvc-id")) {
                throw new SAXException(exception);
            }
        }

        @Override
        public void fatalError(final SAXParseException exception) throws SAXException {
            if (!exception.getMessage().startsWith("cvc-id")) {
                throw new SAXException(exception);
            }
        }
    }

    private void updateChildrenIds(final List<? extends TestModelItemDto> children, int maxDepth) {
        if (children != null && maxDepth > 0) {
            for (final ModelItemTreeNode child : children) {
                if (child instanceof Dto) {
                    ((Dto) child).setId(EidFactory.getDefault().createRandomId());
                }
                updateChildrenIds(child.getChildren(), --maxDepth);
            }
        }
    }

    @Override
    protected void doUpdateProperties(final ExecutableTestSuiteDto executableTestSuiteDto) {
        updateChildrenIds(executableTestSuiteDto.getChildren(), 8);
    }
}
