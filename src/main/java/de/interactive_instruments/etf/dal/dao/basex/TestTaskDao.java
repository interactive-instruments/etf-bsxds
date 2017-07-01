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

import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
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
