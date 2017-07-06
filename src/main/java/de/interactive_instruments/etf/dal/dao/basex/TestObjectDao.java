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
