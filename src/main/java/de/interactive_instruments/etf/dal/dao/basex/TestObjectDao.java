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
import org.basex.core.cmd.DropDB;

import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.StorageException;

/**
 * Test Object Data Access Object
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class TestObjectDao extends AbstractBsxWriteDao<TestObjectDto> {

	private static final String ETF_TESTDB_PREFIX = "etf-tdb-";

	public TestObjectDao(final BsxDsCtx ctx) throws StorageException, IOException, TransformerConfigurationException {
		super("/etf:TestObject", "TestObject", ctx,
				(dsResultSet) -> dsResultSet.getTestObjects());
	}

	@Override
	public Class<TestObjectDto> getDtoType() {
		return TestObjectDto.class;
	}

	@Override
	protected void doCleanAfterDelete(final EID eid) throws BaseXException {
		for (int i = 0; i < 10; i++) {
			final String testDbName = ETF_TESTDB_PREFIX + eid.toString() + "-" + i;
			try {
				final boolean dropped = Boolean.valueOf(new DropDB(testDbName).execute(ctx.getBsxCtx()));
				if (dropped) {
					ctx.getLogger().info("Dropped test database {}", testDbName);
				}
			} catch (final Exception e) {
				ExcUtils.suppress(e);
			}
		}
	}
}
