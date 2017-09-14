/**
 * Copyright 2017 European Union, interactive instruments GmbH
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

import de.interactive_instruments.etf.dal.dao.PreparedDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;

/**
 * Test Run Data Access Object
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class TestRunDao extends AbstractBsxWriteDao<TestRunDto> {

	protected TestRunDao(final BsxDsCtx ctx) throws StorageException {
		super("/etf:TestRun", "TestRun", ctx,
				(dsResultSet) -> dsResultSet.getTestRuns());
		configProperties = new ConfigProperties("etf.webapp.base.url");
	}

	@Override
	protected void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException {
		try {
			final XsltOutputTransformer reportTransformer = DsUtils.loadReportTransformer(this);
			outputFormatIdMap.put(reportTransformer.getId(), reportTransformer);
		} catch (IOException | TransformerConfigurationException e) {
			throw new InitializationException(e);
		}
	}

	@Override
	protected void doCleanBeforeDelete(final EID eid) throws BaseXException {
		try {
			final PreparedDto<TestRunDto> testRun = getById(eid);
			ctx.delete(testRun.getDto().getTestTaskResults());
		} catch (BsxPreparedDtoException | ObjectWithIdNotFoundException | StorageException e) {
			ctx.getLogger().warn("Ignoring error during clean ", e);
		}
	}

	@Override
	protected void doCleanAfterDelete(final EID eid) throws BaseXException {}

	@Override
	public Class<TestRunDto> getDtoType() {
		return TestRunDto.class;
	}

	@Override
	public boolean isDisabled(final EID eid) {
		return false;
	}
}
